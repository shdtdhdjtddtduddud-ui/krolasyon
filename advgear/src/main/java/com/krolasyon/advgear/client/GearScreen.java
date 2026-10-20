package com.krolasyon.advgear.client;

import com.krolasyon.advgear.gear.GearMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Kuşanım ekranı: deri ve pirinç görünümlü panel, ortada oyuncu, iki yanda kuşanım yuvaları. */
public class GearScreen extends AbstractContainerScreen<GearMenu> {
    private static final String[] LABELS = {"gui.advgear.slot.ring", "gui.advgear.slot.ring", "gui.advgear.slot.goggles", "gui.advgear.slot.belt",
            "gui.advgear.slot.back"};

    public GearScreen(GearMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        // boş yuvanın üstündeyken ne takılacağını söyle
        for (int i = 0; i < 5; i++) {
            Slot s = menu.slots.get(i);
            if (!s.hasItem() && isHovering(s.x, s.y, 16, 16, mx, my)) g.renderTooltip(font, Component.translatable(LABELS[i]), mx, my);
        }
    }

    private void frame(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A1B10);
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, 0x40FFFFFF);
    }

    private void slot(GuiGraphics g, int x, int y, int accent) {
        g.fill(x - 1, y - 1, x + 17, y + 17, accent);
        g.fill(x, y, x + 16, y + 16, 0xFF3B2A1C);
        g.fill(x, y, x + 16, y + 1, 0xFF1E140C);
        g.fill(x, y, x + 1, y + 16, 0xFF1E140C);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        // deri panel
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF6B4A2E);
        g.fillGradient(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF8A6440, 0xFF6E4B2D);
        // dikiş çizgisi
        for (int i = x + 6; i < x + imageWidth - 6; i += 4) {
            g.fill(i, y + 5, i + 2, y + 6, 0xFFCDB089);
            g.fill(i, y + imageHeight - 6, i + 2, y + imageHeight - 5, 0xFFCDB089);
        }
        // pirinç köşe süsleri
        for (int[] c : new int[][]{{x, y}, {x + imageWidth - 8, y}, {x, y + imageHeight - 8}, {x + imageWidth - 8, y + imageHeight - 8}}) {
            g.fill(c[0], c[1], c[0] + 8, c[1] + 8, 0xFFB8892F);
            g.fill(c[0] + 2, c[1] + 2, c[0] + 6, c[1] + 6, 0xFFE0B65A);
        }
        // oyuncu penceresi
        frame(g, x + 50, y + 8, 76, 66, 0xFF1F1610);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 88, y + 70, 28, x + 88 - mx, y + 30 - my, minecraft.player);
        int gold = 0xFFB8892F;
        for (int i = 0; i < 5; i++) slot(g, x + GearMenu.POS[i][0], y + GearMenu.POS[i][1], gold);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) slot(g, x + 8 + c * 18, y + 84 + r * 18, 0xFF2A1B10);
        for (int c = 0; c < 9; c++) slot(g, x + 8 + c * 18, y + 142, 0xFF2A1B10);
        // boş yuvalarda soluk simgeler
        for (int i = 0; i < 5; i++) {
            if (menu.slots.get(i).hasItem()) continue;
            int sx = x + GearMenu.POS[i][0], sy = y + GearMenu.POS[i][1];
            int c = 0x55E0B65A;
            switch (i) {
                case 0, 1 -> { g.fill(sx + 4, sy + 5, sx + 12, sy + 7, c); g.fill(sx + 4, sy + 11, sx + 12, sy + 13, c);
                    g.fill(sx + 3, sy + 7, sx + 5, sy + 11, c); g.fill(sx + 11, sy + 7, sx + 13, sy + 11, c); }
                case 2 -> { g.fill(sx + 2, sy + 6, sx + 7, sy + 11, c); g.fill(sx + 9, sy + 6, sx + 14, sy + 11, c); g.fill(sx + 7, sy + 7, sx + 9, sy + 8, c); }
                case 3 -> { g.fill(sx + 1, sy + 7, sx + 15, sy + 10, c); g.fill(sx + 6, sy + 6, sx + 10, sy + 11, c); }
                default -> { g.fill(sx + 3, sy + 3, sx + 13, sy + 14, c); g.fill(sx + 2, sy + 3, sx + 14, sy + 7, c); }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, -10, 0xFFE0B65A, true);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFF0E0C0, false);
    }
}
