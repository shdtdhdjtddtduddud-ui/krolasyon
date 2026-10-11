package com.krolasyon.omnitrix.net;

import com.krolasyon.omnitrix.Fx;
import com.krolasyon.omnitrix.OmniCapability;
import com.krolasyon.omnitrix.OmniData;
import com.krolasyon.omnitrix.Omnitrix;
import com.krolasyon.omnitrix.Powers;
import com.krolasyon.omnitrix.Transform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Omnitrix.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Action.class, Action::encode, Action::new, Action::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Sync.class, Sync::encode, Sync::new, Sync::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Look.class, Look::encode, Look::new, Look::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Fx.Packet.class, Fx.Packet::encode, Fx.Packet::new, Fx.Packet::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** owner gets the full state, everyone tracking gets the appearance */
    public static void sync(ServerPlayer p) {
        OmniData d = OmniCapability.get(p);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Sync(d.save()));
        appearance(p);
    }

    public static void appearance(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new Look(p.getId(), OmniCapability.get(p).appearance()));
    }

    public static void appearanceTo(ServerPlayer to, Player of) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> to), new Look(of.getId(), OmniCapability.get(of).appearance()));
    }

    /** client -> server: 0 = transform(arg alien), 1 = revert, 2 = ability(arg slot), 3 = block(arg 0/1), 4 = activate watch */
    public record Action(int kind, int arg) {
        public static final int TRANSFORM = 0, REVERT = 1, ABILITY = 2, BLOCK = 3, ACTIVATE = 4;

        Action(FriendlyByteBuf b) { this(b.readByte(), b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeByte(kind); b.writeByte(arg); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p == null) return;
                switch (kind) {
                    case TRANSFORM -> Transform.transform(p, arg);
                    case REVERT -> Transform.revert(p, false);
                    case ABILITY -> Powers.use(p, arg);
                    case BLOCK -> Powers.setBlocking(p, arg != 0);
                    case ACTIVATE -> Transform.activate(p);
                    default -> {}
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Sync(CompoundTag tag) {
        Sync(FriendlyByteBuf b) { this(b.readNbt()); }

        void encode(FriendlyByteBuf b) { b.writeNbt(tag); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.omnitrix.client.ClientState.onSync(tag)));
            ctx.get().setPacketHandled(true);
        }
    }

    public record Look(int entity, CompoundTag tag) {
        Look(FriendlyByteBuf b) { this(b.readVarInt(), b.readNbt()); }

        void encode(FriendlyByteBuf b) { b.writeVarInt(entity); b.writeNbt(tag); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.omnitrix.client.ClientState.onLook(entity, tag)));
            ctx.get().setPacketHandled(true);
        }
    }
}
