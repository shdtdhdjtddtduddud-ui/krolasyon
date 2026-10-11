package com.krolasyon.ben10;

import com.krolasyon.ben10.net.Net;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Ben10.MODID)
public final class Ben10 {
    public static final String MODID = "krolben10";
    public static final Logger LOG = LogUtils.getLogger();

    public Ben10() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        bus.addListener(this::setup);
        if (Boolean.getBoolean("krolben10.autotest")) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.ben10.client.AutoTest::init);
        }
    }

    private void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(Net::register);
    }
}
