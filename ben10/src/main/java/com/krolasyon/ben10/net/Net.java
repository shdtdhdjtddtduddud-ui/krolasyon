package com.krolasyon.ben10.net;

import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.Transform;
import com.krolasyon.ben10.ability.Powers;
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
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Ben10.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Select.class, Select::encode, Select::new, Select::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Use.class, Use::encode, Use::new, Use::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Guard.class, Guard::encode, Guard::new, Guard::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Sync.class, Sync::encode, Sync::new, Sync::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Wear.class, Wear::encode, Wear::new, Wear::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Shake.class, Shake::encode, Shake::new, Shake::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sync(ServerPlayer p) {
        OmniData d = OmniData.get(p);
        d.dirty = false;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), Sync.of(p, d));
    }

    public static void syncTo(ServerPlayer target, Player about) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), Sync.of(about, OmniData.get(about)));
    }

    /** camera shake for every player near a point */
    public static void shake(ServerPlayer origin, double radius, float amount) {
        for (ServerPlayer sp : origin.serverLevel().players()) {
            double d = sp.distanceTo(origin);
            if (d < radius) CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new Shake((float) (amount * (1 - d / radius))));
        }
    }

    // ---- client -> server ----

    /** choose an alien on the dial (-1 = return to human) */
    public record Select(int alien) {
        Select(FriendlyByteBuf b) { this(b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeByte(alien); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) Transform.request(p, alien); });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Use(int slot) {
        Use(FriendlyByteBuf b) { this(b.readByte()); }

        void encode(FriendlyByteBuf b) { b.writeByte(slot); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) Powers.use(p, slot); });
            ctx.get().setPacketHandled(true);
        }
    }

    public record Guard(boolean on) {
        Guard(FriendlyByteBuf b) { this(b.readBoolean()); }

        void encode(FriendlyByteBuf b) { b.writeBoolean(on); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) Powers.guard(p, on); });
            ctx.get().setPacketHandled(true);
        }
    }

    /** take the Omnitrix off the wrist */
    public record Wear(boolean on) {
        Wear(FriendlyByteBuf b) { this(b.readBoolean()); }

        void encode(FriendlyByteBuf b) { b.writeBoolean(on); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null && !on) Transform.unequip(p); });
            ctx.get().setPacketHandled(true);
        }
    }

    // ---- server -> client ----

    public record Sync(int entity, int alien, int ability, int abilityTick, boolean blocking, int flags, int timeLeft, int recharge,
                       int target, int[] cd, int[] cdMax) {
        static Sync of(Player p, OmniData d) {
            int flags = d.flags | (d.worn ? OmniData.F_WATCH : 0);
            return new Sync(p.getId(), d.alien == null ? -1 : d.alien.ordinal(), d.ability, d.abilityTick, d.blocking, flags,
                    d.timeLeft, d.recharge, d.targetId, d.cooldown.clone(), d.cooldownMax.clone());
        }

        Sync(FriendlyByteBuf b) {
            this(b.readVarInt(), b.readByte(), b.readByte(), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                    b.readVarInt(), b.readVarIntArray(), b.readVarIntArray());
        }

        void encode(FriendlyByteBuf b) {
            b.writeVarInt(entity);
            b.writeByte(alien);
            b.writeByte(ability);
            b.writeVarInt(abilityTick);
            b.writeBoolean(blocking);
            b.writeVarInt(flags);
            b.writeVarInt(timeLeft);
            b.writeVarInt(recharge);
            b.writeVarInt(target);
            b.writeVarIntArray(cd);
            b.writeVarIntArray(cdMax);
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.ben10.client.ClientHooks.onSync(this)));
            ctx.get().setPacketHandled(true);
        }
    }

    public record Shake(float amount) {
        Shake(FriendlyByteBuf b) { this(b.readFloat()); }

        void encode(FriendlyByteBuf b) { b.writeFloat(amount); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.ben10.client.ClientHooks.shake(amount)));
            ctx.get().setPacketHandled(true);
        }
    }
}
