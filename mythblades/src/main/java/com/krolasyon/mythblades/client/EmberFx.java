package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.network.FxNet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Kor Kılıcı'nın dünyadaki efektleri: kesik izleri (anime "smear"), atılış alev izi, saplamada ateş kasırgası ve
 * çarpma halkası ile alev sütunları, savunma kalkanı, karşı koyma patlaması; kamera sarsıntısı, hız çizgileri, parlama.
 */
public final class EmberFx {
    private EmberFx() {}

    private abstract static class Fx {
        final float start;
        Fx(float start) {
            this.start = start;
        }
        abstract float life();
        abstract void draw(MultiBufferSource buf, Matrix4f m, Vec3 cam, float now, Camera camera);
    }

    private static final List<Fx> ACTIVE = new ArrayList<>();
    /** Varlık başına: atılış/saplama bitişi ve son savunma darbesi zamanı (üçüncü şahıs duruşları için). */
    static final Map<Integer, Float> DASH_UNTIL = new HashMap<>(), PLUNGE_UNTIL = new HashMap<>(), BLOCK_FLASH = new HashMap<>();
    private static float shake, shakeDecay;
    public static float dashLocalUntil, flashAt = -100, flashPower;

    public static float now(float pt) {
        Level l = Minecraft.getInstance().level;
        return l == null ? 0 : l.getGameTime() + pt;
    }

    public static void shake(float amount) {
        shake = Math.max(shake, amount);
        shakeDecay = 0.86F;
    }

    public static void flash(float power) {
        flashAt = now(0);
        flashPower = power;
    }

    // ------------------------------------------------------------------ sunucudan gelen efektler

    public static void receive(FxNet.Fx f) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity e = mc.level.getEntity(f.entity());
        float t = now(0);
        boolean self = e == mc.player;
        Vec3 at = new Vec3(f.x(), f.y(), f.z());
        switch (f.type()) {
            case FxNet.DASH -> {
                if (!self && e != null) {
                    DASH_UNTIL.put(e.getId(), t + 8);
                    ACTIVE.add(new Trail(e, t));
                }
            }
            case FxNet.PLUNGE -> {
                if (e != null) {
                    PLUNGE_UNTIL.put(e.getId(), t + 45);
                    if (!self) ACTIVE.add(new Vortex(e, t));
                }
            }
            case FxNet.IMPACT -> {
                if (e != null) PLUNGE_UNTIL.remove(e.getId());
                ACTIVE.add(new Impact(at, t, f.a()));
                double d = mc.player.position().distanceTo(at);
                if (d < 20) shake((float) (2.2 * (1 - d / 20)));
                if (self) flash(0.55F);
                for (int i = 0; i < 60; i++) {
                    double a = mc.level.random.nextDouble() * Math.PI * 2, r = mc.level.random.nextDouble() * f.a();
                    mc.level.addParticle(i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, at.x + Math.cos(a) * r, at.y + 0.2, at.z + Math.sin(a) * r,
                            Math.cos(a) * 0.15, 0.25 + mc.level.random.nextDouble() * 0.3, Math.sin(a) * 0.15);
                }
            }
            case FxNet.PARRY -> {
                ACTIVE.add(new Burst(at, t, 1.0F));
                double d = mc.player.position().distanceTo(at);
                if (d < 12) shake(self ? 1.4F : 0.6F);
                if (self) flash(0.8F);
                for (int i = 0; i < 40; i++) {
                    Vec3 v = new Vec3(mc.level.random.nextGaussian(), mc.level.random.nextGaussian(), mc.level.random.nextGaussian()).normalize().scale(0.35);
                    mc.level.addParticle(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, v.x, v.y, v.z);
                }
            }
            case FxNet.BLOCK_HIT -> {
                if (e != null) BLOCK_FLASH.put(e.getId(), t);
                ACTIVE.add(new Burst(at, t, 0.45F));
                if (self) shake(0.5F);
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ istemcide başlayan efektler

    public static void slash(LivingEntity e, int stage, boolean firstPerson) {
        ACTIVE.add(new Slash(e, now(0), stage, e.getYRot(), e.getXRot(), firstPerson));
    }

    public static void dashLocal(Player p) {
        float t = now(0);
        dashLocalUntil = t + 8;
        DASH_UNTIL.put(p.getId(), t + 8);
        ACTIVE.add(new Trail(p, t));
        shake(0.6F);
    }

    public static void plungeLocal(Player p) {
        float t = now(0);
        PLUNGE_UNTIL.put(p.getId(), t + 45);
        ACTIVE.add(new Vortex(p, t));
    }

    public static boolean dashing(Entity e, float pt) {
        Float u = DASH_UNTIL.get(e.getId());
        return u != null && now(pt) < u;
    }

    public static boolean plunging(Entity e, float pt) {
        Float u = PLUNGE_UNTIL.get(e.getId());
        return u != null && now(pt) < u;
    }

    public static void clear() {
        ACTIVE.clear();
        DASH_UNTIL.clear();
        PLUNGE_UNTIL.clear();
        BLOCK_FLASH.clear();
    }

    // ------------------------------------------------------------------ tik: izleri örnekle, kıvılcım saç

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            clear();
            return;
        }
        float t = now(0);
        for (Fx f : ACTIVE) {
            if (f instanceof Trail tr) tr.sample(t);
            if (f instanceof Slash s) s.sparks(t);
            if (f instanceof Vortex v) v.sparks(t);
        }
        if (shake > 0.01F) shake *= shakeDecay;
        else shake = 0;
    }

