package com.krolasyon.sololeveling.client;

import com.krolasyon.sololeveling.client.render.*;
import com.krolasyon.sololeveling.client.screen.StatusScreen;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    private static final String CAT = "key.categories.sololeveling";
    public static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_K);
    public static final KeyMapping MAP = key("map", GLFW.GLFW_KEY_M);
    public static final KeyMapping ARISE = key("arise", GLFW.GLFW_KEY_R);
    public static final KeyMapping SHADOWS = key("shadows", GLFW.GLFW_KEY_G);
    public static final KeyMapping NEWS = key("news", GLFW.GLFW_KEY_N);
    public static final KeyMapping[] SKILLS = {key("skill1", GLFW.GLFW_KEY_Z), key("skill2", GLFW.GLFW_KEY_X), key("skill3", GLFW.GLFW_KEY_C),
            key("skill4", GLFW.GLFW_KEY_V), key("skill5", GLFW.GLFW_KEY_B)};

    private ClientSetup() {}

    private static KeyMapping key(String id, int code) {
        return new KeyMapping("key.sololeveling." + id, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CAT);
    }

    public static void init(IEventBus bus) {
        bus.addListener(ClientSetup::renderers);
        bus.addListener(ClientSetup::layers);
        bus.addListener(ClientSetup::keys);
        bus.addListener(ClientSetup::overlays);
        bus.addListener(ClientSetup::setup);
        MinecraftForge.EVENT_BUS.register(new ClientSetup.Events());
    }

    private static void setup(FMLClientSetupEvent e) {
        if (Boolean.getBoolean("sololeveling.autotest")) AutoTest.init();
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (MobKind k : MobKind.values()) e.registerEntityRenderer(ModEntities.mob(k), ctx -> new SLMobRenderer(ctx, k));
        e.registerEntityRenderer(ModEntities.SHADOW.get(), ShadowRenderer::new);
        e.registerEntityRenderer(ModEntities.NPC.get(), NpcRenderer::new);
        e.registerEntityRenderer(ModEntities.GATE.get(), GateRenderer::new);
        e.registerEntityRenderer(ModEntities.PROJECTILE.get(), ProjectileRenderer::new);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (MobKind k : MobKind.values()) e.registerLayerDefinition(SLMobRenderer.layer(k), () -> com.krolasyon.sololeveling.client.model.GenModels.create(k));
        e.registerLayerDefinition(ArmorModels.MONARCH, ArmorModels::monarchLayer);
        e.registerLayerDefinition(ArmorModels.KNIGHT, ArmorModels::knightLayer);
        e.registerLayerDefinition(ArmorModels.ORC, ArmorModels::orcLayer);
        e.registerLayerDefinition(ArmorModels.HUNTER, ArmorModels::hunterLayer);
        e.registerLayerDefinition(ArmorModels.INNER, ArmorModels::innerLayer);
    }

    private static void keys(RegisterKeyMappingsEvent e) {
        e.register(STATUS);
        e.register(MAP);
        e.register(ARISE);
        e.register(SHADOWS);
        e.register(NEWS);
        for (KeyMapping k : SKILLS) e.register(k);
    }

    private static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("system_hud", Hud::render);
    }

    public static class Events {
        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (ClientHooks.ariseTicks > 0) ClientHooks.ariseTicks--;
            if (ClientHooks.regionTicks > 0) ClientHooks.regionTicks--;
            if (ClientHooks.shakeTicks > 0) ClientHooks.shakeTicks--;
            if (ClientHooks.tintTicks > 0) ClientHooks.tintTicks--;
            if (mc.player == null || mc.screen != null) return;
            while (STATUS.consumeClick()) {
                if (ClientHooks.DATA.awakened) mc.setScreen(new StatusScreen());
                else Net.toServer(new Net.Action("awaken_prompt", 0, 0, ""));
            }
            while (MAP.consumeClick()) Net.toServer(new Net.Action("map", 0, 0, ""));
            while (NEWS.consumeClick()) Net.toServer(new Net.Action("news", 0, 0, ""));
            while (ARISE.consumeClick()) Net.toServer(new Net.Action("arise", 0, 0, ""));
            while (SHADOWS.consumeClick()) Net.toServer(new Net.Action("shadows", 0, 0, ""));
            for (int i = 0; i < SKILLS.length; i++) while (SKILLS[i].consumeClick()) Net.toServer(new Net.Action("skill", i, 0, ""));
        }

        @SubscribeEvent
        public void camera(ViewportEvent.ComputeCameraAngles e) {
            if (ClientHooks.shakeTicks <= 0) return;
            float s = ClientHooks.shakeTicks * 0.12F;
            double t = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + e.getPartialTick();
            e.setPitch(e.getPitch() + (float) Math.sin(t * 2.7) * s);
            e.setYaw(e.getYaw() + (float) Math.cos(t * 3.1) * s);
        }
    }
}
