package com.krolasyon.futbol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class MatchS2C {
    public record Entry(UUID id, String name, int team, int number, boolean online, int role) {}

    public final int state, red, blue, timeLeft, stateTimer, kickoffTeam, playersPerTeam, minutes;
    public final boolean hasPitch;
    public final int px, py, pz;
    public final String lastScorer;
    public final int lastGoalTeam, bots;
    public final List<Entry> entries;

    public MatchS2C(int state, int red, int blue, int timeLeft, int stateTimer, int kickoffTeam, int playersPerTeam, int minutes,
                    boolean hasPitch, int px, int py, int pz, String lastScorer, int lastGoalTeam, int bots, List<Entry> entries) {
        this.state = state;
        this.red = red;
        this.blue = blue;
        this.timeLeft = timeLeft;
        this.stateTimer = stateTimer;
        this.kickoffTeam = kickoffTeam;
        this.playersPerTeam = playersPerTeam;
        this.minutes = minutes;
        this.hasPitch = hasPitch;
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.lastScorer = lastScorer;
        this.lastGoalTeam = lastGoalTeam;
        this.bots = bots;
        this.entries = entries;
    }

    public MatchS2C(FriendlyByteBuf b) {
        this.state = b.readVarInt();
        this.red = b.readVarInt();
        this.blue = b.readVarInt();
        this.timeLeft = b.readVarInt();
        this.stateTimer = b.readVarInt();
        this.kickoffTeam = b.readVarInt();
        this.playersPerTeam = b.readVarInt();
        this.minutes = b.readVarInt();
        this.hasPitch = b.readBoolean();
        this.px = b.readInt();
        this.py = b.readInt();
        this.pz = b.readInt();
        this.lastScorer = b.readUtf(64);
        this.lastGoalTeam = b.readVarInt();
        this.bots = b.readVarInt();
        int n = b.readVarInt();
        List<Entry> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new Entry(b.readUUID(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readByte()));
        this.entries = l;
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(state);
        b.writeVarInt(red);
        b.writeVarInt(blue);
        b.writeVarInt(Math.max(0, timeLeft));
        b.writeVarInt(Math.max(0, stateTimer));
        b.writeVarInt(kickoffTeam);
        b.writeVarInt(playersPerTeam);
        b.writeVarInt(minutes);
        b.writeBoolean(hasPitch);
        b.writeInt(px);
        b.writeInt(py);
        b.writeInt(pz);
        b.writeUtf(lastScorer == null ? "" : lastScorer, 64);
        b.writeVarInt(lastGoalTeam);
        b.writeVarInt(bots);
        b.writeVarInt(entries.size());
        for (Entry e : entries) {
            b.writeUUID(e.id());
            b.writeUtf(e.name(), 64);
            b.writeVarInt(e.team());
            b.writeVarInt(e.number());
            b.writeBoolean(e.online());
            b.writeByte(e.role());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.futbol.client.ClientPackets.match(this));
        ctx.get().setPacketHandled(true);
    }
}
