package com.rabona.arena.client;

import com.rabona.arena.game.Move;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Hareket kutuphanesi: kategoriler, 3B onizleme, tus atama ve deneme. */
public class MoveScreen extends Screen {
    private static Move.Cat tab = Move.Cat.SKILL;
    private Move selected;
    private int replayAt;
    private final List<Move> list = new ArrayList<>();
    private final List<Button> actions = new ArrayList<>();

    public MoveScreen() { super(Component.translatable("screen.rabonaarena.moves")); }

    @Override
    protected void init() {
        int x = 10, y = 24;
        for (Move.Cat c : Move.Cat.values()) {
            if (c == Move.Cat.REACTION) continue;
            int bw = font.width(c.title()) + 12;
            addRenderableWidget(Button.builder(c.title(), b -> {
                tab = c;
                rebuild();
            }).bounds(x, y, bw, 18).build());
            x += bw + 2;
        }
        rebuild();
    }

    private void rebuild() {
        list.clear();
        for (Move m : Move.values()) if (m.cat == tab && m.selectable()) list.add(m);
        if (selected == null || selected.cat != tab) select(list.isEmpty() ? null : list.get(0));
        actions.forEach(this::removeWidget);
        actions.clear();
        int px = width - 210, py = height - 74;
        if (selected == null) return;
        if (selected.cat == Move.Cat.SHOT) {
            for (int i = 0; i < 3; i++) {
                final int slot = i;
                act(Component.translatable("screen.rabonaarena.assign", Keys.SHOTS[i].getTranslatedKeyMessage()), px + i * 67, py + 22, 65,
                        () -> ClientState.shotSlots[slot] = selected);
            }
        } else if (selected.cat == Move.Cat.ABILITY) {
            act(Component.translatable("screen.rabonaarena.set_ability"), px, py + 22, 200, () -> ClientState.ability = selected);
        } else if (selected.cat == Move.Cat.CELEBRATION) {
            act(Component.translatable("screen.rabonaarena.set_celebration"), px, py + 22, 200, () -> ClientState.celebration = selected);
        } else {
            for (int i = 0; i < 3; i++) {
                final int slot = i;
                act(Component.translatable("screen.rabonaarena.assign", Keys.SKILLS[i].getTranslatedKeyMessage()), px + i * 67, py + 22, 65,
                        () -> ClientState.slots[slot] = selected);
            }
        }
        actions.add(addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.try"), b -> {
            Move m = selected;
            onClose();
            ClientInput.send(0, m, 1, 0, false);
        }).bounds(px, py + 44, 200, 18).build()));
    }

    private void act(Component label, int x, int y, int w, Runnable r) {
        actions.add(addRenderableWidget(Button.builder(label, b -> {
            r.run();
            ClientState.save();
        }).bounds(x, y, w, 18).build()));
    }

    private void select(Move m) {
        selected = m;
        replayAt = 0;
        if (m != null && minecraft != null && minecraft.player != null) {
            ClientAnims.play(minecraft.player, m, 1);
            replayAt = minecraft.player.tickCount + m.duration + 12;
        }
    }

    @Override
    public void tick() {
        if (selected != null && minecraft.player != null && minecraft.player.tickCount >= replayAt) select(selected);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = cardAt(mx, my);
        if (i >= 0 && i < list.size()) {
            select(list.get(i));
            rebuild();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private int cardAt(double mx, double my) {
        int cw = cardW();
        for (int i = 0; i < list.size(); i++) {
            int cx = 10 + (i % 2) * (cw + 6), cy = 48 + (i / 2) * 30;
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + 27) return i;
        }
        return -1;
    }

    private int cardW() { return Math.min(170, (width - 240) / 2); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fillGradient(0, 0, width, height, 0xC0081018, 0xE0101C2A);
        g.drawString(font, title, 10, 8, 0xFFFFD54F, true);
        Component tip = Component.translatable("screen.rabonaarena.moves_tip");
        g.drawString(font, tip, width - font.width(tip) - 10, 8, 0xFF90A4AE, false);
        int cw = cardW();
        for (int i = 0; i < list.size(); i++) {
            Move m = list.get(i);
            int cx = 10 + (i % 2) * (cw + 6), cy = 48 + (i / 2) * 30;
            boolean hov = mx >= cx && mx < cx + cw && my >= cy && my < cy + 27;
            boolean sel = m == selected;
            int col = m.cat.color | 0xFF000000;
            g.fill(cx, cy, cx + cw, cy + 27, sel ? 0xF0263850 : hov ? 0xE0202C3C : 0xD0161E28);
            g.fill(cx, cy, cx + 3, cy + 27, col);
            if (sel) g.renderOutline(cx, cy, cw, 27, col);
            g.drawString(font, m.title(), cx + 8, cy + 4, 0xFFFFFFFF, true);
            String cost = m.energyCost() > 0 ? Component.translatable("screen.rabonaarena.energy_cost", m.energyCost()).getString()
                    : Component.translatable("screen.rabonaarena.stamina_cost", m.stamina).getString();
            g.drawString(font, cost, cx + 8, cy + 15, 0xFF90A4AE, false);
            String tag = slotTag(m);
            if (!tag.isEmpty()) g.drawString(font, tag, cx + cw - font.width(tag) - 5, cy + 4, 0xFFFFEB3B, true);
        }
        // sag panel
        int px = width - 214, pw = 208;
        g.fill(px - 4, 46, width - 4, height - 6, 0xC0000000);
        if (selected != null && minecraft.player != null) {
            g.drawString(font, selected.title(), px + 4, 52, selected.cat.color | 0xFF000000, true);
            g.drawWordWrap(font, selected.desc(), px + 4, 66, pw - 8, 0xFFCFD8DC);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + pw / 2, height - 92, 42, px + pw / 2 - mx, height - 160 - my, minecraft.player);
            Component ctx = Component.translatable("req.rabonaarena." + selected.req.name().toLowerCase());
            g.drawString(font, ctx, px + 4, height - 88, 0xFF80CBC4, false);
        }
        super.render(g, mx, my, partial);
    }

    private static String slotTag(Move m) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 3; i++) if (ClientState.slots[i] == m) s.append('[').append(Keys.SKILLS[i].getTranslatedKeyMessage().getString()).append(']');
        for (int i = 0; i < 3; i++) if (ClientState.shotSlots[i] == m) s.append('[').append(Keys.SHOTS[i].getTranslatedKeyMessage().getString()).append(']');
        if (ClientState.ability == m) s.append("[").append(Keys.ABILITY.getTranslatedKeyMessage().getString()).append("]");
        if (ClientState.celebration == m) s.append("[").append(Keys.CELEBRATE.getTranslatedKeyMessage().getString()).append("]");
        return s.toString();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
