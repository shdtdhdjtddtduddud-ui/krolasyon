package com.krolasyon.bosses;

import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.entity.SealWardenEntity;
import com.krolasyon.bosses.entity.mob.MobSpec;
import com.krolasyon.bosses.entity.mob.MobSpecs;
import com.krolasyon.bosses.net.Net;
import com.krolasyon.bosses.registry.ModBlocks;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModItems;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KrolasyonBosses.MODID)
public class KrolasyonBosses {
    public static final String MODID = "krolasyonbosses";

    public KrolasyonBosses() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        com.krolasyon.bosses.registry.ModFeatures.FEATURES.register(bus);
        ModItems.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::onAttributes);
        bus.addListener(this::onSpawnPlacements);
        bus.addListener(this::onCommonSetup);
        if (Boolean.getBoolean("krolasyon.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.bosses.client.AutoTest::init);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::init);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SEAL_WARDEN.get(), SealWardenEntity.createAttributes().build());
        event.put(ModEntities.CRIMSON_HOUND.get(), CrimsonHoundEntity.createAttributes().build());
        event.put(ModEntities.REVENGE.get(), com.krolasyon.bosses.entity.RevengeEntity.createAttributes().build());
        event.put(ModEntities.HEART_DEMON.get(), com.krolasyon.bosses.entity.HeartDemonEntity.createAttributes().build());
        for (MobSpec s : MobSpecs.ALL.values()) event.put(ModEntities.MOBS.get(s.id).get(), HellMob.createAttributes(s).build());
    }

    private void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        for (MobSpec s : MobSpecs.ALL.values()) {
            event.register(ModEntities.MOBS.get(s.id).get(), s.flying ? SpawnPlacements.Type.NO_RESTRICTIONS : SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HellMob::checkSpawn, SpawnPlacementRegisterEvent.Operation.OR);
        }
    }
}
