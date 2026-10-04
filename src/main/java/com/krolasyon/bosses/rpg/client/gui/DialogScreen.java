package com.krolasyon.bosses.rpg.client.gui;

import com.krolasyon.bosses.rpg.net.RpgNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Conversation window: who you talk to, what they say and what you can answer. */
public class DialogScreen extends RpgScreen {
    private final CompoundTag tag;

    public DialogScreen(CompoundTag tag) {
        super(tag.getString("Name"));
        this.tag = tag;
    }

    private List<FormattedCharSequence> lines() {
        return this.font.split(Component.literal(tag.getString("Text")), pw - 80);
    }

    @Override
    protected void init() {
        pw = Math.min(420, this.width - 20);
        ListTag opts = tag.getList("Options", Tag.TAG_COMPOUND);
        boolean two = opts.size() > 8;
        int rows = two ? (opts.size() + 1) / 2 : opts.size();
        int textH = Math.max(50, lines().size() * 10 + 6);
        ph = 34 + textH + rows * 20 + 8;
        left = (this.width - pw) / 2;
        top = Math.max(6, this.height - ph - 10);
        int bw = two ? (pw - 24) / 2 : pw - 20;
        int y0 = top + 34 + textH;
        for (int i = 0; i < opts.size(); i++) {
            CompoundTag o = opts.getCompound(i);
            int col = two ? i % 2 : 0, row = two ? i / 2 : i;
            String id = o.getString("Id");
            String label = o.getString("Label");
            if (this.font.width(label) > bw - 8) label = this.font.plainSubstrByWidth(label, bw - 14) + "…";
            button(label, left + 10 + col * (bw + 4), y0 + row * 20, bw, 18, b -> {
                CompoundTag t = new CompoundTag();
                t.putInt("Npc", tag.getInt("Npc"));
                t.putString("Id", id);
                RpgNet.toServer(RpgNet.CHOICE, t);
                if (id.equals("bye") || id.equals("trade")) this.onClose();
            });
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        panel(g, left, top, pw, ph);
        title(g, tag.getString("Name"), left + 64, top + 6);
        g.drawString(this.font, tag.getString("Title"), left + 64, top + 16, 0xFFB0A080, false);
        g.drawString(this.font, tag.getString("Rel"), left + 64, top + 25, 0xFF90C0E0, false);
        int y = top + 38;
        for (FormattedCharSequence l : lines()) {
            g.drawString(this.font, l, left + 64, y, 0xFFF0E8D8, false);
            y += 10;
        }
        g.fill(left + 6, top + 6, left + 58, top + 74, 0xFF0C0806);
        Entity e = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getEntity(tag.getInt("Npc")) : null;
        if (e instanceof LivingEntity le) {
            float scale = 26.0F / Math.max(1.0F, le.getBbHeight() / 1.9F);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 32, top + 70, (int) scale, left + 32 - mx, top + 30 - my, le);
        }
        super.render(g, mx, my, pt);
    }
}
