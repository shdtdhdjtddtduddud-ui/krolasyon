package com.krolasyon.bosses.client.form;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.form.DemonForm;
import com.krolasyon.bosses.item.HeartbreakerBladeItem;
import com.krolasyon.bosses.network.ModNetwork;
import com.krolasyon.bosses.registry.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Client side of the player boss form: synced state, animation blending inputs, input, sounds, particles and rendering hooks. */
@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DemonClient {
    private DemonClient() {}

    public static final int GHOST_LIFE = 8;

    public static final class Ghost {
        final double x, y, z;
        final float yaw;
        int age;

        Ghost(double x, double y, double z, float yaw) {
            this.x = x; this.y = y; this.z = z; this.yaw = yaw;
        }
    }

    public static final class Data {
        public boolean demon;
        public int anim = -1;
        public float animStart;
        public float run, prevRun, air, prevAir, crouch, prevCrouch;
        public float swingStart = -1;
        public boolean slashLeft;
        boolean lastSwinging;
        int lastSwingTime;
        int stepIdx;
        int airTicks;
        boolean airJumped, lastJump;
        final ArrayDeque<Ghost> ghosts = new ArrayDeque<>();
    }

    private static final Map<Integer, Data> STATES = new HashMap<>();
    public static final int[] COOLDOWN = new int[DemonForm.COUNT];
    public static final int[] COOLDOWN_MAX = new int[DemonForm.COUNT];
    public static DemonPlayerRenderer renderer;

    public static Data get(Entity e) { return STATES.get(e.getId()); }

    public static boolean isDemon(Player p) {
        Data d = STATES.get(p.getId());
        return d != null && d.demon;
    }

    /** the local player's current animation (for the HUD), or -1 */
    public static int localAnim() {
        Player p = Minecraft.getInstance().player;
        Data d = p == null ? null : STATES.get(p.getId());
        return d == null ? -1 : d.anim;
    }

    public static float localAnimTime(float partial) {
        Player p = Minecraft.getInstance().player;
        Data d = p == null ? null : STATES.get(p.getId());
        return d == null ? 0F : (p.tickCount + partial - d.animStart) / 20F;
    }

    // ------------------------------------------------------------------ packets
    public static void onSync(int id, boolean demon) {
        Data d = STATES.computeIfAbsent(id, k -> new Data());
        d.demon = demon;
        if (!demon) {
            d.anim = -1;
            d.ghosts.clear();
        }
    }

    public static void onAnim(int id, int anim) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(id);
        if (e == null) return;
        Data d = STATES.computeIfAbsent(id, k -> new Data());
        d.anim = anim;
        d.animStart = e.tickCount;
        if (anim == DemonForm.ANIM_TRANSFORM) d.demon = true;
    }

    public static void onCooldowns(int[] remaining, int[] max) {
        for (int i = 0; i < COOLDOWN.length && i < remaining.length; i++) {
            COOLDOWN[i] = remaining[i];
            COOLDOWN_MAX[i] = Math.max(1, max[i]);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
        java.util.Arrays.fill(COOLDOWN, 0);
    }

    // ------------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.isPaused()) return;
        for (int i = 0; i < COOLDOWN.length; i++) if (COOLDOWN[i] > 0) COOLDOWN[i]--;
        for (AbstractClientPlayer p : mc.level.players()) {
            Data d = STATES.get(p.getId());
            if (d != null && d.demon) tickPlayer(mc, p, d);
        }
        LocalPlayer lp = mc.player;
        if (lp == null) return;
        boolean demon = isDemon(lp);
        for (int i = 0; i < KeyBinds.ABILITIES.length; i++) {
            while (KeyBinds.ABILITIES[i].consumeClick()) {
                if (!demon) {
                    lp.displayClientMessage(Component.translatable("message.krolasyonbosses.not_transformed"), true);
                } else if (COOLDOWN[i] > 0) {
                    lp.displayClientMessage(Component.translatable("message.krolasyonbosses.cooldown",
                            Component.translatable("ability.krolasyonbosses." + DemonForm.NAMES[i]), String.format("%.1f", COOLDOWN[i] / 20F)), true);
                } else {
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.Ability(i));
                }
            }
        }
        if (demon) tickAirJump(lp, STATES.get(lp.getId()));
    }

    private static void tickAirJump(LocalPlayer lp, Data d) {
        boolean jump = lp.input != null && lp.input.jumping;
        if (lp.onGround() || lp.isInWater() || lp.onClimbable()) d.airJumped = false;
        if (jump && !d.lastJump && !lp.onGround() && !d.airJumped && d.airTicks > 2 && !lp.getAbilities().mayfly
                && !lp.isInWater() && !lp.onClimbable() && !lp.isPassenger() && !lp.isFallFlying()) {
            d.airJumped = true;
            Vec3 v = lp.getDeltaMovement();
            float yaw = lp.getYRot() * Mth.DEG_TO_RAD;
            Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(0.45);
            lp.setDeltaMovement(v.x * 0.6 + f.x, 0.9, v.z * 0.6 + f.z);
            lp.fallDistance = 0;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.AirJump());
        }
        d.lastJump = jump;
    }

    private static void tickPlayer(Minecraft mc, AbstractClientPlayer p, Data d) {
        Level level = p.level();
        RandomSource r = p.getRandom();
        d.prevRun = d.run;
        d.run += ((p.isSprinting() ? 1F : 0F) - d.run) * 0.25F;
        boolean inAir = !p.onGround() && !p.isInWater() && !p.isPassenger() && !p.getAbilities().flying && !p.onClimbable() && !p.isFallFlying();
        d.airTicks = inAir ? d.airTicks + 1 : 0;
        d.prevAir = d.air;
        d.air += ((d.airTicks > 2 ? 1F : 0F) - d.air) * 0.3F;
        d.prevCrouch = d.crouch;
        d.crouch += ((p.isCrouching() ? 1F : 0F) - d.crouch) * 0.35F;

        boolean firstPersonSelf = p == mc.player && mc.options.getCameraType().isFirstPerson();
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));

        // claw swings (left click) alternate between the right and left claw
        if (p.swinging && (!d.lastSwinging || p.swingTime < d.lastSwingTime)) {
            d.slashLeft = !d.slashLeft;
            d.swingStart = p.tickCount;
            level.playLocalSound(p.getX(), p.getY() + 1, p.getZ(), ModSounds.CLAW_SWIPE.get(), SoundSource.PLAYERS, 0.8F, 0.9F + r.nextFloat() * 0.25F, false);
            int side = d.slashLeft ? -1 : 1;
            for (int i = 0; i < 9; i++) {
                double a = (i / 8.0 - 0.5) * Math.PI * 0.8 * side;
                Vec3 v = fwd.yRot((float) a).scale(1.9);
                level.addParticle(BossEntity.dust(i % 2 == 0 ? DemonForm.CRIMSON : DemonForm.SHADOW, 1.0F),
                        p.getX() + v.x, p.getY() + 1.6 - i * 0.08, p.getZ() + v.z, 0, 0, 0);
            }
        }
        d.lastSwinging = p.swinging;
        d.lastSwingTime = p.swingTime;

        // heavy demon footsteps in time with the walk / run cycle
        if (p.onGround() && p.walkAnimation.speed() > 0.15F && !p.isCrouching() && !p.isInWater()) {
            float pos = p.walkAnimation.position();
            int idx = d.run > 0.5F ? (int) Math.floor(pos * 1.3F / 20F / 0.35F) : (int) Math.floor(pos * 1.7F / 20F / 0.6F);
            if (idx != d.stepIdx) {
                d.stepIdx = idx;
                level.playLocalSound(p.getX(), p.getY(), p.getZ(), ModSounds.DEMON_STEP.get(), SoundSource.PLAYERS,
                        d.run > 0.5F ? 0.5F : 0.35F, 0.9F + r.nextFloat() * 0.2F, false);
                if (d.run > 0.5F) level.addParticle(BossEntity.dust(DemonForm.SHADOW, 1.2F), p.getX(), p.getY() + 0.05, p.getZ(), 0, 0, 0);
            }
        }

        // ambient: smouldering shoulders and a beating heart sigil
        if (!firstPersonSelf && !p.isInvisible()) {
            if (r.nextInt(3) == 0) {
                level.addParticle(r.nextInt(3) == 0 ? BossEntity.dust(DemonForm.CRIMSON, 0.8F) : ParticleTypes.SMOKE,
                        p.getX() + (r.nextDouble() - 0.5) * 1.1, p.getY() + 1.7 + r.nextDouble() * 0.8, p.getZ() + (r.nextDouble() - 0.5) * 1.1, 0, 0.02, 0);
            }
            if (r.nextInt(12) == 0) {
                Vec3 h = p.position().add(fwd.scale(0.35)).add(0, 1.55, 0);
                level.addParticle(BossEntity.dust(DemonForm.HEART_PINK, 0.7F), h.x + (r.nextDouble() - 0.5) * 0.3, h.y + (r.nextDouble() - 0.5) * 0.3, h.z + (r.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
            }
        }

        // afterimages during dash / flip / tempest
        for (Ghost g : d.ghosts) g.age++;
        while (!d.ghosts.isEmpty() && d.ghosts.peekFirst().age >= GHOST_LIFE) d.ghosts.pollFirst();
        if (d.anim == DemonForm.DASH || d.anim == DemonForm.ANIM_FLIP || d.anim == DemonForm.TEMPEST || d.anim == DemonForm.JUDGEMENT) {
            float t = (p.tickCount - d.animStart) / 20F;
            boolean moving = p.getDeltaMovement().lengthSqr() > 0.04 || d.anim == DemonForm.TEMPEST;
            if (moving && t > 0.15F && p.tickCount % 2 == 0) {
                d.ghosts.addLast(new Ghost(p.getX(), p.getY(), p.getZ(), p.yBodyRot));
                if (d.ghosts.size() > 6) d.ghosts.pollFirst();
            }
            if (d.anim == DemonForm.DASH && !firstPersonSelf) {
                for (int i = 0; i < 3; i++) level.addParticle(BossEntity.dust(DemonForm.SHADOW, 1.8F), p.getX() + (r.nextDouble() - 0.5) * 1.4,
                        p.getY() + r.nextDouble() * 2.4, p.getZ() + (r.nextDouble() - 0.5) * 1.4, 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ rendering
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        if (renderer == null || !(p instanceof AbstractClientPlayer acp) || !isDemon(p)) return;
        event.setCanceled(true);
        float partial = event.getPartialTick();
        float yaw = Mth.lerp(partial, p.yRotO, p.getYRot());
        renderer.render(acp, yaw, partial, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    /** first person: the demon's thorned claw replaces the hand (and the blade, which it absorbed) */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || renderer == null || !isDemon(p) || p.isInvisible()) return;
        ItemStack stack = event.getItemStack();
        boolean blade = stack.getItem() instanceof HeartbreakerBladeItem;
        if (event.getHand() == InteractionHand.OFF_HAND) {
            if (blade) event.setCanceled(true);
            return;
        }
        if (!stack.isEmpty() && !blade) return;
        if (p.isUsingItem() && p.getUseItem() != stack) return;
        event.setCanceled(true);
        boolean right = p.getMainArm() == HumanoidArm.RIGHT;
        renderClaw(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getEquipProgress(), event.getSwingProgress(), right);
    }

    private static void renderClaw(PoseStack pose, MultiBufferSource buffers, int light, float equip, float swing, boolean right) {
        float f = right ? 1.0F : -1.0F;
        float sq = Mth.sqrt(swing);
        float x = -0.3F * Mth.sin(sq * (float) Math.PI);
        float y = 0.4F * Mth.sin(sq * ((float) Math.PI * 2F));
        float z = -0.4F * Mth.sin(swing * (float) Math.PI);
        pose.pushPose();
        pose.translate(f * (x + 0.64F), y - 0.6F + equip * -0.6F, z - 0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float s1 = Mth.sin(swing * swing * (float) Math.PI);
        float s2 = Mth.sin(sq * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(f * s2 * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(f * s1 * -20.0F));
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pose.translate(f * 5.6F, 0.0F, 0.0F);
        // vanilla arm pivot pushed a little forward so the thorned forearm and claws sit in view
        pose.translate(f * -5.0F / 16.0F, 3.0F / 16.0F, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(-12.0F));
        pose.scale(0.42F, 0.42F, 0.42F);
        ModelPart arm = renderer.getModel().arm(right);
        arm.getAllParts().forEach(ModelPart::resetPose);
        arm.x = 0; arm.y = 0; arm.z = 0;
        arm.xRot = 0; arm.yRot = 0; arm.zRot = 0;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(DemonPlayerRenderer.TEXTURE));
        arm.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        arm.render(pose, buffers.getBuffer(RenderType.eyes(DemonPlayerRenderer.GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        arm.resetPose();
        pose.popPose();
    }
}
