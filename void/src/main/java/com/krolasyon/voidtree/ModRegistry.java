package com.krolasyon.voidtree;

import com.krolasyon.voidtree.entity.RiftOrbEntity;
import com.krolasyon.voidtree.entity.ShadowCloneEntity;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VoidTree.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VoidTree.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, VoidTree.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, VoidTree.MODID);

    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("void_grimoire",
            () -> new GrimoireItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<EntityType<RiftOrbEntity>> RIFT_ORB = ENTITIES.register("rift_orb",
            () -> EntityType.Builder.<RiftOrbEntity>of(RiftOrbEntity::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("rift_orb"));
    public static final RegistryObject<EntityType<ShadowCloneEntity>> SHADOW_CLONE = ENTITIES.register("shadow_clone",
            () -> EntityType.Builder.<ShadowCloneEntity>of(ShadowCloneEntity::new, MobCategory.MISC).sized(0.6F, 1.8F)
                    .clientTrackingRange(10).fireImmune().build("shadow_clone"));

    public static final RegistryObject<SimpleParticleType> WISP = PARTICLES.register("wisp", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> STAR = PARTICLES.register("star", () -> new SimpleParticleType(true));

    public static final RegistryObject<SoundEvent> WHOOSH = sound("whoosh");
    public static final RegistryObject<SoundEvent> PHANTOM = sound("phantom");
    public static final RegistryObject<SoundEvent> VORTEX = sound("vortex");
    public static final RegistryObject<SoundEvent> SLASH = sound("slash");
    public static final RegistryObject<SoundEvent> PRISON = sound("prison");
    public static final RegistryObject<SoundEvent> BLADE = sound("blade");
    public static final RegistryObject<SoundEvent> HOLE = sound("hole");
    public static final RegistryObject<SoundEvent> IMPLODE = sound("implode");
    public static final RegistryObject<SoundEvent> RIFT = sound("rift");
    public static final RegistryObject<SoundEvent> ASCEND = sound("ascend");
    public static final RegistryObject<SoundEvent> SEAL = sound("seal");
    public static final RegistryObject<SoundEvent> SUMMON = sound("summon");
    public static final RegistryObject<SoundEvent> COSMIC = sound("cosmic");
    public static final RegistryObject<SoundEvent> UNLOCK = sound("unlock");
    public static final RegistryObject<SoundEvent> EVADE = sound("evade");

    public static final ResourceKey<DamageType> VOID_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(VoidTree.MODID, "void"));

    private ModRegistry() {}

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VoidTree.MODID, name)));
    }

    /** void damage credited to the caster (kills give xp/loot as player kills) */
    public static DamageSource voidDamage(Entity direct, Entity caster) {
        return new DamageSource(caster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(VOID_DAMAGE), direct, caster);
    }
}
