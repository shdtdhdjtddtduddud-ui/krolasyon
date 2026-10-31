package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.SarkanModel;
import com.krolasyon.cille.entity.SarkanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SarkanRenderer extends CilleRenderer<SarkanEntity, SarkanModel> {
    public SarkanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SarkanModel(ctx.bakeLayer(SarkanModel.LAYER)), "sarkan", 1.0F, 0.5F);
    }

    @Override protected float mouthForward() { return 0.4F; }
    @Override protected float mouthUp() { return 3.0F; }
    @Override protected float[] beamColors() { return new float[]{0.8F, 0.6F, 0.3F, 0.9F, 0.75F, 0.45F, 1.0F, 0.95F, 0.8F}; }
    @Override protected float shieldRadius() { return 2.4F; }
    @Override protected float shieldCenter() { return 1.7F; }
}
