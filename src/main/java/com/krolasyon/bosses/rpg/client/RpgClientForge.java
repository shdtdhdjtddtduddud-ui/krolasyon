package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.client.gui.CharacterScreen;
import com.krolasyon.bosses.rpg.client.gui.JournalScreen;
import com.krolasyon.bosses.rpg.client.gui.MapScreen;
import com.krolasyon.bosses.rpg.net.RpgNet;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Key handling on the Forge bus (client only). */
@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RpgClientForge {
    private RpgClientForge() {}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (RpgKeys.CAST.consumeClick()) RpgNet.toServer(RpgNet.CAST, new CompoundTag());
        while (RpgKeys.NEXT.consumeClick()) { CompoundTag t = new CompoundTag(); t.putInt("Dir", 1); RpgNet.toServer(RpgNet.CYCLE, t); }
        while (RpgKeys.PREV.consumeClick()) { CompoundTag t = new CompoundTag(); t.putInt("Dir", -1); RpgNet.toServer(RpgNet.CYCLE, t); }
        while (RpgKeys.CHARACTER.consumeClick()) { RpgNet.toServer(RpgNet.OPEN, new CompoundTag()); mc.setScreen(new CharacterScreen()); }
        while (RpgKeys.JOURNAL.consumeClick()) { RpgNet.toServer(RpgNet.OPEN, new CompoundTag()); mc.setScreen(new JournalScreen()); }
        while (RpgKeys.MAP.consumeClick()) { RpgNet.toServer(RpgNet.OPEN, new CompoundTag()); mc.setScreen(new MapScreen()); }
    }
}
