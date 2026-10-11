package com.krolasyon.ben10.client.render;

import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.client.ClientSetup;
import com.krolasyon.ben10.client.model.AlienModel;
import com.krolasyon.ben10.entity.AlienProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ProjectileRenderer extends EntityRenderer<AlienProjectile> {
    public static final ResourceLocation GLOW = new ResourceLocation(Ben10.MODID, "textures/fx/glow.png");
    public static final ResourceLocation FIREBALL = new ResourceLocation(Ben10.MODID, "textures/fx/fireball.png");
    public static final ResourceLocation ACID = new ResourceLocation(Ben10.MODID, "textures/fx/acid.png");
    private final AlienModel shard, spear, disc;
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;

    public ProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shard = new AlienModel(ctx.bakeLayer(ClientSetup.layer("crystal_shard")));
        spear = new AlienModel(ctx.bakeLayer(ClientSetup.layer("ice_spear")));
        disc = new AlienModel(ctx.bakeLayer(ClientSetup.layer("sonic_disc")));
        blocks = ctx.getBlockRenderDispatcher();
        shadowRadius = 0.1F;
    }

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(Ben10.MODID, "textures/alien/" + n + ".png");
    }

    private static ResourceLocation glow(String n) {
        return new ResourceLocation(Ben10.MODID, "textures/alien/" + n + "_glow.png");
    }

    @Override
    public void render(AlienProjectile e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float s = e.scale();
        ps.pushPose();
        ps.translate(0, e.getBbHeight() / 2, 0);
        switch (e.kind()) {
            case AlienProjectile.FIREBALL -> {
                Quads.billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), 0.9F * s, 0xFF8020, 0.8F, Quads.FULL);
                ps.pushPose();
                ps.mulPose(net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                ps.mulPose(Axis.ZP.rotationDegrees(age * 25));
                Quads.quadXY(ps, buf.getBuffer(RenderType.eyes(FIREBALL)), 0.45F * s, 0xFFFFFF, 1F, Quads.FULL, 0, 0, 1, 1);
                ps.popPose();
            }
            case AlienProjectile.ACID -> {
                Quads.billboard(ps, buf.getBuffer(RenderType.entityTranslucent(ACID)), 0.35F * s, 0xFFFFFF, 1F, light);
                Quads.billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), 0.5F * s, 0x60C020, 0.4F, Quads.FULL);
            }
            case AlienProjectile.CRYSTAL, AlienProjectile.SHARD_FALL, AlienProjectile.REFLECT -> {
                orient(ps, e, pt);
                ps.scale(s, s, s);
                model(shard, "crystal_shard", ps, buf, light);
            }
            case AlienProjectile.ICE_SPEAR -> {
                orient(ps, e, pt);
                ps.scale(s, s, s);
                model(spear, "ice_spear", ps, buf, light);
            }
            case AlienProjectile.DISC, AlienProjectile.HOMING_DISC -> {
                ps.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pt, e.yRotO, e.getYRot())));
                ps.mulPose(Axis.XP.rotationDegrees(80));
                ps.mulPose(Axis.ZP.rotationDegrees(age * (e.stuck() == -2 ? 40 : 8)));
                float pulse = e.stuck() != -2 ? 1F + 0.12F * Mth.sin(age * 0.8F) : 1F;
                ps.scale(s * pulse, s * pulse, s * pulse);
                model(disc, "sonic_disc", ps, buf, light);
            }
            case AlienProjectile.BOULDER -> {
                ps.mulPose(Axis.XP.rotationDegrees(age * 18));
                ps.mulPose(Axis.ZP.rotationDegrees(age * 11));
                float k = 1.1F * s;
                ps.scale(k, k, k);
                ps.translate(-0.5, -0.5, -0.5);
                blocks.renderSingleBlock(e.block(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            }
            default -> {}
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    private static void orient(PoseStack ps, AlienProjectile e, float pt) {
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(pt, e.xRotO, e.getXRot())));
    }

    /** fx models are authored pointing along +z around the origin (D=2 units) */
    private static void model(AlienModel m, String n, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.scale(0.5F, 0.5F, 0.5F);
        m.root().render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex(n))), light, OverlayTexture.NO_OVERLAY);
        m.root().render(ps, buf.getBuffer(RenderType.eyes(glow(n))), Quads.FULL, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AlienProjectile e) {
        return GLOW;
    }
}
