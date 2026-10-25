package com.krolasyon.labirent.client.render;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.client.ClientState;
import com.krolasyon.labirent.entity.GladerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

/** Gladers wear their own painted skins (Teresa the slim one), hold their job's tool and talk in speech bubbles. */
public class GladerRenderer extends HumanoidMobRenderer<GladerEntity, PlayerModel<GladerEntity>> {
    private final PlayerModel<GladerEntity> wide;
    private final PlayerModel<GladerEntity> slim;

    public GladerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = this.model;
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    private static boolean slim(GladerEntity e) { return "teresa".equals(e.storyId()) || e.getSkin() >= GladerEntity.SKINS; }

    @Override
    public void render(GladerEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        this.model = slim(e) ? slim : wide;
        this.model.crouching = e.isNewcomer();
        this.model.rightArmPose = e.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        super.render(e, yaw, partial, pose, buf, light);
        renderBubble(e, partial, pose, buf, light);
    }

    private void renderBubble(GladerEntity e, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        ClientState.Bubble b = ClientState.bubbles.get(e.getId());
        Minecraft mc = Minecraft.getInstance();
        if (b == null || mc.level == null) return;
        float age = mc.level.getGameTime() - b.start() + partial;
        if (age > 110) {
            ClientState.bubbles.remove(e.getId());
            return;
        }
        if (entityRenderDispatcher.distanceToSqr(e) > 32 * 32) return;
        Font font = getFont();
        List<FormattedCharSequence> lines = font.split(b.text(), 150);
        if (lines.size() > 4) lines = lines.subList(0, 4);
        float fade = Mth.clamp(age / 5F, 0F, 1F) * Mth.clamp((110 - age) / 12F, 0F, 1F);
        float rise = Mth.clamp(age / 8F, 0F, 1F) * 0.15F;
        pose.pushPose();
        pose.translate(0, e.getBbHeight() + 0.85 + rise + lines.size() * 0.25, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f m = pose.last().pose();
        int bg = ((int) (fade * 0.55F * 255) << 24);
        int fg = ((int) (Math.max(0.1F, fade) * 255) << 24) | 0xFFFFFF;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence l = lines.get(i);
            float x = -font.width(l) / 2F;
            font.drawInBatch(l, x, i * 10, fg, false, m, buf, Font.DisplayMode.SEE_THROUGH, bg, light);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GladerEntity e) {
        if (e.hasCustomSkin()) return new ResourceLocation(Labirent.MODID, "textures/entity/glader/" + e.storyId() + ".png");
        int s = e.getSkin();
        if (s >= GladerEntity.SKINS) return new ResourceLocation("textures/entity/player/slim/alex.png");
        return new ResourceLocation("textures/entity/player/wide/" + GladerEntity.SKIN_NAMES[Math.max(0, s)] + ".png");
    }

    @Override
    protected void scale(GladerEntity e, PoseStack pose, float partial) {
        float s = "chuck".equals(e.storyId()) ? 0.86F : 0.9375F;
        pose.scale(s, s, s);
    }
}
