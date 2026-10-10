package com.krolasyon.storm.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Additive, unculled, no-depth-write render types for the glowing effects. */
public final class FxRenderTypes extends RenderType {
    private FxRenderTypes(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean a, boolean b, Runnable r1, Runnable r2) {
        super(n, f, m, s, a, b, r1, r2);
    }

    public static final RenderType ARC = create("stormtree_arc", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(tex -> create("stormtree_glow",
            DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 65536, false, true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexShader))
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)));

    public static RenderType glow(ResourceLocation tex) { return GLOW.apply(tex); }
}
