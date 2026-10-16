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

import java.util.List;

/** Spin gauge + ability cooldowns for the player's active Libra. */
public class LibraHud {
    private static final ResourceLocation ICON = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/libra_icon.png");
    private static final ResourceLocation ICON_LDRAGO = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/ldrago_icon.png");
    private static final ResourceLocation ICON_LEONE = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/leone_icon.png");
    private static final int[] LE_COLORS = {0xFF9CFFB0, 0xFF6CFF6A, 0xFF40E080, 0xFFE0C070, 0xFFB0FFD0, 0xFFD8D8D8};
    private static final ResourceLocation ICON_EAGLE = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/eagle_icon.png");
    private static final int[] EA_COLORS = {0xFFE0C0FF, 0xFFC0A0FF, 0xFFFF7070, 0xFFD0D0E0, 0xFFFFD060};
    private static final ResourceLocation ICON_PEGASUS = com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/pegasus_icon.png");
    private static final int[] PG_COLORS = {0xFFA0D8FF, 0xFF70B0FF, 0xFFFFF080, 0xFF60C0FF, 0xFFFFFFFF};
    private static final int[] DR_COLORS = {0xFFFF4060, 0xFFE040A0, 0xFFFF8A2A, 0xFFFF3050, 0xFFFFB040};
    private static final int[] MODE_COLORS = {0xFFFF6050, 0xFF60A0FF, 0xFF70FF90, 0xFFFFD050};
    private static final int[] AB_COLORS = {0xFF6CFF9A, 0xFF5CFFB0, 0xFFFF8A2A, 0xFF7AFFF0, 0xFFFFD25A};

