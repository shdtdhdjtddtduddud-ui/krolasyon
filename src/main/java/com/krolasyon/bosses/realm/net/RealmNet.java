package com.krolasyon.bosses.realm.net;

import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.story.Politics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class RealmNet {
    private RealmNet() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Realm.rl("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(Sync.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Sync::encode).decoder(Sync::decode).consumerMainThread(Sync::handle).add();
        CHANNEL.messageBuilder(OpenEnvoy.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenEnvoy::encode).decoder(OpenEnvoy::decode).consumerMainThread(OpenEnvoy::handle).add();
        CHANNEL.messageBuilder(EnvoyAction.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EnvoyAction::encode).decoder(EnvoyAction::decode).consumerMainThread(EnvoyAction::handle).add();
        CHANNEL.messageBuilder(Dash.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Dash::encode).decoder(Dash::decode).consumerMainThread(Dash::handle).add();
    }

    public static void sync(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Sync(RealmData.get(p).write()));
    }

    public static void openEnvoy(ServerPlayer p, int entityId, int faction, Component reply) {
        sync(p);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenEnvoy(entityId, faction, reply));
    }

    // ------------------------------------------------------------------ messages
    public record Sync(CompoundTag tag) {
        static void encode(Sync m, FriendlyByteBuf b) { b.writeNbt(m.tag); }

        static Sync decode(FriendlyByteBuf b) { return new Sync(b.readNbt()); }

        static void handle(Sync m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.realm.client.ClientRealm.receive(m.tag));
        }
    }

    public record OpenEnvoy(int entityId, int faction, Component reply) {
        static void encode(OpenEnvoy m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId);
            b.writeVarInt(m.faction);
            b.writeComponent(m.reply);
        }

        static OpenEnvoy decode(FriendlyByteBuf b) { return new OpenEnvoy(b.readVarInt(), b.readVarInt(), b.readComponent()); }

        static void handle(OpenEnvoy m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.realm.client.RealmScreens.openEnvoy(m.entityId, m.faction, m.reply));
        }
    }

    public record EnvoyAction(int entityId, int action) {
        public static final int TRIBUTE = 0, QUEST = 1, ALLIANCE = 2;

        static void encode(EnvoyAction m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId);
            b.writeVarInt(m.action);
        }

        static EnvoyAction decode(FriendlyByteBuf b) { return new EnvoyAction(b.readVarInt(), b.readVarInt()); }

        static void handle(EnvoyAction m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) Politics.envoyAction(p, m.entityId, m.action);
        }
    }

    public record Dash() {
        static void encode(Dash m, FriendlyByteBuf b) {}

        static Dash decode(FriendlyByteBuf b) { return new Dash(); }

        static void handle(Dash m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) com.krolasyon.bosses.realm.story.PlayerPowers.dash(p);
        }
    }
}
