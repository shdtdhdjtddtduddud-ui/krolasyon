package com.krolasyon.bosses.rpg.client.gui;

import com.krolasyon.bosses.rpg.client.ClientRpg;
import com.krolasyon.bosses.rpg.net.RpgNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** J: the story so far, side quests, fate bonds and the news of the world. */
public class JournalScreen extends RpgScreen {
    private int tab;

    public JournalScreen() { super("Günlük"); }

    @Override
    protected void init() {
        pw = Math.min(420, width - 20);
        ph = Math.min(250, height - 30);
        left = (width - pw) / 2;
        top = (height - ph) / 2 + 8;
        String[] tabs = {"Hikaye", "Görevler", "Kader Bağları", "Haberler"};
        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            button((tab == i ? "» " : "") + tabs[i], left + 4 + i * 102, top - 22, 98, 18, b -> { tab = t; rebuildWidgets(); });
        }
        if (tab == 1) {
            ListTag q = ClientRpg.data.getList("Quests", Tag.TAG_COMPOUND);
            for (int i = 0; i < q.size(); i++) {
                int idx = i;
                button("✕", left + pw - 22, top + 22 + i * 34, 14, 12, b -> {
                    CompoundTag t = new CompoundTag();
                    t.putInt("Idx", idx);
                    RpgNet.toServer(RpgNet.QUEST_DROP, t);
                });
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        panel(g, left, top, pw, ph);
        CompoundTag d = ClientRpg.data;
        int x = left + 10, y = top + 8;
        switch (tab) {
            case 0 -> {
                title(g, d.getString("Chapter"), x, y);
                y += 16;
                g.drawString(font, "Hedef:", x, y, 0xFFE8C060, false);
                y = wrap(g, d.getString("Story"), x + 40, y, pw - 60, 0xFFF0E8D8) + 10;
                y = wrap(g, "Sen, Solmere'nin Çamur Mahallesi'nde doğmuş yoksul bir çocuksun. Seçimlerin kimin dostun, kimin düşmanın olacağını belirleyecek. "
                        + "Kurtardığın insanlar seni unutmaz; ihanet ettiklerin de.", x, y, pw - 20, 0xFFA09070) + 8;
                g.drawString(font, "Bulunduğun yer: §e" + d.getString("Region"), x, y, 0xFFE0D0B0, false);
            }
            case 1 -> {
                title(g, "Aktif Görevler", x, y);
                ListTag q = d.getList("Quests", Tag.TAG_COMPOUND);
                y += 14;
                if (q.isEmpty()) wrap(g, "Aktif görevin yok. İnsanlarla konuş, Maceracılar Loncası'nın panosuna bak.", x, y, pw - 20, 0xFFA09070);
                for (int i = 0; i < q.size(); i++) {
                    CompoundTag t = q.getCompound(i);
                    g.drawString(font, (t.getBoolean("Done") ? "§a✔ " : "§e• ") + t.getString("Title"), x, y, 0xFFF0E8D8, false);
                    g.drawString(font, "§7" + t.getString("Desc"), x + 10, y + 10, 0xFFB0A080, false);
                    g.drawString(font, "§8Veren: " + t.getString("Giver"), x + 10, y + 20, 0xFF807060, false);
                    y += 34;
                }
            }
            case 2 -> {
                title(g, "Kader Bağların", x, y);
                y += 14;
                ListTag b = d.getList("Bonds", Tag.TAG_STRING);
                if (b.isEmpty()) y = wrap(g, "Henüz kimsenin kaderine dokunmadın. Yolda yardım isteyenleri duy; kurtardığın her can, bir gün karşına yeniden çıkabilir.", x, y, pw - 20, 0xFFA09070);
                for (int i = 0; i < b.size() && y < top + ph - 12; i++) {
                    g.drawString(font, "§b• §f" + b.getString(i), x, y, 0xFFF0E8D8, false);
                    y += 11;
                }
            }
            default -> {
                title(g, "Dünyadan Haberler", x, y);
                y += 14;
                ListTag n = d.getList("News", Tag.TAG_STRING);
                for (int i = 0; i < n.size() && y < top + ph - 20; i++) y = wrap(g, "• " + n.getString(i), x, y, pw - 20, 0xFFE0D0B0) + 3;
            }
        }
        super.render(g, mx, my, pt);
    }
}
