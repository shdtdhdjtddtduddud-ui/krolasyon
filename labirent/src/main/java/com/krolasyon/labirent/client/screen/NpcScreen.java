package com.krolasyon.labirent.client.screen;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.team.Role;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** Orders for one Glader. */
public class NpcScreen extends Screen {
    private final GladerEntity g;

    public NpcScreen(GladerEntity g) {
        super(g.getDisplayName());
        this.g = g;
    }

    @Override
    protected void init() {
        int x = width / 2 + 10, y = height / 2 - 80;
        String[] cmds = {"npc.labirent.follow", "npc.labirent.stay", "npc.labirent.work", "npc.labirent.return", "npc.labirent.home"};
        for (int k = 0; k < cmds.length; k++) {
            int id = k;
            addRenderableWidget(Button.builder(Component.translatable(cmds[k]), b -> {
                Net.CH.sendToServer(new Net.NpcCommand(g.getId(), id));
                onClose();
            }).bounds(x, y + k * 22, 120, 20).build());
        }
        int ry = y + cmds.length * 22 + 8;
        Role[] roles = Role.values();
        for (int k = 0; k < roles.length; k++) {
            Role r = roles[k];
            Button b = Button.builder(r.title(), btn -> {
                Net.CH.sendToServer(new Net.NpcCommand(g.getId(), 10 + r.ordinal()));
                onClose();
            }).bounds(x + (k % 2) * 62, ry + (k / 2) * 22, 60, 20).build();
            b.active = r != g.getRole();
            addRenderableWidget(b);
        }
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float partial) {
        renderBackground(gg);
        int cx = width / 2 - 70, cy = height / 2 + 60;
        InventoryScreen.renderEntityInInventoryFollowsMouse(gg, cx, cy, 55, cx - mx, cy - 90 - my, g);
        gg.drawCenteredString(font, title, width / 2, 20, 0xFFFFFFFF);
        gg.drawCenteredString(font, g.getRole().description(), width / 2, 34, 0xFFAAAAAA);
        gg.drawString(font, Component.translatable("npc.labirent.health", (int) g.getHealth(), (int) g.getMaxHealth()), cx - 40, cy + 8, 0xFFFF8080, true);
        super.render(gg, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
