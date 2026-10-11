package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Alien;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Extra geometry attached to the aliens in model space: Goop's UFO, crystal blades, shields, wings, discs, speed blur. */
public final class AlienExtras {
    private AlienExtras() {}

    private static Vec3 v(Vector3f f) { return f == null ? null : new Vec3(f.x, f.y, f.z); }

    static void render(AbstractClientPlayer pl, Alien a, ClientState.Look look, OmxModel m, Pose pose, float[] sp, PoseStack ps, MultiBufferSource buf, int light, float pt) {
        Matrix4f mat = ps.last().pose();
        float age = pl.tickCount + pt;
        Vec3 cam = new Vec3(0, m.maxY * 0.6, 6); // approximate view direction for ribbons in model space
        switch (a) {
            case GOOP -> {
                // anti-gravity projector hovering over the head (the saucer from the cartoon)
                if (look.puddle()) break;
                Vec3 c = new Vec3(Mth.sin(age * 0.07F) * 0.05, m.maxY * pose.scaleY + 0.42 + Mth.sin(age * 0.11F) * 0.05, 0);
                VertexConsumer soft = buf.getBuffer(OmniRenderTypes.SOFT);
                Geo.sphere(soft, mat, c, 0.24, 0.05, 0.24, 0x4A4F5A, 1F, 6, 14);
                Geo.sphere(soft, mat, c.add(0, 0.05, 0), 0.1, 0.07, 0.1, 0x6A6F7A, 1F, 5, 10);
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4 + age * 0.05;
                    Geo.sphere(soft, mat, c.add(Math.cos(ang) * 0.16, -0.02, Math.sin(ang) * 0.16), 0.025, 0.015, 0.025, i % 2 == 0 ? 0xF0D020 : 0x111111, 1F, 3, 6);
                }
                VertexConsumer glow = buf.getBuffer(OmniRenderTypes.GLOW);
                float flick = 0.5F + 0.2F * Mth.sin(age * 0.6F);
                Vec3 top = c.add(0, -0.06, 0), bot = new Vec3(0, m.maxY * pose.scaleY - 0.1, 0);
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3;
                    Vec3 a1 = top.add(Math.cos(ang) * 0.03, 0, Math.sin(ang) * 0.03), a2 = top.add(Math.cos(ang + 1.05) * 0.03, 0, Math.sin(ang + 1.05) * 0.03);
                    Vec3 b1 = bot.add(Math.cos(ang) * 0.22, 0, Math.sin(ang) * 0.22), b2 = bot.add(Math.cos(ang + 1.05) * 0.22, 0, Math.sin(ang + 1.05) * 0.22);
                    Geo.quad(glow, mat, a1, a2, b2, b1, 0x60FF40, 0.12F * flick);
                }
                Geo.sphere(glow, mat, top, 0.04, 0.03, 0.04, 0xFFF080, flick, 3, 6);
            }
            case DIAMONDHEAD -> {
                if (!look.blades()) break;
                VertexConsumer soft = buf.getBuffer(OmniRenderTypes.SOFT);
                for (String f : new String[]{"forearm_l", "forearm_r"}) {
                    Vec3 base = v(AlienRenderer.bonePoint(pl, a, f, 0.45F));
                    Vec3 tip = v(AlienRenderer.bonePoint(pl, a, f, 1.35F));
                    if (base == null || tip == null) continue;
                    Geo.crystal(soft, mat, base, tip, 0.11, 0x9FF5C8, 0.9F, age * 0.02);
                    Geo.crystal(soft, mat, base, base.add(tip.subtract(base).scale(0.6)).add(0.08, 0, 0.05), 0.06, 0xC8FFE0, 0.9F, 1);
                }
            }
            case ULTIMATE_ECHO_ECHO -> {
                if (look.flying()) {
                    VertexConsumer soft = buf.getBuffer(OmniRenderTypes.SOFT);
                    for (int i = 0; i < 2; i++) {
                        Vec3 c = new Vec3(i == 0 ? 0.2 : -0.2, -0.08, 0);
                        Geo.sphere(soft, mat, c, 0.26, 0.05, 0.26, 0x1A1E26, 1F, 4, 14);
                        Geo.sphere(soft, mat, c.add(0, 0.02, 0), 0.13, 0.04, 0.13, 0x18FFF0, 1F, 3, 12);
                    }
                }
            }
            case BIGCHILL -> {
                if (!look.flying()) break;
                // ice moth wings unfold from the shoulders
                VertexConsumer soft = buf.getBuffer(OmniRenderTypes.SOFT);
                float flap = Mth.sin(age * 0.35F) * 0.35F;
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 root = new Vec3(0.12 * s, m.maxY * 0.78, -0.12);
                    Vec3 tipUp = root.add(1.0 * s * Math.cos(flap), 0.65 + Math.sin(flap) * 0.4, -0.35);
                    Vec3 tipOut = root.add(1.25 * s * Math.cos(flap * 0.7), -0.1 + Math.sin(flap) * 0.3, -0.4);
                    Vec3 low = root.add(0.55 * s, -0.95, -0.25);
                    Geo.tri(soft, mat, root, tipUp, tipOut, 0x1E5F8E, 0.85F);
                    Geo.tri(soft, mat, root, tipOut, low, 0x174C72, 0.85F);
                    Geo.tri(soft, mat, root, tipUp.add(tipOut).scale(0.5).add(0, 0.05, 0.01), tipOut, 0x4FC3F0, 0.5F);
                }
            }
            case XLR8 -> {
                if (!look.overdrive()) break;
                VertexConsumer glow = buf.getBuffer(OmniRenderTypes.GLOW);
                RandomSource r = RandomSource.create(pl.getId() * 31L + pl.tickCount / 2);
                for (int i = 0; i < 6; i++) {
                    double y = 0.3 + r.nextDouble() * 1.3, x = (r.nextDouble() - 0.5) * 0.6;
                    List<Vec3> l = new ArrayList<>();
                    l.add(new Vec3(x, y, -0.2));
                    l.add(new Vec3(x, y, -1.4 - r.nextDouble()));
                    Geo.ribbon(glow, mat, cam, l, 0.05F, 0x3FD9F0, 0.5F, true);
                }
            }
            default -> {}
        }
        if (look.blocking) shield(a, m, pose, ps, buf, age);
    }

    private static void shield(Alien a, OmxModel m, Pose pose, PoseStack ps, MultiBufferSource buf, float age) {
        Matrix4f mat = ps.last().pose();
        float h = m.maxY;
        switch (a) {
            case HEATBLAST -> {
                VertexConsumer vc = buf.getBuffer(OmniRenderTypes.sprite(FxRenderer.FIRE_TEX));
                for (int i = 0; i < 9; i++) {
                    double ang = (i - 4) * 0.22;
                    Vec3 c = new Vec3(Math.sin(ang) * 0.9, h * 0.5 + Math.sin(age * 0.5 + i) * 0.1, Math.cos(ang) * 0.9);
                    Geo.plane(vc, mat, c, new Vec3(Math.cos(ang) * 0.35, 0, -Math.sin(ang) * 0.35), new Vec3(0, h * 0.4, 0), 1, 0.8F, 0.4F, 0.9F);
                }
            }
            case DIAMONDHEAD -> {
                VertexConsumer soft = buf.getBuffer(OmniRenderTypes.SOFT);
                for (int i = 0; i < 7; i++) {
                    double ang = (i - 3) * 0.28;
                    Vec3 base = new Vec3(Math.sin(ang) * 0.75, 0.1, Math.cos(ang) * 0.75);
                    Geo.crystal(soft, mat, base, base.add(Math.sin(ang) * 0.15, h * (0.75 + 0.15 * Math.cos(i * 1.7)), 0.15), 0.18, 0x9FF5C8, 0.75F, i);
                }
            }
            case ULTIMATE_ECHO_ECHO -> {
                VertexConsumer glow = buf.getBuffer(OmniRenderTypes.GLOW);
                for (int k = 0; k < 4; k++) {
                    double r = 1.1 + 0.05 * Math.sin(age * 0.4 + k);
                    Geo.ribbon(glow, mat, new Vec3(0, h / 2, 6), Geo.circle(new Vec3(0, h / 2, 0), new Vec3(1, 0, 0), new Vec3(0, Math.cos(k * 0.8), Math.sin(k * 0.8)), r, 40, age * 0.05),
                            0.04F, 0x30FFF0, 0.6F, false);
                }
            }
            case FOURARMS, BIGCHILL, GOOP, XLR8, TERRASPIN, GHOSTFREAK -> {}
        }
    }
}
