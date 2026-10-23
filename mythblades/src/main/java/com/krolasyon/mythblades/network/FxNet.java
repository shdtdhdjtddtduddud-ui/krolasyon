package com.krolasyon.mythblades.network;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.combat.EmberCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Kor Kılıcı dövüş paketleri: istemciden atılış/saplama isteği, sunucudan görsel efektler. */
public final class FxNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MythBlades.MODID, "fx"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static final int DASH = 0, PLUNGE = 1, IMPACT = 2, PARRY = 3, BLOCK_HIT = 4, COUNTER = 5;

    private FxNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, Move.class, Move::write, Move::read, Move::handle);
        CHANNEL.registerMessage(1, Fx.class, Fx::write, Fx::read, Fx::handle);
    }

    /** Efekti bu varlığı izleyen herkese (ve kendisine) gönderir. */
    public static void fx(Entity source, Fx fx) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> source), fx);
    }

    public static void move(boolean plunge, double dx, double dz) {
        CHANNEL.sendToServer(new Move(plunge, dx, dz));
    }

    /** İstemciden: atılış (dash) ya da kor saplama başladı; yön yatay birim vektör. */
    public record Move(boolean plunge, double dx, double dz) {
        public void write(FriendlyByteBuf b) {
            b.writeBoolean(plunge);
            b.writeDouble(dx);
            b.writeDouble(dz);
        }

        public static Move read(FriendlyByteBuf b) {
            return new Move(b.readBoolean(), b.readDouble(), b.readDouble());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p != null) EmberCombat.onMove(p, plunge, dx, dz);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Sunucudan: görsel efekt (tür, varlık, konum, iki ek değer). */
    public record Fx(int type, int entity, double x, double y, double z, float a, float b) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(type);
            buf.writeVarInt(entity);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(a);
            buf.writeFloat(b);
        }

        public static Fx read(FriendlyByteBuf buf) {
            return new Fx(buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.mythblades.client.EmberFx.receive(this)));
            ctx.get().setPacketHandled(true);
        }
    }
}
