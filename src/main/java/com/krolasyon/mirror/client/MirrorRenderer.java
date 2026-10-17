package com.krolasyon.mirror.client;

import com.krolasyon.mirror.MirrorWorld;
import com.krolasyon.mirror.block.MirrorBlock;
import com.krolasyon.mirror.block.MirrorBlockEntity;
import com.krolasyon.mirror.world.HouseBlueprint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Draws the world behind the glass. Everything is squashed into a thin slab just behind the glass surface so the
 * mirror works against a wall: the reflection (a mirror image of its owner, with the owner's skin) and, once it
 * starts building, a little view of its house as it stands in the mirror dimension.
 */
public class MirrorRenderer implements BlockEntityRenderer<MirrorBlockEntity> {
    private static final ResourceLocation GLASS = new ResourceLocation(MirrorWorld.MODID, "textures/misc/mirror_glass.png");
    static final float GLASS_Z = -0.5F + 2.5F / 16F;
    static final float FIG_Z = -0.385F;
    static final float X_HALF = 0.375F, Y_MIN = 0.125F, Y_MAX = 1.875F;
    static final float DIO_S = 0.082F, DIO_FLAT = 0.03F, DIO_Z = -0.405F;
    private static final int FOG = 0xD0E4F2FF;
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();

    private final PlayerModel<LivingEntity> wide, slim;
    private final Font font;
    private final ItemRenderer items;
    private final BlockRenderDispatcher blocks;

    public MirrorRenderer(BlockEntityRendererProvider.Context ctx) {
        wide = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        font = ctx.getFont();
        items = ctx.getItemRenderer();
        blocks = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(MirrorBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        BlockState state = be.getBlockState();
        if (level == null || !state.hasProperty(MirrorBlock.FACING)) return;
        Direction f = state.getValue(MirrorBlock.FACING);
        BlockPos pos = be.getBlockPos();
        long gt = level.getGameTime();
        float t = gt + pt;
        int inner = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        Matrix3f n0 = new Matrix3f(ps.last().normal());

        UUID ownerId = be.getOwner();
        Player owner = ownerId != null ? level.getPlayerByUUID(ownerId)
                : level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 12, false);
        UUID skinId = ownerId != null ? ownerId : owner != null ? owner.getUUID() : null;
        ResourceLocation skin = SkinHelper.skin(skinId);
        PlayerModel<LivingEntity> m = SkinHelper.slim(skinId) ? slim : wide;
        boolean thanks = be.thanksUntil > gt, shake = be.shakeUntil > gt, nod = be.nodUntil > gt;
        int shown = Math.min(be.houseShown(), HouseBlueprint.HOUSE.size());

        switch (be.getPhase()) {
            case MirrorBlockEntity.MIMIC -> {
                if (owner != null) mimic(f, pos, ps, buf, inner, owner, PoseHistory.get(owner, pt, 0), m, skin);
            }
            case MirrorBlockEntity.AWAKEN -> {
                int action = MirrorClientEffects.awakenAction(gt, pos);
                if (action == MirrorClientEffects.ACT_MIMIC) {
                    if (owner != null) mimic(f, pos, ps, buf, inner, owner, PoseHistory.get(owner, pt, MirrorClientEffects.awakenDelay(gt, pos)), m, skin);
                } else {
                    reset(m);
                    idle(m, t);
                    if (action == MirrorClientEffects.ACT_STARE) {
                        m.head.zRot = 0.35F * Mth.sin(t * 0.04F);
                        m.head.xRot = -0.1F;
                        text(ps, buf, Component.translatable("mirrorworld.glass.see"), 1.72F, 0.0052F, FOG);
                    } else if (action == MirrorClientEffects.ACT_WAVE) {
                        wave(m, t);
                        text(ps, buf, Component.translatable("mirrorworld.glass.hello"), 1.72F, 0.0085F, FOG);
                    } else {
                        knock(m, t);
                    }
                    finish(m);
                    drawFigure(ps, buf, inner, m, skin, 0F, 0.5F, FIG_Z, 0.5F, 0F, false, 0.12F);
                }
            }
            case MirrorBlockEntity.BUILD -> {
                HouseBlueprint.Entry target = HouseBlueprint.HOUSE.get(Math.min(shown, HouseBlueprint.HOUSE.size() - 1));
                float fx = Mth.clamp(target.x(), -4, 4);
                diorama(ps, buf, n0, inner, shown, p -> {
                    reset(m);
                    idle(m, t);
                    if (thanks) {
                        wave(m, t);
                        finish(m);
                        drawFigure(p, buf, inner, m, skin, fx, 0F, -1.6F, 1F, 0F, false, 1F);
                    } else {
                        m.rightArm.xRot = -1.9F + Mth.sin(t * 0.55F) * 0.7F;
                        m.head.xRot = target.y() > 2 ? -0.5F : -0.15F;
                        finish(m);
                        drawFigure(p, buf, inner, m, skin, fx, 0F, -1.6F, 1F, 180F, false, 1F);
                    }
                });
                if (thanks) text(ps, buf, Component.translatable("mirrorworld.glass.thanks"), 1.72F, 0.0058F, FOG);
            }
            case MirrorBlockEntity.REQUEST -> {
                diorama(ps, buf, n0, inner, shown, null);
                reset(m);
                idle(m, t);
                if (shake) {
                    m.head.yRot = Mth.sin(t * 1.6F) * 0.6F;
                } else if (nod) {
                    m.head.xRot = 0.25F + Mth.sin(t * 1.2F) * 0.25F;
                    wave(m, t);
                } else {
                    knock(m, t);
                }
                finish(m);
                drawFigure(ps, buf, inner, m, skin, -0.1F, 0.16F, FIG_Z, 0.44F, 0F, false, 0.12F);
                ItemStack wanted = new ItemStack(be.requestedItem());
                if (!wanted.isEmpty()) {
                    item(ps, buf, wanted, 0.19F, 1.3F, t, inner, n0, level);
                    text(ps, buf, Component.literal("x" + be.getNeed()), 1.1F, 0.0065F, FOG, 0.19F);
                }
                text(ps, buf, Component.translatable("mirrorworld.glass.help"), 1.7F, 0.0092F, FOG);
            }
            case MirrorBlockEntity.DONE -> {
                diorama(ps, buf, n0, inner, shown, null);
                reset(m);
                idle(m, t);
                wave(m, t);
                finish(m);
                drawFigure(ps, buf, inner, m, skin, -0.2F, 0.16F, FIG_Z, 0.4F, 0F, false, 0.12F);
                int alpha = (int) (150 + 90 * Mth.sin(t * 0.08F));
                text(ps, buf, Component.translatable("mirrorworld.glass.break"), 1.7F, 0.0072F, (alpha << 24) | 0xE4F2FF);
                text(ps, buf, Component.translatable("mirrorworld.glass.break2"), 1.6F, 0.0042F, (alpha << 24) | 0xE4F2FF);
            }
            default -> { }
        }
        glass(ps, buf, t, n0);
        ps.popPose();
    }

    /** The reflection copies a player like a real plane mirror: mirror image, half size on the glass, at eye level. */
    private void mimic(Direction f, BlockPos bp, PoseStack ps, MultiBufferSource buf, int light, Player p, PoseHistory.Snap s,
                       PlayerModel<LivingEntity> m, ResourceLocation skin) {
        double dx = s.eye().x - (bp.getX() + 0.5), dz = s.eye().z - (bp.getZ() + 0.5);
        Direction r = f.getCounterClockWise();
        double lx = dx * r.getStepX() + dz * r.getStepZ();
        double dist = dx * f.getStepX() + dz * f.getStepZ() - GLASS_Z;
        if (dist < 0.1 || dist > 20 || Math.abs(lx) > 0.7 + dist * 0.15) return;
        double eyeRel = s.eye().y - bp.getY(), footRel = s.footY() - bp.getY();
        float y = (float) Mth.clamp((eyeRel + footRel) / 2.0, Y_MIN, Y_MAX - 0.95);
        float x = (float) Mth.clamp(lx, -0.12, 0.12);
        reset(m);
        m.crouching = s.crouch();
        m.attackTime = s.attack();
        m.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        m.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        m.prepareMobModel(p, s.limbPos(), s.limbSpeed(), 1.0F);
        m.setupAnim(p, s.limbPos(), s.limbSpeed(), s.age(), Mth.wrapDegrees(s.headYaw() - s.bodyYaw()), s.pitch());
        drawFigure(ps, buf, light, m, skin, x, y - (s.crouch() ? 0.0625F : 0F), FIG_Z, 0.5F, s.bodyYaw() - f.toYRot(), true, 0.12F);
    }

    /**
     * Renders the player model standing at (x, y, z) in the mirror frame. {@code yaw} is relative to the mirror's
     * facing (0 = looking out of the glass). {@code mirrored} reflects it through the glass plane.
     */
    private static void drawFigure(PoseStack ps, MultiBufferSource buf, int light, PlayerModel<LivingEntity> m, ResourceLocation skin,
                                   float x, float y, float z, float scale, float yaw, boolean mirrored, float flat) {
        ps.pushPose();
        Matrix3f n0 = new Matrix3f(ps.last().normal());
        ps.translate(x, y, z);
        ps.scale(scale, scale, scale * flat * (mirrored ? -1F : 1F));
        ps.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        ps.scale(-1F, -1F, 1F);
        ps.translate(0, -1.501F, 0);
        // squashing and reflecting would wreck the normals: rebuild them as rotation (and reflection) only
        Matrix3f nm = new Matrix3f(n0);
        if (mirrored) nm.mul(new Matrix3f().scaling(1F, 1F, -1F));
        nm.mul(new Matrix3f().rotationY((float) Math.toRadians(180F - yaw))).mul(new Matrix3f().scaling(-1F, -1F, 1F));
        ps.last().normal().set(nm);
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY, 0.86F, 0.9F, 1.0F, 1.0F);
        ps.popPose();
    }

    private void diorama(PoseStack ps, MultiBufferSource buf, Matrix3f n0, int light, int shown, Consumer<PoseStack> figure) {
        ps.pushPose();
        ps.translate(0, Y_MIN + DIO_S, DIO_Z);
        ps.scale(DIO_S, DIO_S, DIO_S * DIO_FLAT);
        ps.last().normal().set(n0);
        for (int x = -4; x <= 4; x++) {
            for (int z = -1; z >= -11; z--) {
                int fi = HouseBlueprint.foundationIndex(x, z);
                if (fi >= 0 && fi < shown) continue;
                block(ps, buf, GRASS, x, -1, z, light);
            }
        }
        for (int i = 0; i < shown; i++) {
            HouseBlueprint.Entry e = HouseBlueprint.HOUSE.get(i);
            if (e.state().getRenderShape() == RenderShape.MODEL || e.state().is(Blocks.CHEST)) block(ps, buf, e.state(), e.x(), e.y(), e.z(), light);
        }
        if (figure != null) figure.accept(ps);
        ps.popPose();
    }

    private void block(PoseStack ps, MultiBufferSource buf, BlockState state, int x, int y, int z, int light) {
        ps.pushPose();
        ps.translate(x - 0.5F, y, z);
        blocks.renderSingleBlock(state, ps, buf, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        ps.popPose();
    }

    private void item(PoseStack ps, MultiBufferSource buf, ItemStack stack, float x, float y, float t, int light, Matrix3f n0, Level level) {
        ps.pushPose();
        ps.translate(x, y + Mth.sin(t * 0.1F) * 0.02F, GLASS_Z - 0.012F);
        ps.scale(0.24F, 0.24F, 0.02F);
        ps.last().normal().set(n0);
        ps.mulPose(Axis.YP.rotationDegrees(180F));
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, level, 0);
        ps.popPose();
    }

    private void text(PoseStack ps, MultiBufferSource buf, Component c, float y, float size, int argb) {
        text(ps, buf, c, y, size, argb, 0F);
    }

    /** Writing in the fog on the glass. */
    private void text(PoseStack ps, MultiBufferSource buf, Component c, float y, float size, int argb, float x) {
        ps.pushPose();
        ps.translate(x, y, GLASS_Z + 0.004F);
        ps.scale(size, -size, size);
        float w = font.width(c);
        font.drawInBatch(c, -w / 2F, 0, argb, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        ps.popPose();
    }

    private static void glass(PoseStack ps, MultiBufferSource buf, float t, Matrix3f n) {
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(GLASS));
        Matrix4f mat = ps.last().pose();
        float c = 0.5F + 0.08F * Mth.sin(t * 0.05F);
        vertex(vc, mat, n, -X_HALF, Y_MIN, 0F, 1F, c);
        vertex(vc, mat, n, X_HALF, Y_MIN, 1F, 1F, c);
        vertex(vc, mat, n, X_HALF, Y_MAX, 1F, 0F, c);
        vertex(vc, mat, n, -X_HALF, Y_MAX, 0F, 0F, c);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, Matrix3f n, float x, float y, float u, float v, float c) {
        vc.vertex(mat, x, y, GLASS_Z).color(c, c, c, 1F).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0F, 0F, 1F).endVertex();
    }

    // ---- hand made poses for when the reflection acts on its own ----

    private static void reset(PlayerModel<LivingEntity> m) {
        m.head.resetPose();
        m.hat.resetPose();
        m.body.resetPose();
        m.rightArm.resetPose();
        m.leftArm.resetPose();
        m.rightLeg.resetPose();
        m.leftLeg.resetPose();
        m.crouching = false;
        m.attackTime = 0F;
        m.riding = false;
        m.young = false;
    }

    private static void idle(PlayerModel<LivingEntity> m, float t) {
        float b = Mth.sin(t * 0.067F) * 0.05F;
        m.rightArm.zRot = 0.05F + b;
        m.leftArm.zRot = -0.05F - b;
        m.rightArm.xRot = b;
        m.leftArm.xRot = -b;
    }

    private static void wave(PlayerModel<LivingEntity> m, float t) {
        m.rightArm.xRot = -2.8F;
        m.rightArm.zRot = -0.25F + Mth.sin(t * 0.45F) * 0.4F;
        m.head.zRot = 0.1F;
    }

    private static void knock(PlayerModel<LivingEntity> m, float t) {
        float hit = MirrorClientEffects.knock(t);
        m.rightArm.xRot = -1.2F - 0.45F * hit;
        m.rightArm.yRot = -0.15F;
        m.head.xRot = -0.08F;
        m.body.xRot = 0.04F * hit;
    }

    private static void finish(PlayerModel<LivingEntity> m) {
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
    }

    @Override
    public boolean shouldRenderOffScreen(MirrorBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
