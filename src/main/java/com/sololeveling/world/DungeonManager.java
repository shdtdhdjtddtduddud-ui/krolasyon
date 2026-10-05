package com.sololeveling.world;

import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.GateEntity;
import com.sololeveling.entity.SLMonster;
import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModDimensions;
import com.sololeveling.registry.ModEntities;
import com.sololeveling.registry.ModItems;
import com.sololeveling.system.*;
import com.sololeveling.util.Ranks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Allocates, builds, populates and resolves dungeon instances. */
public class DungeonManager extends SavedData {
    public static class Inst {
        public long id;
        public String theme = "goblin_cave", rank = "E";
        public boolean red, cleared, broken;
        public BlockPos origin = BlockPos.ZERO, spawn = BlockPos.ZERO, exit = BlockPos.ZERO, boss = BlockPos.ZERO;
        public int minX, maxX, minZ, maxZ;
        public String returnDim = "minecraft:overworld";
        public BlockPos returnPos = BlockPos.ZERO;
        public UUID entryGate;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("id", id); t.putString("theme", theme); t.putString("rank", rank);
            t.putBoolean("red", red); t.putBoolean("cleared", cleared); t.putBoolean("broken", broken);
            t.putLong("origin", origin.asLong()); t.putLong("spawn", spawn.asLong()); t.putLong("exit", exit.asLong()); t.putLong("boss", boss.asLong());
            t.putInt("minX", minX); t.putInt("maxX", maxX); t.putInt("minZ", minZ); t.putInt("maxZ", maxZ);
            t.putString("rdim", returnDim); t.putLong("rpos", returnPos.asLong());
            if (entryGate != null) t.putUUID("gate", entryGate);
            return t;
        }

