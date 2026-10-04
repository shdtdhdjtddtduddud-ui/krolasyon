package com.krolasyon.futbol.client;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.game.CardData;
import com.krolasyon.futbol.game.Role;
import com.krolasyon.futbol.network.ClubC2S;
import com.krolasyon.futbol.network.Net;
import net.minecraft.client.Minecraft;
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

/** Club: jeton balance, card packs, collection and squad. */
public class ClubScreen extends Screen {
    public static final int CW = 62, CH = 88;
    private static final int[] TOP = {0xFFC08A62, 0xFFE3E9EC, 0xFFFFE07A, 0xFFD7A8FF};
    private static final int[] BOTTOM = {0xFF6D4C33, 0xFF7B8D97, 0xFFC28E00, 0xFF3D0E7A};
    private static final int[] TEXT = {0xFF2A1708, 0xFF1E272C, 0xFF3A2600, 0xFFFFFFFF};
    private int scroll;
    private final List<int[]> hit = new ArrayList<>();

    public ClubScreen() { super(Component.literal("Kulüp")); }

    @Override
    protected void init() {
        Net.toServer(new ClubC2S(ClubC2S.SYNC, 0));
        int bx = width / 2 - 165;
        for (int i = 0; i < 3; i++) {
            final int k = i;
            addRenderableWidget(Button.builder(Component.literal(CardData.PACK_NAME[i] + " (" + CardData.PACK_PRICE[i] + ")"),
                    b -> Net.toServer(new ClubC2S(ClubC2S.OPEN, k))).bounds(bx + i * 112, 30, 106, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose()).bounds(width / 2 - 40, height - 22, 80, 18).build());
    }

    private List<CardData.Card> sorted() {
        List<CardData.Card> l = new ArrayList<>(ClientState.cards);
        l.sort(Comparator.comparing((CardData.Card c) -> !c.squad).thenComparing(c -> -c.overall()));
        return l;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, "⚽ KULÜBÜM", width / 2, 6, 0xFFD54F);
        long squad = ClientState.cards.stream().filter(c -> c.squad).count();
        String info = "Jeton: " + ClientState.coins + "    Kadro: " + squad + "/" + CardData.MAX_SQUAD + "    Kart: " + ClientState.cards.size();
        g.drawCenteredString(font, info, width / 2, 18, 0xFFFFFF);
        super.render(g, mx, my, pt);

        int top = 58, bottom = height - 30;
        int cols = Math.max(1, (width - 20) / (CW + 6));
        int gx = width / 2 - cols * (CW + 6) / 2;
        List<CardData.Card> list = sorted();
        int rows = (list.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows * (CH + 6) - (bottom - top));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        hit.clear();
        g.enableScissor(0, top, width, bottom);
        for (int i = 0; i < list.size(); i++) {
            int x = gx + (i % cols) * (CW + 6), y = top + (i / cols) * (CH + 6) - scroll;
            if (y + CH < top || y > bottom) continue;
            CardData.Card c = list.get(i);
            boolean hov = mx >= x && mx < x + CW && my >= y && my < y + CH && my >= top && my < bottom;
            drawCard(g, font, x, y, c, hov);
            hit.add(new int[]{x, y, c.id});
        }
        g.disableScissor();
        if (list.isEmpty()) g.drawCenteredString(font, "Henüz kartın yok — bir paket aç! Maç oynayarak jeton kazanırsın.", width / 2, top + 40, 0xB0B0B0);
        g.drawCenteredString(font, "Tıkla: kadroya al/çıkar   •   Shift + tık: sat   •   Kadrodaki kartlar takımındaki botların yerine oynar", width / 2, height - 34, 0x909090);
        reveal(g, pt);
    }

