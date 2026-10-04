package com.krolasyon.bosses.rpg.client.gui;

import com.krolasyon.bosses.rpg.client.ClientRpg;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.net.RpgNet;
import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** K: stats, spells and standing with every kingdom. */
public class CharacterScreen extends RpgScreen {
    private int tab;
    private int scroll;

    public CharacterScreen() { super("Karakter"); }

    private static final String[] STATS = {"Güç", "Çeviklik", "Zeka", "Dayanıklılık"};
    private static final String[] STAT_INFO = {"+0.5 yakın dövüş hasarı", "+%1 hız, +%2 saldırı hızı", "+8 mana, büyü gücü", "+2 can"};

    @Override
    protected void init() {
        pw = Math.min(400, width - 20);
        ph = Math.min(250, height - 20);
        left = (width - pw) / 2;
        top = (height - ph) / 2;
        String[] tabs = {"Karakter", "Büyüler", "Saygınlık"};
        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            button((tab == i ? "» " : "") + tabs[i], left + 6 + i * 92, top - 22, 88, 18, b -> { tab = t; scroll = 0; rebuildWidgets(); });
        }
        CompoundTag d = ClientRpg.data;
        if (tab == 0 && d.getInt("Points") > 0) {
            for (int i = 0; i < 4; i++) {
                int s = i;
                button("+", left + 150, top + 104 + i * 14, 14, 12, b -> {
                    CompoundTag t = new CompoundTag();
                    t.putInt("Stat", s);
                    RpgNet.toServer(RpgNet.STAT, t);
                });
            }
        }
        if (tab == 1) {
            ListTag sp = d.getList("Spells", Tag.TAG_STRING);
            int rows = (ph - 40) / 16;
            for (int i = 0; i < Math.min(rows, sp.size() - scroll); i++) {
                int idx = i + scroll;
                SpellDef s = RpgDefs.SPELL_BY_ID.get(sp.getString(idx));
                if (s == null) continue;
                button((idx == d.getInt("Selected") ? "✦ " : "") + s.name(), left + 10, top + 24 + i * 16, 150, 14, b -> {
                    CompoundTag t = new CompoundTag();
                    t.putInt("Idx", idx);
                    RpgNet.toServer(RpgNet.SELECT, t);
                });
            }
            if (sp.size() > rows) {
                button("▲", left + pw - 24, top + 24, 16, 14, b -> { scroll = Math.max(0, scroll - rows); rebuildWidgets(); });
                button("▼", left + pw - 24, top + ph - 22, 16, 14, b -> { scroll = Math.min(Math.max(0, sp.size() - rows), scroll + rows); rebuildWidgets(); });
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        panel(g, left, top, pw, ph);
        CompoundTag d = ClientRpg.data;
        switch (tab) {
            case 0 -> drawCharacter(g, d);
            case 1 -> drawSpells(g, d);
            default -> drawRep(g, d);
        }
        super.render(g, mx, my, pt);
    }

    private void drawCharacter(GuiGraphics g, CompoundTag d) {
        int x = left + 10, y = top + 8;
        String name = minecraft != null && minecraft.player != null ? minecraft.player.getName().getString() : "";
        title(g, name + " — Seviye " + d.getInt("Level"), x, y);
        y += 12;
        g.drawString(font, "TP", x, y + 1, 0xFFB0B0B0, false);
        bar(g, x + 18, y, 160, 9, d.getLong("XpNext") > 0 ? d.getLong("Xp") / (float) d.getLong("XpNext") : 0, 0xFF60C040);
        g.drawString(font, d.getLong("Xp") + "/" + d.getLong("XpNext"), x + 184, y + 1, 0xFFE0E0E0, false);
        y += 12;
        g.drawString(font, "Mana", x, y + 1, 0xFFB0B0B0, false);
        bar(g, x + 28, y, 150, 9, d.getInt("MaxMana") > 0 ? d.getFloat("Mana") / d.getInt("MaxMana") : 0, 0xFF3070F0);
        g.drawString(font, (int) d.getFloat("Mana") + "/" + d.getInt("MaxMana"), x + 184, y + 1, 0xFFE0E0E0, false);
        y += 16;
        g.drawString(font, "Sosyal statü: §e" + d.getString("Social"), x, y, 0xFFE0D0B0, false); y += 11;
        g.drawString(font, "Lonca sınıfı: §e" + d.getString("Guild") + " §7(" + d.getInt("GuildXp") + " puan)", x, y, 0xFFE0D0B0, false); y += 11;
        g.drawString(font, "Şöhret: §e" + d.getInt("Fame") + "  §fÖldürülen: §e" + d.getInt("Kills") + "  §fBoss: §e" + d.getInt("BossKills"), x, y, 0xFFE0D0B0, false); y += 11;
        int[] st = d.getIntArray("Stats");
        g.drawString(font, "Stat puanı: §a" + d.getInt("Points"), x, y + 4, 0xFFE0D0B0, false);
        y = top + 104;
        for (int i = 0; i < 4; i++) {
            g.drawString(font, STATS[i] + ": §f" + (st.length == 4 ? st[i] : 0), x, y + 2 + i * 14, 0xFFC0B090, false);
            g.drawString(font, "§8" + STAT_INFO[i], x + 170, y + 2 + i * 14, 0xFF808080, false);
        }
        y += 64;
        String spouse = d.getString("Spouse");
        g.drawString(font, "Eş: §d" + (spouse.isEmpty() ? "-" : spouse) + "  §fÇocuk: §d" + d.getInt("Children") + "  §fEkip: §a" + d.getInt("Party"), x, y, 0xFFE0D0B0, false);
        y += 11;
        int al = d.getInt("Allegiance");
        g.drawString(font, "Bağlılık: §6" + (al >= 0 ? Kingdom.of(al).title : "Yok"), x, y, 0xFFE0D0B0, false);
    }

    private void drawSpells(GuiGraphics g, CompoundTag d) {
        title(g, "Büyü Kitabın (" + d.getList("Spells", Tag.TAG_STRING).size() + "/" + RpgDefs.SPELLS.size() + ")", left + 10, top + 8);
        ListTag sp = d.getList("Spells", Tag.TAG_STRING);
        int sel = d.getInt("Selected");
        if (sel >= 0 && sel < sp.size()) {
            SpellDef s = RpgDefs.SPELL_BY_ID.get(sp.getString(sel));
            if (s != null) {
                int x = left + 175, y = top + 26;
                g.drawString(font, s.name(), x, y, 0xFF000000 | s.color(), true); y += 12;
                g.drawString(font, "Okul: " + RpgDefs.schoolName(s.school()), x, y, 0xFFC0B090, false); y += 10;
                g.drawString(font, "Tür: " + PlayerMagic.shapeName(s.shape()), x, y, 0xFFC0B090, false); y += 10;
                g.drawString(font, "Güç: " + (int) s.power() + "  Mana: " + s.mana(), x, y, 0xFFC0B090, false); y += 10;
                g.drawString(font, "Bekleme: " + String.format("%.1f", s.cooldown() / 20F) + " sn", x, y, 0xFFC0B090, false); y += 16;
                wrap(g, "R tuşu ile ya da asa elindeyken sağ tıkla kullanırsın. Z/X ile büyüler arasında geçiş yaparsın. Yeni büyüleri büyü kitaplarından öğrenirsin: canavarlar düşürür, büyücüler satar.", x, y, pw - 190, 0xFF908070);
            }
        }
        if (sp.isEmpty()) wrap(g, "Henüz hiç büyü bilmiyorsun. Büyü kitapları bul ya da bir büyücü kulesinden satın al.", left + 10, top + 30, pw - 20, 0xFFB0A080);
    }

    private void drawRep(GuiGraphics g, CompoundTag d) {
        title(g, "Krallıklar ve Saygınlığın", left + 10, top + 8);
        int[] rep = d.getIntArray("Rep");
        int[] bounty = d.getIntArray("Bounty");
        int[] war = d.getIntArray("War");
        int al = d.getInt("Allegiance");
        int y = top + 24;
        for (int i = 0; i < Kingdom.COUNT; i++) {
            Kingdom k = Kingdom.of(i);
            int r = i < rep.length ? rep[i] : 0;
            g.fill(left + 10, y + 2, left + 16, y + 8, 0xFF000000 | k.color);
            g.drawString(font, (al == i ? "§6★ " : "") + k.title, left + 20, y, 0xFFE0D0B0, false);
            g.drawString(font, r + " " + com.krolasyon.bosses.rpg.data.PlayerRpg.repTitle(r), left + 210, y, r >= 0 ? 0xFF80E080 : 0xFFE08080, false);
            if (i < bounty.length && bounty[i] > 0) g.drawString(font, "§cAranıyor", left + 320, y, 0xFFFF6060, false);
            StringBuilder wars = new StringBuilder();
            for (int j = 0; j < Kingdom.COUNT; j++) if (war.length == Kingdom.COUNT * Kingdom.COUNT && war[i * Kingdom.COUNT + j] == 1) wars.append(Kingdom.of(j).capital).append(" ");
            if (wars.length() > 0) g.drawString(font, "§4⚔ " + wars, left + 20, y + 9, 0xFFC04040, false);
            y += wars.length() > 0 ? 20 : 12;
            if (y > top + ph - 12) break;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (tab == 1) {
            int size = ClientRpg.data.getList("Spells", Tag.TAG_STRING).size();
            int rows = (ph - 40) / 16;
            scroll = Math.max(0, Math.min(Math.max(0, size - rows), scroll - (int) Math.signum(delta)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
}
