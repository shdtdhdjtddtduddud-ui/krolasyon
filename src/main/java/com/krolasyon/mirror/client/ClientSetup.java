package com.krolasyon.mirror.client;

import com.krolasyon.mirror.MirrorWorld;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientSetup {
    @Mod.EventBusSubscriber(modid = MirrorWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(MirrorWorld.MIRROR_BE.get(), MirrorRenderer::new);
            event.registerEntityRenderer(MirrorWorld.REFLECTION.get(), ReflectionRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = MirrorWorld.MODID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (event.phase == TickEvent.Phase.END && mc.player != null && !mc.isPaused()) PoseHistory.record(mc.player);
        }
    }

    private ClientSetup() {}
}
