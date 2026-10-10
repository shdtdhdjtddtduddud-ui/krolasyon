package com.krolasyon.storm.client;

import com.krolasyon.storm.Fx;
import com.krolasyon.storm.ModRegistry;
import com.krolasyon.storm.StormTree;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Client list of active visual effects, rendered every frame after particles. */
public final class FxRenderer {
    public static final ResourceLocation ORB = new ResourceLocation(StormTree.MODID, "textures/fx/ball_lightning.png");
    public static final ResourceLocation HEX = new ResourceLocation(StormTree.MODID, "textures/fx/shield_hex.png");
    public static final ResourceLocation SKULL = new ResourceLocation(StormTree.MODID, "textures/fx/skull.png");

    private static final class Effect {
        Fx.Type type;
        Vec3 a, b;
        int entA, entB, duration, seed;
        float param;
        long start;
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static int counter;

    private FxRenderer() {}

    public static void add(Fx.Packet p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Effect e = new Effect();
        e.type = Fx.Type.values()[Mth.clamp(p.type(), 0, Fx.Type.values().length - 1)];
        e.a = new Vec3(p.ax(), p.ay(), p.az());
        e.b = new Vec3(p.bx(), p.by(), p.bz());
        e.entA = p.entA();
        e.entB = p.entB();
        e.duration = Math.max(1, p.duration());
        e.param = p.param();
        e.start = mc.level.getGameTime();
        e.seed = ++counter * 7919;
        if (e.type == Fx.Type.DOME || e.type == Fx.Type.RING || e.type == Fx.Type.AURA) {
            EFFECTS.removeIf(o -> o.type == e.type && o.entA == e.entA);
        }
        EFFECTS.add(e);
        if (mc.player != null) {
            double dist = mc.player.position().distanceTo(e.a);
            switch (e.type) {
                case STRIKE -> ClientHooks.shake((float) (0.5 * Math.max(0, 1 - dist / 30)));
                case NOVA -> {
                    ClientHooks.shake((float) (Math.min(1.0, e.param / 8) * Math.max(0, 1 - dist / 40)));
                    if (e.param > 8 && dist < 24) ClientHooks.flash(0.55F);
                }
                case BEAM -> ClientHooks.shake((float) (0.6 * Math.max(0, 1 - dist / 30)));
                default -> {}
            }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { EFFECTS.clear(); return; }
        long now = mc.level.getGameTime();
        EFFECTS.removeIf(e -> now - e.start > e.duration || now < e.start - 40);
        // electric particles for the following effects
        RandomSource r = mc.level.random;
        for (Effect e : EFFECTS) {
            Entity owner = e.entA >= 0 ? mc.level.getEntity(e.entA) : null;
            if (e.type == Fx.Type.AURA && owner != null && r.nextInt(2) == 0) {
                mc.level.addParticle(ModRegistry.SPARK.get(), owner.getX() + (r.nextDouble() - 0.5) * owner.getBbWidth() * 1.6,
                        owner.getY() + r.nextDouble() * owner.getBbHeight(), owner.getZ() + (r.nextDouble() - 0.5) * owner.getBbWidth() * 1.6, 0, 0.05, 0);
            } else if (e.type == Fx.Type.CHARGE && owner != null) {
                for (int i = 0; i < 3; i++) {
                    Vec3 hand = hand(owner, 1F);
                    Vec3 off = new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().scale(2.2);
                    Vec3 from = hand.add(off);
                    mc.level.addParticle(ModRegistry.SPARK.get(), from.x, from.y, from.z, -off.x * 0.18, -off.y * 0.18, -off.z * 0.18);
                }
            } else if (e.type == Fx.Type.DOME && owner != null && r.nextInt(3) == 0) {
                Vec3 d = new Vec3(r.nextDouble() - 0.5, r.nextDouble() * 0.5, r.nextDouble() - 0.5).normalize().scale(e.param);
                mc.level.addParticle(ModRegistry.SPARK.get(), owner.getX() + d.x, owner.getY() + 0.9 + d.y, owner.getZ() + d.z, 0, 0, 0);
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float pt = event.getPartialTick();
        Vector3f lv = camera.getLeftVector(), uv = camera.getUpVector();
        Vec3 right = new Vec3(-lv.x(), -lv.y(), -lv.z()), up = new Vec3(uv.x(), uv.y(), uv.z());
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        long now = mc.level.getGameTime();
        // textured parts first (separate batches), arcs last
        for (Effect e : EFFECTS) {
            float age = now - e.start + pt;
            float t = Mth.clamp(age / e.duration, 0, 1);
            Entity owner = e.entA >= 0 ? mc.level.getEntity(e.entA) : null;
            switch (e.type) {
                case DOME -> { if (owner != null) dome(buf.getBuffer(FxRenderTypes.glow(HEX)), m, owner.getPosition(pt).add(0, 0.9, 0), e, age); }
                case SKULL -> {
                    float a = t < 0.15F ? t / 0.15F : 1 - (t - 0.15F) / 0.85F;
                    float flick = 0.75F + 0.25F * Mth.sin(age * 2.7F);
                    ArcGen.sprite(buf.getBuffer(FxRenderTypes.glow(SKULL)), m, e.a.add(0, t * 1.1, 0), right, up, 0.55F + t * 0.35F, 0.7F, 0.85F, 1F, a * flick);
                }
                case RING -> { if (owner != null) ringNodes(buf.getBuffer(FxRenderTypes.glow(ORB)), m, owner.getPosition(pt), e, age, right, up); }
                case AURA -> {
                    if (owner != null && e.duration > 60 && !(owner == mc.player && mc.options.getCameraType().isFirstPerson())) {
                        float a = Math.min(1, Math.min(t * 10, (1 - t) * 8)) * (0.45F + 0.15F * Mth.sin(age * 0.7F));
                        ArcGen.sprite(buf.getBuffer(FxRenderTypes.glow(ORB)), m, center(owner, pt), right, up, owner.getBbHeight() * 0.85F, 0.6F, 0.75F, 1F, a);
                    }
                }
                case BEAM -> {
                    float fade = t < 0.1F ? t / 0.1F : 1 - (t - 0.1F) / 0.9F;
                    VertexConsumer g = buf.getBuffer(FxRenderTypes.glow(ORB));
                    ArcGen.sprite(g, m, e.a, right, up, 0.7F * fade + 0.2F, 1, 1, 1, fade);
                    ArcGen.sprite(g, m, e.b, right, up, 1.8F * fade, 1, 1, 1, fade);
                }
                case CHARGE -> {
                    if (owner != null) {
                        float s = 0.15F + t * 0.55F * e.param;
                        ArcGen.sprite(buf.getBuffer(FxRenderTypes.glow(ORB)), m, hand(owner, pt), right, up, s, 1, 1, 1, 0.5F + 0.5F * t);
                    }
                }
                default -> {}
            }
        }
        buf.endBatch(FxRenderTypes.glow(HEX));
        buf.endBatch(FxRenderTypes.glow(SKULL));
        buf.endBatch(FxRenderTypes.glow(ORB));

        VertexConsumer vc = buf.getBuffer(FxRenderTypes.ARC);
        for (Effect e : EFFECTS) {
            float age = now - e.start + pt;
            float t = Mth.clamp(age / e.duration, 0, 1);
            long flicker = e.seed + (long) (age / 2);
            Entity owner = e.entA >= 0 ? mc.level.getEntity(e.entA) : null;
            Entity other = e.entB >= 0 ? mc.level.getEntity(e.entB) : null;
            switch (e.type) {
                case ARC -> {
                    Vec3 a = owner != null ? source(owner, pt) : e.a;
                    Vec3 b = other != null ? center(other, pt) : e.b;
                    float alpha = 1 - t * t;
                    ArcGen.arc(vc, m, cam, a, b, flicker, 0.07F * e.param, alpha, 0.22F, 5, 2);
                }
                case TETHER -> {
                    if (owner == null || other == null) break;
                    float alpha = t > 0.85F ? (1 - t) / 0.15F : 1F;
                    Vec3 a = hand(owner, pt), b = center(other, pt);
                    ArcGen.arc(vc, m, cam, a, b, flicker, 0.09F, alpha, 0.16F, 5, 1);
                    ArcGen.arc(vc, m, cam, a, b, flicker * 31 + 7, 0.05F, alpha * 0.7F, 0.25F, 4, 0);
                    // crackle cage around the victim
                    RandomSource r = RandomSource.create(flicker);
                    for (int i = 0; i < 3; i++) {
                        Vec3 p0 = b.add(rnd(r, other.getBbWidth() * 0.9, other.getBbHeight() * 0.6));
                        Vec3 p1 = b.add(rnd(r, other.getBbWidth() * 0.9, other.getBbHeight() * 0.6));
                        ArcGen.arc(vc, m, cam, p0, p1, r.nextLong(), 0.035F, alpha, 0.3F, 3, 0);
                    }
                }
                case STRIKE -> strike(vc, m, cam, e, age, t, flicker);
                case BEAM -> beam(vc, m, cam, e, owner, pt, age, t, flicker, right, up);
                case NOVA -> nova(vc, m, cam, e.a, e.param, t, flicker, 1F);
                case BURST -> burst(vc, m, cam, e, t, flicker);
                case DOME -> { if (owner != null) domeArcs(vc, m, cam, owner.getPosition(pt).add(0, 0.9, 0), e, age, flicker); }
                case RING -> { if (owner != null) ring(vc, m, cam, owner.getPosition(pt), e, age, flicker); }
                case AURA -> { if (owner != null) aura(vc, m, cam, owner, pt, e, t, flicker); }
                case CHARGE -> {
                    if (owner == null) break;
                    Vec3 h = hand(owner, pt);
                    RandomSource r = RandomSource.create(flicker);
                    double rad = 2.6 * (1 - t) + 0.3;
                    for (int i = 0; i < 5; i++) {
                        Vec3 from = h.add(new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().scale(rad));
                        ArcGen.arc(vc, m, cam, from, h, r.nextLong(), 0.04F, 0.4F + 0.6F * t, 0.25F, 4, 0);
                    }
                }
                default -> {}
            }
        }
        buf.endBatch(FxRenderTypes.ARC);
        ps.popPose();
    }

    // ------------------------------------------------------------------ individual effects

    private static void strike(VertexConsumer vc, Matrix4f m, Vec3 cam, Effect e, float age, float t, long flicker) {
        RandomSource r = RandomSource.create(e.seed);
        Vec3 top = e.a.add((r.nextDouble() - 0.5) * 6, 26, (r.nextDouble() - 0.5) * 6);
        float a = t < 0.5F ? 1F : 1 - (t - 0.5F) * 2;
        // flash flicker: visible, dim, visible
        if (age > 2 && age < 3.5F) a *= 0.35F;
        ArcGen.arc(vc, m, cam, top, e.a, flicker, 0.22F * e.param, a, 0.12F, 6, 4);
        // ground creepers
        RandomSource g = RandomSource.create(flicker * 13);
        for (int i = 0; i < 7; i++) {
            double ang = g.nextDouble() * Math.PI * 2, len = 1.2 + g.nextDouble() * 2.5 * e.param;
            Vec3 end = e.a.add(Math.cos(ang) * len, 0.05, Math.sin(ang) * len);
            ArcGen.arc(vc, m, cam, e.a.add(0, 0.05, 0), end, g.nextLong(), 0.05F, a, 0.25F, 4, 0);
        }
        nova(vc, m, cam, e.a.add(0, 0.06, 0), 3.2F * e.param, t, flicker, a);
    }

    private static void beam(VertexConsumer vc, Matrix4f m, Vec3 cam, Effect e, Entity owner, float pt, float age, float t, long flicker, Vec3 right, Vec3 up) {
        Vec3 a = e.a, b = e.b;
        float fade = t < 0.1F ? t / 0.1F : 1 - (t - 0.1F) / 0.9F;
        float thick = 0.42F * (1 - t * 0.7F);
        List<Vec3> line = List.of(a, b);
        ArcGen.ribbon(vc, m, cam, line, thick * 6F, ArcGen.GLOW, fade * 0.25F);
        ArcGen.ribbon(vc, m, cam, line, thick * 2.6F, ArcGen.MID, fade * 0.6F);
        ArcGen.ribbon(vc, m, cam, line, thick, ArcGen.CORE, fade);
        // spiral arcs wound around the beam
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        Vec3 n = dir.normalize();
        Vec3 u = ArcGen.perp(n, RandomSource.create(1)), w = n.cross(u);
        for (int s = 0; s < 3; s++) {
            List<Vec3> spiral = new ArrayList<>();
            int steps = (int) Math.max(8, len * 3);
            for (int i = 0; i <= steps; i++) {
                double f = (double) i / steps;
                double ang = f * len * 1.6 + s * Math.PI * 2 / 3 + age * 0.9;
                double rad = 0.55 + 0.15 * Math.sin(f * 40 + age);
                spiral.add(a.add(dir.scale(f)).add(u.scale(Math.cos(ang) * rad)).add(w.scale(Math.sin(ang) * rad)));
            }
            ArcGen.layers(vc, m, cam, spiral, 0.05F, fade * 0.8F);
        }
        // shock rings travelling down the beam
        for (int k = 0; k < 4; k++) {
            double f = ((age * 0.09 + k * 0.25) % 1.0);
            Vec3 c = a.add(dir.scale(f));
            List<Vec3> ring = new ArrayList<>();
            for (int i = 0; i <= 16; i++) {
                double ang = i * Math.PI * 2 / 16;
                ring.add(c.add(u.scale(Math.cos(ang) * 0.9)).add(w.scale(Math.sin(ang) * 0.9)));
            }
            ArcGen.ribbon(vc, m, cam, ring, 0.12F, ArcGen.MID, fade * 0.5F * (float) (1 - f));
        }
        // side arcs
        RandomSource r = RandomSource.create(flicker);
        for (int i = 0; i < 6; i++) {
            Vec3 s = a.add(dir.scale(r.nextDouble()));
            Vec3 end = s.add(ArcGen.perp(n, r).scale(1 + r.nextDouble() * 2)).add(n.scale(r.nextDouble() * 2));
            ArcGen.arc(vc, m, cam, s, end, r.nextLong(), 0.04F, fade, 0.3F, 3, 0);
        }
    }

    private static void nova(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 c, float radius, float t, long flicker, float alphaMul) {
        float ease = 1 - (1 - t) * (1 - t) * (1 - t);
        float a = (1 - t) * alphaMul;
        for (int ringIdx = 0; ringIdx < 2; ringIdx++) {
            float rr = radius * Math.max(0, ease - ringIdx * 0.18F);
            if (rr < 0.2F) continue;
            RandomSource r = RandomSource.create(flicker + ringIdx * 101);
            int n = Math.max(12, (int) (rr * 5));
            List<Vec3> pts = new ArrayList<>();
            for (int i = 0; i <= n; i++) {
                double ang = i * Math.PI * 2 / n;
                double j = i == n ? 0 : (r.nextDouble() - 0.5) * 0.35;
                pts.add(c.add(Math.cos(ang) * (rr + j), 0.1 + Math.abs(j) * 0.6, Math.sin(ang) * (rr + j)));
            }
            pts.set(n, pts.get(0));
            ArcGen.layers(vc, m, cam, pts, 0.09F * (1 - ringIdx * 0.4F), a);
            // flat shockwave band
            List<Vec3> band = new ArrayList<>();
            for (int i = 0; i <= 48; i++) {
                double ang = i * Math.PI * 2 / 48;
                band.add(c.add(Math.cos(ang) * rr, 0.05, Math.sin(ang) * rr));
            }
            ArcGen.ribbon(vc, m, cam, band, 0.6F + rr * 0.08F, ArcGen.GLOW, a * 0.25F);
        }
        // radial spokes
        RandomSource r = RandomSource.create(flicker * 7);
        float rr = radius * ease;
        for (int i = 0; i < 6; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            Vec3 from = c.add(Math.cos(ang) * rr * 0.2, 0.1, Math.sin(ang) * rr * 0.2);
            Vec3 to = c.add(Math.cos(ang) * rr, 0.15, Math.sin(ang) * rr);
            ArcGen.arc(vc, m, cam, from, to, r.nextLong(), 0.05F, a * 0.8F, 0.18F, 4, 1);
        }
    }

    private static void burst(VertexConsumer vc, Matrix4f m, Vec3 cam, Effect e, float t, long flicker) {
        float ease = 1 - (1 - t) * (1 - t);
        float a = 1 - t;
        RandomSource dirs = RandomSource.create(e.seed);
        int n = Mth.clamp((int) (e.param * 2.5F), 6, 18);
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(dirs.nextDouble() - 0.5, (dirs.nextDouble() - 0.35) * 0.8, dirs.nextDouble() - 0.5).normalize();
            Vec3 end = e.a.add(d.scale(e.param * ease));
            Vec3 start = e.a.add(d.scale(e.param * ease * 0.15));
            ArcGen.arc(vc, m, cam, start, end, flicker * 17 + i, 0.06F, a, 0.2F, 4, 1);
        }
    }

    private static void dome(VertexConsumer vc, Matrix4f m, Vec3 c, Effect e, float age) {
        float in = Mth.clamp(age / 6F, 0, 1);
        float out = Mth.clamp((e.duration - age) / 10F, 0, 1);
        float pop = in < 1 ? in * (1.15F - 0.15F * in) : 1F;
        float r = e.param * pop;
        float pulse = 0.55F + 0.15F * Mth.sin(age * 0.4F);
        float alpha = pulse * Math.min(in, out);
        int lat = 12, lon = 24;
        float scroll = age * 0.01F;
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                float u0 = (float) j / lon * 4 + scroll, u1 = (float) (j + 1) / lon * 4 + scroll;
                float v0 = (float) i / lat * 2 - scroll, v1 = (float) (i + 1) / lat * 2 - scroll;
                // fresnel-ish: brighter near the silhouette is approximated by brighter near the equator band
                float a = alpha * (0.6F + 0.4F * (float) Math.abs(Math.cos(t0)));
                ArcGen.t(vc, m, sph(c, r, t0, p0), 0.6F, 0.8F, 1F, a, u0, v0);
                ArcGen.t(vc, m, sph(c, r, t0, p1), 0.6F, 0.8F, 1F, a, u1, v0);
                ArcGen.t(vc, m, sph(c, r, t1, p1), 0.6F, 0.8F, 1F, a, u1, v1);
                ArcGen.t(vc, m, sph(c, r, t1, p0), 0.6F, 0.8F, 1F, a, u0, v1);
            }
        }
    }

