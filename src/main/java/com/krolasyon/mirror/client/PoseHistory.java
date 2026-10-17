package com.krolasyon.mirror.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Recent poses of the local player, so a waking reflection can lag behind its original. */
public final class PoseHistory {
    public record Snap(Vec3 eye, double footY, float bodyYaw, float headYaw, float pitch, float limbPos, float limbSpeed,
                       float attack, boolean crouch, float age) {}

    private static final Snap[] RING = new Snap[64];
    private static int head;
    private static int size;

    public static Snap live(Player p, float pt) {
        return new Snap(p.getEyePosition(pt), Mth.lerp(pt, p.yo, p.getY()), Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot),
                Mth.rotLerp(pt, p.yHeadRotO, p.yHeadRot), Mth.lerp(pt, p.xRotO, p.getXRot()), p.walkAnimation.position(pt),
                p.walkAnimation.speed(pt), p.getAttackAnim(pt), p.isCrouching(), p.tickCount + pt);
    }

    static void record(Player p) {
        RING[head] = live(p, 1.0F);
        head = (head + 1) % RING.length;
        size = Math.min(size + 1, RING.length);
    }

    /** The pose of {@code p} {@code delay} ticks ago (only remembered for the local player). */
    public static Snap get(Player p, float pt, int delay) {
        if (delay <= 0 || p != Minecraft.getInstance().player || delay >= size) return live(p, pt);
        Snap s = RING[Math.floorMod(head - 1 - delay, RING.length)];
        return s != null ? s : live(p, pt);
    }

    private PoseHistory() {}
}
