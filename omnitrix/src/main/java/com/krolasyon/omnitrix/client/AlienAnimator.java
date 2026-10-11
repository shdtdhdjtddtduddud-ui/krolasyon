package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Ability;
import com.krolasyon.omnitrix.Alien;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Procedural skeletal animation for the alien meshes: locomotion (idle / walk / run / air / swim / sneak), melee swings,
 * blocking stances and 36 key-framed ability poses. Model space: y up, character faces +z, character's left is +x.
 * Positive X rotation swings a hanging limb backwards; positive Z rotation lifts the left arm sideways.
 */
public final class AlienAnimator {
    private AlienAnimator() {}

    /** bone index cache per model */
    private static final class Bones {
        final int body, head, armL, armR, foreL, foreR, legL, legR, shinL, shinR, arm2L, arm2R, fore2L, fore2R, tail, tail1, tail2;

        Bones(OmxModel m) {
            body = m.bone("body"); head = m.bone("head");
            armL = m.bone("arm_l"); armR = m.bone("arm_r"); foreL = m.bone("forearm_l"); foreR = m.bone("forearm_r");
            legL = m.bone("leg_l"); legR = m.bone("leg_r"); shinL = m.bone("shin_l"); shinR = m.bone("shin_r");
            arm2L = m.bone("arm2_l"); arm2R = m.bone("arm2_r"); fore2L = m.bone("forearm2_l"); fore2R = m.bone("forearm2_r");
            tail = m.bone("tail"); tail1 = m.bone("tail1"); tail2 = m.bone("tail2");
        }
    }

    private static final java.util.Map<OmxModel, Bones> BONES = new java.util.IdentityHashMap<>();

    private static void rot(Pose p, int b, float x, float y, float z) {
        if (b < 0) return;
        p.rx[b] += x;
        p.ry[b] += y;
        p.rz[b] += z;
    }

