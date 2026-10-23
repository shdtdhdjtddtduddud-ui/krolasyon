package com.krolasyon.mythblades.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Çift yüzlü, eklemeli (ışıyan) efekt çizim türü; doku başına bir kez oluşturulur. */
public final class FxTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> ADD = new HashMap<>();

    @SuppressWarnings("DataFlowIssue")
    private FxTypes() {
        super(null, null, null, 0, false, false, null, null);
    }

    public static RenderType additive(ResourceLocation t) {
        return ADD.computeIfAbsent(t, k -> create("mythblades_fx_add", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(new TextureStateShard(k, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
    }
}
