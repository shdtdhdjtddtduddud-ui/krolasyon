package com.rabona.arena.net;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.game.Athlete;
import com.rabona.arena.game.Match;
import com.rabona.arena.game.Move;
import com.rabona.arena.game.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Sunucudan istemciye paketler. Islemler client.ClientHooks icinde. */
public final class S2C {
    private S2C() {}

    static void client(Supplier<NetworkEvent.Context> ctx, Runnable r) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> r));
    }

    public record Anim(int entity, int move, int side) {
        public Anim(FriendlyByteBuf b) { this(b.readVarInt(), b.readVarInt(), b.readByte()); }

        public void encode(FriendlyByteBuf b) {
            b.writeVarInt(entity);
            b.writeVarInt(move);
            b.writeByte(side);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.anim(this));
        }
    }

    public record Shake(int entity, float power) {
        public Shake(FriendlyByteBuf b) { this(b.readVarInt(), b.readFloat()); }

        public void encode(FriendlyByteBuf b) {
            b.writeVarInt(entity);
            b.writeFloat(power);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.shake(this));
        }
    }

    public record RosterEntry(UUID id, String name, int team, int number, int pos) {}

    public record MatchState(int phase, int scoreRed, int scoreBlue, int timeLeft, int totalTime, int half, boolean swapped,
                             int duration, int teamSize, int difficulty, boolean hasPitch, long origin, boolean alongX,
                             int ballId, int restrictTeam, List<RosterEntry> roster) {
        public static MatchState of(Match m) {
            List<RosterEntry> r = new ArrayList<>();
            m.roster.forEach((u, t) -> {
                ServerPlayer p = m.level().getServer().getPlayerList().getPlayer(u);
                if (p != null) r.add(new RosterEntry(u, p.getGameProfile().getName(), t.ordinal(), m.numbers.getOrDefault(u, 10), m.posOf(p).ordinal()));
            });
            BallEntity b = m.ball();
            int restrict = b != null && b.tickCount < b.restrictUntil ? b.restrictTeam : -1;
            return new MatchState(m.phase.ordinal(), m.scoreRed, m.scoreBlue, m.timeLeft, m.totalTime, m.half, m.swapped,
                    m.durationMin, m.teamSize, m.difficulty, m.pitch != null, m.pitch == null ? 0 : m.pitch.origin.asLong(),
                    m.pitch != null && m.pitch.alongX, b == null ? -1 : b.getId(), restrict, r);
        }

        public MatchState(FriendlyByteBuf b) {
            this(b.readByte(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readByte(), b.readBoolean(),
                    b.readByte(), b.readByte(), b.readByte(), b.readBoolean(), b.readLong(), b.readBoolean(), b.readVarInt(),
                    b.readByte(), b.readList(x -> new RosterEntry(x.readUUID(), x.readUtf(40), x.readByte(), x.readByte(), x.readByte())));
        }

        public void encode(FriendlyByteBuf b) {
            b.writeByte(phase);
            b.writeVarInt(scoreRed);
            b.writeVarInt(scoreBlue);
            b.writeVarInt(Math.max(0, timeLeft));
            b.writeVarInt(totalTime);
            b.writeByte(half);
            b.writeBoolean(swapped);
            b.writeByte(duration);
            b.writeByte(teamSize);
            b.writeByte(difficulty);
            b.writeBoolean(hasPitch);
            b.writeLong(origin);
            b.writeBoolean(alongX);
            b.writeVarInt(ballId);
            b.writeByte(restrictTeam);
            b.writeCollection(roster, (x, e) -> {
                x.writeUUID(e.id());
                x.writeUtf(e.name(), 40);
                x.writeByte(e.team());
                x.writeByte(e.number());
                x.writeByte(e.pos());
            });
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.match(this));
        }

        public Team teamOf(UUID u) {
            for (RosterEntry e : roster) if (e.id().equals(u)) return Team.byId(e.team());
            return Team.NONE;
        }

        public int numberOf(UUID u) {
            for (RosterEntry e : roster) if (e.id().equals(u)) return e.number();
            return 0;
        }
    }

    public record Stats(float stamina, float energy, int goals, int assists, short[] cooldowns) {
        public static Stats of(Athlete a) {
            short[] c = new short[a.cooldowns.length];
            for (int i = 0; i < c.length; i++) c[i] = (short) Math.min(Short.MAX_VALUE, a.cooldowns[i]);
            return new Stats(a.stamina, a.energy, a.goals, a.assists, c);
        }

        public Stats(FriendlyByteBuf b) { this(b.readFloat(), b.readFloat(), b.readVarInt(), b.readVarInt(), readShorts(b)); }

        static short[] readShorts(FriendlyByteBuf b) {
            int n = b.readVarInt();
            short[] s = new short[Math.min(n, 256)];
            for (int i = 0; i < n; i++) {
                short v = b.readShort();
                if (i < s.length) s[i] = v;
            }
            return s;
        }

        public void encode(FriendlyByteBuf b) {
            b.writeFloat(stamina);
            b.writeFloat(energy);
            b.writeVarInt(goals);
            b.writeVarInt(assists);
            b.writeVarInt(cooldowns.length);
            for (short s : cooldowns) b.writeShort(s);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.stats(this));
        }
    }

    /** Buyuk ekran yazisi: 1 = GOL, 2 = mac sonu. */
    public record Banner(int type, int scoreRed, int scoreBlue, String name, String extra) {
        public Banner(FriendlyByteBuf b) { this(b.readByte(), b.readVarInt(), b.readVarInt(), b.readUtf(64), b.readUtf(128)); }

        public void encode(FriendlyByteBuf b) {
            b.writeByte(type);
            b.writeVarInt(scoreRed);
            b.writeVarInt(scoreBlue);
            b.writeUtf(name, 64);
            b.writeUtf(extra, 128);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.banner(this));
        }
    }

    public record Feed(Component text) {
        public Feed(FriendlyByteBuf b) { this(b.readComponent()); }

        public void encode(FriendlyByteBuf b) { b.writeComponent(text); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.feed(this));
        }
    }

    public record OpenMenu(int which) {
        public OpenMenu(FriendlyByteBuf b) { this(b.readByte()); }

        public void encode(FriendlyByteBuf b) { b.writeByte(which); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.openMenu(this));
        }
    }

    /** Oyuncu profili: jeton, kartlar, kadro, yeni acilan kartlar. */
    public record Profile(int coins, java.util.List<com.rabona.arena.game.Cards.Card> cards, java.util.List<Integer> squad,
                          java.util.List<com.rabona.arena.game.Cards.Card> opened) {
        public Profile(FriendlyByteBuf b) {
            this(b.readVarInt(), b.readList(com.rabona.arena.game.Cards.Card::read), b.readList(FriendlyByteBuf::readVarInt),
                    b.readList(com.rabona.arena.game.Cards.Card::read));
        }

        public void encode(FriendlyByteBuf b) {
            b.writeVarInt(coins);
            b.writeCollection(cards, (x, c) -> c.write(x));
            b.writeCollection(squad, FriendlyByteBuf::writeVarInt);
            b.writeCollection(opened, (x, c) -> c.write(x));
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            client(ctx, () -> com.rabona.arena.client.ClientHooks.profile(this));
        }
    }

    public static Move moveOf(Anim a) { return Move.byId(a.move()); }
}