    private void reveal(GuiGraphics g, float pt) {
        if (ClientState.revealed < 0) return;
        float t = ClientState.clientTicks - ClientState.revealStart + pt;
        if (t > 90) {
            ClientState.revealed = -1;
            return;
        }
        CardData.Card c = null;
        for (CardData.Card k : ClientState.cards) if (k.id == ClientState.revealed) c = k;
        if (c == null) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0xC0000000);
        int col = BOTTOM[c.rarity] & 0xFFFFFF;
        float glow = Mth.clamp((t - 12) / 10F, 0F, 1F);
        for (int i = 0; i < 16; i++) {
            float a = (float) (i * Math.PI / 8 + t * 0.02);
            int len = (int) (120 * glow);
            int cx = width / 2, cy = height / 2;
            for (int s = 0; s < len; s += 3) {
                int x = cx + (int) (Math.cos(a) * s), y = cy + (int) (Math.sin(a) * s);
                g.fill(x, y, x + 2, y + 2, ((int) (0x70 * (1 - s / (float) Math.max(1, len))) << 24) | col);
            }
        }
        float flip = Mth.clamp(t / 14F, 0F, 1F);
        float sx = Math.abs(Mth.cos(flip * (float) Math.PI * 2F)) * 0.6F + 0.4F * flip;
        float sc = 1.9F * (0.5F + 0.5F * flip);
        g.pose().translate(width / 2F, height / 2F, 0);
        g.pose().scale(sc * sx, sc, 1F);
        g.pose().translate(-CW / 2F, -CH / 2F, 0);
        if (flip < 0.5F) {
            g.fillGradient(0, 0, CW, CH, 0xFF263238, 0xFF101418);
            g.drawCenteredString(font, "?", CW / 2, CH / 2 - 4, 0xFFFFFF);
        } else drawCard(g, font, 0, 0, c, false);
        g.pose().popPose();
        if (t > 20) g.drawCenteredString(font, CardData.RARITY[c.rarity].toUpperCase() + " KART!", width / 2, height / 2 + 100, col);
    }

    public static void drawCard(GuiGraphics g, Font font, int x, int y, CardData.Card c, boolean hover) {
        int r = Mth.clamp(c.rarity, 0, 3);
        g.fill(x - 1, y - 1, x + CW + 1, y + CH + 1, c.squad ? 0xFF4CAF50 : hover ? 0xFFFFFFFF : 0xFF000000);
        g.fillGradient(x, y, x + CW, y + CH, TOP[r], BOTTOM[r]);
        if (r == 3) {
            float t = (ClientState.clientTicks % 60) / 60F;
            int sx = x + (int) (t * (CW + 20)) - 10;
            g.fill(Math.max(x, sx), y, Math.min(x + CW, sx + 6), y + CH, 0x40FFFFFF);
        }
        int tc = TEXT[r];
        g.pose().pushPose();
        g.pose().translate(x + 4, y + 4, 0);
        g.pose().scale(1.8F, 1.8F, 1F);
        g.drawString(font, Integer.toString(c.overall()), 0, 0, tc, false);
        g.pose().popPose();
        g.drawString(font, Role.byId(c.role).abbr, x + 5, y + 21, tc, false);
        ResourceLocation skin = new ResourceLocation(FutbolMod.MODID, "textures/entity/footballer_" + Mth.clamp(c.skin, 0, 7) + ".png");
        g.blit(skin, x + CW - 30, y + 5, 25, 25, 8, 8, 8, 8, 64, 64);
        g.blit(skin, x + CW - 30, y + 5, 25, 25, 40, 8, 8, 8, 64, 64);
        String name = c.name.length() > 10 ? c.name.substring(0, 10) : c.name;
        g.drawString(font, name, x + CW / 2 - font.width(name) / 2, y + 34, tc, false);
        g.fill(x + 4, y + 44, x + CW - 4, y + 45, (tc & 0xFFFFFF) | 0x60000000);
        for (int i = 0; i < 6; i++) {
            int col = i / 3, row = i % 3;
            String s = c.stats[i] + " " + CardData.statName(i);
            g.drawString(font, s, x + 4 + col * 30, y + 48 + row * 10, tc, false);
        }
        if (c.squad) g.drawString(font, "KADRO", x + CW / 2 - font.width("KADRO") / 2, y + CH - 10, 0xFF1B5E20, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (ClientState.revealed >= 0) {
            ClientState.revealed = -1;
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        for (int[] h : hit) {
            if (mx >= h[0] && mx < h[0] + CW && my >= h[1] && my < h[1] + CH && my >= 58 && my < height - 30) {
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                Net.toServer(new ClubC2S(hasShiftDown() ? ClubC2S.SELL : ClubC2S.SQUAD, h[2]));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll -= (int) (delta * 24);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
