package com.krolasyon.storm;

import com.krolasyon.storm.net.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Server side triggers for the client visual effects (rendered by client.FxRenderer). */
public final class Fx {
    public enum Type {
        /** jagged arc a -> b (or entity a -> entity b when ids are set) */
        ARC,
        /** sky bolt hitting a + ground flash */
        STRIKE,
        /** lightning spear beam a -> b */
        BEAM,
        /** expanding shockwave ring at a, param = radius */
        NOVA,
        /** spark burst ring at a, param = radius */
        BURST,
        /** shield dome around entity a */
        DOME,
        /** orbiting ring around entity a */
        RING,
        /** crawling arcs over entity a */
        AURA,
        /** persistent arc from entity a's hand to entity b */
        TETHER,
        /** lightning skull rising at a */
        SKULL,
        /** energy gathering at entity a's hands (wind-up) */
        CHARGE
    }

    private Fx() {}

    public static void send(ServerLevel level, Type type, Vec3 a, Vec3 b, int entA, int entB, int duration, float param) {
        Packet p = new Packet(type.ordinal(), (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, entA, entB, duration, param);
        Net.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(a.x, a.y, a.z, 128, level.dimension())), p);
    }

    public static void arc(ServerLevel level, Vec3 a, Vec3 b, int duration) { send(level, Type.ARC, a, b, -1, -1, duration, 1F); }

    public static void arc(ServerLevel level, Entity from, Entity to, int duration, float width) {
        send(level, Type.ARC, from.position(), to.position(), from.getId(), to.getId(), duration, width);
    }

    public static void at(ServerLevel level, Type type, Vec3 pos, int duration, float param) { send(level, type, pos, pos, -1, -1, duration, param); }

    public static void follow(ServerLevel level, Type type, Entity e, int duration, float param) {
        send(level, type, e.position(), e.position(), e.getId(), -1, duration, param);
    }

    public record Packet(int type, float ax, float ay, float az, float bx, float by, float bz, int entA, int entB, int duration, float param) {
        public Packet(FriendlyByteBuf b) {
            this(b.readByte(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(),
                    b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readFloat());
        }

        public void encode(FriendlyByteBuf b) {
            b.writeByte(type);
            b.writeFloat(ax); b.writeFloat(ay); b.writeFloat(az);
            b.writeFloat(bx); b.writeFloat(by); b.writeFloat(bz);
            b.writeVarInt(entA); b.writeVarInt(entB); b.writeVarInt(duration);
            b.writeFloat(param);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.storm.client.FxRenderer.add(this)));
            ctx.get().setPacketHandled(true);
        }
    }
}
