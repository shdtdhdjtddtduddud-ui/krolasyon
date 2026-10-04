package com.rabona.arena.client;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.game.Pitch;
import com.rabona.arena.game.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

import java.lang.reflect.Method;

/**
 * Istege bagli futbol kameralari (O tusu):
 * 0 = Normal (Minecraft), 1 = TV Yayini (yandan, yuksekten), 2 = Ustten Takip (arkadan-yukaridan), 3 = Kusbakisi.
 * Futbol kameralarinda hareket kameraya goredir (W = ekranin yukarisi), oyuncu kosu yonune doner.
 */
public final class CameraCtl {
    private static Vec3 camPos, lookPos;
    private static float camYaw;
    private static CameraType saved;
    private static boolean forced;
    private static Method setPos, setRot;

    private CameraCtl() {}

    public static int mode() {
        // 2 kisilik oyunda iki oyuncuyu da gormek icin TV kamerasi
        if (ClientState.cameraMode == 0 && PadControl.p2Active()) return 1;
        return ClientState.cameraMode;
    }

    /** Hareket yonu icin kamera acisi (futbol kamerasi yoksa oyuncunun bakisi). */
    public static float yawFor(LocalPlayer p) {
        return mode() > 0 && camPos != null ? camYaw : p.getYRot();
    }

    public static void cycle() {
        ClientState.cameraMode = (ClientState.cameraMode + 1) % 4;
        ClientState.save();
        camPos = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            mc.player.displayClientMessage(Component.translatable("msg.rabonaarena.camera", Component.translatable("camera.rabonaarena." + mode())), true);
    }

    /** Futbol kamerasi su an etkin mi? */
    public static boolean active() {
        return Replay.playing() || mode() > 0;
    }

