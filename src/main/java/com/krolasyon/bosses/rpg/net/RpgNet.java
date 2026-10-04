package com.krolasyon.bosses.rpg.net;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Two generic packets carrying NBT: server → client data/screens and client → server actions. */
public final class RpgNet {
    private RpgNet() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(KrolasyonBosses.MODID, "rpg"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    // server -> client kinds
    public static final byte SYNC = 0, DIALOG = 1, SCREEN = 2, TOAST = 3, CLOSE = 4;
    // client -> server kinds
    public static final byte CAST = 0, CYCLE = 1, STAT = 2, CHOICE = 3, OPEN = 4, SELECT = 5, QUEST_DROP = 6;

    public static void register() {
        CHANNEL.messageBuilder(ToClient.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ToClient::encode).decoder(ToClient::decode).consumerMainThread(ToClient::handle).add();
        CHANNEL.messageBuilder(ToServer.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToServer::encode).decoder(ToServer::decode).consumerMainThread(ToServer::handle).add();
    }

    public static void send(ServerPlayer p, byte kind, CompoundTag tag) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ToClient(kind, tag));
    }

    public static void toServer(byte kind, CompoundTag tag) {
        CHANNEL.sendToServer(new ToServer(kind, tag));
    }

    public record ToClient(byte kind, CompoundTag tag) {
        static void encode(ToClient m, FriendlyByteBuf b) { b.writeByte(m.kind); b.writeNbt(m.tag); }

        static ToClient decode(FriendlyByteBuf b) {
            byte k = b.readByte();
            CompoundTag t = b.readNbt();
            return new ToClient(k, t == null ? new CompoundTag() : t);
        }

        static void handle(ToClient m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.rpg.client.ClientRpg.handle(m.kind, m.tag));
        }
    }

    public record ToServer(byte kind, CompoundTag tag) {
        static void encode(ToServer m, FriendlyByteBuf b) { b.writeByte(m.kind); b.writeNbt(m.tag); }

        static ToServer decode(FriendlyByteBuf b) {
            byte k = b.readByte();
            CompoundTag t = b.readNbt();
            return new ToServer(k, t == null ? new CompoundTag() : t);
        }

        static void handle(ToServer m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) ServerActions.handle(p, m.kind, m.tag);
        }
    }
}
