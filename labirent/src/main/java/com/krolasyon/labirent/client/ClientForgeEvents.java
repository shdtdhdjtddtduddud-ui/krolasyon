package com.krolasyon.labirent.client;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.client.screen.CaptainScreen;
import com.krolasyon.labirent.client.screen.JournalScreen;
import com.krolasyon.labirent.client.screen.MapScreen;
import com.krolasyon.labirent.maze.Dims;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keys, camera shake and the grey sky. */
@Mod.EventBusSubscriber(modid = Labirent.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ClientState.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        while (Keys.MAP.consumeClick()) mc.setScreen(new MapScreen(false));
        while (Keys.CAPTAIN.consumeClick()) mc.setScreen(new CaptainScreen());
        while (Keys.JOURNAL.consumeClick()) mc.setScreen(JournalScreen.create());
        while (Keys.MINIMAP.consumeClick()) ClientState.minimap = !ClientState.minimap;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (ClientState.shakeTicks <= 0 || ClientState.shakeTotal <= 0) return;
        float t = ClientState.shakeTicks - (float) e.getPartialTick();
        float s = ClientState.shake * Math.max(0, t / ClientState.shakeTotal) * 2.2F;
        float time = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + (float) e.getPartialTick();
        e.setPitch(e.getPitch() + Mth.sin(time * 2.7F) * s);
        e.setYaw(e.getYaw() + Mth.cos(time * 2.1F) * s * 0.7F);
        e.setRoll(e.getRoll() + Mth.sin(time * 3.3F) * s * 0.5F);
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Dims.MAZE || !ClientState.sunGone()) return;
        float grey = 0.42F;
        e.setRed(Mth.lerp(0.85F, e.getRed(), grey));
        e.setGreen(Mth.lerp(0.85F, e.getGreen(), grey));
        e.setBlue(Mth.lerp(0.85F, e.getBlue(), grey + 0.02F));
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Dims.MAZE || !ClientState.sunGone()) return;
        e.setNearPlaneDistance(e.getNearPlaneDistance() * 0.3F);
        e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), 72F));
        e.setCanceled(true);
    }
}