    private static void ensureDetached(boolean want) {
        Minecraft mc = Minecraft.getInstance();
        if (want && !forced) {
            saved = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            forced = true;
        } else if (!want && forced) {
            mc.options.setCameraType(saved == null ? CameraType.FIRST_PERSON : saved);
            forced = false;
        }
    }

    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean on = active() && p != null && mc.level != null;
        ensureDetached(on);
        if (!on) {
            camPos = null;
            return;
        }
        float partial = (float) e.getPartialTick();
        Vec3 me = p.getPosition(partial);
        Vec3 tPos, tLook;
        if (Replay.playing()) {
            Vec3 f = Replay.focus();
            if (f == null) f = me;
            float ang = Replay.cursor() * 0.025f + 0.6f;
            tPos = f.add(Math.cos(ang) * 9, 4.5, Math.sin(ang) * 9);
            tLook = f.add(0, 0.6, 0);
        } else {
            BallEntity ball = nearestBall(mc, me);
            Vec3 ballPos = ball == null ? null : ball.getPosition(partial);
            Pitch pitch = ClientState.pitch;
            Vec3 att = attackDir(p);
            switch (mode()) {
                case 1 -> { // TV yayini
                    Vec3 focus = ballPos != null && ballPos.distanceTo(me) < 45 ? ballPos.scale(0.7).add(me.scale(0.3)) : me;
                    if (pitch != null && pitch.inside(me, 20)) {
                        double a = Mth.clamp(pitch.a(focus), -Pitch.HALF_LEN, Pitch.HALF_LEN);
                        double fb = pitch.b(focus);
                        tPos = pitch.world(a, Math.max(-(Pitch.HALF_WID + 8), fb - 15), pitch.surfaceY() + 10.5);
                    } else {
                        tPos = focus.add(0, 9, -13);
                    }
                    tLook = focus.add(0, 0.5, 0);
                }
                case 2 -> { // ustten takip
                    Vec3 dir = att != null ? att : Vec3.directionFromRotation(0, p.getYRot());
                    dir = new Vec3(dir.x, 0, dir.z).normalize();
                    tPos = me.subtract(dir.scale(6.5)).add(0, 4.8, 0);
                    Vec3 aim = ballPos != null && ballPos.distanceTo(me) < 20 ? ballPos.scale(0.35).add(me.scale(0.65)) : me;
                    tLook = aim.add(dir.scale(5)).add(0, 0.5, 0);
                }
                default -> { // kusbakisi
                    Vec3 focus = ballPos != null && ballPos.distanceTo(me) < 25 ? me.scale(0.7).add(ballPos.scale(0.3)) : me;
                    Vec3 dir = att != null ? att : new Vec3(0, 0, 1);
                    tPos = focus.subtract(dir.scale(5)).add(0, 16, 0);
                    tLook = focus;
                }
            }
        }
        if (camPos == null) {
            camPos = tPos;
            lookPos = tLook;
        } else {
            camPos = camPos.lerp(tPos, 0.08);
            lookPos = lookPos.lerp(tLook, 0.15);
        }
        Vec3 d = lookPos.subtract(camPos);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        float pitchDeg = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        camYaw = yaw;
        apply(e.getCamera(), camPos, yaw, pitchDeg);
        e.setYaw(yaw);
        e.setPitch(pitchDeg);
        e.setRoll(0);
    }

    private static void apply(Camera c, Vec3 pos, float yaw, float pitch) {
        try {
            if (setPos == null) {
                setPos = Camera.class.getDeclaredMethod(name("setPosition", "m_90581_"), Vec3.class);
                setPos.setAccessible(true);
            }
            if (setRot == null) {
                setRot = Camera.class.getDeclaredMethod(name("setRotation", "m_90572_"), float.class, float.class);
                setRot.setAccessible(true);
            }
            setPos.invoke(c, pos);
            setRot.invoke(c, yaw, pitch);
        } catch (Throwable t) {
            // isimler bulunamazsa imzaya gore ara
            try {
                for (Method m : Camera.class.getDeclaredMethods()) {
                    Class<?>[] ps = m.getParameterTypes();
                    if (m.getReturnType() == void.class && ps.length == 1 && ps[0] == Vec3.class) {
                        m.setAccessible(true);
                        setPos = m;
                    }
                    if (m.getReturnType() == void.class && ps.length == 2 && ps[0] == float.class && ps[1] == float.class
                            && java.lang.reflect.Modifier.isProtected(m.getModifiers())) {
                        m.setAccessible(true);
                        setRot = m;
                    }
                }
                if (setPos != null) setPos.invoke(c, pos);
                if (setRot != null) setRot.invoke(c, yaw, pitch);
            } catch (Throwable t2) {
                RabonaArena.LOG.error("Kamera ayarlanamadi", t2);
                ClientState.cameraMode = 0;
            }
        }
    }

    private static String name(String dev, String srg) {
        return net.minecraftforge.fml.loading.FMLEnvironment.production ? srg : dev;
    }

    /** Kameraya gore hareket: W ekranin yukarisi. Oyuncu kosu yonune doner. */
    public static void onInput(LocalPlayer p, Input in) {
        if (mode() == 0 || Replay.playing() || camPos == null) return;
        float fwd = (in.up ? 1 : 0) - (in.down ? 1 : 0);
        float str = (in.left ? 1 : 0) - (in.right ? 1 : 0);
        if (fwd == 0 && str == 0) return;
        double yr = Math.toRadians(camYaw);
        double fx = -Math.sin(yr), fz = Math.cos(yr);
        double lx = Math.cos(yr), lz = Math.sin(yr);
        double dx = fx * fwd + lx * str, dz = fz * fwd + lz * str;
        float target = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float cur = p.getYRot();
        float diff = Mth.wrapDegrees(target - cur);
        float yaw = cur + Mth.clamp(diff, -35, 35);
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
        p.setXRot(Mth.clamp(p.getXRot(), -20, 25));
        float mag = in.shiftKeyDown ? 0.3f : 1f;
        in.forwardImpulse = mag;
        in.leftImpulse = 0;
    }

    private static BallEntity nearestBall(Minecraft mc, Vec3 me) {
        BallEntity best = null;
        double bd = 60 * 60;
        for (BallEntity b : mc.level.getEntitiesOfClass(BallEntity.class, new net.minecraft.world.phys.AABB(me, me).inflate(60))) {
            double d = b.position().distanceToSqr(me);
            if (d < bd) { bd = d; best = b; }
        }
        return best;
    }

    private static Vec3 attackDir(LocalPlayer p) {
        Pitch pitch = ClientState.pitch;
        if (pitch == null || ClientState.match == null) return null;
        Team t = ClientState.teamOf(p);
        if (!t.playing()) return null;
        int s = t == Team.RED ? 1 : -1;
        if (ClientState.match.swapped()) s = -s;
        return pitch.axisA().scale(s);
    }
}
