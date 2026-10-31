package com.krolasyon.cille;

import com.krolasyon.cille.entity.KabkabaEntity;
import com.krolasyon.cille.registry.ModEntities;
import com.krolasyon.cille.registry.ModItems;
import com.krolasyon.cille.registry.ModSounds;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CilleMod.MODID)
public class CilleMod {
    public static final String MODID = "cille";

    public CilleMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::onAttributes);
        com.krolasyon.cille.network.CilleNetwork.register();
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.KABKABA.get(), KabkabaEntity.createAttributes().build());
        event.put(ModEntities.NIRUMEND.get(), com.krolasyon.cille.entity.NirumendEntity.createAttributes().build());
        event.put(ModEntities.SARKAN.get(), com.krolasyon.cille.entity.SarkanEntity.createAttributes().build());
        event.put(ModEntities.BEHMUD.get(), com.krolasyon.cille.entity.BehmudEntity.createAttributes().build());
    }
}
