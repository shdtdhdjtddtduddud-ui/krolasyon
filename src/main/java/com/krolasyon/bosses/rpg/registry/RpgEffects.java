package com.krolasyon.bosses.rpg.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RpgEffects {
    private RpgEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, KrolasyonBosses.MODID);

    /** Kanama: damage every second, ignores armour */
    public static final RegistryObject<MobEffect> BLEED = EFFECTS.register("bleed", () -> new RpgEffect(MobEffectCategory.HARMFUL, 0xA0101A) {
        @Override
        public void applyEffectTick(LivingEntity e, int amp) {
            e.hurt(e.damageSources().magic(), 1.0F + amp);
            if (e.level() instanceof net.minecraft.server.level.ServerLevel sl)
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amp) { return duration % 20 == 0; }
    });

    /** Sersemleme: cannot move */
    public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun", () -> new RpgEffect(MobEffectCategory.HARMFUL, 0xF0E060)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6f0a9a7c-3e57-4c51-9a2e-6b1c0d6a5e01", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "6f0a9a7c-3e57-4c51-9a2e-6b1c0d6a5e02", -0.8D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** Mana Akışı: faster mana regeneration (handled by the player data tick) */
    public static final RegistryObject<MobEffect> MANA_FLOW = EFFECTS.register("mana_flow", () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x40A0FF));

    /** İlahi Kalkan: absorbs hits, handled in the damage event */
    public static final RegistryObject<MobEffect> DIVINE_SHIELD = EFFECTS.register("divine_shield", () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xFFF0A0));

    /** Kan Antlaşması: life steal on every hit */
    public static final RegistryObject<MobEffect> BLOOD_PACT = EFFECTS.register("blood_pact", () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0xC01020));

    /** Statik Alan / Diken: damages attackers */
    public static final RegistryObject<MobEffect> THORN_AURA = EFFECTS.register("thorn_aura", () -> new RpgEffect(MobEffectCategory.BENEFICIAL, 0x60C040));

    public static class RpgEffect extends MobEffect {
        public RpgEffect(MobEffectCategory cat, int color) { super(cat, color); }
    }
}
