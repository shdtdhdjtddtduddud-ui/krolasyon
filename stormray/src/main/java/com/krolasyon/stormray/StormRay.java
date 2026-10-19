package com.krolasyon.stormray;

import com.krolasyon.stormray.entity.StormRayEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(StormRay.MODID)
public final class StormRay {
    public static final String MODID = "stormray";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<StormRayEntity>> STORM_RAY = ENTITIES.register("storm_ray",
            () -> EntityType.Builder.of(StormRayEntity::new, MobCategory.CREATURE).sized(2.4F, 0.8F).clientTrackingRange(10).build("storm_ray"));
    public static final RegistryObject<Item> SPAWN_EGG = ITEMS.register("storm_ray_spawn_egg",
            () -> new ForgeSpawnEggItem(STORM_RAY, 0x0B2235, 0x3AD7E8, new Item.Properties()));

    public StormRay() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacement);
        bus.addListener(this::creativeTabs);
        MinecraftForge.EVENT_BUS.addListener(StormRay::skySpawner);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.stormray.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("stormray.autotest")) com.krolasyon.stormray.test.AutoTest.init();
        }
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(STORM_RAY.get(), StormRayEntity.createAttributes().build());
    }

    private void spawnPlacement(SpawnPlacementRegisterEvent event) {
        event.register(STORM_RAY.get(), SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                StormRayEntity::checkSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPAWN_EGG);
    }

    /** Okyanus üzerindeki oyuncuların çevresinde, gökyüzünde ara sıra vatoz belirir (fırtınada daha sık). */
    private static void skySpawner(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 600 != 0) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !level.getBiome(p.blockPosition()).is(BiomeTags.IS_OCEAN)) continue;
            float chance = level.isThundering() ? 0.65F : level.isRaining() ? 0.4F : 0.2F;
            if (level.random.nextFloat() > chance) continue;
            if (level.getEntitiesOfClass(StormRayEntity.class, p.getBoundingBox().inflate(96)).size() >= 3) continue;
            double ang = level.random.nextDouble() * Math.PI * 2, dist = 36 + level.random.nextDouble() * 28;
            int x = (int) (p.getX() + Math.cos(ang) * dist), z = (int) (p.getZ() + Math.sin(ang) * dist);
            int y = Math.max(level.getSeaLevel(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)) + 14 + level.random.nextInt(12);
            StormRayEntity ray = STORM_RAY.get().create(level);
            if (ray == null) continue;
            ray.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0);
            ray.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(ray);
        }
    }
}
