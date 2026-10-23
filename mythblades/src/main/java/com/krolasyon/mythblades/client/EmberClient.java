package com.krolasyon.mythblades.client;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.network.FxNet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.IArmPoseTransformer;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Kor Kılıcı'nın istemci tarafı: atılış tuşu, üç vuruşluk kombonun takibi (her savuruşun kendi animasyonu ve kesik izi),
 * atılış/saplama hareketi, üçüncü şahıs vücut duruşları ve birinci şahıs kılıç animasyonları.
 */
public final class EmberClient {
    private EmberClient() {}

    public static final KeyMapping DASH = new KeyMapping("key.mythblades.ember_dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.mythblades");

    private record Combo(int stage, float start) {}
    private static final Map<Integer, Combo> COMBO = new HashMap<>();
    private static final Map<Integer, int[]> PREV = new HashMap<>();
    private static int dashTicks, plungeTicks;
    private static Vec3 dashDir = Vec3.ZERO;
    private static long dashCd, plungeCd;

    private static boolean holds(LivingEntity e) {
        return e.getMainHandItem().is(MythBlades.EMBER.get());
    }

    public static int stage(LivingEntity e) {
        Combo c = COMBO.get(e.getId());
        return c == null ? 0 : c.stage;
    }

    // ------------------------------------------------------------------ tik

