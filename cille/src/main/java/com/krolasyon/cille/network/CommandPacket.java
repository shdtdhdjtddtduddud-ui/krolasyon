package com.krolasyon.cille.network;

import com.krolasyon.cille.entity.KabkabaEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** client -> server: the Cahan gives Kabkaba a battle command */
public class CommandPacket {
    private final int command;
    private final int targetId;
    private final boolean hasPoint;
    private final double x, y, z;

    public CommandPacket(int command, int targetId, Vec3 point) {
        this.command = command;
        this.targetId = targetId;
        this.hasPoint = point != null;
        this.x = point != null ? point.x : 0;
        this.y = point != null ? point.y : 0;
        this.z = point != null ? point.z : 0;
    }

    public CommandPacket(FriendlyByteBuf buf) {
        this.command = buf.readVarInt();
        this.targetId = buf.readInt();
        this.hasPoint = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(command);
        buf.writeInt(targetId);
        buf.writeBoolean(hasPoint);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp == null) return;
        List<KabkabaEntity> mine = sp.serverLevel().getEntitiesOfClass(KabkabaEntity.class, sp.getBoundingBox().inflate(96),
                k -> sp.getUUID().equals(k.getOwnerUUID()) && k.isAlive());
        if (mine.isEmpty()) {
            sp.displayClientMessage(Component.translatable("msg.cille.cmd.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        KabkabaEntity best = mine.get(0);
        for (KabkabaEntity k : mine) if (k.distanceToSqr(sp) < best.distanceToSqr(sp)) best = k;
        Vec3 p = hasPoint && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Vec3(x, y, z) : null;
        best.command(sp, command, targetId, p);
    }
}
