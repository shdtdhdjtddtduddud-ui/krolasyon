package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.item.PartItem;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.workbench.WorkbenchMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The Beyblade Workbench screen: parts around the beyblade, upgrade buttons, live stats and a turning 3D preview. */
public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private static final int W = 258, H = 250;
    /** where each part slot's upgrade button sits, relative to the slot */
    private static final int[][] BTN = {{19, 4}, {-1, 19}, {6, 19}, {-1, 19}, {6, 19}};
    private final Button[] upgrade = new Button[5];
    private static final String[] STATS = {"atk", "def", "sta", "spd"};
    private static final int[] STAT_COL = {0xFFFF5A4A, 0xFF5A9AFF, 0xFF5AE07A, 0xFF5AE8F0};

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.inventoryLabelX = WorkbenchMenu.INV_X;
        this.inventoryLabelY = WorkbenchMenu.INV_Y - 11;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        for (PartType t : PartType.values()) {
            int i = t.ordinal();
            int[] p = WorkbenchMenu.POS[1 + i];
            upgrade[i] = addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
            }).bounds(leftPos + p[0] + BTN[i][0], topPos + p[1] + BTN[i][1], 11, 11).build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (PartType t : PartType.values()) {
            Button b = upgrade[t.ordinal()];
            ItemStack s = menu.partStack(t);
            b.visible = s.getItem() instanceof PartItem;
            b.active = menu.canUpgrade(t);
            if (b.visible) {
                int lvl = PartItem.level(s);
                b.setTooltip(Tooltip.create(lvl >= Loadout.MAX_LEVEL ? Component.translatable("screen.librabeyblade.workbench.maxed")
                        : Component.translatable("screen.librabeyblade.workbench.upgrade", lvl + 1, WorkbenchMenu.UPGRADE_COUNT[lvl],
                        WorkbenchMenu.UPGRADE_ITEM[lvl].getDescription())));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // frame and panels
        g.fill(x, y, x + W, y + H, 0xFF1A1428);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, 0xFF2A2140);
        g.fillGradient(x + 4, y + 16, x + 128, y + 152, 0xFF3A2E5A, 0xFF231A38);
        g.fillGradient(x + 131, y + 16, x + W - 4, y + 152, 0xFF2C2446, 0xFF1E1830);
        // spokes from the beyblade to its parts
        int cx = x + WorkbenchMenu.POS[0][0] + 8, cy = y + WorkbenchMenu.POS[0][1] + 8;
        for (int i = 1; i <= 5; i++) line(g, cx, cy, x + WorkbenchMenu.POS[i][0] + 8, y + WorkbenchMenu.POS[i][1] + 8, 0x66B080FF);
        for (Slot s : this.menu.slots) {
            boolean special = s.index < WorkbenchMenu.SIZE;
            int col = s.index == WorkbenchMenu.BEY ? 0xFFFFC040 : special ? 0xFF9A7AE0 : 0xFF4A4060;
            g.fill(x + s.x - 2, y + s.y - 2, x + s.x + 18, y + s.y + 18, col);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF120E1E);
        }
        // little captions over the part slots
        g.pose().pushPose();
        g.pose().scale(0.6F, 0.6F, 1F);
        for (PartType t : PartType.values()) {
            int[] p = WorkbenchMenu.POS[1 + t.ordinal()];
            Component c = Component.translatable(t.nameKey());
            g.drawString(this.font, c, (int) ((x + p[0] + 8) / 0.6F) - this.font.width(c) / 2, (int) ((y + p[1] - 8) / 0.6F), 0xFFD8C8FF, false);
        }
        int[] mp = WorkbenchMenu.POS[WorkbenchMenu.MATERIAL];
        Component mc = Component.translatable("screen.librabeyblade.workbench.material");
        g.drawString(this.font, mc, (int) ((x + mp[0] + 8) / 0.6F) - this.font.width(mc) / 2, (int) ((y + mp[1] - 8) / 0.6F), 0xFFFFE080, false);
        g.pose().popPose();
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= n; i += 2) {
            int px = x0 + (x1 - x0) * i / Math.max(1, n), py = y0 + (y1 - y0) * i / Math.max(1, n);
            g.fill(px, py, px + 1, py + 1, col);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFD860, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFC8C0E0, false);
        int px = 134, py = 20;
        ItemStack bey = menu.bey();
        Loadout l = menu.loadout();
        if (l == null) {
            g.drawWordWrap(this.font, Component.translatable("screen.librabeyblade.workbench.empty"), px, py + 30, 118, 0xFFB0A8D0);
            return;
        }
        // turning 3D preview
        g.pose().pushPose();
        g.pose().translate(W - 26, py - 2, 100);
        g.pose().scale(1.3F, 1.3F, 1.3F);
        g.renderItem(bey, 0, 0);
        g.pose().popPose();
        g.drawString(this.font, trim(bey.getHoverName(), 94), px, py, 0xFFFFFFFF, false);
        String[] lean = {"attack", "defense", "stamina", "balance"};
        g.drawString(this.font, Component.translatable("screen.librabeyblade.workbench.type",
                Component.translatable("mode.librabeyblade." + lean[l.leaning()])), px, py + 11, 0xFFFFC870, false);
        if (!l.complete()) g.drawString(this.font, Component.translatable("screen.librabeyblade.workbench.incomplete"), px, py + 22, 0xFFFF6060, false);
        float[] vals = {l.attack(), l.defense(), l.stamina(), l.speed()};
        for (int i = 0; i < 4; i++) {
            int by = py + 34 + i * 11;
            g.drawString(this.font, Component.translatable("stat.librabeyblade." + STATS[i] + ".short"), px, by, STAT_COL[i], false);
            int bw = 70;
            g.fill(px + 26, by + 1, px + 26 + bw, by + 7, 0xFF120E1E);
            int fill = Math.min(bw, Math.round(vals[i] / 90F * bw));
            g.fill(px + 26, by + 1, px + 26 + fill, by + 7, STAT_COL[i]);
            g.drawString(this.font, String.valueOf(Math.round(vals[i])), px + 100, by, 0xFFFFFFFF, false);
        }
        int iy = py + 80;
        g.drawString(this.font, Component.translatable("stat.librabeyblade.weight", String.format(java.util.Locale.ROOT, "%.1f", l.weight())), px, iy, 0xFFD0D0D8, false);
        g.drawString(this.font, Component.translatable("stat.librabeyblade.height", String.format(java.util.Locale.ROOT, "%.1f", l.height() / 10F)), px, iy + 10, 0xFFD0D0D8, false);
        // tip and track specials, small and wrapped so the whole sentence fits
        g.pose().pushPose();
        g.pose().translate(px, iy + 21, 0);
        g.pose().scale(0.7F, 0.7F, 1F);
        int ty = 0;
        Component tip = Component.translatable("tip.librabeyblade." + l.tip().name().toLowerCase(java.util.Locale.ROOT));
        g.drawWordWrap(this.font, tip, 0, ty, 168, 0xFF80E0F0);
        ty += this.font.wordWrapHeight(tip, 168) + 2;
        if (l.trackTrait() != BeyPart.Trait.NONE)
            g.drawWordWrap(this.font, Component.translatable("trait.librabeyblade." + l.trackTrait().name().toLowerCase(java.util.Locale.ROOT)), 0, ty, 168, 0xFFFFB0E0);
        g.pose().popPose();
    }

    private Component trim(Component c, int w) {
        String s = c.getString();
        if (this.font.width(s) <= w) return c;
        return Component.literal(this.font.plainSubstrByWidth(s, w - 6) + "…");
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        // for parts sitting in the part slots show the full part card
        if (this.hoveredSlot != null && this.hoveredSlot.index > 0 && this.hoveredSlot.index < 6 && this.hoveredSlot.hasItem()
                && this.hoveredSlot.getItem().getItem() instanceof PartItem pi) {
            List<Component> lines = new ArrayList<>();
            lines.add(this.hoveredSlot.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
            PartItem.appendPartLines(pi.part(), PartItem.level(this.hoveredSlot.getItem()), lines);
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }
}
