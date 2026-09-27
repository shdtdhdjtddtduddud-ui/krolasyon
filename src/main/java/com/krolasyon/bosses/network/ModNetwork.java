package com.krolasyon.bosses.network;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.morph.MorphServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Morph state sync (server -> client) and key input (client -> server). */
public final class ModNetwork {
    private ModNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KrolasyonBosses.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(KeyMsg.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(KeyMsg::encode).decoder(KeyMsg::decode).consumerMainThread(KeyMsg::handle).add();
        CHANNEL.messageBuilder(SyncMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncMsg::encode).decoder(SyncMsg::decode).consumerMainThread(SyncMsg::handle).add();
        CHANNEL.messageBuilder(AnimMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AnimMsg::encode).decoder(AnimMsg::decode).consumerMainThread(AnimMsg::handle).add();
        CHANNEL.messageBuilder(CooldownMsg.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CooldownMsg::encode).decoder(CooldownMsg::decode).consumerMainThread(CooldownMsg::handle).add();
    }

    public static void toTrackingAndSelf(Entity e, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), msg);
    }

    public static void toTracking(Entity e, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> e), msg);
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    // ------------------------------------------------------------------ messages
    /** client key press: 0-4 abilities, 5 transform toggle, 6 double jump, 7 claw swing */
    public record KeyMsg(byte action) {
        public static final byte TOGGLE = 5, DOUBLE_JUMP = 6, SWING = 7;

        void encode(FriendlyByteBuf buf) { buf.writeByte(action); }

        static KeyMsg decode(FriendlyByteBuf buf) { return new KeyMsg(buf.readByte()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) MorphServer.handleKey(p, action);
            ctx.get().setPacketHandled(true);
        }
    }

    public record SyncMsg(int entityId, boolean morphed) {
        void encode(FriendlyByteBuf buf) { buf.writeVarInt(entityId); buf.writeBoolean(morphed); }

        static SyncMsg decode(FriendlyByteBuf buf) { return new SyncMsg(buf.readVarInt(), buf.readBoolean()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.morph.ClientMorph.onSync(entityId, morphed));
            ctx.get().setPacketHandled(true);
        }
    }

    public record AnimMsg(int entityId, byte anim) {
        void encode(FriendlyByteBuf buf) { buf.writeVarInt(entityId); buf.writeByte(anim); }

        static AnimMsg decode(FriendlyByteBuf buf) { return new AnimMsg(buf.readVarInt(), buf.readByte()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.morph.ClientMorph.onAnim(entityId, anim));
            ctx.get().setPacketHandled(true);
        }
    }

    public record CooldownMsg(int[] remaining, int[] total) {
        void encode(FriendlyByteBuf buf) { buf.writeVarIntArray(remaining); buf.writeVarIntArray(total); }

        static CooldownMsg decode(FriendlyByteBuf buf) { return new CooldownMsg(buf.readVarIntArray(), buf.readVarIntArray()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.morph.ClientMorph.onCooldowns(remaining, total));
            ctx.get().setPacketHandled(true);
        }
    }
}
