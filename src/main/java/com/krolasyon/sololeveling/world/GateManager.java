package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.HunterNpc;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.registry.ModEntities;
import com.krolasyon.sololeveling.system.Guild;
import com.krolasyon.sololeveling.system.Rank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/** Spawns gates around Seoul and the overworld, sends NPC raid teams and handles dungeon breaks. */
public final class GateManager {
    static final String[] DISTRICTS = {"gangnam", "hongdae", "jongno", "itaewon", "myeongdong", "yeouido", "mapo", "songpa"};
    private static int timer = 20 * 30;

    private GateManager() {}

    public static void tick(MinecraftServer s) {
        if (--timer > 0) return;
        timer = 20 * 60 * 2 + s.overworld().random.nextInt(20 * 60 * 2);
        if (s.getPlayerList().getPlayerCount() == 0) return;
        RandomSource r = s.overworld().random;
        DungeonManager dm = DungeonManager.get(s);
        int seoulGates = 0, owGates = 0;
        for (DungeonManager.Instance i : dm.all()) {
            if (i.cleared || i.instant || i.gateDim.isEmpty()) continue;
            if (i.gateDim.equals(Regions.SEOUL.location().toString())) seoulGates++;
            else owGates++;
        }
        ServerLevel seoul = s.getLevel(Regions.SEOUL);
        if (seoul != null && seoulGates < 6 && !seoul.players().isEmpty()) spawnSeoul(s, seoul, r);
        if (owGates < 3) {
            List<ServerPlayer> ps = s.overworld().players();
            if (!ps.isEmpty()) spawnNear(s, s.overworld(), ps.get(r.nextInt(ps.size())), r);
        }
    }

    static Rank randomRank(RandomSource r) {
        int x = r.nextInt(100);
        if (x < 30) return Rank.E;
        if (x < 55) return Rank.D;
        if (x < 73) return Rank.C;
        if (x < 86) return Rank.B;
        if (x < 95) return Rank.A;
        return Rank.S;
    }

    public static String district(int x, int z) {
        if (Math.abs(x) < 64 && Math.abs(z) < 64) return "jongno";
        double a = Math.atan2(z, x);
        int i = (int) Math.floor((a + Math.PI) / (Math.PI * 2) * DISTRICTS.length) % DISTRICTS.length;
        return DISTRICTS[i];
    }

    public static GateEntity spawnSeoul(MinecraftServer s, ServerLevel seoul, RandomSource r) {
        int sx, sz;
        do {
            sx = r.nextInt(13) - 6;
            sz = r.nextInt(13) - 6;
        } while (Math.abs(sx) <= 1 && Math.abs(sz) <= 1);
        BlockPos pos = new BlockPos(sx * CityPlan.SB + 8, CityPlan.GROUND + 1, sz * CityPlan.SB + 8);
        return spawnGate(s, seoul, pos, randomRank(r), r.nextInt(14) == 0, district(pos.getX(), pos.getZ()), r);
    }

    public static GateEntity spawnNear(MinecraftServer s, ServerLevel l, ServerPlayer p, RandomSource r) {
        double a = r.nextDouble() * Math.PI * 2, d = 40 + r.nextDouble() * 80;
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        l.getChunk(x >> 4, z >> 4);
        BlockPos pos = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (!l.getFluidState(pos.below()).isEmpty()) return null;
        return spawnGate(s, l, pos, randomRank(r), r.nextInt(12) == 0, "wilderness", r);
    }

