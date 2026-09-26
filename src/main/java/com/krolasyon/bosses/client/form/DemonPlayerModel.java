package com.krolasyon.bosses.client.form;

import com.krolasyon.bosses.client.model.HeartDemonAnimations;
import com.krolasyon.bosses.form.DemonForm;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Heartbreaker Demon model driven by a player: blends idle / walk / run / crouch / airborne locomotion,
 * alternating claw slashes on every swing and the ability, transformation, flip and slam animations.
 */
public class DemonPlayerModel extends HierarchicalModel<AbstractClientPlayer> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final Vector3f cache = new Vector3f();

    public DemonPlayerModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = this.getAnyDescendantWithName("head").orElseThrow();
        this.rightArm = this.getAnyDescendantWithName("right_arm").orElseThrow();
        this.leftArm = this.getAnyDescendantWithName("left_arm").orElseThrow();
    }

    @Override
    public ModelPart root() { return root; }

    public ModelPart arm(boolean right) { return right ? rightArm : leftArm; }

    static AnimationDefinition animFor(int id) {
        return switch (id) {
            case DemonForm.WHIP -> HeartDemonAnimations.WHIP;
            case DemonForm.HEARTS -> HeartDemonAnimations.HEART;
            case DemonForm.DASH -> HeartDemonAnimations.DASH;
            case DemonForm.TEMPEST -> HeartDemonAnimations.TEMPEST;
            case DemonForm.JUDGEMENT -> HeartDemonAnimations.ULT_RISE;
            case DemonForm.ANIM_TRANSFORM -> HeartDemonAnimations.TRANSFORM;
            case DemonForm.ANIM_FLIP -> HeartDemonAnimations.FLIP;
            case DemonForm.ANIM_SLAM -> HeartDemonAnimations.ULT_SLAM;
            default -> null;
        };
    }

    /** animations whose whole-body rotations must not be faded (a scaled 360° spin would look broken) */
    private static boolean noFade(int id) {
        return id == DemonForm.ANIM_TRANSFORM || id == DemonForm.ANIM_FLIP || id == DemonForm.TEMPEST;
    }

    private void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    @Override
    public void setupAnim(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        DemonClient.Data d = DemonClient.get(p);
        if (d == null) return;
        float partial = ageInTicks - p.tickCount;

        if (p.deathTime > 0) {
            play(HeartDemonAnimations.DEATH, (p.deathTime + partial) / 20F, 1F);
            return;
        }

        AnimationDefinition cast = null;
        float castT = 0F, castW = 0F;
        if (d.anim >= 0) {
            cast = animFor(d.anim);
            castT = Math.max(0F, (ageInTicks - d.animStart) / 20F);
            if (cast == null || castT >= cast.lengthInSeconds()) {
                d.anim = -1;
                cast = null;
            } else if (noFade(d.anim)) {
                castW = 1F;
            } else {
                float len = cast.lengthInSeconds();
                float in = d.anim == DemonForm.ANIM_SLAM ? 1F : Mth.clamp(castT / 0.1F, 0F, 1F);
                castW = in * Mth.clamp((len - castT) / 0.2F, 0F, 1F);
            }
        }

        float slashW = 0F, slashT = 0F;
        if (d.swingStart >= 0) {
            AnimationDefinition sl = d.slashLeft ? HeartDemonAnimations.SLASH_L : HeartDemonAnimations.SLASH_R;
            slashT = (ageInTicks - d.swingStart) / 20F;
            float len = sl.lengthInSeconds();
            if (slashT >= len || slashT < 0) d.swingStart = -1;
            else slashW = Mth.clamp(slashT / 0.04F, 0F, 1F) * Mth.clamp((len - slashT) / 0.12F, 0F, 1F);
        }

        float base = 1F - castW;
        float loco = base * (1F - slashW * 0.45F);
        float air = Mth.lerp(partial, d.prevAir, d.air);
        float crouch = Mth.lerp(partial, d.prevCrouch, d.crouch);
        float run = Mth.lerp(partial, d.prevRun, d.run);
        float move = Math.min(limbSwingAmount * 2.2F, 1F);
        float ground = 1F - air;
        float stand = 1F - crouch;

        play(HeartDemonAnimations.IDLE, ageInTicks / 20F, loco * ground * (1F - move) * stand);
        if (move > 0.001F) {
            play(HeartDemonAnimations.WALK, limbSwing * 1.7F / 20F, loco * ground * move * (1F - run) * stand);
            play(HeartDemonAnimations.RUN, limbSwing * 1.3F / 20F, loco * ground * move * run * stand);
            play(HeartDemonAnimations.WALK, limbSwing * 1.7F / 20F, loco * ground * move * crouch * 0.55F);
        }
        play(HeartDemonAnimations.CROUCH, ageInTicks / 20F, loco * ground * crouch);
        play(HeartDemonAnimations.JUMP, ageInTicks / 20F, loco * air);
        if (slashW > 0F) play(d.slashLeft ? HeartDemonAnimations.SLASH_L : HeartDemonAnimations.SLASH_R, slashT, slashW * (1F - castW));
        if (cast != null) play(cast, castT, castW);

        float look = 1F - castW * 0.7F;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.6F * look;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD * 0.6F * look;
    }
}
