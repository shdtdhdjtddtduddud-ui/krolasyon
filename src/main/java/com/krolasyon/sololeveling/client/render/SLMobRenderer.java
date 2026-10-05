package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.client.model.GenModels;
import com.krolasyon.sololeveling.client.model.SLModel;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** Renders every monster with its generated model, high resolution texture and an emissive glow layer. */
public class SLMobRenderer extends MobRenderer<SLMonster, SLModel> {
    private final MobKind kind;
    private final ResourceLocation tex;
    private final float scale;

    public static ModelLayerLocation layer(MobKind k) { return new ModelLayerLocation(SoloLeveling.id(k.id()), "main"); }

    public SLMobRenderer(EntityRendererProvider.Context ctx, MobKind k) {
        super(ctx, new SLModel(ctx.bakeLayer(layer(k)), k), GenModels.rig(k).shadow());
        this.kind = k;
        this.scale = GenModels.rig(k).scale();
        this.tex = SoloLeveling.id("textures/entity/" + k.id() + ".png");
        ResourceLocation glow = SoloLeveling.id("textures/entity/" + k.id() + "_glow.png");
        RenderType eyes = RenderType.eyes(glow);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() { return eyes; }
        });
    }

    @Override
    protected void scale(SLMonster e, PoseStack pose, float partial) {
        float s = scale * (e.isElite() ? 1.25F : 1F);
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(SLMonster e) { return tex; }

    @Override
    protected float getFlipDegrees(SLMonster e) { return kind == MobKind.KASAKA || kind == MobKind.GIANT_CENTIPEDE ? 180F : 90F; }

    public static String id(MobKind k) { return k.name().toLowerCase(Locale.ROOT); }
}
