package com.krolasyon.futbol.network;

import com.krolasyon.futbol.game.MatchManager;
import com.krolasyon.futbol.game.PitchBuilder;
import com.krolasyon.futbol.game.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Match menu actions sent from the client GUI. */
public class ControlC2S {
    public static final int TEAM = 0, START = 1, STOP = 2, PLAYERS = 3, MINUTES = 4, BUILD = 5, CENTER = 6;

    public final int action;
    public final int arg;

    public ControlC2S(int action, int arg) {
        this.action = action;
        this.arg = arg;
    }

    public ControlC2S(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.arg = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(arg);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p == null) return;
        switch (action) {
            case TEAM -> MatchManager.setTeam(p, Team.byId(arg));
            case START -> MatchManager.start(p);
            case STOP -> MatchManager.stop();
            case PLAYERS -> {
                MatchManager.playersPerTeam = Mth.clamp(arg, 1, 11);
                MatchManager.rebalance();
                MatchManager.markDirty();
            }
            case MINUTES -> {
                MatchManager.minutes = Mth.clamp(arg, 1, 90);
                MatchManager.markDirty();
            }
            case BUILD -> {
                if (!p.isCreative() && !p.hasPermissions(2)) {
                    p.displayClientMessage(Component.literal("Saha kurmak için yaratıcı mod veya Saha Kurucu eşyası gerekli.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                MatchManager.stop();
                PitchBuilder.build(p.serverLevel(), p.blockPosition());
                MatchManager.centerBall();
                p.displayClientMessage(Component.literal("⚽ Stadyum kuruldu!").withStyle(ChatFormatting.GREEN), false);
                MatchManager.markDirty();
            }
            case CENTER -> MatchManager.centerBall();
            default -> {}
        }
    }
}
