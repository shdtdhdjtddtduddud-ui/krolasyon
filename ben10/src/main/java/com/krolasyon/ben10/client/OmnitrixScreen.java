package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** The Omnitrix dial: aliens on a rotating ring, hover/scroll to pick, click (or Enter) to slam the core down. */
public class OmnitrixScreen extends Screen {
    private static final ResourceLocation DIAL = new ResourceLocation(Ben10.MODID, "textures/gui/dial.png");
    private static final ResourceLocation CORE = new ResourceLocation(Ben10.MODID, "textures/gui/core.png");
    private int selected;
    private float spin, spinO;
    private int open;

    public OmnitrixScreen() {
        super(Component.translatable("gui.krolben10.dial"));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            OmniData d = OmniData.get(mc.player);
            if (d.alien != null) selected = d.alien.ordinal();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.S_DIAL_OPEN.get(), 1F, 0.8F));
        }
        spin = spinO = target();
    }

    private float target() {
        return -selected * 360F / Alien.values().length;
    }

    @Override
    public void tick() {
        open++;
        spinO = spin;
        float t = target();
        float diff = Mth.wrapDegrees(t - spin);
        spin += diff * 0.35F;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void select(int i) {
        int n = Alien.values().length;
        i = Math.floorMod(i, n);
        if (i != selected) {
            selected = i;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.S_DIAL.get(), 1F, 1F + i * 0.03F));
        }
    }

    private int hovered(int mx, int my) {
        int cx = width / 2, cy = height / 2;
        double dx = mx - cx, dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 40 || dist > 120) return -1;
        int n = Alien.values().length;
        double ang = Math.toDegrees(Math.atan2(dy, dx)) + 90 - Mth.lerp(1F, spinO, spin);
        return Math.floorMod((int) Math.round(ang / (360.0 / n)), n);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        select(selected + (delta < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int cx = width / 2, cy = height / 2;
        double dist = Math.hypot(mx - cx, my - cy);
        Minecraft mc = Minecraft.getInstance();
        OmniData d = mc.player == null ? null : OmniData.get(mc.player);
        if (dist < 36) {
            confirm();
            return true;
        }
        int h = hovered((int) mx, (int) my);
        if (h >= 0) {
            if (h == selected) confirm();
            else select(h);
            return true;
        }
        if (d != null && d.alien != null && my > cy + 125 && my < cy + 141 && Math.abs(mx - cx) < 60) {
            Net.CHANNEL.sendToServer(new Net.Select(-1));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
            select(selected - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
            select(selected + 1);
            return true;
        }
        if (ClientSetup.DIAL.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void confirm() {
        Net.CHANNEL.sendToServer(new Net.Select(selected));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xA0000800);
        int cx = width / 2, cy = height / 2;
        float sp = Mth.lerp(pt, spinO, spin);
        float pop = Math.min(1F, (open + pt) / 6F);
        int R = (int) (88 * (0.6F + 0.4F * pop));
        // dial plate
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(sp));
        g.blit(DIAL, -128, -128, 0, 0, 256, 256, 256, 256);
        g.pose().popPose();
        int n = Alien.values().length;
        int hov = hovered(mx, my);
        for (int i = 0; i < n; i++) {
            double ang = Math.toRadians(i * 360.0 / n + sp - 90);
            int x = cx + (int) (Math.cos(ang) * R), y = cy + (int) (Math.sin(ang) * R);
            Alien a = Alien.values()[i];
            boolean sel = i == selected;
            int s = sel ? 34 : (i == hov ? 30 : 26);
            g.fill(x - s / 2 - 2, y - s / 2 - 2, x + s / 2 + 2, y + s / 2 + 2, sel ? 0xFF66FF33 : 0xFF203820);
            g.fill(x - s / 2, y - s / 2, x + s / 2, y + s / 2, 0xFF0C140C);
            g.blit(OmniHud.portrait(a), x - s / 2, y - s / 2, 0, 0, s, s, s, s);
        }
        // core with the selected alien hologram
        Alien a = Alien.values()[selected];
        g.blit(CORE, cx - 36, cy - 36, 0, 0, 72, 72, 72, 72);
        g.blit(OmniHud.portrait(a), cx - 24, cy - 24, 0, 0, 48, 48, 48, 48);
        // info panel
        int px = cx + 132, py = cy - 70;
        if (px + 150 > width) px = width - 152;
        g.fill(px, py, px + 150, py + 104, 0xC0081008);
        g.fill(px, py, px + 150, py + 1, 0xFF55FF33);
        g.drawString(font, a.displayName().copy().withStyle(ChatFormatting.BOLD), px + 5, py + 5, 0xFF000000 | a.color, true);
        for (int i = 0; i < 4; i++) {
            g.drawString(font, Component.literal((i + 1) + ". ").append(a.abilityName(i)), px + 5, py + 20 + i * 12, 0xFFDDFFDD, true);
        }
        g.drawString(font, Component.translatable("guard.krolben10." + a.id).withStyle(ChatFormatting.AQUA), px + 5, py + 70, 0xFF99DDFF, true);
        g.drawWordWrap(font, Component.translatable("passive.krolben10." + a.id), px + 5, py + 82, 140, 0xFFB0B0B0);
        g.drawCenteredString(font, Component.translatable("gui.krolben10.hint"), cx, cy + 112, 0xFF99CC99);
        Minecraft mc = Minecraft.getInstance();
        OmniData d = mc.player == null ? null : OmniData.get(mc.player);
        if (d != null && d.alien != null) {
            boolean over = my > cy + 125 && my < cy + 141 && Math.abs(mx - cx) < 60;
            g.fill(cx - 60, cy + 125, cx + 60, cy + 141, over ? 0xFF3A6A2A : 0xFF1A3A12);
            g.drawCenteredString(font, Component.translatable("gui.krolben10.human"), cx, cy + 129, 0xFFFFFFFF);
        } else if (d != null && d.recharge > 0) {
            g.drawCenteredString(font, Component.translatable("msg.krolben10.recharging", (d.recharge + 19) / 20), cx, cy + 129, 0xFFFF5544);
        }
        super.render(g, mx, my, pt);
    }
}
