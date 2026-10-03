package com.krolasyon.bosses.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.CrimsonHoundModel;
import com.krolasyon.bosses.client.model.ModelLayers;
import com.krolasyon.bosses.client.model.SealWardenModel;
import com.krolasyon.bosses.client.model.SimpleEffectModel;
import com.krolasyon.bosses.client.render.*;
import com.krolasyon.bosses.client.model.MobModel;
import com.krolasyon.bosses.client.model.MobModelLoader;
import com.krolasyon.bosses.entity.mob.MobSpec;
import com.krolasyon.bosses.entity.mob.MobSpecs;
import com.krolasyon.bosses.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.EXPERIENCE_BAR.id(), "mana", (gui, g, pt, w, h) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            float mana = ClientHooks.mana(), max = ClientHooks.maxMana();
            var held = mc.player.getMainHandItem().getItem();
            boolean tool = held instanceof com.krolasyon.bosses.item.AbilitySword || held instanceof com.krolasyon.bosses.item.SpellTome;
            if (!tool && mana >= max - 0.5F) return;
            int bw = 81, x = w / 2 - 91, y = h - 32 - 7;
            g.fill(x - 1, y - 1, x + bw + 1, y + 4, 0xC0101018);
            g.fill(x, y, x + (int) (bw * Math.min(1F, mana / max)), y + 3, 0xFF3AA0FF);
            g.fill(x, y, x + (int) (bw * Math.min(1F, mana / max)), y + 1, 0xFF9AD8FF);
        });
    }

    @SubscribeEvent
    public static void registerEffects(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event) {
        event.register(new net.minecraft.resources.ResourceLocation(KrolasyonBosses.MODID, "azrakor"), new AzrakorEffects());
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SealWardenModel.LAYER, ModelLayers::createSealWarden);
        event.registerLayerDefinition(CrimsonHoundModel.LAYER, ModelLayers::createCrimsonHound);
        event.registerLayerDefinition(SimpleEffectModel.CRYSTAL_SPIKE, ModelLayers::createCrystalSpike);
        event.registerLayerDefinition(SimpleEffectModel.FIRE_PILLAR, ModelLayers::createFirePillar);
        event.registerLayerDefinition(SimpleEffectModel.WATCHER, ModelLayers::createWatcher);
        event.registerLayerDefinition(com.krolasyon.bosses.client.model.RevengeModel.LAYER, ModelLayers::createRevenge);
        event.registerLayerDefinition(com.krolasyon.bosses.client.model.HeartDemonModel.LAYER, ModelLayers::createHeartDemon);
        event.registerLayerDefinition(SimpleEffectModel.BLOOD_SPIKE, ModelLayers::createBloodSpike);
        event.registerLayerDefinition(SimpleEffectModel.THORN_SPIKE, ModelLayers::createThornSpike);
        event.registerLayerDefinition(SimpleEffectModel.BLOOD_LANCE, ModelLayers::createBloodLance);
        for (MobSpec s : MobSpecs.ALL.values()) event.registerLayerDefinition(MobModel.layer(s.id), () -> MobModelLoader.layer(s.id));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAL_WARDEN.get(), SealWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_HOUND.get(), CrimsonHoundRenderer::new);
        event.registerEntityRenderer(ModEntities.ERUPTION.get(), EruptionRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAL_PRISON.get(), SealPrisonRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.SEAL_BOLT.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.HELLFIRE_BALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.REVENGE.get(), RevengeRenderer::new);
        event.registerEntityRenderer(ModEntities.HEART_DEMON.get(), HeartDemonRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_LANCE.get(), BloodLanceRenderer::new);
        event.registerEntityRenderer(ModEntities.HEART_ORB.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.BOLT.get(), NoopRenderer::new);
        for (MobSpec s : MobSpecs.ALL.values()) event.registerEntityRenderer(ModEntities.MOBS.get(s.id).get(), ctx -> new HellMobRenderer(ctx, s));
    }
}