    private static Vec3 sph(Vec3 c, double r, double theta, double phi) {
        return c.add(r * Math.sin(theta) * Math.cos(phi), r * Math.cos(theta) * 0.92, r * Math.sin(theta) * Math.sin(phi));
    }

    private static void domeArcs(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 c, Effect e, float age, long flicker) {
        float out = Mth.clamp((e.duration - age) / 10F, 0, 1);
        float in = Mth.clamp(age / 6F, 0, 1);
        RandomSource r = RandomSource.create(flicker);
        for (int i = 0; i < 4; i++) {
            double th = r.nextDouble() * Math.PI, ph = r.nextDouble() * Math.PI * 2;
            Vec3 p0 = sph(c, e.param * 1.01, th, ph);
            Vec3 p1 = sph(c, e.param * 1.01, Mth.clamp(th + (r.nextDouble() - 0.5) * 1.2, 0.05, Math.PI - 0.05), ph + (r.nextDouble() - 0.5) * 1.2);
            ArcGen.arc(vc, m, cam, p0, p1, r.nextLong(), 0.04F, in * out, 0.2F, 4, 0);
        }
        // bright rim at ground level on spawn
        if (age < 10) nova(vc, m, cam, c.add(0, -0.85, 0), e.param * 1.4F, age / 10F, flicker, 1F);
    }

