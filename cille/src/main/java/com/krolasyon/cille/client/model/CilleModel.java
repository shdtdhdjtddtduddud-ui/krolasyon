package com.krolasyon.cille.client.model;

import com.krolasyon.cille.entity.CilleEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** keyframe animated Cille with weighted blending between idle / move / run / rest / abilities / death */
public abstract class CilleModel<T extends CilleEntity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final Vector3f cache = new Vector3f();

    protected CilleModel(ModelPart root, ModelPart head) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = head;
    }

    @Override
    public ModelPart root() { return root; }

    protected abstract AnimationDefinition idle();
    protected abstract AnimationDefinition walk();
    protected abstract AnimationDefinition run();
    protected abstract AnimationDefinition rest();
    protected abstract AnimationDefinition death();
    @Nullable protected abstract AnimationDefinition ability(int id);
    protected abstract float walkSpeed();
    protected abstract float runSpeed();
    /** abilities that use scale / big body moves and must not be blended */
    protected boolean hardCut(int id) { return id == CilleEntity.RELEASE || id == CilleEntity.RECALL; }

    protected void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    /** used by marble renderers to show the Cille curled up inside the glass */
    public void poseStatic(float seconds) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        play(rest(), seconds, 1F);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - e.tickCount;

        if (e.deathTime > 0 || !e.isAlive()) {
            play(death(), (e.deathTime + partial) / 20F, 1F);
            return;
        }

        float castW = 0F;
        AnimationDefinition cast = null;
        float castT = 0F;
        if (e.clientAnimId >= 0) {
            cast = ability(e.clientAnimId);
            castT = (ageInTicks - e.clientAnimStart) / 20F;
            float len = cast == null ? 0F : cast.lengthInSeconds();
            if (cast == null || (castT >= len && e.clientAnimId != CilleEntity.RECALL)) {
                e.clientAnimId = -1;
                cast = null;
            } else {
                castT = Math.min(castT, len);
                castW = hardCut(e.clientAnimId) ? 1F : Mth.clamp(castT / 0.12F, 0F, 1F) * Mth.clamp((len - castT) / 0.22F, 0F, 1F);
            }
        }

        float base = 1F - castW;
        float move = Math.min(limbSwingAmount * 2.2F, 1F);
        float run = Mth.lerp(partial, e.prevRunBlend, e.runBlend);
        float sit = Mth.lerp(partial, e.prevSitBlend, e.sitBlend);

        play(idle(), ageInTicks / 20F, base * (1F - move) * (1F - sit));
        play(rest(), ageInTicks / 20F, base * (1F - move) * sit);
        if (move > 0.001F) {
            play(walk(), limbSwing * walkSpeed() / 20F, base * move * (1F - run));
            play(run(), limbSwing * runSpeed() / 20F, base * move * run);
        }
        if (cast != null) play(cast, castT, castW);

        float look = (1F - castW * 0.75F) * (1F - sit * 0.5F);
        this.head.yRot += Mth.clamp(netHeadYaw, -50F, 50F) * Mth.DEG_TO_RAD * 0.6F * look;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F * look;
    }
}
