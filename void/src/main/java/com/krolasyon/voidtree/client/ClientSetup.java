package com.krolasyon.voidtree.client;

import com.krolasyon.voidtree.ModRegistry;
import com.krolasyon.voidtree.VoidTree;
import com.krolasyon.voidtree.net.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static final String CATEGORY = "key.categories.voidtree";
    public static final KeyMapping OPEN_TREE = new KeyMapping("key.voidtree.tree", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping[] SLOT_KEYS = {
            new KeyMapping("key.voidtree.slot1", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY),
            new KeyMapping("key.voidtree.slot2", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY),
            new KeyMapping("key.voidtree.slot3", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY),
            new KeyMapping("key.voidtree.slot4", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY)};

    private ClientSetup() {}

    @Mod.EventBusSubscriber(modid = VoidTree.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(OPEN_TREE);
            for (KeyMapping k : SLOT_KEYS) e.register(k);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModRegistry.RIFT_ORB.get(), RiftOrbRenderer::new);
            e.registerEntityRenderer(ModRegistry.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
        }

        @SubscribeEvent
        public static void particles(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ModRegistry.WISP.get(), VoidParticle.WispProvider::new);
            e.registerSpriteSet(ModRegistry.STAR.get(), VoidParticle.StarProvider::new);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "void_hud", new HudOverlay());
        }
    }

    @Mod.EventBusSubscriber(modid = VoidTree.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            FxRenderer.tick();
            CastAnim.tick();
            ClientHooks.trauma = Math.max(0, ClientHooks.trauma - 0.045F);
            ClientHooks.flash *= 0.82F;
            if (mc.player == null) return;
            while (OPEN_TREE.consumeClick()) if (mc.screen == null) ClientHooks.openTree();
            for (int i = 0; i < SLOT_KEYS.length; i++) {
                while (SLOT_KEYS[i].consumeClick()) if (mc.screen == null) Net.CHANNEL.sendToServer(new Net.Cast(i));
            }
            // client side energy prediction between syncs
            ClientHooks.DATA.energy = Math.min(ClientHooks.DATA.maxEnergy(), ClientHooks.DATA.energy + ClientHooks.DATA.regenPerTick());
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
            ClientHooks.synced = false;
            ClientHooks.DATA.load(new net.minecraft.nbt.CompoundTag());
            ClientHooks.DATA.unlocked.clear();
            ClientHooks.shownEnergy = -1;
        }

        @SubscribeEvent
        public static void renderLevel(RenderLevelStageEvent e) { FxRenderer.render(e); }

        @SubscribeEvent
        public static void renderHand(RenderHandEvent e) {
            Minecraft mc = Minecraft.getInstance();
            if (e.getHand() == InteractionHand.MAIN_HAND && mc.player != null) CastAnim.applyFirstPerson(e.getPoseStack(), mc.player.getId());
        }

        @SubscribeEvent
        public static void camera(ViewportEvent.ComputeCameraAngles e) {
            float tr = ClientHooks.trauma;
            if (tr <= 0.001F || Minecraft.getInstance().level == null) return;
            float s = tr * tr;
            double t = Minecraft.getInstance().level.getGameTime() + e.getPartialTick();
            e.setYaw(e.getYaw() + s * 2.5F * Mth.sin((float) t * 1.9F));
            e.setPitch(e.getPitch() + s * 2.5F * Mth.sin((float) t * 2.3F + 1.3F));
            e.setRoll(e.getRoll() + s * 4.0F * Mth.sin((float) t * 1.7F + 2.1F));
        }
    }
}
