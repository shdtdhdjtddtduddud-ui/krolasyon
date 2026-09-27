package com.krolasyon.bosses.client.morph;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.morph.Aigoar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Ability bar shown while transformed: icon, key, cooldown sweep and seconds for each of the five abilities. */
public final class AigoarHud {
    private AigoarHud() {}

    private static final ResourceLocation[] ICONS = new ResourceLocation[com.krolasyon.bosses.morph.Forms.COUNT];

    static {
        for (int i = 0; i < ICONS.length; i++)
            ICONS[i] = new ResourceLocation(KrolasyonBosses.MODID, "textures/gui/" + com.krolasyon.bosses.morph.Forms.KEY[i] + "_abilities.png");
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        ClientMorph.CState me = ClientMorph.get(mc.player.getId());
        if (me == null || !com.krolasyon.bosses.morph.Forms.valid(me.form)) return;
        int form = me.form;
        int accent = com.krolasyon.bosses.morph.Forms.COLOR[form];
        Font font = mc.font;
        int size = 22, gap = 4;
        int total = Aigoar.ABILITIES * size + (Aigoar.ABILITIES - 1) * gap;
        int x0 = width / 2 + 91 + 10;
        int y0 = height - size - 3;
        if (x0 + total > width - 4) {           // narrow screens: right side, above the hunger and air bars
            x0 = width / 2 + 91 - total;
            y0 = height - 59 - size - 8;
        }
        float time = mc.player.tickCount + partial;
        for (int i = 0; i < Aigoar.ABILITIES; i++) {
            int x = x0 + i * (size + gap);
            int cd = ClientMorph.COOLDOWN[i];
            boolean ready = cd <= 0;
            if (ready) {
                int a = (int) (90 + 60 * Mth.sin(time * 0.15F + i));
                g.fill(x - 1, y0 - 1, x + size + 1, y0 + size + 1, (a << 24) | accent);
            }
            g.blit(ICONS[form], x, y0, size, size, i * 32, 0, 32, 32, 192, 32);
            if (!ready) {
                float k = Mth.clamp(cd / (float) ClientMorph.COOLDOWN_MAX[i], 0F, 1F);
                int h = Math.round(size * k);
                g.fill(x, y0 + size - h, x + size, y0 + size, 0xB0101828);
                String s = cd >= 200 ? String.valueOf((cd + 19) / 20) : String.format(java.util.Locale.ROOT, "%.1f", cd / 20F);
                g.drawString(font, s, x + (size - font.width(s)) / 2, y0 + (size - 8) / 2, 0xFFFFFFFF, true);
            }
            Component key = ClientMorph.ABILITY_KEYS[i].getTranslatedKeyMessage();
            String ks = key.getString();
            if (ks.length() > 3) ks = ks.substring(0, 3);
            g.pose().pushPose();
            g.pose().translate(x + size - font.width(ks) * 0.75F - 1, y0 - 7, 0);
            g.pose().scale(0.75F, 0.75F, 1F);
            g.drawString(font, ks, 0, 0, ready ? (0xFF000000 | com.krolasyon.bosses.morph.Forms.COLOR2[form]) : 0xFF6A7C8C, true);
            g.pose().popPose();
        }
    }
}
