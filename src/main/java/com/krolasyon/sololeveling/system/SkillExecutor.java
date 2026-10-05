package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.entity.SLProjectile;
import com.krolasyon.sololeveling.entity.ShadowEntity;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModSounds;
import com.krolasyon.sololeveling.shadow.ShadowManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** Server side implementation of every active skill. */
public final class SkillExecutor {
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.55F, 0.3F, 1F), 1.5F);
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.3F, 0.75F, 1F), 1.3F);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1F, 0.15F, 0.2F), 1.6F);

    private SkillExecutor() {}

    public static void useSlot(ServerPlayer p, int slot) {
        HunterData d = HunterCapability.get(p);
        if (!d.awakened || slot < 0 || slot >= HunterData.SLOTS) return;
        Skill s = d.slots[slot];
        if (s == null) {
            Sys.warn(p, "skill.empty_slot", slot + 1);
            return;
        }
        use(p, s);
    }

    public static void use(ServerPlayer p, Skill s) {
        HunterData d = HunterCapability.get(p);
        if (!s.active || !d.hasSkill(s)) {
            Sys.warn(p, "skill.locked");
            return;
        }
        if (d.cooldown(s) > 0) {
            p.displayClientMessage(Sys.t("skill.cooldown", Sys.t("skill." + s.id()), String.format(java.util.Locale.ROOT, "%.1f", d.cooldown(s) / 20F)), true);
            return;
        }
        if (d.mana < s.mana && !p.isCreative()) {
            Sys.warn(p, "skill.no_mana");
            return;
        }
        if (!execute(p, d, s)) return;
        if (!p.isCreative()) d.mana -= s.mana;
        d.cooldowns.put(s, s.cooldown);
        d.markDirty();
        p.displayClientMessage(Sys.t("skill.used", Sys.t("skill." + s.id())), true);
    }

    /** Physical power of the player: attack damage attribute (weapon + strength). */
    public static float physical(Player p) { return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE); }

    public static float magic(HunterData d) { return 4 + d.stat(Stat.INT) * 0.45F + d.level * 0.2F; }

    private static boolean execute(ServerPlayer p, HunterData d, Skill s) {
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        switch (s) {
            case SPRINT -> {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1, false, false, true));
                l.sendParticles(BLUE, p.getX(), p.getY() + 0.2, p.getZ(), 30, 0.4, 0.1, 0.4, 0.05);
                l.playSound(null, p.blockPosition(), ModSounds.DASH.get(), SoundSource.PLAYERS, 1F, 1.2F);
            }
            case DAGGER_THROW -> {
                float dmg = 3 + physical(p) * 0.6F + d.stat(Stat.AGI) * 0.12F;
                SLProjectile.shoot(p, SLProjectile.Kind.SHADOW_DAGGER, look, 2.2, dmg).pierce = d.level >= 30 ? 2 : 0;
                l.playSound(null, p.blockPosition(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 1F, 1.6F);
            }
            case BLOODLUST -> {
                List<LivingEntity> list = enemies(p, 12);
                for (LivingEntity e : list) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                    l.sendParticles(RED, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 8, 0.3, 0.3, 0.3, 0);
                }
                l.sendParticles(RED, p.getX(), p.getY() + 1, p.getZ(), 80, 4, 1, 4, 0);
                l.playSound(null, p.blockPosition(), ModSounds.BLOODLUST.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
                Net.to(p, new Net.Fx("tint", 1, 0.1, 0.1, 25));
            }
            case MUTILATION -> {
                Vec3 dir = new Vec3(look.x, 0, look.z).normalize();
                Vec3 start = p.position();
                Vec3 end = start.add(dir.scale(7));
                BlockHitResult bh = l.clip(new ClipContext(start.add(0, 0.6, 0), end.add(0, 0.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (bh.getType() == HitResult.Type.BLOCK) end = bh.getLocation().subtract(dir.scale(0.6)).subtract(0, 0.6, 0);
                float dmg = 4 + physical(p) * 1.4F;
                AABB box = new AABB(start, end).inflate(1.4, 1.2, 1.4);
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e))) {
                    e.invulnerableTime = 0;
                    e.hurt(p.damageSources().playerAttack(p), dmg);
                    l.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 3, 0.3, 0.3, 0.3, 0);
                }
                for (int i = 0; i <= 10; i++) {
                    Vec3 q = start.lerp(end, i / 10.0);
                    l.sendParticles(PURPLE, q.x, q.y + 1, q.z, 4, 0.2, 0.4, 0.2, 0);
                }
                p.teleportTo(end.x, end.y, end.z);
                p.fallDistance = 0;
                l.playSound(null, p.blockPosition(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 1.3F, 0.9F);
            }
            case STEALTH -> {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, true));
                d.stealthTicks = 200;
                for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) m.setTarget(null);
                l.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            }
            case VITAL_STRIKE -> {
                LivingEntity t = lookTarget(p, 5);
                if (t == null) {
                    Sys.warn(p, "skill.no_target");
                    return false;
                }
                t.invulnerableTime = 0;
                t.hurt(p.damageSources().playerAttack(p), physical(p) * 2.6F + d.stat(Stat.PER) * 0.2F);
                l.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 30, 0.3, 0.4, 0.3, 0.4);
                l.sendParticles(RED, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 15, 0.2, 0.3, 0.2, 0);
                l.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5F, 0.7F);
            }
            case SHADOW_EXCHANGE -> {
                if (!ShadowManager.exchange(p)) {
                    Sys.warn(p, "shadows.none_active");
                    return false;
                }
                l.playSound(null, p.blockPosition(), ModSounds.DASH.get(), SoundSource.PLAYERS, 1F, 0.6F);
            }
            case RULERS_AUTHORITY -> {
                LivingEntity t = lookTarget(p, 24);
                float dmg = magic(d) * 1.3F;
                if (t != null) {
                    t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 22, 3));
                    l.sendParticles(BLUE, t.getX(), t.getY() + 1, t.getZ(), 40, 0.5, 1, 0.5, 0);
                    Scheduler.later(24, () -> {
                        if (!t.isAlive()) return;
                        t.removeEffect(MobEffects.LEVITATION);
                        t.setDeltaMovement(0, -2.5, 0);
                        t.hurtMarked = true;
                        Scheduler.later(6, () -> {
                            t.invulnerableTime = 0;
                            t.hurt(p.damageSources().indirectMagic(p, p), dmg);
                            l.sendParticles(ParticleTypes.EXPLOSION, t.getX(), t.getY(), t.getZ(), 3, 0.5, 0.2, 0.5, 0);
                            l.playSound(null, t.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.3F);
                        });
                    });
                } else {
                    for (LivingEntity e : enemies(p, 9)) {
                        Vec3 to = e.position().subtract(p.position());
                        if (to.normalize().dot(look) < 0.4) continue;
                        e.hurt(p.damageSources().indirectMagic(p, p), dmg * 0.6F);
                        Vec3 k = to.normalize().scale(2.2);
                        e.push(k.x, 0.6, k.z);
                        e.hurtMarked = true;
                    }
                    for (int i = 1; i < 9; i++) l.sendParticles(BLUE, p.getX() + look.x * i, p.getEyeY() + look.y * i, p.getZ() + look.z * i, 6, 0.4 * i / 3, 0.4, 0.4 * i / 3, 0);
                }
                l.playSound(null, p.blockPosition(), ModSounds.AUTHORITY.get(), SoundSource.PLAYERS, 1.2F, 1F);
            }
            case DAGGER_STORM -> {
                float dmg = 2 + physical(p) * 0.5F;
                for (int w = 0; w < 4; w++) {
                    int wave = w;
                    Scheduler.later(w * 4, () -> {
                        for (int i = -3; i <= 3; i++) {
                            Vec3 dir = p.getLookAngle().yRot((float) Math.toRadians(i * 9 + (wave % 2) * 4));
                            SLProjectile.shoot(p, SLProjectile.Kind.SHADOW_DAGGER, dir, 2.0, dmg);
                        }
                        l.playSound(null, p.blockPosition(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 0.8F, 1.8F);
                    });
                }
            }
            case DRAGONS_FEAR -> {
                for (LivingEntity e : enemies(p, 20)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 6));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
                    e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2));
                    e.hurt(p.damageSources().indirectMagic(p, p), 4 + magic(d) * 0.5F);
                    if (e instanceof Mob m) m.getNavigation().stop();
                }
                for (int r = 2; r <= 20; r += 3) {
                    int rr = r;
                    Scheduler.later(r / 3, () -> {
                        for (int i = 0; i < 36; i++) {
                            double a = Math.PI * 2 * i / 36;
                            l.sendParticles(PURPLE, p.getX() + Math.cos(a) * rr, p.getY() + 0.5, p.getZ() + Math.sin(a) * rr, 1, 0, 0.1, 0, 0);
                        }
                    });
                }
                l.playSound(null, p.blockPosition(), ModSounds.ROAR.get(), SoundSource.PLAYERS, 3F, 0.7F);
                Net.to(p, new Net.Fx("shake", 0, 0, 0, 20));
            }
            case MONARCHS_DOMAIN -> {
                d.domainTicks = 600;
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0, false, false, true));
                l.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.1, p.getZ(), 300, 8, 0.05, 8, 0);
                l.playSound(null, p.blockPosition(), ModSounds.DOMAIN.get(), SoundSource.PLAYERS, 2F, 0.9F);
                Net.to(p, new Net.Fx("tint", 0.4, 0.2, 0.8, 30));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public static boolean isEnemy(Player p, LivingEntity e) {
        if (e == p || !e.isAlive()) return false;
        if (e instanceof ShadowEntity s && s.getOwner() == p) return false;
        if (e instanceof Player) return false;
        if (e instanceof com.krolasyon.sololeveling.entity.HunterNpc) return false;
        if (e instanceof net.minecraft.world.entity.animal.Animal || e instanceof net.minecraft.world.entity.npc.AbstractVillager) return false;
        return true;
    }

    public static List<LivingEntity> enemies(Player p, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> isEnemy(p, e));
    }

    public static LivingEntity lookTarget(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() == HitResult.Type.BLOCK) end = bh.getLocation();
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(p, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e instanceof LivingEntity le && isEnemy(p, le), range * range);
        return eh == null ? null : (LivingEntity) eh.getEntity();
    }

}
