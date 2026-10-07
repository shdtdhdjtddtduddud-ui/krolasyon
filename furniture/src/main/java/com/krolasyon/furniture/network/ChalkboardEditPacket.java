package com.krolasyon.furniture.network;

import com.krolasyon.furniture.blockentity.ChalkboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** client -> server: new text for a chalkboard */
public class ChalkboardEditPacket {
    private final BlockPos pos;
    private final String[] lines;

    public ChalkboardEditPacket(BlockPos pos, String[] lines) {
        this.pos = pos;
        this.lines = lines;
    }

    public static void encode(ChalkboardEditPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        for (int i = 0; i < ChalkboardBlockEntity.LINES; i++) buf.writeUtf(i < p.lines.length ? p.lines[i] : "", 64);
    }

    public static ChalkboardEditPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String[] lines = new String[ChalkboardBlockEntity.LINES];
        for (int i = 0; i < lines.length; i++) lines[i] = buf.readUtf(64);
        return new ChalkboardEditPacket(pos, lines);
    }

    public static void handle(ChalkboardEditPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;
            ServerLevel level = sp.serverLevel();
            if (!level.isLoaded(p.pos)) return;
            if (sp.distanceToSqr(p.pos.getX() + 0.5D, p.pos.getY() + 0.5D, p.pos.getZ() + 0.5D) > 100.0D) return;
            BlockEntity be = level.getBlockEntity(p.pos);
            if (be instanceof ChalkboardBlockEntity board && !board.isWaxed()) board.setLines(p.lines);
        });
        c.setPacketHandled(true);
    }
}
