package com.sololeveling.client.gui;

import com.sololeveling.client.ClientHooks;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModItems;
import com.sololeveling.system.Guilds;
import com.sololeveling.system.Shop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dialogue / trade window for hunter-world NPCs. */
public class NpcScreen extends Screen {
    private final int entityId;
    private final String role, name, guild;
    private int px, py, pw, ph;
    private final List<Object[]> clicks = new ArrayList<>();
    private int scroll = 0, maxScroll = 0;
    private boolean showGates = false;

    public NpcScreen(int entityId, String role, String name, String guild) {
        super(Component.literal(name));
        this.entityId = entityId;
        this.role = role;
        this.name = name;
        this.guild = guild;
    }

    @Override
    protected void init() {
        if (role.equals("receptionist")) Net.toServer(new Packets.Action("gates", "", 0));
    }

    @Override public boolean isPauseScreen() { return false; }

    private void act(String a, String arg, int n) { Net.toServer(new Packets.Action(a, arg, n)); }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean enabled, int mx, int my, Runnable r) {
        boolean hov = enabled && UiKit.in(mx, my, x, y, w, h);
        UiKit.box(g, x, y, w, h, enabled ? (hov ? 0xFF1E5A86 : 0xFF12304A) : 0xFF141A24, enabled ? (hov ? 0xFFFFFFFF : UiKit.BORDER) : 0xFF334455);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? UiKit.TEXT : 0xFF667788);
        if (enabled) clicks.add(new Object[]{x, y, w, h, r});
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        clicks.clear();
        pw = Math.min(width - 12, 400);
        ph = Math.min(height - 12, 230);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        UiKit.panel(g, px, py, pw, ph);
        Minecraft mc = Minecraft.getInstance();
        SLPlayer d = ClientHooks.data;
        // portrait
        UiKit.box(g, px + 8, py + 8, 84, ph - 16, 0xFF070D18, 0xFF22405A);
        Entity e = mc.level == null ? null : mc.level.getEntity(entityId);
        if (e instanceof LivingEntity le) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 50, py + ph - 30, 38, (float) (px + 50) - mx, (float) (py + ph - 80) - my, le);
        }
        g.drawCenteredString(font, Component.literal(name), px + 50, py + 12, UiKit.TEXT);
        g.drawCenteredString(font, Component.translatable("gui.sololeveling.role." + role), px + 50, py + 23, UiKit.BORDER);
        int x = px + 100, w = pw - 108, y = py + 10;
        g.drawString(font, "G " + d.gold, px + pw - 10 - font.width("G " + d.gold), y - 2, UiKit.GOLD, true);
        // dialogue
        int line = 1 + Math.floorMod(entityId, 3);
        List<FormattedCharSequence> txt = font.split(Component.translatable("npc.sololeveling." + role + ".line" + line), w - 10);
        int ty = y + 12;
        UiKit.box(g, x, y + 8, w, 12 + txt.size() * 10 + 4, UiKit.BG2, 0xFF22405A);
        for (FormattedCharSequence s : txt) { g.drawString(font, s, x + 6, ty + 2, UiKit.TEXT, false); ty += 10; }
        int by = ty + 14;
        switch (role) {
            case "receptionist" -> {
                button(g, x, by, w, 18, Component.translatable("gui.sololeveling.npc.register"), true, mx, my, () -> act("register", "", 0));
                button(g, x, by + 22, w, 18, Component.translatable("gui.sololeveling.npc.quests"), true, mx, my, () -> { act("refresh_offers", "", 0); onClose(); ClientHooks.openSystem(2); });
                button(g, x, by + 44, w, 18, Component.translatable("gui.sololeveling.npc.gates", ClientHooks.gates.size()), true, mx, my, () -> showGates = !showGates);
                button(g, x, by + 66, w, 18, Component.translatable("gui.sololeveling.npc.map"), true, mx, my, () -> { onClose(); ClientHooks.openSystem(3); });
                if (showGates) {
                    int gy = by + 90;
                    if (ClientHooks.gates.isEmpty()) g.drawString(font, Component.translatable("gui.sololeveling.no_gates"), x, gy, UiKit.DIM, false);
                    int i = 0;
                    for (ClientHooks.GateInfo gi : ClientHooks.gates) {
                        if (i++ > 4) break;
                        g.drawString(font, Component.translatable("gui.sololeveling.gate_tooltip", com.sololeveling.util.Ranks.tag(gi.rank()), Component.translatable("theme.sololeveling." + gi.theme()), gi.x(), gi.z()), x, gy, UiKit.TEXT, false);
                        gy += 10;
                    }
                }
            }
            case "guild_master" -> {
                if (guild.isEmpty() || !Guilds.valid(guild)) {
                    g.drawString(font, Component.translatable("gui.sololeveling.no_guild_here"), x, by, UiKit.DIM, false);
                } else {
                    g.drawString(font, Component.translatable("guild.sololeveling." + guild), x, by, UiKit.GOLD, true);
                    g.drawString(font, Component.translatable("guild.sololeveling." + guild + ".perk"), x, by + 12, UiKit.TEXT, false);
                    g.drawString(font, Component.translatable("gui.sololeveling.guild_help", Guilds.JOIN_LEVEL, Guilds.JOIN_COST), x, by + 24, UiKit.DIM, false);
                    boolean mine = d.guild.equals(guild);
                    if (mine) button(g, x, by + 44, w, 20, Component.translatable("gui.sololeveling.leave"), true, mx, my, () -> act("guild_leave", "", 0));
                    else button(g, x, by + 44, w, 20, Component.translatable("gui.sololeveling.join"), d.guild.isEmpty() && d.level >= Guilds.JOIN_LEVEL && d.gold >= Guilds.JOIN_COST, mx, my, () -> act("guild_join", guild, 0));
                }
            }
            case "merchant" -> drawShop(g, x, by - 4, w, ph - (by - py) - 8, mx, my);
            case "journalist" -> {
                button(g, x, by, w, 20, Component.translatable("gui.sololeveling.npc.read_news"), true, mx, my, () -> { onClose(); ClientHooks.openSystem(4); });
                button(g, x, by + 26, w, 20, Component.translatable("gui.sololeveling.npc.map"), true, mx, my, () -> { onClose(); ClientHooks.openSystem(3); });
            }
            case "healer" -> {
                long cost = 50 + d.level * 5L;
                button(g, x, by, w, 22, Component.translatable("gui.sololeveling.npc.heal", cost), d.gold >= cost, mx, my, () -> act("heal", "", 0));
            }
            default -> {
                button(g, x, by, w, 20, Component.translatable("gui.sololeveling.npc.map"), true, mx, my, () -> { onClose(); ClientHooks.openSystem(3); });
            }
        }
        button(g, px + pw - 74, py + ph - 26, 66, 18, Component.translatable("gui.sololeveling.close"), true, mx, my, this::onClose);
    }

    private void drawShop(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        SLPlayer d = ClientHooks.data;
        int rowH = 22;
        List<Map.Entry<String, Integer>> items = new ArrayList<>(Shop.PRICES.entrySet());
        int listH = h - 28;
        maxScroll = Math.max(0, items.size() * rowH - listH);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        g.enableScissor(x, y, x + w, y + listH);
        for (int i = 0; i < items.size(); i++) {
            int ry = y + i * rowH - scroll;
            if (ry + rowH < y || ry > y + listH) continue;
            Map.Entry<String, Integer> en = items.get(i);
            UiKit.box(g, x, ry, w, rowH - 2, UiKit.BG2, 0xFF22405A);
            ItemStack st = new ItemStack(ModItems.get(en.getKey()));
            g.renderItem(st, x + 3, ry + 1);
            g.drawString(font, Component.translatable("item.sololeveling." + en.getKey()), x + 24, ry + 5, UiKit.TEXT, false);
            String price = en.getValue() + " G";
            g.drawString(font, price, x + w - 70 - font.width(price), ry + 5, d.gold >= en.getValue() ? UiKit.GOLD : UiKit.RED, false);
            final String id = en.getKey();
            if (my < y + listH) button(g, x + w - 62, ry + 2, 56, rowH - 6, Component.translatable("gui.sololeveling.buy"), d.gold >= en.getValue(), mx, my, () -> act("buy", id, hasShiftDown() ? 5 : 1));
            if (UiKit.in(mx, my, x + 24, ry, w - 90, rowH - 2) && my < y + listH) g.renderTooltip(font, Component.translatable("item.sololeveling." + id + ".desc"), mx, my);
        }
        g.disableScissor();
        g.drawString(font, Component.translatable("gui.sololeveling.shop_hint"), x, y + h - 22, UiKit.DIM, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = clicks.size() - 1; i >= 0; i--) {
            Object[] c = clicks.get(i);
            if (UiKit.in(mx, my, (int) c[0], (int) c[1], (int) c[2], (int) c[3])) { ((Runnable) c[4]).run(); return true; }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Mth.clamp(scroll - (int) (delta * 14), 0, maxScroll);
        return true;
    }
}
