package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Mana orb bar, selected spell, level and the current story objective. */
public final class RpgHud {
    private RpgHud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) return;
        CompoundTag d = ClientRpg.data;
        if (!d.contains("Level")) return;
        var font = mc.font;
        int x = 6, y = h - 34;
        int max = Math.max(1, d.getInt("MaxMana"));
        float mana = d.getFloat("Mana");
        // panel
        g.fill(x - 2, y - 14, x + 104, y + 28, 0x80000000);
        g.drawString(font, "§eSv " + d.getInt("Level") + " §7" + d.getString("Social"), x, y - 12, 0xFFFFFFFF, true);
        g.fill(x, y, x + 100, y + 7, 0xFF0A0A20);
        g.fillGradient(x + 1, y + 1, x + 1 + (int) (98 * Math.min(1, mana / max)), y + 6, 0xFF60A0FF, 0xFF2040C0);
        g.drawCenteredString(font, (int) mana + "/" + max, x + 50, y - 1, 0xFFE0F0FF);
        long xp = d.getLong("Xp"), next = Math.max(1, d.getLong("XpNext"));
        g.fill(x, y + 9, x + 100, y + 11, 0xFF101010);
        g.fill(x, y + 9, x + (int) (100 * Math.min(1, xp / (double) next)), y + 11, 0xFF70E040);
        ListTag sp = d.getList("Spells", Tag.TAG_STRING);
        int sel = d.getInt("Selected");
        if (sel >= 0 && sel < sp.size()) {
            SpellDef s = RpgDefs.SPELL_BY_ID.get(sp.getString(sel));
            if (s != null) {
                CompoundTag cds = d.getCompound("Cds");
                int cd = cds.getInt(s.id());
                String label = "✦ " + s.name() + (cd > 0 ? " §c" + String.format("%.1f", cd / 20F) : " §a✔");
                g.drawString(font, label, x, y + 14, 0xFF000000 | s.color(), true);
            }
        } else {
            g.drawString(font, "§8Büyü yok (R)", x, y + 14, 0xFF808080, true);
        }
        // objective
        String obj = d.getString("Story");
        if (!obj.isEmpty() && mc.screen == null) {
            String ch = d.getString("Chapter");
            int tw = Math.max(font.width(ch), Math.min(220, font.width(obj))) + 8;
            g.fill(4, 4, 4 + tw, 28, 0x70000000);
            g.drawString(font, ch, 8, 7, 0xFFE8C060, true);
            g.drawString(font, font.plainSubstrByWidth(obj, 216), 8, 17, 0xFFF0F0F0, true);
        }
    }
}
