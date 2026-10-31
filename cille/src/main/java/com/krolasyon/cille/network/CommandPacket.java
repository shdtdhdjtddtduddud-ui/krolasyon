package com.krolasyon.cille.network;

import com.krolasyon.cille.entity.CilleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** client -> server: the Cahan gives one of their Cilles a battle command */
public class CommandPacket {
    private final int command;
    private final int cilleId;
    private final int targetId;
    private final boolean hasPoint;
    private final double x, y, z;

    public CommandPacket(int command, int cilleId, int targetId, Vec3 point) {
        this.command = command;
        this.cilleId = cilleId;
        this.targetId = targetId;
        this.hasPoint = point != null;
        this.x = point != null ? point.x : 0;
        this.y = point != null ? point.y : 0;
        this.z = point != null ? point.z : 0;
    }

    public CommandPacket(FriendlyByteBuf buf) {
        this.command = buf.readVarInt();
        this.cilleId = buf.readInt();
        this.targetId = buf.readInt();
        this.hasPoint = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(command);
        buf.writeInt(cilleId);
        buf.writeInt(targetId);
        buf.writeBoolean(hasPoint);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp == null) return;
        CilleEntity chosen = null;
        if (cilleId >= 0) {
            Entity e = sp.serverLevel().getEntity(cilleId);
            if (e instanceof CilleEntity c && c.isAlive() && sp.getUUID().equals(c.getOwnerUUID()) && c.distanceToSqr(sp) < 128 * 128) chosen = c;
        }
        if (chosen == null) {
            List<CilleEntity> mine = sp.serverLevel().getEntitiesOfClass(CilleEntity.class, sp.getBoundingBox().inflate(96),
                    k -> sp.getUUID().equals(k.getOwnerUUID()) && k.isAlive());
            for (CilleEntity k : mine) if (chosen == null || k.distanceToSqr(sp) < chosen.distanceToSqr(sp)) chosen = k;
        }
        if (chosen == null) {
            sp.displayClientMessage(Component.translatable("msg.cille.cmd.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vec3 p = hasPoint && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Vec3(x, y, z) : null;
        chosen.command(sp, command, targetId, p);
    }
}
