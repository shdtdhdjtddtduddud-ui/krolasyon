package com.krolasyon.realweather;

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

/** Sunucudan istemcilere hava durumu: durum, yoğunluk, rüzgâr yönü ve şiddeti. */
public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RealWeather.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        CHANNEL.registerMessage(0, Sync.class, Sync::write, Sync::read, Sync::handle);
    }

    public static void sendAll(Sync s) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), s);
    }

    public static void send(ServerPlayer p, Sync s) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), s);
    }

    public record Sync(int state, int next, int ticksLeft, float intensity, float windAngle, float wind) {
        public void write(FriendlyByteBuf b) {
            b.writeVarInt(state);
            b.writeVarInt(next);
            b.writeVarInt(ticksLeft);
            b.writeFloat(intensity);
            b.writeFloat(windAngle);
            b.writeFloat(wind);
        }

        public static Sync read(FriendlyByteBuf b) {
            return new Sync(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.realweather.client.ClientWeather.receive(this)));
            ctx.get().setPacketHandled(true);
        }
    }
}
