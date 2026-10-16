package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.item.LaunchPlan;
import com.krolasyon.beyblade.item.LauncherItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The launch gauge next to the crosshair while winding a launcher: power (with the Perfect Launch window), the
 * launcher angle against the clean 15..50 degree zone, how steady the launch is and the main thing to fix.
 */
public class LaunchHud {
    public void render(GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !mc.player.isUsingItem() || !(mc.player.getUseItem().getItem() instanceof LauncherItem)) return;
        int used = mc.player.getTicksUsingItem();
        LaunchPlan plan = LaunchPlan.of(mc.player, used);
        Font f = mc.font;
        int x = width / 2 + 18, y = height / 2 - 34, w = 128;
        g.fill(x - 4, y - 4, x + w + 4, y + 66, 0x90000000);
        // power: wind-up bar, the gold Perfect Launch window right at full charge, red when the arm tires
        g.drawString(f, Component.translatable("launch.librabeyblade.power"), x, y, 0xFFFFFFFF, true);
        int bx = x + 44, bw = w - 44;
        float pw = Mth.clamp(used / (float) LauncherItem.FULL_CHARGE, 0F, 1F);
        g.fill(bx, y + 1, bx + bw, y + 8, 0xFF202020);
        int col = plan.overcharged ? 0xFFFF4040 : plan.perfect ? 0xFFFFD040 : 0xFF50E070;
        g.fill(bx, y + 1, bx + Math.round(bw * pw), y + 8, col);
        if (plan.perfect && (used / 2) % 2 == 0) g.fill(bx - 1, y, bx + bw + 1, y + 9, 0x60FFE080);
        // angle: a strip 0..90 degrees down, clean zone in green, the current angle as a marker
        g.drawString(f, Component.translatable("launch.librabeyblade.angle", Math.round(plan.pitch)), x, y + 14, plan.idealAngle() ? 0xFF70FF90 : 0xFFFF7060, true);
        int ay = y + 24;
        g.fill(bx, ay, bx + bw, ay + 5, 0xFF402020);
        int z0 = bx + Math.round(bw * LaunchPlan.IDEAL_MIN / 90F), z1 = bx + Math.round(bw * LaunchPlan.IDEAL_MAX / 90F);
        g.fill(z0, ay, z1, ay + 5, 0xFF2E8A3E);
        int mk = bx + Math.round(bw * Mth.clamp(plan.pitch, 0F, 90F) / 90F);
        g.fill(mk - 1, ay - 2, mk + 1, ay + 7, 0xFFFFFFFF);
        // steadiness: what is left after all the mistakes
        float steady = 1F - plan.wobble;
        g.drawString(f, Component.translatable("launch.librabeyblade.balance"), x, y + 32, 0xFFFFFFFF, true);
        g.fill(bx, y + 33, bx + bw, y + 40, 0xFF202020);
        int sc = steady > 0.7F ? 0xFF50E070 : steady > 0.4F ? 0xFFF0D040 : 0xFFFF4040;
        g.fill(bx, y + 33, bx + Math.round(bw * steady), y + 40, sc);
        g.drawString(f, Math.round(steady * 100) + "%", bx + bw - f.width(Math.round(steady * 100) + "%") - 1, y + 32, 0xFFFFFFFF, true);
        // the advice
        Component hint = plan.hint == null ? Component.translatable("launch.librabeyblade.hint.good")
                : Component.translatable("launch.librabeyblade.hint." + plan.hint);
        int hc = plan.hint == null ? 0xFF90FFA0 : "perfect".equals(plan.hint) ? 0xFFFFD040 : 0xFFFFA070;
        g.drawString(f, hint, x, y + 46, hc, true);
        if (plan.powerLaunch) g.drawString(f, Component.translatable("launch.librabeyblade.power_launch"), x, y + 56, 0xFFFF8080, true);
    }
}
