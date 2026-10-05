package com.sololeveling.net;

import com.sololeveling.SoloLeveling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private Net() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SoloLeveling.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++, Packets.Sync.class, Packets.Sync::encode, Packets.Sync::decode, Packets.Sync::handle);
        CHANNEL.registerMessage(id++, Packets.Notify.class, Packets.Notify::encode, Packets.Notify::decode, Packets.Notify::handle);
        CHANNEL.registerMessage(id++, Packets.News.class, Packets.News::encode, Packets.News::decode, Packets.News::handle);
        CHANNEL.registerMessage(id++, Packets.OpenNpc.class, Packets.OpenNpc::encode, Packets.OpenNpc::decode, Packets.OpenNpc::handle);
        CHANNEL.registerMessage(id++, Packets.OpenSystem.class, Packets.OpenSystem::encode, Packets.OpenSystem::decode, Packets.OpenSystem::handle);
        CHANNEL.registerMessage(id++, Packets.Fx.class, Packets.Fx::encode, Packets.Fx::decode, Packets.Fx::handle);
        CHANNEL.registerMessage(id++, Packets.Cooldown.class, Packets.Cooldown::encode, Packets.Cooldown::decode, Packets.Cooldown::handle);
        CHANNEL.registerMessage(id++, Packets.Gates.class, Packets.Gates::encode, Packets.Gates::decode, Packets.Gates::handle);
        CHANNEL.registerMessage(id++, Packets.Action.class, Packets.Action::encode, Packets.Action::decode, Packets.Action::handle);
        CHANNEL.registerMessage(id++, Packets.Cast.class, Packets.Cast::encode, Packets.Cast::decode, Packets.Cast::handle);
    }

    public static void toPlayer(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void toAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
}
