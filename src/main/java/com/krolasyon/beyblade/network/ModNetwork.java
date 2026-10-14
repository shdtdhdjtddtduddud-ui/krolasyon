package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LibraBeyblade.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

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
            c.enqueueWork(() -> {
                ServerPlayer player = c.getSender();
                if (player == null) return;
                BeybladeEntity bey = BeybladeEntity.findActive(player);
                if (bey == null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.librabeyblade.none_active"), true);
                    return;
                }
                if (m.cmd == CMD_RECALL) bey.recall();
                else if (m.cmd >= CMD_ORDER && m.cmd < CMD_ORDER + BeybladeEntity.ORDER_COUNT) bey.setOrder(m.cmd - CMD_ORDER);
                else if (m.cmd == CMD_SPIRIT) bey.releaseSpirit();
                else if (m.cmd == CMD_MODE_CYCLE) bey.setMode(bey.getMode() + 1);
                else if (m.cmd >= CMD_MODE && m.cmd < CMD_MODE + BeybladeEntity.MODE_COUNT) bey.setMode(m.cmd - CMD_MODE);
                else if (m.cmd >= 1 && m.cmd <= BeybladeEntity.ABILITY_COUNT) bey.tryAbility(m.cmd, true);
            });
            c.setPacketHandled(true);
        }
    }
}