    public void render(GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        BeybladeEntity bey = null;
        List<BeybladeEntity> list = mc.level.getEntitiesOfClass(BeybladeEntity.class, mc.player.getBoundingBox().inflate(96),
                b -> mc.player.getUUID().equals(b.getOwnerId()));
        if (!list.isEmpty()) bey = list.get(0);
        if (bey == null) return;
        Font font = mc.font;
        int x = 8, y = height / 2 - 66;
        g.fill(x - 4, y - 4, x + 168, y + 174, 0x88000000);
        boolean tempo = bey.kindId().equals("tempo"), variares = bey.kindId().equals("variares");
        boolean drago = bey.kindId().equals("ldrago"), leone = bey.kindId().equals("leone"), eagle = bey.kindId().equals("eagle"), pegasus = bey.kindId().equals("pegasus");
        com.krolasyon.beyblade.entity.CommonBeybladeEntity common = bey instanceof com.krolasyon.beyblade.entity.CommonBeybladeEntity c ? c : null;
        if (common != null) g.blit(com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/common_" + common.getVariant() + "_icon.png"), x, y, 0, 0, 24, 24, 24, 24);
        else if (variares) g.blit(com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/variares_icon.png"), x, y, 0, 0, 24, 24, 24, 24);
        else if (tempo) g.blit(com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "textures/item/tempo_icon.png"), x, y, 0, 0, 24, 24, 24, 24);
        else g.blit(drago ? ICON_LDRAGO : (leone ? ICON_LEONE : (eagle ? ICON_EAGLE : (pegasus ? ICON_PEGASUS : ICON))), x, y, 0, 0, 24, 24, 24, 24);
        g.drawString(font, Component.translatable(bey.titleKey()), x + 29, y + 2, common != null ? 0xFF000000 | common.variant().primary() : tempo ? 0xFFD0E4FF : variares ? 0xFFFF4040 : drago ? 0xFFFF5070 : (leone ? 0xFF60FF90 : (eagle ? 0xFFC890FF : (pegasus ? 0xFF70B8FF : 0xFFB6FF6A))), true);
        String state = switch (bey.getState()) {
            case BeybladeEntity.STATE_DEFEATED -> "hud.librabeyblade.state.defeated";
            case BeybladeEntity.STATE_RECALL -> "hud.librabeyblade.state.recall";
            default -> bey.getAbility() != BeybladeEntity.AB_NONE ? "hud.librabeyblade.state.ability" : "hud.librabeyblade.state.battle";
        };
        g.drawString(font, Component.translatable(state), x + 29, y + 13, 0xFFCFCFCF, true);
        String lv = "Lv " + bey.getLevel();
        g.drawString(font, lv, x + 160 - font.width(lv), y + 13, 0xFFFFD040, true);
        // spin gauge
        float ratio = bey.spinRatio();
        int by = y + 29, bw = 160;
        g.fill(x, by, x + bw, by + 7, 0xFF1A1A1A);
        int col = ratio > 0.5F ? 0xFF7CFF4A : (ratio > 0.2F ? 0xFFFFD040 : 0xFFFF4A3A);
        g.fill(x + 1, by + 1, x + 1 + (int) ((bw - 2) * ratio), by + 6, col);
        g.drawString(font, Component.translatable("hud.librabeyblade.spin", Mth.ceil(bey.getSpin()), Mth.ceil(bey.getMaxSpin())), x, by + 9, 0xFFFFFFFF, true);
        // experience bar
        float xr = bey.getLevel() >= BeybladeEntity.MAX_LEVEL ? 1F : Mth.clamp(bey.getXp() / (float) BeybladeEntity.xpNeeded(bey.getLevel()), 0F, 1F);
        g.fill(x, by + 18, x + bw, by + 20, 0xFF202020);
        g.fill(x, by + 18, x + (int) (bw * xr), by + 20, 0xFF60D0FF);
        // abilities
        int ay = by + 24;
        for (int i = 1; i <= bey.abilityCount(); i++) {
            KeyMapping key = ClientSetup.ABILITY_KEYS[i - 1];
            int cd = bey.cooldownRemaining(i);
            boolean active = bey.getAbility() == i || (i == bey.buffAbility() && bey.isShielded());
            boolean locked = !bey.abilityAvailable(i);
            int c = active ? 0xFFFFFFFF : (cd > 0 || locked ? 0xFF808080 : common != null ? 0xFF000000 | common.variant().secondary() : tempo ? 0xFFC8DCFF : variares ? 0xFFFF6A50 : (drago ? DR_COLORS : (leone ? LE_COLORS : (eagle ? EA_COLORS : (pegasus ? PG_COLORS : AB_COLORS))))[i - 1]);
            String keyName = key.getTranslatedKeyMessage().getString();
            g.drawString(font, "[" + keyName + "]", x, ay, 0xFFFFE070, true);
            g.drawString(font, Component.translatable(bey.abilityKey(i)), x + 22, ay, c, true);
            if (locked && common != null && i == com.krolasyon.beyblade.entity.CommonBeybladeEntity.CM_BURST) {
                String s = "Lv" + com.krolasyon.beyblade.entity.CommonBeybladeEntity.BURST_UNLOCK;
                g.drawString(font, s, x + 160 - font.width(s), ay, 0xFF909090, true);
            } else if (cd > 0 && !locked) {
                String s = ((cd + 19) / 20) + "s";
                g.drawString(font, s, x + 160 - font.width(s), ay, 0xFFFF7070, true);
            } else if (active) {
                g.drawString(font, "✦", x + 154, ay, 0xFFFFFFFF, true);
            }
            ay += 10;
        }
        g.drawString(font, "[" + ClientSetup.RECALL_KEY.getTranslatedKeyMessage().getString() + "] ", x, ay + 2, 0xFFFFE070, true);
        g.drawString(font, Component.translatable("hud.librabeyblade.recall"), x + 22, ay + 2, 0xFFB0E0FF, true);
        ay += 12;
        g.drawString(font, "[" + ClientSetup.MODE_KEY.getTranslatedKeyMessage().getString() + "]", x, ay, 0xFFFFE070, true);
        boolean nat = bey.getMode() == bey.nativeMode();
        g.drawString(font, Component.translatable("mode.librabeyblade." + BeybladeEntity.MODE_IDS[bey.getMode()]).append(nat ? " \u2605" : ""), x + 22, ay, MODE_COLORS[bey.getMode()], true);
        ay += 10;
        g.drawString(font, "[" + ClientSetup.COMMAND_KEY.getTranslatedKeyMessage().getString() + "]", x, ay, 0xFFFFE070, true);
        g.drawString(font, Component.translatable("order.librabeyblade." + BeybladeEntity.ORDER_IDS[bey.getOrder()]), x + 22, ay, 0xFFE0E0E0, true);
        // Blader Spirit gauge
        ay += 12;
        g.drawString(font, "[" + ClientSetup.SPIRIT_KEY.getTranslatedKeyMessage().getString() + "]", x, ay, 0xFFFFE070, true);
        boolean sp = bey.isSpiritActive();
        float sr = sp ? bey.spiritRemaining() / (float) BeybladeEntity.SPIRIT_TICKS : bey.getSpirit() / (float) BeybladeEntity.SPIRIT_MAX;
        g.fill(x + 22, ay + 1, x + 160, ay + 8, 0xFF202020);
        int scol = sp ? ((mc.level.getGameTime() / 3) % 2 == 0 ? 0xFFFFE060 : 0xFFFF9020) : (bey.getSpirit() >= BeybladeEntity.SPIRIT_MAX ? 0xFFFFD040 : 0xFFB07020);
        g.fill(x + 23, ay + 2, x + 23 + (int) (136 * Mth.clamp(sr, 0F, 1F)), ay + 7, scol);
        g.drawString(font, Component.translatable(sp ? "hud.librabeyblade.spirit_on" : (bey.getSpirit() >= BeybladeEntity.SPIRIT_MAX ? "hud.librabeyblade.spirit_ready" : "hud.librabeyblade.spirit")),
                x + 26, ay, 0xFFFFFFFF, true);
        ay += 11;
        net.minecraft.network.chat.Component passive = bey.passiveLine();
        if (passive != null) {
            g.drawString(font, passive, x, ay, 0xFFFF6070, true);
            ay += 10;
        }
        if (bey.isFinalDrive()) g.drawString(font, Component.translatable("hud.librabeyblade.final_drive"), x, ay, 0xFFFF6040, true);
        else if (bey.isLifeAfterDeath()) g.drawString(font, Component.translatable("hud.librabeyblade.lad"), x, ay, 0xFF60FF90, true);
    }
}
