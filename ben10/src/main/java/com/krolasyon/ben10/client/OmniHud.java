package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.Config;
import com.krolasyon.ben10.OmniData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Omnitrix HUD: alien portrait, time ring, four ability slots with cooldowns and the guard key; green transformation flash. */
public class OmniHud implements IGuiOverlay {
    public static ResourceLocation portrait(Alien a) {
        return new ResourceLocation(Ben10.MODID, "textures/gui/portrait/" + a.id + ".png");
    }

    static final ResourceLocation BADGE = new ResourceLocation(Ben10.MODID, "textures/gui/badge.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            drawFlash(g, sw, sh);
            return;
        }
        OmniData d = OmniData.get(mc.player);
        Font font = mc.font;
        drawFlash(g, sw, sh);
        WatchMode.hud(g, sw, sh);
        if (d.alien == null) {
            if (d.recharge > 0 && d.has(OmniData.F_WATCH)) {
                int x = 6, y = sh / 2 - 12;
                g.fill(x, y, x + 96, y + 22, 0x90100808);
                g.blit(BADGE, x + 2, y + 2, 0, 0, 18, 18, 18, 18);
                g.fill(x + 2, y + 2, x + 20, y + 20, 0x80FF2010);
                g.drawString(font, Component.translatable("hud.krolben10.recharging"), x + 24, y + 3, 0xFF5544, true);
                g.drawString(font, String.format("%.1fs", d.recharge / 20F), x + 24, y + 12, 0xFFAAAA, true);
            }
            return;
        }
        Alien a = d.alien;
        int x = 6, y = sh / 2 - 58;
        int w = 132;
        g.fill(x, y, x + w, y + 116, 0x9A0A0F0A);
        g.fill(x, y, x + w, y + 1, 0xFF55FF33);
        g.fill(x, y + 115, x + w, y + 116, 0xFF2A8A1A);
        // portrait + time bar
        g.fill(x + 3, y + 3, x + 35, y + 35, 0xFF1A2A18);
        g.blit(portrait(a), x + 3, y + 3, 0, 0, 32, 32, 32, 32);
        g.drawString(font, a.displayName(), x + 39, y + 5, 0xFF000000 | a.color, true);
        int dur = Config.durationTicks();
        float frac = dur <= 0 ? 1F : Mth.clamp(d.timeLeft / (float) dur, 0, 1);
        int barCol = frac > 0.35F ? 0xFF55FF33 : frac > 0.12F ? 0xFFFFD020 : ((mc.level.getGameTime() / 5) % 2 == 0 ? 0xFFFF3020 : 0xFF801008);
        g.fill(x + 39, y + 18, x + w - 4, y + 23, 0xFF203020);
        g.fill(x + 39, y + 18, x + 39 + (int) ((w - 43) * frac), y + 23, barCol);
        String time = dur <= 0 ? "∞" : String.format("%d:%02d", d.timeLeft / 1200, (d.timeLeft / 20) % 60);
        g.drawString(font, time, x + 39, y + 26, 0xFFCCFFCC, true);
        // abilities
        for (int i = 0; i < 4; i++) {
            int yy = y + 39 + i * 15;
            boolean active = d.ability == i;
            int cd = d.cooldown[i], max = Math.max(1, d.cooldownMax[i]);
            g.fill(x + 3, yy, x + 17, yy + 13, active ? 0xFF55FF33 : cd > 0 ? 0xFF303830 : 0xFF2F6A22);
            if (cd > 0 && !active) {
                int hgt = (int) Math.ceil(13 * cd / (float) max);
                g.fill(x + 3, yy + 13 - hgt, x + 17, yy + 13, 0xC0101010);
            }
            String key = ClientSetup.ABILITY[i].getTranslatedKeyMessage().getString();
            if (key.length() > 2) key = key.substring(0, 2);
            g.drawCenteredString(font, key, x + 10, yy + 3, 0xFFFFFFFF);
            int nameCol = active ? 0xFF99FF66 : cd > 0 ? 0xFF8A9A8A : 0xFFE8FFE0;
            Component name = a.abilityName(i);
            String n = font.plainSubstrByWidth(name.getString(), w - 50);
            g.drawString(font, n, x + 21, yy + 3, nameCol, true);
            if (cd > 0 && !active) g.drawString(font, String.format("%.0f", Math.ceil(cd / 20F)), x + w - 12, yy + 3, 0xFFFFAA44, true);
        }
        int yy = y + 39 + 4 * 15;
        String gk = ClientSetup.GUARD.getTranslatedKeyMessage().getString();
        if (gk.length() > 2) gk = gk.substring(0, 2);
        g.fill(x + 3, yy, x + 17, yy + 13, d.blocking ? 0xFF33AAFF : 0xFF1F3A5A);
        g.drawCenteredString(font, gk, x + 10, yy + 3, 0xFFFFFFFF);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("guard.krolben10." + a.id).getString(), w - 26), x + 21, yy + 3,
                d.blocking ? 0xFF99DDFF : 0xFFB0C8E0, true);
    }

    private static void drawFlash(GuiGraphics g, int sw, int sh) {
        float f = ClientHooks.flash;
        if (f < 0.02F) return;
        int a = (int) (Mth.clamp(f, 0, 1) * 170) << 24;
        g.fill(0, 0, sw, sh, a | (ClientHooks.flashColor & 0xFFFFFF));
    }
}
