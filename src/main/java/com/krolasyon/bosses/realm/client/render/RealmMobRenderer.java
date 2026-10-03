package com.krolasyon.bosses.realm.client.render;

import com.krolasyon.bosses.client.render.GlowLayer;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.client.gen.GenModels;
import com.krolasyon.bosses.realm.entity.AnimState;
import com.krolasyon.bosses.realm.entity.RealmMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class RealmMobRenderer<T extends Mob & AnimState.Holder> extends MobRenderer<T, RealmMobModel<T>> {
    private final ResourceLocation texture;
    private final GenModels.Info info;

    public RealmMobRenderer(EntityRendererProvider.Context ctx, GenModels.Info info) {
        super(ctx, new RealmMobModel<>(ctx.bakeLayer(layer(info.id())), info), info.shadow());
        this.info = info;
        this.texture = Realm.rl("textures/entity/realm/" + info.id() + ".png");
        this.addLayer(new GlowLayer<>(this, Realm.rl("textures/entity/realm/" + info.id() + "_glow.png")));
    }

    public static ModelLayerLocation layer(String id) { return new ModelLayerLocation(Realm.rl(id), "main"); }

    @Override
    public ResourceLocation getTextureLocation(T e) { return texture; }

    @Override
    protected void scale(T e, PoseStack pose, float partial) {
        float s = info.scale();
        if (e instanceof RealmMob rm && rm.isAlly()) s *= 0.97F;
        pose.scale(s, s, s);
    }

    @Override
    protected void setupRotations(T e, PoseStack pose, float ageInTicks, float yaw, float partial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
    }

    @Override
    protected float getFlipDegrees(T e) { return 0F; }
}
