package com.rabona.arena.client;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.game.Cards;
import com.rabona.arena.game.Pos;
import com.rabona.arena.net.C2S;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Oyuncu kartlari: paket magazasi, koleksiyon/kadro ve paket acilis animasyonu. */
public class CardScreen extends Screen {
    static int tab;
    private int scroll;
    private final List<int[]> hits = new ArrayList<>(); // x, y, w, h, kart indeksi

    public CardScreen() { super(Component.translatable("screen.rabonaarena.cards")); }

    @Override
    protected void init() {
        Net.toServer(new C2S.Menu(C2S.Menu.CARD_SYNC, 0));
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.shop"), b -> tab = 0).bounds(10, 26, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.collection"), b -> tab = 1).bounds(104, 26, 90, 18).build());
        int pw = 96, gap = 12;
        int total = 4 * pw + 3 * gap;
        int x0 = width / 2 - total / 2;
        for (int i = 0; i < 4; i++) {
            final int pack = i;
            Button b = Button.builder(Component.translatable("screen.rabonaarena.open_pack", Cards.Pack.byId(i).price),
                    bt -> Net.toServer(new C2S.Menu(C2S.Menu.CARD_OPEN, pack))).bounds(x0 + i * (pw + gap), 196, pw, 20).build();
            addRenderableWidget(b);
            shopButtons.add(b);
        }
    }

    private final List<Button> shopButtons = new ArrayList<>();

    @Override
    public void tick() {
        for (Button b : shopButtons) b.visible = tab == 0 && !revealing();
    }

    private boolean revealing() {
        return !ClientState.packReveal.isEmpty() && ClientState.clientTicks - ClientState.packRevealStart < 40 + ClientState.packReveal.size() * 28 + 60;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fillGradient(0, 0, width, height, 0xF0081428, 0xF0140A28);
        g.drawString(font, title, 10, 9, 0xFFFFD54F, true);
        S2C.Profile pr = ClientState.profile;
        String coins = "⛁ " + (pr == null ? 0 : pr.coins()) + " " + Component.translatable("screen.rabonaarena.coins").getString();
        g.drawString(font, coins, width - font.width(coins) - 12, 9, 0xFFFFD54F, true);
        hits.clear();
        if (tab == 0) shop(g, mx, my); else collection(g, mx, my, pr);
        super.render(g, mx, my, partial);
        if (revealing()) reveal(g, partial);
    }

    // ================================================================ magaza
    private void shop(GuiGraphics g, int mx, int my) {
        int pw = 96, gap = 12;
        int total = 4 * pw + 3 * gap;
        int x0 = width / 2 - total / 2;
        int[][] cols = {{0xFF6D4C2F, 0xFFC98B4B}, {0xFF6F7B86, 0xFFE3E8EC}, {0xFF9C7206, 0xFFFFE27A}, {0xFF4A148C, 0xFF00E5FF}};
        String[] keys = {"bronze", "silver", "gold", "legend"};
        for (int i = 0; i < 4; i++) {
            int x = x0 + i * (pw + gap), y = 56;
            int c1 = cols[i][0], c2 = cols[i][1];
            if (i == 3) c2 = Mth.hsvToRgb((ClientState.clientTicks % 120) / 120f, 0.6f, 1f) | 0xFF000000;
            g.fill(x - 2, y - 2, x + pw + 2, y + 134, 0xFF000000);
            g.fillGradient(x, y, x + pw, y + 132, c2, c1);
            // parlama seridi
            int sh = (ClientState.clientTicks * 3 + i * 40) % (pw + 60) - 30;
            for (int k = 0; k < 10; k++) {
                int sx = x + sh + k;
                if (sx > x && sx < x + pw) g.fill(sx, y, sx + 1, y + 132, 0x30FFFFFF);
            }
            g.renderOutline(x + 4, y + 4, pw - 8, 124, 0x80FFFFFF);
            Component name = Component.translatable("pack.rabonaarena." + keys[i]);
            g.pose().pushPose();
            g.pose().translate(x + pw / 2f, y + 22, 0);
            g.pose().scale(1.4f, 1.4f, 1);
            g.drawString(font, name, -font.width(name) / 2, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
            Cards.Pack p = Cards.Pack.byId(i);
            String range = p.min + "-" + (p.legendChance > 0 ? 94 : p.max);
            g.drawCenteredString(font, Component.translatable("screen.rabonaarena.pack_ovr", range), x + pw / 2, y + 52, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("screen.rabonaarena.pack_count", p.count), x + pw / 2, y + 66, 0xFFE0E0E0);
            if (p.legendChance > 0)
                g.drawCenteredString(font, Component.translatable("screen.rabonaarena.pack_legend", (int) (p.legendChance * 100)), x + pw / 2, y + 80, 0xFFE1BEE7);
            String price = "⛁ " + p.price;
            g.pose().pushPose();
            g.pose().translate(x + pw / 2f, y + 104, 0);
            g.pose().scale(1.6f, 1.6f, 1);
            g.drawString(font, price, -font.width(price) / 2, 0, 0xFFFFD54F, true);
            g.pose().popPose();
        }
        g.drawCenteredString(font, Component.translatable("screen.rabonaarena.earn_tip"), width / 2, 226, 0xFF90A4AE);
    }

    // ================================================================ koleksiyon
    private void collection(GuiGraphics g, int mx, int my, S2C.Profile pr) {
        if (pr == null || pr.cards().isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.rabonaarena.no_cards"), width / 2, height / 2, 0xFFB0BEC5);
            return;
        }
        g.drawString(font, Component.translatable("screen.rabonaarena.squad_tip", pr.squad().size(), Cards.SQUAD_MAX), 10, 48, 0xFF90A4AE, false);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < pr.cards().size(); i++) order.add(i);
        order.sort(Comparator.comparingInt((Integer i) -> pr.squad().contains(i) ? 0 : 1).thenComparingInt(i -> -pr.cards().get(i).ovr()));
        float sc = 0.62f;
        int cw = (int) (CW * sc) + 6, ch = (int) (CH * sc) + 6;
        int cols = Math.max(1, (width - 20) / cw);
        int y0 = 62 - scroll;
        for (int k = 0; k < order.size(); k++) {
            int idx = order.get(k);
            int x = 10 + (k % cols) * cw, y = y0 + (k / cols) * ch;
            if (y + ch < 58 || y > height) continue;
            drawCard(g, font, pr.cards().get(idx), x, y, sc, pr.squad().contains(idx));
            hits.add(new int[]{x, y, cw - 6, ch - 6, idx});
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (revealing()) {
            ClientState.packReveal = List.of();
            return true;
        }
        if (tab == 1) {
            for (int[] h : hits) {
                if (mx >= h[0] && mx < h[0] + h[2] && my >= h[1] && my < h[1] + h[3]) {
                    if (button == 1 && hasShiftDown()) Net.toServer(new C2S.Menu(C2S.Menu.CARD_SELL, h[4]));
                    else if (button == 0) Net.toServer(new C2S.Menu(C2S.Menu.CARD_SQUAD, h[4]));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, scroll - (int) (delta * 24));
        return true;
    }

    // ================================================================ paket acilisi
    private void reveal(GuiGraphics g, float partial) {
        g.fill(0, 0, width, height, 0xE0000000);
        List<Cards.Card> cards = ClientState.packReveal;
        float t = ClientState.clientTicks - ClientState.packRevealStart + partial;
        float sc = 0.95f;
        int cw = (int) (CW * sc), gap = 16;
        int total = cards.size() * cw + (cards.size() - 1) * gap;
        int x0 = width / 2 - total / 2;
        int y = height / 2 - (int) (CH * sc) / 2;
        for (int i = 0; i < cards.size(); i++) {
            Cards.Card c = cards.get(i);
            float lt = t - 30 - i * 28;
            int x = x0 + i * (cw + gap);
            if (lt < 0) {
                cardBack(g, x, y, sc, t);
                continue;
            }
            float flip = Math.min(1, lt / 10f);
            float sx = Math.abs(Mth.cos(flip * Mth.PI));
            boolean front = flip > 0.5f;
            g.pose().pushPose();
            g.pose().translate(x + cw / 2f, 0, 0);
            g.pose().scale(Math.max(0.02f, sx), 1, 1);
            g.pose().translate(-(x + cw / 2f), 0, 0);
            if (front) {
                if (c.rarity() >= 2 && lt < 40) rays(g, x + cw / 2, y + (int) (CH * sc) / 2, lt, c.rarity());
                drawCard(g, font, c, x, y, sc, false);
            } else {
                cardBack(g, x, y, sc, t);
            }
            g.pose().popPose();
        }
        g.drawCenteredString(font, Component.translatable("screen.rabonaarena.click_continue"), width / 2, height - 24, 0xFFB0BEC5);
    }

    private void rays(GuiGraphics g, int cx, int cy, float lt, int rarity) {
        int col = rarity == 3 ? 0x60E040FB : 0x60FFD54F;
        int r = (int) (40 + lt * 4);
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6 + lt * 0.05;
            for (int d = 20; d < r; d += 3) {
                int px = cx + (int) (Math.cos(a) * d), py = cy + (int) (Math.sin(a) * d);
                g.fill(px, py, px + 2, py + 2, col);
            }
        }
    }

    private void cardBack(GuiGraphics g, int x, int y, float sc, float t) {
        int w = (int) (CW * sc), h = (int) (CH * sc);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fillGradient(x, y, x + w, y + h, 0xFF1A237E, 0xFF0D1440);
        g.renderOutline(x + 4, y + 4, w - 8, h - 8, 0x80FFD54F);
        Component r = Component.literal("R").withStyle(net.minecraft.ChatFormatting.BOLD);
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + h / 2f - 8, 0);
        float pulse = 2.5f + 0.2f * Mth.sin(t * 0.3f);
        g.pose().scale(pulse, pulse, 1);
        g.drawString(font, r, -font.width(r) / 2, -4, 0xFFFFD54F, true);
        g.pose().popPose();
    }

    // ================================================================ kart cizimi
    static final int CW = 110, CH = 156;
    private static final String[] STAT_KEYS = {"pac", "sho", "pas", "dri", "def", "phy"};

    public static void drawCard(GuiGraphics g, Font f, Cards.Card c, int x, int y, float sc, boolean inSquad) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(sc, sc, 1);
        int[] top = {0xFFC98B4B, 0xFFE3E8EC, 0xFFFFE27A, 0};
        int[] bot = {0xFF6D4C2F, 0xFF6F7B86, 0xFF9C7206, 0xFF311B92};
        int t1 = c.rarity() == 3 ? Mth.hsvToRgb((ClientState.clientTicks % 90) / 90f, 0.55f, 1f) | 0xFF000000 : top[Mth.clamp(c.rarity(), 0, 2)];
        int t2 = bot[Mth.clamp(c.rarity(), 0, 3)];
        int textC = c.rarity() == 1 ? 0xFF263238 : c.rarity() == 2 ? 0xFF3E2723 : 0xFFFFFFFF;
        g.fill(-2, -2, CW + 2, CH + 2, inSquad ? 0xFF00E676 : 0xFF000000);
        g.fillGradient(0, 0, CW, CH, t1, t2);
        g.renderOutline(4, 4, CW - 8, CH - 8, 0x70FFFFFF);
        // puan + mevki
        g.pose().pushPose();
        g.pose().translate(10, 10, 0);
        g.pose().scale(2.2f, 2.2f, 1);
        g.drawString(f, Integer.toString(c.ovr()), 0, 0, textC, false);
        g.pose().popPose();
        g.drawString(f, Pos.byId(c.pos()).shortName(), 12, 32, textC, false);
        // yuz (bot teninden)
        ResourceLocation skin = RabonaArena.id("textures/entity/footballer/skin_" + Math.floorMod(c.skin(), 8) + ".png");
        g.blit(skin, 46, 10, 48, 48, 8, 8, 8, 8, 64, 64);
        g.blit(skin, 46, 10, 48, 48, 40, 8, 8, 8, 64, 64);
        // isim
        g.fill(6, 64, CW - 6, 65, 0x60000000);
        String name = c.name();
        float ns = f.width(name) > CW - 12 ? (CW - 12f) / f.width(name) : 1f;
        g.pose().pushPose();
        g.pose().translate(CW / 2f, 70, 0);
        g.pose().scale(ns, ns, 1);
        g.drawString(f, name, -f.width(name) / 2, 0, textC, false);
        g.pose().popPose();
        g.fill(6, 82, CW - 6, 83, 0x60000000);
        int[] st = {c.pac(), c.sho(), c.pas(), c.dri(), c.def(), c.phy()};
        for (int i = 0; i < 6; i++) {
            int col = i < 3 ? 0 : 1, row = i % 3;
            int sx = 12 + col * 50, sy = 88 + row * 14;
            g.drawString(f, Integer.toString(st[i]), sx, sy, textC, false);
            g.drawString(f, Component.translatable("stat.rabonaarena." + STAT_KEYS[i]), sx + 16, sy, textC & 0xD0FFFFFF, false);
        }
        String rar = Component.translatable("rarity.rabonaarena." + c.rarity()).getString();
        g.drawString(f, rar, CW / 2 - f.width(rar) / 2, CH - 14, textC, false);
        if (inSquad) g.drawString(f, "✔", CW - 14, 8, 0xFF00E676, true);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
