package com.krolasyon.bosses.net;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class Net {
    private Net() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KrolasyonBosses.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CH.registerMessage(id++, SyncMsg.class, SyncMsg::encode, SyncMsg::decode, SyncMsg::handle);
    }

    public static void syncTo(ServerPlayer sp) {
        CH.send(PacketDistributor.PLAYER.with(() -> sp), new SyncMsg(com.krolasyon.bosses.faction.PlayerData.copyForSync(sp)));
    }

    // ------------------------------------------------------------------ player data sync
    public record SyncMsg(CompoundTag data) {
        public static void encode(SyncMsg m, FriendlyByteBuf b) { b.writeNbt(m.data); }

        public static SyncMsg decode(FriendlyByteBuf b) {
            CompoundTag t = b.readNbt();
            return new SyncMsg(t == null ? new CompoundTag() : t);
        }

        public static void handle(SyncMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.ClientHooks.sync(m.data)));
            ctx.get().setPacketHandled(true);
        }
    }
}
