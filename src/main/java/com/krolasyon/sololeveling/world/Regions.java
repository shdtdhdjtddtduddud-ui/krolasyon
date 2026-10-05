package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModSounds;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The regions of the world, their waypoints and access rules. Seoul is its own dimension, all dungeons and special
 * regions live in the dungeon dimension at fixed, far apart coordinates.
 */
public final class Regions {
    public static final ResourceKey<Level> SEOUL = ResourceKey.create(Registries.DIMENSION, SoloLeveling.id("seoul"));
    public static final ResourceKey<Level> DUNGEON = ResourceKey.create(Registries.DIMENSION, SoloLeveling.id("dungeon"));

    /** Region instances in the dungeon dimension are built along z = REGION_Z. */
    public static final int REGION_Z = -60000;

    public enum Kind { CITY, SPECIAL, OVERWORLD }

    public record Waypoint(String id, ResourceKey<Level> dim, BlockPos pos, Kind kind, float mapX, float mapY, int icon) {}

    public static final Map<String, Waypoint> WAYPOINTS = new LinkedHashMap<>();

    static {
        int g = CityPlan.GROUND + 1;
        // Seoul (city dimension) — map coordinates are in 0..1 of the map picture
        add("seoul_plaza", SEOUL, CityPlan.landmarkDoor(0, 0), Kind.CITY, 0.50F, 0.50F, 0);
        add("association", SEOUL, CityPlan.landmarkDoor(1, 0), Kind.CITY, 0.60F, 0.50F, 1);
        add("guild_hunters", SEOUL, CityPlan.landmarkDoor(-1, 0), Kind.CITY, 0.40F, 0.50F, 2);
        add("guild_white_tiger", SEOUL, CityPlan.landmarkDoor(0, 1), Kind.CITY, 0.50F, 0.60F, 2);
        add("guild_fiend", SEOUL, CityPlan.landmarkDoor(0, -1), Kind.CITY, 0.50F, 0.40F, 2);
        add("guild_knights", SEOUL, CityPlan.landmarkDoor(1, 1), Kind.CITY, 0.60F, 0.60F, 2);
        add("guild_ahjin", SEOUL, CityPlan.landmarkDoor(-1, -1), Kind.CITY, 0.40F, 0.40F, 2);
        add("hospital", SEOUL, CityPlan.landmarkDoor(1, -1), Kind.CITY, 0.60F, 0.40F, 3);
        add("market", SEOUL, CityPlan.landmarkDoor(-1, 1), Kind.CITY, 0.40F, 0.60F, 4);
        add("home", SEOUL, CityPlan.landmarkDoor(2, 0), Kind.CITY, 0.70F, 0.50F, 5);
        // special regions (dungeon dimension)
        add("double_dungeon", DUNGEON, new BlockPos(0, 81, REGION_Z), Kind.SPECIAL, 0.22F, 0.30F, 6);
        add("job_change", DUNGEON, new BlockPos(1000, 81, REGION_Z), Kind.SPECIAL, 0.80F, 0.22F, 7);
        add("demon_castle", DUNGEON, new BlockPos(2000, 81, REGION_Z), Kind.SPECIAL, 0.84F, 0.74F, 8);
        add("jeju_island", DUNGEON, new BlockPos(3000, 81, REGION_Z), Kind.SPECIAL, 0.25F, 0.88F, 9);
        add("penalty_zone", DUNGEON, new BlockPos(4000, 81, REGION_Z), Kind.SPECIAL, 0.12F, 0.70F, 10);
        add("overworld", Level.OVERWORLD, BlockPos.ZERO, Kind.OVERWORLD, 0.12F, 0.12F, 11);
    }

    private static void add(String id, ResourceKey<Level> dim, BlockPos pos, Kind k, float mx, float my, int icon) {
        WAYPOINTS.put(id, new Waypoint(id, dim, pos, k, mx, my, icon));
    }

    private Regions() {}

    /** Whether the player may travel to a waypoint right now; returns a lang key explaining why not, or null. */
    public static String lockReason(ServerPlayer p, String id) {
        HunterData d = HunterCapability.get(p);
        switch (id) {
            case "job_change" -> {
                if (d.level < 40) return "region.locked_level40";
                if (d.job != Job.NONE) return "region.job_done";
            }
            case "demon_castle" -> {
                if (!d.waypoints.contains("demon_castle")) return "region.locked_key";
            }
            case "jeju_island" -> {
                if (d.rank.ordinal() < Rank.S.ordinal() && !d.waypoints.contains("jeju_island")) return "region.locked_s";
            }
            case "penalty_zone" -> {
                return "region.locked_penalty";
            }
            default -> {}
        }
        if (d.penaltyTicks > 0) return "region.in_penalty";
        return null;
    }

    public static void travelFromMap(ServerPlayer p, String id) {
        HunterData d = HunterCapability.get(p);
        if (!WAYPOINTS.containsKey(id) && !id.startsWith("gate:")) return;
        String why = id.startsWith("gate:") ? (d.penaltyTicks > 0 ? "region.in_penalty" : null) : lockReason(p, id);
        if (why != null) {
            Sys.warn(p, why);
            return;
        }
        if (d.mapTravelCooldown > 0 && !p.isCreative()) {
            Sys.warn(p, "map.cooldown", d.mapTravelCooldown / 20);
            return;
        }
        d.mapTravelCooldown = 20 * 20;
        teleport(p, id);
    }

