package com.sololeveling.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sololeveling.entity.SLCaster;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** One model class for every generated creature; behaviour comes from the {@link GenModels.BoneAnim} table. */
public class SLModel<T extends LivingEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final List<ModelPart> parts = new ArrayList<>();
    private final List<GenModels.BoneAnim> anims = new ArrayList<>();

    public SLModel(ModelPart root, String name) {
        this.root = root;
        GenModels.BoneAnim[] table = GenModels.MAP.get(name);
        if (table != null) {
            for (GenModels.BoneAnim a : table) {
                if (root.hasChild(a.bone())) {
                    parts.add(root.getChild(a.bone()));
                    anims.add(a);
                }
            }
        }
    }

    @Override
    public void setupAnim(T e, float limb, float limbAmt, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = age - e.tickCount;
        float attack = e.getAttackAnim(partial);
        boolean casting = e instanceof SLCaster c && c.isCasting();
        for (int i = 0; i < anims.size(); i++) {
            GenModels.BoneAnim a = anims.get(i);
            ModelPart p = parts.get(i);
            switch (a.kind()) {
                case GenModels.WALK -> p.xRot += Mth.cos(limb * 0.6662F + a.phase()) * a.amp() * limbAmt;
                case GenModels.ATTACK -> {
                    if (attack > 0) {
                        float f = Mth.sin(attack * Mth.PI);
                        float f2 = Mth.sin((1.0F - (1.0F - attack) * (1.0F - attack)) * Mth.PI);
                        p.xRot += -f * a.amp() * 0.9F - f2 * 0.5F;
                        p.yRot += f * 0.3F;
                    }
                }
                case GenModels.IDLE -> {
                    float v = Mth.sin(age * 0.067F + a.phase()) * a.amp();
                    if (a.axis() == 0) p.xRot += v; else if (a.axis() == 1) p.yRot += v; else p.zRot += v;
                }
                case GenModels.WAVE -> p.yRot += Mth.sin(age * 0.15F - a.phase()) * a.amp() * (0.35F + limbAmt * 1.5F);
                case GenModels.FLAP -> p.zRot += Mth.sin(age * 0.35F + a.phase()) * a.amp() * 2.0F;
                case GenModels.LOOK -> {
                    p.yRot += headYaw * Mth.DEG_TO_RAD;
                    p.xRot += headPitch * Mth.DEG_TO_RAD;
                }
                case GenModels.CAST -> { if (casting) p.xRot -= a.amp(); }
                case GenModels.WALKY -> p.yRot += Mth.cos(limb * 0.9F + a.phase()) * a.amp() * limbAmt;
                case GenModels.LIFTZ -> p.zRot += Math.max(0F, Mth.sin(limb * 0.9F + a.phase())) * a.amp() * limbAmt * a.axis();
                default -> { }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
