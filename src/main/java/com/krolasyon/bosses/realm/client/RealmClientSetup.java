package com.krolasyon.bosses.realm.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Realm;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RealmClientSetup {
    private RealmClientSetup() {}

    @SubscribeEvent
    public static void registerLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e) {
        for (com.krolasyon.bosses.realm.client.gen.GenModels.Info info : com.krolasyon.bosses.realm.client.gen.GenModels.ALL.values()) {
            e.registerLayerDefinition(com.krolasyon.bosses.realm.client.render.RealmMobRenderer.layer(info.id()), info.layer());
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers e) {
        for (com.krolasyon.bosses.realm.client.gen.GenModels.Info info : com.krolasyon.bosses.realm.client.gen.GenModels.ALL.values()) {
            net.minecraft.world.entity.EntityType type = com.krolasyon.bosses.realm.entity.RealmEntities.type(info.id());
            com.krolasyon.bosses.realm.entity.MobSpec spec = com.krolasyon.bosses.realm.entity.RealmEntities.spec(info.id());
            if (type == null || spec == null) continue;
            if (spec.boss()) e.registerEntityRenderer(type, ctx -> new com.krolasyon.bosses.realm.client.render.RealmBossRenderer(ctx, info));
            else e.registerEntityRenderer(type, ctx -> new com.krolasyon.bosses.realm.client.render.RealmMobRenderer(ctx, info));
        }
        e.registerEntityRenderer(com.krolasyon.bosses.realm.entity.RealmEntities.SPELL.get(), com.krolasyon.bosses.realm.client.render.SpellProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(Realm.rl("crimson_realm"), new RealmSky());
    }

    /** crimson sky: no clouds, the sky is the biome fog colour, a burning eclipse hangs overhead */
    public static class RealmSky extends DimensionSpecialEffects {
        public RealmSky() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) { return color; }

        @Override
        public boolean isFoggyAt(int x, int z) { return false; }

        @Override
        public float[] getSunriseColor(float time, float partial) { return null; }

        @Override
        public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.Camera camera, org.joml.Matrix4f projection, boolean foggy, Runnable setupFog) {
            RealmSkyRenderer.render(level, ticks, partialTick, pose, projection);
            return true;
        }
    }
}
