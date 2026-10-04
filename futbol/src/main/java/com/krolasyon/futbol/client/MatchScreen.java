package com.krolasyon.futbol.client;

import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.network.ControlC2S;
import com.krolasyon.futbol.network.MatchS2C;
import com.krolasyon.futbol.network.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Team selection and match control. */
public class MatchScreen extends Screen {
    private Button startStop;
    private int lastState = -1;
    private boolean lastPitch;

    public MatchScreen() { super(Component.literal("Maç")); }

    private static void send(int action, int arg) { Net.toServer(new ControlC2S(action, arg)); }

    @Override
    protected void init() {
        lastState = ClientState.state;
        lastPitch = ClientState.hasPitch;
        int cx = width / 2;
        int top = 46;
        int colW = Math.min(130, (width - 40) / 3);
        addRenderableWidget(Button.builder(Component.literal("§c§lKırmızıya Katıl"), b -> send(ControlC2S.TEAM, Team.RED.ordinal()))
                .bounds(cx - colW * 3 / 2 - 6, top, colW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("§7İzleyici"), b -> send(ControlC2S.TEAM, Team.NONE.ordinal()))
                .bounds(cx - colW / 2, top, colW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("§9§lMaviye Katıl"), b -> send(ControlC2S.TEAM, Team.BLUE.ordinal()))
                .bounds(cx + colW / 2 + 6, top, colW, 20).build());

        int sy = height - 76;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(ControlC2S.PLAYERS, ClientState.playersPerTeam - 1)).bounds(cx - 150, sy, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(ControlC2S.PLAYERS, ClientState.playersPerTeam + 1)).bounds(cx - 40, sy, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(ControlC2S.MINUTES, ClientState.minutes - 1)).bounds(cx + 20, sy, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(ControlC2S.MINUTES, ClientState.minutes + 1)).bounds(cx + 130, sy, 20, 20).build());

        int by = height - 50;
        if (!ClientState.hasPitch) {
            addRenderableWidget(Button.builder(Component.literal("§a⚽ Stadyumu Buraya Kur"), b -> send(ControlC2S.BUILD, 0)).bounds(cx - 100, by, 200, 20).build());
        } else {
            boolean running = ClientState.state != 0;
            startStop = addRenderableWidget(Button.builder(Component.literal(running ? "§cMaçı Bitir" : "§a§lMaçı Başlat"),
                    b -> send(running ? ControlC2S.STOP : ControlC2S.START, 0)).bounds(cx - 152, by, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Topu Ortala"), b -> send(ControlC2S.CENTER, 0)).bounds(cx - 48, by, 96, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Stadyumu Yeniden Kur"), b -> send(ControlC2S.BUILD, 0)).bounds(cx + 52, by, 100, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose()).bounds(cx - 40, height - 25, 80, 18).build());
    }

    @Override
    public void tick() {
        if (lastState != ClientState.state || lastPitch != ClientState.hasPitch) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int cx = width / 2;
        g.drawCenteredString(font, "⚽ KROLASYON FUTBOL", cx, 8, 0xFFD54F);
        String score = Team.RED.title + "  " + ClientState.red + " - " + ClientState.blue + "  " + Team.BLUE.title;
        g.drawCenteredString(font, ClientState.state == 0 ? "Maç oynanmıyor" : score, cx, 22, 0xFFFFFF);
        if (!ClientState.hasPitch) g.drawCenteredString(font, "Henüz saha yok — aşağıdaki butonla veya Saha Kurucu eşyasıyla kur.", cx, 33, 0xFF8080);

        int colW = Math.min(130, (width - 40) / 3);
        int[] xs = {cx - colW * 3 / 2 - 6, cx - colW / 2, cx + colW / 2 + 6};
        Team[] teams = {Team.RED, Team.NONE, Team.BLUE};
        int ly = 72;
        Team mine = ClientState.myTeam();
        for (int i = 0; i < 3; i++) {
            Team t = teams[i];
            int x = xs[i];
            int boxH = height - 160;
            g.fill(x, ly, x + colW, ly + boxH, 0x90000000);
            g.fill(x, ly, x + colW, ly + 2, 0xFF000000 | t.color);
            if (t == mine) g.fill(x, ly + boxH - 2, x + colW, ly + boxH, 0xFFFFFFFF);
            int y = ly + 6;
            g.drawString(font, t == Team.NONE ? "İzleyiciler" : t.title, x + 5, y, t.color, true);
            y += 12;
            for (MatchS2C.Entry e : ClientState.entries) {
                if (e.team() != t.ordinal()) continue;
                String s = (e.number() > 0 ? "#" + e.number() + " " : "") + e.name() + (e.online() ? "" : " (çevrimdışı)");
                g.drawString(font, s, x + 5, y, 0xFFFFFF, false);
                y += 10;
                if (y > ly + boxH - 10) break;
            }
            if (t != Team.NONE && ClientState.state != 0) {
                g.drawString(font, "+ botlar", x + 5, Math.min(y, ly + boxH - 10), 0x909090, false);
            }
        }
        int sy = height - 76;
        g.drawCenteredString(font, "Takım başı oyuncu: " + ClientState.playersPerTeam, cx - 85, sy + 6, 0xFFFFFF);
        g.drawCenteredString(font, "Süre: " + ClientState.minutes + " dk", cx + 85, sy + 6, 0xFFFFFF);
        g.drawCenteredString(font, "Eksik oyuncuları yapay zekalı botlar tamamlar", cx, sy - 11, 0x909090);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
