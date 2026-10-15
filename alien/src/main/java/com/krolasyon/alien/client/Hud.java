package com.krolasyon.alien.client;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.registry.ModItems;
import com.krolasyon.alien.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.HashMap;
import java.util.Map;

/** Screen overlays: film grain + vignette, locker view, motion tracker radar and the death close-up. */
public final class Hud {
    private Hud() {}

    private static final ResourceLocation GRAIN = new ResourceLocation(AlienMod.MODID, "textures/misc/grain.png");
    private static final ResourceLocation VIGNETTE = new ResourceLocation(AlienMod.MODID, "textures/misc/vignette.png");
    private static final ResourceLocation SLATS = new ResourceLocation(AlienMod.MODID, "textures/misc/locker_slats.png");

    // ------------------------------------------------------------------ atmosphere
    public static void atmosphere(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        float k = ClientEvents.stationBlend;
        float t = ClientEvents.threatSmooth;
        if (k < 0.02F && t < 0.02F) return;
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1F, 1F, 1F, Mth.clamp(0.5F * k + 0.45F * t, 0F, 0.95F));
        g.blit(VIGNETTE, 0, 0, w, h, 0F, 0F, 256, 256, 256, 256);
        int u = mc.level == null ? 0 : (int) ((mc.level.getGameTime() * 73L + (long) (pt * 7)) % 256);
        int v = mc.level == null ? 0 : (int) ((mc.level.getGameTime() * 151L) % 256);
        g.setColor(1F, 1F, 1F, Mth.clamp(0.13F * k + 0.1F * t, 0F, 0.3F));
        g.blit(GRAIN, 0, 0, u, v, w, h, 256, 256);
        if (t > 0.45F) {
            float pulse = (Mth.sin((mc.player == null ? 0 : mc.player.tickCount + pt) * 0.35F) * 0.5F + 0.5F) * (t - 0.45F) * 0.25F;
            g.setColor(1F, 1F, 1F, 1F);
            g.fill(0, 0, w, h, ((int) (pulse * 255) << 24) | 0x3A0000);
        }
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------------ locker
    public static void locker(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        if (!ClientState.hidden) return;
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(SLATS, 0, 0, w, h, 0F, 0F, 256, 256, 256, 256);
        RenderSystem.disableBlend();

        int cx = w / 2;
        int y = h - 64;
        boolean show = ClientState.danger || ClientState.holding || ClientState.breath < 0.999F;
        if (show) {
            int bw = 140;
            g.fill(cx - bw / 2 - 2, y - 2, cx + bw / 2 + 2, y + 6, 0xC0000000);
            int fillW = (int) (bw * ClientState.breath);
            int col = ClientState.breath < 0.3F ? 0xFFD03020 : ClientState.holding ? 0xFF7FD6FF : 0xFFBFC8C0;
            g.fill(cx - bw / 2, y, cx - bw / 2 + fillW, y + 4, col);
            Component msg = Component.translatable(ClientState.danger ? "hud.alienisolation.hold_breath" : "hud.alienisolation.breath");
            int c = ClientState.danger ? (mc.player != null && mc.player.tickCount % 10 < 5 ? 0xFFFF5040 : 0xFFFFFFFF) : 0xFFB0B8B0;
            g.drawCenteredString(mc.font, msg, cx, y - 14, c);
        }
        g.drawCenteredString(mc.font, Component.translatable("hud.alienisolation.leave"), cx, h - 40, 0xFF808880);
    }

    // ------------------------------------------------------------------ motion tracker
    private static final Map<Integer, Long> LAST_MOVE = new HashMap<>();
    private static final Map<Integer, Vec3> LAST_POS = new HashMap<>();
    private static int pulseTick;
    private static final int PULSE = 26;
    private static final float RANGE = 30F;

    private static boolean holdingTracker(LocalPlayer p) {
        return p.getMainHandItem().is(ModItems.MOTION_TRACKER.get()) || p.getOffhandItem().is(ModItems.MOTION_TRACKER.get());
    }

