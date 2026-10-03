package com.krolasyon.bosses;

import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import com.krolasyon.bosses.entity.SealWardenEntity;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModItems;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KrolasyonBosses.MODID)
public class KrolasyonBosses {
    public static final String MODID = "krolasyonbosses";

    public KrolasyonBosses() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        com.krolasyon.bosses.realm.registry.RealmBlocks.BLOCKS.register(bus);
        com.krolasyon.bosses.realm.registry.RealmItems.ITEMS.register(bus);
        com.krolasyon.bosses.realm.registry.RealmItems.TABS.register(bus);
        com.krolasyon.bosses.realm.world.RealmFeatures.FEATURES.register(bus);
        com.krolasyon.bosses.realm.entity.RealmEntities.ENTITIES.register(bus);
        bus.addListener(com.krolasyon.bosses.realm.entity.RealmEntities::onAttributes);
        bus.addListener(com.krolasyon.bosses.realm.entity.RealmEntities::onSpawnPlacements);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(com.krolasyon.bosses.realm.net.RealmNet::register));
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.krolasyon.bosses.realm.RealmConfig.SPEC, "krolasyon-realm.toml");
        bus.addListener(this::onAttributes);
        if (Boolean.getBoolean("krolasyon.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.bosses.client.AutoTest::init);
        }
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SEAL_WARDEN.get(), SealWardenEntity.createAttributes().build());
        event.put(ModEntities.CRIMSON_HOUND.get(), CrimsonHoundEntity.createAttributes().build());
        event.put(ModEntities.REVENGE.get(), com.krolasyon.bosses.entity.RevengeEntity.createAttributes().build());
        event.put(ModEntities.HEART_DEMON.get(), com.krolasyon.bosses.entity.HeartDemonEntity.createAttributes().build());
    }
}
