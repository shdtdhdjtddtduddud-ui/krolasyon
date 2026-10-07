package com.krolasyon.furniture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** server -> clients nearby: animate a piano key */
public class PianoAnimPacket {
    private final BlockPos pos;
    private final byte note;

    public PianoAnimPacket(BlockPos pos, byte note) {
        this.pos = pos;
        this.note = note;
    }

    public static void encode(PianoAnimPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeByte(p.note);
    }

    public static PianoAnimPacket decode(FriendlyByteBuf buf) {
        return new PianoAnimPacket(buf.readBlockPos(), buf.readByte());
    }

    public static void handle(PianoAnimPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.furniture.client.ClientHooks.pianoAnim(p.pos, p.note)));
        c.setPacketHandled(true);
    }
}
