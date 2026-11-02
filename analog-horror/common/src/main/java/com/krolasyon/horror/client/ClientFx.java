package com.krolasyon.horror.client;

import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Client-side dread: VHS static intensity, heartbeat, chase drone, camera shake and jumpscares. */
public final class ClientFx {
    private ClientFx() {}

    /** 0..1, how close/aggressive the nearest Hatman is - drives the overlay */
    public static float intensity, targetIntensity;
    public static int flashTicks, jumpscareTicks, shakeTicks;
    private static int heartbeatTimer, droneTimer, staticTimer, jumpscareCooldown, seenTick = -1000;
    private static long lastTick;
    private static final RandomSource RNG = RandomSource.create();

    static void onHatmanTick(HatmanEntity e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || e.isDeadOrDying()) return;
        double d = e.distanceTo(p);
        float t = (float) Mth.clamp(1.0 - (d - 3.0) / 34.0, 0, 1);
        int s = e.getState();
        if (s == HatmanEntity.HUNT) t = Math.max(t, 0.45F) + 0.25F;
        if (s == HatmanEntity.FROZEN && e.isObservedBy(p)) {
            t += 0.2F;
            seenTick = e.tickCount;
        }
        targetIntensity = Math.max(targetIntensity, Mth.clamp(t, 0, 1));
        // looking straight at it while it lunges for you
        if (s == HatmanEntity.HUNT && d < 3.6 && jumpscareCooldown == 0 && e.isObservedBy(p)) jumpscare();
    }

    static void onHatmanEvent(HatmanEntity e, byte id) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        double d = e.distanceTo(p);
        if (id == HatmanEntity.EV_STATIC && d < 48) {
            flashTicks = 12;
            play(ModSounds.STATIC.get(), 0.8F, 1.0F);
        } else if (id == HatmanEntity.EV_JUMPSCARE && d < 14 && jumpscareCooldown == 0) {
            jumpscare();
        }
    }

    private static void jumpscare() {
        jumpscareTicks = 16;
        shakeTicks = 30;
        jumpscareCooldown = 600;
        play(ModSounds.JUMPSCARE.get(), 1.0F, 1.0F);
    }

    public static void debugJumpscare() { jumpscareCooldown = 0; jumpscare(); }

    public static void debugFlash(int ticks) {
        flashTicks = ticks;
        play(ModSounds.STATIC.get(), 0.8F, 1.0F);
    }

    private static void play(SoundEvent s, float vol, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        mc.level.playLocalSound(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), s, SoundSource.HOSTILE, vol, pitch, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            intensity = 0;
            return;
        }
        intensity += (targetIntensity - intensity) * (targetIntensity > intensity ? 0.25F : 0.05F);
        targetIntensity = 0;
        if (flashTicks > 0) flashTicks--;
        if (jumpscareTicks > 0) jumpscareTicks--;
        if (shakeTicks > 0) shakeTicks--;
        if (jumpscareCooldown > 0) jumpscareCooldown--;

        if (intensity > 0.3F && --heartbeatTimer <= 0) {
            heartbeatTimer = (int) Mth.lerp((intensity - 0.3F) / 0.7F, 26, 9);
            play(ModSounds.HEARTBEAT.get(), 0.35F + intensity * 0.6F, 1.0F);
        }
        if (intensity > 0.7F && --droneTimer <= 0) {
            droneTimer = 170;
            play(ModSounds.DRONE.get(), 0.5F, 1.0F);
        }
        if (intensity > 0.8F && --staticTimer <= 0) {
            staticTimer = 200 + RNG.nextInt(300);
            if (RNG.nextFloat() < intensity) {
                flashTicks = Math.max(flashTicks, 3);
                play(ModSounds.STATIC.get(), 0.25F + intensity * 0.3F, 0.9F + RNG.nextFloat() * 0.2F);
            }
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        float shake = shakeTicks > 0 ? shakeTicks / 30F * 2.2F : 0F;
        shake += Math.max(0, intensity - 0.75F) * 1.2F;
        if (shake <= 0.001F) return;
        e.setPitch(e.getPitch() + (RNG.nextFloat() - 0.5F) * shake);
        e.setYaw(e.getYaw() + (RNG.nextFloat() - 0.5F) * shake);
        e.setRoll(e.getRoll() + (RNG.nextFloat() - 0.5F) * shake * 1.5F);
    }
}
