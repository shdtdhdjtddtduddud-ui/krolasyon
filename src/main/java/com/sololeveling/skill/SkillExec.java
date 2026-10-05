package com.sololeveling.skill;

import com.sololeveling.entity.ShadowEntity;
import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModEntities;
import com.sololeveling.registry.ModItems;
import com.sololeveling.system.*;
import com.sololeveling.util.Ranks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SkillExec {
    private SkillExec() {}

    private static final Map<String, Long> CD = new HashMap<>();

    public static Content.SkillDef find(String id) {
        for (Content.SkillDef s : Content.SKILLS) if (s.id().equals(id)) return s;
        return null;
    }

    public static void cast(ServerPlayer sp, String id) {
        Content.SkillDef def = find(id);
        if (def == null) return;
        SLPlayer d = ModCaps.get(sp);
        if (!d.hasSkill(id)) { Sys.warn(sp, "gui.sololeveling.skill_locked"); return; }
        ServerLevel lvl = sp.serverLevel();
        long now = lvl.getGameTime();
        String key = sp.getUUID() + id;
        Long until = CD.get(key);
        if (until != null && now < until) return;
        int cost = def.mana();
        if (id.equals("shadow_extraction")) {
            Corpses.Corpse c = Corpses.nearest(sp, 16);
            cost = c == null ? 0 : 15 + c.rankIdx * 10;
        }
        if (d.mana < cost) { Sys.warn(sp, "gui.sololeveling.no_mana"); return; }
        boolean ok = run(sp, d, lvl, id);
        if (!ok) return;
        d.mana -= cost;
        d.fatigue = Math.min(100, d.fatigue + 2);
        CD.put(key, now + def.cooldown() * 20L);
        Net.toPlayer(sp, new Packets.Cooldown(id, def.cooldown() * 20));
        if (!id.equals("shadow_extraction") && !id.equals("shadow_step") && !id.equals("sprint"))
            lvl.playSound(null, sp.blockPosition(), com.sololeveling.registry.ModSounds.SKILL_CAST.get(), SoundSource.PLAYERS, 0.5F, 0.9F + sp.getRandom().nextFloat() * 0.3F);
        PlayerSync.sync(sp);
    }

    private static float atk(ServerPlayer sp) {
        return (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private static boolean run(ServerPlayer sp, SLPlayer d, ServerLevel lvl, String id) {
        switch (id) {
            case "shadow_extraction": return extract(sp, d, lvl);
            case "sprint": {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 4));
                lvl.sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY() + 0.2, sp.getZ(), 20, 0.4, 0.1, 0.4, 0.05);
                lvl.playSound(null, sp.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1F, 1.6F);
                return true;
            }
            case "dagger_rush": {
                Vec3 look = sp.getLookAngle();
                Vec3 dir = new Vec3(look.x, Math.max(-0.1, Math.min(0.25, look.y)), look.z).normalize();
                sp.setDeltaMovement(dir.scale(2.2));
                sp.hurtMarked = true;
                sp.fallDistance = 0;
                lvl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1F, 1.8F);
                for (int k = 1; k <= 7; k++) {
                    Scheduler.later(k, () -> {
                        lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0.2, 0.2, 0.2, 0);
                        lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sp.getX(), sp.getY() + 0.8, sp.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
                        for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(1.2), x -> ShadowUtil.isEnemy(sp, x)))
                            e.hurt(sp.damageSources().playerAttack(sp), atk(sp) * 0.9F + d.level * 0.3F);
                    });
                }
                return true;
            }
            case "violent_slash": {
                Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
                boolean hit = false;
                for (LivingEntity e : ShadowUtil.enemies(sp, 6)) {
                    Vec3 to = e.position().subtract(sp.position()).multiply(1, 0, 1);
                    if (to.length() < 0.01 || to.normalize().dot(look) < 0.45) continue;
                    e.hurt(sp.damageSources().playerAttack(sp), atk(sp) * 2.2F + d.level * 0.5F);
                    e.knockback(0.8, -look.x, -look.z);
                    hit = true;
                }
                for (int i = -3; i <= 3; i++) {
                    Vec3 side = new Vec3(-look.z, 0, look.x).scale(i * 0.9);
                    Vec3 p = sp.position().add(look.scale(2.8 - Math.abs(i) * 0.3)).add(side);
                    lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, sp.getY() + 1.1, p.z, 1, 0, 0, 0, 0);
                }
                lvl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4F, 0.7F);
                return true;
            }
            case "stealth": {
                sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 20, 0, false, false));
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, sp.getX(), sp.getY() + 1, sp.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
                for (net.minecraft.world.entity.Mob m : lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(32)))
                    if (m.getTarget() == sp) m.setTarget(null);
                return true;
            }
            case "bloodlust": {
                for (LivingEntity e : ShadowUtil.enemies(sp, 14)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                    lvl.sendParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 4, 0.3, 0.2, 0.3, 0);
                }
                lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sp.getX(), sp.getY() + 1, sp.getZ(), 60, 3, 0.8, 3, 0.05);
                lvl.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.2F, 1.1F);
                return true;
            }
            case "shadow_step": {
                HitResult hr = sp.pick(18, 1.0F, false);
                Vec3 dest = hr.getType() == HitResult.Type.BLOCK ? hr.getLocation().subtract(sp.getLookAngle().scale(1.0)) : sp.position().add(sp.getLookAngle().scale(18));
                lvl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.4);
                sp.teleportTo(dest.x, dest.y, dest.z);
                sp.fallDistance = 0;
                lvl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.4, 0.8, 0.4, 0.4);
                lvl.playSound(null, sp.blockPosition(), com.sololeveling.registry.ModSounds.SHADOW_STEP.get(), SoundSource.PLAYERS, 1F, 1.0F);
                return true;
            }
            case "mutilation": {
                LivingEntity t = null;
                double best = 100;
                Vec3 look = sp.getLookAngle();
                for (LivingEntity e : ShadowUtil.enemies(sp, 10)) {
                    Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(sp.getEyePosition());
                    double dot = to.normalize().dot(look);
                    if (dot > 0.8 && to.lengthSqr() < best) { best = to.lengthSqr(); t = e; }
                }
                if (t == null) { Sys.warn(sp, "gui.sololeveling.no_target"); return false; }
                final LivingEntity target = t;
                for (int i = 0; i < 9; i++) {
                    final int k = i;
                    Scheduler.later(2 + i * 2, () -> {
                        if (!target.isAlive() || !sp.isAlive()) return;
                        target.invulnerableTime = 0;
                        target.hurt(sp.damageSources().playerAttack(sp), atk(sp) * (k == 8 ? 2.5F : 0.7F) + d.level * 0.2F);
                        lvl.sendParticles(k % 2 == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 4, 0.4, 0.4, 0.4, 0.1);
                        lvl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1F, 1.2F + k * 0.05F);
                    });
                }
                return true;
            }
            case "rulers_authority": {
                List<LivingEntity> list = ShadowUtil.enemies(sp, 10);
                if (list.isEmpty()) { Sys.warn(sp, "gui.sololeveling.no_target"); return false; }
                for (LivingEntity e : list) {
                    e.setDeltaMovement(0, 1.3, 0);
                    e.hurtMarked = true;
                    lvl.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY(), e.getZ(), 20, 0.3, 0.8, 0.3, 0.5);
                    Scheduler.later(16, () -> {
                        if (!e.isAlive()) return;
                        e.setDeltaMovement(0, -2.0, 0);
                        e.hurtMarked = true;
                        e.hurt(sp.damageSources().playerAttack(sp), atk(sp) * 2.5F + d.level * 0.5F);
                        lvl.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
                    });
                }
                lvl.playSound(null, sp.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5F, 0.6F);
                return true;
            }
            case "quicksilver": {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 12, 3));
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 12, 3));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 12, 1));
                lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, sp.getX(), sp.getY() + 1, sp.getZ(), 50, 0.5, 0.9, 0.5, 0.2);
                lvl.playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1F, 1.8F);
                return true;
            }
            case "shadow_exchange": {
                ShadowEntity best = null;
                double bd = Double.MAX_VALUE;
                for (ShadowEntity s : ShadowUtil.owned(sp)) {
                    if (s.level() != lvl) continue;
                    double dd = s.distanceToSqr(sp);
                    if (dd < bd && dd < 60 * 60) { bd = dd; best = s; }
                }
                if (best == null) { Sys.warn(sp, "gui.sololeveling.no_shadows"); return false; }
                Vec3 a = sp.position(), b = best.position();
                lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.x, a.y + 1, a.z, 30, 0.3, 0.8, 0.3, 0.05);
                lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, b.x, b.y + 1, b.z, 30, 0.3, 0.8, 0.3, 0.05);
                sp.teleportTo(b.x, b.y, b.z);
                best.teleportTo(a.x, a.y, a.z);
                lvl.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 0.7F);
                return true;
            }
            case "dragons_fear": {
                for (LivingEntity e : ShadowUtil.enemies(sp, 24)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 9));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
                    e.hurt(sp.damageSources().magic(), Math.min(e.getMaxHealth() * 0.25F, atk(sp) * 10F) + d.level);
                    lvl.sendParticles(ParticleTypes.DRAGON_BREATH, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                }
                lvl.sendParticles(ParticleTypes.DRAGON_BREATH, sp.getX(), sp.getY() + 0.5, sp.getZ(), 200, 8, 0.5, 8, 0.05);
                lvl.playSound(null, sp.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2F, 0.7F);
                return true;
            }
            case "domain_of_monarch": {
                List<ShadowEntity> sh = ShadowUtil.owned(sp);
                buff(sp);
                for (ShadowEntity s : sh) {
                    s.empowered = true;
                    s.rescale(false);
                    buff(s);
                }
                for (int r = 1; r <= 20; r++) {
                    final int rr = r;
                    Scheduler.later(r, () -> {
                        for (int a = 0; a < 40; a++) {
                            double ang = a / 40.0 * Math.PI * 2;
                            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sp.getX() + Math.cos(ang) * rr * 1.5, sp.getY() + 0.2, sp.getZ() + Math.sin(ang) * rr * 1.5, 1, 0, 0.1, 0, 0.02);
                        }
                    });
                }
                Scheduler.later(600, () -> { for (ShadowEntity s : sh) if (s.isAlive()) { s.empowered = false; s.rescale(false); } });
                lvl.playSound(null, sp.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.2F, 1.4F);
                return true;
            }
            default:
                return false;
        }
    }

    private static void buff(LivingEntity e) {
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
    }

    // ------------------------------------------------------------------ ARISE
    private static boolean extract(ServerPlayer sp, SLPlayer d, ServerLevel lvl) {
        Corpses.Corpse c = Corpses.nearest(sp, 16);
        if (c == null) { Sys.warn(sp, "gui.sololeveling.no_corpse"); return false; }
        List<ShadowEntity> owned = ShadowUtil.owned(sp);
        if (owned.size() >= d.maxShadows()) { Sys.warn(sp, "gui.sololeveling.army_full", d.maxShadows()); return false; }
        int pr = Ranks.index(d.rank());
        double chance = 0.55 - 0.1 * Math.max(0, c.rankIdx - pr) + d.intel() * 0.004 + (c.boss ? -0.15 : 0);
        boolean essence = false;
        for (ItemStack s : sp.getInventory().items) {
            if (s.getItem() == ModItems.get("shadow_essence")) { essence = true; s.shrink(1); chance += 0.25; break; }
        }
        chance = Math.max(0.05, Math.min(0.95, chance));
        Corpses.remove(c);
        lvl.sendParticles(ParticleTypes.SOUL, c.x, c.y + 0.5, c.z, 60, 0.5, 1.0, 0.5, 0.1);
        if (sp.getRandom().nextDouble() > chance) {
            Sys.notify(sp, Sys.WARN, Component.translatable("gui.sololeveling.extract_failed"), Component.translatable("gui.sololeveling.extract_failed_body"));
            lvl.playSound(null, sp.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.8F);
            return true;
        }
        String type = switch (c.shadowType) {
            case "mage" -> "shadow_mage";
            case "wolf" -> "shadow_wolf";
            case "tank" -> "shadow_tank";
            case "igris" -> "shadow_igris";
            case "beru" -> "shadow_beru";
            default -> "shadow_soldier";
        };
        if (type.equals("shadow_igris") || type.equals("shadow_beru")) {
            for (ShadowEntity s : owned) if (s.def.id().equals(type)) { type = "shadow_soldier"; break; }
        }
        ShadowEntity sh = ModEntities.SHADOWS.get(type).get().create(lvl);
        if (sh == null) return false;
        sh.moveTo(c.x, c.y, c.z, sp.getYRot() + 180F, 0);
        sh.setOwner(sp);
        sh.setCustomName(Component.translatable("gui.sololeveling.shadow_name", c.name));
        sh.setRankIdx(c.rankIdx);
        lvl.addFreshEntity(sh);
        d.shadowsExtracted++;
        Adv.grant(sp, "arise");
        if (d.shadowsExtracted >= 10) Adv.grant(sp, "army");
        if (d.job.equals("none")) {
            d.job = "necromancer";
            Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.job_change"), Component.translatable("gui.sololeveling.job.necromancer"));
        }
        for (int a = 0; a < 36; a++) {
            double ang = a / 36.0 * Math.PI * 2;
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(ang) * 1.5, c.y + 0.1, c.z + Math.sin(ang) * 1.5, 1, 0, 0.3, 0, 0.05);
        }
        lvl.playSound(null, sp.blockPosition(), com.sololeveling.registry.ModSounds.ARISE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        Sys.notify(sp, Sys.ARISE, Component.translatable("gui.sololeveling.arise"), Component.translatable("gui.sololeveling.extracted", sh.getDisplayName()));
        Net.toPlayer(sp, new Packets.Fx("arise", 0));
        return true;
    }
}
