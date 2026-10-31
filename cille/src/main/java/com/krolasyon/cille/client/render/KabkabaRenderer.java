package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.KabkabaModel;
import com.krolasyon.cille.entity.KabkabaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class KabkabaRenderer extends CilleRenderer<KabkabaEntity, KabkabaModel> {
    public KabkabaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KabkabaModel(ctx.bakeLayer(KabkabaModel.LAYER)), "kabkaba", 1.7F, 0.4F);
    }

    /** kept for the other renderers that look textures up through here */
    public static ResourceLocation tex(String n) { return CilleRenderer.tex(n); }

    @Override protected float mouthForward() { return 3.1F; }
    @Override protected float mouthUp() { return 1.55F; }
    @Override protected float[] beamColors() { return new float[]{0.35F, 0.12F, 0.6F, 0.6F, 0.4F, 1.0F, 0.95F, 0.9F, 1.0F}; }
}
