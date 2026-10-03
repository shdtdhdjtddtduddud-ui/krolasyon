package com.krolasyon.bosses.realm.client;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.net.RealmNet;
import com.krolasyon.bosses.realm.registry.RealmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RealmScreens {
    private RealmScreens() {}

    static final ResourceLocation BG = Realm.rl("textures/gui/chronicle.png");
    static final int BG_W = 320, BG_H = 216;

    public static void openJournal() { Minecraft.getInstance().setScreen(new Journal()); }

    public static void openEnvoy(int entityId, int faction, Component reply) {
        Minecraft mc = Minecraft.getInstance();
        Faction f = Faction.byId(faction);
        if (f == null || mc.level == null) return;
        if (mc.screen instanceof Envoy env && env.entityId == entityId) env.reply = reply;
        else mc.setScreen(new Envoy(entityId, f, reply));
    }

    static void panel(GuiGraphics g, int x, int y) {
        g.blit(BG, x, y, 0, 0, BG_W, BG_H, BG_W, BG_H);
    }

    static int text(GuiGraphics g, Font font, Component c, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(c, width)) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    static void repBar(GuiGraphics g, int x, int y, int w, int rep, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + 4, 0xFF140406);
        g.fill(x, y, x + w, y + 3, 0xFF3A2020);
        int mid = x + w / 2;
        int end = mid + (int) (w / 2F * rep / 100F);
        g.fill(Math.min(mid, end), y, Math.max(mid, end), y + 3, 0xFF000000 | color);
        g.fill(mid, y - 1, mid + 1, y + 4, 0xFFFFE0A0);
    }

    // ------------------------------------------------------------------ chronicle
    public static class Journal extends Screen {
        private int page;

        public int page() { return page; }

        public void setPage(int p) { page = p; }
        private int left, top;

        Journal() { super(Component.translatable("gui.krolasyonbosses.journal")); }

        @Override
        protected void init() {
            left = (width - BG_W) / 2;
            top = (height - BG_H) / 2;
            addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonbosses.page_prev"), b -> page = (page + 2) % 3)
                    .bounds(left + 16, top + BG_H - 26, 70, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonbosses.page_next"), b -> page = (page + 1) % 3)
                    .bounds(left + BG_W - 86, top + BG_H - 26, 70, 18).build());
        }

        @Override
        public boolean isPauseScreen() { return false; }

        @Override
        public void render(GuiGraphics g, int mx, int my, float partial) {
            renderBackground(g);
            panel(g, left, top);
            RealmData d = ClientRealm.data;
            Component head = switch (page) {
                case 0 -> Component.translatable("gui.krolasyonbosses.story");
                case 1 -> Component.translatable("gui.krolasyonbosses.kingdoms");
                default -> Component.translatable("gui.krolasyonbosses.blessings");
            };
            g.drawCenteredString(font, Component.translatable("gui.krolasyonbosses.journal").withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFE8B060);
            g.drawCenteredString(font, head, width / 2, top + 22, 0xFFB08060);
            int x = left + 20, y = top + 38, w = BG_W - 40;
            if (page == 0) storyPage(g, d, x, y, w);
            else if (page == 1) kingdomPage(g, d, x, y, w);
            else powerPage(g, d, x, y, w);
            g.drawCenteredString(font, Component.literal((page + 1) + " / 3"), width / 2, top + BG_H - 21, 0xFF9A7050);
            super.render(g, mx, my, partial);
        }

        private void storyPage(GuiGraphics g, RealmData d, int x, int y, int w) {
            String k = "chapter.krolasyonbosses." + d.chapter;
            g.drawString(font, Component.literal("§l" + (d.chapter + 1) + ". ").append(Component.translatable(k)).withStyle(ChatFormatting.RED), x, y, 0xFFFF6050, false);
            y = text(g, font, Component.translatable(k + ".story"), x, y + 13, w, 0xFFD8C8B0);
            y += 4;
            g.drawString(font, Component.translatable("gui.krolasyonbosses.goal").withStyle(ChatFormatting.BOLD), x, y, 0xFFFFC040, false);
            y = text(g, font, Component.translatable(k + ".goal"), x, y + 11, w, 0xFFFFE0A0);
            if (d.ruler) {
                g.drawCenteredString(font, Component.translatable("gui.krolasyonbosses.ruler").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x + w / 2, y + 4, 0xFFFFD040);
                return;
            }
            int sy = top + BG_H - 52;
            g.drawString(font, Component.translatable("gui.krolasyonbosses.sigils", d.sigilsEarned()), x, sy + 4, 0xFFE0C090, false);
            int ix = x + 90;
            for (Faction f : Faction.ALL) {
                ItemStack s = new ItemStack(RealmItems.sigil(f));
                g.renderItem(s, ix, sy);
                if (!(d.allied[f.ordinal()] || d.conquered[f.ordinal()])) g.fill(ix, sy, ix + 16, sy + 16, 0xB0100808);
                ix += 22;
            }
        }

        private void kingdomPage(GuiGraphics g, RealmData d, int x, int y, int w) {
            for (Faction f : Faction.ALL) {
                RealmData.Standing st = d.standing(f);
                g.drawString(font, f.title().copy().withStyle(ChatFormatting.BOLD), x, y, 0xFF000000 | f.color, false);
                Component s = Component.translatable("standing.krolasyonbosses." + st.id);
                g.drawString(font, s, x + w - font.width(s), y, 0xFF000000 | st.color, false);
                repBar(g, x, y + 11, 110, d.rep(f), st.color);
                g.drawString(font, f.lord(), x + 118, y + 9, 0xFFB09880, false);
                g.drawString(font, relations(f), x, y + 18, 0xFF8A7868, false);
                y += 31;
            }
        }

        private Component relations(Faction f) {
            List<String> friends = new ArrayList<>(), foes = new ArrayList<>();
            MutableComponent fr = Component.empty(), fo = Component.empty();
            for (Faction g : Faction.ALL) {
                if (f.alliedWith(g)) fr.append(fr.getSiblings().isEmpty() ? Component.empty() : Component.literal(", ")).append(Component.translatable("faction.krolasyonbosses." + g.id));
                if (f.atWarWith(g)) fo.append(fo.getSiblings().isEmpty() ? Component.empty() : Component.literal(", ")).append(Component.translatable("faction.krolasyonbosses." + g.id));
            }
            Component none = Component.translatable("gui.krolasyonbosses.none");
            return Component.literal("§a+ ").append(fr.getSiblings().isEmpty() ? none : fr).append(Component.literal("   §c- ")).append(fo.getSiblings().isEmpty() ? none : fo);
        }

        private void powerPage(GuiGraphics g, RealmData d, int x, int y, int w) {
            for (Faction f : Faction.ALL) {
                boolean on = d.bound(f);
                Component c = Component.translatable("blessing.krolasyonbosses." + f.id);
                g.drawString(font, Component.literal(on ? "✦ " : "· ").append(c), x, y, on ? 0xFF000000 | f.color : 0xFF5A4A40, false);
                y += 13;
            }
            y += 6;
            g.drawString(font, Component.translatable("gui.krolasyonbosses.mana").append(": " + (int) d.mana + " / " + (int) RealmData.MAX_MANA), x, y, 0xFF9AB0FF, false);
            y += 13;
            g.drawString(font, Component.translatable("key.krolasyonbosses.dash").append(" [").append(ClientRealm.DASH.getTranslatedKeyMessage()).append("]"),
                    x, y, d.chapter >= RealmData.CH_FIREBORN ? 0xFFFF8060 : 0xFF5A4A40, false);
            y += 15;
            Faction tgt = d.questTargetFaction();
            Faction giver = Faction.byId(d.questGiver);
            if (tgt != null && giver != null) {
                text(g, font, Component.translatable("gui.krolasyonbosses.quest_active", tgt.title(), d.questProgress, RealmData.QUEST_NEED)
                        .append(Component.literal(" — ")).append(giver.title()), x, y, w, 0xFFE0C090);
            }
        }
    }

    // ------------------------------------------------------------------ envoy dialogue
    static class Envoy extends Screen {
        final int entityId;
        final Faction faction;
        Component reply;
        private int left, top;

        Envoy(int entityId, Faction f, Component reply) {
            super(Component.translatable("gui.krolasyonbosses.envoy_title", f.title()));
            this.entityId = entityId;
            this.faction = f;
            this.reply = reply;
        }

        @Override
        protected void init() {
            left = (width - BG_W) / 2;
            top = (height - BG_H) / 2;
            int bx = left + 116, by = top + BG_H - 74, bw = 90;
            addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonbosses.tribute"), b -> act(RealmNet.EnvoyAction.TRIBUTE))
                    .bounds(bx, by, bw, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.krolasyonbosses.tribute_hint"))).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonbosses.quest"), b -> act(RealmNet.EnvoyAction.QUEST))
                    .bounds(bx + bw + 6, by, bw, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.krolasyonbosses.quest_none"))).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonbosses.alliance"), b -> act(RealmNet.EnvoyAction.ALLIANCE))
                    .bounds(bx, by + 22, bw, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.krolasyonbosses.alliance_hint"))).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.krolasyonbosses.leave"), b -> onClose())
                    .bounds(bx + bw + 6, by + 22, bw, 18).build());
        }

        private void act(int action) {
            RealmNet.CHANNEL.sendToServer(new RealmNet.EnvoyAction(entityId, action));
            Minecraft.getInstance().player.playSound(SoundEvents.BOOK_PAGE_TURN, 1F, 1F);
        }

        @Override
        public boolean isPauseScreen() { return false; }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(entityId);
            if (e == null || !e.isAlive() || mc.player == null || e.distanceToSqr(mc.player) > 144) onClose();
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float partial) {
            renderBackground(g);
            panel(g, left, top);
            RealmData d = ClientRealm.data;
            g.drawCenteredString(font, getTitle().copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFF000000 | faction.color);
            g.drawCenteredString(font, faction.lord(), width / 2, top + 22, 0xFFB09880);
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(entityId);
            if (e instanceof LivingEntity le) {
                g.fill(left + 18, top + 38, left + 104, top + BG_H - 30, 0x60000000);
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 61, top + BG_H - 40, 38, left + 61 - mx, top + 80 - my, le);
            }
            int x = left + 116, w = BG_W - 136;
            int y = text(g, font, Component.literal("“").append(reply).append("”"), x, top + 40, w, 0xFFE8D8C0);
            RealmData.Standing st = d.standing(faction);
            int ry = top + BG_H - 98;
            g.drawString(font, Component.translatable("gui.krolasyonbosses.reputation", d.rep(faction),
                    Component.translatable("standing.krolasyonbosses." + st.id)), x, ry, 0xFF000000 | st.color, false);
            repBar(g, x, ry + 11, w, d.rep(faction), st.color);
            super.render(g, mx, my, partial);
        }
    }
}
