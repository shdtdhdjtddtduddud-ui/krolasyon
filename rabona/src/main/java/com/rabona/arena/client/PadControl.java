package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rabona.arena.game.Move;
import com.rabona.arena.net.C2S;
import com.rabona.arena.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Kumanda kontrolu (FIFA klasik duzen) ve ayni bilgisayarda 2 kisilik oyun.
 * 1. kumanda Minecraft oyuncusunu, 2. kumanda sunucudaki "2. oyuncu" futbolcusunu yonetir.
 * Tek kumanda varsa ve 2. oyuncu aciksa: klavye = 1. oyuncu, kumanda = 2. oyuncu.
 */
public final class PadControl {
    private static final class Ctl {
        final boolean p2;
        boolean charging;
        int chargeTicks;
        final Combo combo = new Combo();
        float lastX, lastZ;
        boolean lastSprint;
        int sendTimer;

        Ctl(boolean p2) { this.p2 = p2; }
    }

    private static final Ctl P1 = new Ctl(false), P2 = new Ctl(true);
    private static final Combo KEY_COMBO = new Combo();
    public static Gamepads.Pad p1Pad, p2Pad;
    public static float p2Charge;
    private static Component lastCombo;
    private static int lastComboTime;

    private PadControl() {}

    public static boolean p2Active() {
        return ClientState.p2Mode > 0 && ClientState.match != null && ClientState.match.p2Bot() >= 0;
    }

    public static Component lastCombo() {
        return ClientState.clientTicks - lastComboTime < 30 ? lastCombo : null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Gamepads.poll();
        List<Gamepads.Pad> pads = Gamepads.pads();
        if (p2Active()) {
            p2Pad = pads.size() >= 2 ? pads.get(1) : pads.isEmpty() ? null : pads.get(0);
            p1Pad = pads.size() >= 2 ? pads.get(0) : null;
        } else {
            p1Pad = pads.isEmpty() ? null : pads.get(0);
            p2Pad = null;
        }
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (mc.screen != null) {
            if (p1Pad != null && p1Pad.pressed(Gamepads.START)) mc.setScreen(null);
            return;
        }
        if (p1Pad != null) handle(P1, p1Pad, p);
        if (p2Pad != null) handle(P2, p2Pad, p);
        // klavye kombolari: yon oklari
        int dir = Keys.COMBO_UP.isDown() ? 0 : Keys.COMBO_RIGHT.isDown() ? 1 : Keys.COMBO_DOWN.isDown() ? 2 : Keys.COMBO_LEFT.isDown() ? 3 : -1;
        Combo.Result r = KEY_COMBO.feed(dir);
        if (r != null) fireCombo(r, false);
    }

    private static void handle(Ctl c, Gamepads.Pad pad, LocalPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        int id = c.p2 ? ClientState.match.p2Bot() : p.getId();
        boolean hasBall = ClientAnims.CONTROLLERS.contains(id);
        boolean sprint = pad.rt > 0.5f;
        // hareket (2. oyuncu: sunucuya dunya yonu gonder)
        if (c.p2) {
            Vec3 d = worldDir(pad.lx, pad.ly, CameraCtl.yawFor(p));
            float x = (float) d.x, z = (float) d.z;
            if (--c.sendTimer <= 0 || Math.abs(x - c.lastX) > 0.05f || Math.abs(z - c.lastZ) > 0.05f || sprint != c.lastSprint) {
                Net.toServer(new C2S.Pad(x, z, sprint));
                c.lastX = x;
                c.lastZ = z;
                c.lastSprint = sprint;
                c.sendTimer = 4;
            }
        } else if (sprint && (Math.abs(pad.lx) + Math.abs(pad.ly)) > 0.6f) {
            p.setSprinting(true);
        }
        // sag cubuk: topla kombo, topsuz (normal kamera) bakis
        if (hasBall || CameraCtl.mode() > 0 || c.p2) {
            Combo.Result r = c.combo.feed(Combo.dirOf(pad.rx, pad.ry));
            if (r != null) fireCombo(r, c.p2);
        } else {
            p.turn(pad.rx * 18, pad.ry * 12);
        }
        // B: sut (basili tut) / top calma
        if (pad.down[Gamepads.B] && (hasBall || c.charging)) {
            if (!c.charging) {
                c.charging = true;
                c.chargeTicks = 0;
            }
            c.chargeTicks++;
        } else if (c.charging) {
            c.charging = false;
            float power = c.chargeTicks < 3 ? 0.35f : Math.min(1.35f, c.chargeTicks / 22f);
            int slot = pad.down[Gamepads.RB] ? 1 : pad.lt > 0.5f ? 2 : 0;
            send(1, ClientState.shots(c.p2)[slot], power, 0, false, c.p2);
        } else if (pad.pressed(Gamepads.B)) {
            send(4, Move.TACKLE, 1, 0, false, c.p2);
        }
        float ch = c.charging ? Math.min(1.35f, c.chargeTicks / 22f) : 0;
        if (c.p2) {
            p2Charge = ch;
        } else if (c.charging) {
            ClientInput.charging = true;
            ClientInput.charge = ch;
            ClientInput.chargingSlot = pad.down[Gamepads.RB] ? 1 : pad.lt > 0.5f ? 2 : 0;
        } else if (ClientInput.charging && !Keys.SHOOT1.isDown() && !Keys.SHOOT2.isDown() && !Keys.SHOOT3.isDown()) {
            ClientInput.charging = false;
        }
        if (pad.pressed(Gamepads.A)) {
            if (hasBall) send(2, Move.PASS_SHORT, 1, 0, false, c.p2);
            else if (!c.p2) send(5, Move.CALL_PASS, 1, 0, false, false);
        }
        if (pad.pressed(Gamepads.Y) && hasBall) send(2, Move.PASS_THROUGH, 1, 0, true, c.p2);
        if (pad.pressed(Gamepads.X)) {
            if (hasBall) send(3, Move.PASS_LOB, 1, 0, pad.down[Gamepads.RB], c.p2);
            else send(4, Move.SLIDE, 1, 0, true, c.p2);
        }
        if (pad.pressed(Gamepads.LB)) send(6, Move.CALL_PASS, 1, 0, false, c.p2);
        if (pad.pressed(Gamepads.UP)) send(0, ClientState.ability(c.p2), 1, 0, false, c.p2);
        if (pad.pressed(Gamepads.DOWN)) send(0, ClientState.celebration(c.p2), 1, 0, false, c.p2);
        if (pad.pressed(Gamepads.LEFT)) send(0, ClientState.slots(c.p2)[0], 1, -1, false, c.p2);
        if (pad.pressed(Gamepads.RIGHT)) send(0, ClientState.slots(c.p2)[1], 1, 1, false, c.p2);
        if (!c.p2 && pad.pressed(Gamepads.START)) mc.setScreen(new MatchScreen());
        if (pad.pressed(Gamepads.BACK)) CameraCtl.cycle();
    }

