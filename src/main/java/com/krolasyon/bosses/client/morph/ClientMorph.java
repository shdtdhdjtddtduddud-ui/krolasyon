package com.krolasyon.bosses.client.morph;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.client.render.BeamRenderer;
import com.krolasyon.bosses.item.FormBladeItem;
import com.krolasyon.bosses.morph.Forms;
import com.krolasyon.bosses.morph.Aigoar;
import com.krolasyon.bosses.morph.MorphServer;
import com.krolasyon.bosses.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client side of the transformation: per-player animation state, keys, rendering hooks, beam and ambient fx. */
@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientMorph {
    private ClientMorph() {}

    public static final String CATEGORY = "key.categories.krolasyonbosses";
    public static final KeyMapping[] ABILITY_KEYS = {
            key("ability1", GLFW.GLFW_KEY_R), key("ability2", GLFW.GLFW_KEY_G), key("ability3", GLFW.GLFW_KEY_V),
            key("ability4", GLFW.GLFW_KEY_Z), key("ability5", GLFW.GLFW_KEY_B)};
    public static final KeyMapping TRANSFORM_KEY = key("transform", GLFW.GLFW_KEY_H);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.krolasyonbosses." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    static {
        FormBladeItem.keyNames = () -> {
            String[] s = new String[6];
            for (int i = 0; i < 5; i++) s[i] = ABILITY_KEYS[i].getTranslatedKeyMessage().getString();
            s[5] = TRANSFORM_KEY.getTranslatedKeyMessage().getString();
            return s;
        };
    }

    public static final class CState {
        public boolean morphed;
        public int form = -1;
        public int anim = -1;
        public float animStart;
        public float air, prevAir, rise, prevRise, swim, prevSwim, crouch, prevCrouch, run, prevRun;
        boolean attackAlt;
    }

    private static final Map<Integer, CState> STATES = new ConcurrentHashMap<>();
    static final int[] COOLDOWN = new int[Aigoar.ABILITIES];
    static final int[] COOLDOWN_MAX = Forms.COOLDOWN[0].clone();
    private static boolean jumpWasDown;
    private static boolean doubleJumpSent;

    @Nullable
    public static CState get(int id) {
        CState s = STATES.get(id);
        return s != null && s.morphed ? s : null;
    }

    public static boolean localMorphed() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && get(mc.player.getId()) != null;
    }

    // ------------------------------------------------------------------ packets
    public static void onSync(int id, byte form) {
        CState s = STATES.computeIfAbsent(id, k -> new CState());
        s.morphed = Forms.valid(form);
        s.form = s.morphed ? form : -1;
        if (s.morphed) Aigoar.CLIENT_FORM.put(id, (int) form);
        else {
            Aigoar.CLIENT_FORM.remove(id);
            s.anim = -1;
        }
    }

    public static void onAnim(int id, byte anim) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(id);
        CState s = STATES.computeIfAbsent(id, k -> new CState());
        s.anim = anim;
        s.animStart = e != null ? e.tickCount : 0;

    }

    public static void onCooldowns(int[] remaining, int[] total) {
        for (int i = 0; i < Aigoar.ABILITIES && i < remaining.length; i++) {
            COOLDOWN[i] = remaining[i];
            COOLDOWN_MAX[i] = Math.max(1, total[i]);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        STATES.clear();
        Aigoar.CLIENT_FORM.clear();
        java.util.Arrays.fill(COOLDOWN, 0);
    }

    // ------------------------------------------------------------------ input + state
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null) return;
        while (TRANSFORM_KEY.consumeClick()) ModNetwork.toServer(new ModNetwork.KeyMsg(ModNetwork.KeyMsg.TOGGLE));
        boolean morphed = localMorphed();
        for (int i = 0; i < ABILITY_KEYS.length; i++) {
            while (ABILITY_KEYS[i].consumeClick()) if (morphed) ModNetwork.toServer(new ModNetwork.KeyMsg((byte) i));
        }
        for (int i = 0; i < COOLDOWN.length; i++) if (COOLDOWN[i] > 0) COOLDOWN[i]--;

        boolean jump = mc.options.keyJump.isDown();
        if (me.onGround() || me.isInWater()) doubleJumpSent = false;
        if (morphed && jump && !jumpWasDown && !me.onGround() && !me.isInWater() && !me.getAbilities().flying && !me.onClimbable() && !doubleJumpSent && mc.screen == null) {
            doubleJumpSent = true;
            ModNetwork.toServer(new ModNetwork.KeyMsg(ModNetwork.KeyMsg.DOUBLE_JUMP));
            CState st = get(me.getId());
            if (st != null && st.anim < 0) { st.anim = Aigoar.ANIM_DOUBLE_JUMP; st.animStart = me.tickCount; }
        }
        jumpWasDown = jump;

        for (Player p : mc.level.players()) {
            CState st = get(p.getId());
            if (st == null) continue;
            st.prevAir = st.air; st.prevRise = st.rise; st.prevSwim = st.swim; st.prevCrouch = st.crouch; st.prevRun = st.run;
            boolean airborne = !p.onGround() && !p.isInWater() && !p.getAbilities().flying && !p.isPassenger() && !p.onClimbable();
            double dy = p.getY() - p.yo;
            st.air = approach(st.air, airborne ? 1F : 0F, 0.25F);
            st.rise = approach(st.rise, dy > 0.02 ? 1F : 0F, 0.2F);
            st.swim = approach(st.swim, p.isVisuallySwimming() ? 1F : (p.isInWater() && !p.onGround() ? 0.6F : 0F), 0.15F);
            st.crouch = approach(st.crouch, p.isCrouching() ? 1F : 0F, 0.3F);
            st.run = approach(st.run, p.isSprinting() ? 1F : 0F, 0.18F);
            ambientFx(mc, p, st);
        }
    }

    private static float approach(float v, float goal, float k) {
        return v + (goal - v) * k;
    }

    private static void ambientFx(Minecraft mc, Player p, CState st) {
        if (p.isInvisible() || mc.isPaused()) return;
        var r = p.getRandom();
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        double cos = Mth.cos(yaw), sin = Mth.sin(yaw);
        int f = st.form;
        boolean moving = p.walkAnimation.speed() > 0.4F && p.onGround();
        if (f == Forms.AIGOAR) {
            if (r.nextInt(4) == 0) {
                // water dripping off the liquid claws
                double side = r.nextBoolean() ? 0.5 : -0.5;
                double x = p.getX() - cos * side, z = p.getZ() - sin * side;
                mc.level.addParticle(ParticleTypes.FALLING_WATER, x + r.nextGaussian() * 0.05, p.getY() + 1.0, z + r.nextGaussian() * 0.05, 0, 0, 0);
            }
            if (r.nextInt(12) == 0) mc.level.addParticle(ParticleTypes.GLOW, p.getX() + r.nextGaussian() * 0.4, p.getY() + 1.9, p.getZ() + r.nextGaussian() * 0.4, 0, 0.02, 0);
            if (moving && r.nextInt(3) == 0)
                mc.level.addParticle(ParticleTypes.SPLASH, p.getX() + r.nextGaussian() * 0.3, p.getY() + 0.05, p.getZ() + r.nextGaussian() * 0.3, 0, 0.1, 0);
        } else if (f == Forms.SHADE) {
            if (r.nextInt(3) == 0) mc.level.addParticle(ParticleTypes.SMOKE, p.getX() + r.nextGaussian() * 0.35, p.getY() + 0.2 + r.nextDouble() * 1.6, p.getZ() + r.nextGaussian() * 0.35, 0, 0.01, 0);
            if (r.nextInt(10) == 0) mc.level.addParticle(ParticleTypes.CRIMSON_SPORE, p.getX() + r.nextGaussian() * 0.5, p.getY() + 1.5, p.getZ() + r.nextGaussian() * 0.5, 0, 0, 0);
            if (moving && r.nextInt(3) == 0) mc.level.addParticle(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.1, p.getZ(), 0, 0.01, 0);
        } else {
            // embers drifting off the burning body and weapon
            if (r.nextInt(2) == 0) {
                double side = r.nextBoolean() ? 0.5 : -0.5;
                mc.level.addParticle(r.nextInt(3) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, p.getX() - cos * side + r.nextGaussian() * 0.15,
                        p.getY() + 0.6 + r.nextDouble() * 1.4, p.getZ() - sin * side + r.nextGaussian() * 0.15, 0, 0.03, 0);
            }
            if (r.nextInt(8) == 0) mc.level.addParticle(ParticleTypes.LAVA, p.getX(), p.getY() + 1.2, p.getZ(), 0, 0, 0);
            if (moving && r.nextInt(2) == 0) mc.level.addParticle(ParticleTypes.FLAME, p.getX() + r.nextGaussian() * 0.25, p.getY() + 0.05, p.getZ() + r.nextGaussian() * 0.25, 0, 0.02, 0);
        }
        if (st.anim == Aigoar.ANIM_TRANSFORM) {
            float t = p.tickCount - st.animStart;
            if (t < Aigoar.TRANSFORM_BURST) {
                for (int i = 0; i < 3; i++) {
                    double a = t * 0.5 + i * 2.1;
                    double rr = 1.2 + r.nextDouble() * 0.3;
                    var part = f == Forms.AIGOAR ? ParticleTypes.SPLASH : f == Forms.SHADE ? ParticleTypes.SMOKE : ParticleTypes.FLAME;
                    mc.level.addParticle(part, p.getX() + Math.cos(a) * rr, p.getY() + r.nextDouble() * 2.6, p.getZ() + Math.sin(a) * rr, 0, 0.1, 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onInteractKey(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        CState st = get(mc.player.getId());
        if (st == null || (st.anim >= 0 && st.anim < Aigoar.ANIM_ATTACK_R) || st.anim == Aigoar.ANIM_TRANSFORM) return;
        st.attackAlt = !st.attackAlt;
        st.anim = st.attackAlt ? Aigoar.ANIM_ATTACK_R : Aigoar.ANIM_ATTACK_L;
        st.animStart = mc.player.tickCount;
        ModNetwork.toServer(new ModNetwork.KeyMsg(ModNetwork.KeyMsg.SWING));
    }

    // ------------------------------------------------------------------ rendering hooks
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer p)) return;
        CState st = get(p.getId());
        if (st == null || !FormRenderer.ready(st.form)) return;
        e.setCanceled(true);
        FormRenderer.render(p, st, e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        ClientMorph.CState me = mc.player == null ? null : get(mc.player.getId());
        if (me == null || !FormRenderer.ready(me.form)) return;
        if (e.getHand() != InteractionHand.MAIN_HAND) return;
        e.setCanceled(true);
        HumanoidArm arm = mc.player.getMainArm();
        FormRenderer.renderArm(mc.player, e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), e.getEquipProgress(), e.getSwingProgress(), arm);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean any = false;
        for (Player p : mc.level.players()) {
            CState st = get(p.getId());
            if (st == null) continue;
            float t = p.tickCount + partial - st.animStart;
            boolean beam = st.form == Forms.AIGOAR && st.anim == Aigoar.BEAM && t >= 2 && t <= Aigoar.BEAM_END + 2;
            boolean breath = st.form == Forms.DRAGON && st.anim == 0 && t >= 9 && t <= 46;
            if (!beam && !breath) continue;
            if (!any) {
                ps.pushPose();
                ps.translate(-cam.x, -cam.y, -cam.z);
                any = true;
            }
            if (beam) drawBeam(mc, p, t, partial, ps.last().pose(), buffers.getBuffer(RenderType.lightning()));
            else drawBreath(mc, p, t, partial, ps.last().pose(), buffers.getBuffer(RenderType.lightning()));
        }
        if (any) {
            ps.popPose();
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static void drawBeam(Minecraft mc, Player p, float t, float partial, Matrix4f m, VertexConsumer vc) {
        Vec3 look = p.getViewVector(partial);
        Vec3 origin = p.getEyePosition(partial).add(0, -0.25, 0).add(look.scale(1.1));
        float time = p.tickCount + partial;
        if (t < Aigoar.BEAM_START) {
            // gathering water orb between the claws
            float k = t / Aigoar.BEAM_START;
            float r = 0.12F + 0.38F * k + 0.04F * Mth.sin(time * 1.3F);
            Vec3[] axes = {new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1), new Vec3(0.7, 0.7, 0), new Vec3(0, 0.7, 0.7)};
            for (Vec3 ax : axes) {
                BeamRenderer.beam(m, vc, origin.subtract(ax.scale(r)), origin.add(ax.scale(r)), r * 0.9F, 0.2F, 0.8F, 0.85F, 0.35F);
                BeamRenderer.beam(m, vc, origin.subtract(ax.scale(r * 0.6)), origin.add(ax.scale(r * 0.6)), r * 0.45F, 0.8F, 1F, 1F, 0.6F);
            }
            return;
        }
        float fade = t > Aigoar.BEAM_END ? Mth.clamp(1F - (t - Aigoar.BEAM_END) / 2F, 0F, 1F) : Mth.clamp((t - Aigoar.BEAM_START) / 2F, 0F, 1F);
        Vec3 end = origin.add(look.scale(Aigoar.BEAM_RANGE));
        BlockHitResult bh = mc.level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        float pulse = 1F + 0.12F * Mth.sin(time * 2.1F);
        BeamRenderer.beam(m, vc, origin, end, 0.09F * pulse * fade, 0.85F, 1F, 1F, 0.95F);
        BeamRenderer.beam(m, vc, origin, end, 0.22F * pulse * fade, 0.25F, 0.9F, 0.95F, 0.6F);
        BeamRenderer.beam(m, vc, origin, end, 0.42F * pulse * fade, 0.05F, 0.55F, 0.75F, 0.3F);
        // spiralling water rings around the torrent
        Vec3 d = end.subtract(origin);
        double len = d.length();
        Vec3 dn = d.normalize();
        Vec3 up = Math.abs(dn.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dn.cross(up).normalize();
        Vec3 v = dn.cross(u).normalize();
        int n = (int) (len * 3);
        Vec3 prev = null, prev2 = null;
        for (int i = 0; i <= n; i++) {
            double s = i / 3.0;
            double a = s * 2.2 - time * 0.9;
            double rad = (0.55 + 0.1 * Math.sin(s * 3 + time)) * fade;
            Vec3 q = origin.add(dn.scale(s)).add(u.scale(Math.cos(a) * rad)).add(v.scale(Math.sin(a) * rad));
            Vec3 q2 = origin.add(dn.scale(s)).add(u.scale(Math.cos(a + Math.PI) * rad)).add(v.scale(Math.sin(a + Math.PI) * rad));
            if (prev != null) {
                BeamRenderer.beam(m, vc, prev, q, 0.06F * fade, 0.6F, 1F, 1F, 0.7F);
                BeamRenderer.beam(m, vc, prev2, q2, 0.06F * fade, 0.3F, 0.9F, 1F, 0.6F);
            }
            prev = q;
            prev2 = q2;
        }
        // impact splash disc
        float ir = (0.9F + 0.25F * Mth.sin(time * 1.7F)) * fade;
        BeamRenderer.beam(m, vc, end.subtract(dn.scale(0.05)), end.add(dn.scale(0.05)), ir, 0.4F, 1F, 1F, 0.5F);
        if (mc.level.random.nextInt(2) == 0 && !mc.isPaused())
            mc.level.addParticle(ParticleTypes.SPLASH, end.x, end.y, end.z, mc.level.random.nextGaussian() * 0.2, 0.3, mc.level.random.nextGaussian() * 0.2);
    }

    /** Ejder Nefesi: a roaring cone of dragon fire from the mask */
    private static void drawBreath(Minecraft mc, Player p, float t, float partial, Matrix4f m, VertexConsumer vc) {
        Vec3 look = p.getViewVector(partial);
        Vec3 mouth = p.getEyePosition(partial).add(0, 0.35, 0).add(look.scale(0.9));
        float time = p.tickCount + partial;
        float fade = Mth.clamp((t - 9F) / 3F, 0F, 1F) * Mth.clamp((46F - t) / 4F, 0F, 1F);
        float len = 8.5F * Mth.clamp((t - 9F) / 5F, 0.2F, 1F);
        Vec3 end = mouth.add(look.scale(len));
        BlockHitResult bh = mc.level.clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        Vec3 d = end.subtract(mouth);
        int seg = 10;
        for (int i = 0; i < seg; i++) {
            float k0 = i / (float) seg, k1 = (i + 1) / (float) seg;
            Vec3 a = mouth.add(d.scale(k0)), b = mouth.add(d.scale(k1));
            float w = (0.15F + 1.5F * k1) * (1F + 0.12F * Mth.sin(time * 1.9F + i));
            BeamRenderer.beam(m, vc, a, b, w * fade, 1F, 0.35F + 0.2F * (1 - k1), 0.05F, 0.28F * (1F - k1 * 0.6F));
            BeamRenderer.beam(m, vc, a, b, w * 0.55F * fade, 1F, 0.7F, 0.2F, 0.4F * (1F - k1 * 0.7F));
            BeamRenderer.beam(m, vc, a, b, w * 0.22F * fade, 1F, 0.95F, 0.7F, 0.6F * (1F - k1));
        }
        if (!mc.isPaused()) {
            var r = mc.level.random;
            for (int i = 0; i < 4; i++) {
                Vec3 v = look.add(r.nextGaussian() * 0.12, r.nextGaussian() * 0.08, r.nextGaussian() * 0.12).normalize().scale(0.5 + r.nextDouble() * 0.4);
                mc.level.addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            }
        }
    }

    /** used by the transformation check in shared code paths */
    public static boolean isMorphedClient(Player p) {
        return MorphServer.isMorphed(p);
    }
}
