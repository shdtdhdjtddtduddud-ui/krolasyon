package com.krolasyon.horror.client.story;

import com.krolasyon.horror.compat.Compat;
import com.krolasyon.horror.story.StoryBridge;
import com.krolasyon.horror.story.StoryData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Full-screen VHS playback of one of the family's tapes (or a Channel 13 broadcast). */
public class TapeScreen extends Screen {
    private static final ResourceLocation[] STATIC = {
            Compat.id("textures/gui/static_0.png"), Compat.id("textures/gui/static_1.png"),
            Compat.id("textures/gui/static_2.png"), Compat.id("textures/gui/static_3.png")};
    private final String id;
    private final StoryData.Tape tape;
    private int t;
    private boolean done;
    private final RandomSource rng = RandomSource.create();

    public TapeScreen(String id) {
        super(Component.empty());
        this.id = id;
        this.tape = StoryData.TAPES.get(id);
    }

    @Override
    public void tick() {
        if (tape == null) {
            onClose();
            return;
        }
        for (StoryData.Cue c : tape.cues()) if (c.tick() == t) ClientStory.play(c.sound(), c.volume());
        if (++t >= tape.length()) onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void report() {
        if (!done) {
            done = true;
            StoryBridge.TO_SERVER.add(StoryBridge.Msg.of("tape_done", id));
        }
    }

    @Override
    public void onClose() {
        report();
        super.onClose();
    }

    /** however the screen goes away (ESC, end of tape, replaced by another screen) the story must hear about it */
    @Override
    public void removed() {
        report();
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = this.width, h = this.height;
        g.fill(0, 0, w, h, 0xFF000000);
        if (tape == null) return;
        String frame = tape.frames()[0].texture();
        for (StoryData.Frame f : tape.frames()) if (t >= f.tick()) frame = f.texture();
        int ih = h, iw = ih * 4 / 3;
        if (iw > w) {
            iw = w;
            ih = w * 3 / 4;
        }
        int x0 = (w - iw) / 2, y0 = (h - ih) / 2;
        int jitter = rng.nextInt(40) == 0 ? rng.nextInt(7) - 3 : 0;
        RenderSystem.enableBlend();
        g.blit(Compat.id("textures/tape/" + frame + ".png"), x0 + jitter, y0, iw, ih, 0, 0, 320, 240, 320, 240);
        // tape noise
        float sa = t < 10 ? 0.45F - t * 0.04F : 0.05F + (rng.nextInt(150) == 0 ? 0.2F : 0F);
        g.setColor(1F, 1F, 1F, sa);
        g.blit(STATIC[rng.nextInt(4)], x0, y0, iw, ih, rng.nextInt(128), rng.nextInt(64), iw / 3, ih / 3, 256, 128);
        g.setColor(1F, 1F, 1F, 1F);
        for (int y = y0; y < y0 + ih; y += 3) g.fill(x0, y, x0 + iw, y + 1, 0x12000000);
        int band = y0 + (t * 5) % (ih + 30) - 15;
        g.fill(x0, band, x0 + iw, band + 3, 0x14FFFFFF);
        if (rng.nextInt(6) == 0) {
            int y = y0 + rng.nextInt(ih);
            g.fill(x0, y, x0 + iw, y + 1 + rng.nextInt(2), 0x40FFFFFF);
        }
        // on screen display
        if ((t / 15) % 2 == 0) g.drawString(this.font, "PLAY ▶", x0 + 12, y0 + 10, 0xFFEDEDED, true);
        g.drawString(this.font, I18n.get("tape.krolasyonhorror." + id + ".label"), x0 + iw - 12 - this.font.width(I18n.get("tape.krolasyonhorror." + id + ".label")),
                y0 + 10, 0xFFEDEDED, true);
        int secs = t / 20;
        g.drawString(this.font, String.format("SP  0:%02d:%02d", secs / 60, secs % 60), x0 + 12, y0 + ih - 18, 0xFFEDEDED, true);
        g.drawString(this.font, I18n.get("msg.krolasyonhorror.skip"), x0 + iw - 12 - this.font.width(I18n.get("msg.krolasyonhorror.skip")), y0 + ih - 18, 0x80EDEDED, false);
        for (StoryData.Line l : tape.lines())
            if (t >= l.start() && t < l.start() + l.dur()) StoryOverlay.drawLine(g, this.font, w, y0 + ih - 34, l, t - l.start());
    }
}
