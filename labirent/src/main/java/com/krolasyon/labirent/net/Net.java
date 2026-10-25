package com.krolasyon.labirent.net;

import com.krolasyon.labirent.Labirent;
import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.maze.ExploreTracker;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.maze.MazeState;
import com.krolasyon.labirent.team.Role;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class Net {
    private Net() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(new ResourceLocation(Labirent.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    // ------------------------------------------------------------------ messages
    public record MapFull(byte[] known) {}

    public record MapDelta(int[] idx, byte[] val) {}

    public record Shake(float strength, int ticks) {}

    public record OpenScreen(int type, int arg) {}

    public record Cinematic(int id) {}

    public record RosterEntry(UUID id, String name, int role, boolean npc, float x, float z, boolean stung, int entityId) {}

    public record State(UUID captain, String captainName, int chapter, int[] progress, int stones, int words, int events, int monthDays,
                        int lastArrivalDay, int doorStep, int shiftedMask, int mapPercent, int memories, List<RosterEntry> roster) {}

    // client -> server
    public record Order(int order, String text) {}

    public record SetRole(UUID target, int role) {}

    public record NpcCommand(int entityId, int cmd) {}

    public record TerminalCode(int[] order) {}

    public record Transfer(UUID target) {}

    public static final int SCREEN_MAP = 0, SCREEN_CAPTAIN = 1, SCREEN_NPC = 2, SCREEN_TERMINAL = 3, SCREEN_JOURNAL = 4;

    public static void register() {
        int id = 0;
        CH.messageBuilder(MapFull.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> b.writeByteArray(m.known()))
                .decoder(b -> new MapFull(b.readByteArray()))
                .consumerMainThread((m, c) -> client(() -> () -> ClientHooks.mapFull(m))).add();
        CH.messageBuilder(MapDelta.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> {
                    b.writeVarInt(m.idx().length);
                    for (int k = 0; k < m.idx().length; k++) {
                        b.writeVarInt(m.idx()[k]);
                        b.writeByte(m.val()[k]);
                    }
                })
                .decoder(b -> {
                    int n = b.readVarInt();
                    int[] i = new int[n];
                    byte[] v = new byte[n];
                    for (int k = 0; k < n; k++) {
                        i[k] = b.readVarInt();
                        v[k] = b.readByte();
                    }
                    return new MapDelta(i, v);
                })
                .consumerMainThread((m, c) -> client(() -> () -> ClientHooks.mapDelta(m))).add();
        CH.messageBuilder(Shake.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> { b.writeFloat(m.strength()); b.writeVarInt(m.ticks()); })
                .decoder(b -> new Shake(b.readFloat(), b.readVarInt()))
                .consumerMainThread((m, c) -> client(() -> () -> ClientHooks.shake(m))).add();
        CH.messageBuilder(OpenScreen.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> { b.writeVarInt(m.type()); b.writeVarInt(m.arg()); })
                .decoder(b -> new OpenScreen(b.readVarInt(), b.readVarInt()))
                .consumerMainThread((m, c) -> client(() -> () -> ClientHooks.open(m))).add();
        CH.messageBuilder(Cinematic.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> b.writeVarInt(m.id()))
                .decoder(b -> new Cinematic(b.readVarInt()))
                .consumerMainThread((m, c) -> client(() -> () -> ClientHooks.cinematic(m))).add();
        CH.messageBuilder(State.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Net::writeState)
                .decoder(Net::readState)
                .consumerMainThread((m, c) -> client(() -> () -> ClientHooks.state(m))).add();

        CH.messageBuilder(Order.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> { b.writeVarInt(m.order()); b.writeUtf(m.text(), 120); })
                .decoder(b -> new Order(b.readVarInt(), b.readUtf(120)))
                .consumerMainThread((m, c) -> ServerHooks.order(m, c.get().getSender())).add();
        CH.messageBuilder(SetRole.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> { b.writeUUID(m.target()); b.writeVarInt(m.role()); })
                .decoder(b -> new SetRole(b.readUUID(), b.readVarInt()))
                .consumerMainThread((m, c) -> ServerHooks.setRole(m, c.get().getSender())).add();
        CH.messageBuilder(NpcCommand.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> { b.writeVarInt(m.entityId()); b.writeVarInt(m.cmd()); })
                .decoder(b -> new NpcCommand(b.readVarInt(), b.readVarInt()))
                .consumerMainThread((m, c) -> ServerHooks.npcCommand(m, c.get().getSender())).add();
        CH.messageBuilder(TerminalCode.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> b.writeVarIntArray(m.order()))
                .decoder(b -> new TerminalCode(b.readVarIntArray(16)))
                .consumerMainThread((m, c) -> ServerHooks.terminal(m, c.get().getSender())).add();
        CH.messageBuilder(Transfer.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> b.writeUUID(m.target()))
                .decoder(b -> new Transfer(b.readUUID()))
                .consumerMainThread((m, c) -> ServerHooks.transfer(m, c.get().getSender())).add();
    }

    private static void client(Supplier<Runnable> r) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, r);
    }

    private static void writeState(State s, FriendlyByteBuf b) {
        b.writeBoolean(s.captain() != null);
        if (s.captain() != null) b.writeUUID(s.captain());
        b.writeUtf(s.captainName());
        b.writeVarInt(s.chapter());
        b.writeVarIntArray(s.progress());
        b.writeVarInt(s.stones());
        b.writeVarInt(s.words());
        b.writeVarInt(s.events());
        b.writeVarInt(s.monthDays());
        b.writeInt(s.lastArrivalDay());
        b.writeVarInt(s.doorStep());
        b.writeVarInt(s.shiftedMask());
        b.writeVarInt(s.mapPercent());
        b.writeVarInt(s.memories());
        b.writeVarInt(s.roster().size());
        for (RosterEntry e : s.roster()) {
            b.writeUUID(e.id());
            b.writeUtf(e.name(), 64);
            b.writeVarInt(e.role());
            b.writeBoolean(e.npc());
            b.writeFloat(e.x());
            b.writeFloat(e.z());
            b.writeBoolean(e.stung());
            b.writeVarInt(e.entityId());
        }
    }

    private static State readState(FriendlyByteBuf b) {
        UUID cap = b.readBoolean() ? b.readUUID() : null;
        String cn = b.readUtf();
        int ch = b.readVarInt();
        int[] pr = b.readVarIntArray(64);
        int st = b.readVarInt(), wo = b.readVarInt(), ev = b.readVarInt(), md = b.readVarInt();
        int la = b.readInt(), ds = b.readVarInt(), sm = b.readVarInt(), mp = b.readVarInt(), mem = b.readVarInt();
        int n = b.readVarInt();
        List<RosterEntry> list = new ArrayList<>();
        for (int k = 0; k < n; k++)
            list.add(new RosterEntry(b.readUUID(), b.readUtf(64), b.readVarInt(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readVarInt()));
        return new State(cap, cn, ch, pr, st, wo, ev, md, la, ds, sm, mp, mem, list);
    }

    // ------------------------------------------------------------------ server helpers
    public static void sendFullMap(ServerPlayer p, MazeState s) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new MapFull(s.known.clone()));
    }

    public static void sendMapDelta(ServerLevel level, MazeState s, BitSet dirty) {
        int n = dirty.cardinality();
        int[] idx = new int[n];
        byte[] val = new byte[n];
        int k = 0;
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) {
            idx[k] = i;
            val[k++] = s.known[i];
        }
        MapDelta msg = new MapDelta(idx, val);
        for (ServerPlayer p : level.players()) CH.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    public static void shake(ServerPlayer p, float strength, int ticks) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new Shake(strength, ticks));
    }

    public static void open(ServerPlayer p, int type, int arg) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new OpenScreen(type, arg));
    }

    public static void cinematic(ServerPlayer p, int id) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), new Cinematic(id));
    }

    public static void title(ServerPlayer p, Component title, Component sub, int in, int stay, int out) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static void sendState(ServerPlayer p, MazeManager m) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), buildState(m));
    }

    private static State buildState(MazeManager m) {
        MazeState s = m.state;
        List<RosterEntry> roster = new ArrayList<>();
        ServerLevel level = m.level();
        for (UUID id : s.roles.keySet()) {
            ServerPlayer pl = m.server.getPlayerList().getPlayer(id);
            boolean here = pl != null && level != null && pl.level() == level;
            roster.add(new RosterEntry(id, s.names.getOrDefault(id, "?"), s.role(id).ordinal(), false,
                    here ? (float) pl.getX() : Float.NaN, here ? (float) pl.getZ() : Float.NaN,
                    pl != null && pl.hasEffect(com.krolasyon.labirent.registry.ModEffects.VENOM.get()), pl == null ? -1 : pl.getId()));
        }
        if (level != null) {
            AABB all = new AABB(MazeLayout.ORIGIN - 20, 0, MazeLayout.ORIGIN - 20, MazeLayout.ORIGIN + MazeLayout.SIZE + 20, 255, MazeLayout.ORIGIN + MazeLayout.SIZE + 20);
            for (GladerEntity g : level.getEntitiesOfClass(GladerEntity.class, all)) {
                roster.add(new RosterEntry(g.getUUID(), g.getGladerName(), g.getRole().ordinal(), true, (float) g.getX(), (float) g.getZ(),
                        g.isStung(), g.getId()));
            }
        }
        int doorStep = m.doors.step();
        return new State(s.captain, s.captainName, s.chapter, s.progress.clone(), s.stonesFound, s.wordsFound, s.events,
                com.krolasyon.labirent.LabConfig.MONTH_DAYS.get(), s.lastArrivalDay, doorStep, s.shiftedSectorsMask,
                ExploreTracker.percent(s), s.memoriesUnlocked, roster);
    }

    /** helper used by role commands */
    public static Role role(int i) { return Role.byId(i); }
}
