package com.krolasyon.alien.network;

import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.ai.HidingManager;
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

public final class Net {
    private Net() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(AlienMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(HideState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HideState::encode).decoder(HideState::decode).consumerMainThread(HideState::handle).add();
        CHANNEL.messageBuilder(ObjectiveState.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ObjectiveState::encode).decoder(ObjectiveState::decode).consumerMainThread(ObjectiveState::handle).add();
        CHANNEL.messageBuilder(LeaveHiding.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> {}).decoder(b -> new LeaveHiding()).consumerMainThread(LeaveHiding::handle).add();
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    /** server -> client: locker state for the HUD (hidden, breath 0..1, alien right outside). */
    public record HideState(boolean hidden, float breath, boolean danger, boolean holding) {
        void encode(FriendlyByteBuf b) {
            b.writeBoolean(hidden); b.writeFloat(breath); b.writeBoolean(danger); b.writeBoolean(holding);
        }

        static HideState decode(FriendlyByteBuf b) {
            return new HideState(b.readBoolean(), b.readFloat(), b.readBoolean(), b.readBoolean());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.alien.client.ClientState.onHideState(this));
            ctx.get().setPacketHandled(true);
        }
    }

    /** server -> client: current escape objective (stage, fuses inserted, launch countdown ticks). */
    public record ObjectiveState(int stage, int fuses, int countdown) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(stage); b.writeVarInt(fuses); b.writeVarInt(countdown);
        }

        static ObjectiveState decode(FriendlyByteBuf b) {
            return new ObjectiveState(b.readVarInt(), b.readVarInt(), b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.alien.client.ClientState.onObjective(this));
            ctx.get().setPacketHandled(true);
        }
    }

    /** client -> server: jump pressed while inside a locker. */
    public record LeaveHiding() {
        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) HidingManager.exit(p);
            ctx.get().setPacketHandled(true);
        }
    }
}
