package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.compat.V;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Key / command menu commands sent to the server (Forge 1.21.1 networking). */
public final class ModNetwork {
    private ModNetwork() {}

    public static final SimpleChannel CHANNEL = ChannelBuilder.named(V.rl(LibraBeyblade.MODID, "main"))
            .networkProtocolVersion(1).simpleChannel();

    public static final int CMD_RECALL = 0;   // 1..6 = abilities
    public static final int CMD_ORDER = 20;   // 20 + order id
    public static final int CMD_MODE_CYCLE = 39, CMD_MODE = 40;   // 40 + mode id
    public static final int CMD_SPIRIT = 50;

    public static void register() {
        CHANNEL.messageBuilder(Command.class).encoder(Command::encode).decoder(Command::decode).consumerMainThread(Command::handle).add();
    }

    public static void sendCommand(int cmd) {
        CHANNEL.send(new Command(cmd), PacketDistributor.SERVER.noArg());
    }

    public record Command(int cmd) {
        static void encode(Command m, FriendlyByteBuf buf) { buf.writeByte(m.cmd); }

        static Command decode(FriendlyByteBuf buf) { return new Command(buf.readByte()); }

        static void handle(Command m, CustomPayloadEvent.Context c) {
            CommandHandler.handle(c.getSender(), m.cmd);
            c.setPacketHandled(true);
        }
    }
}
