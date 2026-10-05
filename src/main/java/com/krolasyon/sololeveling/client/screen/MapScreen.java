package com.krolasyon.sololeveling.client.screen;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.client.ClientHooks;
import com.krolasyon.sololeveling.client.Holo;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.system.Sys;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** World map: regions with their portals/waypoints, live gates, fast travel. */
public class MapScreen extends Screen {
    private static final ResourceLocation MAP = SoloLeveling.id("textures/gui/world_map.png");
    private static final ResourceLocation ICONS = SoloLeveling.id("textures/gui/map_icons.png");
    private final List<CompoundTag> points = new ArrayList<>(), gates = new ArrayList<>();
    private final String dim;
    private final int px, pz;
    private int ticks;

    public MapScreen(CompoundTag t) {
        super(Component.empty());
        for (Tag x : t.getList("points", Tag.TAG_COMPOUND)) points.add((CompoundTag) x);
        for (Tag x : t.getList("gates", Tag.TAG_COMPOUND)) gates.add((CompoundTag) x);
        dim = t.getString("dim");
        px = t.getInt("px");
        pz = t.getInt("pz");
    }

    @Override
    public void tick() { ticks++; }

    @Override
    public boolean isPauseScreen() { return false; }

    private int[] box() {
        int mw = Math.min(width - 150, (int) ((height - 30) * 1.6F));
        int mh = (int) (mw / 1.6F);
        int x = 10, y = (height - mh) / 2;
        return new int[]{x, y, mw, mh};
    }

    private int[] seoulPos(int[] b, int wx, int wz) {
        // Seoul occupies a circle of radius 0.09 map-width around (0.5, 0.5); the city is about 1300 blocks wide
        float sx = 0.5F + wx / 1300F * 0.18F, sy = 0.5F + wz / 1300F * 0.18F * 1.6F;
        return new int[]{b[0] + (int) (sx * b[2]), b[1] + (int) (sy * b[3])};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int[] b = box();
        Holo.panel(g, b[0] - 4, b[1] - 18, b[2] + 8, b[3] + 22, 1F, Holo.CYAN);
        g.drawString(font, Sys.t("map.title"), b[0] + 2, b[1] - 13, Holo.GOLD, true);
        RenderSystem.enableBlend();
        g.blit(MAP, b[0], b[1], b[2], b[3], 0, 0, 512, 320, 512, 320);
        List<Component> tip = new ArrayList<>();
        // gates in Seoul
        for (CompoundTag gt : gates) {
            if (!gt.getString("dim").equals("sololeveling:seoul")) continue;
            int[] p = seoulPos(b, gt.getInt("x"), gt.getInt("z"));
            int c = gt.getInt("color");
            float pulse = 0.6F + 0.4F * (float) Math.sin((ticks + partial) * 0.3F);
            g.fill(p[0] - 3, p[1] - 3, p[0] + 3, p[1] + 3, Holo.a(c, pulse));
            g.fill(p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1, 0xFFFFFFFF);
            if (Holo.in(mx, my, p[0] - 4, p[1] - 4, 8, 8)) {
                tip.add(Component.literal("[" + gt.getString("rank") + "] ").append(Sys.t("map.gate")).withStyle(s -> s.withColor(c)));
                tip.add(Component.translatable("sololeveling.place." + gt.getString("place")));
                tip.add(Component.translatable("sololeveling.theme." + gt.getString("theme")));
                tip.add(Sys.t("map.click_travel"));
            }
        }
        // waypoints
        for (CompoundTag pt : points) {
            int x = b[0] + (int) (pt.getFloat("x") * b[2]), y = b[1] + (int) (pt.getFloat("y") * b[3]);
            boolean known = pt.getBoolean("known");
            boolean locked = pt.contains("lock");
            int icon = pt.getInt("icon");
            int s = 14;
            RenderSystem.enableBlend();
            if (!known || locked) RenderSystem.setShaderColor(0.45F, 0.45F, 0.5F, 1F);
            g.blit(ICONS, x - s / 2, y - s / 2, s, s, (icon % 8) * 32, (icon / 8) * 32, 32, 32, 256, 64);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (Holo.in(mx, my, x - s / 2, y - s / 2, s, s)) {
                g.fill(x - s / 2 - 1, y - s / 2 - 1, x + s / 2 + 1, y - s / 2, 0xFFFFFFFF);
                tip.add(Sys.t("region." + pt.getString("id")).copy().withStyle(st -> st.withColor(Holo.GOLD)));
                tip.add(Sys.t("region." + pt.getString("id") + ".desc"));
                if (locked) tip.add(Sys.t(pt.getString("lock")).copy().withStyle(st -> st.withColor(Holo.RED)));
                else tip.add(Sys.t("map.click_travel"));
            }
        }
        // player marker
        if (dim.equals("sololeveling:seoul")) {
            int[] p = seoulPos(b, px, pz);
            g.fill(p[0] - 2, p[1] - 2, p[0] + 2, p[1] + 2, 0xFF7CFF9A);
        }
        // side list of gates
        int lx = b[0] + b[2] + 10, lw = width - lx - 8, ly = b[1] - 18;
        Holo.panel(g, lx, ly, lw, b[3] + 22, 1F, 0xFFB347);
        g.drawString(font, Sys.t("news.gates", gates.size()), lx + 6, ly + 5, 0xFFB347, true);
        int gy = ly + 18;
        for (CompoundTag gt : gates) {
            if (gy > ly + b[3]) break;
            int c = gt.getInt("color");
            boolean hover = Holo.in(mx, my, lx + 4, gy, lw - 8, 20);
            g.fill(lx + 4, gy, lx + lw - 4, gy + 20, Holo.a(hover ? 0x22446E : 0x0F2747, 0.8F));
            g.fill(lx + 4, gy, lx + 6, gy + 20, Holo.a(c, 1F));
            g.drawString(font, "[" + gt.getString("rank") + "]" + (gt.getBoolean("red") ? " !" : ""), lx + 9, gy + 2, c, false);
            g.pose().pushPose();
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, Component.translatable("sololeveling.place." + gt.getString("place")), (int) ((lx + 9) / 0.75F), (int) ((gy + 12) / 0.75F), Holo.DIM, false);
            g.pose().popPose();
            gy += 22;
        }
        g.drawString(font, Sys.t("map.hint"), b[0], b[1] + b[3] + 6, 0x5F7FA0, false);
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int[] b = box();
        for (CompoundTag gt : gates) {
            if (!gt.getString("dim").equals("sololeveling:seoul")) continue;
            int[] p = seoulPos(b, gt.getInt("x"), gt.getInt("z"));
            if (Holo.in(mx, my, p[0] - 4, p[1] - 4, 8, 8)) return travel("gate:" + gt.getString("id"));
        }
        for (CompoundTag pt : points) {
            int x = b[0] + (int) (pt.getFloat("x") * b[2]), y = b[1] + (int) (pt.getFloat("y") * b[3]);
            if (Holo.in(mx, my, x - 7, y - 7, 14, 14) && !pt.contains("lock")) return travel(pt.getString("id"));
        }
        int lx = b[0] + b[2] + 10, lw = width - lx - 8, gy = b[1];
        for (CompoundTag gt : gates) {
            if (gy > b[1] + b[3] - 18) break;
            if (Holo.in(mx, my, lx + 4, gy, lw - 8, 20)) return travel("gate:" + gt.getString("id"));
            gy += 22;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean travel(String id) {
        ClientHooks.playClick();
        Net.toServer(new Net.Action("travel", 0, 0, id));
        onClose();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (com.krolasyon.sololeveling.client.ClientSetup.MAP.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