    private static void fireCombo(Combo.Result r, boolean p2) {
        send(0, r.move(), 1, r.side(), false, p2);
        lastCombo = Component.literal(p2 ? "P2 " : "").append(r.move().title());
        lastComboTime = ClientState.clientTicks;
    }

    private static void send(int kind, Move m, float power, int side, boolean mod, boolean p2) {
        Net.toServer(new C2S.Act(kind, m.ordinal(), power, side, mod, p2));
    }

    /** Cubuk -> dunya yonu (kameraya gore; yukari = ekranin yukarisi). */
    static Vec3 worldDir(float lx, float ly, float camYaw) {
        float fwd = -ly, str = -lx;
        if (fwd * fwd + str * str < 0.04f) return Vec3.ZERO;
        double yr = Math.toRadians(camYaw);
        double fx = -Math.sin(yr), fz = Math.cos(yr);
        double lxv = Math.cos(yr), lzv = Math.sin(yr);
        Vec3 d = new Vec3(fx * fwd + lxv * str, 0, fz * fwd + lzv * str);
        double len = Math.min(1, d.length());
        return d.normalize().scale(len);
    }

    /** 1. kumanda: Minecraft hareket girdisine analog cubuk. */
    public static void applyP1(LocalPlayer p, Input in) {
        Gamepads.Pad pad = p1Pad;
        if (pad == null || (pad.lx == 0 && pad.ly == 0)) return;
        if (CameraCtl.mode() > 0) {
            Vec3 d = worldDir(pad.lx, pad.ly, CameraCtl.yawFor(p));
            double mag = d.length();
            if (mag < 0.05) return;
            float target = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float yaw = p.getYRot() + Mth.clamp(Mth.wrapDegrees(target - p.getYRot()), -35, 35);
            p.setYRot(yaw);
            p.setYHeadRot(yaw);
            in.forwardImpulse = (float) mag;
            in.leftImpulse = 0;
        } else {
            in.forwardImpulse = -pad.ly;
            in.leftImpulse = -pad.lx;
        }
    }

    /** P1 / P2 isaretleri (kafanin ustunde). */
    public static void renderMarkers(PoseStack ps, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean show = p2Active() || p1Pad != null;
        if (!show) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        if (CameraCtl.active()) marker(ps, buf, mc.player, partial, cam, "P1", 0xFF5AD2FF);
        if (p2Active()) {
            Entity e = mc.level.getEntity(ClientState.match.p2Bot());
            if (e != null) marker(ps, buf, e, partial, cam, "P2", 0xFFFFC94A);
        }
        buf.endBatch();
    }

    private static void marker(PoseStack ps, MultiBufferSource buf, Entity e, float partial, Vec3 cam, String text, int color) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 pos = e.getPosition(partial);
        ps.pushPose();
        ps.translate(pos.x - cam.x, pos.y - cam.y + e.getBbHeight() + 0.95, pos.z - cam.z);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float sc = 0.018f * (float) Mth.clamp(pos.distanceTo(cam) / 14, 1, 1.5);
        ps.scale(-sc, -sc, sc);
        Font f = mc.font;
        Matrix4f m = ps.last().pose();
        float x = -f.width(text) / 2f;
        f.drawInBatch(text, x, 0, color, true, m, buf, Font.DisplayMode.NORMAL, 0x60000000, LightTexture.FULL_BRIGHT);
        ps.popPose();
    }
}
