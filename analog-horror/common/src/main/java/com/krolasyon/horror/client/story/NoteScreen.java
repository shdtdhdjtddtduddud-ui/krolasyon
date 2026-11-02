package com.krolasyon.horror.client.story;

import com.krolasyon.horror.compat.Compat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Reading a note: aged paper, handwriting-dark ink, pages if it is long. */
public class NoteScreen extends Screen {
    private static final ResourceLocation PAPER = Compat.id("textures/gui/paper.png");
    private final int note;
    private int page;
    private List<FormattedCharSequence> lines;
    private int perPage = 14, px, py, pw, ph;

    public NoteScreen(int note) {
        super(Component.empty());
        this.note = note;
    }

    @Override
    protected void init() {
        ph = (int) (this.height * 0.9F);
        pw = ph * 256 / 320;
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        lines = this.font.split(Component.literal(I18n.get("note.krolasyonhorror." + note + ".body")), pw - 36);
        perPage = Math.max(4, (ph - 64) / 10);
        ClientStory.play("story.page", 0.8F);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private int pages() { return Math.max(1, (lines.size() + perPage - 1) / perPage); }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_D) return flip(1);
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) return flip(-1);
        if (key == GLFW.GLFW_KEY_E) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (page + 1 < pages()) return flip(1);
        onClose();
        return true;
    }

    private boolean flip(int d) {
        int np = Math.max(0, Math.min(pages() - 1, page + d));
        if (np != page) {
            page = np;
            ClientStory.play("story.page", 0.6F);
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        g.blit(PAPER, px, py, pw, ph, 0, 0, 256, 320, 256, 320);
        String title = I18n.get("note.krolasyonhorror." + note + ".title");
        g.drawCenteredString(this.font, title, this.width / 2, py + 16, 0xFF4A2C1C);
        g.fill(px + 24, py + 28, px + pw - 24, py + 29, 0x604A2C1C);
        int y = py + 38;
        for (int i = page * perPage; i < Math.min(lines.size(), (page + 1) * perPage); i++) {
            g.drawString(this.font, lines.get(i), px + 18, y, 0xFF2A2018, false);
            y += 10;
        }
        if (pages() > 1) g.drawCenteredString(this.font, (page + 1) + " / " + pages() + "  ▶", this.width / 2, py + ph - 18, 0xFF6A5040);
    }
}
