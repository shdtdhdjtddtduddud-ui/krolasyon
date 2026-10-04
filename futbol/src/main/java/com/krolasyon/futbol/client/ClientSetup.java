package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = FutbolMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    private static Field modelField;

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) { event.enqueueWork(ClientState::loadPrefs); }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(JerseyLayer.WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.32F), false), 64, 64));
        event.registerLayerDefinition(JerseyLayer.SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.32F), true), 64, 64));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BALL.get(), BallRenderer::new);
        event.registerEntityRenderer(ModEntities.FOOTBALLER.get(), FootballerRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        for (String skin : event.getSkins()) {
            PlayerRenderer r = event.getSkin(skin);
            if (r == null) continue;
            boolean slim = "slim".equals(skin);
            setModel(r, new AnimatedPlayerModel<>(models.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim));
            r.addLayer(new JerseyLayer<>(r, models));
        }
    }

    private static void setModel(LivingEntityRenderer<?, ?> r, EntityModel<?> m) {
        try {
            if (modelField == null) {
                for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
                    if (EntityModel.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        modelField = f;
                        break;
                    }
                }
            }
            if (modelField != null) modelField.set(r, m);
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("[futbol] could not install animated player model", e);
        }
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        for (var k : Keys.ALL) event.register(k);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) { event.registerAboveAll("futbol_hud", Hud::render); }
}
