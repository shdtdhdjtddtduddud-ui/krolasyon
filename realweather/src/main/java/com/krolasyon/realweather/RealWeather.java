package com.krolasyon.realweather;

import com.krolasyon.realweather.item.BarometerItem;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Krolasyon Gerçekçi Hava: hava cepheleri, biyoma göre yağmur/kar/tipi/kum fırtınası, sis, rüzgâr, sesler ve ortam etkileri. */
@Mod(RealWeather.MODID)
public final class RealWeather {
    public static final String MODID = "realweather";

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final Map<String, RegistryObject<SoundEvent>> SOUND = new LinkedHashMap<>();

    static {
        for (String s : new String[]{"rain_heavy", "rain_light", "rain_indoor", "rain_leaves", "wind_soft", "wind_strong", "blizzard", "sandstorm",
                "leaves", "crickets", "thunder_near", "thunder_far"}) {
            SOUND.put(s, SOUNDS.register(s, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, s))));
        }
    }

    public static final RegistryObject<SimpleParticleType> SNOWFLAKE = particle("snowflake");
    public static final RegistryObject<SimpleParticleType> LEAF = particle("leaf");
    public static final RegistryObject<SimpleParticleType> SAND = particle("sand");
    public static final RegistryObject<SimpleParticleType> FIREFLY = particle("firefly");
    public static final RegistryObject<SimpleParticleType> RIPPLE = particle("ripple");
    public static final RegistryObject<SimpleParticleType> MIST = particle("mist");
    public static final RegistryObject<SimpleParticleType> BREATH = particle("breath");
    public static final RegistryObject<SimpleParticleType> SPLASH = particle("splash");

    public static final RegistryObject<Item> BAROMETER = ITEMS.register("barometer", BarometerItem::new);

    private static RegistryObject<SimpleParticleType> particle(String id) {
        return PARTICLES.register(id, () -> new SimpleParticleType(false));
    }

    public static SoundEvent sound(String id) {
        return SOUND.get(id).get();
    }

    public RealWeather() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(bus);
        PARTICLES.register(bus);
        ITEMS.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Net::register));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(BAROMETER);
        });
        MinecraftForge.EVENT_BUS.addListener(WeatherServer::onLevelTick);
        MinecraftForge.EVENT_BUS.addListener(WeatherServer::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(WeatherServer::onLogin);
        MinecraftForge.EVENT_BUS.addListener(WeatherServer::commands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.realweather.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("realweather.autotest")) com.krolasyon.realweather.test.AutoTest.init();
        }
    }
}
