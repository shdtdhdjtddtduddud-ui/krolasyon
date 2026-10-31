package com.krolasyon.cille.client;

import com.krolasyon.cille.entity.KabkabaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** small status panel: your Kabkaba's health, stance and order */
public final class CilleHud implements IGuiOverlay {
    public static final CilleHud INSTANCE = new CilleHud();

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        KabkabaEntity k = CilleClient.myKabkaba(mc);
        if (k == null) return;
        int x = 6, y = 6, w = 132;
        g.fill(x - 3, y - 3, x + w + 3, y + 37, 0x90100A1C);
        g.drawString(mc.font, k.getDisplayName(), x, y, 0xD8CCFF);
        float hp = k.getHealth() / k.getMaxHealth();
        g.fill(x, y + 11, x + w, y + 16, 0xFF2A2033);
        g.fill(x, y + 11, x + (int) (w * hp), y + 16, hp > 0.5F ? 0xFF8A4CFF : hp > 0.25F ? 0xFFD08A30 : 0xFFE03040);
        Component stance = Component.translatable("stance.cille." + k.getStance());
        Component mode = Component.translatable(k.isStaying() ? "msg.cille.stay" : "msg.cille.follow");
        g.drawString(mc.font, stance.copy().append(" | ").append(mode), x, y + 19, 0xB0B8D0);
        g.drawString(mc.font, Component.translatable("hud.cille.hint"), x, y + 29, 0x7080A0);
    }
}
