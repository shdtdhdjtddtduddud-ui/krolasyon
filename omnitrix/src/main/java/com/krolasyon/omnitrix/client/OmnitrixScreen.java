package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Ability;
import com.krolasyon.omnitrix.Alien;
import com.krolasyon.omnitrix.ModRegistry;
import com.krolasyon.omnitrix.net.Net;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * The Omnitrix dial: pops up from the watch, shows the alien silhouettes around the face like the cartoon and a
 * rotating hologram of the selected form. Mouse direction / wheel / arrow keys select, click or Enter slams the core down.
 */
public class OmnitrixScreen extends Screen {
    private final long opened = Util.getMillis();
    private int selected;
    private float shownAngle;
    private long confirmAt = -1;

    public OmnitrixScreen() { super(Component.translatable("screen.omnitrix.title")); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.ACTIVATE.get(), 1F, 0.9F));
    }

    private float t() { return (Util.getMillis() - opened) / 50F; }

    private int radius() { return Math.min(width, height) / 3; }

    private static float slotAngle(int i) { return (float) (i * Math.PI * 2 / Alien.VALUES.length - Math.PI / 2); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float time = t();
        float pop = Mth.clamp(time / 8F, 0, 1);
        float popEase = pop < 1 ? 1 - (1 - pop) * (1 - pop) * (1 - 2.2F * pop) : 1;
        int cx = width / 2, cy = height / 2;
        // background: dark green tinted vignette + scanlines
        g.fillGradient(0, 0, width, height, 0xC0000800, 0xD0001400);
        for (int y = (int) (time * 2) % 4; y < height; y += 4) g.fill(0, y, width, y + 1, 0x1030FF30);
        int r = (int) (radius() * popEase);
        // dial rings
        ring(g, cx, cy, r + 26, 0x8040FF40, 2);
        ring(g, cx, cy, r - 26, 0x5040FF40, 1);
        for (int i = 0; i < 60; i++) {
            float a = (float) (i * Math.PI / 30 + time * 0.01);
            int x0 = cx + (int) (Math.cos(a) * (r + 30)), y0 = cy + (int) (Math.sin(a) * (r + 30));
            int x1 = cx + (int) (Math.cos(a) * (r + (i % 5 == 0 ? 40 : 34))), y1 = cy + (int) (Math.sin(a) * (r + (i % 5 == 0 ? 40 : 34)));
            line(g, x0, y0, x1, y1, i % 5 == 0 ? 0xC060FF60 : 0x6040FF40);
        }
        // hologram dial texture (Higgsfield)
        // the texture has a black background: add it on top instead of alpha blending it
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(1, 1, 1, 0.6F * pop);
        int ds = (int) (r * 1.1F);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(time * 0.6F));
        g.blit(FxRenderer.DIAL, -ds, -ds, ds * 2, ds * 2, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
        g.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.defaultBlendFunc();
        // the 3d watch at the bottom of the hologram
        // mouse selection
        if (confirmAt < 0) {
            double dx = mx - cx, dy = my - cy;
            if (dx * dx + dy * dy > 30 * 30) {
                double ang = Math.atan2(dy, dx) + Math.PI / 2;
                int s = Math.floorMod((int) Math.round(ang / (Math.PI * 2) * Alien.VALUES.length), Alien.VALUES.length);
                if (s != selected) select(s);
            }
        }
        float target = slotAngle(selected);
        shownAngle = Mth.rotLerp(0.3F, shownAngle * Mth.RAD_TO_DEG, target * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        // slots
        for (int i = 0; i < Alien.VALUES.length; i++) {
            Alien a = Alien.VALUES[i];
            float ang = slotAngle(i);
            int sx = cx + (int) (Math.cos(ang) * r), sy = cy + (int) (Math.sin(ang) * r);
            boolean sel = i == selected;
            int size = sel ? 30 : 22;
            disc(g, sx, sy, size + 3, sel ? 0xFF60FF60 : 0xFF1E5A1E);
            disc(g, sx, sy, size, 0xFF000000);
            RenderSystem.enableBlend();
            g.blit(sel ? a.icon() : a.silhouette(), sx - size + 4, sy - size + 4, (size - 4) * 2, (size - 4) * 2, 0, 0, 128, 128, 128, 128);
        }
        // pointer from the centre to the selected slot
        line(g, cx, cy, cx + (int) (Math.cos(shownAngle) * (r - 34)), cy + (int) (Math.sin(shownAngle) * (r - 34)), 0xC080FF80);
        // centre: rotating hologram of the selected alien
        Alien sel = Alien.VALUES[selected];
        g.flush();
        g.pose().pushPose();
        g.pose().translate(cx, cy + r * 0.45F, 300);
        float holo = Math.min(r * 0.95F, 140);
        g.pose().scale(holo, -holo, holo);
        g.pose().mulPose(Axis.YP.rotationDegrees(time * 3F));
        g.pose().translate(0, 0.45F, 0);
        Lighting.setupForEntityInInventory();
        boolean confirmed = confirmAt >= 0;
        AlienRenderer.renderPreview(sel, g.pose(), g.bufferSource(), 0.9F, time, !confirmed);
        g.flush();
        g.pose().popPose();
        // the watch itself, in front, tilted towards the viewer
        g.pose().pushPose();
        g.pose().translate(cx, height - 34, 350);
        float ws = 70 * popEase;
        g.pose().scale(ws, -ws, ws);
        g.pose().mulPose(Axis.XP.rotationDegrees(65 + (confirmed ? 25 : 0)));
        g.pose().mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 0.05F) * 15));
        AlienRenderer.renderWatch(g.pose(), g.bufferSource(), 0xF000F0, 1F, true);
        g.flush();
        g.pose().popPose();
        Lighting.setupFor3DItems();
        // texts
        g.drawCenteredString(font, sel.displayName().copy().withStyle(s -> s.withColor(sel.color).withBold(true)), cx, cy - r - 52, 0xFFFFFF);
        int y = cy - r - 40;
        for (int i = 0; i < 4; i++) {
            Ability ab = sel.ability(i);
            if (ab == null) continue;
            Component line = Component.literal(ClientEvents.ABILITY[i].getTranslatedKeyMessage().getString() + ": ").withStyle(ChatFormatting.DARK_GREEN)
                    .append(ab.displayName().copy().withStyle(ChatFormatting.GREEN));
            g.drawString(font, line, 8, 30 + i * 11, 0xFFFFFF);
        }
        g.drawString(font, Component.translatable("passive.omnitrix." + sel.id).withStyle(ChatFormatting.GRAY), 8, 30 + 4 * 11 + 3, 0xFFFFFF);
        g.drawString(font, Component.translatable("screen.omnitrix.block", ClientEvents.BLOCK.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GREEN), 8, 30 + 5 * 11 + 6, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.omnitrix.hint"), cx, height - 12, 0x60FF60);
        g.drawString(font, title.copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), 8, 10, 0xFFFFFF);
        // confirmation flash and close
        if (confirmed) {
            float f = (Util.getMillis() - confirmAt) / 300F;
            g.fill(0, 0, width, height, ((int) (Mth.clamp(f, 0, 1) * 220) << 24) | 0x60FF60);
            if (f >= 1) {
                Net.CHANNEL.sendToServer(new Net.Action(Net.Action.TRANSFORM, selected));
                onClose();
            }
        }
    }

    private void select(int s) {
        selected = s;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.DIAL.get(), 1.1F + s * 0.03F, 0.6F));
    }

    private void confirm() {
        if (confirmAt >= 0) return;
        confirmAt = Util.getMillis();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.SELECT.get(), 1F, 1F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { confirm(); return true; }
        if (button == 1) { onClose(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        select(Math.floorMod(selected + (delta > 0 ? -1 : 1), Alien.VALUES.length));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) { select(Math.floorMod(selected + 1, Alien.VALUES.length)); return true; }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) { select(Math.floorMod(selected - 1, Alien.VALUES.length)); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) { confirm(); return true; }
        if (ClientEvents.WATCH.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    /** test hook */
    public void autoSelect(int i, boolean go) {
        selected = i;
        if (go) confirm();
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int w = (int) Math.sqrt(r * r - y * y);
            g.fill(cx - w, cy + y, cx + w + 1, cy + y + 1, color);
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int color, int thick) {
        int n = Math.max(48, r);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + thick, y + thick, color);
        }
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= n; i++) {
            int x = x0 + (x1 - x0) * i / Math.max(1, n), y = y0 + (y1 - y0) * i / Math.max(1, n);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }
}