    private static void ring(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 pos, Effect e, float age, long flicker) {
        float in = Mth.clamp(age / 8F, 0, 1);
        float out = Mth.clamp((e.duration - age) / 10F, 0, 1);
        float a = in * out;
        float r = e.param * (0.4F + 0.6F * in);
        float tilt = 0.18F * Mth.sin(age * 0.07F);
        RandomSource rnd = RandomSource.create(flicker);
        for (int layer = 0; layer < 2; layer++) {
            List<Vec3> pts = new ArrayList<>();
            int n = 36;
            for (int i = 0; i <= n; i++) {
                double ang = i * Math.PI * 2 / n + age * 0.12 * (layer == 0 ? 1 : -1.3);
                double j = i == n ? 0 : (rnd.nextDouble() - 0.5) * 0.25;
                double y = 1.0 + Math.sin(ang) * tilt * r + j * 0.5 + layer * 0.12;
                pts.add(pos.add(Math.cos(ang) * (r + j), y, Math.sin(ang) * (r + j)));
            }
            pts.set(n, pts.get(0));
            ArcGen.layers(vc, m, cam, pts, layer == 0 ? 0.07F : 0.04F, a);
        }
    }

    private static void ringNodes(VertexConsumer vc, Matrix4f m, Vec3 pos, Effect e, float age, Vec3 right, Vec3 up) {
        float in = Mth.clamp(age / 8F, 0, 1);
        float out = Mth.clamp((e.duration - age) / 10F, 0, 1);
        float r = e.param * (0.4F + 0.6F * in);
        float tilt = 0.18F * Mth.sin(age * 0.07F);
        for (int i = 0; i < 3; i++) {
            double ang = age * 0.12 + i * Math.PI * 2 / 3;
            Vec3 p = pos.add(Math.cos(ang) * r, 1.0 + Math.sin(ang) * tilt * r, Math.sin(ang) * r);
            ArcGen.sprite(vc, m, p, right, up, 0.42F + 0.06F * Mth.sin(age * 0.8F + i), 1, 1, 1, in * out);
        }
    }

