package com.sololeveling;

import com.sololeveling.entity.GateEntity;
import com.sololeveling.entity.NpcEntity;
import com.sololeveling.entity.SLMonster;
import com.sololeveling.entity.ShadowEntity;
import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.registry.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SoloLeveling.MODID)
public class SoloLeveling {
    public static final String MODID = "sololeveling";

    public SoloLeveling() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModTabs.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModFeatures.FEATURES.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::common);
        if (Boolean.getBoolean("sololeveling.clienttest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.sololeveling.client.ClientAutoTest::init);
        }
    }

    private void common(FMLCommonSetupEvent e) {
        e.enqueueWork(Net::init);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        for (Content.MonsterDef d : Content.MONSTERS) e.put(ModEntities.MONSTERS.get(d.id()).get(), SLMonster.attributes(d).build());
        for (Content.ShadowDef d : Content.SHADOWS) e.put(ModEntities.SHADOWS.get(d.id()).get(), ShadowEntity.attributes(d).build());
        for (String r : Content.NPC_ROLES) e.put(ModEntities.NPCS.get(r).get(), NpcEntity.attributes().build());
    }
}
