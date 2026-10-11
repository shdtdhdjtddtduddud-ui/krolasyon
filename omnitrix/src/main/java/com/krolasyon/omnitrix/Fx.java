package com.krolasyon.omnitrix;

import com.krolasyon.omnitrix.net.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Server side triggers for client visual effects (rendered by client.FxRenderer). */
public final class Fx {
    public enum Type {
        /** omnitrix transformation column + flash on entity a (param 1 = transform, 0 = revert, -1 = timeout) */
        TRANSFORM,
        /** flame cone from entity a's hands along its look, duration = channel */
        FLAME_CONE,
        /** expanding ring of fire at a, param = radius */
        FIRE_RING,
        /** fire/rocket trail following entity a */
        FIRE_TRAIL,
        /** wind gust cone from entity a */
        WIND_CONE,
        /** moving tornado a -> b, param = radius */
        TORNADO,
        /** ground shockwave ring at a, param = radius */
        SHOCKWAVE,
        /** line of crystal spikes a -> b */
        CRYSTAL_SPIKES,
        /** crystal cluster around entity b */
        CRYSTAL_PRISON,
        /** slime splash at a, param = radius */
        SLIME_SPLASH,
        /** stretching slime arm from entity a to entity b */
        STRETCH,
        /** speed streak a -> b (xlr8 dash) */
        SPEED_TRAIL,
        /** speed vortex around a, param = radius */
        SPEED_VORTEX,
        /** clap shock cone from entity a */
        CLAP,
        /** frost breath cone from entity a */
        FROST_CONE,
        /** ice block cluster around entity b */
        ICE_PRISON,
        /** sonic wave rings from entity a along its look */
        SONIC_WAVE,
        /** ring of sonic discs around entity b */
        SONIC_DOOM,
        /** ghost tentacle from entity a to entity b */
        TENTACLE,
        /** dark terror pulse at a, param = radius */
        TERROR,
        /** generic hit spark at a (param = alien ordinal for colour) */
        HIT
    }

    private Fx() {}

    public static void send(ServerLevel level, Type type, Vec3 a, Vec3 b, int entA, int entB, int duration, float param) {
        Packet p = new Packet(type.ordinal(), (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, entA, entB, duration, param);
        Net.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(a.x, a.y, a.z, 128, level.dimension())), p);
    }

    public static void at(ServerLevel level, Type type, Vec3 pos, int duration, float param) { send(level, type, pos, pos, -1, -1, duration, param); }

    public static void line(ServerLevel level, Type type, Vec3 a, Vec3 b, int duration, float param) { send(level, type, a, b, -1, -1, duration, param); }

    public static void follow(ServerLevel level, Type type, Entity e, int duration, float param) {
        send(level, type, e.position(), e.position(), e.getId(), -1, duration, param);
    }

    public static void link(ServerLevel level, Type type, Entity from, Entity to, int duration, float param) {
        send(level, type, from.position(), to.position(), from.getId(), to.getId(), duration, param);
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
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.omnitrix.client.FxRenderer.add(this)));
            ctx.get().setPacketHandled(true);
        }
    }
}
