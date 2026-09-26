package com.krolasyon.bosses.client.form;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.form.DemonForm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Ability bar (icons, keys and cooldown sweeps) plus the crimson flash of the transformation. */
public final class DemonHud {
    private DemonHud() {}

    private static final String[] ICONS = {"whip", "hearts", "dash", "spin", "judgement"};
    private static final ResourceLocation[] TEX = new ResourceLocation[ICONS.length];

    static {
        for (int i = 0; i < ICONS.length; i++) TEX[i] = new ResourceLocation(KrolasyonBosses.MODID, "textures/gui/ability_" + ICONS[i] + ".png");
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !DemonClient.isDemon(mc.player)) return;

        // transformation flash
        if (DemonClient.localAnim() == DemonForm.ANIM_TRANSFORM) {
            float t = DemonClient.localAnimTime(partial);
            float a = t < 0.6F ? t / 0.6F * 0.35F : Math.max(0F, 0.55F - (t - 0.6F) * 0.5F);
            if (a > 0.01F) g.fill(0, 0, width, height, ((int) (a * 255) << 24) | 0x5A0010);
        }

        Font font = mc.font;
        int size = 20, gap = 3;
        int total = DemonForm.COUNT * size + (DemonForm.COUNT - 1) * gap;
        int x0 = width / 2 + 98;
        if (x0 + total > width - 4) x0 = width / 2 - total / 2;
        int y0 = height - size - 3;
        if (x0 == width / 2 - total / 2) y0 = height - 22 - size - 30;
        for (int i = 0; i < DemonForm.COUNT; i++) {
            int x = x0 + i * (size + gap);
            g.fill(x - 1, y0 - 1, x + size + 1, y0 + size + 1, 0xFF1A0508);
            g.blit(TEX[i], x, y0, size, size, 0, 0, 64, 64, 64, 64);
            int cd = DemonClient.COOLDOWN[i];
            if (cd > 0) {
                float frac = Mth.clamp(cd / (float) DemonClient.COOLDOWN_MAX[i], 0F, 1F);
                int h = Math.round(size * frac);
                g.fill(x, y0 + size - h, x + size, y0 + size, 0xB0000000);
                String s = cd >= 200 ? String.valueOf((cd + 19) / 20) : String.format("%.1f", cd / 20F);
                g.drawString(font, s, x + (size - font.width(s)) / 2, y0 + 6, 0xFFFFD0D0, true);
            } else {
                float pulse = 0.5F + 0.5F * Mth.sin((mc.player.tickCount + partial) * 0.2F + i);
                int a = (int) (60 + 80 * pulse);
                g.fill(x - 1, y0 - 1, x + size + 1, y0, (a << 24) | 0xFF3050);
            }
            Component key = KeyBinds.ABILITIES[i].getTranslatedKeyMessage();
            String k = key.getString();
            if (k.length() > 3) k = k.substring(0, 3);
            g.drawString(font, k, x + size - font.width(k), y0 - 9, 0xFFFF7080, true);
        }
    }
}
