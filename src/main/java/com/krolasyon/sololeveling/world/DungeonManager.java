package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.*;

/** All dungeon instances behind gates (and instant dungeons). Instances live in the dungeon dimension along z = +60000. */
public class DungeonManager extends SavedData {
    public static final int INSTANCE_Z = 60000, SPACING = 400;
    final Map<String, Instance> instances = new LinkedHashMap<>();

    public static class Instance {
        public String id;
        public DungeonTheme theme;
        public Rank rank;
        public boolean red, built, cleared, instant;
        public BlockPos origin = BlockPos.ZERO, spawn = BlockPos.ZERO, boss = BlockPos.ZERO;
        public String gateDim = "";
        public BlockPos gatePos = BlockPos.ZERO;
        public long seed;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("id", id);
            t.putInt("theme", theme.ordinal());
            t.putInt("rank", rank.ordinal());
            t.putBoolean("red", red);
            t.putBoolean("built", built);
            t.putBoolean("cleared", cleared);
            t.putBoolean("instant", instant);
            t.putLong("origin", origin.asLong());
            t.putLong("spawn", spawn.asLong());
            t.putLong("boss", boss.asLong());
            t.putString("gateDim", gateDim);
            t.putLong("gatePos", gatePos.asLong());
            t.putLong("seed", seed);
            return t;
        }

        static Instance load(CompoundTag t) {
            Instance i = new Instance();
            i.id = t.getString("id");
            i.theme = DungeonTheme.byOrdinal(t.getInt("theme"));
            i.rank = Rank.byOrdinal(t.getInt("rank"));
            i.red = t.getBoolean("red");
            i.built = t.getBoolean("built");
            i.cleared = t.getBoolean("cleared");
            i.instant = t.getBoolean("instant");
            i.origin = BlockPos.of(t.getLong("origin"));
            i.spawn = BlockPos.of(t.getLong("spawn"));
            i.boss = BlockPos.of(t.getLong("boss"));
            i.gateDim = t.getString("gateDim");
            i.gatePos = BlockPos.of(t.getLong("gatePos"));
            i.seed = t.getLong("seed");
            return i;
        }

