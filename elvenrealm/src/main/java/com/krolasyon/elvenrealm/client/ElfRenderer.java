package com.krolasyon.elvenrealm.client;

import com.krolasyon.elvenrealm.entity.Activity;
import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.krolasyon.elvenrealm.entity.ElfKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ElfRenderer extends HumanoidMobRenderer<ElfEntity, ElfModel> {
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(new ResourceLocation("elvenrealm", "elf_slim"), "main");
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(new ResourceLocation("elvenrealm", "elf_wide"), "main");

    private final ElfKind kind;
    private final ResourceLocation texture;

    public ElfRenderer(EntityRendererProvider.Context ctx, ElfKind kind) {
        super(ctx, new ElfModel(ctx.bakeLayer(kind.slim ? SLIM : WIDE), kind), 0.5F);
        this.kind = kind;
        this.texture = new ResourceLocation("elvenrealm", "textures/entity/elf_" + kind.id + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(ElfEntity e) {
        return texture;
    }

    @Override
    protected void scale(ElfEntity e, PoseStack ps, float pt) {
        float s = 0.9375F * kind.scale;
        ps.scale(s, s, s);
    }

    @Override
    public void render(ElfEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Activity a = e.getActivity();
        float drop = switch (a) {
            case SIT, RECOVER -> 0.62F;
            case MEDITATE -> 0.66F;
            case PRAY -> 0.55F;
            case KNEEL -> 0.32F;
            default -> 0F;
        };
        ps.pushPose();
        if (drop > 0) ps.translate(0, -drop * kind.scale, 0);
        super.render(e, yaw, pt, ps, buf, light);
        ps.popPose();
    }
}
