package com.krolasyon.storm.client;

import com.krolasyon.storm.Skill;
import com.krolasyon.storm.SkillLogic;
import com.krolasyon.storm.StormData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** 4 skill slots with cooldowns next to the hotbar, the storm energy bar, active buffs and the nova flash. */
public final class HudOverlay implements IGuiOverlay {
    private final int[] lastCd = new int[StormData.SLOTS];
    private final long[] readyFlash = new long[StormData.SLOTS];

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (ClientHooks.flash > 0.01F) g.fill(0, 0, w, h, SkillTreeScreen.argb(ClientHooks.flash * 0.6F, 0xCFE4FF));
        if (mc.player == null || mc.options.hideGui) return;
        StormData d = ClientHooks.DATA;
        boolean any = false;
        for (Skill s : d.slots) any |= s != null;
        if (!any && d.unlocked.isEmpty()) return;

        long now = net.minecraft.Util.getMillis();
        int x0 = w / 2 + 98, y = h - 23;
        // energy bar
        float max = d.maxEnergy();
        if (ClientHooks.shownEnergy < 0) ClientHooks.shownEnergy = d.energy;
        ClientHooks.shownEnergy = Mth.lerp(0.15F, ClientHooks.shownEnergy, d.energy);
        float frac = Mth.clamp(ClientHooks.shownEnergy / max, 0, 1);
        int bw = 4 * 22 - 2, by = y - 7;
        g.fill(x0 - 1, by - 1, x0 + bw + 1, by + 5, 0xC0000000);
        int fw = (int) (bw * frac);
        g.fillGradient(x0, by, x0 + fw, by + 4, 0xFF9FE4FF, 0xFF3A5BFF);
        // travelling glint
        int gl = (int) ((now / 12) % (bw + 30)) - 15;
        if (gl > 0 && gl < fw) g.fill(x0 + gl, by, x0 + Math.min(fw, gl + 4), by + 4, 0x88FFFFFF);
        for (int i = 1; i < 4; i++) g.fill(x0 + bw * i / 4, by, x0 + bw * i / 4 + 1, by + 4, 0x55000000);
        g.pose().pushPose();
        g.pose().translate(x0 + bw + 4, by - 1, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(mc.font, (int) d.energy + "⚡", 0, 0, 0x9FE4FF);
        g.pose().popPose();

        for (int i = 0; i < StormData.SLOTS; i++) {
            int x = x0 + i * 22;
            Skill s = d.slots[i];
            g.fill(x - 1, y - 1, x + 21, y + 21, 0xFF3A3846);
            g.fill(x, y, x + 20, y + 20, 0xE00F0E18);
            if (s == null) continue;
            int cd = d.cooldowns[s.ordinal()];
            if (lastCd[i] > 0 && cd == 0) readyFlash[i] = now;
            lastCd[i] = cd;
            boolean noEnergy = d.energy < SkillLogic.energyCost(mc.player, s);
            if (cd > 0 || noEnergy) RenderSystem.setShaderColor(0.45F, 0.45F, 0.55F, 1F);
            g.blit(s.icon(), x + 1, y + 1, 18, 18, 0, 0, 90, 90, 90, 90);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            if (cd > 0) {
                int hgt = (int) Math.ceil(18F * cd / Math.max(1, s.cooldown));
                g.fill(x + 1, y + 19 - hgt, x + 19, y + 19, 0x99000020);
                String sec = cd >= 20 ? String.valueOf((cd + 19) / 20) : String.format("%.1f", cd / 20F);
                g.drawCenteredString(mc.font, sec, x + 10, y + 6, 0xFFFFFF);
            } else if (noEnergy) {
                g.fill(x + 1, y + 1, x + 19, y + 19, 0x553050FF);
            }
            float rf = (now - readyFlash[i]) / 300F;
            if (rf < 1) g.fill(x + 1, y + 1, x + 19, y + 19, SkillTreeScreen.argb((1 - rf) * 0.8F, 0xFFFFFF));
            g.pose().pushPose();
            g.pose().translate(x + 1, y + 1, 200);
            g.pose().scale(0.5F, 0.5F, 1);
            g.drawString(mc.font, ClientSetup.SLOT_KEYS[i].getTranslatedKeyMessage(), 0, 0, 0xFFE070);
            g.pose().popPose();
        }
        // active timed skills above the bar
        int bx = x0;
        bx = buff(g, mc, bx, by - 12, Skill.STORM_DOME, d.domeTicks);
        bx = buff(g, mc, bx, by - 12, Skill.STATIC_RING, d.ringTicks);
        buff(g, mc, bx, by - 12, Skill.STORM_AVATAR, d.avatarTicks);
    }

    private static int buff(GuiGraphics g, Minecraft mc, int x, int y, Skill s, int ticks) {
        if (ticks <= 0) return x;
        g.blit(s.icon(), x, y, 10, 10, 0, 0, 90, 90, 90, 90);
        g.drawString(mc.font, String.valueOf((ticks + 19) / 20), x + 12, y + 1, 0x9FE4FF);
        return x + 28;
    }
}
