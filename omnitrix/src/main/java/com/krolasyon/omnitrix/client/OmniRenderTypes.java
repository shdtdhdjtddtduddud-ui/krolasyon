package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Omnitrix;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

/** Toon outline (inverted hull, front faces culled), additive glow and plain colour render types. */
public final class OmniRenderTypes extends RenderType {
    public static final ResourceLocation WHITE = new ResourceLocation(Omnitrix.MODID, "textures/misc/white.png");

    private OmniRenderTypes(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean a, boolean b, Runnable r1, Runnable r2) {
        super(n, f, m, s, a, b, r1, r2);
    }

    private static final CullStateShard FRONT_CULL = new CullStateShard(true) {
        @Override
        public void setupRenderState() {
            super.setupRenderState();
            RenderSystem.enableCull();
            GL11.glCullFace(GL11.GL_FRONT);
        }

        @Override
        public void clearRenderState() {
            GL11.glCullFace(GL11.GL_BACK);
            super.clearRenderState();
        }
    };

    /** black inverted hull drawn behind the mesh = cartoon outline */
    public static final RenderType OUTLINE = create("omnitrix_outline", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                    .setTextureState(new TextureStateShard(WHITE, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(FRONT_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true));

    /** additive POSITION_COLOR lines / ribbons for effects */
    public static final RenderType GLOW = create("omnitrix_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** alpha blended POSITION_COLOR (smoke, slime, shields) */
    public static final RenderType SOFT = create("omnitrix_soft", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144, false, true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> SPRITE = Util.memoize(tex -> create("omnitrix_sprite",
            DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 65536, false, true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexShader))
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> SPRITE_ALPHA = Util.memoize(tex -> create("omnitrix_sprite_alpha",
            DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 65536, false, true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexShader))
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)));

    /** additive textured quads */
    public static RenderType sprite(ResourceLocation tex) { return SPRITE.apply(tex); }

    /** alpha blended textured quads */
    public static RenderType spriteAlpha(ResourceLocation tex) { return SPRITE_ALPHA.apply(tex); }
}
