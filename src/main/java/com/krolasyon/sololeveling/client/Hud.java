package com.krolasyon.sololeveling.client;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.system.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;

/** The System HUD: status panel, skill slots, quest tracker, System popups and big screen effects. */
public final class Hud {
    public static final ResourceLocation ICONS = SoloLeveling.id("textures/gui/skills.png");
    private static final String[] REGION_KEYS = {"seoul_plaza", "association", "guild", "hospital", "market", "home", "double_dungeon", "job_change",
            "demon_castle", "jeju_island", "penalty_zone", "overworld"};

    private Hud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui) {
            effects(g, mc, sw, sh, partial);
            return;
        }
        HunterData d = ClientHooks.DATA;
        Font f = mc.font;
        if (d.awakened) {
            statusPanel(g, f, p, d, partial);
            skillBar(g, f, d, sw, sh);
            tracker(g, f, d, sw);
        }
        effects(g, mc, sw, sh, partial);
        popups(g, f, sw);
    }

    private static void statusPanel(GuiGraphics g, Font f, Player p, HunterData d, float partial) {
        int x = 6, y = 6, w = 150, h = 54;
        Holo.panel(g, x, y, w, h, 0.9F, Holo.CYAN);
        g.drawString(f, Component.literal("Lv. " + d.level), x + 6, y + 5, Holo.GOLD, true);
        Component rank = Component.translatable(d.rank.langKey());
        g.drawString(f, rank, x + w - 6 - f.width(rank), y + 5, d.rank.color, true);
        Component job = Component.translatable(d.job.langKey());
        g.drawString(f, job, x + 44, y + 5, d.job == Job.NONE ? Holo.DIM : Holo.PURPLE, false);
        float hp = p.getHealth(), maxHp = p.getMaxHealth();
        Holo.bar(g, x + 22, y + 18, w - 28, 6, hp / maxHp, 0xFF5A6E, 0xB0182E);
        g.drawString(f, "HP", x + 6, y + 17, 0xFF8A99, false);
        String hs = (int) Math.ceil(hp) + "/" + (int) maxHp;
        g.pose().pushPose();
        g.pose().scale(0.5F, 0.5F, 1);
        g.drawString(f, hs, (x + w - 8) * 2 - f.width(hs), (y + 19) * 2, 0xFFFFFF, true);
        g.pose().popPose();
        float max = d.maxMana();
        Holo.bar(g, x + 22, y + 29, w - 28, 6, d.mana / max, 0x6BC8FF, 0x1A4FD6);
        g.drawString(f, "MP", x + 6, y + 28, 0x8FD4FF, false);
        String ms = (int) d.mana + "/" + (int) max;
        g.pose().pushPose();
        g.pose().scale(0.5F, 0.5F, 1);
        g.drawString(f, ms, (x + w - 8) * 2 - f.width(ms), (y + 30) * 2, 0xFFFFFF, true);
        g.pose().popPose();
        Holo.bar(g, x + 6, y + 41, w - 12, 2, d.exp / (float) d.expToNext(), 0xFFE36B, 0xFFA92E);
        g.pose().pushPose();
        g.pose().scale(0.5F, 0.5F, 1);
        String xp = "EXP " + d.exp + " / " + d.expToNext();
        g.drawString(f, xp, (x + 6) * 2, (y + 45) * 2, Holo.DIM, false);
        if (d.statPoints > 0) {
            String sp = "+" + d.statPoints + " STAT";
            g.drawString(f, sp, (x + w - 6) * 2 - f.width(sp), (y + 45) * 2, Holo.GOLD, true);
        }
        g.pose().popPose();
        if (d.penaltyTicks > 0) {
            Component pen = Sys.t("hud.penalty", d.penaltyTicks / 20);
            Holo.panel(g, x, y + h + 4, w, 14, 0.9F, Holo.RED);
            g.drawString(f, pen, x + 6, y + h + 7, Holo.RED, true);
        }
    }

    private static void skillBar(GuiGraphics g, Font f, HunterData d, int sw, int sh) {
        int size = 20, gap = 2;
        int x0 = sw / 2 + 96, y0 = sh - 24;
        if (x0 + 5 * (size + gap) > sw - 4) x0 = sw - 4 - 5 * (size + gap);
        for (int i = 0; i < HunterData.SLOTS; i++) {
            int x = x0 + i * (size + gap);
            Holo.panel(g, x, y0, size, size, 0.85F, Holo.CYAN);
            Skill s = d.slots[i];
            if (s != null) {
                RenderSystem.enableBlend();
                g.blit(ICONS, x + 2, y0 + 2, 16, 16, (s.icon % 8) * 32, (s.icon / 8) * 32, 32, 32, 256, 256);
                int cd = d.cooldown(s);
                if (cd > 0) {
                    float frac = cd / (float) Math.max(1, s.cooldown);
                    int ch = (int) (16 * frac);
                    g.fill(x + 2, y0 + 2 + 16 - ch, x + 18, y0 + 18, 0xB0000000);
                    String c = String.valueOf((cd + 19) / 20);
                    g.drawCenteredString(f, c, x + 10, y0 + 7, 0xFFFFFF);
                } else if (d.mana < s.mana) g.fill(x + 2, y0 + 2, x + 18, y0 + 18, 0x80301060);
            }
            String key = ClientSetup.SKILLS[i].getTranslatedKeyMessage().getString();
            if (key.length() > 2) key = key.substring(0, 2);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.drawString(f, key, x + 2, y0 - 8, Holo.DIM, true);
            g.pose().popPose();
        }
    }

    private static void tracker(GuiGraphics g, Font f, HunterData d, int sw) {
        int w = 132;
        int x = sw - w - 6, y = 6;
        int lines = 2 + (d.dailyDone && d.dailyRewarded ? 1 : 5) + (d.guild != Guild.NONE && !d.guildTask.isEmpty() ? 2 : 0);
        Holo.panel(g, x, y, w, 8 + lines * 10, 0.8F, Holo.CYAN);
        int ty = y + 5;
        g.drawString(f, Sys.t("hud.quest"), x + 5, ty, Holo.GOLD, true);
        ty += 10;
        List<FormattedCharSequence> q = f.split(Component.translatable(d.quest.langKey()), w - 10);
        if (!q.isEmpty()) g.drawString(f, q.get(0), x + 5, ty, Holo.TEXT, false);
        ty += 10;
        if (d.dailyDone && d.dailyRewarded) {
            g.drawString(f, Sys.t("hud.daily_done"), x + 5, ty, 0x7CFF9A, false);
            ty += 10;
        } else {
            g.drawString(f, Sys.t(d.dailyDone ? "hud.daily_claim" : "hud.daily"), x + 5, ty, d.dailyDone ? 0x7CFF9A : Holo.CYAN, true);
            ty += 10;
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            for (int i = 0; i < 4; i++) {
                int goal = HunterData.DAILY_GOAL[i];
                boolean ok = d.daily[i] >= goal;
                Component c = Sys.t("daily.task" + i).copy().append(" " + d.daily[i] + "/" + goal);
                g.drawString(f, c, (int) ((x + 8) / 0.75F), (int) (ty / 0.75F), ok ? 0x7CFF9A : Holo.TEXT, false);
                ty += 8;
            }
            g.pose().popPose();
            ty += 4;
        }
        if (d.guild != Guild.NONE && !d.guildTask.isEmpty()) {
            g.drawString(f, Component.translatable(d.guild.langKey()), x + 5, ty, d.guild.color, true);
            ty += 10;
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(f, com.krolasyon.sololeveling.world.GuildManager.describe(d).copy().append(" " + d.guildTaskProgress + "/" + d.guildTaskGoal),
                    (int) ((x + 8) / 0.75F), (int) (ty / 0.75F), Holo.TEXT, false);
            g.pose().popPose();
        }
    }

    private static void popups(GuiGraphics g, Font f, int sw) {
        long now = System.currentTimeMillis();
        ClientHooks.POPUPS.removeIf(pp -> now - pp.start() > 5200);
        int y = 34;
        for (ClientHooks.Popup pp : ClientHooks.POPUPS) {
            long age = now - pp.start();
            float in = Mth.clamp(age / 250F, 0, 1), out = Mth.clamp((5200 - age) / 500F, 0, 1);
            float alpha = Math.min(in, out);
            List<FormattedCharSequence> lines = f.split(pp.body(), 220);
            int w = 240, h = 24 + lines.size() * 10;
            int x = sw / 2 - w / 2;
            int yy = y - (int) ((1 - in) * 10);
            Holo.panel(g, x, yy, w, h, alpha, pp.color());
            Holo.title(g, f, pp.title(), x, yy, w, alpha, pp.color());
            int ly = yy + 23;
            for (FormattedCharSequence l : lines) {
                g.drawCenteredString(f, l, sw / 2, ly, Holo.a(Holo.TEXT, Math.max(0.05F, alpha)));
                ly += 10;
            }
            y += h + 6;
        }
    }

    private static void effects(GuiGraphics g, Minecraft mc, int sw, int sh, float partial) {
        if (ClientHooks.tintTicks > 0) {
            float a = ClientHooks.tintTicks / (float) Math.max(1, ClientHooks.tintMax) * 0.35F;
            int c = ((int) (ClientHooks.tintR * 255) << 16) | ((int) (ClientHooks.tintG * 255) << 8) | (int) (ClientHooks.tintB * 255);
            g.fill(0, 0, sw, sh, Holo.a(c, a));
        }
        HunterData d = ClientHooks.DATA;
        if (d.domainTicks > 0 || hasDomain()) {
            g.fillGradient(0, 0, sw, 40, Holo.a(0x2A0A55, 0.35F), 0);
            g.fillGradient(0, sh - 40, sw, sh, 0, Holo.a(0x2A0A55, 0.35F));
        }
        if (ClientHooks.ariseTicks > 0) {
            float t = (50 - ClientHooks.ariseTicks + partial) / 50F;
            float alpha = t < 0.2F ? t / 0.2F : Mth.clamp((1 - t) / 0.4F, 0, 1);
            float scale = 4.5F - t * 1.2F;
            g.fill(0, 0, sw, sh, Holo.a(0x12002A, alpha * 0.35F));
            g.pose().pushPose();
            g.pose().translate(sw / 2F, sh / 2F - 30, 0);
            g.pose().scale(scale, scale, 1);
            Component c = Sys.t("arise.shout");
            int tw = mc.font.width(c);
            g.drawString(mc.font, c, -tw / 2 + 1, 1, Holo.a(0x000000, alpha * 0.8F), false);
            g.drawString(mc.font, c, -tw / 2, 0, Holo.a(0xC9A6FF, alpha), false);
            g.pose().popPose();
        }
        if (ClientHooks.regionTicks > 0) {
            float t = (80 - ClientHooks.regionTicks + partial) / 80F;
            float alpha = t < 0.15F ? t / 0.15F : Mth.clamp((1 - t) / 0.3F, 0, 1);
            String key = REGION_KEYS[Math.max(0, Math.min(REGION_KEYS.length - 1, ClientHooks.regionIcon))];
            Component c = Sys.t("region." + key);
            g.pose().pushPose();
            g.pose().translate(sw / 2F, sh / 3F, 0);
            g.pose().scale(2.2F, 2.2F, 1);
            int tw = mc.font.width(c);
            g.fill(-tw / 2 - 8, -4, tw / 2 + 8, 12, Holo.a(0x061226, alpha * 0.6F));
            g.fill(-tw / 2 - 8, 11, tw / 2 + 8, 12, Holo.a(Holo.CYAN, alpha));
            g.drawString(mc.font, c, -tw / 2, 0, Holo.a(0xFFFFFF, alpha), true);
            g.pose().popPose();
        }
    }

    private static boolean hasDomain() {
        Player p = Minecraft.getInstance().player;
        return p != null && ClientHooks.DATA.cooldown(Skill.MONARCHS_DOMAIN) > Skill.MONARCHS_DOMAIN.cooldown - 600;
    }
}
