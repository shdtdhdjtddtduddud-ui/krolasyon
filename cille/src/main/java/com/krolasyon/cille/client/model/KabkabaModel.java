package com.krolasyon.cille.client.model;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.KabkabaEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Keyframe animated Kabkaba with weighted blending between idle / walk / run / sit / abilities / death. */
public class KabkabaModel extends HierarchicalModel<KabkabaEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(CilleMod.MODID, "kabkaba"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final Vector3f cache = new Vector3f();

    public KabkabaModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("base").getChild("body").getChild("chest").getChild("mane").getChild("head");
    }

    @Override
    public ModelPart root() { return root; }

    private void play(AnimationDefinition def, float seconds, float weight) {
        if (weight <= 0.001F) return;
        KeyframeAnimations.animate(this, def, (long) (seconds * 1000F), weight, cache);
    }

    private static AnimationDefinition ability(int id) {
        return switch (id) {
            case KabkabaEntity.MELEE -> KabkabaAnimations.ATTACK;
            case KabkabaEntity.BLACK_HOLE -> KabkabaAnimations.BLACK_HOLE;
            case KabkabaEntity.BLINK -> KabkabaAnimations.BLINK;
            case KabkabaEntity.DRAIN -> KabkabaAnimations.DRAIN;
            case KabkabaEntity.REFLECT -> KabkabaAnimations.REFLECT;
            case KabkabaEntity.SHADOW -> KabkabaAnimations.SHADOW;
            case KabkabaEntity.GUARD -> KabkabaAnimations.GUARD;
            case KabkabaEntity.SLAM -> KabkabaAnimations.SLAM;
            case KabkabaEntity.DODGE -> KabkabaAnimations.DODGE;
            case KabkabaEntity.RELEASE -> KabkabaAnimations.RELEASE;
            case KabkabaEntity.RECALL -> KabkabaAnimations.RECALL;
            default -> null;
        };
    }

    /** used by the marble renderer to show the sleeping Cille inside the glass */
    public void poseStatic(float seconds) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        play(KabkabaAnimations.SIT, seconds, 1F);
    }

    @Override
    public void setupAnim(KabkabaEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - e.tickCount;

        if (e.deathTime > 0 || !e.isAlive()) {
            play(KabkabaAnimations.DEATH, (e.deathTime + partial) / 20F, 1F);
            return;
        }

        float castW = 0F;
        AnimationDefinition cast = null;
        float castT = 0F;
        if (e.clientAnimId >= 0) {
            cast = ability(e.clientAnimId);
            castT = (ageInTicks - e.clientAnimStart) / 20F;
            float len = cast == null ? 0F : cast.lengthInSeconds();
            if (cast == null || (castT >= len && e.clientAnimId != KabkabaEntity.RECALL)) {
                e.clientAnimId = -1;
                cast = null;
            } else {
                castT = Math.min(castT, len);
                boolean hard = e.clientAnimId == KabkabaEntity.RELEASE || e.clientAnimId == KabkabaEntity.RECALL
                        || e.clientAnimId == KabkabaEntity.BLINK || e.clientAnimId == KabkabaEntity.DODGE;
                castW = hard ? 1F : Mth.clamp(castT / 0.12F, 0F, 1F) * Mth.clamp((len - castT) / 0.22F, 0F, 1F);
            }
        }

        float base = 1F - castW;
        float move = Math.min(limbSwingAmount * 2.2F, 1F);
        float run = Mth.lerp(partial, e.prevRunBlend, e.runBlend);
        float sit = Mth.lerp(partial, e.prevSitBlend, e.sitBlend);

        play(KabkabaAnimations.IDLE, ageInTicks / 20F, base * (1F - move) * (1F - sit));
        play(KabkabaAnimations.SIT, ageInTicks / 20F, base * (1F - move) * sit);
        if (move > 0.001F) {
            play(KabkabaAnimations.WALK, limbSwing * 0.9F / 20F, base * move * (1F - run));
            play(KabkabaAnimations.RUN, limbSwing * 0.75F / 20F, base * move * run);
        }
        if (cast != null) play(cast, castT, castW);

        float look = (1F - castW * 0.75F) * (1F - sit * 0.5F);
        this.head.yRot += Mth.clamp(netHeadYaw, -50F, 50F) * Mth.DEG_TO_RAD * 0.6F * look;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F * look;
    }
}
