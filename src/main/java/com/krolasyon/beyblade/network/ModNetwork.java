package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.LibraBeyblade;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            com.krolasyon.beyblade.compat.V.rl(LibraBeyblade.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    /** command ids sent from the key bindings */
    public static final int CMD_RECALL = 0;   // 1..5 = abilities
    public static final int CMD_ORDER = 20;   // 20 + order id
    public static final int CMD_MODE_CYCLE = 39, CMD_MODE = 40;   // 40 + mode id
    public static final int CMD_SPIRIT = 50;

    public static void register() {
        CHANNEL.registerMessage(0, Command.class, Command::encode, Command::decode, Command::handle);
    }

    public static void sendCommand(int cmd) {
        CHANNEL.sendToServer(new Command(cmd));
    }

    public record Command(int cmd) {
        static void encode(Command m, FriendlyByteBuf buf) { buf.writeByte(m.cmd); }

        static Command decode(FriendlyByteBuf buf) { return new Command(buf.readByte()); }

        static void handle(Command m, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> CommandHandler.handle(c.getSender(), m.cmd));
            c.setPacketHandled(true);
        }
    }
}
