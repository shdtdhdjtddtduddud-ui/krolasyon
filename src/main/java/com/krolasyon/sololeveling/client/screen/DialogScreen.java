package com.krolasyon.sololeveling.client.screen;

import com.krolasyon.sololeveling.client.ClientHooks;
import com.krolasyon.sololeveling.client.Holo;
import com.krolasyon.sololeveling.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** NPC conversation: portrait, name, typewriter text and answer buttons. */
public class DialogScreen extends Screen {
    private final int npc;
    private final Component name, text;
    private final List<String> ids = new ArrayList<>();
    private final List<Component> labels = new ArrayList<>();
    private int ticks;

    public DialogScreen(CompoundTag t) {
        super(Component.empty());
        npc = t.getInt("npc");
        name = parse(t.getString("name"));
        text = parse(t.getString("text"));
        for (Tag x : t.getList("options", Tag.TAG_COMPOUND)) {
            CompoundTag o = (CompoundTag) x;
            ids.add(o.getString("id"));
            labels.add(parse(o.getString("label")));
        }
    }

    static Component parse(String json) {
        try {
            Component c = Component.Serializer.fromJson(json);
            return c == null ? Component.empty() : c;
        } catch (RuntimeException e) {
            return Component.literal(json);
        }
    }

    @Override
    public void tick() { ticks++; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        int w = Math.min(420, width - 20), h = 112;
        int x = (width - w) / 2, y = height - h - 12;
        g.fillGradient(0, height / 2, width, height, 0, Holo.a(0x000814, 0.6F));
        Entity e = minecraft.level == null ? null : minecraft.level.getEntity(npc);
        if (e instanceof LivingEntity le) {
            Holo.panel(g, x, y - 92, 70, 88, 0.9F, Holo.CYAN);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 35, y - 10, 36, x + 35 - mx, y - 60 - my, le);
        }
        Holo.panel(g, x, y, w, h, 1F, Holo.CYAN);
        g.fill(x + 4, y + 4, x + 6 + font.width(name) + 8, y + 16, Holo.a(Holo.CYAN, 0.25F));
        g.drawString(font, name, x + 9, y + 6, Holo.GOLD, true);
        // typewriter text
        String full = text.getString();
        int shown = Math.min(full.length(), (int) ((ticks + partial) * 2.5F));
        Component visible = shown >= full.length() ? text : Component.literal(full.substring(0, shown));
        List<FormattedCharSequence> lines = font.split(visible, w - 20);
        int ly = y + 22;
        for (FormattedCharSequence l : lines) {
            if (ly > y + h - 30) break;
            g.drawString(font, l, x + 10, ly, Holo.TEXT, false);
            ly += 10;
        }
        int bx = x + 8, by = y + h - 20;
        for (int i = 0; i < labels.size(); i++) {
            int bw = Math.min(140, font.width(labels.get(i)) + 16);
            if (bx + bw > x + w - 8) {
                bx = x + 8;
                by -= 16;
            }
            Holo.button(g, font, labels.get(i), bx, by, bw, 14, mx, my, true, ids.get(i).equals("bye") ? 0x8899AA : Holo.CYAN);
            bx += bw + 4;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int w = Math.min(420, width - 20), h = 112;
        int x = (width - w) / 2, y = height - h - 12;
        String full = text.getString();
        if (ticks * 2.5F < full.length()) {
            ticks = 10000;
            return true;
        }
        int bx = x + 8, by = y + h - 20;
        for (int i = 0; i < labels.size(); i++) {
            int bw = Math.min(140, font.width(labels.get(i)) + 16);
            if (bx + bw > x + w - 8) {
                bx = x + 8;
                by -= 16;
            }
            if (Holo.in(mx, my, bx, by, bw, 14)) {
                ClientHooks.playClick();
                if (ids.get(i).equals("bye")) onClose();
                else Net.toServer(new Net.Action("dialog", npc, 0, ids.get(i)));
                return true;
            }
            bx += bw + 4;
        }
        return super.mouseClicked(mx, my, b);
    }
}