    // ------------------------------------------------------------------ çizim

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = e.getPartialTick();
        float t = now(pt);
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Matrix4f m = ps.last().pose();
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx f = it.next();
            if (t - f.start > f.life()) {
                it.remove();
                continue;
            }
            f.draw(buf, m, cam, t, e.getCamera());
        }
        // savunan oyuncuların önünde dönen alev kalkanı
        for (Player p : mc.level.players()) {
            if (p.isUsingItem() && p.getUseItem().is(MythBlades.EMBER.get())) shield(buf, m, cam, p, pt, t);
        }
        buf.endBatch();
    }

    private static void shield(MultiBufferSource buf, Matrix4f m, Vec3 cam, Player p, float pt, float t) {
        Vec3 eye = p.getEyePosition(pt);
        Vec3 look = p.getViewVector(pt);
        boolean fp = p == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        Vec3 c = eye.add(look.scale(fp ? 1.1 : 0.85)).add(0, fp ? -0.25 : -0.35, 0).subtract(cam);
        Vec3 r = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 u = r.cross(look).normalize();
        float grow = Mth.clamp((p.getTicksUsingItem() + pt) / 4F, 0, 1);
        Float hitAt = BLOCK_FLASH.get(p.getId());
        float flare = hitAt == null ? 0 : Mth.clamp(1 - (t - hitAt) / 7F, 0, 1);
        boolean parryWindow = p.getTicksUsingItem() <= 7;
        float rad = (fp ? 0.75F : 0.95F) * grow * (1 + 0.15F * flare);
        float a = 0.35F + 0.4F * flare + (parryWindow ? 0.25F : 0);
        double rot = t * 0.12;
        float[][] q = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(FxDraw.SHIELD) : FxDraw.glow(FxDraw.SHIELD));
            float k = pass == 0 ? a * 0.6F : a;
            for (float[] c4 : q) {
                // dokuyu döndürerek kalkanın içindeki alev girdabı döner
                double ca = Math.cos(rot), sa = Math.sin(rot);
                float uu = (float) (0.5 + (c4[0] * ca - c4[1] * sa) * 0.5), vv = (float) (0.5 + (c4[0] * sa + c4[1] * ca) * 0.5);
                Vec3 pnt = c.add(r.scale(c4[0] * rad)).add(u.scale(c4[1] * rad));
                FxDraw.v(vc, m, (float) pnt.x, (float) pnt.y, (float) pnt.z, uu, vv, 1, 1, 1, k);
            }
        }
    }

    // ------------------------------------------------------------------ kamera ve ekran

    public static void cameraAngles(ViewportEvent.ComputeCameraAngles e) {
        if (shake <= 0.01F) return;
        float t = now((float) e.getPartialTick());
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 2.7) * shake));
        e.setPitch(e.getPitch() + (float) (Math.cos(t * 3.3) * shake * 0.8));
        e.setRoll(e.getRoll() + (float) (Math.sin(t * 4.1) * shake * 0.6));
    }

    public static void overlay(GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        float t = now(pt);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (t < dashLocalUntil) {
            float a = Mth.clamp((dashLocalUntil - t) / 8F, 0, 1);
            g.setColor(1, 1, 1, a * 0.85F);
            g.blit(FxDraw.SPEED, 0, 0, 0, 0, w, h, w, h);
        }
        float fl = 1 - (t - flashAt) / 6F;
        if (fl > 0) {
            g.setColor(1, 1, 1, fl * flashPower * 0.3F);
            g.fill(0, 0, w, h, 0xFFFFD9A0);
        }
        g.setColor(1, 1, 1, 1);
        RenderSystem.enableDepthTest();
    }

    // ================================================================== efekt türleri

    /** Ateşli yay şeridi (yay boyunca u, içten dışa v). */
    static void arc(MultiBufferSource buf, Matrix4f m, Vec3 c, Vec3 a, Vec3 b, double from, double to, float r0, float r1, float alpha,
                            net.minecraft.resources.ResourceLocation tex, float glowK) {
        int n = 28;
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(tex) : FxDraw.glow(tex));
            float k = pass == 0 ? alpha * 0.85F : alpha * glowK;
            for (int i = 0; i < n; i++) {
                double t0 = i / (double) n, t1 = (i + 1) / (double) n;
                double th0 = Mth.lerp(t0, from, to), th1 = Mth.lerp(t1, from, to);
                // kuyruk ince, baş kalın
                float w0 = (float) (0.25 + 0.75 * t0), w1 = (float) (0.25 + 0.75 * t1);
                Vec3 d0 = a.scale(Math.cos(th0)).add(b.scale(Math.sin(th0))), d1 = a.scale(Math.cos(th1)).add(b.scale(Math.sin(th1)));
                float ri0 = r1 - (r1 - r0) * w0, ri1 = r1 - (r1 - r0) * w1;
                Vec3 p00 = c.add(d0.scale(ri0)), p01 = c.add(d0.scale(r1)), p10 = c.add(d1.scale(ri1)), p11 = c.add(d1.scale(r1));
                FxDraw.v(vc, m, (float) p00.x, (float) p00.y, (float) p00.z, (float) t0, 0, 1, 1, 1, k);
                FxDraw.v(vc, m, (float) p01.x, (float) p01.y, (float) p01.z, (float) t0, 1, 1, 1, 1, k);
                FxDraw.v(vc, m, (float) p11.x, (float) p11.y, (float) p11.z, (float) t1, 1, 1, 1, 1, k);
                FxDraw.v(vc, m, (float) p10.x, (float) p10.y, (float) p10.z, (float) t1, 0, 1, 1, 1, k);
            }
        }
    }

    static Vec3 forward(float yaw, float pitch) {
        return Vec3.directionFromRotation(pitch, yaw);
    }

    /** Kesik izi: kılıcın savrulduğu yayı izleyen, başta parlak, kuyrukta sönen alev şeridi. */
    private static final class Slash extends Fx {
        final LivingEntity e;
        final int stage;
        final float yaw, pitch;
        final boolean fp;
        static final float DUR = 5, FADE = 6;

        Slash(LivingEntity e, float start, int stage, float yaw, float pitch, boolean fp) {
            super(start);
            this.e = e;
            this.stage = stage;
            this.yaw = yaw;
            this.pitch = pitch;
            this.fp = fp;
        }

        @Override
        float life() {
            return DUR + FADE;
        }

        private Vec3[] basis() {
            Vec3 f = forward(yaw, stage == 2 ? 0 : pitch * 0.6F);
            Vec3 right = f.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(f).normalize();
            double tilt = Math.toRadians(stage == 0 ? 18 : stage == 1 ? -24 : 90);
            Vec3 b = right.scale(Math.cos(tilt)).add(up.scale(Math.sin(tilt)));
            return new Vec3[]{f, b};
        }

        private double[] range(float t) {
            double s, en;
            if (stage == 0) {
                s = Math.toRadians(85);
                en = Math.toRadians(-100);
            } else if (stage == 1) {
                s = Math.toRadians(-90);
                en = Math.toRadians(95);
            } else {
                s = Math.toRadians(150);
                en = Math.toRadians(-55);
            }
            float k = Mth.clamp((t - start) / DUR, 0, 1);
            float ease = 1 - (1 - k) * (1 - k) * (1 - k);
            double head = Mth.lerp(ease, s, en);
            double tail = head - Math.signum(en - s) * Math.toRadians(125);
            if (Math.signum(en - s) > 0 ? tail < s : tail > s) tail = s;
            return new double[]{tail, head};
        }

        private Vec3 center(float t) {
            float pt = t - (float) Math.floor(t);
            Vec3 c = e.getPosition(pt).add(0, e.getEyeHeight() - (fp ? 0.35 : 0.45), 0);
            return c;
        }

        @Override
        void draw(MultiBufferSource buf, Matrix4f m, Vec3 cam, float t, Camera camera) {
            Vec3[] ab = basis();
            double[] rg = range(t);
            float age = t - start;
            float a = age < DUR ? 1 : Mth.clamp(1 - (age - DUR) / FADE, 0, 1);
            Vec3 c = center(t).subtract(cam);
            if (fp) c = c.add(ab[0].scale(0.15));
            arc(buf, m, c, ab[0], ab[1], rg[0], rg[1], 0.55F, stage == 2 ? 3.1F : 2.9F, a, FxDraw.SLASH, 1.0F);
        }

        void sparks(float t) {
            if (t - start > DUR) return;
            Vec3[] ab = basis();
            double head = range(t)[1];
            Vec3 tip = center(t).add(ab[0].scale(Math.cos(head) * 2.6)).add(ab[1].scale(Math.sin(head) * 2.6));
            Level l = e.level();
            for (int i = 0; i < 3; i++)
                l.addParticle(i == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, tip.x, tip.y, tip.z, (l.random.nextDouble() - 0.5) * 0.12, 0.05, (l.random.nextDouble() - 0.5) * 0.12);
        }
    }

    /** Atılış izi: oyuncunun geçtiği yol boyunca dikine alev perdesi. */
    private static final class Trail extends Fx {
        final Entity e;
        final List<Vec3> pts = new ArrayList<>();

        Trail(Entity e, float start) {
            super(start);
            this.e = e;
            pts.add(e.position());
        }

        @Override
        float life() {
            return 20;
        }

        void sample(float t) {
            if (t - start < 9) {
                pts.add(e.position());
                Level l = e.level();
                for (int i = 0; i < 6; i++)
                    l.addParticle(ParticleTypes.FLAME, e.getX() + l.random.nextGaussian() * 0.3, e.getY() + l.random.nextDouble() * 1.6,
                            e.getZ() + l.random.nextGaussian() * 0.3, 0, 0.02, 0);
            }
        }

        @Override
        void draw(MultiBufferSource buf, Matrix4f m, Vec3 cam, float t, Camera camera) {
            if (pts.size() < 2) return;
            float age = t - start;
            float a = Mth.clamp(1 - (age - 6) / 14F, 0, 1);
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(FxDraw.SLASH) : FxDraw.glow(FxDraw.SLASH));
                float k = pass == 0 ? a * 0.7F : a;
                for (int i = 0; i < pts.size() - 1; i++) {
                    Vec3 p0 = pts.get(i).subtract(cam), p1 = pts.get(i + 1).subtract(cam);
                    float u0 = i / (float) (pts.size() - 1), u1 = (i + 1) / (float) (pts.size() - 1);
                    float h0 = 0.2F + 1.4F * u0, h1 = 0.2F + 1.4F * u1;
                    FxDraw.v(vc, m, (float) p0.x, (float) p0.y + 0.1F, (float) p0.z, u0, 0, 1, 1, 1, k * u0);
                    FxDraw.v(vc, m, (float) p0.x, (float) p0.y + h0, (float) p0.z, u0, 1, 1, 1, 1, k * u0);
                    FxDraw.v(vc, m, (float) p1.x, (float) p1.y + h1, (float) p1.z, u1, 1, 1, 1, 1, k * u1);
                    FxDraw.v(vc, m, (float) p1.x, (float) p1.y + 0.1F, (float) p1.z, u1, 0, 1, 1, 1, k * u1);
                }
            }
        }
    }

    /** Saplama sıçrayışında oyuncuyu saran ateş kasırgası. */
    private static final class Vortex extends Fx {
        final Entity e;

        Vortex(Entity e, float start) {
            super(start);
            this.e = e;
        }

        @Override
        float life() {
            return plunging(e, 0) ? 46 : Math.min(46, Minecraft.getInstance().level.getGameTime() - start + 1);
        }

        void sparks(float t) {
            Level l = e.level();
            for (int i = 0; i < 4; i++) {
                double a = l.random.nextDouble() * Math.PI * 2;
                l.addParticle(ParticleTypes.FLAME, e.getX() + Math.cos(a) * 0.9, e.getY() + l.random.nextDouble() * 2, e.getZ() + Math.sin(a) * 0.9,
                        -Math.sin(a) * 0.1, 0.05, Math.cos(a) * 0.1);
            }
        }

        @Override
        void draw(MultiBufferSource buf, Matrix4f m, Vec3 cam, float t, Camera camera) {
            if (!plunging(e, t - (float) Math.floor(t))) return;
            float pt = t - (float) Math.floor(t);
            Vec3 c = e.getPosition(pt).subtract(cam);
            float age = t - start;
            float grow = Mth.clamp(age / 5F, 0, 1);
            for (int s = 0; s < 2; s++) {
                int n = 30;
                for (int pass = 0; pass < 2; pass++) {
                    VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(FxDraw.SLASH) : FxDraw.glow(FxDraw.SLASH));
                    float k = (pass == 0 ? 0.7F : 1F) * grow;
                    for (int i = 0; i < n; i++) {
                        float f0 = i / (float) n, f1 = (i + 1) / (float) n;
                        double a0 = t * 0.6 + s * Math.PI + f0 * Math.PI * 3, a1 = t * 0.6 + s * Math.PI + f1 * Math.PI * 3;
                        double r0 = 0.6 + 0.5 * f0, r1 = 0.6 + 0.5 * f1;
                        float y0 = f0 * 2.4F, y1 = f1 * 2.4F;
                        float x0 = (float) (c.x + Math.cos(a0) * r0), z0 = (float) (c.z + Math.sin(a0) * r0);
                        float x1 = (float) (c.x + Math.cos(a1) * r1), z1 = (float) (c.z + Math.sin(a1) * r1);
                        FxDraw.v(vc, m, x0, (float) c.y + y0, z0, f0, 0, 1, 1, 1, k);
                        FxDraw.v(vc, m, x0, (float) c.y + y0 + 0.45F, z0, f0, 1, 1, 1, 1, k);
                        FxDraw.v(vc, m, x1, (float) c.y + y1 + 0.45F, z1, f1, 1, 1, 1, 1, k);
                        FxDraw.v(vc, m, x1, (float) c.y + y1, z1, f1, 0, 1, 1, 1, k);
                    }
                }
            }
        }
    }

    /** Saplamanın çarpma anı: yerde genişleyen ateş halkası, yükselen alev sütunları, merkezde ışık. */
    private static final class Impact extends Fx {
        final Vec3 at;
        final float radius;

        Impact(Vec3 at, float start, float radius) {
            super(start);
            this.at = at;
            this.radius = radius;
        }

        @Override
        float life() {
            return 26;
        }

        @Override
        void draw(MultiBufferSource buf, Matrix4f m, Vec3 cam, float t, Camera camera) {
            float age = t - start;
            Vec3 c = at.subtract(cam).add(0, 0.06, 0);
            float k = age / 14F;
            float R = radius * (1 - (1 - Math.min(k, 1)) * (1 - Math.min(k, 1)));
            float a = Mth.clamp(1 - (age - 8) / 18F, 0, 1);
            // genişleyen halka
            int n = 48;
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(FxDraw.SHOCK) : FxDraw.glow(FxDraw.SHOCK));
                float kk = pass == 0 ? a * 0.8F : a;
                float r0 = Math.max(0, R - 1.3F), r1 = R;
                for (int i = 0; i < n; i++) {
                    double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
                    float u0 = i / 8F, u1 = (i + 1) / 8F;
                    FxDraw.v(vc, m, (float) (c.x + Math.cos(a0) * r0), (float) c.y, (float) (c.z + Math.sin(a0) * r0), u0, 0, 1, 1, 1, kk);
                    FxDraw.v(vc, m, (float) (c.x + Math.cos(a0) * r1), (float) c.y, (float) (c.z + Math.sin(a0) * r1), u0, 1, 1, 1, 1, kk);
                    FxDraw.v(vc, m, (float) (c.x + Math.cos(a1) * r1), (float) c.y, (float) (c.z + Math.sin(a1) * r1), u1, 1, 1, 1, 1, kk);
                    FxDraw.v(vc, m, (float) (c.x + Math.cos(a1) * r0), (float) c.y, (float) (c.z + Math.sin(a1) * r0), u1, 0, 1, 1, 1, kk);
                }
            }
            // merkezdeki kızgın iz
            VertexConsumer g = buf.getBuffer(FxDraw.glow(FxDraw.GLOW));
            float gs = 2.2F + R * 0.3F, ga = Mth.clamp(1 - age / 22F, 0, 1);
            FxDraw.v(g, m, (float) c.x - gs, (float) c.y, (float) c.z - gs, 0, 0, 1, 0.7F, 0.4F, ga);
            FxDraw.v(g, m, (float) c.x - gs, (float) c.y, (float) c.z + gs, 0, 1, 1, 0.7F, 0.4F, ga);
            FxDraw.v(g, m, (float) c.x + gs, (float) c.y, (float) c.z + gs, 1, 1, 1, 0.7F, 0.4F, ga);
            FxDraw.v(g, m, (float) c.x + gs, (float) c.y, (float) c.z - gs, 1, 0, 1, 0.7F, 0.4F, ga);
            // alev sütunları: halkanın üstünde kameraya dönük, animasyonlu ateş
            org.joml.Vector3f left = camera.getLeftVector();
            float lx = left.x(), lz = left.z();
            float ll = (float) Math.sqrt(lx * lx + lz * lz);
            lx /= ll;
            lz /= ll;
            int cols = 14;
            for (int i = 0; i < cols; i++) {
                double ang = i * Math.PI * 2 / cols + (i % 2) * 0.2;
                float rr = R * (0.55F + 0.45F * ((i * 7) % 5) / 4F);
                float h = (2.2F + (i % 3) * 0.6F) * Mth.clamp(age / 4F, 0, 1) * a;
                float w = 0.55F + (i % 2) * 0.2F;
                float px = (float) (c.x + Math.cos(ang) * rr), pz = (float) (c.z + Math.sin(ang) * rr);
                float[] fr = FxDraw.frame((int) (t * 0.9F) + i * 5);
                for (int pass = 0; pass < 2; pass++) {
                    VertexConsumer vc = buf.getBuffer(pass == 0 ? FxDraw.body(FxDraw.FIRE) : FxDraw.glow(FxDraw.FIRE));
                    float kk = pass == 0 ? a : a * 0.9F;
                    FxDraw.v(vc, m, px - lx * w, (float) c.y, pz - lz * w, fr[0], fr[3], 1, 1, 1, kk);
                    FxDraw.v(vc, m, px - lx * w, (float) c.y + h, pz - lz * w, fr[0], fr[1], 1, 1, 1, kk);
                    FxDraw.v(vc, m, px + lx * w, (float) c.y + h, pz + lz * w, fr[2], fr[1], 1, 1, 1, kk);
                    FxDraw.v(vc, m, px + lx * w, (float) c.y, pz + lz * w, fr[2], fr[3], 1, 1, 1, kk);
                }
            }
        }
    }

    /** Karşı koyma / savunma darbesi: kameraya dönük ışık patlaması ve genişleyen ateş halkası. */
    private static final class Burst extends Fx {
        final Vec3 at;
        final float power;

        Burst(Vec3 at, float start, float power) {
            super(start);
            this.at = at;
            this.power = power;
        }

        @Override
        float life() {
            return 12;
        }

        @Override
        void draw(MultiBufferSource buf, Matrix4f m, Vec3 cam, float t, Camera camera) {
            float age = t - start;
            float k = Mth.clamp(1 - age / 12F, 0, 1);
            Vec3 c = at.subtract(cam);
            org.joml.Vector3f l = camera.getLeftVector(), u = camera.getUpVector();
            float s = (0.6F + age * 0.25F) * (1 + power);
            VertexConsumer g = buf.getBuffer(FxDraw.glow(FxDraw.GLOW));
            float[][] q = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
            for (float[] p : q) {
                FxDraw.v(g, m, (float) c.x + (l.x() * p[0] + u.x() * p[1]) * s, (float) c.y + (l.y() * p[0] + u.y() * p[1]) * s,
                        (float) c.z + (l.z() * p[0] + u.z() * p[1]) * s, (p[0] + 1) / 2, (p[1] + 1) / 2, 1, 0.85F, 0.6F, k * power);
            }
            // halka
            float R = (0.4F + age * 0.22F) * (1 + power), r0 = R * 0.7F;
            int n = 32;
            VertexConsumer vc = buf.getBuffer(FxDraw.glow(FxDraw.SHOCK));
            for (int i = 0; i < n; i++) {
                double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                FxDraw.v(vc, m, (float) c.x + (l.x() * c0 + u.x() * s0) * r0, (float) c.y + (l.y() * c0 + u.y() * s0) * r0, (float) c.z + (l.z() * c0 + u.z() * s0) * r0,
                        i / 8F, 0, 1, 1, 1, k);
                FxDraw.v(vc, m, (float) c.x + (l.x() * c0 + u.x() * s0) * R, (float) c.y + (l.y() * c0 + u.y() * s0) * R, (float) c.z + (l.z() * c0 + u.z() * s0) * R,
                        i / 8F, 1, 1, 1, 1, k);
                FxDraw.v(vc, m, (float) c.x + (l.x() * c1 + u.x() * s1) * R, (float) c.y + (l.y() * c1 + u.y() * s1) * R, (float) c.z + (l.z() * c1 + u.z() * s1) * R,
                        (i + 1) / 8F, 1, 1, 1, 1, k);
                FxDraw.v(vc, m, (float) c.x + (l.x() * c1 + u.x() * s1) * r0, (float) c.y + (l.y() * c1 + u.y() * s1) * r0, (float) c.z + (l.z() * c1 + u.z() * s1) * r0,
                        (i + 1) / 8F, 0, 1, 1, 1, k);
            }
        }
    }
}
