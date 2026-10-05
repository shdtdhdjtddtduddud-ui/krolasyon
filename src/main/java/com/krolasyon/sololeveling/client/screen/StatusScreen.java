package com.krolasyon.sololeveling.client.screen;

import com.krolasyon.sololeveling.client.ClientHooks;
import com.krolasyon.sololeveling.client.Holo;
import com.krolasyon.sololeveling.client.Hud;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.shadow.ShadowManager;
import com.krolasyon.sololeveling.system.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The "Status Window". Tabs: status, skills, shadows, quests, titles. */
public class StatusScreen extends Screen {
    private static final String[] TABS = {"status", "skills", "shadows", "quests", "titles"};
    private int tab;
    private int x, y, w = 360, h = 232;
    private Skill picked;
    private int scroll;

    public StatusScreen() { super(Sys.t("status.title")); }

    @Override
    protected void init() {
        x = (width - w) / 2;
        y = (height - h) / 2;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private HunterData d() { return ClientHooks.DATA; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Holo.panel(g, x, y, w, h, 1F, Holo.CYAN);
        Holo.title(g, font, Sys.t("status.window"), x, y, w, 1F, Holo.CYAN);
        // tabs
        int tx = x + 8;
        for (int i = 0; i < TABS.length; i++) {
            Component c = Sys.t("status.tab." + TABS[i]);
            int tw = font.width(c) + 12;
            boolean hover = Holo.in(mx, my, tx, y + 22, tw, 13);
            g.fill(tx, y + 22, tx + tw, y + 35, Holo.a(i == tab ? Holo.CYAN : 0x14315C, i == tab ? 0.45F : hover ? 0.6F : 0.4F));
            g.drawString(font, c, tx + 6, y + 25, i == tab ? 0xFFFFFF : Holo.DIM, false);
            tx += tw + 3;
        }
        g.fill(x + 6, y + 35, x + w - 6, y + 36, Holo.a(Holo.CYAN, 0.6F));
        List<Component> tip = new ArrayList<>();
        switch (tab) {
            case 0 -> status(g, mx, my);
            case 1 -> skills(g, mx, my, tip);
            case 2 -> shadows(g, mx, my);
            case 3 -> quests(g, mx, my);
            default -> titles(g, mx, my);
        }
        // footer buttons
        int by = y + h - 18;
        Holo.button(g, font, Sys.t("status.shop"), x + 8, by, 70, 13, mx, my, true, Holo.GOLD);
        Holo.button(g, font, Sys.t("status.map"), x + 82, by, 70, 13, mx, my, true, Holo.CYAN);
        Holo.button(g, font, Sys.t("status.news"), x + 156, by, 70, 13, mx, my, true, 0xFFB347);
        g.drawString(font, Sys.t("status.gold", d().gold), x + w - 8 - font.width(Sys.t("status.gold", d().gold)), by + 3, Holo.GOLD, true);
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
        super.render(g, mx, my, partial);
    }

    private void line(GuiGraphics g, Component label, Component value, int lx, int ly, int color) {
        g.drawString(font, label, lx, ly, Holo.DIM, false);
        g.drawString(font, value, lx + 62, ly, color, false);
    }

    private void status(GuiGraphics g, int mx, int my) {
        HunterData d = d();
        int lx = x + 12, ly = y + 44;
        line(g, Sys.t("status.name"), minecraft.player.getName(), lx, ly, 0xFFFFFF);
        line(g, Sys.t("status.level"), Component.literal(String.valueOf(d.level)), lx, ly + 12, Holo.GOLD);
        line(g, Sys.t("status.job"), Component.translatable(d.job.langKey()), lx, ly + 24, d.job == Job.NONE ? Holo.DIM : Holo.PURPLE);
        line(g, Sys.t("status.title_label"), d.title == Title.NONE ? Sys.t("status.none") : Component.translatable(d.title.langKey()), lx, ly + 36, Holo.TEXT);
        line(g, Sys.t("status.rank"), Component.translatable(d.rank.langKey()).copy().append(d.licensed ? "" : " *"), lx, ly + 48, d.rank.color);
        line(g, Sys.t("status.guild"), Component.translatable(d.guild.langKey()), lx, ly + 60, d.guild.color);
        line(g, Sys.t("status.fatigue"), Component.literal(String.valueOf(d.fatigue)), lx, ly + 72, Holo.TEXT);
        float hp = minecraft.player.getHealth(), max = minecraft.player.getMaxHealth();
        g.drawString(font, "HP", lx, ly + 90, 0xFF8A99, false);
        Holo.bar(g, lx + 20, ly + 91, 130, 6, hp / max, 0xFF5A6E, 0xB0182E);
        g.drawString(font, (int) hp + "/" + (int) max, lx + 154, ly + 90, 0xFFFFFF, false);
        g.drawString(font, "MP", lx, ly + 102, 0x8FD4FF, false);
        Holo.bar(g, lx + 20, ly + 103, 130, 6, d.mana / d.maxMana(), 0x6BC8FF, 0x1A4FD6);
        g.drawString(font, (int) d.mana + "/" + (int) d.maxMana(), lx + 154, ly + 102, 0xFFFFFF, false);
        g.drawString(font, "EXP", lx, ly + 114, Holo.GOLD, false);
        Holo.bar(g, lx + 20, ly + 115, 130, 4, d.exp / (float) d.expToNext(), 0xFFE36B, 0xFFA92E);
        // stats
        int sx = x + 214, sy = y + 44;
        g.fill(sx - 8, sy - 4, sx - 7, sy + 120, Holo.a(Holo.CYAN, 0.4F));
        for (Stat s : Stat.values()) {
            int row = sy + s.ordinal() * 18;
            g.drawString(font, Sys.t("stat." + s.key()), sx, row, s.color, true);
            g.drawString(font, String.valueOf(d.stat(s)), sx + 70, row, 0xFFFFFF, true);
            boolean can = d.statPoints > 0;
            Holo.button(g, font, Component.literal("+"), sx + 100, row - 2, 14, 12, mx, my, can, Holo.CYAN);
            if (Holo.in(mx, my, sx, row - 2, 95, 12)) {
                g.renderTooltip(font, font.split(Sys.t("stat." + s.key() + ".desc"), 200), mx, my);
            }
        }
        g.drawString(font, Sys.t("status.points", d.statPoints), sx, sy + 96, d.statPoints > 0 ? Holo.GOLD : Holo.DIM, true);
        g.drawString(font, Sys.t("status.shift_hint"), sx, sy + 108, 0x5F7FA0, false);
        g.drawString(font, Sys.t("status.gates_cleared", d.gatesCleared), lx, ly + 128, Holo.DIM, false);
        g.drawString(font, Sys.t("status.shadows_count", d.shadows.size(), d.shadowCapacity()), lx + 120, ly + 128, Holo.PURPLE, false);
    }

    private void skills(GuiGraphics g, int mx, int my, List<Component> tip) {
        HunterData d = d();
        int sx = x + 10, sy = y + 42;
        int i = 0;
        for (Skill s : Skill.values()) {
            int cx = sx + (i % 4) * 86, cy = sy + (i / 4) * 34;
            boolean has = d.hasSkill(s);
            boolean sel = picked == s;
            g.fill(cx, cy, cx + 82, cy + 30, Holo.a(sel ? Holo.CYAN : 0x0F2747, sel ? 0.4F : 0.7F));
            RenderSystem.enableBlend();
            if (!has) RenderSystem.setShaderColor(0.35F, 0.35F, 0.4F, 1F);
            g.blit(Hud.ICONS, cx + 3, cy + 3, 24, 24, (s.icon % 8) * 32, (s.icon / 8) * 32, 32, 32, 256, 256);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            List<FormattedCharSequence> nm = font.split(Component.translatable(s.langKey()), 52);
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            int ty = (int) ((cy + 4) / 0.75F);
            for (FormattedCharSequence l : nm) {
                g.drawString(font, l, (int) ((cx + 30) / 0.75F), ty, has ? 0xFFFFFF : 0x7A8A9A, false);
                ty += 9;
            }
            Component sub = s.active ? Component.literal(s.mana + " MP") : Sys.t("skill.passive");
            if (!has) sub = Sys.t("skill.req", s.unlockLevel);
            g.drawString(font, sub, (int) ((cx + 30) / 0.75F), (int) ((cy + 22) / 0.75F), has ? Holo.CYAN : 0xAA5566, false);
            g.pose().popPose();
            if (Holo.in(mx, my, cx, cy, 82, 30)) {
                tip.add(Component.translatable(s.langKey()).withStyle(net.minecraft.ChatFormatting.AQUA));
                tip.add(Component.translatable(s.descKey()));
                if (s.active) tip.add(Sys.t("skill.stats", s.mana, s.cooldown / 20));
                if (s.job != Job.NONE) tip.add(Sys.t("skill.job", Component.translatable(s.job.langKey())));
                if (has && s.active) tip.add(Sys.t("skill.click_assign").copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            i++;
        }
        // quick slots
        int qy = y + h - 40;
        g.drawString(font, Sys.t("skill.slots"), x + 10, qy + 5, Holo.DIM, false);
        for (int k = 0; k < HunterData.SLOTS; k++) {
            int qx = x + 90 + k * 26;
            Holo.panel(g, qx, qy, 22, 22, 1F, picked != null ? Holo.GOLD : Holo.CYAN);
            Skill s = d.slots[k];
            if (s != null) {
                RenderSystem.enableBlend();
                g.blit(Hud.ICONS, qx + 3, qy + 3, 16, 16, (s.icon % 8) * 32, (s.icon / 8) * 32, 32, 32, 256, 256);
            }
            g.drawString(font, String.valueOf(k + 1), qx + 1, qy + 1, Holo.DIM, false);
        }
    }

    private void shadows(GuiGraphics g, int mx, int my) {
        HunterData d = d();
        g.drawString(font, Sys.t("shadows.header", d.shadows.size(), d.shadowCapacity()), x + 12, y + 42, Holo.PURPLE, true);
        Holo.button(g, font, Sys.t(d.shadowsOut ? "shadows.recall_all" : "shadows.summon_all"), x + w - 112, y + 39, 102, 13, mx, my, !d.shadows.isEmpty(), Holo.PURPLE);
        if (d.job == Job.NONE) {
            g.drawWordWrap(font, Sys.t("shadows.locked"), x + 12, y + 60, w - 24, Holo.DIM);
            return;
        }
        int rows = 9;
        for (int i = 0; i < rows && i + scroll < d.shadows.size(); i++) {
            HunterData.ShadowRecord r = d.shadows.get(i + scroll);
            int ry = y + 58 + i * 16;
            g.fill(x + 10, ry, x + w - 10, ry + 14, Holo.a(0x1A0F3A, 0.7F));
            g.drawString(font, ShadowManager.displayName(r), x + 14, ry + 3, r.named ? Holo.GOLD : 0xD8C8FF, false);
            g.drawString(font, "Lv. " + r.level, x + 200, ry + 3, Holo.TEXT, false);
            Holo.button(g, font, Sys.t("shadows.toggle"), x + w - 96, ry + 1, 52, 12, mx, my, true, Holo.PURPLE);
            Holo.button(g, font, Component.literal("x"), x + w - 40, ry + 1, 14, 12, mx, my, hasShiftDown(), Holo.RED);
        }
        if (d.shadows.size() > rows) g.drawString(font, (scroll + 1) + "-" + Math.min(d.shadows.size(), scroll + rows) + " / " + d.shadows.size(), x + 12, y + h - 34, Holo.DIM, false);
        g.drawString(font, Sys.t("shadows.release_hint"), x + 130, y + h - 34, 0x5F7FA0, false);
    }

    private void quests(GuiGraphics g, int mx, int my) {
        HunterData d = d();
        int lx = x + 12, ly = y + 44;
        g.drawString(font, Sys.t("quest.main"), lx, ly, Holo.GOLD, true);
        g.drawString(font, Component.translatable(d.quest.langKey()), lx, ly + 12, 0xFFFFFF, false);
        g.drawWordWrap(font, Component.translatable(d.quest.descKey()), lx, ly + 24, w / 2 - 20, Holo.DIM);
        int dx = x + w / 2 + 6;
        g.drawString(font, Sys.t("daily.title"), dx, ly, Holo.CYAN, true);
        for (int i = 0; i < 4; i++) {
            int goal = HunterData.DAILY_GOAL[i];
            g.drawString(font, Sys.t("daily.task" + i), dx, ly + 14 + i * 16, d.daily[i] >= goal ? 0x7CFF9A : Holo.TEXT, false);
            Holo.bar(g, dx, ly + 24 + i * 16, 120, 3, d.daily[i] / (float) goal, 0x7CFF9A, 0x2E9A55);
            g.drawString(font, d.daily[i] + "/" + goal, dx + 126, ly + 20 + i * 16, Holo.DIM, false);
        }
        boolean claim = d.dailyDone && !d.dailyRewarded;
        Holo.button(g, font, Sys.t(d.dailyRewarded ? "daily.claimed" : "daily.claim"), dx, ly + 84, 120, 14, mx, my, claim, 0x7CFF9A);
        g.drawWordWrap(font, Sys.t("daily.warning"), dx, ly + 104, w / 2 - 20, 0xFF8A99);
        if (d.guild != Guild.NONE) {
            g.drawString(font, Component.translatable(d.guild.langKey()), lx, ly + 100, d.guild.color, true);
            g.drawString(font, Sys.t("guild.rep", d.guildRep, Component.translatable("sololeveling.guild_rank." + Guild.guildRankFor(d.guildRep))), lx, ly + 112, Holo.DIM, false);
            if (!d.guildTask.isEmpty())
                g.drawString(font, com.krolasyon.sololeveling.world.GuildManager.describe(d).copy().append(" " + d.guildTaskProgress + "/" + d.guildTaskGoal), lx, ly + 124, Holo.TEXT, false);
        }
    }

    private void titles(GuiGraphics g, int mx, int my) {
        HunterData d = d();
        int i = 0;
        for (Title t : Title.values()) {
            boolean own = t == Title.NONE || d.titles.contains(t);
            int ty = y + 44 + i * 22;
            boolean eq = d.title == t;
            g.fill(x + 10, ty, x + w - 10, ty + 20, Holo.a(eq ? Holo.GOLD : 0x0F2747, eq ? 0.35F : 0.7F));
            g.drawString(font, t == Title.NONE ? Sys.t("status.none") : Component.translatable(t.langKey()), x + 16, ty + 3, own ? 0xFFFFFF : 0x6B7C8F, false);
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, own ? Component.translatable(t.langKey() + ".desc") : Sys.t("title.locked"), (int) ((x + 16) / 0.75F), (int) ((ty + 12) / 0.75F), Holo.DIM, false);
            g.pose().popPose();
            if (own && !eq) Holo.button(g, font, Sys.t("title.equip"), x + w - 74, ty + 4, 58, 12, mx, my, true, Holo.GOLD);
            i++;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        HunterData d = d();
        int tx = x + 8;
        for (int i = 0; i < TABS.length; i++) {
            int tw = font.width(Sys.t("status.tab." + TABS[i])) + 12;
            if (Holo.in(mx, my, tx, y + 22, tw, 13)) {
                tab = i;
                scroll = 0;
                ClientHooks.playClick();
                return true;
            }
            tx += tw + 3;
        }
        int by = y + h - 18;
        if (Holo.in(mx, my, x + 8, by, 70, 13)) return send("system_shop", 0, 0, "");
        if (Holo.in(mx, my, x + 82, by, 70, 13)) return send("map", 0, 0, "");
        if (Holo.in(mx, my, x + 156, by, 70, 13)) return send("news", 0, 0, "");
        switch (tab) {
            case 0 -> {
                int sx = x + 214, sy = y + 44;
                for (Stat s : Stat.values()) {
                    int row = sy + s.ordinal() * 18;
                    if (d.statPoints > 0 && Holo.in(mx, my, sx + 100, row - 2, 14, 12)) return send("stat", s.ordinal(), hasShiftDown() ? 5 : 1, "");
                }
            }
            case 1 -> {
                int sx = x + 10, sy = y + 42, i = 0;
                for (Skill s : Skill.values()) {
                    int cx = sx + (i % 4) * 86, cy = sy + (i / 4) * 34;
                    if (Holo.in(mx, my, cx, cy, 82, 30) && s.active && d.hasSkill(s)) {
                        picked = picked == s ? null : s;
                        ClientHooks.playClick();
                        return true;
                    }
                    i++;
                }
                int qy = y + h - 40;
                for (int k = 0; k < HunterData.SLOTS; k++) {
                    int qx = x + 90 + k * 26;
                    if (Holo.in(mx, my, qx, qy, 22, 22)) {
                        send("slot", k, picked == null ? -1 : picked.ordinal(), "");
                        picked = null;
                        return true;
                    }
                }
            }
            case 2 -> {
                if (Holo.in(mx, my, x + w - 112, y + 39, 102, 13)) return send("shadows", 0, 0, "");
                for (int i = 0; i < 9 && i + scroll < d.shadows.size(); i++) {
                    int ry = y + 58 + i * 16;
                    if (Holo.in(mx, my, x + w - 96, ry + 1, 52, 12)) return send("shadow_summon", i + scroll, 0, "");
                    if (hasShiftDown() && Holo.in(mx, my, x + w - 40, ry + 1, 14, 12)) return send("shadow_release", i + scroll, 0, "");
                }
            }
            case 3 -> {
                int dx = x + w / 2 + 6, ly = y + 44;
                if (d.dailyDone && !d.dailyRewarded && Holo.in(mx, my, dx, ly + 84, 120, 14)) return send("daily_claim", 0, 0, "");
            }
            case 4 -> {
                int i = 0;
                for (Title t : Title.values()) {
                    int ty = y + 44 + i * 22;
                    if ((t == Title.NONE || d.titles.contains(t)) && Holo.in(mx, my, x + w - 74, ty + 4, 58, 12)) return send("title", t.ordinal(), 0, "");
                    i++;
                }
            }
            default -> {}
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == 2) {
            scroll = Math.max(0, Math.min(Math.max(0, d().shadows.size() - 9), scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean send(String a, int x1, int x2, String s) {
        ClientHooks.playClick();
        Net.toServer(new Net.Action(a, x1, x2, s));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (com.krolasyon.sololeveling.client.ClientSetup.STATUS.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
