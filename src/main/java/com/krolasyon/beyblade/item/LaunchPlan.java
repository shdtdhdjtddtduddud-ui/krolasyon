package com.krolasyon.beyblade.item;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Everything the player controls in a launch, computed the same way on both sides (the HUD gauge on the client, the
 * real launch on the server):
 * <ul>
 *   <li>direction: where the player looks; strafing while releasing curves the launch sideways</li>
 *   <li>angle: the launcher's tilt = the player's pitch; 15..50 degrees looking down is a clean launch</li>
 *   <li>power: how long the ripcord is wound; releasing right at full charge is a Perfect Launch</li>
 * </ul>
 * A sloppy launch (bad angle, shaking aim, running, a weak pull or holding too long) leaves the beyblade wobbling:
 * {@link #wobble} 0 = rock steady .. 1 = barely staying up.
 */
public final class LaunchPlan {
    public static final float IDEAL_MIN = 15F, IDEAL_MAX = 50F;
    public static final int OVERCHARGE = LauncherItem.FULL_CHARGE + 25;

    /** aim steadiness: how much the view moved over the last ticks of winding */
    private static final class Aim {
        float yaw, pitch, jitter;
        boolean init;
        double x, z, speed;
    }

    private static final Map<Player, Aim> SERVER = new WeakHashMap<>(), CLIENT = new WeakHashMap<>();

    private static Map<Player, Aim> map(Level l) { return l.isClientSide() ? CLIENT : SERVER; }

    public static void reset(Player p) { map(p.level()).remove(p); }

    /** called every tick while winding */
    public static void track(Player p) {
        Aim a = map(p.level()).computeIfAbsent(p, k -> new Aim());
        if (!a.init) {
            a.init = true;
            a.yaw = p.getYRot();
            a.pitch = p.getXRot();
            a.x = p.getX();
            a.z = p.getZ();
            return;
        }
        float d = Math.abs(Mth.wrapDegrees(p.getYRot() - a.yaw)) + Math.abs(p.getXRot() - a.pitch);
        a.jitter = a.jitter * 0.88F + d;
        a.yaw = p.getYRot();
        a.pitch = p.getXRot();
        double sp = Math.hypot(p.getX() - a.x, p.getZ() - a.z);
        a.speed = a.speed * 0.7 + sp * 0.3;
        a.x = p.getX();
        a.z = p.getZ();
    }

    public final float power, pitch, jitter, wobble;
    public final double moving;
    public final boolean perfect, powerLaunch, overcharged;
    /** the main thing to fix, as a translation key suffix (or null for a clean launch) */
    public final String hint;

    private LaunchPlan(Player p, int used) {
        Aim a = map(p.level()).get(p);
        this.power = Mth.clamp(used / (float) LauncherItem.FULL_CHARGE, 0.25F, 1F);
        this.pitch = p.getXRot();
        this.jitter = a == null ? 0F : a.jitter;
        this.moving = a == null ? 0 : a.speed;
        this.perfect = used >= LauncherItem.FULL_CHARGE && used <= LauncherItem.FULL_CHARGE + 8;
        this.powerLaunch = p.isShiftKeyDown();
        this.overcharged = used > OVERCHARGE;
        float err = pitch < IDEAL_MIN ? IDEAL_MIN - pitch : (pitch > IDEAL_MAX ? pitch - IDEAL_MAX : 0F);
        float tilt = Mth.clamp(err / 35F, 0F, 1F) * 0.55F;
        float shake = Mth.clamp((jitter - 6F) / 45F, 0F, 1F) * 0.45F;
        float run = (float) Mth.clamp(moving / 0.25, 0, 1) * 0.3F;
        float weak = power < 0.45F ? (0.45F - power) / 0.45F * 0.25F : 0F;
        float tired = overcharged ? Math.min(0.4F, (used - OVERCHARGE) * 0.01F) : 0F;
        float w = tilt + shake + run + weak + tired + (powerLaunch ? 0.1F : 0F);
        if (perfect) w *= 0.35F;
        this.wobble = Mth.clamp(w, 0F, 1F);
        float worst = Math.max(Math.max(tilt, shake), Math.max(Math.max(run, weak), tired));
        if (worst < 0.08F) hint = perfect ? "perfect" : null;
        else if (worst == tilt) hint = pitch < IDEAL_MIN ? "too_flat" : "too_steep";
        else if (worst == shake) hint = "shaky";
        else if (worst == run) hint = "moving";
        else if (worst == weak) hint = "weak";
        else hint = "tired";
    }

    public static LaunchPlan of(Player p, int used) { return new LaunchPlan(p, used); }

    public boolean idealAngle() { return pitch >= IDEAL_MIN && pitch <= IDEAL_MAX; }

    /** launch velocity in the look direction: the angle sets the arc, strafing curves it sideways */
    public Vec3 velocity(Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        // looking up throws a high arc, looking straight down slams it into the floor
        double arc = Mth.clamp((25F - pitch) / 60F, -0.6, 1.0);
        double speed = (0.35 + 0.55 * power) * (pitch > 60F ? 0.7 : 1.0);
        Vec3 v = flat.scale(speed).add(0, 0.12 + 0.22 * arc, 0);
        return v.add(curve(p));
    }

    /** sideways push from strafing while releasing */
    public Vec3 curve(Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        if (side.lengthSqr() < 1.0E-4) return Vec3.ZERO;
        side = side.normalize();
        Aim a = map(p.level()).get(p);
        if (a == null) return Vec3.ZERO;
        Vec3 move = new Vec3(p.getX() - p.xo, 0, p.getZ() - p.zo);
        double s = Mth.clamp(move.dot(side) * 2.0, -0.25, 0.25);
        return side.scale(s);
    }
}
