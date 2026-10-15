package com.krolasyon.alien.client;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.network.Net;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Atmosphere: station detection, fog, ambient station noises, heartbeat, locker controls. */
@Mod.EventBusSubscriber(modid = AlienMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    public static boolean inStation;
    public static float stationBlend;
    /** 0..1 how close and active the nearest hunting alien is. */
    public static float threat;
    public static float threatSmooth;
    private static int nextAmbient = 200;
    private static int nextBeat;

    private static boolean isModBlock(Level level, BlockPos p) {
        ResourceLocation k = ForgeRegistries.BLOCKS.getKey(level.getBlockState(p).getBlock());
        return k != null && AlienMod.MODID.equals(k.getNamespace());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        Level level = mc.level;
        RandomSource r = level.random;

        if (p.tickCount % 10 == 0) {
            boolean found = false;
            BlockPos b = p.blockPosition();
            for (int i = 1; i <= 9 && !found; i++) found = isModBlock(level, b.above(i));
            if (!found) found = isModBlock(level, b.below());
            inStation = found;
        }
        stationBlend += ((inStation ? 1F : 0F) - stationBlend) * 0.05F;

        // nearest active alien
        float t = 0F;
        XenomorphEntity near = null;
        for (XenomorphEntity x : level.getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(24))) {
            float d = x.distanceTo(p);
            float v = (1F - d / 24F) * (x.isHunting() ? 1F : x.isHidden() ? 0.35F : 0.55F);
            if (v > t) { t = v; near = x; }
        }
        if (ClientState.hidden && ClientState.danger) t = Math.max(t, 0.9F);
        threat = t;
        threatSmooth += (threat - threatSmooth) * 0.08F;

        // heartbeat when the hunter is close
        if (threat > 0.35F && --nextBeat <= 0) {
            nextBeat = (int) (34 - threat * 20);
            p.playSound(ModSounds.HEARTBEAT.get(), 0.4F + threat * 0.6F, 1.0F);
        }

        // ambient station life
        if (inStation && --nextAmbient <= 0) {
            nextAmbient = 140 + r.nextInt(420);
            SoundEvent s = switch (r.nextInt(6)) {
                case 0, 1 -> ModSounds.STATION_HUM.get();
                case 2 -> ModSounds.STATION_CREAK.get();
                case 3 -> ModSounds.STATION_VENT.get();
                case 4 -> ModSounds.STATION_ALARM.get();
                default -> ModSounds.STATION_CREAK.get();
            };
            double a = r.nextDouble() * Math.PI * 2, dist = 6 + r.nextDouble() * 14;
            level.playLocalSound(p.getX() + Math.cos(a) * dist, p.getY() + 2, p.getZ() + Math.sin(a) * dist, s, SoundSource.AMBIENT,
                    0.5F + r.nextFloat() * 0.4F, 0.85F + r.nextFloat() * 0.3F, false);
        }

        Hud.trackerTick();

        // locker: jump to get out
        if (ClientState.hidden) {
            while (mc.options.keyJump.consumeClick()) Net.CHANNEL.sendToServer(new Net.LeaveHiding());
        }
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!ClientState.hidden) return;
        var in = e.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (stationBlend < 0.02F || e.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) return;
        float far = Math.min(e.getFarPlaneDistance(), net.minecraft.util.Mth.lerp(stationBlend, e.getFarPlaneDistance(), 30F - threatSmooth * 8F));
        e.setFarPlaneDistance(far);
        e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), far * 0.05F));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (stationBlend < 0.02F) return;
        float k = stationBlend;
        e.setRed(net.minecraft.util.Mth.lerp(k, e.getRed(), 0.035F + threatSmooth * 0.03F));
        e.setGreen(net.minecraft.util.Mth.lerp(k, e.getGreen(), 0.045F));
        e.setBlue(net.minecraft.util.Mth.lerp(k, e.getBlue(), 0.04F));
    }
}
