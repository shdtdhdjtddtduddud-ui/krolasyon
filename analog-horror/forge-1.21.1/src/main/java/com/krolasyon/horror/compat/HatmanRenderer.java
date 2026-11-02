package com.krolasyon.horror.compat;

import com.krolasyon.horror.entity.HatmanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HatmanRenderer extends GeoEntityRenderer<HatmanEntity> {
    public HatmanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(Compat.id("hatman"), true));
        this.shadowRadius = 0.55F;
    }

    @Override
    protected float getDeathMaxRotation(HatmanEntity animatable) { return 0F; }
}
