package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

/** Command menu (default key B): tell the active beyblade what to do and switch its battle mode. */
public class CommandScreen extends Screen {
    private static final int BW = 104, BH = 20, GAP = 4;

    public CommandScreen() {
        super(Component.translatable("screen.librabeyblade.commands"));
    }

    @Nullable
    private static BeybladeEntity active() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        List<BeybladeEntity> list = mc.level.getEntitiesOfClass(BeybladeEntity.class, mc.player.getBoundingBox().inflate(96),
                b -> mc.player.getUUID().equals(b.getOwnerId()));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    protected void init() {
        int cols = 3;
        int rows = (BeybladeEntity.ORDER_COUNT + cols - 1) / cols;
        int totalW = cols * BW + (cols - 1) * GAP;
        int x0 = (this.width - totalW) / 2, y0 = Math.max(30, this.height / 2 - (rows * (BH + GAP) + 50) / 2);
        for (int i = 0; i < BeybladeEntity.ORDER_COUNT; i++) {
            final int id = i;
            int cx = x0 + (i % cols) * (BW + GAP), cy = y0 + (i / cols) * (BH + GAP);
            String key = "order.librabeyblade." + BeybladeEntity.ORDER_IDS[i];
            addRenderableWidget(Button.builder(Component.translatable(key), b -> {
                        ModNetwork.sendCommand(ModNetwork.CMD_ORDER + id);
                        onClose();
                    }).bounds(cx, cy, BW, BH).tooltip(Tooltip.create(Component.translatable(key + ".desc"))).build());
        }
        int my = y0 + rows * (BH + GAP) + 22;
        int mw = (totalW - 3 * GAP) / 4;
        for (int m = 0; m < BeybladeEntity.MODE_COUNT; m++) {
            final int id = m;
            String key = "mode.librabeyblade." + BeybladeEntity.MODE_IDS[m];
            addRenderableWidget(Button.builder(Component.translatable(key), b -> ModNetwork.sendCommand(ModNetwork.CMD_MODE + id))
                    .bounds(x0 + m * (mw + GAP), my, mw, BH).tooltip(Tooltip.create(Component.translatable(key + ".desc"))).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int cols = 3, rows = (BeybladeEntity.ORDER_COUNT + cols - 1) / cols;
        int totalW = cols * BW + (cols - 1) * GAP;
        int x0 = (this.width - totalW) / 2, y0 = Math.max(30, this.height / 2 - (rows * (BH + GAP) + 50) / 2);
        g.drawCenteredString(this.font, this.title, this.width / 2, y0 - 22, 0xFFFFD040);
        BeybladeEntity b = active();
        if (b == null) {
            g.drawCenteredString(this.font, Component.translatable("msg.librabeyblade.none_active"), this.width / 2, y0 - 11, 0xFFFF7070);
        } else {
            // highlight the current order and mode
            int o = b.getOrder();
            int ox = x0 + (o % cols) * (BW + GAP), oy = y0 + (o / cols) * (BH + GAP);
            g.fill(ox - 2, oy - 2, ox + BW + 2, oy + BH + 2, 0xFFFFD040);
            int my = y0 + rows * (BH + GAP) + 22;
            int mw = (totalW - 3 * GAP) / 4;
            int mx = x0 + b.getMode() * (mw + GAP);
            g.fill(mx - 2, my - 2, mx + mw + 2, my + BH + 2, 0xFF60D0FF);
            g.drawCenteredString(this.font, Component.translatable("screen.librabeyblade.level", b.getLevel(), b.getXp(), BeybladeEntity.xpNeeded(b.getLevel())),
                    this.width / 2, y0 - 11, 0xFFB0FFB0);
        }
        int my = y0 + rows * (BH + GAP) + 10;
        g.drawCenteredString(this.font, Component.translatable("screen.librabeyblade.modes"), this.width / 2, my, 0xFF9CD8FF);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientSetup.COMMAND_KEY.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
