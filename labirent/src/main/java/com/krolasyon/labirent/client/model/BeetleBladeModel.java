package com.krolasyon.labirent.client.model;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.BeetleBladeEntity;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class BeetleBladeModel extends HierarchicalModel<BeetleBladeEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(Labirent.MODID, "beetle_blade"), "main");
    private final ModelPart root;
    private final Vector3f cache = new Vector3f();

    public BeetleBladeModel(ModelPart root) { this.root = root; }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(BeetleBladeEntity e, float limbSwing, float amount, float age, float yaw, float pitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float move = Math.min(1F, amount * 2.5F);
        KeyframeAnimations.animate(this, BeetleBladeAnimations.IDLE, (long) (age * 50F), 1F - move, cache);
        if (move > 0.01F) KeyframeAnimations.animate(this, BeetleBladeAnimations.WALK, (long) (limbSwing * 40F), move, cache);
    }
}
