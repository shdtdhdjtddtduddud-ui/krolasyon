package com.krolasyon.voidtree;

import com.krolasyon.voidtree.net.Net;
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
        /** shadow trail of a dash a -> b */
        TRAIL,
        /** ghostly shimmer around entity a */
        PHANTOM,
        /** ground vortex at a, param = radius */
        VORTEX,
        /** tornado moving a -> b over the duration, param = radius */
        TWISTER,
        /** crescent claw slash in front of entity a, b = look direction point, param = side (+1/-1, 2 = big) */
        SLASH,
        /** void sphere prison around entity b (caster a) */
        PRISON,
        /** falling shadow blade a -> b */
        BLADE,
        /** black hole at a, param = pull radius */
        HOLE,
        /** inward collapse + flash at a, param = radius */
        IMPLODE,
        /** dimensional rift tear at a */
        RIFT,
        /** halo, wings and aura on entity a */
        ASCEND,
        /** rune seal circle at a, param = radius */
        SEAL,
        /** starfield sky + falling stars over a, param = radius */
        COSMIC,
        /** radial shadow tendrils at a, param = radius */
        WISPBURST,
        /** tendril from entity a's hand to entity b */
        TETHER,
        /** void energy gathering in entity a's hands */
        CHARGE,
        /** sparkle star at a */
        MARK
    }

    private Fx() {}

    public static void send(ServerLevel level, Type type, Vec3 a, Vec3 b, int entA, int entB, int duration, float param) {
        Packet p = new Packet(type.ordinal(), (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, entA, entB, duration, param);
        Net.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(a.x, a.y, a.z, 128, level.dimension())), p);
    }

    public static void line(ServerLevel level, Type type, Vec3 a, Vec3 b, int duration, float param) { send(level, type, a, b, -1, -1, duration, param); }

    public static void link(ServerLevel level, Type type, Entity from, Entity to, int duration, float param) {
        send(level, type, from.position(), to.position(), from.getId(), to.getId(), duration, param);
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
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.voidtree.client.FxRenderer.add(this)));
            ctx.get().setPacketHandled(true);
        }
    }
}
