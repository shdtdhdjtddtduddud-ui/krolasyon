package com.krolasyon.mythblades.combat;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.entity.EmberWaveEntity;
import com.krolasyon.mythblades.network.FxNet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Kor Kılıcı dövüş stili (sunucu): üç vuruşluk kombo (üçüncüsü alev hilali), savunma ve karşı koyma (parry),
 * kor atılışı (dash: dokunulmazlık ve yol boyunca yanık) ve kor saplama (sıçrayıp yere çakılma).
 */
public final class EmberCombat {
    private EmberCombat() {}

    public static final int COMBO_WINDOW = 22, DASH_CD = 45, PLUNGE_CD = 140, PARRY_WINDOW = 7;

    public static final class State {
        public int stage = -1;
        long lastSwing = -100, dashCd, plungeCd, counterUntil;
        int dashTicks, plungeTicks;
        boolean prevSwinging;
        int prevSwingTime;
        double dashX, dashZ;
        final Set<Integer> dashHit = new HashSet<>();
        long safeFallUntil;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static State state(Player p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new State());
    }

    private static boolean holds(Player p) {
        return p.getMainHandItem().is(MythBlades.EMBER.get());
    }

    static boolean foe(Player p, Entity e) {
        return e != p && e instanceof LivingEntity le && le.isAlive() && !e.isSpectator() && !p.isAlliedTo(e)
                && !(e instanceof TamableAnimal t && t.isOwnedBy(p)) && !(e instanceof Player);
    }

    // ------------------------------------------------------------------ istemciden hareket

