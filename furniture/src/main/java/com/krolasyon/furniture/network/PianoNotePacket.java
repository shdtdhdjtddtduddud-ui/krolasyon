package com.krolasyon.furniture.network;

import com.krolasyon.furniture.block.PianoBlock;
import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.block.WidePart;
import com.krolasyon.furniture.blockentity.PianoBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** client -> server: a piano key was played */
public class PianoNotePacket {
    private final BlockPos pos;
    private final byte note;

    public PianoNotePacket(BlockPos pos, byte note) {
        this.pos = pos;
        this.note = note;
    }

    public static void encode(PianoNotePacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeByte(p.note);
    }

    public static PianoNotePacket decode(FriendlyByteBuf buf) {
        return new PianoNotePacket(buf.readBlockPos(), buf.readByte());
    }

    public static void handle(PianoNotePacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;
            ServerLevel level = sp.serverLevel();
            if (!level.isLoaded(p.pos)) return;
            if (sp.distanceToSqr(p.pos.getX() + 0.5D, p.pos.getY() + 0.5D, p.pos.getZ() + 0.5D) > 100.0D) return;
            BlockState st = level.getBlockState(p.pos);
            if (st.getBlock() instanceof PianoBlock && st.getValue(WideBlock.PART) == WidePart.RIGHT) {
                PianoBlockEntity.playNote(level, p.pos, st, p.note, sp);
            }
        });
        c.setPacketHandled(true);
    }
}
