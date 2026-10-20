package com.krolasyon.advgear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Giyilen eşyaları çizer (oyuncu katmanı ve eldeki 3D eşya ortak kullanır). */
public final class GearRender {
    private static GearModels.Goggles goggles;
    private static GearModels.Backpack backpack;
    private static GearModels.Belt belt;

    private GearRender() {}

    public static void bake(EntityModelSet set) {
        goggles = new GearModels.Goggles(set.bakeLayer(GearModels.GOGGLES));
        backpack = new GearModels.Backpack(set.bakeLayer(GearModels.BACKPACK));
        belt = new GearModels.Belt(set.bakeLayer(GearModels.BELT));
    }

    private static void ensure() {
        if (goggles == null) bake(Minecraft.getInstance().getEntityModels());
    }

    private static ResourceLocation tex(String name, int v) {
        return new ResourceLocation("advgear", "textures/models/" + name + "_" + v + ".png");
    }

    public static void goggles(PoseStack ps, MultiBufferSource buf, int light, int variant) {
        ensure();
        ResourceLocation t = tex("goggles", variant);
        goggles.prepare(variant, false);
        goggles.root.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(t)), light, OverlayTexture.NO_OVERLAY);
        goggles.prepare(variant, true);
        // Mühendis ve Avcı camları karanlıkta hafifçe parlar
        goggles.root.render(ps, buf.getBuffer(RenderType.entityTranslucent(t)), light, OverlayTexture.NO_OVERLAY);
        if (variant != 0) goggles.root.render(ps, buf.getBuffer(RenderType.eyes(t)), light, OverlayTexture.NO_OVERLAY, 0.55F, 0.55F, 0.55F, 1F);
        goggles.prepare(variant, false);
    }

    public static void backpack(PoseStack ps, MultiBufferSource buf, int light, int variant, float sway) {
        ensure();
        backpack.prepare(variant, sway);
        backpack.root.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex("backpack", variant))), light, OverlayTexture.NO_OVERLAY);
    }

    public static void belt(PoseStack ps, MultiBufferSource buf, int light, int variant) {
        ensure();
        belt.prepare(variant);
        belt.root.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex("belt", variant))), light, OverlayTexture.NO_OVERLAY);
    }
}
