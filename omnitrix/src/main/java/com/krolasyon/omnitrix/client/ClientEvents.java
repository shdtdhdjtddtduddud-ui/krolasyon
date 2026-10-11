package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Alien;
import com.krolasyon.omnitrix.ModRegistry;
import com.krolasyon.omnitrix.Omnitrix;
import com.krolasyon.omnitrix.Transform;
import com.krolasyon.omnitrix.net.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
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
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final String CATEGORY = "key.categories.omnitrix";
    public static final KeyMapping WATCH = new KeyMapping("key.omnitrix.watch", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CATEGORY);
    public static final KeyMapping BLOCK = new KeyMapping("key.omnitrix.block", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping[] ABILITY = {
            new KeyMapping("key.omnitrix.ability1", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.omnitrix.ability2", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.omnitrix.ability3", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.omnitrix.ability4", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY)};

    static float trauma, flash;
    static int flashColor = 0x50FF50;
    private static boolean blockSent;
    /** local watch activation animation start (client time, ticks) */
    public static long watchAnim = -1000;

    private ClientEvents() {}

    public static void shake(float a) { trauma = Math.min(1F, trauma + a); }

    public static void flash(int color, float a) { flashColor = color; flash = Math.max(flash, a); }

    @Mod.EventBusSubscriber(modid = Omnitrix.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(WATCH);
            e.register(BLOCK);
            for (KeyMapping k : ABILITY) e.register(k);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModRegistry.PROJECTILE.get(), ProjectileRenderer::new);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers e) {
            for (String skin : e.getSkins()) {
                if (e.getSkin(skin) instanceof PlayerRenderer pr) pr.addLayer(new WatchLayer(pr));
            }
        }

        @SubscribeEvent
        public static void particles(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ModRegistry.FIRE.get(), s -> new OmniParticle.Provider(s, OmniParticle.Style.FIRE));
            e.registerSpriteSet(ModRegistry.SLIME.get(), s -> new OmniParticle.Provider(s, OmniParticle.Style.SLIME));
            e.registerSpriteSet(ModRegistry.FROST.get(), s -> new OmniParticle.Provider(s, OmniParticle.Style.FROST));
            e.registerSpriteSet(ModRegistry.ECTO.get(), s -> new OmniParticle.Provider(s, OmniParticle.Style.ECTO));
            e.registerSpriteSet(ModRegistry.OMNI.get(), s -> new OmniParticle.Provider(s, OmniParticle.Style.OMNI));
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "omnitrix_hud", new OmniHud());
        }
    }

    @Mod.EventBusSubscriber(modid = Omnitrix.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            FxRenderer.tick();
            trauma = Math.max(0, trauma - 0.04F);
            flash *= 0.85F;
            if (mc.player == null) return;
            boolean transformed = ClientState.SELF.transformed();
            while (WATCH.consumeClick()) {
                if (mc.screen != null) continue;
                if (!ClientState.SELF.wearing) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.omnitrix.not_wearing"), true);
                } else if (transformed) {
                    Net.CHANNEL.sendToServer(new Net.Action(Net.Action.REVERT, 0));
                } else {
                    Net.CHANNEL.sendToServer(new Net.Action(Net.Action.ACTIVATE, 0));
                    if (ClientState.SELF.recharge <= 0) {
                        watchAnim = mc.level.getGameTime();
                        mc.setScreen(new OmnitrixScreen());
                    }
                }
            }
            for (int i = 0; i < ABILITY.length; i++) {
                while (ABILITY[i].consumeClick()) if (mc.screen == null && transformed) Net.CHANNEL.sendToServer(new Net.Action(Net.Action.ABILITY, i));
            }
            boolean block = transformed && BLOCK.isDown() && mc.screen == null;
            if (block != blockSent) {
                blockSent = block;
                Net.CHANNEL.sendToServer(new Net.Action(Net.Action.BLOCK, block ? 1 : 0));
            }
            if (!ClientState.SELF.transformed() || ClientState.SELF.current() == null) return;
            // ambient client particles
            Alien a = ClientState.SELF.current();
            if (a == Alien.BIGCHILL && mc.player.tickCount % 4 == 0)
                mc.level.addParticle(ModRegistry.FROST.get(), mc.player.getX(), mc.player.getY() + 1.6, mc.player.getZ(), 0, -0.02, 0);
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { ClientState.clear(); }

        @SubscribeEvent
        public static void renderPlayer(RenderPlayerEvent.Pre e) {
            if (!(e.getEntity() instanceof AbstractClientPlayer pl)) return;
            ClientState.Look look = ClientState.look(pl);
            Alien a = look.alien();
            Minecraft mc = Minecraft.getInstance();
            if (a == null || mc.level == null) return;
            float since = mc.level.getGameTime() - look.transformTime + e.getPartialTick();
            if (since < 12) return; // the human is still visible inside the omnitrix flash
            e.setCanceled(true);
            AlienRenderer.render(pl, a, look, e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
        }

        @SubscribeEvent
        public static void nameTag(RenderNameTagEvent e) {
            if (e.getEntity() instanceof AbstractClientPlayer pl && ClientState.look(pl).alien() != null) {
                e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        }

        @SubscribeEvent
        public static void renderHand(RenderHandEvent e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.getCameraEntity() != mc.player) { e.setCanceled(true); return; }
            ClientState.Look look = ClientState.look(mc.player);
            Alien a = look.alien();
            if (a == null) return;
            if (look.puddle()) { e.setCanceled(true); return; }
            if (e.getHand() != InteractionHand.MAIN_HAND) { e.setCanceled(!e.getItemStack().isEmpty() ? false : true); return; }
            if (!e.getItemStack().isEmpty()) return;
            e.setCanceled(true);
            var ps = e.getPoseStack();
            ps.pushPose();
            float swing = e.getSwingProgress();
            float k = Mth.sin(Mth.sqrt(swing) * Mth.PI);
            float equip = e.getEquipProgress();
            ps.translate(0.42F + -0.1F * k, -0.55F - equip * 0.6F + 0.1F * k, -0.75F - 0.4F * k);
            float t = look.castProgress(e.getPartialTick());
            if (t >= 0) {
                float w = Mth.sin(t * Mth.PI);
                ps.translate(-0.1F * w, 0.1F * w, -0.2F * w);
            }
            AlienRenderer.renderArm(mc.player, a, look, e.getPartialTick(), ps, e.getMultiBufferSource(), e.getPackedLight());
            ps.popPose();
        }

        @SubscribeEvent
        public static void renderLevel(RenderLevelStageEvent e) { FxRenderer.render(e); }

        @SubscribeEvent
        public static void camera(ViewportEvent.ComputeCameraAngles e) {
            if (trauma <= 0.001F || Minecraft.getInstance().level == null) return;
            float s = trauma * trauma;
            double t = Minecraft.getInstance().level.getGameTime() + e.getPartialTick();
            e.setYaw(e.getYaw() + s * 2.5F * Mth.sin((float) t * 1.9F));
            e.setPitch(e.getPitch() + s * 2.5F * Mth.sin((float) t * 2.3F + 1.3F));
            e.setRoll(e.getRoll() + s * 4F * Mth.sin((float) t * 1.7F + 2.1F));
        }

        @SubscribeEvent
        public static void fov(ViewportEvent.ComputeFov e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            ClientState.Look look = ClientState.look(mc.player);
            float since = (float) (mc.level.getGameTime() - look.transformTime + e.getPartialTick());
            if (since >= 0 && since < Transform.TRANSFORM_ANIM) e.setFOV(e.getFOV() * (1 + 0.25 * Math.sin(since / Transform.TRANSFORM_ANIM * Math.PI)));
        }
    }
}
