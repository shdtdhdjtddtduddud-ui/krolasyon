package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.client.model.AlienAnims;
import com.krolasyon.ben10.client.model.AlienModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Procedural locomotion (walk, run, fly, float, roll, punches) layered with generated keyframe clips for abilities and guarding. */
public final class Animator {
    private static final Vector3f CACHE = new Vector3f();
    private static final float DEG = Mth.DEG_TO_RAD;

    private Animator() {}

    public static float firstPersonScale(Alien a) {
        return switch (a) {
            case FOURARMS -> 0.75F;
            case BLOWPOKE -> 0.85F;
            case ECHOECHO, BIGCHILL -> 0.9F;
            default -> 1.0F;
        };
    }

    private static void rx(ModelPart p, float v) { if (p != null) p.xRot += v; }

    private static void ry(ModelPart p, float v) { if (p != null) p.yRot += v; }

    private static void rz(ModelPart p, float v) { if (p != null) p.zRot += v; }

    private static void dy(ModelPart p, float v) { if (p != null) p.y += v; }

    private static void dz(ModelPart p, float v) { if (p != null) p.z += v; }

    private static void sc(ModelPart p, float x, float y, float z) {
        if (p != null) {
            p.xScale *= x;
            p.yScale *= y;
            p.zScale *= z;
        }
    }

    private static void vis(ModelPart p, boolean v) { if (p != null) p.visible = v; }

    /** effect-only bones are hidden unless their state is active */
    public static void hideFx(AlienModel m, OmniData d, float guard) {
        Alien a = d.alien;
        boolean ball = a == Alien.BLOWPOKE && (d.has(OmniData.F_ROLLING) || guard > 0.5F || (d.ability >= 1));
        vis(m.bone("shield"), guard > 0.25F);
        vis(m.bone("disc_wall"), guard > 0.25F);
        vis(m.bone("ball"), ball);
        vis(m.bone("stand"), !ball);
        vis(m.bone("blade"), a == Alien.DIAMONDHEAD && d.ability == 3);
        vis(m.bone("chest_eye"), a == Alien.GHOSTFREAK && (d.ability == 1 || d.ability == 2));
    }

    public static void ghostPose(AlienModel m, Alien a, float age) {
        ModelPart body = m.bone("body");
        rx(body, 0.6F);
        rx(m.bone("right_leg"), -0.9F);
        rx(m.bone("left_leg"), 0.8F);
        rx(m.bone("right_shin"), 0.6F);
        rx(m.bone("left_shin"), 0.9F);
        rx(m.bone("right_arm"), 0.8F);
        rx(m.bone("left_arm"), -0.6F);
        vis(m.bone("shield"), false);
        vis(m.bone("disc_wall"), false);
        vis(m.bone("ball"), false);
        vis(m.bone("blade"), false);
        vis(m.bone("chest_eye"), false);
    }

