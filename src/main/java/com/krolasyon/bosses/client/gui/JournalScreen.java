package com.krolasyon.bosses.client.gui;

import com.krolasyon.bosses.client.ClientHooks;
import com.krolasyon.bosses.faction.Faction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The Chronicle of Azrakor: story pages, then one standing page per house. */
public class JournalScreen extends Screen {
    private static final int LORE_PAGES = 5;
    private int page = 0;
    private int bx, by;
    private static final int W = 256, H = 190;

    public JournalScreen() { super(Component.translatable("journal.title")); }

    private int pageCount() { return LORE_PAGES + Faction.HOUSES.length + 1; }

    @Override
    protected void init() {
        bx = (width - W) / 2;
        by = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (page > 0) page--; }).bounds(bx + 8, by + H - 24, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { if (page < pageCount() - 1) page++; }).bounds(bx + W - 32, by + H - 24, 24, 18).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(bx + W / 2 - 30, by + H - 24, 60, 18).build());
    }

    private static String oathKey(int o) {
        return o == 2 ? "journal.oath.conqueror" : o == 1 ? "journal.oath.ally" : "journal.oath.none";
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(bx - 3, by - 3, bx + W + 3, by + H + 3, 0xFF2A1810);
        g.fill(bx, by, bx + W, by + H, 0xFFD8C79E);
        g.fill(bx + W / 2, by + 4, bx + W / 2 + 1, by + H - 30, 0x30000000);
        int ink = 0xFF3A2414;
        List<Component> body = new ArrayList<>();
        Component head;
        int accent = ink;
        if (page < LORE_PAGES) {
            head = Component.translatable("journal.lore." + page + ".title");
            body.add(Component.translatable("journal.lore." + page));
        } else if (page < LORE_PAGES + Faction.HOUSES.length) {
            Faction f = Faction.HOUSES[page - LORE_PAGES];
            accent = 0xFF000000 | f.color;
            head = Component.translatable("faction.krolasyonbosses." + f.id);
            int rep = ClientHooks.rep(f.id);
            body.add(Component.translatable("journal.rep", rep));
            body.add(Component.translatable(oathKey(ClientHooks.data.getInt("oath_" + f.id))));
            body.add(Component.translatable("journal.quest." + ClientHooks.data.getInt("stage_" + f.id)));
            body.add(Component.translatable("journal.house." + f.id));
        } else {
            head = Component.translatable("journal.throne.title");
            body.add(Component.translatable(ClientHooks.data.getBoolean("f_sovereign") ? "journal.throne.done" : "journal.throne.todo"));
        }
        g.drawString(font, head, bx + 14, by + 12, accent == ink ? ink : (accent & 0xFF7F7F7F), false);
        g.fill(bx + 12, by + 24, bx + W - 12, by + 25, accent);
        int y = by + 32;
        for (Component c : body) {
            for (FormattedCharSequence l : font.split(c, W - 28)) {
                g.drawString(font, l, bx + 14, y, ink, false);
                y += font.lineHeight + 1;
            }
            y += 5;
        }
        g.drawCenteredString(font, (page + 1) + "/" + pageCount(), bx + W / 2, by + H - 40, 0xFF6A5030);
        super.render(g, mx, my, pt);
    }
}