    private static void blend(Pose p, int b, float w, float x, float y, float z) {
        if (b < 0) return;
        p.rx[b] = Mth.lerp(w, p.rx[b], x);
        p.ry[b] = Mth.lerp(w, p.ry[b], y);
        p.rz[b] = Mth.lerp(w, p.rz[b], z);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static float seg(float t, float a, float b) { return smooth((t - a) / (b - a)); }

    public static void animate(OmxModel m, Pose p, Alien a, Player pl, ClientState.Look look, float pt) {
        Bones b = BONES.computeIfAbsent(m, Bones::new);
        p.reset();
        float age = pl.tickCount + pt;
        float ls = pl.walkAnimation.position(pt);
        float la = Math.min(1F, pl.walkAnimation.speed(pt));
        boolean ground = pl.onGround();
        boolean sprint = pl.isSprinting();
        float bodyYaw = Mth.rotLerp(pt, pl.yBodyRotO, pl.yBodyRot);
        float headYaw = Mth.wrapDegrees(Mth.rotLerp(pt, pl.yHeadRotO, pl.yHeadRot) - bodyYaw);
        float pitch = Mth.lerp(pt, pl.xRotO, pl.getXRot());
        float hy = -headYaw * Mth.DEG_TO_RAD, hx = pitch * Mth.DEG_TO_RAD;
        float swing = pl.getAttackAnim(pt);
        float stride = sprint ? 1.0F : 0.75F;
        float c = Mth.cos(ls * 0.6662F), s = Mth.sin(ls * 0.6662F);
        float breath = Mth.sin(age * 0.08F);

        // ---- locomotion base
        rot(p, b.head, Mth.clamp(hx, -0.9F, 0.9F), Mth.clamp(hy, -1.2F, 1.2F), 0);
        rot(p, b.legL, -c * 1.1F * la * stride, 0, 0);
        rot(p, b.legR, c * 1.1F * la * stride, 0, 0);
        rot(p, b.shinL, Math.max(0, s) * 1.1F * la, 0, 0);
        rot(p, b.shinR, Math.max(0, -s) * 1.1F * la, 0, 0);
        rot(p, b.armL, c * 0.8F * la, 0, 0.06F + breath * 0.03F);
        rot(p, b.armR, -c * 0.8F * la, 0, -0.06F - breath * 0.03F);
        rot(p, b.foreL, -0.15F - 0.35F * la, 0, 0);
        rot(p, b.foreR, -0.15F - 0.35F * la, 0, 0);
        rot(p, b.arm2L, -c * 0.6F * la, 0, 0.04F - breath * 0.02F);
        rot(p, b.arm2R, c * 0.6F * la, 0, -0.04F + breath * 0.02F);
        rot(p, b.fore2L, -0.2F - 0.3F * la, 0, 0);
        rot(p, b.fore2R, -0.2F - 0.3F * la, 0, 0);
        rot(p, b.body, breath * 0.02F + (sprint ? 0.25F : 0.05F) * la, Mth.sin(ls * 0.6662F) * 0.12F * la, 0);
        p.rootY = Math.abs(Mth.cos(ls * 0.6662F)) * 0.05F * la;
        if (!ground && !look.flying()) {
            float fall = Mth.clamp((float) -pl.getDeltaMovement().y * 2F, -1F, 1F);
            rot(p, b.legL, -0.5F, 0, 0.1F);
            rot(p, b.legR, 0.2F, 0, -0.1F);
            rot(p, b.shinL, 0.9F, 0, 0);
            rot(p, b.shinR, 0.5F, 0, 0);
            rot(p, b.armL, -0.4F - fall * 0.5F, 0, 0.5F + fall * 0.4F);
            rot(p, b.armR, -0.4F - fall * 0.5F, 0, -0.5F - fall * 0.4F);
        }
        if (pl.isCrouching()) {
            rot(p, b.body, 0.45F, 0, 0);
            rot(p, b.legL, -0.5F, 0, 0);
            rot(p, b.legR, -0.5F, 0, 0);
            rot(p, b.shinL, 0.9F, 0, 0);
            rot(p, b.shinR, 0.9F, 0, 0);
            p.rootY -= 0.15F;
        }
        if (pl.isSwimming() || pl.isVisuallySwimming()) {
            p.rootPitch = 1.4F;
            rot(p, b.armL, -2.6F + c * 0.6F, 0, 0.3F);
            rot(p, b.armR, -2.6F - c * 0.6F, 0, -0.3F);
        }

        // ---- species specific motion
        switch (a) {
            case XLR8 -> {
                float run = Math.max(la, look.overdrive() ? 1 : 0);
                rot(p, b.body, 0.15F + run * 0.35F, 0, 0);
                rot(p, b.head, -0.15F - run * 0.3F, 0, 0);
                rot(p, b.armL, 0.6F * run, 0, 0.2F);
                rot(p, b.armR, 0.6F * run, 0, -0.2F);
                rot(p, b.tail1, -0.2F * run + Mth.sin(age * 0.15F) * 0.08F, Mth.sin(age * 0.12F + ls) * 0.25F, 0);
                rot(p, b.tail2, Mth.sin(age * 0.18F) * 0.12F, Mth.sin(age * 0.12F + ls - 0.8F) * 0.35F, 0);
            }
            case TERRASPIN -> {
                p.rootRoll = Mth.sin(ls * 0.6662F) * 0.1F * la;
                rot(p, b.armL, 0, 0, 0.1F + Mth.sin(age * 0.1F) * 0.05F);
                rot(p, b.armR, 0, 0, -0.1F - Mth.sin(age * 0.1F) * 0.05F);
                if (look.flying()) {
                    p.rootYaw = age * 0.9F;
                    rot(p, b.armL, 0, 0, 1.2F);
                    rot(p, b.armR, 0, 0, -1.2F);
                }
            }
            case GOOP -> {
                float wob = Mth.sin(age * 0.3F) * 0.04F;
                p.scaleY = 1 + wob;
                p.scaleX = p.scaleZ = 1 - wob * 0.5F;
                rot(p, b.body, 0, 0, Mth.sin(age * 0.11F) * 0.05F);
                rot(p, b.head, 0, 0, Mth.sin(age * 0.13F + 1) * 0.08F);
                if (look.puddle()) {
                    p.scaleY = 0.08F;
                    p.scaleX = p.scaleZ = 1.7F + Mth.sin(age * 0.4F) * 0.08F;
                }
            }
            case GHOSTFREAK -> {
                p.rootY += 0.18F + Mth.sin(age * 0.1F) * 0.08F;
                rot(p, b.tail, Mth.sin(age * 0.12F) * 0.15F, Mth.sin(age * 0.09F) * 0.3F, Mth.sin(age * 0.1F + 1) * 0.15F);
                rot(p, b.body, 0.1F + la * 0.3F, 0, 0);
                rot(p, b.armL, Mth.sin(age * 0.07F) * 0.15F, 0, 0);
                rot(p, b.armR, Mth.sin(age * 0.07F + 2) * 0.15F, 0, 0);
            }
            case FOURARMS -> {
                rot(p, b.body, 0, 0, Mth.sin(ls * 0.6662F) * 0.05F * la);
                rot(p, b.armL, 0, 0, 0.1F);
                rot(p, b.armR, 0, 0, -0.1F);
            }
            case ULTIMATE_ECHO_ECHO -> rot(p, b.head, 0, Mth.sin(age * 0.05F) * 0.1F, 0);
            case HEATBLAST -> rot(p, b.head, 0, 0, Mth.sin(age * 0.2F) * 0.02F);
            default -> {}
        }
        if (look.flying() && a != Alien.TERRASPIN) {
            // flight pose: legs trailing, body leaning into the motion
            p.rootPitch = (float) Math.min(1.2, pl.getDeltaMovement().horizontalDistance() * 2.5);
            rot(p, b.legL, 0.3F + Mth.sin(age * 0.2F) * 0.1F, 0, 0);
            rot(p, b.legR, 0.4F + Mth.sin(age * 0.2F + 1) * 0.1F, 0, 0);
            rot(p, b.armL, 0.3F, 0, 0.6F);
            rot(p, b.armR, 0.3F, 0, -0.6F);
        }

        // ---- melee swing (alternates arms like a boxer, four arms use the lower pair too)
        if (swing > 0) {
            float k = Mth.sin(Mth.sqrt(swing) * Mth.PI);
            boolean left = (pl.tickCount / 6) % 2 == 1;
            int arm = left ? b.armL : b.armR, fore = left ? b.foreL : b.foreR;
            blend(p, arm, k, -1.5F + hx, (left ? -0.3F : 0.3F), 0);
            blend(p, fore, k, -0.2F, 0, 0);
            rot(p, b.body, 0, (left ? 0.35F : -0.35F) * k, 0);
            if (a == Alien.FOURARMS) blend(p, left ? b.arm2R : b.arm2L, k, -1.3F + hx, 0, 0);
        }

        // ---- block stance
        if (look.blocking) blockPose(p, b, a, age);

        // ---- ability animation
        Ability ab = Ability.byId(look.cast);
        float t = look.castProgress(pt);
        if (ab != null && t >= 0 && ab.alien == a) abilityPose(p, b, ab, t, age, hx);
    }

    private static void blockPose(Pose p, Bones b, Alien a, float age) {
        switch (a) {
            case TERRASPIN -> {
                // withdraw into the shell
                for (int i : new int[]{b.head, b.armL, b.armR, b.foreL, b.foreR, b.legL, b.legR, b.shinL, b.shinR}) if (i >= 0) p.s[i] = 0.05F;
                p.rootY -= 0.35F;
            }
            case GOOP -> {
                p.scaleY = 0.55F;
                p.scaleX = p.scaleZ = 1.35F;
            }
            case XLR8 -> {
                blend(p, b.body, 1, 0.6F, 0, 0);
                blend(p, b.armL, 1, -1.2F, -0.6F, 0.2F);
                blend(p, b.armR, 1, -1.2F, 0.6F, -0.2F);
                rot(p, b.legL, -0.6F, 0, 0);
                rot(p, b.legR, -0.6F, 0, 0);
                rot(p, b.shinL, 1.0F, 0, 0);
                rot(p, b.shinR, 1.0F, 0, 0);
                p.rootY -= 0.2F;
            }
            case ULTIMATE_ECHO_ECHO -> {
                blend(p, b.armL, 1, -1.4F, 0, 0.5F);
                blend(p, b.armR, 1, -1.4F, 0, -0.5F);
            }
            default -> {
                // arms crossed in front of the face
                blend(p, b.armL, 1, -1.6F, -0.7F, 0.1F);
                blend(p, b.armR, 1, -1.6F, 0.7F, -0.1F);
                blend(p, b.foreL, 1, -0.9F, 0, 0);
                blend(p, b.foreR, 1, -0.9F, 0, 0);
                blend(p, b.arm2L, 1, -1.2F, -0.5F, 0);
                blend(p, b.arm2R, 1, -1.2F, 0.5F, 0);
                rot(p, b.body, 0.15F, 0, 0);
                rot(p, b.legL, -0.3F, 0, 0.1F);
                rot(p, b.legR, 0.25F, 0, -0.1F);
            }
        }
    }

    private static void abilityPose(Pose p, Bones b, Ability ab, float t, float age, float hx) {
        float w = seg(t, 0, 0.12F) * (1 - seg(t, 0.85F, 1F));
        float aim = -Mth.HALF_PI + hx;
        float tremble = Mth.sin(age * 2.7F) * 0.05F;
        switch (ab) {
            // ---- shoot with the right arm
            case FIREBALL, CRYSTAL_SHARDS, ACID_SPIT, SONIC_DISCS -> {
                float back = seg(t, 0, 0.3F), go = seg(t, 0.3F, 0.45F);
                blend(p, b.armR, w, Mth.lerp(go, -0.6F * back, aim), Mth.lerp(go, 0.4F * back, 0.1F), 0);
                blend(p, b.foreR, w, Mth.lerp(go, -1.2F * back, 0), 0, 0);
                rot(p, b.body, 0, w * Mth.lerp(go, 0.35F * back, -0.25F), 0);
                if (ab == Ability.SONIC_DISCS) {
                    blend(p, b.armL, w, Mth.lerp(go, -0.6F * back, aim), Mth.lerp(go, -0.4F * back, -0.1F), 0);
                }
            }
            // ---- channel a beam with both hands
            case FLAMETHROWER, SONIC_SCREAM -> {
                blend(p, b.armL, w, aim + tremble, -0.25F, 0);
                blend(p, b.armR, w, aim - tremble, 0.25F, 0);
                blend(p, b.foreL, w, 0, 0, 0);
                blend(p, b.foreR, w, 0, 0, 0);
                rot(p, b.legL, -0.35F * w, 0, 0);
                rot(p, b.legR, 0.3F * w, 0, 0);
                if (ab == Ability.SONIC_SCREAM) rot(p, b.head, w * (t < 0.3F ? -0.5F * seg(t, 0, 0.3F) : 0.3F), 0, 0);
            }
            case FREEZE_BREATH -> {
                rot(p, b.head, w * (0.2F + tremble), 0, 0);
                blend(p, b.armL, w, 0.4F, 0, 0.9F);
                blend(p, b.armR, w, 0.4F, 0, -0.9F);
                rot(p, b.body, 0.25F * w, 0, 0);
            }
            // ---- burst outwards
            case SUPERNOVA, TERROR, SONIC_DOOM, OVERDRIVE -> {
                float gather = seg(t, 0, 0.4F), burst = seg(t, 0.4F, 0.55F);
                float x = Mth.lerp(burst, -1.0F * gather, -2.4F), z = Mth.lerp(burst, -0.5F * gather, 1.3F);
                blend(p, b.armL, w, x + gather * (1 - burst) * tremble, 0, z);
                blend(p, b.armR, w, x - gather * (1 - burst) * tremble, 0, -z);
                blend(p, b.arm2L, w, x * 0.7F, 0, z * 0.8F);
                blend(p, b.arm2R, w, x * 0.7F, 0, -z * 0.8F);
                rot(p, b.body, w * Mth.lerp(burst, 0.3F * gather, -0.25F), 0, 0);
                rot(p, b.head, w * Mth.lerp(burst, 0.3F * gather, -0.4F), 0, 0);
                rot(p, b.legL, 0, 0, 0.25F * burst * w);
                rot(p, b.legR, 0, 0, -0.25F * burst * w);
            }
            // ---- slam the ground
            case GROUND_POUND, SHELL_SLAM, SPIKE_WAVE -> {
                float up = seg(t, 0, 0.45F), slam = seg(t, 0.45F, 0.6F);
                float x = Mth.lerp(slam, -2.9F * up, -0.3F);
                blend(p, b.armL, w, x, 0, Mth.lerp(slam, 0.15F, 0.3F));
                blend(p, b.armR, w, x, 0, Mth.lerp(slam, -0.15F, -0.3F));
                blend(p, b.arm2L, w, x * 0.8F, 0, 0.3F);
                blend(p, b.arm2R, w, x * 0.8F, 0, -0.3F);
                rot(p, b.body, w * Mth.lerp(slam, -0.2F * up, 0.6F), 0, 0);
                rot(p, b.legL, w * -0.6F * slam, 0, 0.2F * slam * w);
                rot(p, b.legR, w * 0.3F * slam, 0, -0.2F * slam * w);
                rot(p, b.shinL, w * 0.9F * slam, 0, 0);
                if (ab == Ability.SHELL_SLAM) p.rootYaw += w * t * 720F;
            }
            // ---- four handed clap
            case THUNDER_CLAP -> {
                float open = seg(t, 0, 0.4F), clap = seg(t, 0.4F, 0.5F);
                float z = Mth.lerp(clap, 1.2F * open, -0.15F);
                blend(p, b.armL, w, -1.4F, 0, z);
                blend(p, b.armR, w, -1.4F, 0, -z);
                blend(p, b.arm2L, w, -1.2F, 0, z * 0.9F);
                blend(p, b.arm2R, w, -1.2F, 0, -z * 0.9F);
                blend(p, b.foreL, w, 0, 0, 0);
                blend(p, b.foreR, w, 0, 0, 0);
            }
            // ---- punch combo (each arm in turn)
            case QUAD_COMBO, BLITZ -> {
                int[] arms = {b.armR, b.armL, b.arm2R, b.arm2L};
                int hits = ab == Ability.BLITZ ? 8 : 4;
                float k = t * hits;
                int i = Math.min(hits - 1, (int) k);
                float f = Mth.sin(smooth(k - i) * Mth.PI);
                int arm = arms[i % 4];
                if (arm < 0) arm = arms[i % 2];
                blend(p, arm, w * f, aim, (i % 2 == 0 ? 0.25F : -0.25F), 0);
                rot(p, b.body, 0, w * f * (i % 2 == 0 ? -0.4F : 0.4F), 0);
            }
            // ---- throw overhead
            case BOULDER_THROW, ACID_BOMB -> {
                float lift = seg(t, 0, 0.5F), thr = seg(t, 0.5F, 0.65F);
                float x = Mth.lerp(thr, -3.0F * lift, aim + 0.2F);
                blend(p, b.armL, w, x, 0, 0.2F);
                blend(p, b.armR, w, x, 0, -0.2F);
                blend(p, b.arm2L, w, x, 0, 0.3F);
                blend(p, b.arm2R, w, x, 0, -0.3F);
                rot(p, b.body, w * Mth.lerp(thr, -0.3F * lift, 0.4F), 0, 0);
            }
            // ---- reach and pull
            case STRETCH_GRAB, TENTACLES, POSSESS, CRYSTAL_PRISON, ICE_PRISON -> {
                float reach = seg(t, 0, 0.25F), pull = seg(t, 0.6F, 0.8F);
                blend(p, b.armR, w, Mth.lerp(pull, aim * reach, -0.6F), 0.1F, 0);
                blend(p, b.foreR, w, Mth.lerp(pull, 0, -1.0F), 0, 0);
                if (ab != Ability.STRETCH_GRAB) blend(p, b.armL, w, Mth.lerp(pull, aim * reach, -0.6F), -0.1F, 0);
                if (ab == Ability.STRETCH_GRAB) {
                    // goop's arm literally stretches
                    if (b.foreR >= 0) p.s[b.foreR] = 1 + reach * (1 - pull) * 1.6F;
                }
                rot(p, b.body, w * Mth.lerp(pull, 0.2F * reach, -0.15F), 0, 0);
            }
            // ---- spins
            case TORNADO, SPIN_FLIGHT, VORTEX_RUN, WIND_GUST -> {
                if (ab == Ability.WIND_GUST) {
                    float flap = seg(t, 0, 0.4F), fwd = seg(t, 0.4F, 0.6F);
                    float z = Mth.lerp(fwd, 1.6F * flap, 0.2F);
                    blend(p, b.armL, w, Mth.lerp(fwd, 0, -1.4F), 0, z);
                    blend(p, b.armR, w, Mth.lerp(fwd, 0, -1.4F), 0, -z);
                } else {
                    p.rootYaw += w * t * (ab == Ability.VORTEX_RUN ? 2160F : 1080F);
                    blend(p, b.armL, w, 0, 0, 1.4F);
                    blend(p, b.armR, w, 0, 0, -1.4F);
                }
            }
            // ---- dash
            case SPEED_DASH, FIRE_JET, PHASE, INTANGIBLE -> {
                p.rootPitch += w * 0.5F;
                blend(p, b.armL, w, 1.1F, 0, 0.3F);
                blend(p, b.armR, w, 1.1F, 0, -0.3F);
                blend(p, b.legL, w, 0.6F, 0, 0);
                blend(p, b.legR, w, -0.3F, 0, 0);
            }
            // ---- power up (arms crossed then flung open)
            case CRYSTAL_BLADES, PUDDLE, ICE_FLIGHT, DISC_RIDE -> {
                float cross = seg(t, 0, 0.4F), open = seg(t, 0.4F, 0.6F);
                float x = Mth.lerp(open, -1.3F * cross, -0.5F), y = Mth.lerp(open, -0.8F * cross, 0);
                float z = Mth.lerp(open, 0, 1.1F);
                blend(p, b.armL, w, x, y, z);
                blend(p, b.armR, w, x, -y, -z);
                rot(p, b.head, w * Mth.lerp(open, 0.3F * cross, -0.35F), 0, 0);
                if (ab == Ability.PUDDLE) {
                    float melt = seg(t, 0.2F, 0.9F);
                    p.scaleY = Mth.lerp(melt, p.scaleY, 0.08F);
                    p.scaleX = p.scaleZ = Mth.lerp(melt, p.scaleX, 1.7F);
                }
            }
            default -> {}
        }
    }
}
