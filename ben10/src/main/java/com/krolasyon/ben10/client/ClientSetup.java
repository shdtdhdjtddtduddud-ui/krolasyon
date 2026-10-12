package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.client.model.AlienLayers;
import com.krolasyon.ben10.client.render.DebrisRenderer;
import com.krolasyon.ben10.client.render.FxRenderer;
import com.krolasyon.ben10.client.render.ProjectileRenderer;
import com.krolasyon.ben10.client.render.WristLayer;
import com.krolasyon.ben10.net.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class ClientSetup {
    public static final String CATEGORY = "key.categories.krolben10";
    public static final KeyMapping DIAL = new KeyMapping("key.krolben10.dial", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping GUARD = new KeyMapping("key.krolben10.guard", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping[] ABILITY = {
            new KeyMapping("key.krolben10.ability1", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.krolben10.ability2", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.krolben10.ability3", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.krolben10.ability4", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY)};
    public static final Map<String, ModelLayerLocation> LAYERS = new HashMap<>();
    private static boolean guardSent;

    private ClientSetup() {}

    public static ModelLayerLocation layer(String name) {
        return LAYERS.computeIfAbsent(name, n -> new ModelLayerLocation(new ResourceLocation(Ben10.MODID, n), "main"));
    }

    @Mod.EventBusSubscriber(modid = Ben10.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(DIAL);
            e.register(GUARD);
            for (KeyMapping k : ABILITY) e.register(k);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
            AlienLayers.all().forEach((name, def) -> e.registerLayerDefinition(layer(name), def));
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModRegistry.PROJECTILE.get(), ProjectileRenderer::new);
            e.registerEntityRenderer(ModRegistry.DEBRIS.get(), DebrisRenderer::new);
            e.registerEntityRenderer(ModRegistry.FX.get(), FxRenderer::new);
        }

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void addLayers(EntityRenderersEvent.AddLayers e) {
            for (String skin : e.getSkins()) {
                PlayerRenderer r = e.getSkin(skin);
                if (r != null) r.addLayer(new WristLayer(r));
            }
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("omnitrix_hud", new OmniHud());
        }
    }

    @Mod.EventBusSubscriber(modid = Ben10.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            ClientHooks.trauma = Math.max(0, ClientHooks.trauma - 0.04F);
            ClientHooks.flash *= 0.86F;
            if (mc.player == null) return;
            OmniData d = OmniData.get(mc.player);
            while (DIAL.consumeClick()) if (mc.screen == null) ClientHooks.openDial();
            for (int i = 0; i < ABILITY.length; i++) {
                while (ABILITY[i].consumeClick()) {
                    if (mc.screen == null && d.alien != null) Net.CHANNEL.sendToServer(new Net.Use(i));
                }
            }
            boolean g = GUARD.isDown() && mc.screen == null && d.alien != null;
            if (g != guardSent) {
                guardSent = g;
                Net.CHANNEL.sendToServer(new Net.Guard(g));
            }
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
            OmniData.clearClient();
            guardSent = false;
        }

        @SubscribeEvent
        public static void renderPlayer(RenderPlayerEvent.Pre e) {
            if (!(e.getEntity() instanceof AbstractClientPlayer p)) return;
            OmniData d = OmniData.get(p);
            if (d.alien == null) return;
            e.setCanceled(true);
            if (d.alien.has(com.krolasyon.ben10.Alien.Flag.FIRE_IMMUNE)) {
                // Heatblast is made of fire: never draw the vanilla "burning" overlay on him
                p.clearFire();
                p.setSharedFlagOnFire(false);
            }
            AlienRenderer.render(p, d, e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
        }

        @SubscribeEvent
        public static void renderArm(RenderArmEvent e) {
            OmniData d = OmniData.get(e.getPlayer());
            if (d.alien == null) return;
            e.setCanceled(true);
            AlienRenderer.renderArm(e.getPlayer(), d, e.getArm(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
        }

        @SubscribeEvent
        public static void screenFire(net.minecraftforge.client.event.RenderBlockScreenEffectEvent e) {
            if (e.getOverlayType() != net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType.FIRE) return;
            OmniData d = OmniData.get(e.getPlayer());
            if (d.alien != null && d.alien.has(com.krolasyon.ben10.Alien.Flag.FIRE_IMMUNE)) e.setCanceled(true);
        }

        @SubscribeEvent
        public static void camera(ViewportEvent.ComputeCameraAngles e) {
            float tr = ClientHooks.trauma;
            if (tr <= 0.001F || Minecraft.getInstance().level == null) return;
            float s = tr * tr;
            double t = Minecraft.getInstance().level.getGameTime() + e.getPartialTick();
            e.setYaw(e.getYaw() + s * 3F * Mth.sin((float) t * 1.9F));
            e.setPitch(e.getPitch() + s * 3F * Mth.sin((float) t * 2.3F + 1.3F));
            e.setRoll(e.getRoll() + s * 5F * Mth.sin((float) t * 1.7F + 2.1F));
        }

        @SubscribeEvent
        public static void fov(ComputeFovModifierEvent e) {
            OmniData d = OmniData.get(e.getPlayer());
            if (d.alien == com.krolasyon.ben10.Alien.XLR8) {
                // keep the speed FOV from going crazy with XLR8's huge movement speed
                e.setNewFovModifier(Math.min(e.getNewFovModifier(), d.has(OmniData.F_OVERDRIVE) ? 1.35F : 1.2F));
            } else if (d.alien != null) {
                e.setNewFovModifier(Math.min(e.getNewFovModifier(), 1.25F));
            }
        }
    }
}
