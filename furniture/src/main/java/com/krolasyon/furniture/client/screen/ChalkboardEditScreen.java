package com.krolasyon.furniture.client.screen;

import com.krolasyon.furniture.blockentity.ChalkboardBlockEntity;
import com.krolasyon.furniture.network.ChalkboardEditPacket;
import com.krolasyon.furniture.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ChalkboardEditScreen extends Screen {
    private final ChalkboardBlockEntity board;
    private final EditBox[] boxes = new EditBox[ChalkboardBlockEntity.LINES];

    public ChalkboardEditScreen(ChalkboardBlockEntity board) {
        super(Component.translatable("gui.krolasyonfurniture.chalkboard"));
        this.board = board;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        for (int i = 0; i < boxes.length; i++) {
            EditBox box = new EditBox(font, cx - 90, 44 + i * 23, 180, 20, Component.empty());
            box.setMaxLength(ChalkboardBlockEntity.MAX_LEN);
            box.setValue(board.getLine(i));
            boxes[i] = box;
            addRenderableWidget(box);
        }
        setInitialFocus(boxes[0]);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(cx - 100, 44 + boxes.length * 23 + 10, 200, 20).build());
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_DOWN) {
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i].isFocused()) {
                    if (i + 1 < boxes.length) setFocused(boxes[i + 1]);
                    else onClose();
                    return true;
                }
            }
        }
        if (key == GLFW.GLFW_KEY_UP) {
            for (int i = 1; i < boxes.length; i++) {
                if (boxes[i].isFocused()) {
                    setFocused(boxes[i - 1]);
                    return true;
                }
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        String[] lines = new String[boxes.length];
        for (int i = 0; i < boxes.length; i++) lines[i] = boxes[i].getValue();
        ModNetwork.CHANNEL.sendToServer(new ChalkboardEditPacket(board.getBlockPos(), lines));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
