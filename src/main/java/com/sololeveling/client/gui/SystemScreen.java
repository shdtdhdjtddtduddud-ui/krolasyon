package com.sololeveling.client.gui;

import com.sololeveling.client.ClientEvents;
import com.sololeveling.client.ClientHooks;
import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.system.Guilds;
import com.sololeveling.system.Quests;
import com.sololeveling.util.Ranks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** The Player's "System" window: Status / Skills / Quests / Map / News / Guild. */
public class SystemScreen extends Screen {
    private static final String[] TABS = {"status", "skills", "quests", "map", "news", "guild"};
    private int tab;
    private int px, py, pw, ph;
    private final List<Object[]> clicks = new ArrayList<>();
    private Component tooltip;
    private int scroll = 0, maxScroll = 0;
    private String selRegion = "seoul";

    public SystemScreen(int tab) {
        super(Component.translatable("gui.sololeveling.system"));
        this.tab = Mth.clamp(tab, 0, TABS.length - 1);
    }

    @Override
    protected void init() {
        if (tab == 3 || tab == 2) Net.toServer(new Packets.Action("gates", "", 0));
        if (tab == 4) Net.toServer(new Packets.Action("news", "", 0));
    }

    @Override public boolean isPauseScreen() { return false; }

    private void click(int x, int y, int w, int h, Runnable r) { clicks.add(new Object[]{x, y, w, h, r}); }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean enabled, int mx, int my, Runnable r) {
        boolean hov = enabled && UiKit.in(mx, my, x, y, w, h);
        UiKit.box(g, x, y, w, h, enabled ? (hov ? 0xFF1E5A86 : 0xFF12304A) : 0xFF141A24, enabled ? (hov ? 0xFFFFFFFF : UiKit.BORDER) : 0xFF334455);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? UiKit.TEXT : 0xFF667788);
        if (enabled) click(x, y, w, h, r);
    }

    private void act(String action, String arg, int n) { Net.toServer(new Packets.Action(action, arg, n)); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        clicks.clear();
        tooltip = null;
        pw = Math.min(width - 12, 440);
        ph = Math.min(height - 12, 262);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        UiKit.panel(g, px, py, pw, ph);
        g.drawString(font, "[ " + Component.translatable("gui.sololeveling.system").getString().toUpperCase() + " ]", px + 10, py + 8, UiKit.BORDER, true);
        SLPlayer d = ClientHooks.data;
        String gold = "G " + d.gold;
        g.drawString(font, gold, px + pw - 10 - font.width(gold), py + 8, UiKit.GOLD, true);
        // tabs
        int tx = px + 8, tw = (pw - 16) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int ti = i;
            boolean sel = i == tab;
            boolean hov = UiKit.in(mx, my, tx + i * tw, py + 22, tw - 2, 16);
            UiKit.box(g, tx + i * tw, py + 22, tw - 2, 16, sel ? 0xFF1E5A86 : (hov ? 0xFF16405E : 0xFF0E1E30), sel ? 0xFFFFFFFF : UiKit.BORDER);
            g.drawCenteredString(font, Component.translatable("gui.sololeveling.tab." + TABS[i]), tx + i * tw + (tw - 2) / 2, py + 26, sel ? 0xFFFFFFFF : UiKit.DIM);
            click(tx + i * tw, py + 22, tw - 2, 16, () -> { tab = ti; scroll = 0; init(); });
        }
        int cx = px + 10, cy = py + 44, cw = pw - 20, ch = ph - 54;
        g.enableScissor(cx - 2, cy - 2, cx + cw + 2, cy + ch + 2);
        switch (tab) {
            case 0 -> drawStatus(g, cx, cy, cw, ch, mx, my);
            case 1 -> drawSkills(g, cx, cy, cw, ch, mx, my);
            case 2 -> drawQuests(g, cx, cy, cw, ch, mx, my);
            case 3 -> drawMap(g, cx, cy, cw, ch, mx, my);
            case 4 -> drawNews(g, cx, cy, cw, ch, mx, my);
            default -> drawGuild(g, cx, cy, cw, ch, mx, my);
        }
        g.disableScissor();
        if (tooltip != null) g.renderTooltip(font, tooltip, mx, my);
    }

    // ------------------------------------------------------------------ STATUS
    private void drawStatus(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        SLPlayer d = ClientHooks.data;
        Minecraft mc = Minecraft.getInstance();
        int half = w / 2 - 6;
        // left column
        g.drawString(font, Component.literal(mc.player == null ? "Player" : mc.player.getGameProfile().getName()), x, y, UiKit.TEXT, true);
        g.drawString(font, Component.translatable("gui.sololeveling.title_label", d.title), x, y + 11, UiKit.DIM, false);
        g.pose().pushPose();
        g.pose().translate(x, y + 24, 0);
        g.pose().scale(2.2F, 2.2F, 1);
        g.drawString(font, "Lv. " + d.level, 0, 0, UiKit.TEXT, true);
        g.pose().popPose();
        String rk = d.rank();
        int rc = 0xFF000000 | Ranks.color(rk);
        UiKit.box(g, x + 92, y + 24, 24, 18, 0xFF0A1220, rc);
        g.drawCenteredString(font, rk, x + 104, y + 29, rc);
        g.drawString(font, Component.translatable("gui.sololeveling.job", Component.translatable("gui.sololeveling.job." + d.job)), x, y + 48, UiKit.TEXT, false);
        g.drawString(font, Component.translatable("gui.sololeveling.guild", d.guild.isEmpty() ? Component.translatable("gui.sololeveling.none") : Component.translatable("guild.sololeveling." + d.guild)), x, y + 59, UiKit.TEXT, false);
        int by = y + 74;
        float hp = mc.player == null ? 0 : mc.player.getHealth(), mhp = mc.player == null ? 20 : mc.player.getMaxHealth();
        bar(g, x, by, half, "HP", hp / mhp, 0xFFE04050, String.format("%.0f / %.0f", hp, mhp));
        bar(g, x, by + 15, half, "MP", d.mana / d.maxMana(), 0xFF3F8CFF, String.format("%.0f / %.0f", d.mana, d.maxMana()));
        bar(g, x, by + 30, half, "EXP", (float) d.xp / d.xpNeeded(), 0xFF3FD0A0, d.xp + " / " + d.xpNeeded());
        bar(g, x, by + 45, half, Component.translatable("gui.sololeveling.fatigue").getString(), d.fatigue / 100F, 0xFFD0A030, d.fatigue + " / 100");
        g.drawString(font, Component.translatable("gui.sololeveling.kills", d.kills), x, by + 64, UiKit.DIM, false);
        g.drawString(font, Component.translatable("gui.sololeveling.gates_cleared", d.gatesCleared), x, by + 75, UiKit.DIM, false);
        g.drawString(font, Component.translatable("gui.sololeveling.shadows_extracted", d.shadowsExtracted), x, by + 86, UiKit.DIM, false);
        // right column: stats
        int rx = x + half + 14, rw = w - half - 14;
        g.drawString(font, Component.translatable("gui.sololeveling.stats"), rx, y, UiKit.BORDER, true);
        g.drawString(font, Component.translatable("gui.sololeveling.points", d.points), rx + rw - 80, y, d.points > 0 ? UiKit.GOLD : UiKit.DIM, true);
        String[] ids = SLPlayer.STAT_IDS;
        int[] colors = {0xFFFF7060, 0xFF60E090, 0xFFFFA040, 0xFF60A8FF, 0xFFD890FF};
        for (int i = 0; i < 5; i++) {
            int ry = y + 16 + i * 26;
            UiKit.box(g, rx, ry, rw, 22, UiKit.BG2, 0xFF22405A);
            g.fill(rx, ry, rx + 3, ry + 22, colors[i]);
            g.drawString(font, Component.translatable("gui.sololeveling.stat." + ids[i]), rx + 8, ry + 3, UiKit.TEXT, true);
            g.drawString(font, Component.translatable("gui.sololeveling.statdesc." + ids[i]), rx + 8, ry + 13, UiKit.DIM, false);
            g.drawString(font, String.valueOf(d.stats[i]), rx + rw - 76, ry + 7, colors[i], true);
            final int idx = i;
            button(g, rx + rw - 54, ry + 3, 22, 16, Component.literal("+"), d.points > 0, mx, my, () -> act("stat", String.valueOf(idx), 1));
            button(g, rx + rw - 30, ry + 3, 26, 16, Component.literal("+5"), d.points > 0, mx, my, () -> act("stat", String.valueOf(idx), 5));
        }
        g.drawString(font, Component.translatable("gui.sololeveling.hint_keys"), rx, y + 16 + 5 * 26 + 2, UiKit.DIM, false);
    }

    private void bar(GuiGraphics g, int x, int y, int w, String label, float frac, int color, String text) {
        g.drawString(font, label, x, y + 1, UiKit.DIM, false);
        UiKit.bar(g, x + 26, y, w - 26, 11, frac, color);
        g.drawCenteredString(font, text, x + 26 + (w - 26) / 2, y + 2, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ SKILLS
    private void drawSkills(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        SLPlayer d = ClientHooks.data;
        List<Content.SkillDef> unlocked = new ArrayList<>();
        for (Content.SkillDef s : Content.SKILLS) if (d.hasSkill(s.id())) unlocked.add(s);
        ClientHooks.selectedSkill = unlocked.isEmpty() ? 0 : Mth.clamp(ClientHooks.selectedSkill, 0, unlocked.size() - 1);
        String selId = unlocked.isEmpty() ? "" : unlocked.get(ClientHooks.selectedSkill).id();
        int rowH = 19;
        int listH = h - 40;
        maxScroll = Math.max(0, Content.SKILLS.length * rowH - listH);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.enableScissor(x, y, x + w, y + listH);
        Component hover = null;
        for (int i = 0; i < Content.SKILLS.length; i++) {
            Content.SkillDef s = Content.SKILLS[i];
            int ry = y + i * rowH - scroll;
            if (ry + rowH < y || ry > y + listH) continue;
            boolean has = d.hasSkill(s.id());
            boolean sel = s.id().equals(selId);
            boolean hov = UiKit.in(mx, my, x, ry, w, rowH - 2) && my < y + listH;
            UiKit.box(g, x, ry, w, rowH - 2, sel ? 0xFF1E3A5A : (hov ? 0xFF16283E : UiKit.BG2), sel ? 0xFFFFFFFF : (has ? UiKit.BORDER : 0xFF334455));
            g.drawString(font, Component.translatable("skill.sololeveling." + s.id()), x + 6, ry + 5, has ? UiKit.TEXT : 0xFF667788, true);
            String info = has ? Component.translatable("gui.sololeveling.mana_cost", s.mana()).getString() + "  " + s.cooldown() + "s"
                    : Component.translatable("gui.sololeveling.req_level", s.level()).getString();
            g.drawString(font, info, x + w - 8 - font.width(info), ry + 5, has ? UiKit.BLUE : UiKit.RED, false);
            if (hov) hover = Component.translatable("skill.sololeveling." + s.id() + ".desc");
            if (has) {
                final int ui = unlocked.indexOf(s);
                if (my < y + listH) click(x, ry, w, rowH - 2, () -> ClientHooks.selectedSkill = ui);
            }
        }
        g.disableScissor();
        if (hover != null) tooltip = hover;
        g.drawString(font, Component.translatable("gui.sololeveling.skill_hint"), x, y + h - 30, UiKit.DIM, false);
        g.drawString(font, Component.translatable("gui.sololeveling.skill_hint2"), x, y + h - 19, UiKit.DIM, false);
    }

    // ------------------------------------------------------------------ QUESTS
    private void drawQuests(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        SLPlayer d = ClientHooks.data;
        int yy = y - scroll;
        // daily
        UiKit.box(g, x, yy, w, 78, UiKit.BG2, UiKit.BORDER);
        g.drawString(font, Component.translatable("gui.sololeveling.daily_name"), x + 6, yy + 5, UiKit.GOLD, true);
        boolean done = d.dqKills >= d.dqKillTarget && d.dqDist >= d.dqDistTarget && (d.level < 10 || d.dqGates >= 1);
        qline(g, x + 6, yy + 18, w - 130, Component.translatable("gui.sololeveling.dq_kills").getString(), d.dqKills, d.dqKillTarget);
        qline(g, x + 6, yy + 33, w - 130, Component.translatable("gui.sololeveling.dq_dist").getString(), (int) d.dqDist, d.dqDistTarget);
        if (d.level >= 10) qline(g, x + 6, yy + 48, w - 130, Component.translatable("gui.sololeveling.dq_gate").getString(), d.dqGates, 1);
        g.drawString(font, Component.translatable("gui.sololeveling.daily_rewards_hint"), x + 6, yy + 64, UiKit.DIM, false);
        button(g, x + w - 112, yy + 26, 104, 22, d.dqClaimed ? Component.translatable("gui.sololeveling.claimed") : Component.translatable("gui.sololeveling.claim"), done && !d.dqClaimed, mx, my, () -> act("claim_daily", "", 0));
        yy += 86;
        // active
        g.drawString(font, Component.translatable("gui.sololeveling.active_quests", d.quests.size(), Quests.MAX_ACTIVE), x, yy, UiKit.BORDER, true);
        yy += 12;
        if (d.quests.isEmpty()) { g.drawString(font, Component.translatable("gui.sololeveling.none_quests"), x + 4, yy, UiKit.DIM, false); yy += 14; }
        for (int i = 0; i < d.quests.size(); i++) {
            SLPlayer.Quest q = d.quests.get(i);
            UiKit.box(g, x, yy, w, 30, UiKit.BG2, q.done() ? UiKit.GREEN : 0xFF22405A);
            g.drawString(font, Quests.questTitle(q), x + 6, yy + 4, UiKit.TEXT, true);
            g.drawString(font, Component.translatable("gui.sololeveling.quest_reward", q.rewardXp, q.rewardGold), x + 6, yy + 17, UiKit.GOLD, false);
            String pr = q.have + "/" + q.need;
            g.drawString(font, pr, x + w - 130 - font.width(pr), yy + 4, q.done() ? UiKit.GREEN : UiKit.DIM, false);
            final int qi = i;
            boolean canClaim = q.done() || q.type.equals("collect");
            button(g, x + w - 120, yy + 6, 70, 18, Component.translatable("gui.sololeveling.claim"), canClaim, mx, my, () -> act("claim_quest", "", qi));
            button(g, x + w - 46, yy + 6, 40, 18, Component.translatable("gui.sololeveling.drop"), true, mx, my, () -> act("abandon_quest", "", qi));
            yy += 34;
        }
        yy += 4;
        g.drawString(font, Component.translatable("gui.sololeveling.offers"), x, yy, UiKit.BORDER, true);
        button(g, x + w - 90, yy - 3, 86, 14, Component.translatable("gui.sololeveling.refresh"), true, mx, my, () -> act("refresh_offers", "", 0));
        yy += 14;
        if (d.offers.isEmpty()) { g.drawString(font, Component.translatable("gui.sololeveling.no_offers"), x + 4, yy, UiKit.DIM, false); yy += 14; }
        for (int i = 0; i < d.offers.size(); i++) {
            SLPlayer.Quest q = d.offers.get(i);
            UiKit.box(g, x, yy, w, 30, UiKit.BG2, 0xFF22405A);
            g.drawString(font, Quests.questTitle(q), x + 6, yy + 4, UiKit.TEXT, true);
            g.drawString(font, Component.translatable("gui.sololeveling.quest_reward", q.rewardXp, q.rewardGold), x + 6, yy + 17, UiKit.GOLD, false);
            final int qi = i;
            button(g, x + w - 76, yy + 6, 70, 18, Component.translatable("gui.sololeveling.accept"), d.quests.size() < Quests.MAX_ACTIVE, mx, my, () -> act("accept_quest", "", qi));
            yy += 34;
        }
        maxScroll = Math.max(0, yy + scroll - (y + h));
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    private void qline(GuiGraphics g, int x, int y, int w, String label, int have, int need) {
        g.drawString(font, label, x, y, have >= need ? UiKit.GREEN : UiKit.TEXT, false);
        int bx = x + 100;
        UiKit.bar(g, bx, y, w - 100, 10, need == 0 ? 1 : (float) have / need, have >= need ? 0xFF4CC070 : 0xFF3F8CFF);
        g.drawCenteredString(font, Math.min(have, need) + " / " + need, bx + (w - 100) / 2, y + 1, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ MAP
    private void drawMap(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        SLPlayer d = ClientHooks.data;
        Minecraft mc = Minecraft.getInstance();
        int mw = w - 150, mh = h;
        UiKit.box(g, x, y, mw, mh, 0xFF070D18, UiKit.BORDER);
        for (int gx = 0; gx < mw; gx += 20) g.fill(x + gx, y + 1, x + gx + 1, y + mh - 1, 0xFF0E1A2A);
        for (int gy = 0; gy < mh; gy += 20) g.fill(x + 1, y + gy, x + mw - 1, y + gy + 1, 0xFF0E1A2A);
        int minX = -1000, maxX = 1250, minZ = -750, maxZ = 950;
        boolean inHunter = mc.level != null && mc.level.dimension().location().getPath().equals("hunter_world");
        // regions
        for (Content.RegionDef r : Content.REGIONS) {
            int rx = x + 14 + (int) ((r.cx() - minX) / (double) (maxX - minX) * (mw - 28));
            int rz = y + 14 + (int) ((r.cz() - minZ) / (double) (maxZ - minZ) * (mh - 28));
            boolean disc = d.discovered.contains(r.id()) || r.id().equals("seoul");
            boolean sel = r.id().equals(selRegion);
            int rad = 4 + r.radius() / 40;
            int col = disc ? (r.type().equals("ruins") ? 0xFFFF8040 : UiKit.BORDER) : 0xFF445566;
            g.fill(rx - rad - 2, rz - rad - 2, rx + rad + 2, rz + rad + 2, sel ? 0x66FFFFFF : 0x2240B0F0);
            UiKit.box(g, rx - rad, rz - rad, rad * 2, rad * 2, disc ? 0xFF0F2A44 : 0xFF121820, col);
            g.fill(rx - 1, rz - 1, rx + 1, rz + 1, col);
            Component nm = disc ? Component.translatable("region.sololeveling." + r.id()) : Component.literal("???");
            g.drawString(font, nm, rx - font.width(nm) / 2, rz + rad + 3, disc ? UiKit.TEXT : 0xFF667788, true);
            click(rx - rad - 4, rz - rad - 4, rad * 2 + 8, rad * 2 + 8, () -> selRegion = r.id());
        }
        // gates
        for (ClientHooks.GateInfo gi : ClientHooks.gates) {
            int gx2 = x + 14 + (int) ((gi.x() - minX) / (double) (maxX - minX) * (mw - 28));
            int gz2 = y + 14 + (int) ((gi.z() - minZ) / (double) (maxZ - minZ) * (mh - 28));
            if (gx2 < x || gx2 > x + mw || gz2 < y || gz2 > y + mh) continue;
            int col = 0xFF000000 | (gi.red() ? 0xFF3030 : Ranks.color(gi.rank()));
            g.fill(gx2 - 3, gz2 - 3, gx2 + 3, gz2 + 3, 0xFF000000);
            g.fill(gx2 - 2, gz2 - 2, gx2 + 2, gz2 + 2, col);
            if (UiKit.in(mx, my, gx2 - 4, gz2 - 4, 8, 8))
                tooltip = Component.translatable("gui.sololeveling.gate_tooltip", Ranks.tag(gi.rank()), Component.translatable("theme.sololeveling." + gi.theme()), gi.x(), gi.z());
        }
        // player marker
        if (inHunter && mc.player != null) {
            int ppx = x + 14 + (int) ((mc.player.getX() - minX) / (maxX - minX) * (mw - 28));
            int ppz = y + 14 + (int) ((mc.player.getZ() - minZ) / (maxZ - minZ) * (mh - 28));
            ppx = Mth.clamp(ppx, x + 2, x + mw - 3);
            ppz = Mth.clamp(ppz, y + 2, y + mh - 3);
            g.fill(ppx - 2, ppz - 2, ppx + 3, ppz + 3, 0xFFFFFFFF);
            g.drawString(font, Component.translatable("gui.sololeveling.you"), ppx + 5, ppz - 4, 0xFFFFFFFF, true);
        }
        // overworld node
        UiKit.box(g, x + 6, y + mh - 20, 62, 14, 0xFF102018, UiKit.GREEN);
        g.drawCenteredString(font, Component.translatable("gui.sololeveling.overworld"), x + 37, y + mh - 17, UiKit.GREEN);
        click(x + 6, y + mh - 20, 62, 14, () -> selRegion = "overworld");
        // info panel
        int ix = x + mw + 8, iw = w - mw - 8;
        UiKit.box(g, ix, y, iw, h, UiKit.BG2, 0xFF22405A);
        boolean ow = selRegion.equals("overworld");
        Content.RegionDef sr = ow ? null : com.sololeveling.world.Regions.find(selRegion);
        boolean disc = ow || (sr != null && (d.discovered.contains(sr.id()) || sr.id().equals("seoul")));
        Component title = ow ? Component.translatable("gui.sololeveling.overworld") : (disc ? Component.translatable("region.sololeveling." + selRegion) : Component.literal("???"));
        g.drawString(font, title, ix + 6, y + 6, UiKit.TEXT, true);
        if (sr != null && disc) {
            g.drawString(font, Component.translatable("gui.sololeveling.region_type." + sr.type()), ix + 6, y + 20, UiKit.DIM, false);
            g.drawString(font, "X " + sr.cx() + "  Z " + sr.cz(), ix + 6, y + 32, UiKit.DIM, false);
        }
        List<FormattedCharSequence> lines = font.split(Component.translatable("gui.sololeveling.travel_help"), iw - 12);
        int ly = y + 50;
        for (FormattedCharSequence l : lines) { g.drawString(font, l, ix + 6, ly, UiKit.DIM, false); ly += 10; }
        button(g, ix + 6, y + h - 26, iw - 12, 20, Component.translatable("gui.sololeveling.travel"), disc, mx, my, () -> { act("travel", selRegion, 0); onClose(); });
        int near = ClientHooks.gates.size();
        g.drawString(font, Component.translatable("gui.sololeveling.gates_known", near), ix + 6, y + h - 40, UiKit.GOLD, false);
    }

    // ------------------------------------------------------------------ NEWS
    private void drawNews(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.drawString(font, Component.translatable("gui.sololeveling.news_title"), x, y, UiKit.RED, true);
        int yy = y + 14 - scroll;
        int[] cols = {0xFF8FB4CC, 0xFF58A8FF, 0xFF6BE070, 0xFFFF5A5A, 0xFFFFD25A, 0xFFB070FF, 0xFFB0B8C4};
        for (ClientHooks.NewsItem n : ClientHooks.news) {
            List<FormattedCharSequence> lines = font.split(n.text(), w - 80);
            int bh = Math.max(24, lines.size() * 10 + 8);
            UiKit.box(g, x, yy, w, bh, UiKit.BG2, 0xFF22405A);
            g.fill(x, yy, x + 3, yy + bh, cols[Mth.clamp(n.type(), 0, 6)]);
            g.drawString(font, n.time() <= 0 ? "--" : UiKit.time(n.time()), x + 8, yy + 4, UiKit.DIM, false);
            int ly = yy + 4;
            for (FormattedCharSequence l : lines) { g.drawString(font, l, x + 72, ly, UiKit.TEXT, false); ly += 10; }
            yy += bh + 3;
        }
        if (ClientHooks.news.isEmpty()) g.drawString(font, Component.translatable("gui.sololeveling.no_news"), x + 4, y + 16, UiKit.DIM, false);
        maxScroll = Math.max(0, yy + scroll - (y + h));
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    // ------------------------------------------------------------------ GUILD
    private void drawGuild(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        SLPlayer d = ClientHooks.data;
        g.drawString(font, Component.translatable("gui.sololeveling.guild_title"), x, y, UiKit.BORDER, true);
        g.drawString(font, Component.translatable("gui.sololeveling.guild_help", Guilds.JOIN_LEVEL, Guilds.JOIN_COST), x, y + 12, UiKit.DIM, false);
        int yy = y + 28;
        int[] cols = {0xFF58A8FF, 0xFFFF6060, 0xFFFFFFFF};
        for (int i = 0; i < Guilds.IDS.length; i++) {
            String id = Guilds.IDS[i];
            boolean mine = d.guild.equals(id);
            UiKit.box(g, x, yy, w, 44, UiKit.BG2, mine ? UiKit.GREEN : 0xFF22405A);
            g.fill(x, yy, x + 4, yy + 44, cols[i]);
            g.drawString(font, Component.translatable("guild.sololeveling." + id), x + 10, yy + 5, UiKit.TEXT, true);
            g.drawString(font, Component.translatable("guild.sololeveling." + id + ".perk"), x + 10, yy + 18, UiKit.GOLD, false);
            g.drawString(font, Component.translatable("guild.sololeveling." + id + ".leader"), x + 10, yy + 30, UiKit.DIM, false);
            if (mine) button(g, x + w - 96, yy + 12, 88, 20, Component.translatable("gui.sololeveling.leave"), true, mx, my, () -> act("guild_leave", "", 0));
            else g.drawString(font, Component.translatable("gui.sololeveling.guild_npc_hint"), x + w - 10 - font.width(Component.translatable("gui.sololeveling.guild_npc_hint")), yy + 18, 0xFF667788, false);
            yy += 48;
        }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = clicks.size() - 1; i >= 0; i--) {
            Object[] c = clicks.get(i);
            if (UiKit.in(mx, my, (int) c[0], (int) c[1], (int) c[2], (int) c[3])) {
                ((Runnable) c[4]).run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Mth.clamp(scroll - (int) (delta * 14), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientEvents.KEY_SYSTEM.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
