package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        while (ClientSetup.RECALL_KEY.consumeClick()) ModNetwork.sendCommand(ModNetwork.CMD_RECALL);
        while (ClientSetup.SPIRIT_KEY.consumeClick()) ModNetwork.sendCommand(ModNetwork.CMD_SPIRIT);
        while (ClientSetup.MODE_KEY.consumeClick()) ModNetwork.sendCommand(ModNetwork.CMD_MODE_CYCLE);
        while (ClientSetup.COMMAND_KEY.consumeClick()) if (mc.screen == null) mc.setScreen(new CommandScreen());
        for (int i = 0; i < ClientSetup.ABILITY_KEYS.length; i++) {
            while (ClientSetup.ABILITY_KEYS[i].consumeClick()) ModNetwork.sendCommand(i + 1);
        }
    }
}