        static Inst load(CompoundTag t) {
            Inst i = new Inst();
            i.id = t.getLong("id"); i.theme = t.getString("theme"); i.rank = t.getString("rank");
            i.red = t.getBoolean("red"); i.cleared = t.getBoolean("cleared"); i.broken = t.getBoolean("broken");
            i.origin = BlockPos.of(t.getLong("origin")); i.spawn = BlockPos.of(t.getLong("spawn")); i.exit = BlockPos.of(t.getLong("exit")); i.boss = BlockPos.of(t.getLong("boss"));
            i.minX = t.getInt("minX"); i.maxX = t.getInt("maxX"); i.minZ = t.getInt("minZ"); i.maxZ = t.getInt("maxZ");
            i.returnDim = t.getString("rdim"); i.returnPos = BlockPos.of(t.getLong("rpos"));
            if (t.hasUUID("gate")) i.entryGate = t.getUUID("gate");
            return i;
        }
    }

    private final Map<Long, Inst> map = new HashMap<>();
    private long counter = 0;

    public static DungeonManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DungeonManager::load, DungeonManager::new, SoloLeveling.MODID + "_dungeons");
    }

    public static DungeonManager load(CompoundTag t) {
        DungeonManager m = new DungeonManager();
        m.counter = t.getLong("counter");
        for (Tag e : t.getList("i", Tag.TAG_COMPOUND)) {
            Inst i = Inst.load((CompoundTag) e);
            m.map.put(i.id, i);
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putLong("counter", counter);
        ListTag l = new ListTag();
        for (Inst i : map.values()) l.add(i.save());
        t.put("i", l);
        return t;
    }

    public Inst get(long id) { return map.get(id); }

    // ------------------------------------------------------------------ mob tables
    public static String[] mobsFor(String theme) {
        return switch (theme) {
            case "goblin_cave" -> new String[]{"goblin", "goblin", "orc"};
            case "temple" -> new String[]{"stone_soldier"};
            case "venom_swamp" -> new String[]{"venom_ant"};
            case "ice_cave" -> new String[]{"ice_elf"};
            case "hell_den" -> new String[]{"hell_hound"};
            case "ant_nest" -> new String[]{"giant_ant"};
            case "demon_castle" -> new String[]{"demon_knight"};
            default -> new String[]{"demon_knight", "hell_hound"};
        };
    }

    public static String bossFor(String theme) {
        return switch (theme) {
            case "goblin_cave" -> "goblin_warlord";
            case "temple" -> "statue_of_god";
            case "venom_swamp" -> "kasaka";
            case "ice_cave" -> "baruka";
            case "hell_den" -> "cerberus";
            case "ant_nest" -> "ant_king";
            case "demon_castle" -> "igris";
            default -> "kamish";
        };
    }

    // ------------------------------------------------------------------ build
    /** make sure the gate has a built instance; returns it */
    public static Inst ensure(ServerLevel entryLevel, GateEntity gate) {
        MinecraftServer server = entryLevel.getServer();
        DungeonManager dm = get(server);
        Inst inst = gate.dungeonId >= 0 ? dm.map.get(gate.dungeonId) : null;
        if (inst != null) return inst;
        ServerLevel dl = server.getLevel(ModDimensions.DUNGEON);
        if (dl == null) return null;
        inst = new Inst();
        inst.id = dm.counter++;
        inst.theme = gate.theme(); inst.rank = gate.rank(); inst.red = gate.isRed();
        inst.entryGate = gate.getUUID();
        inst.returnDim = entryLevel.dimension().location().toString();
        inst.returnPos = gate.blockPosition();
        int col = (int) (inst.id % 48), row = (int) (inst.id / 48);
        inst.origin = new BlockPos(col * 360 + 64, 70, row * 120);
        int rankIdx = Ranks.index(inst.rank);
        DungeonBuilder.Layout lay = DungeonBuilder.build(dl, inst.origin, inst.theme, inst.rank, inst.id, rankIdx);
        inst.spawn = lay.spawn; inst.exit = lay.exit; inst.boss = lay.boss;
        inst.minX = lay.minX; inst.maxX = lay.maxX; inst.minZ = lay.minZ; inst.maxZ = lay.maxZ;
        populate(dl, inst, lay);
        dm.map.put(inst.id, inst);
        dm.setDirty();
        gate.dungeonId = inst.id;
        return inst;
    }

    private static void populate(ServerLevel dl, Inst inst, DungeonBuilder.Layout lay) {
        RandomSource rnd = RandomSource.create(inst.id * 7 + 3);
        String[] pool = mobsFor(inst.theme);
        double hpMul = inst.red ? 1.5 : 1.0;
        for (DungeonBuilder.MobSpot s : lay.spots) {
            if (inst.red || rnd.nextInt(4) != 0) spawnMob(dl, pool[rnd.nextInt(pool.length)], s.pos(), inst, hpMul, false);
            if (inst.red && rnd.nextBoolean()) spawnMob(dl, pool[rnd.nextInt(pool.length)], s.pos().offset(1, 0, 1), inst, hpMul, false);
        }
        spawnMob(dl, bossFor(inst.theme), lay.boss, inst, hpMul, true);
        // exit gate
        GateEntity exit = ModEntities.GATE.get().create(dl);
        if (exit != null) {
            exit.moveTo(lay.exit.getX() + 0.5, lay.exit.getY(), lay.exit.getZ() + 0.5, 90, 0);
            exit.setup(inst.rank, inst.theme, false, GateEntity.EXIT);
            exit.dungeonId = inst.id;
            exit.returnDim = inst.returnDim;
            exit.returnPos = inst.returnPos;
            dl.addFreshEntity(exit);
        }
    }

    public static SLMonster spawnMob(ServerLevel level, String id, BlockPos pos, Inst inst, double mul, boolean boss) {
        EntityType<SLMonster> type = ModEntities.MONSTERS.get(id).get();
        SLMonster m = type.create(level);
        if (m == null) return null;
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        m.dungeonId = inst == null ? -1 : inst.id;
        m.setPersistenceRequired();
        if (mul != 1.0) {
            m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(m.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * mul);
            m.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(m.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * (1 + (mul - 1) * 0.6));
            m.setHealth(m.getMaxHealth());
        }
        level.addFreshEntity(m);
        return m;
    }

    // ------------------------------------------------------------------ events
    public static void onMobDeath(SLMonster m) {
        if (m.dungeonId < 0 || !(m.level() instanceof ServerLevel sl)) return;
        if (!m.def.boss()) return;
        DungeonManager dm = get(sl.getServer());
        Inst inst = dm.map.get(m.dungeonId);
        if (inst == null || inst.cleared || !m.def.id().equals(bossFor(inst.theme))) return;
        inst.cleared = true;
        dm.setDirty();
        clear(sl, inst, m);
    }

    private static void clear(ServerLevel dl, Inst inst, SLMonster boss) {
        MinecraftServer server = dl.getServer();
        int ri = Ranks.index(inst.rank);
        long baseXp = (long) (120 * Math.pow(2.0, ri) * (inst.red ? 1.6 : 1.0));
        StringBuilder names = new StringBuilder();
        for (ServerPlayer p : dl.players()) {
            if (p.getX() < inst.minX - 4 || p.getX() > inst.maxX + 4 || p.getZ() < inst.minZ - 4 || p.getZ() > inst.maxZ + 4) continue;
            SLPlayer d = ModCaps.get(p);
            d.gatesCleared++;
            d.dqGates++;
            Quests.onGateCleared(p, inst.rank);
            XpHandler.giveXp(p, baseXp);
            long gold = (long) (200 * Math.pow(2.2, ri));
            d.gold += gold;
            Sys.notify(p, Sys.REWARD, Component.translatable("gui.sololeveling.dungeon_cleared"),
                    Component.translatable("gui.sololeveling.clear_reward", baseXp, gold));
            PlayerSync.sync(p);
            if (names.length() > 0) names.append(", ");
            names.append(p.getGameProfile().getName());
        }
        rewardDrops(dl, boss.position().x, boss.position().y + 0.5, boss.position().z, inst);
        NewsManager.add(server, NewsManager.CLEARED, Component.translatable("news.sololeveling.cleared", Ranks.tag(inst.rank), Component.translatable("theme.sololeveling." + inst.theme), names.length() == 0 ? "?" : names.toString()));
        // remove the entry gate in the origin dimension
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(inst.returnDim));
        ServerLevel origin = server.getLevel(key);
        if (origin != null && inst.entryGate != null) {
            net.minecraft.world.entity.Entity g = origin.getEntity(inst.entryGate);
            if (g != null) g.discard();
        }
    }

    private static void rewardDrops(ServerLevel dl, double x, double y, double z, Inst inst) {
        RandomSource r = dl.random;
        int ri = Ranks.index(inst.rank);
        String[] cr = {"mana_crystal_e", "mana_crystal_d", "mana_crystal_c", "mana_crystal_b", "mana_crystal_a", "mana_crystal_s", "mana_crystal_s"};
        drop(dl, x, y, z, new ItemStack(ModItems.get(cr[ri]), 2 + r.nextInt(3) + (inst.red ? 3 : 0)));
        if (ri > 0) drop(dl, x, y, z, new ItemStack(ModItems.get(cr[ri - 1]), 3 + r.nextInt(4)));
        drop(dl, x, y, z, new ItemStack(ModItems.get("gold_coin"), 5 + r.nextInt(10) * (ri + 1)));
        drop(dl, x, y, z, new ItemStack(ModItems.get(ri >= 3 ? "hp_potion_large" : "hp_potion_medium"), 1 + r.nextInt(2)));
        drop(dl, x, y, z, new ItemStack(ModItems.get(ri >= 3 ? "mp_potion_large" : "mp_potion_small"), 1 + r.nextInt(2)));
        if (r.nextFloat() < 0.35F + (inst.red ? 0.3F : 0)) drop(dl, x, y, z, new ItemStack(ModItems.get("rune_stone")));
        if (r.nextFloat() < 0.2F + (inst.red ? 0.2F : 0)) drop(dl, x, y, z, new ItemStack(ModItems.get("skill_scroll")));
        if (r.nextFloat() < 0.5F) drop(dl, x, y, z, new ItemStack(ModItems.get("shadow_essence")));
        if (r.nextFloat() < 0.3F + (inst.red ? 0.25F : 0)) drop(dl, x, y, z, new ItemStack(ModItems.get(r.nextBoolean() ? "dungeon_key" : "red_gate_key")));
        // signature loot of boss
        String sig = switch (inst.theme) {
            case "goblin_cave" -> "iron_blade";
            case "temple" -> "knight_killer";
            case "venom_swamp" -> "kasaka_venom_fang";
            case "ice_cave" -> r.nextBoolean() ? "baruka_dagger" : "ice_elf_staff";
            case "hell_den" -> "obsidian_greatsword";
            case "ant_nest" -> r.nextBoolean() ? "demon_king_dagger" : "necromancer_staff";
            case "demon_castle" -> r.nextBoolean() ? "igris_blade" : "demon_monarch_longsword";
            default -> r.nextBoolean() ? "kamish_wrath" : "shadow_monarch_sword";
        };
        float chance = 0.35F + (inst.red ? 0.35F : 0F);
        if (r.nextFloat() < chance) drop(dl, x, y, z, new ItemStack(ModItems.get(sig)));
        if (ri >= 3 && r.nextFloat() < 0.25F) {
            String set = ri >= 5 ? (r.nextBoolean() ? "monarch" : "knight") : (r.nextBoolean() ? "ice" : "hunter");
            String[] pcs = {"helmet", "chestplate", "leggings", "boots"};
            drop(dl, x, y, z, new ItemStack(ModItems.get(set + "_" + pcs[r.nextInt(4)])));
        }
    }

    private static void drop(ServerLevel l, double x, double y, double z, ItemStack s) {
        ItemEntity e = new ItemEntity(l, x, y, z, s);
        e.setDeltaMovement((l.random.nextDouble() - 0.5) * 0.3, 0.35, (l.random.nextDouble() - 0.5) * 0.3);
        l.addFreshEntity(e);
    }

    /** unattended gate: monsters pour out */
    public static void dungeonBreak(GateEntity gate) {
        if (!(gate.level() instanceof ServerLevel sl)) return;
        String[] pool = mobsFor(gate.theme());
        int n = 4 + Ranks.index(gate.rank()) * 2;
        RandomSource r = sl.random;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 3 + r.nextDouble() * 6;
            BlockPos p = BlockPos.containing(gate.getX() + Math.cos(a) * d, gate.getY(), gate.getZ() + Math.sin(a) * d);
            int y = Travel.surfaceY(sl, p.getX(), p.getZ());
            SLMonster m = spawnMob(sl, pool[r.nextInt(pool.length)], new BlockPos(p.getX(), y, p.getZ()), null, 1.0, false);
            if (m != null) m.setPersistenceRequired();
        }
        if (Ranks.index(gate.rank()) >= 3) {
            int y = Travel.surfaceY(sl, gate.blockPosition().getX() + 4, gate.blockPosition().getZ());
            spawnMob(sl, bossFor(gate.theme()), new BlockPos(gate.blockPosition().getX() + 4, y, gate.blockPosition().getZ()), null, 1.0, true);
        }
        sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("gui.sololeveling.dungeon_break_warn", Ranks.tag(gate.rank()), (int) gate.getX(), (int) gate.getZ()), false);
        NewsManager.add(sl.getServer(), NewsManager.BREAK, Component.translatable("news.sololeveling.break", Ranks.tag(gate.rank()), Component.translatable("theme.sololeveling." + gate.theme()), (int) gate.getX(), (int) gate.getZ()));
    }
}
