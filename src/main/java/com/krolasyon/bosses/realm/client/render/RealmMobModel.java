package com.krolasyon.bosses.realm.client.render;

import com.krolasyon.bosses.realm.client.gen.GenModels;
import com.krolasyon.bosses.realm.entity.AnimState;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Keyframe animated realm creature: weighted blend of idle / walk / run / attack / cast / special / death. */
public class RealmMobModel<T extends LivingEntity & AnimState.Holder> extends HierarchicalModel<T> {
    private final ModelPart root;
    @Nullable private final ModelPart head;
    private final GenModels.Info info;
    private final Vector3f cache = new Vector3f();

    public RealmMobModel(ModelPart root, GenModels.Info info) {
        super(info.translucent() ? RenderType::entityTranslucent : RenderType::entityCutoutNoCull);
        this.root = root;
        this.info = info;
        this.head = this.getAnyDescendantWithName(info.head()).orElse(null);
    }

    @Override
    public ModelPart root() { return root; }

    @Nullable
    private AnimationDefinition anim(int i) {
        AnimationDefinition[] a = info.anims();
        return i >= 0 && i < a.length ? a[i] : null;
    }

    private void play(@Nullable AnimationDefinition def, float seconds, float weight) {
        if (def == null || weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    @Override
    public void setupAnim(T e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - e.tickCount;
        if (e.deathTime > 0 || !e.isAlive()) {
            AnimationDefinition d = anim(5);
            if (d != null) play(d, Math.min((e.deathTime + partial) / 20F, d.lengthInSeconds() - 0.01F), 1F);
            return;
        }
        AnimState st = e.anim();
        float castW = 0F;
        AnimationDefinition cast = null;
        float castT = 0F;
        if (st.id >= 0) {
            cast = anim(st.id);
            castT = (ageInTicks - st.start) / 20F;
            float len = cast == null ? 0F : cast.lengthInSeconds();
            if (cast == null || castT >= len) {
                st.id = -1;
                cast = null;
            } else {
                castW = Mth.clamp(castT / 0.1F, 0F, 1F) * Mth.clamp((len - castT) / 0.2F, 0F, 1F);
            }
        }
        float base = 1F - castW;
        float move = Math.min(limbSwingAmount * 2.4F, 1F);
        float run = Mth.lerp(partial, st.prevRunBlend, st.runBlend);
        play(anim(0), ageInTicks / 20F, base * (1F - move));
        if (move > 0.001F) {
            play(anim(1), limbSwing * info.walkSpeed() / 20F * 2.0F, base * move * (1F - run));
            play(anim(6), limbSwing * info.runSpeed() / 20F * 2.0F, base * move * run);
        }
        if (cast != null) play(cast, castT, castW);
        if (head != null) {
            float look = 1F - castW * 0.7F;
            head.yRot += Mth.clamp(netHeadYaw, -60F, 60F) * Mth.DEG_TO_RAD * 0.5F * look;
            head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F * look;
        }
    }
}
