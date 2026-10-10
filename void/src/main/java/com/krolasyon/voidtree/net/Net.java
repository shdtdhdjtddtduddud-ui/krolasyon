package com.krolasyon.voidtree.net;

import com.krolasyon.voidtree.Fx;
import com.krolasyon.voidtree.SkillLogic;
import com.krolasyon.voidtree.VoidCapability;
import com.krolasyon.voidtree.VoidTree;
import net.minecraft.nbt.CompoundTag;
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

import java.util.Optional;
import java.util.function.Supplier;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(VoidTree.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Cast.class, Cast::encode, Cast::new, Cast::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Unlock.class, Unlock::encode, Unlock::new, Unlock::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Assign.class, Assign::encode, Assign::new, Assign::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Sync.class, Sync::encode, Sync::new, Sync::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Anim.class, Anim::encode, Anim::new, Anim::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Fx.Packet.class, Fx.Packet::encode, Fx.Packet::new, Fx.Packet::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sync(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Sync(VoidCapability.get(p).save()));
    }

    public static void anim(Entity e, int skill) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), new Anim(e.getId(), skill));
    }

    // ---- client -> server ----

    public record Cast(int slot) {
        Cast(FriendlyByteBuf b) { this(b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeByte(slot); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) SkillLogic.castSlot(p, slot); });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Unlock(int skill) {
        Unlock(FriendlyByteBuf b) { this(b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeByte(skill); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) SkillLogic.unlock(p, skill); });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Assign(int slot, int skill) {
        Assign(FriendlyByteBuf b) { this(b.readByte(), b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeByte(slot); b.writeByte(skill); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) SkillLogic.assign(p, slot, skill); });
            ctx.get().setPacketHandled(true);
        }
    }

    // ---- server -> client ----

    public record Sync(CompoundTag tag) {
        Sync(FriendlyByteBuf b) { this(b.readNbt()); }

        void encode(FriendlyByteBuf b) { b.writeNbt(tag); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.voidtree.client.ClientHooks.onSync(tag)));
            ctx.get().setPacketHandled(true);
        }
    }

    public record Anim(int entity, int skill) {
        Anim(FriendlyByteBuf b) { this(b.readVarInt(), b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeVarInt(entity); b.writeByte(skill); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.voidtree.client.ClientHooks.onAnim(entity, skill)));
            ctx.get().setPacketHandled(true);
        }
    }
}
