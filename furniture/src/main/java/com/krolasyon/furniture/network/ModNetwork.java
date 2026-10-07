package com.krolasyon.furniture.network;

import com.krolasyon.furniture.FurnitureMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FurnitureMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, PianoNotePacket.class, PianoNotePacket::encode, PianoNotePacket::decode, PianoNotePacket::handle);
        CHANNEL.registerMessage(id++, PianoAnimPacket.class, PianoAnimPacket::encode, PianoAnimPacket::decode, PianoAnimPacket::handle);
        CHANNEL.registerMessage(id++, ChalkboardEditPacket.class, ChalkboardEditPacket::encode, ChalkboardEditPacket::decode, ChalkboardEditPacket::handle);
    }
}
