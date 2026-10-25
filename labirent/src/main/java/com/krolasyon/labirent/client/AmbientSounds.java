package com.krolasyon.labirent.client;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.maze.Dims;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client soundscape of the Glade and the maze: birds by day and crickets by night in the Glade, wind howling
 * through the corridors, distant Griever cries after dark, a tension drone and a heartbeat when one is close.
 */
@Mod.EventBusSubscriber(modid = Labirent.MODID, value = Dist.CLIENT)
public final class AmbientSounds {
    private AmbientSounds() {}

    private static int birds = 100, crickets = 60, wind = 80, cry = 300, heart, tension = 400;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null || mc.isPaused() || mc.level.dimension() != Dims.MAZE) return;
        RandomSource r = p.getRandom();
        long t = mc.level.getDayTime() % 24000L;
        boolean night = t >= 12800 && t < 23200;
        boolean glade = Glade.inside(p.getX(), p.getZ());
        boolean grey = ClientState.sunGone();

        if (glade) {
            if (!night && !grey && --birds <= 0) {
                birds = 120 + r.nextInt(260);
                around(mc, ModSounds.BIRDS.get(), 0.35F, 0.9F + r.nextFloat() * 0.25F, 14, r);
            }
            if (night && --crickets <= 0) {
                crickets = 70 + r.nextInt(80);
                around(mc, ModSounds.CRICKETS.get(), 0.25F, 0.95F + r.nextFloat() * 0.1F, 10, r);
            }
        } else if (--wind <= 0) {
            wind = 160 + r.nextInt(300);
            around(mc, ModSounds.WIND_GUST.get(), 0.45F + r.nextFloat() * 0.3F, 0.85F + r.nextFloat() * 0.3F, 12, r);
        }
        if ((night || grey) && --cry <= 0) {
            cry = 400 + r.nextInt(900);
            around(mc, ModSounds.DISTANT_CRY.get(), glade ? 0.35F : 0.6F, 0.8F + r.nextFloat() * 0.35F, 40, r);
        }
        // a Griever close by: heartbeat that quickens as it gets nearer, plus a low drone
        List<GrieverEntity> near = mc.level.getEntitiesOfClass(GrieverEntity.class, p.getBoundingBox().inflate(18));
        double best = Double.MAX_VALUE;
        for (GrieverEntity g : near) if (g.isAlive()) best = Math.min(best, g.distanceTo(p));
        if (best < 18) {
            float closeness = (float) (1 - best / 18);
            if (--heart <= 0) {
                heart = (int) (26 - closeness * 14);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F + closeness * 0.15F, 0.25F + closeness * 0.75F));
            }
            if (--tension <= 0) {
                tension = 150;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.TENSION.get(), 1.0F, 0.35F + closeness * 0.4F));
            }
        } else if (tension < 100) tension = 100;
    }

    private static void around(Minecraft mc, SoundEvent ev, float vol, float pitch, double dist, RandomSource r) {
        Player p = mc.player;
        double a = r.nextDouble() * Math.PI * 2;
        double x = p.getX() + Math.cos(a) * dist, z = p.getZ() + Math.sin(a) * dist;
        mc.getSoundManager().play(new SimpleSoundInstance(ev, SoundSource.AMBIENT, vol, pitch, r, x, p.getY() + 3, z));
    }
}