    public static void tick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            COMBO.clear();
            PREV.clear();
            return;
        }
        if (mc.isPaused()) return;
        EmberFx.tick();
        float now = EmberFx.now(0);
        // her oyuncunun yeni savuruşu: kombo aşaması ilerler, kesik izi doğar
        for (Player p : mc.level.players()) {
            int[] prev = PREV.computeIfAbsent(p.getId(), k -> new int[]{0, 0});
            boolean start = p.swinging && (prev[0] == 0 || p.swingTime < prev[1]);
            prev[0] = p.swinging ? 1 : 0;
            prev[1] = p.swingTime;
            if (!start || !holds(p) || p.isUsingItem()) continue;
            Combo c = COMBO.get(p.getId());
            int st = c != null && now - c.start <= 22 ? (c.stage + 1) % 3 : 0;
            COMBO.put(p.getId(), new Combo(st, now));
            EmberFx.slash(p, st, p == mc.player && mc.options.getCameraType().isFirstPerson());
        }
        LocalPlayer me = mc.player;
        while (DASH.consumeClick()) tryMove(me.isShiftKeyDown());
        // atılış: birkaç tik boyunca sabit, yüksek hız (sonra sürtünmeyle durur)
        if (dashTicks > 0) {
            dashTicks--;
            Vec3 v = me.getDeltaMovement();
            me.setDeltaMovement(dashDir.x * 1.35, Math.min(v.y, 0.08), dashDir.z * 1.35);
            if (dashTicks == 0) me.setDeltaMovement(dashDir.x * 0.3, v.y, dashDir.z * 0.3);
        }
        // saplama: yükselip zirvede yere çakılır
        if (plungeTicks > 0) {
            plungeTicks++;
            Vec3 v = me.getDeltaMovement();
            if (plungeTicks > 7 && !me.onGround()) me.setDeltaMovement(v.x * 0.6, Math.min(v.y, -1.7), v.z * 0.6);
            if ((plungeTicks > 7 && me.onGround()) || plungeTicks > 45) {
                plungeTicks = 0;
                EmberFx.shake(1.8F);
            }
        }
    }

    /** Atılış (dash) ya da eğilerek kor saplama; test de bunu doğrudan çağırır. */
    public static void tryMove(boolean plunge) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || !holds(me) || me.isSpectator()) return;
        long t = mc.level.getGameTime();
        if (plunge) {
            if (t < plungeCd || !me.onGround()) return;
            plungeCd = t + 140;
            Vec3 f = Vec3.directionFromRotation(0, me.getYRot());
            me.setDeltaMovement(f.x * 0.45, 0.95, f.z * 0.45);
            plungeTicks = 1;
            EmberFx.plungeLocal(me);
            FxNet.move(true, f.x, f.z);
        } else {
            if (t < dashCd) return;
            dashCd = t + 45;
            // basılı yön tuşlarına göre (yoksa bakılan yöne) atıl
            float fw = me.input.forwardImpulse, st = me.input.leftImpulse;
            Vec3 f = Vec3.directionFromRotation(0, me.getYRot());
            Vec3 l = new Vec3(f.z, 0, -f.x);
            Vec3 d = f.scale(fw).add(l.scale(st));
            if (d.lengthSqr() < 0.01) d = f;
            dashDir = d.normalize();
            dashTicks = 5;
            EmberFx.dashLocal(me);
            FxNet.move(false, dashDir.x, dashDir.z);
        }
    }

    // ------------------------------------------------------------------ üçüncü şahıs: vücut duruşları

    public static final IArmPoseTransformer POSE_FN = EmberClient::pose;

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** Savuruş eğrisi: önce gerilme (0-0.22), sonra hızlı vuruş (0.22-0.6), sonra yavaşça toparlanma. */
    private static float[] curve(float p) {
        if (p < 0.22F) return new float[]{0, smooth(p / 0.22F)};
        if (p < 0.6F) return new float[]{ease((p - 0.22F) / 0.38F), 1};
        return new float[]{1, 1 - smooth((p - 0.6F) / 0.4F) * 0.6F};
    }

    private static void twist(HumanoidModel<?> m, float yaw) {
        m.body.yRot = yaw;
        m.rightArm.z = Mth.sin(yaw) * 5F;
        m.rightArm.x = -Mth.cos(yaw) * 5F;
        m.leftArm.z = -Mth.sin(yaw) * 5F;
        m.leftArm.x = Mth.cos(yaw) * 5F;
    }

    private static void pose(HumanoidModel<?> m, LivingEntity e, HumanoidArm arm) {
        float s = arm == HumanoidArm.RIGHT ? 1 : -1;
        ModelPart main = arm == HumanoidArm.RIGHT ? m.rightArm : m.leftArm, off = arm == HumanoidArm.RIGHT ? m.leftArm : m.rightArm;
        ModelPart legF = arm == HumanoidArm.RIGHT ? m.leftLeg : m.rightLeg, legB = arm == HumanoidArm.RIGHT ? m.rightLeg : m.leftLeg;
        float atk = m.attackTime;
        m.attackTime = 0; // vanilya savuruşu yerine kendi animasyonumuz
        float pt = Minecraft.getInstance().getFrameTime();
        if (e.isUsingItem() && e.getUseItem().is(MythBlades.EMBER.get())) {
            // savunma: kılıç göğsün önünde çapraz, öteki el destekler
            main.xRot = -1.25F;
            main.yRot = -0.8F * s;
            main.zRot = 0.1F * s;
            off.xRot = -1.05F;
            off.yRot = 0.55F * s;
            legF.xRot = -0.25F;
            legB.xRot = 0.3F;
            return;
        }
        if (EmberFx.plunging(e, pt)) {
            boolean falling = e.getDeltaMovement().y < 0.05;
            float x = falling ? -0.35F : -2.95F;
            main.xRot = x;
            off.xRot = x;
            main.yRot = 0.25F * s;
            off.yRot = -0.25F * s;
            legF.xRot = falling ? -0.9F : -0.5F;
            legB.xRot = falling ? 0.5F : 0.9F;
            return;
        }
        if (EmberFx.dashing(e, pt)) {
            // atılış: öne eğik gövde, kılıç geride sürüklenir
            main.xRot = 0.85F;
            main.yRot = 0.35F * s;
            main.zRot = 0.3F * s;
            off.xRot = -0.6F;
            legF.xRot = -0.9F;
            legB.xRot = 0.7F;
            twist(m, -0.25F * s);
            return;
        }
        if (atk > 0) {
            float[] c = curve(atk);
            float w = c[1], k = c[0];
            switch (stage(e)) {
                case 0 -> {
                    // sağdan sola yatay kesik
                    float y = Mth.lerp(k, 1.1F, -1.05F);
                    main.xRot = Mth.lerp(w, main.xRot, Mth.lerp(k, -1.75F, -1.45F));
                    main.yRot = Mth.lerp(w, 0, y) * s;
                    main.zRot = Mth.lerp(w, 0, -0.35F) * s;
                    twist(m, Mth.lerp(w, 0, Mth.lerp(k, 0.55F, -0.65F)) * s);
                    off.xRot = Mth.lerp(w, off.xRot, -0.5F);
                    legF.xRot = Mth.lerp(w * k, legF.xRot, -0.55F);
                    legB.xRot = Mth.lerp(w * k, legB.xRot, 0.45F);
                }
                case 1 -> {
                    // soldan sağa yükselen ters kesik
                    main.xRot = Mth.lerp(w, main.xRot, Mth.lerp(k, -1.05F, -2.2F));
                    main.yRot = Mth.lerp(w, 0, Mth.lerp(k, -1.0F, 1.15F)) * s;
                    main.zRot = Mth.lerp(w, 0, 0.3F) * s;
                    twist(m, Mth.lerp(w, 0, Mth.lerp(k, -0.6F, 0.6F)) * s);
                    off.xRot = Mth.lerp(w, off.xRot, -0.3F);
                    legF.xRot = Mth.lerp(w * k, legF.xRot, -0.4F);
                    legB.xRot = Mth.lerp(w * k, legB.xRot, 0.5F);
                }
                default -> {
                    // iki elle tepeden çakma
                    float x = Mth.lerp(w, main.xRot, Mth.lerp(k, -3.0F, -0.45F));
                    main.xRot = x;
                    off.xRot = x;
                    main.yRot = -0.25F * s * w;
                    off.yRot = 0.45F * s * w;
                    legF.xRot = Mth.lerp(w * k, legF.xRot, -0.75F);
                    legB.xRot = Mth.lerp(w * k, legB.xRot, 0.55F);
                }
            }
            return;
        }
        // tetikte bekleyiş: kılıç önde, hafif aşağıda
        main.xRot = main.xRot * 0.6F - 0.35F;
        main.yRot = -0.12F * s;
    }

    // ------------------------------------------------------------------ birinci şahıs

    public static boolean firstPerson(PoseStack ps, LocalPlayer p, HumanoidArm arm, float pt, float equip, float swing) {
        float i = arm == HumanoidArm.RIGHT ? 1 : -1;
        ps.translate(i * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        float t = EmberFx.now(pt);
        if (p.isUsingItem() && p.getUseItem().is(MythBlades.EMBER.get())) {
            float g = smooth((p.getTicksUsingItem() + pt) / 4F);
            ps.translate(-0.42F * i * g, 0.2F * g, 0.1F * g);
            ps.mulPose(Axis.YP.rotationDegrees(-20 * i * g));
            ps.mulPose(Axis.ZP.rotationDegrees(78 * i * g));
            ps.mulPose(Axis.XP.rotationDegrees(-8 * g + (float) Math.sin(t * 0.3) * 1.5F));
            return true;
        }
        if (EmberFx.plunging(p, pt)) {
            boolean falling = p.getDeltaMovement().y < 0.05;
            if (falling) {
                ps.translate(-0.25F * i, 0.05F, -0.2F);
                ps.mulPose(Axis.XP.rotationDegrees(-115));
            } else {
                ps.translate(-0.1F * i, 0.38F, 0.12F);
                ps.mulPose(Axis.XP.rotationDegrees(30));
                ps.mulPose(Axis.ZP.rotationDegrees(15 * i));
            }
            return true;
        }
        if (EmberFx.dashing(p, pt)) {
            ps.translate(0.18F * i, -0.25F, 0.12F);
            ps.mulPose(Axis.ZP.rotationDegrees(-35 * i));
            ps.mulPose(Axis.XP.rotationDegrees(55));
            return true;
        }
        if (swing > 0) {
            float[] c = curve(swing);
            float w = c[1], k = c[0];
            switch (stage(p)) {
                case 0 -> {
                    ps.translate(i * Mth.lerp(k, 0.2F, -0.7F) * w, (0.05F + 0.12F * Mth.sin(k * Mth.PI)) * w, -0.18F * Mth.sin(k * Mth.PI) * w);
                    ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(k, 65, -105) * i * w));
                    ps.mulPose(Axis.ZP.rotationDegrees(68 * i * w));
                    ps.mulPose(Axis.XP.rotationDegrees(-18 * w));
                }
                case 1 -> {
                    ps.translate(i * Mth.lerp(k, -0.6F, 0.25F) * w, Mth.lerp(k, -0.2F, 0.25F) * w, -0.12F * w);
                    ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(k, -95, 75) * i * w));
                    ps.mulPose(Axis.ZP.rotationDegrees(-52 * i * w));
                    ps.mulPose(Axis.XP.rotationDegrees(-10 * w));
                }
                default -> {
                    ps.translate(-0.18F * i * w, Mth.lerp(k, 0.38F, -0.32F) * w, Mth.lerp(k, 0.1F, -0.25F) * w);
                    ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(k, 28, -118) * w));
                }
            }
            return true;
        }
        // nefes alır gibi hafif salınım
        ps.translate(0, Mth.sin(t * 0.08F) * 0.008F, 0);
        return true;
    }
}
