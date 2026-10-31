package com.krolasyon.cille.client;

import com.krolasyon.cille.entity.KabkabaEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Cille command menu (R): battle orders on the left, Kabkaba's abilities on the right */
public class CilleCommandScreen extends Screen {
    @Nullable private final Entity target;
    @Nullable private final Vec3 point;

    public CilleCommandScreen(@Nullable Entity target, @Nullable Vec3 point) {
        super(Component.translatable("screen.cille.title"));
        this.target = target;
        this.point = point;
    }

    @Override
    protected void init() {
        int w = 150, h = 20, gap = 4;
        int lx = this.width / 2 - w - 10, rx = this.width / 2 + 10;
        int y0 = this.height / 2 - 82;
        int[] cmds = {KabkabaEntity.CMD_ATTACK, KabkabaEntity.CMD_STOP, KabkabaEntity.CMD_AGGRESSIVE, KabkabaEntity.CMD_DEFEND,
                KabkabaEntity.CMD_RETREAT, KabkabaEntity.CMD_MOVE, KabkabaEntity.CMD_STAY, KabkabaEntity.CMD_RECALL};
        String[] keys = {"attack", "stop", "aggressive", "defend", "retreat", "move", "stay", "recall"};
        for (int i = 0; i < cmds.length; i++) {
            final int cmd = cmds[i];
            Button b = Button.builder(Component.translatable("screen.cille.cmd." + keys[i]), btn -> order(cmd))
                    .bounds(lx, y0 + i * (h + gap), w, h).build();
            if (cmd == KabkabaEntity.CMD_MOVE && point == null) b.active = false;
            this.addRenderableWidget(b);
        }
        for (int i = 0; i < CilleKeys.ABILITY_IDS.length; i++) {
            final int id = CilleKeys.ABILITY_IDS[i];
            this.addRenderableWidget(Button.builder(Component.translatable("ability.cille." + id).append(" [ALT+" + (i + 1) + "]"),
                    btn -> order(KabkabaEntity.CMD_ABILITY + id)).bounds(rx, y0 + i * (h + gap), w, h).build());
        }
    }

    private void order(int cmd) {
        CilleClient.send(cmd, target, cmd == KabkabaEntity.CMD_MOVE ? point : null);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        int y0 = this.height / 2 - 82;
        g.drawCenteredString(this.font, this.title, this.width / 2, y0 - 34, 0xD8CCFF);
        Component tgt = target != null ? Component.translatable("screen.cille.target", target.getDisplayName())
                : Component.translatable("screen.cille.no_target");
        g.drawCenteredString(this.font, tgt, this.width / 2, y0 - 20, target != null ? 0xFF8080 : 0x909090);
        g.drawString(this.font, Component.translatable("screen.cille.orders"), this.width / 2 - 160, y0 - 10, 0xA0C8FF);
        g.drawString(this.font, Component.translatable("screen.cille.abilities"), this.width / 2 + 10, y0 - 10, 0xC8A0FF);
        KabkabaEntity k = CilleClient.myKabkaba(this.minecraft);
        if (k == null) g.drawCenteredString(this.font, Component.translatable("msg.cille.cmd.none"), this.width / 2, y0 + 8 * 24 + 6, 0xFF6060);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