    public static GateEntity spawnGate(MinecraftServer s, ServerLevel l, BlockPos pos, Rank rank, boolean red, String place, RandomSource r) {
        DungeonManager dm = DungeonManager.get(s);
        DungeonManager.Instance in = dm.create(s, rank, red, DungeonTheme.pick(rank, red, r));
        in.gateDim = l.dimension().location().toString();
        in.gatePos = pos;
        dm.setDirty();
        GateEntity g = ModEntities.GATE.get().create(l);
        if (g == null) return null;
        g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360, 0);
        g.setup(rank, red, in.id, place);
        g.breakAt = 20 * 60 * (30 + r.nextInt(30));
        if (!red && r.nextInt(100) < 45) {
            g.npcClearAt = 20 * 60 * (6 + r.nextInt(12));
            Guild[] guilds = {Guild.HUNTERS, Guild.WHITE_TIGER, Guild.FIEND, Guild.KNIGHTS};
            g.npcGuild = guilds[r.nextInt(guilds.length)].id();
            raidTeam(l, pos, r);
        }
        l.addFreshEntity(g);
        NewsManager.get(s).gateAppeared(s, rank, red, place);
        return g;
    }

    private static void raidTeam(ServerLevel l, BlockPos pos, RandomSource r) {
        int n = 2 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            HunterNpc h = ModEntities.NPC.get().create(l);
            if (h == null) continue;
            double a = r.nextDouble() * Math.PI * 2;
            BlockPos at = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.offset((int) (Math.cos(a) * 5), 0, (int) (Math.sin(a) * 5)));
            h.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
            h.setupRole("hunter", r);
            h.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            l.addFreshEntity(h);
        }
    }

    static void npcCleared(ServerLevel l, GateEntity g) {
        MinecraftServer s = l.getServer();
        DungeonManager.Instance in = DungeonManager.get(s).get(g.instance);
        if (in != null) {
            in.cleared = true;
            DungeonManager.get(s).setDirty();
        }
        NewsManager.get(s).gateCleared(s, g.rank(), Component.translatable("sololeveling.guild." + g.npcGuild));
        g.close();
    }

    static void dungeonBreak(ServerLevel l, GateEntity g) {
        MinecraftServer s = l.getServer();
        int n = 4 + g.rank().ordinal() * 2;
        for (int i = 0; i < n; i++) {
            MobKind k = g.breakMob();
            double a = l.random.nextDouble() * Math.PI * 2;
            BlockPos at = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.blockPosition().offset((int) (Math.cos(a) * 4), 0, (int) (Math.sin(a) * 4)));
            Structures.spawn(l, k, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, "");
        }
        DungeonManager.Instance in = DungeonManager.get(s).get(g.instance);
        if (in != null) {
            in.cleared = true;
            DungeonManager.get(s).setDirty();
        }
        NewsManager.get(s).dungeonBreak(s, g.rank(), g.place);
        g.close();
    }

    public static void onInstanceCleared(MinecraftServer s, DungeonManager.Instance in, Component by) {
        if (in.instant || in.gateDim.isEmpty()) return;
        ServerLevel l = s.getLevel(DungeonManager.dimKey(in.gateDim));
        if (l != null) {
            for (GateEntity g : l.getEntities(ModEntities.GATE.get(), e -> e.instance.equals(in.id))) g.close();
        }
        NewsManager.get(s).gateCleared(s, in.rank, by);
    }

    public static int count(MinecraftServer s) {
        int n = 0;
        for (DungeonManager.Instance i : DungeonManager.get(s).all()) if (!i.cleared && !i.instant && !i.gateDim.isEmpty()) n++;
        return n;
    }

    /** Active gates for the map and news screens. */
    public static ListTag mapList(MinecraftServer s) {
        ListTag l = new ListTag();
        for (DungeonManager.Instance i : DungeonManager.get(s).all()) {
            if (i.cleared || i.instant || i.gateDim.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putString("id", i.id);
            t.putString("dim", i.gateDim);
            t.putInt("x", i.gatePos.getX());
            t.putInt("y", i.gatePos.getY());
            t.putInt("z", i.gatePos.getZ());
            t.putString("rank", i.rank.label);
            t.putInt("color", i.red ? 0xFF2030 : i.rank.color);
            t.putBoolean("red", i.red);
            t.putString("theme", i.theme.id());
            t.putString("place", i.gateDim.equals(Regions.SEOUL.location().toString()) ? district(i.gatePos.getX(), i.gatePos.getZ()) : "wilderness");
            l.add(t);
        }
        return l;
    }

    public static boolean isSeoul(Level l) { return l.dimension() == Regions.SEOUL; }
}
