package com.krolasyon.futbol.client;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.game.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

import java.lang.reflect.Field;

/**
 * Optional football cameras (K): 0 = normal Minecraft, 1 = TV broadcast from the side line following the ball,
 * 2 = FIFA style elevated camera behind the player looking towards the attacking goal. While a football camera is active
 * WASD moves relative to the screen. Goal replays use their own cinematic orbit.
 */
public final class CameraController {
    private CameraController() {}

    public static final String[] NAMES = {"Normal", "TV Yayını", "Üstten (FIFA)"};

    private static Field posField, blockField;
    private static Vec3 cur;
    private static float yaw, pitch;
    private static long lastNanos;
    private static CameraType saved;
    private static float freeYaw;

    public static boolean active() { return ClientState.cameraMode != 0 || Replay.active; }

    public static float yaw() { return yaw; }

    public static void cycle() {
        ClientState.cameraMode = (ClientState.cameraMode + 1) % NAMES.length;
        ClientState.cameraToastUntil = ClientState.clientTicks + 50;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) freeYaw = p.getYRot();
        cur = null;
        ClientState.savePrefs();
    }

    public static void clientTick(Minecraft mc) {
        if (Replay.active) return;
        boolean want = ClientState.cameraMode != 0;
        if (want && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            if (saved == null) saved = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!want && saved != null) {
            mc.options.setCameraType(saved);
            saved = null;
        }
    }

    private static Vec3 ballPos(float pt) {
        FootballEntity b = ClientState.nearestBall;
        if (b == null || ClientState.nearestBallDist > 80) return null;
        return new Vec3(Mth.lerp(pt, b.xo, b.getX()), Mth.lerp(pt, b.yo, b.getY()), Mth.lerp(pt, b.zo, b.getZ()));
    }

    public static void apply(ViewportEvent.ComputeCameraAngles e) {
        if (!active()) {
            cur = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        float pt = (float) e.getPartialTick();
        Vec3 pp = new Vec3(Mth.lerp(pt, p.xo, p.getX()), Mth.lerp(pt, p.yo, p.getY()), Mth.lerp(pt, p.zo, p.getZ()));
        Vec3 target, desired;
        double smooth = 4.0;
        if (Replay.active) {
            target = Replay.focus.add(0, 0.6, 0);
            double a = Replay.progress() * 0.012 + 0.6;
            desired = target.add(Math.cos(a) * 10.5, 4.8, Math.sin(a) * 10.5);
            smooth = 3.0;
        } else if (ClientState.cameraMode == 1) {
            Vec3 b = ballPos(pt);
            target = b != null ? b.add(pp.subtract(b).scale(0.25)) : pp;
            target = target.add(0, 1, 0);
            if (ClientState.hasPitch) {
                double cx = ClientState.px + 0.5;
                double zSide = ClientState.pz - 20 - 13;
                double x = Mth.clamp(target.x, cx - 26, cx + 26);
                desired = new Vec3(x, ClientState.py + 1 + 15, Math.min(zSide, target.z - 21));
            } else {
                desired = target.add(0, 13, -19);
            }
        } else {
            Team t = ClientState.myTeam();
            Vec3 dir;
            if (t.playing() && ClientState.hasPitch && ClientState.state != 0) dir = new Vec3(t.attackDir(), 0, 0);
            else dir = Vec3.directionFromRotation(0, freeYaw);
            target = pp.add(dir.scale(5)).add(0, 0.8, 0);
            desired = pp.subtract(dir.scale(9.5)).add(0, 11.5, 0);
            smooth = 5.0;
        }
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.016 : Math.min(0.1, (now - lastNanos) / 1.0E9);
        lastNanos = now;
        if (cur == null) cur = desired;
        double k = 1 - Math.exp(-dt * smooth);
        cur = cur.add(desired.subtract(cur).scale(k));
        Vec3 d = target.subtract(cur);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float wantYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float wantPitch = (float) -Math.toDegrees(Math.atan2(d.y, h));
        yaw = wantYaw;
        pitch = wantPitch;
        setPosition(e.getCamera(), cur);
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.setRoll(e.getRoll());
    }

    private static void setPosition(Camera cam, Vec3 pos) {
        try {
            if (posField == null) {
                for (Field f : Camera.class.getDeclaredFields()) {
                    if (f.getType() == Vec3.class && posField == null) {
                        f.setAccessible(true);
                        posField = f;
                    }
                    if (f.getType() == BlockPos.MutableBlockPos.class && blockField == null) {
                        f.setAccessible(true);
                        blockField = f;
                    }
                }
            }
            if (posField != null) posField.set(cam, pos);
            if (blockField != null) ((BlockPos.MutableBlockPos) blockField.get(cam)).set(pos.x, pos.y, pos.z);
        } catch (Exception ex) {
            ClientState.cameraMode = 0;
        }
    }

    /** WASD relative to the screen while a football camera is active */
    public static void remapInput(LocalPlayer p, Input in) {
        if (ClientState.cameraMode == 0 || Replay.active || cur == null) return;
        float f = in.forwardImpulse, l = in.leftImpulse;
        if (Math.abs(f) < 0.01F && Math.abs(l) < 0.01F) return;
        double ry = Math.toRadians(yaw);
        double fx = -Math.sin(ry), fz = Math.cos(ry);
        double lx = Math.cos(ry), lz = Math.sin(ry);
        double dx = fx * f + lx * l, dz = fz * f + lz * l;
        float len = (float) Math.min(1.0, Math.sqrt(dx * dx + dz * dz));
        float newYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(newYaw);
        p.setYHeadRot(newYaw);
        p.setXRot(8F);
        in.forwardImpulse = len;
        in.leftImpulse = 0;
    }
}
