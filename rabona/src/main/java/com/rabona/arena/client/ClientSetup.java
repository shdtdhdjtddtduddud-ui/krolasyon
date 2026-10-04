package com.rabona.arena.client;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = RabonaArena.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    public static final ModelLayerLocation KIT = new ModelLayerLocation(RabonaArena.id("kit"), "main");
    public static final ModelLayerLocation KIT_SLIM = new ModelLayerLocation(RabonaArena.id("kit_slim"), "main");

    private ClientSetup() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        ClientState.load();
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.BALL.get(), BallRenderer::new);
        e.registerEntityRenderer(ModEntities.FOOTBALLER.get(), FootballerRenderer::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(KIT, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.32f), false), 64, 64));
        e.registerLayerDefinition(KIT_SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.32f), true), 64, 64));
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        for (KeyMapping k : Keys.ALL) e.register(k);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("hud", Hud::render);
    }

    /** Oyuncu modellerini animasyonlu modelle degistir, forma katmanini ekle. */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            LivingEntityRenderer r = e.getSkin(skin);
            if (!(r instanceof PlayerRenderer pr)) continue;
            boolean slim = "slim".equals(skin);
            AnimatedPlayerModel<Player> model = new AnimatedPlayerModel<>(
                    e.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
            if (!replaceModel(pr, model)) continue;
            pr.addLayer(new KitLayer(pr, new PlayerModel<>(e.getEntityModels().bakeLayer(slim ? KIT_SLIM : KIT), slim)));
        }
    }

    private static boolean replaceModel(LivingEntityRenderer<?, ?> r, EntityModel<?> model) {
        try {
            for (Field f : LivingEntityRenderer.class.getDeclaredFields()) {
                if (EntityModel.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(r, model);
                    return true;
                }
            }
        } catch (Exception ex) {
            RabonaArena.LOG.error("Oyuncu modeli degistirilemedi", ex);
        }
        return false;
    }
}
