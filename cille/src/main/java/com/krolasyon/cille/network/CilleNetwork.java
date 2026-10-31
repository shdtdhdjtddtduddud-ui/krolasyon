package com.krolasyon.cille.network;

import com.krolasyon.cille.CilleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CilleNetwork {
    private CilleNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CilleMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.messageBuilder(CommandPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CommandPacket::encode)
                .decoder(CommandPacket::new)
                .consumerMainThread(CommandPacket::handle)
                .add();
    }
}
