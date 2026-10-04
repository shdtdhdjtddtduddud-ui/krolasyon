package com.krolasyon.bosses.rpg.client.gui;

import com.krolasyon.bosses.rpg.client.ClientRpg;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Site;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** M: map of the known world with kingdoms, towns and lairs. Drag to pan, scroll to zoom. */
public class MapScreen extends RpgScreen {
    private double cx, cz, zoom = 0.06;
    private boolean centred;

    public MapScreen() { super("Dünya Haritası"); }

    @Override
    protected void init() {
        pw = width - 40;
        ph = height - 40;
        left = 20;
        top = 20;
        if (!centred && minecraft != null && minecraft.player != null) {
            cx = minecraft.player.getX();
            cz = minecraft.player.getZ();
            centred = true;
        }
        button("Merkez", left + pw - 62, top + 4, 56, 16, b -> {
            if (minecraft != null && minecraft.player != null) { cx = minecraft.player.getX(); cz = minecraft.player.getZ(); }
        });
    }

    private int sx(double x) { return (int) (left + pw / 2.0 + (x - cx) * zoom); }
    private int sz(double z) { return (int) (top + ph / 2.0 + (z - cz) * zoom); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        panel(g, left, top, pw, ph);
        g.enableScissor(left, top, left + pw, top + ph);
        ListTag sites = ClientRpg.data.getList("Sites", Tag.TAG_COMPOUND);
        // kingdom territories as faint discs around capitals
        for (int i = 0; i < sites.size(); i++) {
            CompoundTag s = sites.getCompound(i);
            if (s.getByte("T") != Site.Type.CAPITAL.ordinal()) continue;
            Kingdom k = Kingdom.of(s.getByte("K"));
            int r = (int) (k.radius * zoom);
            int x0 = sx(s.getInt("X")), z0 = sz(s.getInt("Z"));
            int step = Math.max(2, r / 24);
            for (int dz = -r; dz <= r; dz += step) {
                int half = (int) Math.sqrt(Math.max(0, r * r - dz * dz));
                g.fill(x0 - half, z0 + dz, x0 + half, z0 + dz + step, 0x22000000 | k.color);
            }
        }
        List<String> hover = new ArrayList<>();
        for (int i = 0; i < sites.size(); i++) {
            CompoundTag s = sites.getCompound(i);
            Site.Type t = Site.Type.values()[Math.floorMod(s.getByte("T"), Site.Type.values().length)];
            int x = sx(s.getInt("X")), z = sz(s.getInt("Z"));
            int k = s.getByte("K");
            int col = k >= 0 ? 0xFF000000 | Kingdom.of(k).color : 0xFF909090;
            int size = switch (t) { case CAPITAL -> 5; case CITY -> 3; case VILLAGE -> 2; default -> 2; };
            if (t == Site.Type.LAIR) {
                g.drawString(font, "☠", x - 3, z - 4, 0xFFFF4040, true);
            } else if (t == Site.Type.CAMP) {
                g.drawString(font, "⚔", x - 3, z - 4, 0xFFD08040, true);
            } else if (t == Site.Type.RUIN) {
                g.drawString(font, "▲", x - 3, z - 4, 0xFFA0A0A0, true);
            } else {
                g.fill(x - size - 1, z - size - 1, x + size + 1, z + size + 1, 0xFF000000);
                g.fill(x - size, z - size, x + size, z + size, col);
            }
            if (t == Site.Type.CAPITAL || zoom > 0.15 && t != Site.Type.LAIR)
                g.drawCenteredString(font, s.getString("N"), x, z + size + 2, t == Site.Type.CAPITAL ? 0xFFFFE080 : 0xFFE0D0B0);
            if (Math.abs(mx - x) < 6 && Math.abs(my - z) < 6) {
                hover.add(s.getString("N") + " (" + t.title + ")");
                if (k >= 0) hover.add(Kingdom.of(k).title);
                hover.add(s.getInt("X") + ", " + s.getInt("Z"));
            }
        }
        if (minecraft != null && minecraft.player != null) {
            int px = sx(minecraft.player.getX()), pz = sz(minecraft.player.getZ());
            g.fill(px - 3, pz - 3, px + 3, pz + 3, 0xFFFFFFFF);
            g.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFF2080FF);
            g.drawCenteredString(font, "Sen", px, pz - 12, 0xFFFFFFFF);
        }
        g.disableScissor();
        title(g, "Dünya Haritası — sürükle: kaydır, tekerlek: yakınlaştır", left + 8, top + 8);
        super.render(g, mx, my, pt);
        if (!hover.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String h : hover) lines.add(Component.literal(h));
            g.renderComponentTooltip(font, lines, mx, my);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        cx -= dx / zoom;
        cz -= dy / zoom;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        zoom = Math.max(0.01, Math.min(1.0, zoom * (delta > 0 ? 1.25 : 0.8)));
        return true;
    }
}
