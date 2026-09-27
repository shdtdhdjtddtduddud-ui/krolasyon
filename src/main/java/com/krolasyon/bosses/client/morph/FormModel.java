package com.krolasyon.bosses.client.morph;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.model.FormAnimSets;
import com.krolasyon.bosses.morph.Aigoar;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** A transformation model worn by players; blends locomotion, air, crouch, swim and ability animations of its form. */
public class FormModel extends HierarchicalModel<AbstractClientPlayer> {
    public static ModelLayerLocation layer(String form) {
        return new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, form), "main");
    }

    private final ModelPart root;
    private final ModelPart head;
    final ModelPart rightArm;
    @Nullable final ModelPart rightPauldron;
    @Nullable final ModelPart weapon;
    private final AnimationDefinition[] set;
    private final Vector3f cache = new Vector3f();

    public FormModel(ModelPart root, int form) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.set = FormAnimSets.SETS[form];
        this.head = this.getAnyDescendantWithName("head").orElseThrow();
        this.rightArm = this.getAnyDescendantWithName("right_arm").orElseThrow();
        this.rightPauldron = this.getAnyDescendantWithName("right_pauldron").orElse(null);
        this.weapon = this.getAnyDescendantWithName("weapon").orElse(null);
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(AbstractClientPlayer p, float a, float b, float c, float d, float e) {}

    private void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    @Nullable
    AnimationDefinition cast(int id) {
        if (id >= 0 && id < Aigoar.ABILITIES) return set[FormAnimSets.ABILITY + id];
        return switch (id) {
            case Aigoar.ANIM_TRANSFORM -> set[FormAnimSets.TRANSFORM];
            case Aigoar.ANIM_ATTACK_R -> set[FormAnimSets.ATTACK_R];
            case Aigoar.ANIM_ATTACK_L -> set[FormAnimSets.ATTACK_L];
            case Aigoar.ANIM_DOUBLE_JUMP -> set[FormAnimSets.JUMP];
            default -> null;
        };
    }

    static float castLength(int id, AnimationDefinition def) {
        return id == Aigoar.ANIM_DOUBLE_JUMP ? 0.6F : def.lengthInSeconds();
    }

    public void setup(AbstractClientPlayer p, ClientMorph.CState st, float partial) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float age = p.tickCount + partial;
        if (p.deathTime > 0 || !p.isAlive()) {
            play(set[FormAnimSets.DEATH], (p.deathTime + partial) / 20F, 1F);
            return;
        }

        float castW = 0F;
        AnimationDefinition cast = null;
        float castT = 0F;
        if (st.anim >= 0) {
            cast = cast(st.anim);
            castT = (age - st.animStart) / 20F;
            float len = cast == null ? 0F : castLength(st.anim, cast);
            if (cast == null || castT >= len || castT < 0F) {
                st.anim = -1;
                cast = null;
            } else {
                float fadeIn = st.anim == Aigoar.ANIM_TRANSFORM ? 0.001F : (st.anim >= Aigoar.ANIM_ATTACK_R ? 0.05F : 0.1F);
                castW = Mth.clamp(castT / fadeIn, 0F, 1F) * Mth.clamp((len - castT) / 0.2F, 0F, 1F);
            }
        }

        float base = 1F - castW;
        float air = Mth.lerp(partial, st.prevAir, st.air);
        float rise = Mth.lerp(partial, st.prevRise, st.rise);
        float swim = Mth.lerp(partial, st.prevSwim, st.swim);
        float crouch = Mth.lerp(partial, st.prevCrouch, st.crouch);
        float run = Mth.lerp(partial, st.prevRun, st.run);
        float ground = Mth.clamp(1F - air - swim, 0F, 1F);
        float move = Math.min(p.walkAnimation.speed(partial) * 1.6F, 1F);
        float pos = p.walkAnimation.position(partial);

        play(set[FormAnimSets.IDLE], age / 20F, base * ground * (1F - move) * (1F - crouch));
        play(set[FormAnimSets.CROUCH], age / 20F, base * ground * crouch);
        if (move > 0.001F) {
            play(set[FormAnimSets.WALK], pos * 0.07F, base * ground * move * (1F - run) * (1F - crouch * 0.5F));
            play(set[FormAnimSets.RUN], pos * 0.052F, base * ground * move * run * (1F - crouch));
        }
        play(set[FormAnimSets.JUMP], age / 20F, base * air * rise);
        play(set[FormAnimSets.FALL], age / 20F, base * air * (1F - rise));
        play(set[FormAnimSets.SWIM], age / 20F, base * swim);
        if (cast != null) play(cast, castT, castW);

        float netHeadYaw = Mth.wrapDegrees(Mth.rotLerp(partial, p.yHeadRotO, p.yHeadRot) - Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot));
        float pitch = Mth.lerp(partial, p.xRotO, p.getXRot());
        float look = (1F - castW * 0.6F) * (1F - swim);
        this.head.yRot += Mth.clamp(netHeadYaw, -70F, 70F) * Mth.DEG_TO_RAD * 0.7F * look;
        this.head.xRot += pitch * Mth.DEG_TO_RAD * 0.6F * look;
    }
}
