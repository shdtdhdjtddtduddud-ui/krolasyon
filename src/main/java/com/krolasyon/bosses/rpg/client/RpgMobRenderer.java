package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.rpg.mob.RpgAnimatable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.function.Function;

/** Renders any archetype model with a per-entity texture, optional emissive layer and per-entity scale. */
public class RpgMobRenderer<T extends Mob & RpgAnimatable> extends MobRenderer<T, RpgMobModel<T>> {
    public interface Scale<T> { float of(T e); }

    private final Function<T, ResourceLocation> texture;
    private final Scale<T> scale;
    private final boolean translucent;

    public RpgMobRenderer(EntityRendererProvider.Context ctx, RpgMobModel<T> model, float shadow, Function<T, ResourceLocation> texture,
                          @Nullable Function<T, ResourceLocation> glow, Scale<T> scale, boolean translucent) {
        super(ctx, model, shadow);
        this.texture = texture;
        this.scale = scale;
        this.translucent = translucent;
        if (glow != null) this.addLayer(new Glow<>(this, glow));
    }

    @Override
    public ResourceLocation getTextureLocation(T e) { return texture.apply(e); }

    @Override
    protected void scale(T e, PoseStack ps, float partial) {
        float s = scale.of(e);
        ps.scale(s, s, s);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(T e, boolean visible, boolean translucentFlag, boolean glowing) {
        if (translucent && visible) return RenderType.entityTranslucent(getTextureLocation(e));
        return super.getRenderType(e, visible, translucentFlag, glowing);
    }

    static class Glow<T extends Mob & RpgAnimatable> extends RenderLayer<T, RpgMobModel<T>> {
        private final Function<T, ResourceLocation> tex;

        Glow(RenderLayerParent<T, RpgMobModel<T>> parent, Function<T, ResourceLocation> tex) {
            super(parent);
            this.tex = tex;
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffers, int light, T e, float limb, float limbAmt, float partial, float age, float yaw, float pitch) {
            if (e.isInvisible()) return;
            VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex.apply(e)));
            this.getParentModel().renderToBuffer(ps, vc, 0xF00000, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
