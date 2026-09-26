package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.entity.BossEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Keyframe animated boss model with weighted blending between idle / walk / run / ability / death animations,
 * so every transition fades smoothly instead of snapping.
 */
public abstract class BossModel<T extends BossEntity> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final Vector3f cache = new Vector3f();

    protected BossModel(ModelPart root, String headName) {
        super(RenderType::entityCutout);
        this.root = root;
        this.head = this.getAnyDescendantWithName(headName).orElseThrow();
    }

    @Override
    public ModelPart root() { return root; }

    protected abstract AnimationDefinition idle();
    protected abstract AnimationDefinition walk();
    protected abstract AnimationDefinition run();
    protected abstract AnimationDefinition death();
    protected abstract AnimationDefinition ability(int id);
    protected abstract float walkSpeed();
    protected abstract float runSpeed();

    protected void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
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
            if (cast == null || castT >= len) {
                e.clientAnimId = -1;
                cast = null;
            } else {
                castW = Mth.clamp(castT / 0.12F, 0F, 1F) * Mth.clamp((len - castT) / 0.22F, 0F, 1F);
            }
        }

        float base = 1F - castW;
        float move = Math.min(limbSwingAmount * 2.2F, 1F);
        float run = Mth.lerp(partial, e.prevRunBlend, e.runBlend);

        play(idle(), ageInTicks / 20F, base * (1F - move));
        if (move > 0.001F) {
            play(walk(), limbSwing * walkSpeed() / 20F, base * move * (1F - run));
            play(run(), limbSwing * runSpeed() / 20F, base * move * run);
        }
        if (cast != null) play(cast, castT, castW);

        float look = 1F - castW * 0.7F;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.55F * look;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD * 0.55F * look;
    }
}
