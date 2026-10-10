package com.krolasyon.voidtree.client;

import com.krolasyon.voidtree.Fx;
import com.krolasyon.voidtree.ModRegistry;
import com.krolasyon.voidtree.VoidTree;
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
import java.util.List;

/** Client list of active void effects. Rendered in three passes: textured sprites, dark smoke, additive glow. */
public final class FxRenderer {
    public static final ResourceLocation HOLE = tex("black_hole"), RIFT = tex("rift"), STARFIELD = tex("starfield"),
            VORTEX = tex("vortex"), SLASH = tex("slash"), STAR = tex("star");

    private static ResourceLocation tex(String n) { return new ResourceLocation(VoidTree.MODID, "textures/fx/" + n + ".png"); }

    private static final class Effect {
        Fx.Type type;
        Vec3 a, b;
        int entA, entB, duration, seed;
        float param;
        long start;
    }

    /** per frame render context */
    private record Ctx(Matrix4f m, Vec3 cam, Vec3 right, Vec3 up, float pt, MultiBufferSource.BufferSource buf) {}

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
        if (e.type == Fx.Type.PHANTOM || e.type == Fx.Type.ASCEND) {
            EFFECTS.removeIf(o -> o.type == e.type && o.entA == e.entA && o.duration > 20);
        }
        EFFECTS.add(e);
        if (mc.player != null) {
            double dist = mc.player.position().distanceTo(e.a);
            switch (e.type) {
                case IMPLODE -> {
                    ClientHooks.shake((float) (Math.min(1.0, e.param / 8) * Math.max(0, 1 - dist / 40)));
                    if (e.param >= 7 && dist < 26) ClientHooks.flash(0.5F);
                }
                case HOLE -> ClientHooks.shake((float) (0.3 * Math.max(0, 1 - dist / 30)));
                case COSMIC -> ClientHooks.shake((float) (0.25 * Math.max(0, 1 - dist / 40)));
                default -> {}
            }
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { EFFECTS.clear(); return; }
        long now = mc.level.getGameTime();
        EFFECTS.removeIf(e -> now - e.start > e.duration || now < e.start - 40);
        RandomSource r = mc.level.random;
        for (Effect e : EFFECTS) {
            Entity owner = e.entA >= 0 ? mc.level.getEntity(e.entA) : null;
            float t = (float) (now - e.start) / e.duration;
            switch (e.type) {
                case ASCEND -> {
                    if (owner != null && r.nextInt(2) == 0) mc.level.addParticle(ModRegistry.STAR.get(), owner.getX() + (r.nextDouble() - 0.5),
                            owner.getY() + r.nextDouble() * 0.5, owner.getZ() + (r.nextDouble() - 0.5), 0, -0.05, 0);
                }
                case PHANTOM -> {
                    if (owner != null && r.nextInt(3) == 0) mc.level.addParticle(ModRegistry.WISP.get(), owner.getX() + (r.nextDouble() - 0.5) * 0.8,
                            owner.getY() + r.nextDouble() * owner.getBbHeight(), owner.getZ() + (r.nextDouble() - 0.5) * 0.8, 0, 0.03, 0);
                }
                case CHARGE -> {
                    if (owner == null) break;
                    Vec3 hand = hand(owner, 1F);
                    for (int i = 0; i < 3; i++) {
                        Vec3 off = new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().scale(2.2);
                        Vec3 from = hand.add(off);
                        mc.level.addParticle(ModRegistry.WISP.get(), from.x, from.y, from.z, -off.x * 0.16, -off.y * 0.16, -off.z * 0.16);
                    }
                }
                case HOLE, VORTEX -> {
                    double rad = e.param;
                    for (int i = 0; i < (e.type == Fx.Type.HOLE ? 4 : 2); i++) {
                        double ang = r.nextDouble() * Math.PI * 2;
                        Vec3 from = e.a.add(Math.cos(ang) * rad, (r.nextDouble() - 0.3) * (e.type == Fx.Type.HOLE ? rad * 0.6 : 0.6), Math.sin(ang) * rad);
                        Vec3 v = e.a.subtract(from).scale(0.06);
                        mc.level.addParticle(ModRegistry.WISP.get(), from.x, from.y, from.z, v.x - Math.sin(ang) * 0.15, v.y, v.z + Math.cos(ang) * 0.15);
                    }
                }
                case COSMIC -> {
                    if (t < 0.9F) for (int i = 0; i < 3; i++) mc.level.addParticle(ModRegistry.STAR.get(), e.a.x + (r.nextDouble() - 0.5) * e.param * 2,
                            e.a.y + 6 + r.nextDouble() * 8, e.a.z + (r.nextDouble() - 0.5) * e.param * 2, 0, -0.2, 0);
                }
                default -> {}
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Vector3f lv = camera.getLeftVector(), uv = camera.getUpVector();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Ctx c = new Ctx(ps.last().pose(), cam, new Vec3(-lv.x(), -lv.y(), -lv.z()), new Vec3(uv.x(), uv.y(), uv.z()), event.getPartialTick(), buf);
        for (int pass = 0; pass < 3; pass++) {
            for (Effect e : EFFECTS) draw(c, e, pass, mc);
            if (pass == 0) buf.endBatch();
            else buf.endBatch(pass == 1 ? FxRenderTypes.DARK : FxRenderTypes.ARC);
        }
        ps.popPose();
    }

    private static VertexConsumer tex(Ctx c, ResourceLocation t) { return c.buf.getBuffer(FxRenderTypes.glow(t)); }

    /** pass 0 = textured sprites, 1 = dark smoke, 2 = additive glow lines */
    private static void draw(Ctx c, Effect e, int pass, Minecraft mc) {
        long now = mc.level.getGameTime();
        float age = now - e.start + c.pt;
        float t = Mth.clamp(age / e.duration, 0, 1);
        float fadeIO = Math.min(1F, Math.min(age / 6F, (e.duration - age) / 10F));
        if (fadeIO <= 0) return;
        long flick = e.seed + (long) (age / 2);
        Entity owner = e.entA >= 0 ? mc.level.getEntity(e.entA) : null;
        Entity other = e.entB >= 0 ? mc.level.getEntity(e.entB) : null;
        VertexConsumer dark = pass == 1 ? c.buf.getBuffer(FxRenderTypes.DARK) : null;
        VertexConsumer glow = pass == 2 ? c.buf.getBuffer(FxRenderTypes.ARC) : null;
        Matrix4f m = c.m;
        switch (e.type) {
            case TRAIL -> {
                if (pass == 0) return;
                float a = (1 - t) * (1 - t);
                for (int k = 0; k < 4; k++) {
                    double h = 0.25 + k * 0.45;
                    List<Vec3> pts = ArcGen.wave(e.a.add(0, h, 0), e.b.add(0, h, 0), 0.35 * e.param, 2.5, k * 1.7 + age * 0.4, 24);
                    ArcGen.shadowLayers(glow, dark, m, c.cam, pts, 0.05F * e.param, a);
                }
            }
            case PHANTOM -> {
                if (owner == null || pass == 0) return;
                Vec3 base = owner.getPosition(c.pt);
                float h = owner.getBbHeight(), w = owner.getBbWidth();
                float a = fadeIO * 0.7F;
                for (int k = 0; k < 5; k++) {
                    double ang = k * 1.256 + age * 0.12;
                    Vec3 p0 = base.add(Math.cos(ang) * w * 0.8, 0.1, Math.sin(ang) * w * 0.8);
                    Vec3 p1 = p0.add(Math.cos(ang + 1) * 0.3, h * 0.9 + Math.sin(age * 0.2 + k) * 0.3, Math.sin(ang + 1) * 0.3);
                    List<Vec3> pts = ArcGen.wave(p0, p1, 0.15, 1.5, age * 0.3 + k, 12);
                    if (pass == 1) ArcGen.ribbon(dark, m, c.cam, pts, 0.25F, ArcGen.SHADOW, a * 0.35F);
                    else ArcGen.ribbon(glow, m, c.cam, pts, 0.06F, ArcGen.MID, a * 0.5F);
                }
            }
            case VORTEX -> {
                float r = e.param * Math.min(1, age / 8F);
                if (pass == 0) flatSprite(tex(c, VORTEX), m, e.a.add(0, 0.08, 0), r * 1.15F, -age * 0.18F, fadeIO);
                else if (pass == 1) flatDisc(dark, m, e.a.add(0, 0.06, 0), r * 0.35F, ArcGen.SHADOW, fadeIO * 0.8F);
                else spiralArms(glow, m, c.cam, e.a.add(0, 0.15, 0), r, 6, age * 0.15F, 0, fadeIO);
            }
            case TWISTER -> {
                Vec3 base = e.a.add(e.b.subtract(e.a).scale(t));
                if (pass == 0) { flatSprite(tex(c, VORTEX), m, base.add(0, 0.08, 0), e.param * 1.1F, -age * 0.3F, fadeIO); return; }
                for (int k = 0; k < 11; k++) {
                    double hgt = k * 0.45;
                    double rad = 0.35 + hgt * 0.42 + 0.1 * Math.sin(age * 0.3 + k);
                    Vec3 cc = base.add(Math.sin(age * 0.15 + k * 0.5) * 0.25 * hgt * 0.3, hgt, Math.cos(age * 0.13 + k * 0.5) * 0.25 * hgt * 0.3);
                    List<Vec3> ring = new ArrayList<>();
                    for (int i = 0; i <= 18; i++) {
                        double ang = i * Math.PI * 2 / 18 + age * (0.5 - k * 0.02);
                        double wob = 1 + 0.12 * Math.sin(ang * 3 + age);
                        ring.add(cc.add(Math.cos(ang) * rad * wob, Math.sin(ang * 2 + age) * 0.08, Math.sin(ang) * rad * wob));
                    }
                    if (pass == 1) ArcGen.ribbon(dark, m, c.cam, ring, 0.32F, ArcGen.DUSK, fadeIO * 0.35F);
                    else ArcGen.ribbon(glow, m, c.cam, ring, 0.07F, k % 3 == 0 ? ArcGen.CORE : ArcGen.MID, fadeIO * 0.75F);
                }
            }
            case SLASH -> {
                if (pass == 0 || owner == null) return;
                Vec3 eye = owner.getEyePosition(c.pt);
                Vec3 look = owner.getViewVector(c.pt);
                Vec3 side = look.cross(new Vec3(0, 1, 0));
                side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
                Vec3 upv = side.cross(look).normalize();
                double roll = e.param == 2 ? 0.0 : e.param > 0 ? 0.75 : -0.75;
                Vec3 u = side.scale(Math.cos(roll)).add(upv.scale(Math.sin(roll)));
                Vec3 center = eye.add(look.scale(1.2)).add(0, -0.25, 0);
                double rad = e.param == 2 ? 2.4 : 1.8;
                float sweep = Mth.clamp(age / 3.5F, 0, 1);
                float a = 1 - Mth.clamp((age - 3) / 4F, 0, 1);
                List<Vec3> arc = new ArrayList<>();
                int n = 20;
                for (int i = 0; i <= n * sweep; i++) {
                    double f = (double) i / n;
                    double ang = (f - 0.5) * Math.PI * 1.1 * (e.param < 0 ? -1 : 1);
                    arc.add(center.add(u.scale(Math.sin(ang) * rad)).add(look.scale(Math.cos(ang) * rad * 0.6 - rad * 0.3)));
                }
                if (arc.size() < 2) return;
                float wdt = e.param == 2 ? 0.16F : 0.11F;
                if (pass == 1) ArcGen.taper(dark, m, c.cam, arc, wdt * 4, ArcGen.SHADOW, a * 0.5F);
                else {
                    ArcGen.taper(glow, m, c.cam, arc, wdt * 3.5F, ArcGen.GLOW, a * 0.4F);
                    ArcGen.taper(glow, m, c.cam, arc, wdt * 1.5F, ArcGen.MID, a * 0.8F);
                    ArcGen.taper(glow, m, c.cam, arc, wdt * 0.5F, ArcGen.CORE, a);
                }
            }
            case PRISON -> {
                if (other == null) return;
                Vec3 cc = other.getPosition(c.pt).add(0, other.getBbHeight() / 2, 0);
                float r = Math.max(other.getBbWidth(), other.getBbHeight()) * 0.7F + 0.4F;
                float pop = Math.min(1, age / 5F);
                if (pass == 0) { sphere(tex(c, STARFIELD), m, cc, r * pop, age * 0.01F, fadeIO * 0.9F); return; }
                if (pass == 1) return;
                for (int k = 0; k < 3; k++) {
                    double tilt = k * 1.05 + age * 0.05;
                    List<Vec3> ring = new ArrayList<>();
                    for (int i = 0; i <= 28; i++) {
                        double ang = i * Math.PI * 2 / 28 + age * 0.1 * (k + 1);
                        Vec3 p = new Vec3(Math.cos(ang) * r * 1.08, 0, Math.sin(ang) * r * 1.08);
                        double y = p.z * Math.sin(tilt), z = p.z * Math.cos(tilt);
                        ring.add(cc.add(p.x, y, z));
                    }
                    ArcGen.layers(glow, m, c.cam, ring, 0.035F, fadeIO * pop);
                }
            }
            case BLADE -> {
                float f = Mth.clamp(age / 5F, 0, 1);
                Vec3 head = e.a.add(e.b.subtract(e.a).scale(f));
                Vec3 back = e.a.subtract(e.b).normalize().scale(e.param == 2 ? 6 : 3.2);
                float a = age < 5 ? 1F : 1 - Mth.clamp((age - 5) / 4F, 0, 1);
                List<Vec3> line = List.of(head, head.add(back));
                if (pass == 0) {
                    if (e.param == 2) ArcGen.sprite(tex(c, STAR), m, head, c.right, c.up, 0.9F, 1, 1, 1, a);
                    else if (age >= 5) ArcGen.sprite(tex(c, STAR), m, e.b.add(0, 0.2, 0), c.right, c.up, 0.9F * a, 1, 1, 1, a);
                } else if (pass == 1) {
                    if (e.param != 2) ArcGen.taper(dark, m, c.cam, line, 0.35F, ArcGen.SHADOW, a * 0.8F);
                } else {
                    ArcGen.taper(glow, m, c.cam, line, e.param == 2 ? 0.6F : 0.3F, ArcGen.GLOW, a * 0.5F);
                    ArcGen.taper(glow, m, c.cam, line, e.param == 2 ? 0.25F : 0.1F, ArcGen.CORE, a);
                }
            }
            case HOLE -> {
                float grow = Math.min(1, age / 10F) * Mth.clamp((e.duration - age) / 6F, 0, 1);
                float pulse = 1 + 0.08F * Mth.sin(age * 0.5F);
                if (pass == 0) {
                    rotSprite(tex(c, HOLE), m, e.a, c.right, c.up, 2.4F * grow * pulse, age * 0.12F, 1F);
                    flatSprite(tex(c, VORTEX), m, e.a.add(0, -0.02, 0), 3.8F * grow, -age * 0.1F, 0.8F * grow);
                } else if (pass == 1) {
                    ArcGen.glowQuad(dark, m, e.a, c.right, c.up, 0.75F * grow * pulse, ArcGen.SHADOW, 0.97F * grow);
                    ArcGen.glowQuad(dark, m, e.a, c.right, c.up, 1.05F * grow * pulse, ArcGen.SHADOW, 0.5F * grow);
                } else {
                    spiralArms(glow, m, c.cam, e.a, e.param * 0.55F * grow, 8, age * 0.22F, 0.6F, grow);
                    // event horizon ring
                    List<Vec3> ring = new ArrayList<>();
                    for (int i = 0; i <= 32; i++) {
                        double ang = i * Math.PI * 2 / 32;
                        ring.add(e.a.add(c.right.scale(Math.cos(ang) * 0.95 * grow * pulse)).add(c.up.scale(Math.sin(ang) * 0.95 * grow * pulse)));
                    }
                    ArcGen.ribbon(glow, m, c.cam, ring, 0.09F, ArcGen.CORE, grow);
                    ArcGen.ribbon(glow, m, c.cam, ring, 0.3F, ArcGen.MID, grow * 0.5F);
                }
            }
            case IMPLODE -> {
                float a = 1 - t;
                if (pass == 0) {
                    float s = t < 0.3F ? t / 0.3F * e.param * 0.35F : e.param * 0.35F * (1 - (t - 0.3F) / 0.7F);
                    rotSprite(tex(c, STAR), m, e.a, c.right, c.up, s * 1.6F, age * 0.2F, 1F);
                    return;
                }
                if (pass == 1) return;
                float rin = e.param * (1 - Math.min(1, t * 2.5F));
                if (rin > 0.2F) ArcGen.layers(glow, m, c.cam, flatRing(e.a, rin, 40, flick, 0.15), 0.1F, 1F);
                if (t > 0.35F) {
                    float ro = e.param * 1.2F * (t - 0.35F) / 0.65F;
                    ArcGen.layers(glow, m, c.cam, flatRing(e.a.add(0, -0.5, 0), ro, 48, flick, 0.25), 0.12F, a);
                    RandomSource r = RandomSource.create(e.seed);
                    for (int i = 0; i < 12; i++) {
                        Vec3 d = new Vec3(r.nextDouble() - 0.5, (r.nextDouble() - 0.3) * 0.7, r.nextDouble() - 0.5).normalize();
                        ArcGen.shadowLayers(glow, null, m, c.cam, ArcGen.wave(e.a.add(d.scale(ro * 0.2)), e.a.add(d.scale(ro)), 0.4, 1.5, i + age * 0.3, 14), 0.05F, a);
                    }
                }
            }
            case RIFT -> {
                float open = Math.min(1, age / 4F) * Mth.clamp((e.duration - age) / 6F, 0, 1);
                Vec3 flatRight = new Vec3(c.right.x, 0, c.right.z);
                flatRight = flatRight.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : flatRight.normalize();
                Vec3 vert = new Vec3(0, 1, 0);
                float h = 1.25F * e.param;
                if (pass == 0) {
                    VertexConsumer vc = tex(c, RIFT);
                    Vec3 rr = flatRight.scale(h * 0.7F * open), uu = vert.scale(h);
                    ArcGen.t(vc, m, e.a.subtract(rr).subtract(uu), 1, 1, 1, open, 0, 1);
                    ArcGen.t(vc, m, e.a.add(rr).subtract(uu), 1, 1, 1, open, 1, 1);
                    ArcGen.t(vc, m, e.a.add(rr).add(uu), 1, 1, 1, open, 1, 0);
                    ArcGen.t(vc, m, e.a.subtract(rr).add(uu), 1, 1, 1, open, 0, 0);
                } else if (pass == 1) {
                    ArcGen.taper(dark, m, c.cam, List.of(e.a, e.a.add(0, h * 0.8, 0)), 0.3F * open, ArcGen.SHADOW, 0.9F);
                    ArcGen.taper(dark, m, c.cam, List.of(e.a, e.a.add(0, -h * 0.8, 0)), 0.3F * open, ArcGen.SHADOW, 0.9F);
                } else {
                    RandomSource r = RandomSource.create(flick);
                    for (int i = 0; i < 4; i++) {
                        Vec3 p0 = e.a.add(0, (r.nextDouble() - 0.5) * h * 1.4, 0);
                        Vec3 p1 = p0.add(flatRight.scale((r.nextDouble() - 0.5) * 2 * h)).add(0, (r.nextDouble() - 0.5) * h, 0);
                        ArcGen.arc(glow, m, c.cam, p0, p1, r.nextLong(), 0.03F, open, 0.25F, 4, 0);
                    }
                }
            }
            case ASCEND -> {
                if (owner == null) return;
                if (owner == mc.player && mc.options.getCameraType().isFirstPerson()) return;
                Vec3 base = owner.getPosition(c.pt);
                float yaw = (owner instanceof LivingEntity le ? Mth.lerp(c.pt, le.yBodyRotO, le.yBodyRot) : owner.getYRot()) * Mth.DEG_TO_RAD;
                Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
                float h = owner.getBbHeight();
                if (pass == 0) {
                    ArcGen.sprite(tex(c, STAR), m, base.add(0, h * 0.55, 0).subtract(fwd.scale(0.2)), c.right, c.up, 1.3F + 0.1F * Mth.sin(age * 0.4F), 1, 0.9F, 1, fadeIO * 0.7F);
                    return;
                }
                if (pass == 1) return;
                // halo
                List<Vec3> halo = new ArrayList<>();
                for (int i = 0; i <= 24; i++) {
                    double ang = i * Math.PI * 2 / 24;
                    halo.add(base.add(side.scale(Math.cos(ang) * 0.38)).add(fwd.scale(Math.sin(ang) * 0.38)).add(0, h + 0.28, 0));
                }
                ArcGen.layers(glow, m, c.cam, halo, 0.05F, fadeIO);
                // wings: fans of feathers from the shoulders
                float flap = Mth.sin(age * 0.25F) * 0.35F;
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 root = base.add(0, h * 0.72, 0).subtract(fwd.scale(0.25)).add(side.scale(0.18 * s));
                    for (int k = 0; k < 6; k++) {
                        double spread = 0.25 + k * 0.22;
                        Vec3 dir = side.scale(s * Math.cos(spread - 0.4) * 1.0).add(0, Math.sin(spread - 0.4) + flap, 0).subtract(fwd.scale(0.45));
                        Vec3 tip = root.add(dir.normalize().scale(1.1 + k * 0.18));
                        List<Vec3> f = ArcGen.wave(root, tip, 0.06, 1, age * 0.2 + k, 10);
                        ArcGen.taper(glow, m, c.cam, f, 0.16F, ArcGen.GLOW, fadeIO * 0.45F);
                        ArcGen.taper(glow, m, c.cam, f, 0.06F, ArcGen.CORE, fadeIO * 0.9F);
                    }
                }
            }
            case SEAL -> {
                float r = e.param * Math.min(1, age / 8F);
                if (pass == 0) { flatSprite(tex(c, STARFIELD), m, e.a.add(0, 0.03, 0), r, age * 0.01F, fadeIO * 0.55F); return; }
                if (pass == 1) return;
                Vec3 cc = e.a.add(0, 0.08, 0);
                ArcGen.layers(glow, m, c.cam, circle(cc, r, 64, age * 0.02), 0.07F, fadeIO);
                ArcGen.layers(glow, m, c.cam, circle(cc, r * 0.82, 64, -age * 0.03), 0.04F, fadeIO);
                // seven pointed star
                List<Vec3> star = new ArrayList<>();
                for (int i = 0; i <= 7; i++) {
                    double ang = i * 3 * Math.PI * 2 / 7 + age * 0.02;
                    star.add(cc.add(Math.cos(ang) * r * 0.82, 0, Math.sin(ang) * r * 0.82));
                }
                ArcGen.layers(glow, m, c.cam, star, 0.035F, fadeIO * 0.9F);
                // light pillars on the rim
                for (int i = 0; i < 7; i++) {
                    double ang = i * Math.PI * 2 / 7 + age * 0.02;
                    Vec3 p0 = cc.add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
                    float ph = 1.5F + Mth.sin(age * 0.2F + i) * 0.6F;
                    ArcGen.taper(glow, m, c.cam, List.of(p0, p0.add(0, ph, 0)), 0.4F, ArcGen.GLOW, fadeIO * 0.5F);
                    ArcGen.taper(glow, m, c.cam, List.of(p0, p0.add(0, ph * 0.8, 0)), 0.1F, ArcGen.CORE, fadeIO * 0.8F);
                }
            }
            case COSMIC -> {
                float r = e.param * Math.min(1, age / 15F);
                Vec3 sky = e.a.add(0, 13, 0);
                if (pass == 0) {
                    flatSprite(tex(c, STARFIELD), m, sky, r * 1.2F, age * 0.006F, fadeIO);
                    flatSprite(tex(c, VORTEX), m, sky.add(0, -0.1, 0), r * 0.9F, -age * 0.03F, fadeIO * 0.7F);
                    flatSprite(tex(c, STARFIELD), m, e.a.add(0, 0.04, 0), r, -age * 0.006F, fadeIO * 0.35F);
                    return;
                }
                if (pass == 1) { flatDisc(dark, m, sky.add(0, 0.05, 0), r * 1.25F, ArcGen.SHADOW, fadeIO * 0.6F); return; }
                ArcGen.layers(glow, m, c.cam, circle(e.a.add(0, 0.1, 0), r, 64, age * 0.01), 0.08F, fadeIO);
                ArcGen.layers(glow, m, c.cam, circle(sky, r * 1.2F, 64, -age * 0.01), 0.1F, fadeIO);
                RandomSource rr = RandomSource.create(e.seed);
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4 + age * 0.01;
                    Vec3 top = sky.add(Math.cos(ang) * r * 1.2, 0, Math.sin(ang) * r * 1.2);
                    Vec3 bot = e.a.add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
                    ArcGen.ribbon(glow, m, c.cam, ArcGen.wave(top, bot, 0.3, 1, age * 0.1 + rr.nextDouble() * 5, 16), 0.12F, ArcGen.GLOW, fadeIO * 0.35F);
                }
            }
            case WISPBURST -> {
                if (pass == 0) return;
                float ease = 1 - (1 - t) * (1 - t);
                float a = 1 - t;
                RandomSource r = RandomSource.create(e.seed);
                int n = Mth.clamp((int) (e.param * 3), 6, 16);
                for (int i = 0; i < n; i++) {
                    Vec3 d = new Vec3(r.nextDouble() - 0.5, (r.nextDouble() - 0.35) * 0.8, r.nextDouble() - 0.5).normalize();
                    List<Vec3> pts = ArcGen.wave(e.a.add(d.scale(e.param * ease * 0.15)), e.a.add(d.scale(e.param * ease)), 0.25, 1.2, i * 2 + age * 0.25, 12);
                    if (pass == 1) ArcGen.taper(dark, m, c.cam, pts, 0.3F, ArcGen.SHADOW, a * 0.5F);
                    else {
                        ArcGen.taper(glow, m, c.cam, pts, 0.14F, ArcGen.GLOW, a * 0.5F);
                        ArcGen.taper(glow, m, c.cam, pts, 0.05F, ArcGen.CORE, a);
                    }
                }
            }
            case TETHER -> {
                if (owner == null || other == null || pass == 0) return;
                List<Vec3> pts = ArcGen.wave(hand(owner, c.pt), center(other, c.pt), 0.35, 2, age * 0.5, 20);
                ArcGen.shadowLayers(glow, dark, m, c.cam, pts, 0.06F, fadeIO);
            }
            case CHARGE -> {
                if (owner == null) return;
                Vec3 h = hand(owner, c.pt);
                if (pass == 0) { rotSprite(tex(c, STAR), m, h, c.right, c.up, 0.2F + t * 0.6F * e.param, age * 0.3F, 0.5F + 0.5F * t); return; }
                if (pass == 1) return;
                RandomSource r = RandomSource.create(flick);
                double rad = 2.6 * (1 - t) + 0.3;
                for (int i = 0; i < 5; i++) {
                    Vec3 from = h.add(new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().scale(rad));
                    ArcGen.layers(glow, m, c.cam, ArcGen.wave(from, h, 0.2, 1, age * 0.4 + i, 10), 0.03F, 0.4F + 0.6F * t);
                }
            }
            case MARK -> {
                if (pass != 0) return;
                float a = t < 0.2F ? t / 0.2F : 1 - (t - 0.2F) / 0.8F;
                rotSprite(tex(c, STAR), m, e.a.add(0, t * 0.4, 0), c.right, c.up, 0.45F * e.param * (0.7F + 0.3F * a), age * 0.15F, a);
            }
        }
    }