    public static void pose(AlienModel m, Player p, OmniData d, float pt) {
        m.reset();
        Alien a = d.alien;
        float age = p.tickCount + pt;
        float limb = p.walkAnimation.position(pt);
        float amt = Math.min(1F, p.walkAnimation.speed(pt));
        float bodyYaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
        float headYaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot) - bodyYaw), -80, 80);
        float pitch = Mth.lerp(pt, p.xRotO, p.getXRot());
        float guard = Mth.lerp(pt, d.blockAmountO, d.blockAmount);
        boolean flying = p.getAbilities().flying && !p.onGround();
        boolean busy = d.ability >= 0;
        float atk = p.getAttackAnim(pt);
        Vec3 vel = p.getDeltaMovement();
        float hspeed = (float) vel.horizontalDistance();
        hideFx(m, d, guard);

        ModelPart base = m.bone("base"), body = m.bone("body"), head = m.bone("head");
        ModelPart ra = m.bone("right_arm"), la = m.bone("left_arm"), rf = m.bone("right_fore"), lf = m.bone("left_fore");
        ModelPart rl = m.bone("right_leg"), ll = m.bone("left_leg"), rs = m.bone("right_shin"), ls = m.bone("left_shin");

        float stride = switch (a) {
            case FOURARMS -> 0.45F;
            case XLR8 -> 0.55F;
            case DIAMONDHEAD, BLOWPOKE -> 0.5F;
            default -> 0.6F;
        };
        float legAmp = switch (a) {
            case FOURARMS -> 0.75F;
            case XLR8 -> 1.25F;
            case GHOSTFREAK -> 0.25F;
            default -> 0.95F;
        };
        float w = busy ? amt * 0.35F : amt;
        float sw = Mth.cos(limb * stride);
        float sw2 = Mth.sin(limb * stride);

        // ---------------- locomotion ----------------
        if (flying && a != Alien.GHOSTFREAK) {
            float tilt = Math.min(0.9F, hspeed * 2.2F);
            rx(body, tilt * 0.6F);
            rx(head, -tilt * 0.5F);
            rx(rl, 0.25F + tilt * 0.4F + Mth.sin(age * 0.12F) * 0.08F);
            rx(ll, 0.35F + tilt * 0.4F + Mth.sin(age * 0.12F + 1.2F) * 0.08F);
            rx(rs, 0.35F);
            rx(ls, 0.45F);
            rz(ra, 0.25F + tilt * 0.2F);
            rz(la, -0.25F - tilt * 0.2F);
            rx(ra, tilt * 0.5F);
            rx(la, tilt * 0.5F);
            dy(base, Mth.sin(age * 0.1F) * 1.2F);
        } else {
            rx(rl, sw * legAmp * w);
            rx(ll, -sw * legAmp * w);
            rx(rs, Math.max(0, Mth.sin(limb * stride + 1.3F)) * 1.0F * w);
            rx(ls, Math.max(0, Mth.sin(limb * stride + 1.3F + Mth.PI)) * 1.0F * w);
            rx(ra, -sw * 0.8F * w);
            rx(la, sw * 0.8F * w);
            rx(rf, -0.15F - Math.max(0, -sw) * 0.4F * w);
            rx(lf, -0.15F - Math.max(0, sw) * 0.4F * w);
            dy(base, -Math.abs(sw2) * 1.6F * w);
            ry(body, sw * 0.08F * w);
            if (!p.onGround() && !p.isInWater() && !busy) {
                rx(rl, -0.4F);
                rx(ll, 0.25F);
                rx(rs, 0.5F);
                rx(ls, 0.2F);
                rz(ra, 0.35F);
                rz(la, -0.35F);
            }
        }
        // idle breathing
        rx(body, Mth.sin(age * 0.07F) * 0.025F);
        rz(ra, Mth.cos(age * 0.09F) * 0.04F + 0.06F);
        rz(la, -Mth.cos(age * 0.09F) * 0.04F - 0.06F);
        // head look
        if (head != null) {
            head.yRot += headYaw * DEG * 0.85F;
            head.xRot += pitch * DEG * 0.8F;
        }
        if (p.isCrouching()) {
            rx(body, 0.38F);
            dy(base, 2.5F);
            dz(base, 1.5F);
            rx(rl, -0.4F);
            rx(ll, -0.4F);
            rx(rs, 0.7F);
            rx(ls, 0.7F);
            rx(head, -0.3F);
        }

        // ---------------- per alien character ----------------
        switch (a) {
            case HEATBLAST -> {
                for (int i = 0; i < 5; i++) {
                    ModelPart f = m.bone("flame" + i);
                    if (f == null) continue;
                    f.yScale *= 1F + 0.18F * Mth.sin(age * 0.9F + i * 1.7F);
                    f.zRot += Mth.sin(age * 0.5F + i) * 0.08F;
                }
            }
            case FOURARMS -> {
                ModelPart ra2 = m.bone("right_arm2"), la2 = m.bone("left_arm2");
                rx(ra2, sw * 0.6F * w + Mth.sin(age * 0.08F) * 0.03F);
                rx(la2, -sw * 0.6F * w);
                rz(ra2, 0.1F + Mth.cos(age * 0.09F + 1F) * 0.03F);
                rz(la2, -0.1F - Mth.cos(age * 0.09F + 1F) * 0.03F);
                rx(body, 0.05F);
            }
            case XLR8 -> {
                float run = Math.min(1F, amt * 1.3F);
                rx(body, 0.12F + run * 0.45F);
                rx(head, -run * 0.45F);
                for (int i = 1; i <= 6; i++) {
                    ModelPart t = m.bone("tail" + i);
                    if (t == null) continue;
                    t.yRot += Mth.sin(age * 0.18F - i * 0.6F) * (0.07F + 0.05F * run);
                    t.xRot += -run * 0.08F + Mth.cos(age * 0.12F - i * 0.5F) * 0.03F;
                }
                if (d.has(OmniData.F_OVERDRIVE)) {
                    rx(ra, 0.5F);
                    rx(la, 0.5F);
                }
            }
            case GOOP -> {
                float wob = Mth.sin(age * 0.22F);
                sc(body, 1 + wob * 0.035F, 1 - wob * 0.035F, 1 + wob * 0.035F);
                rz(ra, Mth.sin(age * 0.13F) * 0.1F);
                rz(la, -Mth.sin(age * 0.13F + 0.8F) * 0.1F);
                ModelPart ufo = m.bone("ufo");
                if (ufo != null) {
                    ufo.y += Mth.sin(age * 0.1F) * 1.5F;
                    ufo.yRot += age * 0.12F;
                    ufo.xRot -= (body == null ? 0 : body.xRot);
                }
                if (guard > 0) {
                    sc(base, 1 + guard * 0.35F, 1 - guard * 0.45F, 1 + guard * 0.35F);
                }
            }
            case BLOWPOKE -> {
                ModelPart ball = m.bone("ball");
                if (ball != null && ball.visible) {
                    float spin = d.has(OmniData.F_ROLLING) || d.ability >= 1 ? age * 0.9F : limb * 0.4F;
                    ball.xRot += spin;
                }
            }
            case BIGCHILL -> {
                ModelPart rw = m.bone("right_wing"), lw = m.bone("left_wing");
                float open = flying ? 1F : 0F;
                float flap = flying ? Mth.sin(age * 0.55F) * 0.35F : Mth.sin(age * 0.06F) * 0.03F;
                ry(rw, open * 0.7F + flap);
                ry(lw, -open * 0.7F - flap);
                rz(rw, open * 0.25F);
                rz(lw, -open * 0.25F);
                ModelPart hood = m.bone("hood");
                rx(hood, -(head == null ? 0 : head.xRot) * 0.5F);
            }
            case ECHOECHO -> {
                for (String n : new String[]{"right_disc", "left_disc"}) {
                    ModelPart dsc = m.bone(n);
                    if (dsc != null) dsc.zRot += Mth.sin(age * 0.05F) * 0.05F;
                }
                ModelPart wall = m.bone("disc_wall");
                if (wall != null) wall.zRot += age * 0.25F;
            }
            case GHOSTFREAK -> {
                dy(base, -5F + Mth.sin(age * 0.08F) * 1.6F);
                rx(body, 0.2F + Math.min(0.5F, hspeed * 1.5F));
                rx(ra, -0.25F + Mth.sin(age * 0.07F) * 0.1F);
                rx(la, -0.35F + Mth.sin(age * 0.07F + 1) * 0.1F);
                rz(ra, 0.2F);
                rz(la, -0.2F);
                for (int i = 1; i <= 4; i++) {
                    ModelPart t = m.bone("tail" + i);
                    if (t != null) {
                        t.xRot += 0.15F + Mth.sin(age * 0.15F - i * 0.7F) * 0.12F + Math.min(0.4F, hspeed * 1.2F);
                        t.zRot += Mth.cos(age * 0.11F - i * 0.6F) * 0.08F;
                    }
                }
            }
            default -> {}
        }

        // ---------------- melee punch ----------------
        if (atk > 0 && !busy) {
            boolean left = (p.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND) != (p.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
            ModelPart arm = left ? la : ra;
            ModelPart fore = left ? lf : rf;
            float f1 = 1F - atk;
            f1 = 1F - f1 * f1 * f1 * f1;
            float swing = Mth.sin(f1 * Mth.PI);
            float twist = Mth.sin(Mth.sqrt(atk) * ((float) (Math.PI * 2))) * 0.25F;
            ry(body, left ? -twist : twist);
            rx(arm, -swing * 1.5F - 0.3F * Mth.sin(atk * Mth.PI));
            ry(arm, (left ? 1 : -1) * swing * 0.25F);
            rx(fore, swing * 0.15F);
            if (a == Alien.FOURARMS) {
                ModelPart arm2 = m.bone(left ? "left_arm2" : "right_arm2");
                float s2 = Mth.sin(Math.max(0, f1 - 0.25F) / 0.75F * Mth.PI);
                rx(arm2, -s2 * 1.3F);
            }
            if (a == Alien.BLOWPOKE || a == Alien.GOOP) rx(fore, -swing * 0.3F);
        }

        // ---------------- ability clips ----------------
        if (busy) {
            AnimationDefinition def = AlienAnims.get(a.id + "/ab" + (d.ability + 1));
            if (def != null) {
                long ms = (long) ((d.abilityTick - 1 + pt) * 50F);
                KeyframeAnimations.animate(m, def, Math.max(0, ms), 1F, CACHE);
            }
        }
        if (guard > 0.01F) {
            AnimationDefinition def = AlienAnims.get(a.id + "/block");
            if (def != null) KeyframeAnimations.animate(m, def, (long) (age * 50F), guard, CACHE);
        }
    }

    static long now() {
        return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
    }
}
