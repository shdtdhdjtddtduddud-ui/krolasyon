package com.krolasyon.bosses.realm.story;

import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.net.RealmNet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/** Mana, kingdom blessings and the Crimson Dash. */
public final class PlayerPowers {
    private PlayerPowers() {}

    private static final UUID LEGION_ARMOR = UUID.fromString("6e1f3a52-8a4d-4d8e-9a51-0c6a3b7f1a01");
    private static final UUID LEGION_ATTACK = UUID.fromString("6e1f3a52-8a4d-4d8e-9a51-0c6a3b7f1a02");
    private static final UUID RULER_HEALTH = UUID.fromString("6e1f3a52-8a4d-4d8e-9a51-0c6a3b7f1a03");

    /** once per second */
    public static void tick(ServerPlayer p) {
        RealmData d = RealmData.get(p);
        float before = d.mana;
        float regen = 1.2F;
        if (d.bound(Faction.SOUL)) regen *= 1.6F;
        if (d.ruler) regen *= 1.5F;
        d.mana = Math.min(RealmData.MAX_MANA, d.mana + regen);

        if (d.bound(Faction.ASH)) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 260, 0, true, false, true));
        if (d.bound(Faction.SHADOW) && Realm.inRealm(p.level())) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
        modifier(p, Attributes.ARMOR, LEGION_ARMOR, "Legion blessing", 4.0, d.bound(Faction.LEGION));
        modifier(p, Attributes.ATTACK_DAMAGE, LEGION_ATTACK, "Legion blessing", 2.0, d.bound(Faction.LEGION));
        modifier(p, Attributes.MAX_HEALTH, RULER_HEALTH, "Sovereign", 10.0, d.ruler);
        long now = p.level().getGameTime();
        if (d.bound(Faction.SOUL) && p.getHealth() < p.getMaxHealth() * 0.3F && now - d.lastSoulRegen > 1200) {
            d.lastSoulRegen = now;
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            p.level().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 1.0F);
        }
        d.save(p);
        if (d.mana != before || p.tickCount % 200 == 0) RealmNet.sync(p);
    }

    private static void modifier(ServerPlayer p, Attribute attr, UUID id, String name, double amount, boolean on) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (on && !has) inst.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        else if (!on && has) inst.removeModifier(id);
    }

    public static boolean useMana(ServerPlayer p, float cost) {
        if (p.getAbilities().instabuild) return true;
        RealmData d = RealmData.get(p);
        if (d.mana < cost) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.mana_low").withStyle(ChatFormatting.BLUE), true);
            p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
            return false;
        }
        d.mana -= cost;
        d.save(p);
        RealmNet.sync(p);
        return true;
    }

    public static void addMana(ServerPlayer p, float amount) {
        RealmData d = RealmData.get(p);
        d.mana = Math.min(RealmData.MAX_MANA, d.mana + amount);
        d.save(p);
        RealmNet.sync(p);
    }

    private static final DustParticleOptions DASH = new DustParticleOptions(new Vector3f(1F, 0.2F, 0.15F), 1.6F);

    public static void dash(ServerPlayer p) {
        RealmData d = RealmData.get(p);
        if (d.chapter < RealmData.CH_FIREBORN) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.dash_locked").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = p.level().getGameTime();
        if (now - d.lastDash < 30 || p.isPassenger()) return;
        if (!useMana(p, 18F)) return;
        d = RealmData.get(p);
        d.lastDash = now;
        d.save(p);
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0E-4) dir = Vec3.directionFromRotation(0, p.getYRot());
        dir = dir.normalize();
        Vec3 start = p.position();
        p.setDeltaMovement(dir.scale(1.9).add(0, 0.25, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.level().playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.4F);
        var sl = p.serverLevel();
        for (int i = 0; i < 12; i++) {
            Vec3 q = start.add(dir.scale(i * 0.6));
            sl.sendParticles(DASH, q.x, q.y + 0.9, q.z, 3, 0.2, 0.4, 0.2, 0);
            sl.sendParticles(ParticleTypes.FLAME, q.x, q.y + 0.4, q.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        AABB path = new AABB(start, start.add(dir.scale(7))).inflate(1.2, 1.5, 1.2);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, path, e -> Allegiance.playerMayHit(p, e))) {
            e.hurt(p.damageSources().playerAttack(p), 5.0F);
            e.setSecondsOnFire(3);
        }
    }
}
