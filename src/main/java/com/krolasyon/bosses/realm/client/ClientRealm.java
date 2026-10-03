package com.krolasyon.bosses.realm.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.item.ManaUser;
import com.krolasyon.bosses.realm.net.RealmNet;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** client mirror of the player's realm data, the mana bar and the dash key */
@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRealm {
    private ClientRealm() {}

    public static RealmData data = new RealmData();
    private static float shownMana = RealmData.MAX_MANA;
    private static int manaVisible;

    public static final KeyMapping DASH = new KeyMapping("key.krolasyonbosses.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key.categories.krolasyonbosses");

    public static void receive(CompoundTag tag) {
        RealmData d = new RealmData();
        d.load(tag);
        data = d;
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(DASH);
        MinecraftForge.EVENT_BUS.addListener(ClientRealm::clientTick);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), "mana", (gui, g, partial, w, h) -> renderMana(g, w, h));
    }

    private static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (DASH.consumeClick()) RealmNet.CHANNEL.sendToServer(new RealmNet.Dash());
        shownMana += (data.mana - shownMana) * 0.3F;
        boolean show = data.mana < RealmData.MAX_MANA - 0.5F || Realm.inRealm(mc.player.level())
                || mc.player.getMainHandItem().getItem() instanceof ManaUser || mc.player.getOffhandItem().getItem() instanceof ManaUser;
        manaVisible = show ? 60 : Math.max(0, manaVisible - 1);
    }

    private static void renderMana(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (manaVisible <= 0 || mc.options.hideGui || mc.player == null || mc.player.isSpectator() || mc.gameMode == null
                || !mc.gameMode.canHurtPlayer()) return;
        int x = w / 2 + 10, y = h - 49;
        if (mc.player.getAirSupply() < mc.player.getMaxAirSupply() || mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) y -= 10;
        int bw = 81;
        float frac = Mth.clamp(shownMana / RealmData.MAX_MANA, 0F, 1F);
        g.fill(x - 1, y - 1, x + bw + 1, y + 5, 0xC0100418);
        g.fill(x, y, x + bw, y + 4, 0xFF1A0F33);
        int fill = (int) (bw * frac);
        for (int i = 0; i < fill; i++) {
            float t = i / (float) bw;
            int r = (int) Mth.lerp(t, 70, 150), gg = (int) Mth.lerp(t, 60, 120), b = 255;
            g.fill(x + i, y, x + i + 1, y + 4, 0xFF000000 | r << 16 | gg << 8 | b);
        }
        g.fill(x, y, x + fill, y + 1, 0x60FFFFFF);
        String s = String.valueOf((int) data.mana);
        g.drawString(mc.font, Component.literal(s), x + bw - mc.font.width(s), y - 9, 0xFF9AB0FF, true);
    }
}