    /** Sends the player to a waypoint (or "return" = where they came from before a dungeon). */
    public static void teleport(ServerPlayer p, String id) {
        MinecraftServer s = p.server;
        HunterData d = HunterCapability.get(p);
        if (id.startsWith("gate:")) {
            DungeonManager.Instance in = DungeonManager.get(s).get(id.substring(5));
            if (in == null || in.cleared || in.gateDim.isEmpty()) {
                Sys.warn(p, "gate.gone");
                return;
            }
            ServerLevel gl = s.getLevel(DungeonManager.dimKey(in.gateDim));
            if (gl == null) return;
            BlockPos gp = gl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, in.gatePos.offset(4, 0, 0));
            p.teleportTo(gl, gp.getX() + 0.5, gp.getY(), gp.getZ() + 0.5, 90, 0);
            p.fallDistance = 0;
            return;
        }
        if (id.equals("return")) {
            returnPlayer(p);
            return;
        }
        Waypoint w = WAYPOINTS.get(id);
        if (w == null) return;
        String why = id.equals("penalty_zone") ? null : lockReason(p, id);
        if (why != null) {
            Sys.warn(p, why);
            return;
        }
        ServerLevel level = s.getLevel(w.dim());
        if (level == null) {
            Sys.warn(p, "region.missing");
            return;
        }
        BlockPos pos = w.pos();
        if (w.kind() == Kind.SPECIAL) {
            rememberReturn(p);
            RegionBuilder.ensure(level, id);
            RegionBuilder.onEnter(level, id);
            pos = RegionBuilder.spawnOf(id);
        } else if (w.kind() == Kind.OVERWORLD) {
            WorldState ws = WorldState.get(s);
            ws.ensureOverworldPortal(s);
            BlockPos b = ws.overworldPortalPos;
            pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.offset(0, 0, 4));
        } else {
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }
        Vec3 at = Vec3.atBottomCenterOf(pos);
        p.teleportTo(level, at.x, at.y, at.z, p.getYRot(), 0);
        p.fallDistance = 0;
        level.playSound(null, pos, ModSounds.GATE_ENTER.get(), SoundSource.PLAYERS, 1F, 1F);
        if (d.waypoints.add(id)) {
            d.markDirty();
            Sys.notify(p, Sys.INFO, Sys.t("map.discovered"), Sys.t("region." + id));
        }
        Net.to(p, new Net.Fx("region", 0, 0, 0, w.icon()));
    }

    public static void rememberReturn(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (p.level().dimension() == DUNGEON) return;
        CompoundTag r = new CompoundTag();
        r.putString("dim", p.level().dimension().location().toString());
        r.putDouble("x", p.getX());
        r.putDouble("y", p.getY());
        r.putDouble("z", p.getZ());
        d.misc.put("return", r);
        d.markDirty();
    }

    public static void returnPlayer(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        CompoundTag r = d.misc.getCompound("return");
        ServerLevel level = null;
        if (r.contains("dim")) {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(r.getString("dim"));
            if (rl != null) level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        }
        if (level == null || level.dimension() == DUNGEON) {
            teleport(p, "seoul_plaza");
            return;
        }
        p.teleportTo(level, r.getDouble("x"), r.getDouble("y"), r.getDouble("z"), p.getYRot(), 0);
        p.fallDistance = 0;
    }

    public static void sendToPenalty(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (!p.isAlive()) return;
        teleport(p, "penalty_zone");
        d.penaltyTicks = 20 * 60 * 4;
        d.markDirty();
        Sys.notify(p, Sys.WARN, Sys.t("penalty.title"), Sys.t("penalty.body"));
    }

    public static void playerTick(ServerPlayer p, HunterData d) {
        if (d.penaltyTicks > 0) {
            d.penaltyTicks--;
            if (d.penaltyTicks % 20 == 0) d.markDirty();
            if (p.tickCount % 100 == 0 && p.level().dimension() == DUNGEON) RegionBuilder.penaltyWave((ServerLevel) p.level(), p);
            if (d.penaltyTicks == 0) {
                Sys.notify(p, Sys.REWARD, Sys.t("penalty.title"), Sys.t("penalty.survived"));
                returnPlayer(p);
            }
        }
        if (p.level().dimension() == DUNGEON && p.getY() < 5 && p.isAlive()) {
            // fell out of an instance
            DungeonManager.get(p.server).rescue(p);
        }
    }

    public static void onPlayerDeath(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (d.penaltyTicks > 0) {
            d.penaltyTicks = 0;
            d.markDirty();
        }
    }

    public static void onEnterDimension(ServerPlayer p, ResourceKey<Level> to) {
        HunterData d = HunterCapability.get(p);
        if (to == SEOUL && d.waypoints.add("seoul_plaza")) d.markDirty();
    }

    public static void openMap(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Waypoint w : WAYPOINTS.values()) {
            CompoundTag e = new CompoundTag();
            e.putString("id", w.id());
            e.putFloat("x", w.mapX());
            e.putFloat("y", w.mapY());
            e.putInt("icon", w.icon());
            e.putBoolean("known", d.waypoints.contains(w.id()) || w.kind() != Kind.SPECIAL);
            String why = lockReason(p, w.id());
            if (why != null) e.putString("lock", why);
            l.add(e);
        }
        t.put("points", l);
        t.put("gates", GateManager.mapList(p.server));
        t.putString("dim", p.level().dimension().location().toString());
        t.putInt("px", p.getBlockX());
        t.putInt("pz", p.getBlockZ());
        Net.to(p, new Net.Open("map", t));
    }
}
