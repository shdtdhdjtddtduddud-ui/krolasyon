package com.krolasyon.futbol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AnimS2C {
    public final int entity, move, variant, cooldown;

    public AnimS2C(int entity, int move, int variant, int cooldown) {
        this.entity = entity;
        this.move = move;
        this.variant = variant;
        this.cooldown = cooldown;
    }

    public AnimS2C(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readByte(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entity);
        buf.writeVarInt(move);
        buf.writeByte(variant);
        buf.writeVarInt(cooldown);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.futbol.client.ClientPackets.anim(this));
        ctx.get().setPacketHandled(true);
    }
}
