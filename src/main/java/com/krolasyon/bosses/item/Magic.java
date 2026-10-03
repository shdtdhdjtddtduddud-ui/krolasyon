package com.krolasyon.bosses.item;

import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.faction.PlayerData;
import com.krolasyon.bosses.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Every active power of the swords and spell tomes. {@link #cast} returns false when the power could not be used (nothing happens, nothing is spent). */
public final class Magic {
    private Magic() {}

    public static boolean isFoe(Player p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (e instanceof Player) return false;
        if (e instanceof HellMob h && h.getOwnerUUID() != null && h.getOwnerUUID().equals(p.getUUID())) return false;
        return true;
    }

    private static List<LivingEntity> foesNear(Player p, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> isFoe(p, e) && e.distanceToSqr(p) <= r * r);
    }

    /** foes inside a cone in front of the player */
    private static List<LivingEntity> cone(Player p, double range, double minDot) {
        Vec3 look = p.getLookAngle().normalize();
        return foesNear(p, range).stream().filter(e -> {
            Vec3 d = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(p.getEyePosition());
            return d.length() < 0.5 || d.normalize().dot(look) >= minDot;
        }).toList();
    }

    private static LivingEntity aimed(Player p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range), x -> isFoe(p, x))) {
            AABB bb = e.getBoundingBox().inflate(0.6);
            if (bb.clip(eye, eye.add(look.scale(range))).isPresent()) {
                double d = e.distanceToSqr(p);
                if (d < bd) { bd = d; best = e; }
            }
        }
        return best;
    }

    private static void fx(ServerLevel l, ParticleOptions pt, double x, double y, double z, int n, double sp, double v) {
        l.sendParticles(pt, x, y, z, n, sp, sp, sp, v);
    }

    private static void ring(ServerLevel l, ParticleOptions pt, Vec3 c, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            l.sendParticles(pt, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0.05, 0.1, 0.05, 0.02);
        }
    }

    private static void magicHit(Player p, LivingEntity e, float dmg) {
        float mul = e instanceof HellMob h && h.isBossMob() ? 1.0F : 1.0F;
        e.hurt(p.damageSources().indirectMagic(p, p), dmg * mul);
    }

    private static void sound(ServerLevel l, Player p, net.minecraft.sounds.SoundEvent s, float pitch) {
        l.playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, 1.0F, pitch);
    }

    public static boolean cast(String id, ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 pos = p.position(), look = p.getLookAngle();
        switch (id) {
            case "ember_brand", "fireball" -> {
                if (id.equals("fireball")) {
                    for (int i = -1; i <= 1; i++) {
                        Vec3 d = look.yRot((float) Math.toRadians(i * 9));
                        SmallFireball fb = new SmallFireball(l, p, d.x, d.y, d.z);
                        fb.setPos(p.getX() + d.x * 1.2, p.getEyeY() - 0.2, p.getZ() + d.z * 1.2);
                        l.addFreshEntity(fb);
                    }
                    sound(l, p, SoundEvents.BLAZE_SHOOT, 1.0F);
                    return true;
                }
                List<LivingEntity> hit = cone(p, 7, 0.55);
                for (int i = 0; i < 40; i++) {
                    double t = i / 40.0 * 6.5;
                    Vec3 q = p.getEyePosition().add(look.scale(t));
                    fx(l, ParticleTypes.FLAME, q.x, q.y - 0.3, q.z, 3, 0.35 + t * 0.06, 0.02);
                }
                for (LivingEntity e : hit) { magicHit(p, e, 7); e.setSecondsOnFire(7); }
                sound(l, p, SoundEvents.FIRECHARGE_USE, 0.7F);
                return true;
            }
            case "hellrain" -> {
                LivingEntity t = aimed(p, 30);
                Vec3 c = t != null ? t.position() : p.getEyePosition().add(look.scale(14));
                for (int i = 0; i < 14; i++) {
                    double ox = (p.getRandom().nextDouble() - 0.5) * 9, oz = (p.getRandom().nextDouble() - 0.5) * 9;
                    SmallFireball fb = new SmallFireball(l, p, 0, -1, 0);
                    fb.setPos(c.x + ox, c.y + 12 + p.getRandom().nextDouble() * 5, c.z + oz);
                    l.addFreshEntity(fb);
                }
                ring(l, ParticleTypes.FLAME, c, 4.5, 36);
                sound(l, p, SoundEvents.BLAZE_SHOOT, 0.5F);
                return true;
            }
            case "bone_reaper" -> {
                for (int i = 1; i <= 8; i++) {
                    Vec3 q = pos.add(look.x * i, 0, look.z * i);
                    fx(l, ParticleTypes.CRIT, q.x, q.y + 0.4, q.z, 8, 0.3, 0.3);
                    fx(l, ParticleTypes.ASH, q.x, q.y + 0.2, q.z, 6, 0.3, 0.05);
                }
                for (LivingEntity e : cone(p, 8, 0.8)) {
                    magicHit(p, e, 6);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    e.push(0, 0.5, 0);
                }
                sound(l, p, SoundEvents.SKELETON_HURT, 0.6F);
                return true;
            }
            case "blood_thirst", "blood_pact" -> {
                float cost = id.equals("blood_pact") ? 4F : 6F;
                if (p.getHealth() <= cost + 2 && !p.isCreative()) return false;
                if (!p.isCreative()) p.hurt(p.damageSources().magic(), cost);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                if (id.equals("blood_pact")) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                fx(l, ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 14, 0.4, 0.1);
                ring(l, ParticleTypes.CRIMSON_SPORE, pos, 1.4, 24);
                sound(l, p, SoundEvents.WITHER_SPAWN, 1.6F);
                return true;
            }
            case "shadow_fang", "void_step" -> {
                double dist = id.equals("void_step") ? 12 : 8;
                Vec3 eye = p.getEyePosition();
                Vec3 end = eye.add(look.scale(dist));
                var hit = l.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
                Vec3 to = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.8));
                fx(l, ParticleTypes.PORTAL, pos.x, pos.y + 1, pos.z, 40, 0.4, 0.5);
                p.teleportTo(to.x, to.y - 1.62 + p.getEyeHeight() - 0.0, to.z);
                p.fallDistance = 0;
                fx(l, ParticleTypes.REVERSE_PORTAL, to.x, to.y, to.z, 40, 0.4, 0.2);
                if (id.equals("shadow_fang")) {
                    p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
                    for (LivingEntity e : foesNear(p, 3)) { magicHit(p, e, 5); e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)); }
                }
                sound(l, p, SoundEvents.ENDERMAN_TELEPORT, 0.8F);
                return true;
            }
            case "rot_cleaver" -> {
                Vec3 c = pos.add(look.x * 4, 0, look.z * 4);
                AreaEffectCloud cloud = new AreaEffectCloud(l, c.x, c.y, c.z);
                cloud.setOwner(p);
                cloud.setRadius(3.5F);
                cloud.setDuration(120);
                cloud.setParticle(ParticleTypes.SPORE_BLOSSOM_AIR);
                cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                l.addFreshEntity(cloud);
                sound(l, p, SoundEvents.SLIME_BLOCK_BREAK, 0.5F);
                return true;
            }
            case "thorn_burst" -> {
                for (LivingEntity e : foesNear(p, 5)) {
                    magicHit(p, e, 6);
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
                }
                for (int r = 1; r <= 5; r++) ring(l, ParticleTypes.COMPOSTER, pos, r, 10 + r * 4);
                sound(l, p, SoundEvents.SWEET_BERRY_BUSH_BREAK, 0.6F);
                return true;
            }
            case "sealbreaker", "obsidian_greataxe" -> {
                double r = id.equals("sealbreaker") ? 5 : 6;
                boolean any = false;
                for (LivingEntity e : foesNear(p, r)) {
                    float dmg = id.equals("sealbreaker") ? 8 : 9;
                    if (e instanceof HellMob h && h.isBossMob()) dmg *= 1.5F;
                    magicHit(p, e, dmg);
                    Vec3 d = e.position().subtract(pos);
                    e.knockback(1.1, -d.x, -d.z);
                    e.push(0, 0.5, 0);
                    any = true;
                }
                for (int i = 1; i <= 5; i++) ring(l, ParticleTypes.CAMPFIRE_COSY_SMOKE, pos, i * r / 5, 12 + i * 5);
                fx(l, ParticleTypes.EXPLOSION, pos.x, pos.y + 0.3, pos.z, 3, 1.0, 0);
                sound(l, p, SoundEvents.GENERIC_EXPLODE, 0.8F);
                return true;
            }
            case "chain_whip", "soul_chain" -> {
                LivingEntity t = aimed(p, id.equals("soul_chain") ? 18 : 13);
                if (t == null) return false;
                Vec3 d = p.position().subtract(t.position());
                t.push(d.x * 0.18, 0.35, d.z * 0.18);
                t.hurtMarked = true;
                magicHit(p, t, id.equals("soul_chain") ? 4 : 5);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                if (id.equals("soul_chain")) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
                Vec3 a = p.getEyePosition().subtract(0, 0.4, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
                for (int i = 0; i <= 20; i++) {
                    Vec3 q = a.lerp(b, i / 20.0);
                    fx(l, id.equals("soul_chain") ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT, q.x, q.y, q.z, 1, 0.02, 0.01);
                }
                sound(l, p, SoundEvents.CHAIN_BREAK, 0.8F);
                return true;
            }
            case "bone_prison" -> {
                LivingEntity t = aimed(p, 20);
                if (t == null) return false;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 6));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    for (double y = 0; y < t.getBbHeight() + 0.4; y += 0.4)
                        fx(l, ParticleTypes.ASH, t.getX() + Math.cos(a) * 0.9, t.getY() + y, t.getZ() + Math.sin(a) * 0.9, 1, 0.02, 0.0);
                }
                sound(l, p, SoundEvents.SKELETON_AMBIENT, 0.6F);
                return true;
            }
            case "summon_imp" -> {
                int owned = l.getEntitiesOfClass(HellMob.class, p.getBoundingBox().inflate(48), m -> p.getUUID().equals(m.getOwnerUUID())).size();
                if (owned >= 4) { p.displayClientMessage(Component.translatable("msg.too_many_minions"), true); return false; }
                for (int i = 0; i < 2; i++) {
                    HellMob imp = ModEntities.MOBS.get("cinder_imp").get().create(l);
                    if (imp == null) return false;
                    imp.moveTo(p.getX() + (i == 0 ? 1.5 : -1.5), p.getY(), p.getZ() + 1.0, p.getYRot(), 0);
                    imp.setOwnerUUID(p.getUUID());
                    l.addFreshEntity(imp);
                    fx(l, ParticleTypes.FLAME, imp.getX(), imp.getY() + 0.5, imp.getZ(), 20, 0.3, 0.05);
                }
                sound(l, p, SoundEvents.EVOKER_PREPARE_SUMMON, 1.2F);
                return true;
            }
            case "hellfire_scythe" -> {
                for (LivingEntity e : foesNear(p, 6)) { magicHit(p, e, 8); e.setSecondsOnFire(8); e.knockback(0.6, pos.x - e.getX(), pos.z - e.getZ()); }
                for (int i = 1; i <= 6; i++) ring(l, ParticleTypes.FLAME, pos, i, 14 + i * 6);
                sound(l, p, SoundEvents.FIRECHARGE_USE, 0.6F);
                return true;
            }
            case "sovereign_blade", "decree" -> {
                if (!PlayerData.isSovereign(p) && !p.isCreative()) { p.displayClientMessage(Component.translatable("msg.not_sovereign"), true); return false; }
                for (LivingEntity e : foesNear(p, 9)) {
                    magicHit(p, e, id.equals("decree") ? 10 : 12);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                }
                for (HellMob m : l.getEntitiesOfClass(HellMob.class, p.getBoundingBox().inflate(24), m -> p.getUUID().equals(m.getOwnerUUID()))) {
                    m.heal(m.getMaxHealth());
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1));
                }
                for (int i = 1; i <= 9; i++) ring(l, ParticleTypes.END_ROD, pos, i, 16 + i * 4);
                fx(l, ParticleTypes.FLASH, pos.x, pos.y + 1, pos.z, 1, 0, 0);
                sound(l, p, SoundEvents.BEACON_ACTIVATE, 0.6F);
                return true;
            }
            default -> { return false; }
        }
    }
}
