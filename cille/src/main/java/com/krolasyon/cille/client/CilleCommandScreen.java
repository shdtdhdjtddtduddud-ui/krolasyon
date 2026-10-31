package com.krolasyon.cille.client;

import com.krolasyon.cille.entity.CilleEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Cille command menu (R): battle orders on the left, the selected Cille's abilities on the right */
public class CilleCommandScreen extends Screen {
    @Nullable private final Entity target;
    @Nullable private final Vec3 point;
    @Nullable private CilleEntity cille;

    public CilleCommandScreen(@Nullable Entity target, @Nullable Vec3 point) {
        super(Component.translatable("screen.cille.title"));
        this.target = target;
        this.point = point;
    }

    @Override
    protected void init() {
        this.cille = CilleClient.myCille(this.minecraft);
        int w = 140, h = 20, gap = 3;
        int[] abil = cille != null ? cille.commandableAbilities() : new int[0];
        int abilCols = abil.length > 8 ? 2 : 1;
        int total = w + 12 + abilCols * (w + 6);
        int lx = this.width / 2 - total / 2;
        int y0 = this.height / 2 - 92;
        int[] cmds = {CilleEntity.CMD_ATTACK, CilleEntity.CMD_STOP, CilleEntity.CMD_AGGRESSIVE, CilleEntity.CMD_DEFEND,
                CilleEntity.CMD_RETREAT, CilleEntity.CMD_MOVE, CilleEntity.CMD_STAY, CilleEntity.CMD_RECALL};
        String[] keys = {"attack", "stop", "aggressive", "defend", "retreat", "move", "stay", "recall"};
        for (int i = 0; i < cmds.length; i++) {
            final int cmd = cmds[i];
            Button b = Button.builder(Component.translatable("screen.cille.cmd." + keys[i]), btn -> order(cmd))
                    .bounds(lx, y0 + i * (h + gap), w, h).build();
            if (cmd == CilleEntity.CMD_MOVE && point == null) b.active = false;
            if (cille == null) b.active = false;
            this.addRenderableWidget(b);
        }
        int rx = lx + w + 12;
        int perCol = abilCols == 2 ? (abil.length + 1) / 2 : abil.length;
        for (int i = 0; i < abil.length; i++) {
            final int id = abil[i];
            int col = i / Math.max(1, perCol), row = i % Math.max(1, perCol);
            MutableComponent label = Component.translatable(cille.abilityKey(id));
            String key = CilleKeys.slotLabel(i);
            if (!key.isEmpty()) label = label.append(" [" + key + "]");
            this.addRenderableWidget(Button.builder(label, btn -> order(CilleEntity.CMD_ABILITY + id))
                    .bounds(rx + col * (w + 6), y0 + row * (h + gap), w, h).build());
        }
    }

    private void order(int cmd) {
        CilleClient.send(cmd, target, cmd == CilleEntity.CMD_MOVE ? point : null);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        int y0 = this.height / 2 - 92;
        Component who = cille != null ? Component.translatable("screen.cille.title_named", cille.getDisplayName()) : this.title;
        g.drawCenteredString(this.font, who, this.width / 2, y0 - 36, 0xD8CCFF);
        Component tgt = target != null ? Component.translatable("screen.cille.target", target.getDisplayName())
                : Component.translatable("screen.cille.no_target");
        g.drawCenteredString(this.font, tgt, this.width / 2, y0 - 24, target != null ? 0xFF8080 : 0x909090);
        g.drawCenteredString(this.font, Component.translatable("screen.cille.select_hint"), this.width / 2, y0 - 13, 0x7080A0);
        if (cille == null) g.drawCenteredString(this.font, Component.translatable("msg.cille.cmd.none"), this.width / 2, y0 + 8 * 23 + 6, 0xFF6060);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
