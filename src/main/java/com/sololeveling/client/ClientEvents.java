package com.sololeveling.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sololeveling.SoloLeveling;
import com.sololeveling.client.gui.Hud;
import com.sololeveling.client.gui.SystemScreen;
import com.sololeveling.client.model.GenModels;
import com.sololeveling.client.model.SLModel;
import com.sololeveling.client.render.*;
import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ClientEvents {
    private ClientEvents() {}

    public static final KeyMapping KEY_SYSTEM = new KeyMapping("key.sololeveling.system", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.sololeveling");
    public static final KeyMapping KEY_CAST = new KeyMapping("key.sololeveling.cast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.sololeveling");
    public static final KeyMapping KEY_NEXT = new KeyMapping("key.sololeveling.next_skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.sololeveling");
    public static final KeyMapping KEY_PREV = new KeyMapping("key.sololeveling.prev_skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.sololeveling");
    public static final KeyMapping KEY_ARISE = new KeyMapping("key.sololeveling.arise", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.sololeveling");

    public static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(SoloLeveling.MODID, name), "main");
    }

    @Mod.EventBusSubscriber(modid = SoloLeveling.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KEY_SYSTEM); e.register(KEY_CAST); e.register(KEY_NEXT); e.register(KEY_PREV); e.register(KEY_ARISE);
        }

        @SubscribeEvent
        public static void overlay(RegisterGuiOverlaysEvent e) {
            e.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "sololeveling_hud", Hud::render);
        }

        @SubscribeEvent
        public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
            for (String n : GenModels.NAMES) e.registerLayerDefinition(layer(n), () -> GenModels.create(n));
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            for (Content.MonsterDef d : Content.MONSTERS)
                e.registerEntityRenderer(ModEntities.MONSTERS.get(d.id()).get(), ctx -> new SLMobRenderer<>(ctx, d.model(), d.scale(), 0.4F + d.width() * 0.5F));
            for (Content.ShadowDef d : Content.SHADOWS)
                e.registerEntityRenderer(ModEntities.SHADOWS.get(d.id()).get(), ctx -> new SLMobRenderer<>(ctx, d.model(), d.scale(), 0.3F + d.width() * 0.4F));
            for (String r : Content.NPC_ROLES) e.registerEntityRenderer(ModEntities.NPCS.get(r).get(), NpcRenderer::new);
            e.registerEntityRenderer(ModEntities.GATE.get(), GateRenderer::new);
            e.registerEntityRenderer(ModEntities.MAGIC_BOLT.get(), MagicBoltRenderer::new);
        }
    }

    @Mod.EventBusSubscriber(modid = SoloLeveling.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            List<Content.SkillDef> unlocked = new ArrayList<>();
            for (Content.SkillDef s : Content.SKILLS) if (ClientHooks.data.hasSkill(s.id())) unlocked.add(s);
            while (KEY_SYSTEM.consumeClick()) if (mc.screen == null) mc.setScreen(new SystemScreen(0));
            if (mc.screen != null) return;
            while (KEY_NEXT.consumeClick()) if (!unlocked.isEmpty()) ClientHooks.selectedSkill = (ClientHooks.selectedSkill + 1) % unlocked.size();
            while (KEY_PREV.consumeClick()) if (!unlocked.isEmpty()) ClientHooks.selectedSkill = (ClientHooks.selectedSkill + unlocked.size() - 1) % unlocked.size();
            while (KEY_CAST.consumeClick()) {
                if (unlocked.isEmpty()) continue;
                int sel = Math.floorMod(ClientHooks.selectedSkill, unlocked.size());
                Net.toServer(new Packets.Cast(unlocked.get(sel).id()));
            }
            while (KEY_ARISE.consumeClick()) Net.toServer(new Packets.Cast("shadow_extraction"));
        }
    }
}