        public AABB area() {
            return new AABB(origin.getX() - 20, 0, origin.getZ() - 120, origin.getX() + 260, 255, origin.getZ() + 120);
        }
    }

    public static DungeonManager get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(DungeonManager::load, DungeonManager::new, "sololeveling_dungeons");
    }

    static DungeonManager load(CompoundTag t) {
        DungeonManager m = new DungeonManager();
        for (String k : t.getAllKeys()) m.instances.put(k, Instance.load(t.getCompound(k)));
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        instances.forEach((k, v) -> t.put(k, v.save()));
        return t;
    }

    public Instance get(String id) { return instances.get(id); }

    public Instance create(MinecraftServer s, Rank rank, boolean red, DungeonTheme theme) {
        WorldState ws = WorldState.get(s);
        int slot = ws.nextInstance++;
        ws.setDirty();
        Instance i = new Instance();
        i.id = "d" + slot;
        i.theme = theme;
        i.rank = rank;
        i.red = red;
        i.origin = new BlockPos(slot * SPACING, DungeonBuilder.Y0, INSTANCE_Z);
        i.seed = s.overworld().getSeed() ^ (slot * 0x9E3779B97F4A7C15L);
        instances.put(i.id, i);
        // keep the save small: forget old cleared instances
        if (instances.size() > 120) {
            Iterator<Map.Entry<String, Instance>> it = instances.entrySet().iterator();
            while (instances.size() > 100 && it.hasNext()) {
                if (it.next().getValue().cleared) it.remove();
            }
        }
        setDirty();
        return i;
    }

    private void ensureBuilt(ServerLevel dl, Instance i) {
        if (i.built) return;
        DungeonBuilder.Result r = DungeonBuilder.build(dl, i.origin, i.theme, i.rank, i.red, i.id, i.seed);
        i.spawn = r.spawn();
        i.boss = r.bossRoom();
        i.built = true;
        setDirty();
    }

    public void enterById(ServerPlayer p, String id) {
        Instance i = instances.get(id);
        if (i == null) {
            Sys.warn(p, "gate.gone");
            return;
        }
        enter(p, i);
    }

    public void enter(ServerPlayer p, Instance i) {
        ServerLevel dl = p.server.getLevel(Regions.DUNGEON);
        if (dl == null) return;
        if (i.cleared) {
            Sys.warn(p, "gate.cleared");
            return;
        }
        HunterData d = HunterCapability.get(p);
        if (!d.awakened) {
            Sys.warn(p, "gate.not_awakened");
            return;
        }
        Regions.rememberReturn(p);
        ensureBuilt(dl, i);
        p.teleportTo(dl, i.spawn.getX() + 0.5, i.spawn.getY(), i.spawn.getZ() + 0.5, p.getYRot(), 0);
        p.fallDistance = 0;
        Sys.notify(p, i.red ? Sys.WARN : Sys.INFO, Sys.t("gate.entered_title", i.rank.label),
                Sys.t(i.red ? "gate.entered_red" : "gate.entered", Component.translatable("sololeveling.theme." + i.theme.id())));
        if (d.rank.ordinal() + 1 < i.rank.ordinal()) Sys.warn(p, "gate.too_strong");
    }

    /** Instant Dungeon Key: a private dungeon matching the hunter's rank. */
    public boolean enterInstant(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (p.level().dimension() == Regions.DUNGEON) {
            Sys.warn(p, "instant.inside");
            return false;
        }
        Rank r = Rank.forLevel(d.level);
        if (r == Rank.NATIONAL) r = Rank.S;
        Instance i = create(p.server, r, false, DungeonTheme.pick(r, false, p.getRandom()));
        i.instant = true;
        i.gateDim = p.level().dimension().location().toString();
        i.gatePos = p.blockPosition();
        enter(p, i);
        return true;
    }

    public void onBossKilled(ServerLevel l, String instanceId, SLMonster boss) {
        if (instanceId.startsWith("region:")) {
            RegionBuilder.onBossKilled(l, instanceId.substring(7), boss);
            return;
        }
        Instance i = instances.get(instanceId);
        if (i == null || i.cleared) return;
        i.cleared = true;
        setDirty();
        DungeonBuilder.exitPortal(l, i.boss.offset(0, 0, 4), i.theme);
        long gold = 150L + i.rank.ordinal() * 250L;
        Component by = Component.translatable("sololeveling.news.a_hunter");
        for (ServerPlayer p : l.getEntitiesOfClass(ServerPlayer.class, i.area())) {
            by = p.getName();
            HunterData d = HunterCapability.get(p);
            d.gatesCleared++;
            d.gold += gold;
            d.markDirty();
            Progression.addExp(p, 40L + i.rank.ordinal() * 120L);
            Sys.notify(p, Sys.REWARD, Sys.t("gate.cleared_title"), Sys.t("gate.cleared_body", i.rank.label, gold));
            GuildManager.onGateCleared(p, i.rank);
            Progression.checkQuest(p);
        }
        GateManager.onInstanceCleared(l.getServer(), i, by);
        // everyone still inside is sent home after a while (the gate closes)
        Scheduler.later(20 * 90, () -> {
            for (ServerPlayer p : l.getEntitiesOfClass(ServerPlayer.class, i.area())) {
                Sys.info(p, "gate.closing");
                Regions.returnPlayer(p);
            }
        });
    }

    /** A player fell out of the world in the dungeon dimension. */
    public void rescue(ServerPlayer p) {
        for (Instance i : instances.values()) {
            if (i.built && i.area().contains(p.getX(), 100, p.getZ())) {
                p.teleportTo(i.spawn.getX() + 0.5, i.spawn.getY(), i.spawn.getZ() + 0.5);
                p.fallDistance = 0;
                return;
            }
        }
        BlockPos sp = RegionBuilder.nearestSpawn(p.blockPosition());
        if (sp != null) {
            p.teleportTo(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
            p.fallDistance = 0;
            return;
        }
        Regions.returnPlayer(p);
    }

    public static ResourceKey<Level> dimKey(String s) {
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, rl);
    }

    public Collection<Instance> all() { return instances.values(); }
}
