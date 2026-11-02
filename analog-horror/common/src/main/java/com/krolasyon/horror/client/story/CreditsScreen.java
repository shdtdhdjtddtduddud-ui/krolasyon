package com.krolasyon.horror.client.story;

import com.krolasyon.horror.story.StoryBridge;
import com.krolasyon.horror.story.StoryData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** Ending title card followed by rolling credits. */
public class CreditsScreen extends Screen {
    private final String ending;
    private int t;
    private boolean done;
    private static final int LENGTH = 760;

    public CreditsScreen(String ending) {
        super(Component.empty());
        this.ending = ending;
    }

    @Override
    protected void init() {
        if (t == 0) ClientStory.play(ending.equals("host") ? "story.jingle" : "story.music_box", 0.8F);
    }

    @Override
    public void tick() {
        if (++t >= LENGTH) onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void report() {
        if (!done) {
            done = true;
            StoryBridge.TO_SERVER.add(StoryBridge.Msg.of("credits_done"));
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
        float tt = t + pt;
        if (tt < 200) {
            float a = Math.min(1F, tt / 30F) * Math.min(1F, (200 - tt) / 30F);
            g.pose().pushPose();
            g.pose().scale(2.2F, 2.2F, 1F);
            g.drawCenteredString(this.font, I18n.get("ending.krolasyonhorror." + ending), (int) (w / 2 / 2.2F), (int) (h / 2 / 2.2F) - 4,
                    StoryOverlay.argb(a, 0xEDE3C8));
            g.pose().popPose();
            return;
        }
        float scroll = (tt - 200) * 0.55F;
        for (int i = 0; i < StoryData.CREDIT_LINES; i++) {
            int y = (int) (h + 10 + i * 22 - scroll);
            if (y < -20 || y > h + 20) continue;
            String s = I18n.get("credits.krolasyonhorror." + i);
            int col = i == 0 ? 0xFFE8C66A : 0xFFD8D0C0;
            g.drawCenteredString(this.font, s, w / 2, y, col);
        }
    }
}
