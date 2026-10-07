package com.krolasyon.furniture.client.screen;

import com.krolasyon.furniture.menu.WardrobeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class WardrobeScreen extends AbstractContainerScreen<WardrobeMenu> {
    public WardrobeScreen(WardrobeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 194;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 100;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonfurniture.swap"),
                        b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WardrobeMenu.BTN_SWAP))
                .bounds(leftPos + 84, topPos + 17, 84, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.krolasyonfurniture.swap.tooltip")))
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF24141A);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF6E4038);
        g.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF8C5646);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF7A4A40);
        for (Slot s : menu.slots) {
            int sx = x + s.x, sy = y + s.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2A1A1E);
            g.fill(sx - 1, sy - 1, sx + 16, sy + 16, 0xFF1A0E12);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF5A3434);
        }
        g.drawString(font, Component.translatable("gui.krolasyonfurniture.outfit"), x + 8, y + 36 - 9, 0xF4DDB8, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xF4EBDD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xF4EBDD, false);
    }
}