    /** called every client tick: movement memory + the pulse / beep sounds. */
    public static void trackerTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE + 2, 12, RANGE + 2))) {
            if (e == p) continue;
            Vec3 last = LAST_POS.put(e.getId(), e.position());
            if (last != null && last.distanceToSqr(e.position()) > 0.0004) LAST_MOVE.put(e.getId(), now);
        }
        if (now % 200 == 0) {
            LAST_POS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
            LAST_MOVE.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        }
        if (!holdingTracker(p) || ClientState.hidden) { pulseTick = 0; return; }
        pulseTick++;
        if (pulseTick % PULSE == 1) {
            p.playSound(ModSounds.TRACKER_PING.get(), 0.35F, 1.0F);
            float nearest = nearestBlip(p, now);
            if (nearest < RANGE) {
                float k = 1F - nearest / RANGE;
                p.playSound(ModSounds.TRACKER_BEEP.get(), 0.4F + k * 0.5F, 0.8F + k * 0.9F);
            }
        }
    }

    private static boolean moving(LivingEntity e, long now) {
        Long t = LAST_MOVE.get(e.getId());
        return t != null && now - t < 30;
    }

    private static float nearestBlip(LocalPlayer p, long now) {
        float best = Float.MAX_VALUE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE, 12, RANGE))) {
            if (e == p || !moving(e, now)) continue;
            float d = (float) Math.sqrt(Mth.square(e.getX() - p.getX()) + Mth.square(e.getZ() - p.getZ()));
            if (d < best) best = d;
        }
        return best;
    }

    public static void tracker(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || ClientState.hidden || !holdingTracker(p) || mc.options.hideGui) return;
        for (XenomorphEntity x : mc.level.getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(4)))
            if (x.getKillTargetId() == p.getId()) return;
        long now = mc.level.getGameTime();
        int pw = 196, ph = 112;
        int x = w / 2 - pw / 2, y = h - ph - 44;
        // casing
        g.fill(x - 9, y - 16, x + pw + 9, y + ph + 9, 0xF0262A20);
        g.fill(x - 7, y - 14, x + pw + 7, y + ph + 7, 0xFF3A3F31);
        g.fill(x - 2, y - 2, x + pw + 2, y + ph + 2, 0xFF0B0D09);
        g.fill(x, y, x + pw, y + ph, 0xFF041407);
        g.drawString(mc.font, "WEYLAND-YUTANI  M314", x, y - 12, 0xFF9AA388, false);
        // scanlines
        for (int yy = y; yy < y + ph; yy += 2) g.fill(x, yy, x + pw, yy + 1, 0x22000000);

        int cx = x + pw / 2, cy = y + ph - 6;
        float R = ph - 14;
        // range rings
        for (int m = 10; m <= 30; m += 10) {
            float r = R * m / RANGE;
            for (int a = 0; a <= 180; a += 3) {
                double rad = Math.toRadians(180 + a);
                int px = cx + (int) (Math.cos(rad) * r), py = cy + (int) (Math.sin(rad) * r);
                g.fill(px, py, px + 1, py + 1, 0xFF1F6B2E);
            }
            g.drawString(mc.font, m + "", cx + (int) r - 10, cy - 9, 0xFF2E8F42, false);
        }
        // field of view wedge lines
        for (int s : new int[]{-1, 1}) {
            for (int i = 0; i < R; i += 2) {
                double rad = Math.toRadians(270 + s * 60);
                g.fill(cx + (int) (Math.cos(rad) * i), cy + (int) (Math.sin(rad) * i), cx + (int) (Math.cos(rad) * i) + 1, cy + (int) (Math.sin(rad) * i) + 1, 0xFF184F23);
            }
        }
        // expanding pulse
        float phase = ((pulseTick % PULSE) + pt) / PULSE;
        float pr = phase * R;
        int alpha = (int) ((1F - phase) * 200);
        for (int a = 0; a <= 180; a += 2) {
            double rad = Math.toRadians(180 + a);
            int px = cx + (int) (Math.cos(rad) * pr), py = cy + (int) (Math.sin(rad) * pr);
            g.fill(px, py, px + 2, py + 1, (alpha << 24) | 0x5CFF7A);
        }
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF7CFF96);

        // blips
        float yaw = (float) Math.toRadians(p.getViewYRot(pt));
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        float nearest = Float.MAX_VALUE;
        boolean behindL = false, behindR = false;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE, 12, RANGE))) {
            if (e == p || !moving(e, now)) continue;
            Vec3 ep = e.getPosition(pt);
            double dx = ep.x - p.getX(), dz = ep.z - p.getZ();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist > RANGE) continue;
            nearest = Math.min(nearest, dist);
            double fwd = dx * fx + dz * fz, side = dx * rx + dz * rz;
            if (fwd < 0) {
                if (side < 0) behindL = true; else behindR = true;
                continue;
            }
            int bx = cx - (int) (side / RANGE * R);
            int by = cy - (int) (fwd / RANGE * R);
            float lit = Mth.clamp(1F - Math.abs(dist / RANGE - phase) * 3F, 0F, 1F) * 0.7F + 0.3F;
            int a2 = (int) (lit * 255);
            int sz = e instanceof XenomorphEntity || e.getBbHeight() > 1.2F ? 3 : 2;
            g.fill(bx - sz - 2, by - sz - 2, bx + sz + 3, by + sz + 3, ((a2 / 4) << 24) | 0x5CFF7A);
            g.fill(bx - sz, by - sz, bx + sz + 1, by + sz + 1, (a2 << 24) | 0xC8FFD2);
        }
        boolean blink = (now / 5) % 2 == 0;
        if (behindL && blink) g.drawString(mc.font, "<", x + 3, cy - 10, 0xFF7CFF96, false);
        if (behindR && blink) g.drawString(mc.font, ">", x + pw - 9, cy - 10, 0xFF7CFF96, false);
        String txt = nearest < RANGE ? String.format("%.0fm", nearest) : "--m";
        g.drawString(mc.font, txt, x + 4, y + 4, nearest < 8 ? 0xFFFF6A50 : 0xFF7CFF96, false);
    }

    // ------------------------------------------------------------------ death close-up
    public static void kill(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        for (XenomorphEntity x : mc.level.getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(4))) {
            if (x.getKillTargetId() != p.getId()) continue;
            float t = x.clientAnimId == XenomorphEntity.ANIM_KILL ? (x.tickCount + pt - x.clientAnimStart) / 20F : 1F;
            float dark = Mth.clamp(t / 1.6F, 0F, 1F) * 0.45F;
            g.fill(0, 0, w, h, ((int) (dark * 255) << 24));
            if (t > 1.58F) {
                float red = Mth.clamp(1F - (t - 1.58F) * 1.5F, 0F, 1F);
                g.fill(0, 0, w, h, ((int) (red * 200) << 24) | 0x900000);
                float black = Mth.clamp((t - 1.7F) * 2F, 0F, 1F);
                g.fill(0, 0, w, h, ((int) (black * 255) << 24));
            }
            return;
        }
    }
}
