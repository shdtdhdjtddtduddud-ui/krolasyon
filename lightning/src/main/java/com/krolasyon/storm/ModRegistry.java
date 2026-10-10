package com.krolasyon.storm;

import com.krolasyon.storm.entity.BallLightningEntity;
import com.krolasyon.storm.entity.BoltProjectile;
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

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StormTree.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StormTree.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StormTree.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, StormTree.MODID);

    public static final RegistryObject<Item> STORM_TOME = ITEMS.register("storm_tome",
            () -> new StormTomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<EntityType<BoltProjectile>> BOLT = ENTITIES.register("lightning_bolt_projectile",
            () -> EntityType.Builder.<BoltProjectile>of(BoltProjectile::new, MobCategory.MISC).sized(0.4F, 0.4F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("lightning_bolt_projectile"));
    public static final RegistryObject<EntityType<BallLightningEntity>> BALL = ENTITIES.register("ball_lightning",
            () -> EntityType.Builder.<BallLightningEntity>of(BallLightningEntity::new, MobCategory.MISC).sized(0.9F, 0.9F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("ball_lightning"));

    public static final RegistryObject<SimpleParticleType> SPARK = PARTICLES.register("spark", () -> new SimpleParticleType(true));

    public static final Map<String, RegistryObject<SoundEvent>> SOUND_MAP = new LinkedHashMap<>();
    public static final RegistryObject<SoundEvent> ZAP = sound("zap");
    public static final RegistryObject<SoundEvent> ARC = sound("arc");
    public static final RegistryObject<SoundEvent> CHARGE = sound("charge");
    public static final RegistryObject<SoundEvent> NOVA = sound("nova");
    public static final RegistryObject<SoundEvent> DOME = sound("dome");
    public static final RegistryObject<SoundEvent> RING = sound("ring");
    public static final RegistryObject<SoundEvent> GRASP = sound("grasp");
    public static final RegistryObject<SoundEvent> BALL_HUM = sound("ball");
    public static final RegistryObject<SoundEvent> SPEAR = sound("spear");
    public static final RegistryObject<SoundEvent> UNLOCK = sound("unlock");
    public static final RegistryObject<SoundEvent> REAPER = sound("reaper");
    public static final RegistryObject<SoundEvent> BURST = sound("burst");

    public static final ResourceKey<DamageType> STORM_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(StormTree.MODID, "storm"));

    private ModRegistry() {}

    private static RegistryObject<SoundEvent> sound(String name) {
        RegistryObject<SoundEvent> o = SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StormTree.MODID, name)));
        SOUND_MAP.put(name, o);
        return o;
    }

    /** storm damage credited to the caster (kills give xp/loot as player kills) */
    public static DamageSource storm(Entity direct, Entity caster) {
        return new DamageSource(caster.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STORM_DAMAGE), direct, caster);
    }
}
