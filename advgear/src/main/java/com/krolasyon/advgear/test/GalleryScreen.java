package com.krolasyon.advgear.test;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Test: eşyaları envanter (GUI) bağlamında büyük ölçekte gösterir. */
public class GalleryScreen extends Screen {
    private final List<Item> items;
    private final int cols;
    private final float scale;

    public GalleryScreen(List<Item> items, int cols, float scale) {
        super(Component.literal("gallery"));
        this.items = items;
        this.cols = cols;
        this.scale = scale;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xFF3A3028, 0xFF241C16);
        int cell = (int) (16 * scale) + 14;
        int rows = (items.size() + cols - 1) / cols;
        int x0 = (width - cols * cell) / 2, y0 = (height - rows * cell) / 2;
        for (int i = 0; i < items.size(); i++) {
            int cx = x0 + (i % cols) * cell, cy = y0 + (i / cols) * cell;
            g.fill(cx + 2, cy + 2, cx + cell - 2, cy + cell - 2, 0x40000000);
            g.pose().pushPose();
            g.pose().translate(cx + 7, cy + 5, 100);
            g.pose().scale(scale, scale, 1);
            g.renderItem(new ItemStack(items.get(i)), 0, 0);
            g.pose().popPose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
