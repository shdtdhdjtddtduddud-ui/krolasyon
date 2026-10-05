package com.krolasyon.sololeveling.client;

import com.krolasyon.sololeveling.client.screen.*;
import com.krolasyon.sololeveling.item.SLArmorItem;
import com.krolasyon.sololeveling.system.HunterData;
import com.krolasyon.sololeveling.system.Sys;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.ArrayList;
import java.util.List;

/** Client entry points called from packets, plus client-side state (System popups, screen effects). */
public final class ClientHooks {
    public static final HunterData DATA = new HunterData();
    public static final List<Popup> POPUPS = new ArrayList<>();
    public static int ariseTicks, regionTicks, regionIcon, shakeTicks, tintTicks, tintMax;
    public static float tintR, tintG, tintB;

    public record Popup(int kind, Component title, Component body, int color, long start) {}

    private ClientHooks() {}

    public static void sync(CompoundTag t) {
        DATA.load(t);
        DATA.dirty = false;
    }

    public static void notify(int kind, Component title, Component body, int color) {
        long now = System.currentTimeMillis();
        POPUPS.add(new Popup(kind, title, body, color, now));
        while (POPUPS.size() > 4) POPUPS.remove(0);
    }

    public static void open(String screen, CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        switch (screen) {
            case "awaken" -> mc.setScreen(new AwakenScreen());
            case "dialog" -> mc.setScreen(new DialogScreen(data));
            case "shop" -> mc.setScreen(new ShopScreen(data));
            case "news" -> mc.setScreen(new NewsScreen(data));
            case "map" -> mc.setScreen(new MapScreen(data));
            case "close" -> mc.setScreen(null);
            default -> {}
        }
    }

    public static void fx(String kind, double x, double y, double z, int arg) {
        Minecraft mc = Minecraft.getInstance();
        switch (kind) {
            case "arise" -> {
                ariseTicks = 50;
                shakeTicks = 10;
                if (mc.level != null) {
                    for (int i = 0; i < 80; i++) {
                        double a = i * Math.PI * 2 / 80;
                        mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SQUID_INK, x + Math.cos(a) * 1.5, y + 0.1, z + Math.sin(a) * 1.5, 0, 0.15, 0);
                    }
                }
            }
            case "shake" -> shakeTicks = arg;
            case "tint" -> {
                tintR = (float) x;
                tintG = (float) y;
                tintB = (float) z;
                tintTicks = tintMax = arg;
            }
            case "region" -> {
                regionTicks = 80;
                regionIcon = arg;
            }
            default -> {}
        }
    }

    public static void playClick() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.4F, 0.25F));
    }

    public static IClientItemExtensions armorExtensions() {
        return com.krolasyon.sololeveling.client.render.ArmorModels.extensions();
    }

    public static Component t(String key, Object... args) { return Sys.t(key, args); }
}