    public static void onMove(ServerPlayer p, boolean plunge, double dx, double dz) {
        if (!holds(p) || p.isSpectator()) return;
        State s = state(p);
        long now = p.level().getGameTime();
        ServerLevel l = p.serverLevel();
        double len = Math.hypot(dx, dz);
        if (len < 1e-3) return;
        dx /= len;
        dz /= len;
        if (plunge) {
            if (now < s.plungeCd) return;
            s.plungeCd = now + PLUNGE_CD;
            s.plungeTicks = 1;
            s.safeFallUntil = now + 80;
            l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1F, 0.6F);
            FxNet.fx(p, new FxNet.Fx(FxNet.PLUNGE, p.getId(), p.getX(), p.getY(), p.getZ(), 0, 0));
        } else {
            if (now < s.dashCd) return;
            s.dashCd = now + DASH_CD;
            s.dashTicks = 7;
            s.dashX = dx;
            s.dashZ = dz;
            s.dashHit.clear();
            l.playSound(null, p.blockPosition(), MythBlades.SFX.get("ember_dash").get(), SoundSource.PLAYERS, 1F, 0.95F + p.getRandom().nextFloat() * 0.1F);
            FxNet.fx(p, new FxNet.Fx(FxNet.DASH, p.getId(), p.getX(), p.getY(), p.getZ(), (float) dx, (float) dz));
        }
        p.getMainHandItem().hurtAndBreak(1, p, x -> x.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND));
    }

    // ------------------------------------------------------------------ tik: kombo, atılış hasarı, saplama

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        State s0 = STATES.get(p.getUUID());
        if (s0 == null && !holds(p)) return;
        final State s = s0 != null ? s0 : state(p);
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();

        // kombo: yeni bir savuruş başladı mı?
        boolean newSwing = p.swinging && (!s.prevSwinging || p.swingTime < s.prevSwingTime);
        s.prevSwinging = p.swinging;
        s.prevSwingTime = p.swingTime;
        if (newSwing && holds(p) && !p.isUsingItem()) {
            s.stage = now - s.lastSwing <= COMBO_WINDOW ? (s.stage + 1) % 3 : 0;
            s.lastSwing = now;
            l.playSound(null, p.blockPosition(), MythBlades.SFX.get("ember_swing" + (s.stage + 1)).get(), SoundSource.PLAYERS, 0.9F,
                    0.92F + p.getRandom().nextFloat() * 0.16F);
            boolean counter = now < s.counterUntil;
            if (s.stage == 2 || counter) {
                EmberWaveEntity w = new EmberWaveEntity(MythBlades.EMBER_WAVE.get(), l);
                w.setOwner(p);
                Vec3 look = p.getLookAngle();
                w.setup(counter, s.stage == 2 ? 0 : (s.stage == 0 ? 18F : -18F));
                w.moveTo(p.getX() + look.x * 0.8, p.getEyeY() - 0.55, p.getZ() + look.z * 0.8, p.getYRot(), p.getXRot());
                w.setDeltaMovement(look.scale(counter ? 1.15 : 1.25));
                l.addFreshEntity(w);
                l.playSound(null, p.blockPosition(), MythBlades.SFX.get("ember_wave").get(), SoundSource.PLAYERS, counter ? 1.4F : 1F, counter ? 0.8F : 1F);
                if (counter) s.counterUntil = 0;
            }
        }

        // atılış: yol boyunca yakar, kısa süre dokunulmaz
        if (s.dashTicks > 0) {
            s.dashTicks--;
            for (Entity t : l.getEntities(p, p.getBoundingBox().inflate(1.4, 0.6, 1.4), x -> foe(p, x) && !s.dashHit.contains(x.getId()))) {
                s.dashHit.add(t.getId());
                LivingEntity le = (LivingEntity) t;
                le.hurt(p.damageSources().playerAttack(p), 7F);
                le.setSecondsOnFire(5);
                le.knockback(0.6, -s.dashZ, s.dashX);
                l.sendParticles(ParticleTypes.FLAME, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 14, 0.3, 0.4, 0.3, 0.06);
            }
            l.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.2, p.getZ(), 4, 0.25, 0.1, 0.25, 0.01);
        }

        // kor saplama: havaya sıçrar, yere değince patlar
        if (s.plungeTicks > 0) {
            s.plungeTicks++;
            if ((s.plungeTicks > 6 && p.onGround()) || s.plungeTicks > 45 || p.isInWater()) {
                s.plungeTicks = 0;
                impact(p, l);
            }
        }
    }

    private static void impact(ServerPlayer p, ServerLevel l) {
        Vec3 c = p.position();
        for (Entity t : l.getEntities(p, p.getBoundingBox().inflate(6, 3, 6), x -> foe(p, x))) {
            double d = t.distanceTo(p);
            if (d > 6.2) continue;
            LivingEntity le = (LivingEntity) t;
            le.hurt(p.damageSources().playerAttack(p), (float) (14 - d * 1.4));
            le.setSecondsOnFire(8);
            Vec3 away = le.position().subtract(c).multiply(1, 0, 1).normalize();
            le.setDeltaMovement(le.getDeltaMovement().add(away.x * 0.9, 0.75, away.z * 0.9));
            le.hurtMarked = true;
        }
        l.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.2, c.z, 30, 2, 0.2, 2, 0);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.3, c.z, 10, 2.5, 0.3, 2.5, 0.02);
        l.playSound(null, p.blockPosition(), MythBlades.SFX.get("ember_impact").get(), SoundSource.PLAYERS, 1.5F, 1F);
        FxNet.fx(p, new FxNet.Fx(FxNet.IMPACT, p.getId(), c.x, c.y, c.z, 6F, 0));
        p.getMainHandItem().hurtAndBreak(2, p, x -> x.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND));
    }

    // ------------------------------------------------------------------ savunma ve karşı koyma

    private static boolean guarding(Player p, DamageSource src) {
        if (!p.isUsingItem() || !p.getUseItem().is(MythBlades.EMBER.get()) || src.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        Vec3 from = src.getSourcePosition();
        if (from == null) return false;
        Vec3 to = from.subtract(p.position()).multiply(1, 0, 1);
        Vec3 look = p.getViewVector(1).multiply(1, 0, 1);
        return to.lengthSqr() < 1e-4 || to.normalize().dot(look.normalize()) > 0.1;
    }

    public static void onAttack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        State s = STATES.get(p.getUUID());
        if (s != null && s.dashTicks > 0) {
            e.setCanceled(true);
            return;
        }
        if (!guarding(p, e.getSource()) || p.getTicksUsingItem() > PARRY_WINDOW) return;
        // karşı koyma: darbe tamamen savuşturulur, saldıran sersemler, sıradaki vuruş dev alev hilali olur
        e.setCanceled(true);
        ServerLevel l = p.serverLevel();
        state(p).counterUntil = l.getGameTime() + 100;
        Entity atk = e.getSource().getEntity();
        if (atk instanceof LivingEntity le && atk != p) {
            Vec3 d = le.position().subtract(p.position()).normalize();
            le.knockback(1.3, -d.x, -d.z);
            le.setSecondsOnFire(4);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        }
        if (e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile pr) pr.discard();
        Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.9));
        l.sendParticles(ParticleTypes.FLAME, at.x, at.y - 0.2, at.z, 40, 0.3, 0.3, 0.3, 0.2);
        l.sendParticles(ParticleTypes.CRIT, at.x, at.y - 0.2, at.z, 20, 0.2, 0.2, 0.2, 0.5);
        l.playSound(null, p.blockPosition(), MythBlades.SFX.get("ember_parry").get(), SoundSource.PLAYERS, 1.3F, 1F);
        FxNet.fx(p, new FxNet.Fx(FxNet.PARRY, p.getId(), at.x, at.y - 0.2, at.z, 0, 0));
        p.stopUsingItem();
    }

    public static void onHurt(LivingHurtEvent e) {
        // savunmada önden gelen hasarın dörtte üçü emilir
        if (e.getEntity() instanceof ServerPlayer p && guarding(p, e.getSource())) {
            e.setAmount(e.getAmount() * 0.25F);
            ServerLevel l = p.serverLevel();
            Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.9));
            l.sendParticles(ParticleTypes.FLAME, at.x, at.y - 0.2, at.z, 12, 0.25, 0.25, 0.25, 0.08);
            l.playSound(null, p.blockPosition(), MythBlades.SFX.get("ember_block").get(), SoundSource.PLAYERS, 1F, 0.9F + p.getRandom().nextFloat() * 0.2F);
            FxNet.fx(p, new FxNet.Fx(FxNet.BLOCK_HIT, p.getId(), at.x, at.y - 0.2, at.z, 0, 0));
            if (e.getSource().getEntity() instanceof LivingEntity le && le != p) {
                Vec3 d = le.position().subtract(p.position()).normalize();
                le.knockback(0.5, -d.x, -d.z);
            }
            p.getUseItem().hurtAndBreak(1, p, x -> x.broadcastBreakEvent(p.getUsedItemHand()));
            return;
        }
        // kombonun son vuruşu ağır gelir
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getSource().getDirectEntity() == p && holds(p)) {
            State s = STATES.get(p.getUUID());
            if (s != null && s.stage == 2) {
                e.setAmount(e.getAmount() * 1.35F);
                Vec3 d = e.getEntity().position().subtract(p.position()).normalize();
                e.getEntity().knockback(0.8, -d.x, -d.z);
            }
        }
    }

    public static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            State s = STATES.get(p.getUUID());
            if (s != null && p.level().getGameTime() < s.safeFallUntil) e.setDamageMultiplier(0);
        }
    }
}
