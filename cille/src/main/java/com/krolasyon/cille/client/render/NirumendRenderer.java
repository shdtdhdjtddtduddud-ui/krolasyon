package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.NirumendModel;
import com.krolasyon.cille.entity.NirumendEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class NirumendRenderer extends CilleRenderer<NirumendEntity, NirumendModel> {
    public NirumendRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NirumendModel(ctx.bakeLayer(NirumendModel.LAYER)), "nirumend", 2.2F, 0.5F);
    }

    @Override protected float mouthForward() { return 3.2F; }
    @Override protected float mouthUp() { return 2.0F; }
    @Override protected float[] beamColors() { return new float[]{0.4F, 0.6F, 1.0F, 0.7F, 0.85F, 1.0F, 1.0F, 1.0F, 1.0F}; }
    @Override protected float shieldRadius() { return 4.2F; }
    @Override protected float shieldCenter() { return 2.0F; }
}
