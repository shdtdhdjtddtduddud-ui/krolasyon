package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Spin gauge + ability cooldowns for the player's active Libra. */
public class LibraHud implements IGuiOverlay {
    private static final ResourceLocation ICON = new ResourceLocation(LibraBeyblade.MODID, "textures/item/libra_icon.png");
    private static final ResourceLocation ICON_LDRAGO = new ResourceLocation(LibraBeyblade.MODID, "textures/item/ldrago_icon.png");
    private static final int[] DR_COLORS = {0xFFFF4060, 0xFFE040A0, 0xFFFF8A2A, 0xFFFF3050, 0xFFFFB040};
    private static final int[] AB_COLORS = {0xFF6CFF9A, 0xFF5CFFB0, 0xFFFF8A2A, 0xFF7AFFF0, 0xFFFFD25A};

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        BeybladeEntity bey = null;
        List<BeybladeEntity> list = mc.level.getEntitiesOfClass(BeybladeEntity.class, mc.player.getBoundingBox().inflate(96),
                b -> mc.player.getUUID().equals(b.getOwnerId()));
        if (!list.isEmpty()) bey = list.get(0);
        if (bey == null) return;
        Font font = mc.font;
        int x = 8, y = height / 2 - 52;
        g.fill(x - 4, y - 4, x + 168, y + 106, 0x88000000);
        boolean drago = bey.kindId().equals("ldrago");
        g.blit(drago ? ICON_LDRAGO : ICON, x, y, 0, 0, 24, 24, 24, 24);
        g.drawString(font, Component.translatable("hud.librabeyblade.title." + bey.kindId()), x + 29, y + 2, drago ? 0xFFFF5070 : 0xFFB6FF6A, true);
        String state = switch (bey.getState()) {
            case BeybladeEntity.STATE_DEFEATED -> "hud.librabeyblade.state.defeated";
            case BeybladeEntity.STATE_RECALL -> "hud.librabeyblade.state.recall";
            default -> bey.getAbility() != BeybladeEntity.AB_NONE ? "hud.librabeyblade.state.ability" : "hud.librabeyblade.state.battle";
        };
        g.drawString(font, Component.translatable(state), x + 29, y + 13, 0xFFCFCFCF, true);
        // spin gauge
        float ratio = bey.spinRatio();
        int by = y + 29, bw = 160;
        g.fill(x, by, x + bw, by + 7, 0xFF1A1A1A);
        int col = ratio > 0.5F ? 0xFF7CFF4A : (ratio > 0.2F ? 0xFFFFD040 : 0xFFFF4A3A);
        g.fill(x + 1, by + 1, x + 1 + (int) ((bw - 2) * ratio), by + 6, col);
        g.drawString(font, Component.translatable("hud.librabeyblade.spin", Mth.ceil(bey.getSpin()), Mth.ceil(bey.getMaxSpin())), x, by + 9, 0xFFFFFFFF, true);
        // abilities
        int ay = by + 21;
        for (int i = 1; i <= BeybladeEntity.ABILITY_COUNT; i++) {
            KeyMapping key = ClientSetup.ABILITY_KEYS[i - 1];
            int cd = bey.cooldownRemaining(i);
            boolean active = bey.getAbility() == i || (i == bey.buffAbility() && bey.isShielded());
            int c = active ? 0xFFFFFFFF : (cd > 0 ? 0xFF808080 : (drago ? DR_COLORS : AB_COLORS)[i - 1]);
            String keyName = key.getTranslatedKeyMessage().getString();
            g.drawString(font, "[" + keyName + "]", x, ay, 0xFFFFE070, true);
            g.drawString(font, Component.translatable(bey.abilityKey(i)), x + 22, ay, c, true);
            if (cd > 0) {
                String s = ((cd + 19) / 20) + "s";
                g.drawString(font, s, x + 160 - font.width(s), ay, 0xFFFF7070, true);
            } else if (active) {
                g.drawString(font, "✦", x + 154, ay, 0xFFFFFFFF, true);
            }
            ay += 10;
        }
        g.drawString(font, "[" + ClientSetup.RECALL_KEY.getTranslatedKeyMessage().getString() + "] ", x, ay + 2, 0xFFFFE070, true);
        g.drawString(font, Component.translatable("hud.librabeyblade.recall"), x + 22, ay + 2, 0xFFB0E0FF, true);
    }
}
