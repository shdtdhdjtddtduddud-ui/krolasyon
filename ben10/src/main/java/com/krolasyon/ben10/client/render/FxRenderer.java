package com.krolasyon.ben10.client.render;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.client.AlienRenderer;
import com.krolasyon.ben10.client.ClientSetup;
import com.krolasyon.ben10.client.model.AlienModel;
import com.krolasyon.ben10.entity.FxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class FxRenderer extends EntityRenderer<FxEntity> {
    private static ResourceLocation fx(String n) {
        return new ResourceLocation(Ben10.MODID, "textures/fx/" + n + ".png");
    }

    private static final ResourceLocation GLOW = fx("glow"), RING = fx("ring"), FLAME = fx("flame"), WIND = fx("wind"), BEAM = fx("beam"),
            GOO = fx("goo"), SONIC = fx("sonic"), SHOCK = fx("shock"), SMOKE = fx("smoke");
    private final AlienModel spike, iceSpike;

    public FxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        spike = new AlienModel(ctx.bakeLayer(ClientSetup.layer("crystal_spike")));
        iceSpike = new AlienModel(ctx.bakeLayer(ClientSetup.layer("ice_spike")));
        shadowRadius = 0;
    }

    @Override
    public boolean shouldRender(FxEntity e, Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(FxEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float life = Math.max(1, e.life());
        float t = Mth.clamp(age / life, 0, 1);
        float fade = 1 - t;
        int col = e.color();
        float size = e.size();
        ps.pushPose();
        switch (e.kind()) {
            case FxEntity.FLASH -> {
                float s = size * (0.3F + 0.9F * Mth.sqrt(t));
                Quads.billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), s, col, fade * fade, Quads.FULL);
                Quads.billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), s * 0.45F, 0xFFFFFF, fade, Quads.FULL);
            }
            case FxEntity.SPHERE -> {
                float s = size >= 0 ? size * (0.2F + 0.8F * Mth.sqrt(t)) : -size * (1 - t);
                float a = size >= 0 ? fade : t;
                Quads.billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), s * 1.3F, col, a * 0.9F, Quads.FULL);
                ps.pushPose();
                ps.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                ps.mulPose(Axis.ZP.rotationDegrees(age * 6));
                Quads.quadXY(ps, buf.getBuffer(RenderType.eyes(SHOCK)), s, col, a, Quads.FULL, 0, 0, 1, 1);
                ps.popPose();
            }
            case FxEntity.RING, FxEntity.SONIC_RING -> {
                float s = size * (0.15F + 0.85F * Mth.sqrt(t));
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(e.kind() == FxEntity.RING ? RING : SONIC));
                if (e.kind() == FxEntity.SONIC_RING) {
                    Quads.flat(ps, vc, s, 0.05F, col, fade, Quads.FULL);
                    ps.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pt, e.yRotO, e.getYRot())));
                    ps.mulPose(Axis.XP.rotationDegrees(90));
                    Quads.flat(ps, vc, s * 0.8F, 0, col, fade * 0.7F, Quads.FULL);
                } else {
                    Quads.flat(ps, vc, s, 0.06F, col, fade, Quads.FULL);
                }
            }
            case FxEntity.PILLAR -> {
                float rise = Math.min(1F, age / 4F);
                float h = 4.2F * size * rise * (t > 0.7F ? (1 - t) / 0.3F : 1F);
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(FLAME));
                for (int i = 0; i < 3; i++) {
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees(i * 60 + age * 4));
                    Quads.vertical(ps, vc, 1.6F * size, 0, h, -age * 0.12F, 1F, col, 1F, Quads.FULL);
                    ps.popPose();
                }
                Quads.flat(ps, buf.getBuffer(RenderType.eyes(GLOW)), 1.4F * size, 0.05F, 0xFF6010, fade, Quads.FULL);
            }
            case FxEntity.SPIKE, FxEntity.ICE_SPIKE -> {
                float rise = Mth.clamp(age / 3F, 0, 1);
                float sink = t > 0.8F ? (t - 0.8F) / 0.2F : 0;
                ps.translate(0, -1.6F * size * (1 - rise) - 1.6F * size * sink, 0);
                ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
                ps.scale(size, size, size);
                ps.scale(-1, -1, 1);
                ps.translate(0, -1.5, 0);
                ps.scale(0.5F, 0.5F, 0.5F);
                AlienModel m = e.kind() == FxEntity.SPIKE ? spike : iceSpike;
                String n = e.kind() == FxEntity.SPIKE ? "crystal_spike" : "ice_spike";
                m.root().render(ps, buf.getBuffer(RenderType.entityTranslucent(new ResourceLocation(Ben10.MODID, "textures/alien/" + n + ".png"))), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 0.92F);
                m.root().render(ps, buf.getBuffer(RenderType.eyes(new ResourceLocation(Ben10.MODID, "textures/alien/" + n + "_glow.png"))), Quads.FULL, OverlayTexture.NO_OVERLAY);
            }
            case FxEntity.TORNADO -> {
                VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(WIND));
                float a = Math.min(1F, age / 6F) * Math.min(1F, (life - age) / 8F);
                for (int i = 0; i < 7; i++) {
                    float y = i * 0.7F * size;
                    float r = (0.45F + i * 0.32F) * size;
                    ps.pushPose();
                    ps.translate(0, y, 0);
                    ps.mulPose(Axis.YP.rotationDegrees(age * (28 - i * 2) + i * 40));
                    for (int k = 0; k < 6; k++) {
                        ps.pushPose();
                        ps.mulPose(Axis.YP.rotationDegrees(k * 60));
                        ps.translate(0, 0, r);
                        Quads.vertical(ps, vc, r * 1.1F, 0, 0.9F * size, 0, 1, 0xE8F4FF, a * 0.55F, light);
                        ps.popPose();
                    }
                    ps.popPose();
                }
            }
            case FxEntity.BEAM -> {
                Vector3f v = e.vec();
                float L = v.length();
                if (L > 0.01F) {
                    ps.mulPose(Axis.YP.rotation((float) Mth.atan2(v.x(), v.z())));
                    ps.mulPose(Axis.XP.rotation((float) -Mth.atan2(v.y(), Math.sqrt(v.x() * v.x() + v.z() * v.z()))));
                    float a = Math.min(1F, (life - age) / 4F);
                    float w = size * (1F + 0.15F * Mth.sin(age * 2.1F));
                    Quads.beam(ps, buf.getBuffer(RenderType.eyes(BEAM)), w, L, -age * 0.3F, col, a, Quads.FULL);
                    Quads.beam(ps, buf.getBuffer(RenderType.eyes(BEAM)), w * 0.45F, L, -age * 0.5F, 0xFFFFFF, a, Quads.FULL);
                }
            }
            case FxEntity.PUDDLE -> {
                float grow = Math.min(1F, age / 5F);
                float a = Math.min(1F, (life - age) / 15F);
                ps.mulPose(Axis.YP.rotationDegrees(e.getId() * 37));
                Quads.flat(ps, buf.getBuffer(RenderType.entityTranslucent(GOO)), size * grow, 0.03F, 0xFFFFFF, a * 0.9F, light);
            }
            case FxEntity.AFTERIMAGE -> {
                Alien al = Alien.byId(col);
                if (al != null) AlienRenderer.renderGhost(al, e.getYRot(), age, 0.55F * fade, ps, buf);
            }
            case FxEntity.SHADOW -> {
                for (int i = 0; i < 3; i++) {
                    ps.pushPose();
                    ps.translate(Mth.sin(age * 0.2F + i * 2) * 0.3F, 0.5F + i * 0.5F, Mth.cos(age * 0.2F + i * 2) * 0.3F);
                    Quads.billboard(ps, buf.getBuffer(RenderType.entityTranslucent(SMOKE)), size * (0.6F + 0.4F * t), 0x302040, fade * 0.8F, light);
                    ps.popPose();
                }
            }
            default -> {}
        }
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FxEntity e) {
        return GLOW;
    }
}