    private static void aura(VertexConsumer vc, Matrix4f m, Vec3 cam, Entity owner, float pt, Effect e, float t, long flicker) {
        float a = Math.min(1, Math.min(t * 10, (1 - t) * 8));
        Vec3 base = owner.getPosition(pt);
        float w = owner.getBbWidth() * 0.75F, h = owner.getBbHeight();
        RandomSource r = RandomSource.create(flicker);
        boolean big = e.duration > 60;
        int n = big ? 8 : 3;
        if (big) {
            // two arcs spiralling up around the body
            for (int k = 0; k < 2; k++) {
                List<Vec3> sp = new ArrayList<>();
                for (int i = 0; i <= 14; i++) {
                    double f = i / 14.0, ang = f * Math.PI * 3 + e.seed + k * Math.PI + (flicker - e.seed) * 0.9;
                    sp.add(base.add(Math.cos(ang) * w * 1.2, f * h * 1.05, Math.sin(ang) * w * 1.2));
                }
                ArcGen.layers(vc, m, cam, sp, 0.03F, a * 0.8F);
            }
        }
        for (int i = 0; i < n; i++) {
            Vec3 p0 = base.add((r.nextDouble() - 0.5) * w * 2, r.nextDouble() * h, (r.nextDouble() - 0.5) * w * 2);
            Vec3 p1 = p0.add((r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 1.1, (r.nextDouble() - 0.5) * 0.9);
            ArcGen.arc(vc, m, cam, p0, p1, r.nextLong(), 0.035F, a, 0.35F, 3, 0);
        }
        // occasional arc to the ground
        if (r.nextInt(3) == 0) {
            Vec3 p0 = base.add(0, h * 0.5, 0);
            Vec3 p1 = base.add((r.nextDouble() - 0.5) * 3, 0.02, (r.nextDouble() - 0.5) * 3);
            ArcGen.arc(vc, m, cam, p0, p1, r.nextLong(), 0.04F, a * 0.8F, 0.25F, 4, 0);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Vec3 rnd(RandomSource r, double w, double h) {
        return new Vec3((r.nextDouble() - 0.5) * w * 2, (r.nextDouble() - 0.5) * h * 2, (r.nextDouble() - 0.5) * w * 2);
    }

    public static Vec3 center(Entity e, float pt) { return e.getPosition(pt).add(0, e.getBbHeight() * 0.55, 0); }

    /** players cast from the hand, everything else from its centre */
    public static Vec3 source(Entity e, float pt) {
        return e instanceof net.minecraft.world.entity.player.Player ? hand(e, pt) : center(e, pt);
    }

    public static Vec3 hand(Entity e, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (e == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Camera c = mc.gameRenderer.getMainCamera();
            Vector3f l = c.getLeftVector(), u = c.getUpVector(), f = c.getLookVector();
            // a bit in front of the visible hand so effects don't fill the screen
            return c.getPosition().add(f.x() * 1.5 - l.x() * 0.55 - u.x() * 0.4, f.y() * 1.5 - l.y() * 0.55 - u.y() * 0.4, f.z() * 1.5 - l.z() * 0.55 - u.z() * 0.4);
        }
        float yaw = (e instanceof LivingEntity le ? Mth.lerp(pt, le.yBodyRotO, le.yBodyRot) : e.getYRot()) * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(0.38);
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(0.55);
        return e.getPosition(pt).add(0, e.getBbHeight() * 0.72, 0).add(side).add(fwd);
    }
}
