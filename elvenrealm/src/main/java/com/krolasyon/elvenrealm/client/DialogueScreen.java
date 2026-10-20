package com.krolasyon.elvenrealm.client;

import com.krolasyon.elvenrealm.network.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Elf diyalog ekranı: canlı portre, daktilo gibi akan metin (karaktere özgü ses tonuyla) ve cevap seçenekleri. */
public class DialogueScreen extends Screen {
    private static final int GOLD = 0xFFD9B45A, PANEL = 0xE0101812, PANEL2 = 0xE01A2A1C;

    private Net.OpenDialogue msg;
    private Component text = Component.empty();
    private List<FormattedCharSequence> lines = new ArrayList<>();
    private int totalChars;
    private float shown;
    private final List<Button> buttons = new ArrayList<>();
    private int panelX, panelY, panelW, panelH, textX, textW;
    private int lastBlip;

    public DialogueScreen(Net.OpenDialogue msg) {
        super(Component.literal(msg.name()));
        this.msg = msg;
    }

    public int entityId() {
        return msg.entityId();
    }

    public void update(Net.OpenDialogue m) {
        this.msg = m;
        shown = 0;
        lastBlip = 0;
        rebuild();
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        for (Button b : buttons) removeWidget(b);
        buttons.clear();
        Component parsed = Component.Serializer.fromJson(msg.text());
        text = parsed == null ? Component.empty() : parsed;
        panelW = Math.min(width - 20, 460);
        panelX = (width - panelW) / 2;
        textX = panelX + 92;
        textW = panelW - 104;
        lines = font.split(text, textW);
        totalChars = 0;
        for (FormattedCharSequence l : lines) totalChars += length(l);

        List<Component> labels = new ArrayList<>();
        for (String s : msg.options()) {
            Component c = Component.Serializer.fromJson(s);
            labels.add(c == null ? Component.literal(s) : c);
        }
        boolean leave = labels.isEmpty();
        if (leave) labels.add(Component.literal("[Ayrıl]"));
        int optH = labels.size() * 20;
        panelH = Math.max(118, 24 + lines.size() * 10 + 8 + optH + 6);
        panelY = height - panelH - 8;
        int y = panelY + 24 + lines.size() * 10 + 8;
        for (int i = 0; i < labels.size(); i++) {
            final int idx = leave ? -1 : i;
            Component label = leave ? labels.get(i) : Component.literal((i + 1) + ". ").append(labels.get(i));
            Button b = Button.builder(label, btn -> choose(idx)).bounds(textX, y + i * 20, textW, 18).build();
            b.visible = false;
            buttons.add(addRenderableWidget(b));
        }
    }

    private static int length(FormattedCharSequence s) {
        int[] n = {0};
        s.accept((i, style, cp) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    private static FormattedCharSequence cut(FormattedCharSequence s, int max) {
        return sink -> {
            int[] n = {0};
            return s.accept((i, style, cp) -> {
                if (n[0]++ >= max) return false;
                return sink.accept(i, style, cp);
            });
        };
    }

    private void choose(int index) {
        Net.CHANNEL.sendToServer(new Net.Choice(msg.entityId(), index));
        if (index < 0) Minecraft.getInstance().setScreen(null);
    }

    private boolean done() {
        return shown >= totalChars;
    }

    @Override
    public void tick() {
        if (!done()) {
            shown = Math.min(totalChars, shown + 2.2F);
            if ((int) shown / 3 != lastBlip) {
                lastBlip = (int) shown / 3;
                float pitch = msg.voice() * (0.92F + (float) Math.random() * 0.16F);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(voice(), Math.min(2F, pitch), 0.18F));
            }
        }
        for (Button b : buttons) b.visible = done();
        Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(msg.entityId());
        if (e == null || Minecraft.getInstance().player == null || e.distanceToSqr(Minecraft.getInstance().player) > 100) onClose();
    }

    private SoundEvent voice() {
        float v = msg.voice();
        if (v < 0.8F) return SoundEvents.NOTE_BLOCK_BASS.value();
        if (v < 0.95F) return SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
        if (v > 1.4F) return SoundEvents.NOTE_BLOCK_BELL.value();
        if (v > 1.1F && v < 1.2F) return SoundEvents.NOTE_BLOCK_CHIME.value();
        return SoundEvents.NOTE_BLOCK_FLUTE.value();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!done() && x >= panelX && x <= panelX + panelW && y >= panelY && y <= panelY + panelH) {
            shown = totalChars;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && done()) {
            int i = key - GLFW.GLFW_KEY_1;
            if (i < buttons.size()) {
                buttons.get(i).onPress();
                return true;
            }
        }
        if ((key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) && !done()) {
            shown = totalChars;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        Net.CHANNEL.sendToServer(new Net.Choice(msg.entityId(), -1));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        // alttan yükselen karartma
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0x90000000);
        // panel
        g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, GOLD);
        g.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, PANEL2, PANEL);
        // köşe süsleri
        for (int[] c : new int[][]{{panelX, panelY}, {panelX + panelW - 6, panelY}, {panelX, panelY + panelH - 6}, {panelX + panelW - 6, panelY + panelH - 6}}) {
            g.fill(c[0] + 1, c[1] + 1, c[0] + 5, c[1] + 5, 0xFF7FC37A);
        }
        // portre
        int px = panelX + 8, py = panelY + 8, pw = 76, ph = Math.min(panelH - 16, 112);
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF3C5A34);
        g.fillGradient(px, py, px + pw, py + ph, 0xFF243B2A, 0xFF0E1710);
        Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(msg.entityId());
        if (e instanceof LivingEntity le) {
            g.enableScissor(px, py, px + pw, py + ph);
            int cx = px + pw / 2, feet = py + ph + 46;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, feet, 58, cx - mouseX, py + 20 - mouseY, le);
            g.disableScissor();
        }
        // ad, unvan, dostluk
        g.drawString(font, Component.literal(msg.name()), textX, panelY + 8, GOLD, true);
        g.drawString(font, Component.literal("— " + msg.title()), textX + font.width(msg.name()) + 6, panelY + 8, 0xFF9DB89A, false);
        if (msg.friendship() >= 0) {
            StringBuilder h = new StringBuilder();
            for (int i = 0; i < 10; i++) h.append(i < msg.friendship() ? '❤' : '♡');
            String hs = h.toString();
            g.drawString(font, hs, panelX + panelW - 8 - font.width(hs), panelY + 8, 0xFFE0566A, false);
        }
        g.fill(textX, panelY + 19, panelX + panelW - 8, panelY + 20, 0x804F7A48);
        // metin
        int remaining = (int) shown, y = panelY + 24;
        for (FormattedCharSequence l : lines) {
            if (remaining <= 0) break;
            int n = length(l);
            g.drawString(font, n <= remaining ? l : cut(l, remaining), textX, y, 0xFFECE6D6, false);
            remaining -= n;
            y += 10;
        }
        if (!done() && (System.currentTimeMillis() / 300) % 2 == 0) g.drawString(font, "▼", panelX + panelW - 14, panelY + panelH - 12, GOLD, false);
        super.render(g, mouseX, mouseY, pt);
    }
}
