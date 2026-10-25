package com.krolasyon.labirent.client.screen;

import com.krolasyon.labirent.client.ClientState;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.net.ServerHooks;
import com.krolasyon.labirent.team.Role;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The captain's table: give orders to everyone, assign jobs to players and Gladers. */
public class CaptainScreen extends Screen {
    private EditBox orderText;
    private int scroll;
    private final List<Net.RosterEntry> roster = new ArrayList<>();
    private boolean captain;

    public CaptainScreen() { super(Component.translatable("screen.labirent.captain")); }

    @Override
    protected void init() {
        Net.State st = ClientState.state;
        Minecraft mc = Minecraft.getInstance();
        captain = st != null && mc.player != null && (mc.player.getUUID().equals(st.captain()) || mc.player.hasPermissions(2));
        roster.clear();
        if (st != null) roster.addAll(st.roster());
        int left = width / 2 - 200;
        int y = 30;
        String[] orders = {"order.labirent.btn.1", "order.labirent.btn.2", "order.labirent.btn.3", "order.labirent.btn.4", "order.labirent.btn.5"};
        int[] ids = {ServerHooks.ORDER_RETURN, ServerHooks.ORDER_RUNNERS, ServerHooks.ORDER_DEFEND, ServerHooks.ORDER_GATHER, ServerHooks.ORDER_WORK};
        for (int k = 0; k < orders.length; k++) {
            int id = ids[k];
            Button b = Button.builder(Component.translatable(orders[k]), btn -> Net.CH.sendToServer(new Net.Order(id, "")))
                    .bounds(left, y + k * 22, 130, 20).build();
            b.active = captain;
            addRenderableWidget(b);
        }
        orderText = new EditBox(font, left, y + 5 * 22 + 6, 130, 18, Component.translatable("screen.labirent.captain.say"));
        orderText.setMaxLength(100);
        orderText.setEditable(captain);
        addRenderableWidget(orderText);
        Button say = Button.builder(Component.translatable("screen.labirent.captain.send"), btn -> {
            if (!orderText.getValue().isBlank()) {
                Net.CH.sendToServer(new Net.Order(ServerHooks.ORDER_SAY, orderText.getValue()));
                orderText.setValue("");
            }
        }).bounds(left, y + 5 * 22 + 28, 130, 20).build();
        say.active = captain;
        addRenderableWidget(say);
        buildRoster();
    }

    private void buildRoster() {
        int left = width / 2 - 55, y = 30;
        int rows = Math.max(1, (height - 70) / 22);
        for (int k = 0; k < rows && k + scroll < roster.size(); k++) {
            Net.RosterEntry e = roster.get(k + scroll);
            Role r = Role.byId(e.role());
            Button b = Button.builder(r.title(), btn -> {
                Role next = Role.byId((Role.byId(e.role()).ordinal() + 1) % Role.values().length);
                Net.CH.sendToServer(new Net.SetRole(e.id(), next.ordinal()));
                int i = roster.indexOf(e);
                Net.RosterEntry n = new Net.RosterEntry(e.id(), e.name(), next.ordinal(), e.npc(), e.x(), e.z(), e.stung(), e.entityId());
                roster.set(i, n);
                rebuildWidgets();
            }).bounds(left + 150, y + k * 22, 100, 20).build();
            b.active = captain;
            addRenderableWidget(b);
            if (!e.npc() && captain && !e.id().equals(Minecraft.getInstance().player.getUUID())) {
                addRenderableWidget(Button.builder(Component.literal("★"), btn -> {
                    Net.CH.sendToServer(new Net.Transfer(e.id()));
                    onClose();
                }).bounds(left + 254, y + k * 22, 20, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("screen.labirent.captain.transfer"))).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFC040);
        int left = width / 2 - 55, y = 30;
        int rows = Math.max(1, (height - 70) / 22);
        for (int k = 0; k < rows && k + scroll < roster.size(); k++) {
            Net.RosterEntry e = roster.get(k + scroll);
            int col = e.stung() ? 0xFF40C040 : e.npc() ? 0xFFE0E0E0 : 0xFF60D0FF;
            String name = (e.npc() ? "" : "☺ ") + e.name() + (Float.isNaN(e.x()) ? "" : "");
            g.drawString(font, name, left, y + k * 22 + 6, col, true);
        }
        if (!captain) g.drawCenteredString(font, Component.translatable("screen.labirent.captain.not"), width / 2, height - 20, 0xFFFF6060);
        else g.drawCenteredString(font, Component.translatable("screen.labirent.captain.hint"), width / 2, height - 20, 0xFFAAAAAA);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        int rows = Math.max(1, (height - 70) / 22);
        scroll = Math.max(0, Math.min(Math.max(0, roster.size() - rows), scroll - (int) Math.signum(d)));
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
