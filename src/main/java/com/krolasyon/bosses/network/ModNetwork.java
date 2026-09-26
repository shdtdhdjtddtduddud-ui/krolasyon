package com.krolasyon.bosses.network;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.form.DemonForm;
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

/** Packets for the player boss form. */
public final class ModNetwork {
    private ModNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KrolasyonBosses.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(Ability.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Ability::encode).decoder(Ability::decode).consumerMainThread(Ability::handle).add();
        CHANNEL.messageBuilder(AirJump.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AirJump::encode).decoder(AirJump::decode).consumerMainThread(AirJump::handle).add();
        CHANNEL.messageBuilder(FormSync.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormSync::encode).decoder(FormSync::decode).consumerMainThread(FormSync::handle).add();
        CHANNEL.messageBuilder(Anim.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Anim::encode).decoder(Anim::decode).consumerMainThread(Anim::handle).add();
        CHANNEL.messageBuilder(Cooldowns.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Cooldowns::encode).decoder(Cooldowns::decode).consumerMainThread(Cooldowns::handle).add();
    }

    public static void toTrackingAndSelf(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), msg);
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    // ------------------------------------------------------------------ client -> server
    /** the player pressed one of the five ability keys */
    public record Ability(int slot) {
        static void encode(Ability m, FriendlyByteBuf b) { b.writeByte(m.slot); }
        static Ability decode(FriendlyByteBuf b) { return new Ability(b.readByte()); }
        static void handle(Ability m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) DemonForm.tryAbility(p, m.slot);
        }
    }

    /** the player performed a mid-air shadow leap (movement is applied client side) */
    public record AirJump() {
        static void encode(AirJump m, FriendlyByteBuf b) {}
        static AirJump decode(FriendlyByteBuf b) { return new AirJump(); }
        static void handle(AirJump m, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) DemonForm.airJump(p);
        }
    }

    // ------------------------------------------------------------------ server -> client
    public record FormSync(int entityId, boolean demon) {
        static void encode(FormSync m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); b.writeBoolean(m.demon); }
        static FormSync decode(FriendlyByteBuf b) { return new FormSync(b.readVarInt(), b.readBoolean()); }
        static void handle(FormSync m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.form.DemonClient.onSync(m.entityId, m.demon));
        }
    }

    public record Anim(int entityId, int anim) {
        static void encode(Anim m, FriendlyByteBuf b) { b.writeVarInt(m.entityId); b.writeByte(m.anim); }
        static Anim decode(FriendlyByteBuf b) { return new Anim(b.readVarInt(), b.readByte()); }
        static void handle(Anim m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.form.DemonClient.onAnim(m.entityId, m.anim));
        }
    }

    public record Cooldowns(int[] remaining, int[] max) {
        static void encode(Cooldowns m, FriendlyByteBuf b) { b.writeVarIntArray(m.remaining); b.writeVarIntArray(m.max); }
        static Cooldowns decode(FriendlyByteBuf b) { return new Cooldowns(b.readVarIntArray(), b.readVarIntArray()); }
        static void handle(Cooldowns m, Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.form.DemonClient.onCooldowns(m.remaining, m.max));
        }
    }
}