    // ------------------------------------------------------------------ shapes

    private static List<Vec3> circle(Vec3 c, double r, int n, double rot) {
        List<Vec3> pts = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) {
            double ang = i * Math.PI * 2 / n + rot;
            pts.add(c.add(Math.cos(ang) * r, 0, Math.sin(ang) * r));
        }
        return pts;
    }

    private static List<Vec3> flatRing(Vec3 c, double r, int n, long seed, double jitter) {
        RandomSource rnd = RandomSource.create(seed);
        List<Vec3> pts = new ArrayList<>(n + 1);
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n, j = (rnd.nextDouble() - 0.5) * jitter * 2;
            pts.add(c.add(Math.cos(ang) * (r + j), 0.1 + Math.abs(j), Math.sin(ang) * (r + j)));
        }
        pts.add(pts.get(0));
        return pts;
    }

    /** spiral arms converging on c (in the horizontal plane, lifted by `lift` toward the middle) */
    private static void spiralArms(VertexConsumer glow, Matrix4f m, Vec3 cam, Vec3 c, float r, int arms, float rot, float lift, float alpha) {
        for (int k = 0; k < arms; k++) {
            List<Vec3> pts = new ArrayList<>();
            for (int i = 0; i <= 20; i++) {
                double f = i / 20.0;
                double ang = k * Math.PI * 2 / arms + rot + f * 3.2;
                double rad = r * (1 - f * 0.92);
                pts.add(c.add(Math.cos(ang) * rad, Math.sin(f * Math.PI) * lift * 0.3 + (f - 1) * 0 , Math.sin(ang) * rad));
            }
            ArcGen.taper(glow, m, cam, pts, 0.22F, ArcGen.GLOW, alpha * 0.5F);
            ArcGen.taper(glow, m, cam, pts, 0.07F, ArcGen.CORE, alpha * 0.9F);
        }
    }

    /** horizontal textured quad rotating around Y */
    private static void flatSprite(VertexConsumer vc, Matrix4f m, Vec3 c, float r, float rot, float a) {
        float cs = Mth.cos(rot) * r, sn = Mth.sin(rot) * r;
        ArcGen.t(vc, m, c.add(-cs + sn, 0, -sn - cs), 1, 1, 1, a, 0, 0);
        ArcGen.t(vc, m, c.add(cs + sn, 0, sn - cs), 1, 1, 1, a, 1, 0);
        ArcGen.t(vc, m, c.add(cs - sn, 0, sn + cs), 1, 1, 1, a, 1, 1);
        ArcGen.t(vc, m, c.add(-cs - sn, 0, -sn + cs), 1, 1, 1, a, 0, 1);
    }

    private static void flatDisc(VertexConsumer vc, Matrix4f m, Vec3 c, float r, float[] col, float a) {
        int n = 24;
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            ArcGen.v(vc, m, c, col, a);
            ArcGen.v(vc, m, c.add(Math.cos(a0) * r, 0, Math.sin(a0) * r), col, 0);
            ArcGen.v(vc, m, c.add(Math.cos(a1) * r, 0, Math.sin(a1) * r), col, 0);
            ArcGen.v(vc, m, c, col, a);
        }
    }

    /** camera facing textured quad rotated around the view axis */
    private static void rotSprite(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 right, Vec3 up, float size, float rot, float a) {
        Vec3 r2 = right.scale(Mth.cos(rot)).add(up.scale(Mth.sin(rot)));
        Vec3 u2 = up.scale(Mth.cos(rot)).subtract(right.scale(Mth.sin(rot)));
        ArcGen.sprite(vc, m, p, r2, u2, size, 1, 1, 1, a);
    }

    private static void sphere(VertexConsumer vc, Matrix4f m, Vec3 c, float r, float scroll, float alpha) {
        int lat = 10, lon = 20;
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                float u0 = (float) j / lon * 3 + scroll, u1 = (float) (j + 1) / lon * 3 + scroll;
                float v0 = (float) i / lat * 2, v1 = (float) (i + 1) / lat * 2;
                ArcGen.t(vc, m, sph(c, r, t0, p0), 0.85F, 0.7F, 1F, alpha, u0, v0);
                ArcGen.t(vc, m, sph(c, r, t0, p1), 0.85F, 0.7F, 1F, alpha, u1, v0);
                ArcGen.t(vc, m, sph(c, r, t1, p1), 0.85F, 0.7F, 1F, alpha, u1, v1);
                ArcGen.t(vc, m, sph(c, r, t1, p0), 0.85F, 0.7F, 1F, alpha, u0, v1);
            }
        }
    }

    private static Vec3 sph(Vec3 c, double r, double theta, double phi) {
        return c.add(r * Math.sin(theta) * Math.cos(phi), r * Math.cos(theta), r * Math.sin(theta) * Math.sin(phi));
    }

    // ------------------------------------------------------------------ helpers

    public static Vec3 center(Entity e, float pt) { return e.getPosition(pt).add(0, e.getBbHeight() * 0.55, 0); }

    public static Vec3 hand(Entity e, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (e == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Camera c = mc.gameRenderer.getMainCamera();
            Vector3f l = c.getLeftVector(), u = c.getUpVector(), f = c.getLookVector();
            return c.getPosition().add(f.x() * 1.5 - l.x() * 0.55 - u.x() * 0.4, f.y() * 1.5 - l.y() * 0.55 - u.y() * 0.4, f.z() * 1.5 - l.z() * 0.55 - u.z() * 0.4);
        }
        float yaw = (e instanceof LivingEntity le ? Mth.lerp(pt, le.yBodyRotO, le.yBodyRot) : e.getYRot()) * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(0.38);
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(0.55);
        return e.getPosition(pt).add(0, e.getBbHeight() * 0.72, 0).add(side).add(fwd);
    }
}
