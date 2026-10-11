package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Ability;
import com.krolasyon.omnitrix.Alien;
import com.krolasyon.omnitrix.OmniData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Omnitrix face with the time-out ring, current form, 4 abilities with cooldowns, screen flash. */
public final class OmniHud implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientEvents.flash > 0.01F) g.fill(0, 0, w, h, ((int) (Mth.clamp(ClientEvents.flash, 0, 1) * 170) << 24) | (ClientEvents.flashColor & 0xFFFFFF));
        if (mc.player == null || mc.options.hideGui) return;
        OmniData d = ClientState.SELF;
        if (!d.wearing) return;
        Alien a = d.current();
        int cx = w / 2 - 91 - 26, cy = h - 22;
        // the watch face
        int state = a != null ? (d.timer < 200 ? 0xFFE04040 : 0xFF50FF50) : d.recharge > 0 ? 0xFFE03030 : 0xFF40D040;
        disc(g, cx, cy, 15, 0xFF1A1A1A);
        disc(g, cx, cy, 12, 0xFF2E2E2E);
        disc(g, cx, cy, 10, state);
        // hourglass symbol
        for (int y = -7; y <= 7; y++) {
            int half = Math.abs(y) * 5 / 7;
            g.fill(cx - half, cy + y, cx + half + 1, cy + y + 1, 0xFF101010);
        }
        // timer / recharge ring
        float frac = a != null ? (float) d.timer / OmniData.TRANSFORM_TIME : d.recharge > 0 ? 1 - (float) d.recharge / OmniData.RECHARGE_TIME : 1;
        int n = 48;
        for (int i = 0; i < n * frac; i++) {
            double ang = i * Math.PI * 2 / n - Math.PI / 2;
            int x = cx + (int) Math.round(Math.cos(ang) * 14), y = cy + (int) Math.round(Math.sin(ang) * 14);
            g.fill(x, y, x + 2, y + 2, state);
        }
        if (a == null) {
            if (d.recharge > 0) g.drawCenteredString(mc.font, (d.recharge + 19) / 20 + "s", cx, cy - 26, 0xFF6060);
            return;
        }
        // name + time left
        int secs = (d.timer + 19) / 20;
        String time = String.format("%d:%02d", secs / 60, secs % 60);
        int x0 = w / 2 + 98, y0 = h - 23;
        g.drawString(mc.font, a.displayName().copy().withStyle(s -> s.withColor(a.color).withBold(true)), x0, y0 - 22, 0xFFFFFF);
        g.drawString(mc.font, time, x0 + mc.font.width(a.displayName()) + 10, y0 - 22, d.timer < 200 ? 0xFF5050 : 0xA0FFA0);
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        for (int i = 0; i < 4; i++) {
            Ability ab = a.ability(i);
            int x = x0 + i * 22;
            g.fill(x - 1, y0 - 1, x + 21, y0 + 21, 0xFF2E2E2E);
            g.fill(x, y0, x + 20, y0 + 20, 0xE0101810);
            if (ab == null) continue;
            int cd = d.cooldowns[ab.ordinal()];
            RenderSystem.enableBlend();
            if (cd > 0) RenderSystem.setShaderColor(0.4F, 0.4F, 0.4F, 1F);
            g.blit(a.icon(), x + 1, y0 + 1, 18, 18, 0, 0, 128, 128, 128, 128);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            // ability glyph: the first letters of its name over the portrait
            String label = ab.displayName().getString();
            label = label.length() > 3 ? label.substring(0, 3) : label;
            g.pose().pushPose();
            g.pose().translate(x + 2, y0 + 12, 200);
            g.pose().scale(0.5F, 0.5F, 1);
            g.drawString(mc.font, label, 0, 0, 0xFFFFFF);
            g.pose().popPose();
            if (cd > 0) {
                int hh = (int) Math.ceil(18F * cd / Math.max(1, ab.cooldown));
                g.fill(x + 1, y0 + 19 - hh, x + 19, y0 + 19, 0x99000000);
                g.drawCenteredString(mc.font, String.valueOf((cd + 19) / 20), x + 10, y0 + 6, 0xFFFFFF);
            }
            g.pose().pushPose();
            g.pose().translate(x + 1, y0 + 1, 200);
            g.pose().scale(0.5F, 0.5F, 1);
            g.drawString(mc.font, ClientEvents.ABILITY[i].getTranslatedKeyMessage(), 0, 0, 0x80FF80);
            g.pose().popPose();
        }
        ClientState.Look look = ClientState.look(mc.player);
        if (look.blocking) g.drawString(mc.font, Component.translatable("hud.omnitrix.blocking"), x0, y0 - 11, 0x80C0FF);
        if (d.timer < 200 && (now / 10) % 2 == 0) g.drawCenteredString(mc.font, Component.translatable("hud.omnitrix.timeout"), w / 2, h - 60, 0xFF4040);
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int w = (int) Math.sqrt(r * r - y * y);
            g.fill(cx - w, cy + y, cx + w + 1, cy + y + 1, color);
        }
    }
}
