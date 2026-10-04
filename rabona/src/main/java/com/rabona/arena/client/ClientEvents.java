package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rabona.arena.RabonaArena;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = RabonaArena.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final Set<LivingEntity> PUSHED = Collections.newSetFromMap(new WeakHashMap<>());
    public static float shake;

    private ClientEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ClientState.clientTicks++;
        if (ClientState.bannerTicks > 0) ClientState.bannerTicks--;
        shake *= 0.86f;
        ClientInput.tick();
        AutoTest.tick();
    }

    /** Takla, plonjon, kayma gibi tum vucut hareketleri. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pre(RenderLivingEvent.Pre<?, ?> e) {
        if (e.isCanceled()) return;
        LivingEntity le = e.getEntity();
        float age = le.tickCount + e.getPartialTick();
        float[] r = ClientAnims.root(le, age);
        if (r == null) return;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.add(le);
        float yaw = Mth.rotLerp(e.getPartialTick(), le.yBodyRotO, le.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.translate(-r[3], r[4], r[5]);
        ps.translate(0, r[6], 0);
        ps.mulPose(Axis.XP.rotationDegrees(r[0]));
        ps.mulPose(Axis.YP.rotationDegrees(r[1]));
        ps.mulPose(Axis.ZP.rotationDegrees(r[2]));
        ps.translate(0, -r[6], 0);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void post(RenderLivingEvent.Post<?, ?> e) {
        if (PUSHED.remove(e.getEntity())) e.getPoseStack().popPose();
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (shake < 0.01f) return;
        float t = (ClientState.clientTicks + (float) e.getPartialTick()) * 1.7f;
        e.setYaw(e.getYaw() + Mth.sin(t * 1.3f) * shake * 2.2f);
        e.setPitch(e.getPitch() + Mth.cos(t * 1.7f) * shake * 1.6f);
        e.setRoll(e.getRoll() + Mth.sin(t * 0.9f) * shake * 1.2f);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientAnims.clear();
        ClientState.match = null;
        ClientState.pitch = null;
        ClientState.stats = null;
        ClientState.feed.clear();
        ClientState.bannerTicks = 0;
    }

    static void addShake(float s) { shake = Math.min(1.5f, shake + s); }

    static boolean near(LivingEntity e, double r) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.distanceTo(e) < r;
    }
}
