package com.krolasyon.sololeveling.net;

import com.krolasyon.sololeveling.SoloLeveling;
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

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(SoloLeveling.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void init() {
        int i = 0;
        CH.messageBuilder(Sync.class, i++, NetworkDirection.PLAY_TO_CLIENT).encoder(Sync::write).decoder(Sync::read).consumerMainThread(Sync::handle).add();
        CH.messageBuilder(Notify.class, i++, NetworkDirection.PLAY_TO_CLIENT).encoder(Notify::write).decoder(Notify::read).consumerMainThread(Notify::handle).add();
        CH.messageBuilder(Open.class, i++, NetworkDirection.PLAY_TO_CLIENT).encoder(Open::write).decoder(Open::read).consumerMainThread(Open::handle).add();
        CH.messageBuilder(Fx.class, i++, NetworkDirection.PLAY_TO_CLIENT).encoder(Fx::write).decoder(Fx::read).consumerMainThread(Fx::handle).add();
        CH.messageBuilder(Action.class, i++, NetworkDirection.PLAY_TO_SERVER).encoder(Action::write).decoder(Action::read).consumerMainThread(Action::handle).add();
    }

    public static void to(ServerPlayer p, Object msg) { CH.send(PacketDistributor.PLAYER.with(() -> p), msg); }

    public static void toServer(Object msg) { CH.sendToServer(msg); }

    /** Full hunter data sync for the owning client. */
    public record Sync(CompoundTag data) {
        void write(FriendlyByteBuf b) { b.writeNbt(data); }

        static Sync read(FriendlyByteBuf b) { return new Sync(b.readNbt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.sololeveling.client.ClientHooks.sync(data));
        }
    }

    /** A blue System window popup. */
    public record Notify(int kind, Component title, Component body, int color) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(kind);
            b.writeComponent(title);
            b.writeComponent(body);
            b.writeInt(color);
        }

        static Notify read(FriendlyByteBuf b) { return new Notify(b.readVarInt(), b.readComponent(), b.readComponent(), b.readInt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.sololeveling.client.ClientHooks.notify(kind, title, body, color));
        }
    }

    /** Asks the client to open a screen (dialog, shop, news, map, awakening...). */
    public record Open(String screen, CompoundTag data) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(screen);
            b.writeNbt(data);
        }

        static Open read(FriendlyByteBuf b) { return new Open(b.readUtf(), b.readNbt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.sololeveling.client.ClientHooks.open(screen, data));
        }
    }

    /** Client-side visual effect (big ARISE text, screen shake, flashes). */
    public record Fx(String kind, double x, double y, double z, int arg) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(kind);
            b.writeDouble(x);
            b.writeDouble(y);
            b.writeDouble(z);
            b.writeVarInt(arg);
        }

        static Fx read(FriendlyByteBuf b) { return new Fx(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.sololeveling.client.ClientHooks.fx(kind, x, y, z, arg));
        }
    }

    /** Generic client to server request. */
    public record Action(String action, int a, int b, String s) {
        void write(FriendlyByteBuf buf) {
            buf.writeUtf(action);
            buf.writeVarInt(a);
            buf.writeVarInt(b);
            buf.writeUtf(s);
        }

        static Action read(FriendlyByteBuf buf) { return new Action(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readUtf(256)); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) com.krolasyon.sololeveling.system.Actions.handle(p, action, a, b, s);
        }
    }
}
