package com.krolasyon.advgear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

/**
 * Eldeki şişelerdeki sıvının ve halkadaki anahtarların fiziği: oyuncunun dönüşü, eğilmesi, ivmesi ve kol sallaması
 * yaylı-sönümlü bir sisteme kuvvet uygular; sıvı yüzeyi gecikmeli eğilir, dalgalanır ve yavaşça durulur.
 */
public final class Slosh {
    /** Eğim (x: yana, z: ileri-geri) ve hızları; önceki tik değerleri ara kare için. */
    public static float tx, tz, vx, vz, ptx, ptz, wave, pwave;
    private static float lastYaw, lastPitch;
    private static Vec3 lastVel = Vec3.ZERO;
    private static boolean init;
    private static float lastSwing;

    private Slosh() {}

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        ptx = tx;
        ptz = tz;
        pwave = wave;
        if (!init) {
            lastYaw = p.getYRot();
            lastPitch = p.getXRot();
            init = true;
        }
        float dyaw = Mth.wrapDegrees(p.getYRot() - lastYaw), dpitch = p.getXRot() - lastPitch;
        lastYaw = p.getYRot();
        lastPitch = p.getXRot();
        Vec3 vel = p.getDeltaMovement();
        Vec3 acc = vel.subtract(lastVel);
        lastVel = vel;
        // ivmeyi bakış uzayına çevir
        double yr = Math.toRadians(p.getYRot());
        float fwd = (float) (-acc.x * Math.sin(yr) + acc.z * Math.cos(yr));
        float side = (float) (acc.x * Math.cos(yr) + acc.z * Math.sin(yr));
        float swing = p.getAttackAnim(1F);
        float swingKick = swing > lastSwing ? (swing - lastSwing) * 0.9F : 0;
        lastSwing = swing;
        float fx = dyaw * 0.012F + side * 2.2F + swingKick;
        float fz = dpitch * 0.010F - fwd * 2.2F + (float) acc.y * -1.2F;
        // yürürken adım ritmi
        float walk = (float) Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (p.onGround() && walk > 0.05F) fz += Mth.sin(p.walkDist * 3.2F) * walk * 0.35F;
        vx += fx - tx * 0.28F - vx * 0.16F;
        vz += fz - tz * 0.28F - vz * 0.16F;
        tx = Mth.clamp(tx + vx, -0.9F, 0.9F);
        tz = Mth.clamp(tz + vz, -0.9F, 0.9F);
        wave = Math.min(0.9F, wave * 0.9F + (Math.abs(fx) + Math.abs(fz)) * 0.8F);
    }

    public static float tiltX(float pt) {
        return Mth.lerp(pt, ptx, tx);
    }

    public static float tiltZ(float pt) {
        return Mth.lerp(pt, ptz, tz);
    }

    public static float wave(float pt) {
        return Mth.lerp(pt, pwave, wave);
    }
}
