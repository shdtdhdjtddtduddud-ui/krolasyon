package com.rabona.arena.client;

import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.game.Pos;
import com.rabona.arena.game.Team;
import com.rabona.arena.net.C2S;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Mac menusu: takim secimi, botlar, ayarlar ve stadyum. */
public class MatchScreen extends Screen {
    private Button diffBtn, posBtn, replayBtn;
    private int rosterBottom;

    private Pos myPos() {
        if (m() != null && minecraft.player != null)
            for (S2C.RosterEntry e : m().roster()) if (e.id().equals(minecraft.player.getUUID())) return Pos.byId(e.pos());
        return Pos.ST;
    }

    public MatchScreen() { super(Component.translatable("screen.rabonaarena.match")); }

    private static void send(int action, int value) { Net.toServer(new C2S.Menu(action, value)); }

    private S2C.MatchState m() { return ClientState.match; }

    @Override
    protected void init() {
        int cx = width / 2;
        int lx = cx - 210, y = 40;
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.join_red"), b -> send(C2S.Menu.JOIN, Team.RED.ordinal()))
                .bounds(lx, y, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.join_blue"), b -> send(C2S.Menu.JOIN, Team.BLUE.ordinal()))
                .bounds(lx + 138, y, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.spectate"), b -> send(C2S.Menu.JOIN, Team.NONE.ordinal()))
                .bounds(lx, y + 24, 130, 18).build());
        posBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            Pos[] all = Pos.values();
            Pos next = all[(myPos().ordinal() + (hasShiftDown() ? all.length - 1 : 1)) % all.length];
            send(C2S.Menu.POS, next.ordinal());
        }).bounds(lx + 138, y + 24, 130, 18).build());
        replayBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            ClientState.autoReplay = !ClientState.autoReplay;
            ClientState.save();
        }).bounds(lx + 138, height - 30, 130, 20).build());

        int rx = cx + 70, ry = 40;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(C2S.Menu.DURATION, val(S2C.MatchState::duration, 6) - 1)).bounds(rx, ry, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(C2S.Menu.DURATION, val(S2C.MatchState::duration, 6) + 1)).bounds(rx + 118, ry, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(C2S.Menu.TEAM_SIZE, val(S2C.MatchState::teamSize, 5) - 1)).bounds(rx, ry + 22, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(C2S.Menu.TEAM_SIZE, val(S2C.MatchState::teamSize, 5) + 1)).bounds(rx + 118, ry + 22, 20, 18).build());
        diffBtn = addRenderableWidget(Button.builder(Component.empty(), b -> send(C2S.Menu.DIFFICULTY, (val(S2C.MatchState::difficulty, 1) + 1) % 3))
                .bounds(rx, ry + 44, 138, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.fill_bots"), b -> send(C2S.Menu.FILL_BOTS, 0)).bounds(rx, ry + 70, 138, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.clear_bots"), b -> send(C2S.Menu.CLEAR_BOTS, 0)).bounds(rx, ry + 92, 138, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.start"), b -> {
            send(C2S.Menu.START, 0);
            onClose();
        }).bounds(rx, ry + 116, 68, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.stop"), b -> send(C2S.Menu.STOP, 0)).bounds(rx + 70, ry + 116, 68, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.build_x"), b -> {
            send(C2S.Menu.BUILD, 1);
            onClose();
        }).bounds(rx, ry + 142, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.build_z"), b -> {
            send(C2S.Menu.BUILD, 0);
            onClose();
        }).bounds(rx + 70, ry + 142, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.give_ball"), b -> send(C2S.Menu.BALL, 0)).bounds(rx, ry + 164, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.teleport"), b -> {
            send(C2S.Menu.TP, 0);
            onClose();
        }).bounds(rx + 70, ry + 164, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.rabonaarena.moves"), b -> minecraft.setScreen(new MoveScreen()))
                .bounds(lx, height - 30, 130, 20).build());
    }

    private int val(java.util.function.ToIntFunction<S2C.MatchState> f, int def) {
        return m() == null ? def : f.applyAsInt(m());
    }

    @Override
    public void tick() {
        int d = val(S2C.MatchState::difficulty, 1);
        posBtn.setMessage(Component.translatable("screen.rabonaarena.my_pos", myPos().title()));
        replayBtn.setMessage(Component.translatable(ClientState.autoReplay ? "screen.rabonaarena.auto_replay_on" : "screen.rabonaarena.auto_replay_off"));
        diffBtn.setMessage(Component.translatable("screen.rabonaarena.difficulty", Component.translatable("difficulty.rabonaarena." + d)));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fillGradient(0, 0, width, height, 0xC0061208, 0xE0102418);
        int cx = width / 2;
        g.drawCenteredString(font, title, cx, 12, 0xFFFFD54F);
        int lx = cx - 210;
        // kadrolar
        rosterBottom = 0;
        drawRoster(g, lx, 92, Team.RED);
        drawRoster(g, lx + 138, 92, Team.BLUE);
        // ayarlar
        int rx = cx + 70, ry = 40;
        g.drawCenteredString(font, Component.translatable("screen.rabonaarena.duration", val(S2C.MatchState::duration, 6)), rx + 69, ry + 5, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.rabonaarena.team_size", val(S2C.MatchState::teamSize, 5)), rx + 69, ry + 27, 0xFFFFFFFF);
        g.fill(rx - 6, ry - 6, rx + 144, ry + 188, 0x50000000);
        g.drawString(font, Component.translatable("screen.rabonaarena.settings"), rx, ry - 18, 0xFFA5D6A7, true);
        boolean pitch = ClientState.pitch != null;
        g.drawString(font, Component.translatable(pitch ? "screen.rabonaarena.pitch_ok" : "screen.rabonaarena.pitch_missing"), rx, ry + 190, pitch ? 0xFF81C784 : 0xFFFF8A65, false);
        // kontroller
        int hy = height - 92;
        if (rosterBottom > hy - 6) {
            super.render(g, mx, my, partial);
            return;
        }
        g.fill(lx - 4, hy - 4, lx + 272, hy + 56, 0x60000000);
        g.drawString(font, Component.translatable("screen.rabonaarena.controls"), lx, hy, 0xFFFFD54F, true);
        g.drawString(font, Component.translatable("screen.rabonaarena.controls1", Keys.SHOOT.getTranslatedKeyMessage(), Keys.PASS.getTranslatedKeyMessage(), Keys.LOB.getTranslatedKeyMessage()), lx, hy + 12, 0xFFE0E0E0, false);
        g.drawString(font, Component.translatable("screen.rabonaarena.controls2", Keys.TACKLE.getTranslatedKeyMessage(), Keys.SKILL1.getTranslatedKeyMessage(), Keys.ABILITY.getTranslatedKeyMessage()), lx, hy + 23, 0xFFE0E0E0, false);
        g.drawString(font, Component.translatable("screen.rabonaarena.controls3", Keys.CALL.getTranslatedKeyMessage(), Keys.CAMERA.getTranslatedKeyMessage(), Keys.REPLAY.getTranslatedKeyMessage(), Keys.CARDS.getTranslatedKeyMessage()), lx, hy + 34, 0xFFE0E0E0, false);
        super.render(g, mx, my, partial);
    }

    private void drawRoster(GuiGraphics g, int x, int y, Team t) {
        int col = t == Team.RED ? 0xFFEF5350 : 0xFF42A5F5;
        g.fill(x, y, x + 130, y + 14, col & 0xC0FFFFFF);
        g.drawString(font, t.displayName(), x + 4, y + 3, 0xFFFFFFFF, true);
        List<String> names = new ArrayList<>();
        if (m() != null) for (S2C.RosterEntry e : m().roster()) if (e.team() == t.ordinal()) names.add(e.number() + "  " + e.name() + " §e" + Pos.byId(e.pos()).shortName().getString());
        int bots = 0;
        if (minecraft.level != null) {
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (e instanceof FootballerEntity f && f.getSquad() == t) {
                    bots++;
                    if (names.size() < 11) names.add(f.getNumber() + "  " + f.getBaseName() + " §8" + f.getFieldPos().shortName().getString());
                }
            }
        }
        int yy = y + 17;
        rosterBottom = Math.max(rosterBottom, y + 18 + Math.max(1, names.size()) * 10);
        g.fill(x, y + 14, x + 130, y + 18 + Math.max(1, names.size()) * 10, 0x60000000);
        for (String n : names) {
            g.drawString(font, n, x + 4, yy, 0xFFFFFFFF, false);
            yy += 10;
        }
        if (names.isEmpty()) g.drawString(font, Component.translatable("screen.rabonaarena.empty"), x + 4, yy, 0xFF9E9E9E, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
