package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Fx;
import com.krolasyon.omnitrix.ModRegistry;
import com.krolasyon.omnitrix.Omnitrix;
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

/** Client list of active alien power effects, rendered in three passes (textured sprites, soft/solid, additive glow). */
public final class FxRenderer {
    public static final ResourceLocation FIRE_TEX = tex("particle/fire_0"), FIRE_TEX2 = tex("particle/fire_2"), WIND = tex("fx/wind"),
            SONIC = tex("fx/sonic"), DIAL = tex("fx/dial_holo"), FROST_TEX = tex("particle/frost_1"), SLIME_TEX = tex("particle/slime_0"),
            ECTO_TEX = tex("particle/ecto_0");

    private static ResourceLocation tex(String n) { return new ResourceLocation(Omnitrix.MODID, "textures/" + n + ".png"); }

    private static final class Effect {
        Fx.Type type;
        Vec3 a, b;
        int entA, entB, duration, seed;
        float param;
        long start;
    }

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
        EFFECTS.add(e);
        if (mc.player == null) return;
        double dist = mc.player.position().distanceTo(e.a);
        switch (e.type) {
            case TRANSFORM -> {
                if (e.entA == mc.player.getId()) ClientEvents.flash(e.param < 0 ? 0xFF3030 : 0x50FF50, 0.75F);
                ClientEvents.shake((float) (0.3 * Math.max(0, 1 - dist / 20)));
            }
            case FIRE_RING, SHOCKWAVE, SONIC_DOOM -> ClientEvents.shake((float) (Math.min(1, e.param / 8) * Math.max(0, 1 - dist / 30)));
            case CLAP -> ClientEvents.shake((float) (0.4 * Math.max(0, 1 - dist / 20)));
            default -> {}
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
            switch (e.type) {
                case FLAME_CONE, FROST_CONE -> {
                    if (owner == null) break;
                    Vec3 h = hand(owner, 1F), look = owner.getLookAngle();
                    for (int i = 0; i < 5; i++) {
                        Vec3 v = look.add((r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.35).scale(0.5 + r.nextDouble() * 0.4);
                        mc.level.addParticle(e.type == Fx.Type.FLAME_CONE ? ModRegistry.FIRE.get() : ModRegistry.FROST.get(), h.x, h.y, h.z, v.x, v.y, v.z);
                    }
                }
                case FIRE_TRAIL -> {
                    if (owner == null) break;
                    for (int i = 0; i < 4; i++) mc.level.addParticle(ModRegistry.FIRE.get(), owner.getX() + (r.nextDouble() - 0.5) * 0.4, owner.getY() + 0.2,
                            owner.getZ() + (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.05, -0.2, (r.nextDouble() - 0.5) * 0.05);
                }
                case TRANSFORM -> {
                    if (owner == null) break;
                    for (int i = 0; i < 3; i++) {
                        double ang = r.nextDouble() * Math.PI * 2;
                        mc.level.addParticle(ModRegistry.OMNI.get(), owner.getX() + Math.cos(ang) * 0.8, owner.getY() + r.nextDouble() * 2, owner.getZ() + Math.sin(ang) * 0.8,
                                -Math.cos(ang) * 0.05, 0.05, -Math.sin(ang) * 0.05);
                    }
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
            else buf.endBatch(pass == 1 ? OmniRenderTypes.SOFT : OmniRenderTypes.GLOW);
        }
        ps.popPose();
    }

    /** pass 0 = textured sprites, 1 = soft solid geometry, 2 = additive glow */
    private static void draw(Ctx c, Effect e, int pass, Minecraft mc) {
        long now = mc.level.getGameTime();
        float age = now - e.start + c.pt;
        float t = Mth.clamp(age / e.duration, 0, 1);
        float fade = Math.min(1F, Math.min(age / 4F, (e.duration - age) / 6F));
        if (fade <= 0) return;
        Entity owner = e.entA >= 0 ? mc.level.getEntity(e.entA) : null;
        Entity other = e.entB >= 0 ? mc.level.getEntity(e.entB) : null;
        Matrix4f m = c.m;
        VertexConsumer soft = pass == 1 ? c.buf.getBuffer(OmniRenderTypes.SOFT) : null;
        VertexConsumer glow = pass == 2 ? c.buf.getBuffer(OmniRenderTypes.GLOW) : null;
        RandomSource rs = RandomSource.create(e.seed);
        switch (e.type) {
            case TRANSFORM -> {
                if (owner == null) return;
                Vec3 base = owner.getPosition(c.pt);
                int col = e.param < 0 ? 0xFF3030 : 0x40FF40;
                float h = 2.6F;
                if (pass == 0) {
                    float s = t < 0.35F ? t / 0.35F : 1 - (t - 0.35F) / 0.65F;
                    Geo.sprite(c.buf.getBuffer(OmniRenderTypes.sprite(DIAL)), m, base.add(0, 1.1, 0), c.right, c.up, 1.6F * s, age * 0.1F, 1, 1, 1, s);
                    return;
                }
                if (pass == 1) return;
                // light column + rising rings + double helix
                float col01 = t < 0.3F ? t / 0.3F : 1 - (t - 0.3F) / 0.7F;
                List<Vec3> line = List.of(base.add(0, -0.2, 0), base.add(0, h + 2, 0));
                Geo.beam(glow, m, c.cam, line, 0.7F * col01, col, 0xE0FFE0, col01);
                for (int k = 0; k < 5; k++) {
                    double y = ((age * 0.12 + k * 0.2) % 1.0) * h;
                    double r = 0.9 - y * 0.15;
                    Geo.ribbon(glow, m, c.cam, Geo.flatCircle(base.add(0, y, 0), r, 30, age * 0.2), 0.06F, col, fade * 0.8F, false);
                }
                for (int s = 0; s < 2; s++) {
                    List<Vec3> helix = new ArrayList<>();
                    for (int i = 0; i <= 40; i++) {
                        double f = i / 40.0, ang = f * Math.PI * 4 + age * 0.4 + s * Math.PI;
                        helix.add(base.add(Math.cos(ang) * 0.7, f * h, Math.sin(ang) * 0.7));
                    }
                    Geo.ribbon(glow, m, c.cam, helix, 0.08F, col, fade, false);
                }
            }
            case FLAME_CONE, FROST_CONE -> {
                if (owner == null || pass != 0) return;
                boolean fire = e.type == Fx.Type.FLAME_CONE;
                Vec3 h = hand(owner, c.pt), look = owner.getViewVector(c.pt);
                VertexConsumer vc = c.buf.getBuffer(OmniRenderTypes.sprite(fire ? (((int) age / 2) % 2 == 0 ? FIRE_TEX : FIRE_TEX2) : FROST_TEX));
                for (int i = 0; i < 22; i++) {
                    double d = ((age * 0.55 + i * 0.37) % 8.0);
                    Vec3 off = Geo.perp(look, rs).scale(d * 0.18 * rs.nextDouble());
                    Vec3 p = h.add(look.scale(d)).add(off);
                    float s = (float) (0.15 + d * 0.12);
                    float al = fade * (float) (1 - d / 8.0);
                    Geo.sprite(vc, m, p, c.right, c.up, s, (float) (age * 0.2 + i), fire ? 1 : 0.8F, fire ? 0.85F : 0.95F, fire ? 0.6F : 1, al);
                }
            }
            case FIRE_RING -> {
                float r = e.param * (1 - (1 - t) * (1 - t));
                if (pass == 0) {
                    VertexConsumer vc = c.buf.getBuffer(OmniRenderTypes.sprite(((int) age / 2) % 2 == 0 ? FIRE_TEX : FIRE_TEX2));
                    int n = (int) (12 + e.param * 4);
                    for (int i = 0; i < n; i++) {
                        double ang = i * Math.PI * 2 / n + rs.nextDouble() * 0.2;
                        Vec3 p = e.a.add(Math.cos(ang) * r, 0.5 + rs.nextDouble() * 0.8, Math.sin(ang) * r);
                        Geo.sprite(vc, m, p, c.right, c.up, 0.7F, (float) ang, 1, 0.9F, 0.7F, fade);
                    }
                    Geo.sprite(vc, m, e.a.add(0, 1, 0), c.right, c.up, 2.5F * (1 - t), age * 0.1F, 1, 1, 1, 1 - t);
                } else if (pass == 2) {
                    Geo.ribbon(glow, m, c.cam, Geo.flatCircle(e.a.add(0, 0.1, 0), r, 48, 0), 0.9F, 0xFF7A10, fade * 0.6F, false);
                    Geo.ribbon(glow, m, c.cam, Geo.flatCircle(e.a.add(0, 0.1, 0), r, 48, 0), 0.25F, 0xFFF0A0, fade, false);
                }
            }
            case WIND_CONE -> {
                if (owner == null) return;
                Vec3 eye = owner.getEyePosition(c.pt), look = owner.getViewVector(c.pt);
                if (pass == 0) {
                    VertexConsumer vc = c.buf.getBuffer(OmniRenderTypes.sprite(WIND));
                    for (int i = 0; i < 6; i++) {
                        double d = (age * 0.8 + i * 1.6) % 10;
                        Geo.sprite(vc, m, eye.add(look.scale(d)), c.right, c.up, (float) (0.4 + d * 0.25), -age * 0.3F + i, 1, 1, 1, fade * (float) (1 - d / 10) * 0.8F);
                    }
                } else if (pass == 2) {
                    for (int k = 0; k < 7; k++) {
                        Vec3 off = Geo.perp(look, rs).scale(0.3 + rs.nextDouble());
                        Vec3 a0 = eye.add(off.scale(0.3)), a1 = eye.add(look.scale(9 * Math.min(1, t * 3))).add(off.scale(2.2));
                        Geo.ribbon(glow, m, c.cam, Geo.wave(a0, a1, 0.25, 2, age * 0.6 + k, 18), 0.07F, 0xD8FFE8, fade * 0.7F, true);
                    }
                }
            }
            case TORNADO, SPEED_VORTEX -> {
                boolean speed = e.type == Fx.Type.SPEED_VORTEX;
                Vec3 base = speed ? e.a : e.a.add(e.b.subtract(e.a).scale(t));
                int colA = speed ? 0x3FD9F0 : 0xE8FFF0, colB = speed ? 0x10303A : 0xA8C890;
                if (pass == 0 && !speed) {
                    VertexConsumer vc = c.buf.getBuffer(OmniRenderTypes.sprite(WIND));
                    for (int k = 0; k < 4; k++) Geo.sprite(vc, m, base.add(0, 0.6 + k * 1.1, 0), c.right, c.up, 0.8F + k * 0.35F, -age * 0.5F + k, 1, 1, 1, fade * 0.7F);
                    return;
                }
                if (pass == 1) {
                    for (int k = 0; k < 9; k++) {
                        double y = k * 0.5, r = (speed ? e.param : 0.4 + y * 0.45);
                        Geo.ribbon(soft, m, c.cam, Geo.flatCircle(base.add(0, y, 0), r, 24, age * 0.4 + k), 0.35F, colB, fade * 0.25F, false);
                    }
                    return;
                }
                for (int k = 0; k < 11; k++) {
                    double y = k * 0.42, r = speed ? e.param * (0.8 + 0.2 * Math.sin(k)) : 0.35 + y * 0.45 + 0.1 * Math.sin(age * 0.3 + k);
                    Geo.ribbon(glow, m, c.cam, Geo.flatCircle(base.add(0, y, 0), r, 26, age * (0.5 - k * 0.02)), 0.05F, colA, fade * 0.8F, false);
                }
            }
            case SHOCKWAVE -> {
                float r = e.param * (1 - (1 - t) * (1 - t));
                if (pass == 1) {
                    Geo.ribbon(soft, m, c.cam, Geo.flatCircle(e.a.add(0, 0.15, 0), r, 48, 0), 0.9F + r * 0.1F, 0x6A5A40, fade * 0.5F, false);
                    for (int i = 0; i < 12; i++) {
                        double ang = rs.nextDouble() * Math.PI * 2, len = e.param * (0.5 + rs.nextDouble() * 0.5) * Math.min(1, t * 4);
                        List<Vec3> crack = Geo.jag(e.a.add(0, 0.03, 0), e.a.add(Math.cos(ang) * len, 0.03, Math.sin(ang) * len), rs, 0.15F, 3);
                        Geo.ribbon(soft, m, e.a.add(0, 50, 0), crack, 0.12F, 0x201810, fade * 0.8F, true);
                    }
                } else if (pass == 2) {
                    Geo.ribbon(glow, m, c.cam, Geo.flatCircle(e.a.add(0, 0.2, 0), r, 48, 0), 0.2F, 0xFFF0D0, fade * 0.7F, false);
                }
            }
            case CRYSTAL_SPIKES -> {
                if (pass != 1) return;
                Vec3 d = e.b.subtract(e.a);
                int n = (int) Math.max(4, d.length() / 0.9);
                for (int i = 0; i < n; i++) {
                    float appear = Mth.clamp((age - i * 0.7F) / 3F, 0, 1);
                    if (appear <= 0) continue;
                    float sink = Mth.clamp((e.duration - age) / 8F, 0, 1);
                    Vec3 p = e.a.add(d.scale((i + 0.5) / n)).add((rs.nextDouble() - 0.5) * 0.6, 0, (rs.nextDouble() - 0.5) * 0.6);
                    double hgt = (1.0 + rs.nextDouble() * 0.9) * appear * sink;
                    Vec3 tip = p.add((rs.nextDouble() - 0.5) * 0.4, hgt, (rs.nextDouble() - 0.5) * 0.4);
                    Geo.crystal(soft, m, p.add(0, -0.3, 0), tip, 0.22 * appear, 0x9FF5C8, 0.85F, i);
                    Geo.crystal(soft, m, p.add(0.3, -0.3, 0.1), p.add(0.45, hgt * 0.6, 0.2), 0.12 * appear, 0xC8FFE0, 0.85F, i + 2);
                }
            }
            case CRYSTAL_PRISON, ICE_PRISON -> {
                if (other == null || pass != 1) return;
                boolean ice = e.type == Fx.Type.ICE_PRISON;
                Vec3 base = other.getPosition(c.pt);
                float hgt = other.getBbHeight(), w = other.getBbWidth();
                float grow = Mth.clamp(age / 5F, 0, 1) * Mth.clamp((e.duration - age) / 4F, 0, 1);
                int n = ice ? 10 : 9;
                for (int i = 0; i < n; i++) {
                    double ang = i * Math.PI * 2 / n + rs.nextDouble() * 0.3;
                    Vec3 p = base.add(Math.cos(ang) * w * 0.7, 0, Math.sin(ang) * w * 0.7);
                    Vec3 tip = p.add(Math.cos(ang) * -0.2, hgt * (0.9 + rs.nextDouble() * 0.4) * grow, Math.sin(ang) * -0.2);
                    if (ice) {
                        double s = w * 0.45 * grow;
                        Geo.sphere(soft, m, p.add(0, hgt * 0.5 * grow, 0), s, hgt * 0.55 * grow, s, 0xA8E4FF, 0.55F, 3, 4);
                    } else {
                        Geo.crystal(soft, m, p, tip, 0.25 * grow, 0x9FF5C8, 0.7F, i);
                    }
                }
            }
            case SLIME_SPLASH -> {
                if (pass != 1) return;
                float ease = 1 - (1 - t) * (1 - t);
                Geo.sphere(soft, m, e.a.add(0, 0.03, 0), e.param * Math.min(1, t * 3), 0.03, e.param * Math.min(1, t * 3), 0x5ACC20, fade * 0.7F, 2, 20);
                for (int i = 0; i < 14; i++) {
                    Vec3 d = new Vec3(rs.nextDouble() - 0.5, rs.nextDouble() * 0.8, rs.nextDouble() - 0.5).normalize();
                    Vec3 p = e.a.add(d.scale(e.param * ease)).add(0, -t * t * 1.5 + 0.3, 0);
                    double s = 0.12 + rs.nextDouble() * 0.12;
                    Geo.sphere(soft, m, p, s, s * 0.8, s, 0x7CDC2A, fade, 3, 6);
                }
            }
            case STRETCH, TENTACLE -> {
                if (owner == null || other == null || pass != 1) return;
                boolean slime = e.type == Fx.Type.STRETCH;
                Vec3 a = hand(owner, c.pt), b = center(other, c.pt);
                float reach = Mth.clamp(age / 4F, 0, 1) * Mth.clamp((e.duration - age) / 4F, 0, 1);
                Vec3 end = a.add(b.subtract(a).scale(reach));
                List<Vec3> pts = Geo.wave(a, end, slime ? 0.15 : 0.35, slime ? 1 : 2, age * 0.4, 20);
                Geo.ribbon(soft, m, c.cam, pts, slime ? 0.32F : 0.22F, slime ? 0x7CDC2A : 0xB8B8B8, 0.95F, !slime);
                Geo.ribbon(soft, m, c.cam, pts, slime ? 0.12F : 0.06F, slime ? 0xB8FF70 : 0x202020, 0.9F, !slime);
            }
            case SPEED_TRAIL -> {
                if (pass == 0) return;
                float al = (1 - t) * (1 - t);
                for (int k = 0; k < 5; k++) {
                    double y = 0.3 + k * 0.35;
                    List<Vec3> l = List.of(e.a.add(0, y, 0), e.b.add(0, y, 0));
                    if (pass == 1) Geo.ribbon(soft, m, c.cam, l, 0.35F, 0x0A1A20, al * 0.6F, false);
                    else Geo.ribbon(glow, m, c.cam, l, 0.12F, k % 2 == 0 ? 0x3FD9F0 : 0xE0FFFF, al, false);
                }
            }
            case CLAP -> {
                if (owner == null || pass != 2) return;
                Vec3 eye = owner.getEyePosition(c.pt).add(0, -0.4, 0), look = owner.getViewVector(c.pt);
                Vec3 ax1 = Geo.perp(look, RandomSource.create(1)), ax2 = look.cross(ax1).normalize();
                for (int k = 0; k < 4; k++) {
                    double d = (t * 9 + k * 1.2);
                    if (d > 9) continue;
                    Geo.ribbon(glow, m, c.cam, Geo.circle(eye.add(look.scale(d)), ax1, ax2, 0.4 + d * 0.45, 32, 0), 0.12F, 0xFFF5D0, fade * (float) (1 - d / 9), false);
                }
            }
            case SONIC_WAVE -> {
                if (owner == null || pass != 0) return;
                Vec3 eye = owner.getEyePosition(c.pt), look = owner.getViewVector(c.pt);
                Vec3 ax1 = Geo.perp(look, RandomSource.create(2)), ax2 = look.cross(ax1).normalize();
                VertexConsumer vc = c.buf.getBuffer(OmniRenderTypes.sprite(SONIC));
                for (int k = 0; k < 6; k++) {
                    double d = (age * 0.9 + k * 2) % 12;
                    float s = (float) (0.4 + d * 0.3);
                    Geo.plane(vc, m, eye.add(look.scale(d)), ax1.scale(s), ax2.scale(s), 1, 1, 1, fade * (float) (1 - d / 12));
                }
            }
            case SONIC_DOOM -> {
                if (other == null) return;
                Vec3 cc = center(other, c.pt);
                float r = 1.4F + other.getBbWidth();
                if (pass == 1) {
                    for (int i = 0; i < 8; i++) {
                        double ang = i * Math.PI / 4 + age * 0.15;
                        Vec3 p = cc.add(Math.cos(ang) * r, Math.sin(ang * 2) * 0.4, Math.sin(ang) * r);
                        Geo.sphere(soft, m, p, 0.24, 0.24, 0.24, 0x1A1E26, 1F, 4, 8);
                    }
                } else if (pass == 2) {
                    for (int i = 0; i < 8; i++) {
                        double ang = i * Math.PI / 4 + age * 0.15;
                        Vec3 p = cc.add(Math.cos(ang) * r, Math.sin(ang * 2) * 0.4, Math.sin(ang) * r);
                        Geo.sphere(glow, m, p, 0.14, 0.14, 0.14, 0x18FFF0, 1F, 3, 6);
                        Geo.beam(glow, m, c.cam, List.of(p, cc), 0.05F, 0x18FFF0, 0xE0FFFF, 0.5F + 0.5F * t);
                    }
                    if (t > 0.9F) Geo.sphere(glow, m, cc, r * 2 * (t - 0.9) * 10, r * 2 * (t - 0.9) * 10, r * 2 * (t - 0.9) * 10, 0x18FFF0, 1 - (t - 0.9F) * 10, 6, 12);
                }
            }
            case TERROR -> {
                float r = e.param * (1 - (1 - t) * (1 - t));
                if (pass == 1) Geo.sphere(soft, m, e.a.add(0, 1, 0), r, r * 0.6, r, 0x050008, fade * 0.35F, 8, 16);
                else if (pass == 2) Geo.ribbon(glow, m, c.cam, Geo.flatCircle(e.a.add(0, 0.2, 0), r, 40, age * 0.1), 0.15F, 0xFF40C0, fade * 0.7F, false);
                else Geo.sprite(c.buf.getBuffer(OmniRenderTypes.sprite(ECTO_TEX)), m, e.a.add(0, 1.6, 0), c.right, c.up, 1.2F, age * 0.1F, 1, 0.6F, 0.9F, fade);
            }
            case FIRE_TRAIL -> {}
            case HIT -> {
                if (pass != 2) return;
                for (int i = 0; i < 6; i++) {
                    Vec3 d = new Vec3(rs.nextDouble() - 0.5, rs.nextDouble() - 0.5, rs.nextDouble() - 0.5).normalize().scale(0.7 * t + 0.2);
                    Geo.ribbon(glow, m, c.cam, List.of(e.a.add(d.scale(0.3)), e.a.add(d)), 0.08F, 0xFFFFFF, 1 - t, true);
                }
            }
        }
    }

    public static Vec3 center(Entity e, float pt) { return e.getPosition(pt).add(0, e.getBbHeight() * 0.55, 0); }

    public static Vec3 hand(Entity e, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (e == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Camera c = mc.gameRenderer.getMainCamera();
            Vector3f l = c.getLeftVector(), u = c.getUpVector(), f = c.getLookVector();
            return c.getPosition().add(f.x() * 1.2 - l.x() * 0.4 - u.x() * 0.3, f.y() * 1.2 - l.y() * 0.4 - u.y() * 0.3, f.z() * 1.2 - l.z() * 0.4 - u.z() * 0.3);
        }
        float yaw = (e instanceof LivingEntity le ? Mth.lerp(pt, le.yBodyRotO, le.yBodyRot) : e.getYRot()) * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(0.35);
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(0.6);
        return e.getPosition(pt).add(0, e.getBbHeight() * 0.7, 0).add(side).add(fwd);
    }
}
