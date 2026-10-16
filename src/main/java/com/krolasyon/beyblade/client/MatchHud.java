package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.StadiumEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Scoreboard at the top of the screen while a stadium match is running nearby. */
public class MatchHud {
    public void render(GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        List<StadiumEntity> list = mc.level.getEntitiesOfClass(StadiumEntity.class, mc.player.getBoundingBox().inflate(48),
                s -> s.matchState() != StadiumEntity.M_IDLE);
        if (list.isEmpty()) return;
        StadiumEntity s = list.get(0);
        Font f = mc.font;
        String a = s.nameA(), b = s.nameB();
        String score = s.scoreA() + "  -  " + s.scoreB();
        int cx = width / 2, y = 6;
        int w = Math.max(220, f.width(a) + f.width(b) + f.width(score) + 60);
        g.fill(cx - w / 2, y - 3, cx + w / 2, y + 26, 0xA0000000);
        g.fill(cx - w / 2, y - 3, cx - w / 2 + 3, y + 26, 0xFF3F7FFF);
        g.fill(cx + w / 2 - 3, y - 3, cx + w / 2, y + 26, 0xFFFF4030);
        g.drawString(f, a, cx - f.width(score) / 2 - 12 - f.width(a), y + 2, 0xFF7FB0FF, true);
        g.drawString(f, b, cx + f.width(score) / 2 + 12, y + 2, 0xFFFF8070, true);
        g.drawCenteredString(f, score, cx, y + 2, 0xFFFFE070);
        String state = switch (s.matchState()) {
            case StadiumEntity.M_COUNTDOWN -> Component.translatable("hud.librabeyblade.match.countdown", s.round()).getString();
            case StadiumEntity.M_BATTLE -> Component.translatable("hud.librabeyblade.match.battle", s.round()).getString();
            case StadiumEntity.M_ROUND_END -> Component.translatable("hud.librabeyblade.match.round_end").getString();
            default -> Component.translatable("hud.librabeyblade.match.end").getString();
        };
        g.drawCenteredString(f, state, cx, y + 14, 0xFFCCCCCC);
        g.drawCenteredString(f, Component.translatable("hud.librabeyblade.match.goal", StadiumEntity.WIN_POINTS).getString(), cx, y + 30, 0xFF909090);
    }
}
