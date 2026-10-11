package com.krolasyon.omnitrix;

import com.krolasyon.omnitrix.entity.AlienProjectile;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Omnitrix.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Omnitrix.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Omnitrix.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Omnitrix.MODID);

    public static final RegistryObject<Item> OMNITRIX = ITEMS.register("omnitrix",
            () -> new OmnitrixItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<EntityType<AlienProjectile>> PROJECTILE = ENTITIES.register("alien_projectile",
            () -> EntityType.Builder.<AlienProjectile>of(AlienProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("alien_projectile"));

    public static final RegistryObject<SimpleParticleType> FIRE = PARTICLES.register("fire", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SLIME = PARTICLES.register("slime", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> FROST = PARTICLES.register("frost", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> ECTO = PARTICLES.register("ecto", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> OMNI = PARTICLES.register("omni", () -> new SimpleParticleType(true));

    public static final Map<String, RegistryObject<SoundEvent>> SOUND_MAP = new LinkedHashMap<>();
    public static final RegistryObject<SoundEvent> ACTIVATE = sound("activate"), DIAL = sound("dial"), SELECT = sound("select"),
            TRANSFORM = sound("transform"), REVERT = sound("revert"), TIMEOUT = sound("timeout"), BEEP = sound("beep"),
            FIRE_SHOT = sound("fire_shot"), FLAME = sound("flame"), SUPERNOVA = sound("supernova"), JET = sound("jet"),
            WIND = sound("wind"), TORNADO = sound("tornado"), CRYSTAL = sound("crystal"), SHATTER = sound("shatter"),
            SLIME_SHOT = sound("slime_shot"), SPLAT = sound("splat"), ZOOM = sound("zoom"), CLAP = sound("clap"),
            SLAM = sound("slam"), PUNCH = sound("punch"), FREEZE = sound("freeze"), PHASE = sound("phase"),
            SONIC = sound("sonic"), SCREAM = sound("scream"), GHOST = sound("ghost"), TERROR = sound("terror"), BLOCK = sound("block");

    public static final ResourceKey<DamageType> ALIEN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Omnitrix.MODID, "alien"));

    private ModRegistry() {}

    private static RegistryObject<SoundEvent> sound(String name) {
        RegistryObject<SoundEvent> o = SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Omnitrix.MODID, name)));
        SOUND_MAP.put(name, o);
        return o;
    }

    public static DamageSource alienDamage(Entity direct, Entity attacker) {
        return new DamageSource(attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ALIEN_DAMAGE), direct, attacker);
    }
}
