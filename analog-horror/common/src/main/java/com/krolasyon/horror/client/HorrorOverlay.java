package com.krolasyon.horror.client;

import com.krolasyon.horror.compat.Compat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Analog-tape overlay: vignette, rolling static, scanlines, tracking bands, chroma tear and the jumpscare frame. */
public final class HorrorOverlay {
    private HorrorOverlay() {}

    private static final ResourceLocation[] STATIC = {
            Compat.id("textures/gui/static_0.png"), Compat.id("textures/gui/static_1.png"),
            Compat.id("textures/gui/static_2.png"), Compat.id("textures/gui/static_3.png")};
    private static final ResourceLocation VIGNETTE = Compat.id("textures/gui/vignette.png");
    private static final ResourceLocation JUMPSCARE = Compat.id("textures/gui/jumpscare.png");
    private static final RandomSource RNG = RandomSource.create();

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float in = ClientFx.intensity;
        float flash = ClientFx.flashTicks / 12F;
        boolean scare = ClientFx.jumpscareTicks > 0;
        if (in < 0.02F && flash <= 0 && !scare) return;
        int w = g.guiWidth(), h = g.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (scare) {
            int ox = RNG.nextInt(9) - 4, oy = RNG.nextInt(9) - 4;
            g.setColor(1F, 0.85F, 0.8F, 1F);
            g.blit(JUMPSCARE, ox - 8, oy - 8, w + 16, h + 16, 0, 0, 512, 384, 512, 384);
            flash = Math.max(flash, 0.6F);
        }

        // vignette darkens toward the edges as it gets closer
        g.setColor(1F, 1F, 1F, Math.min(1F, in * 0.9F));
        g.blit(VIGNETTE, 0, 0, w, h, 0, 0, 256, 256, 256, 256);

        // rolling static
        float sa = Math.min(0.55F, Math.max(0F, in - 0.6F) * 0.35F + flash * 0.55F);
        if (sa > 0.01F) {
            g.setColor(1F, 1F, 1F, sa);
            ResourceLocation tex = STATIC[RNG.nextInt(STATIC.length)];
            int u = RNG.nextInt(256), v = RNG.nextInt(128);
            g.blit(tex, 0, 0, w, h, u, v, w / 2, h / 2, 256, 128);
        }
        g.setColor(1F, 1F, 1F, 1F);

        // scanlines
        int sl = (int) (Math.min(1F, in + flash) * 38);
        if (sl > 2) for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, sl << 24);

        // tracking bands and horizontal tears
        long t = mc.level != null ? mc.level.getGameTime() : 0;
        if (in > 0.55F || flash > 0) {
            int band = (int) ((t * 7) % (h + 40)) - 20;
            int ba = (int) (Math.min(1F, Math.max(0F, in - 0.55F) * 1.2F + flash) * 70);
            g.fill(0, band, w, band + 6 + RNG.nextInt(6), (ba << 24) | 0xDDDDDD);
            int tears = (int) (Math.max(0F, in - 0.6F) * 6 + flash * 5);
            for (int i = 0; i < tears; i++) {
                int y = RNG.nextInt(h), x = RNG.nextInt(w / 3);
                g.fill(x, y, x + w / 2 + RNG.nextInt(w / 2), y + 1 + RNG.nextInt(2), (RNG.nextInt(80) + 40) << 24 | (RNG.nextBoolean() ? 0xFF3030 : 0x30C0FF));
            }
        }
        // red flash
        if (flash > 0) g.fill(0, 0, w, h, ((int) (flash * 60) << 24) | 0x400000);
        RenderSystem.disableBlend();
    }
}
