package com.krolasyon.bosses.rpg.town;

import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.krolasyon.bosses.rpg.npc.NpcRole;
import com.krolasyon.bosses.rpg.npc.RpgNpc;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import com.krolasyon.bosses.rpg.world.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Turns a {@link Site} into a list of build operations: terrain, walls, roads, buildings, people. */
public final class SettlementGen {
    private SettlementGen() {}

    record Resident(int x, int y, int z, NpcRole role, int count) {}

    static final class Plan {
        final List<Canvas.Op> ops = new ArrayList<>();
        final List<Canvas.Op> late = new ArrayList<>();
        final List<Resident> people = new ArrayList<>();
        final List<int[]> lots = new ArrayList<>();
        final Site site;
        final int y;
        final Palette p;
        final RandomSource r;

        Plan(Site site, int y, Palette p, RandomSource r) { this.site = site; this.y = y; this.p = p; this.r = r; }

        Canvas at(int x, int z, int rot, int w, int d) { return Canvas.centred(ops, x, y, z, rot, w, d); }

        Canvas flat() { return new Canvas(ops, site.x, y, site.z, 0, 0, 0); }

        void people(int x, int z, NpcRole role, int n) { people.add(new Resident(x, y + 1, z, role, n)); }

        boolean free(int x, int z, int half) {
            for (int[] l : lots) if (Math.abs(l[0] - x) < l[2] + half + 2 && Math.abs(l[1] - z) < l[2] + half + 2) return false;
            lots.add(new int[]{x, z, half});
            return true;
        }
    }

    /** rotation so that the building front faces from (x,z) toward (tx,tz) */
    static int faceToward(int x, int z, int tx, int tz) {
        int dx = tx - x, dz = tz - z;
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? 1 : 3;
        return dz < 0 ? 0 : 2;
    }

    public static int baseHeight(ServerLevel level, Site s) {
        int sum = 0, n = 0, water = 0, total = 0;
        int rad = Math.min(30, s.type.radius / 2);
        int step = Math.max(4, rad / 4);
        for (int dx = -rad; dx <= rad; dx += step) for (int dz = -rad; dz <= rad; dz += step) {
            int floor = com.krolasyon.bosses.rpg.util.Heights.get(level, Heightmap.Types.OCEAN_FLOOR, s.x + dx, s.z + dz);
            int top = com.krolasyon.bosses.rpg.util.Heights.get(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.x + dx, s.z + dz);
            total++;
            if (top > floor + 1) { water++; continue; }
            sum += floor;
            n++;
        }
        int sea = level.getSeaLevel();
        if (n == 0 || water * 2 > total) return Math.max(sea + 1, level.getMinBuildHeight() + 2);
        int y = sum / n;
        if (water > 0) y = Math.max(y, sea + 1);
        return Math.min(y, Math.max(sea + 45, level.getMinBuildHeight() + 60));
    }

    // ------------------------------------------------------------------ terrain
    private static void terrain(Plan pl, int rad, Block surface) {
        final int cx = pl.site.x, cz = pl.site.z, y = pl.y, blend = 14;
        BlockState top = surface.defaultBlockState();
        for (int dx = -rad - blend; dx <= rad + blend; dx++) {
            for (int dz = -rad - blend; dz <= rad + blend; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > rad + blend) continue;
                final int x = cx + dx, z = cz + dz;
                final double t = dist <= rad ? 1.0 : 1.0 - (dist - rad) / blend;
                pl.ops.add(new Canvas.ActOp(level -> column(level, x, z, y, t, top)));
            }
        }
    }

    private static void column(ServerLevel level, int x, int z, int base, double t, BlockState top) {
        int ground = com.krolasyon.bosses.rpg.util.Heights.get(level, Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        int surface = com.krolasyon.bosses.rpg.util.Heights.get(level, Heightmap.Types.WORLD_SURFACE, x, z);
        int target = (int) Math.round(base * t + ground * (1 - t));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        if (ground <= level.getMinBuildHeight() + 1) return;
        for (int yy = Math.max(Math.min(ground, target - 6), target - 14); yy < target; yy++) {
            m.set(x, yy, z);
            BlockState cur = level.getBlockState(m);
            if (yy > ground || cur.isAir() || !cur.getFluidState().isEmpty() || cur.canBeReplaced())
                level.setBlock(m, yy < target - 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
        }
        m.set(x, target, z);
        level.setBlock(m, t >= 1.0 ? top : Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int yy = target + 1; yy <= Math.min(Math.max(surface, target + 1) + 1, target + 48); yy++) {
            m.set(x, yy, z);
            if (!level.getBlockState(m).isAir()) level.setBlock(m, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void road(Plan pl, int x1, int z1, int x2, int z2, int width, Block b) {
        Canvas c = pl.flat();
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps), z = z1 + (z2 - z1) * i / Math.max(1, steps);
            for (int a = -width / 2; a <= width / 2; a++) for (int b2 = -width / 2; b2 <= width / 2; b2++) {
                c.set(x + a, 0, z + b2, pl.r.nextInt(6) == 0 && b != Blocks.DIRT_PATH ? Blocks.GRAVEL : b);
            }
        }
    }

    private static void ringWall(Plan pl, int rad, int h, boolean palisade) {
        Canvas c = pl.flat();
        Palette p = pl.p;
        Block wall = palisade ? (p == Palette.ORC || p == Palette.BANDIT ? Blocks.STRIPPED_DARK_OAK_LOG : p.cityWall()) : p.cityWall();
        int pts = (int) (rad * Math.PI * 2 * 1.2);
        for (int i = 0; i < pts; i++) {
            double a = i * Math.PI * 2 / pts;
            int x = (int) Math.round(Math.cos(a) * rad), z = (int) Math.round(Math.sin(a) * rad);
            boolean gate = Math.abs(x) <= 3 && Math.abs(z) > rad - 3 || Math.abs(z) <= 3 && Math.abs(x) > rad - 3;
            if (gate) {
                if (!palisade) c.fill(x, h - 1, z, x, h, z, wall);
                continue;
            }
            for (int y = 1; y <= h; y++) c.set(x, y, z, wall);
            if (!palisade && i % 2 == 0) c.set(x, h + 1, z, p.wallTop());
            int xi = (int) Math.round(Math.cos(a) * (rad - 1)), zi = (int) Math.round(Math.sin(a) * (rad - 1));
            if (!palisade) c.set(xi, h - 1, zi, wall);
        }
        if (palisade) return;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            int tx = (int) Math.round(Math.cos(a) * rad), tz = (int) Math.round(Math.sin(a) * rad);
            c.fill(tx - 2, 1, tz - 2, tx + 2, h + 4, tz + 2, wall);
            c.fill(tx - 1, h + 1, tz - 1, tx + 1, h + 4, tz + 1, Blocks.AIR);
            for (int k = -2; k <= 2; k += 2) { c.set(tx + k, h + 5, tz - 2, p.wallTop()); c.set(tx + k, h + 5, tz + 2, p.wallTop()); c.set(tx - 2, h + 5, tz + k, p.wallTop()); c.set(tx + 2, h + 5, tz + k, p.wallTop()); }
            c.set(tx, h + 1, tz, p.light());
        }
        for (int[] g : new int[][]{{0, -rad}, {0, rad}, {-rad, 0}, {rad, 0}}) {
            for (int k = -4; k <= 4; k += 8) {
                int gx = g[0] == 0 ? k : g[0], gz = g[1] == 0 ? k : g[1];
                c.fill(gx - 1, 1, gz - 1, gx + 1, h + 3, gz + 1, wall);
                c.set(gx, h + 4, gz, p.light());
            }
        }
    }

    // ------------------------------------------------------------------ settlement kinds
    public static List<Canvas.Op> plan(ServerLevel level, Site s, RpgWorldData w) {
        RandomSource r = RandomSource.create(s.seed);
        int y = s.y == Integer.MIN_VALUE ? baseHeight(level, s) : s.y;
        s.y = y;
        Palette p = s.type == Site.Type.CAMP ? Palette.BANDIT : Palette.of(s.kingdom);
        Plan pl = new Plan(s, y, p, r);
        switch (s.type) {
            case CAPITAL -> capital(pl, w);
            case CITY -> city(pl);
            case VILLAGE -> village(pl);
            case CAMP -> camp(pl);
            case LAIR -> lair(pl);
            case RUIN -> ruinSite(pl);
            case LANDMARK -> landmark(pl);
        }
        // people last, once the buildings stand
        List<Resident> people = pl.people;
        pl.ops.add(new Canvas.ActOp(l -> populate(l, s, people)));
        pl.ops.addAll(pl.late);
        return pl.ops;
    }

    /** story anchors are set only once everything placed before them stands */
    private static void anchor(Plan pl, RpgWorldData w, String key, BlockPos pos) {
        pl.late.add(new Canvas.ActOp(l -> w.setAnchor(key, pos)));
    }

    private static void plaza(Plan pl, int rad) {
        Canvas c = pl.flat();
        for (int x = -rad; x <= rad; x++) for (int z = -rad; z <= rad; z++) if (x * x + z * z <= rad * rad) c.set(x, 0, z, pl.p.plaza());
        Canvas f = new Canvas(pl.ops, pl.site.x, pl.y, pl.site.z, 0, 0, 0);
        if (rad >= 8) Buildings.fountain(f, pl.p, 3);
        else Buildings.well(Canvas.centred(pl.ops, pl.site.x, pl.y, pl.site.z, 0, 5, 5), pl.p);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int lx = (int) Math.round(Math.cos(a) * (rad - 1)), lz = (int) Math.round(Math.sin(a) * (rad - 1));
            c.fill(lx, 1, lz, lx, 2, lz, pl.p.fence());
            c.set(lx, 3, lz, pl.p.light());
        }
    }

    private static void capital(Plan pl, RpgWorldData w) {
        Site s = pl.site;
        int R = s.type.radius;
        boolean giant = pl.p == Palette.GIANT;
        terrain(pl, R + 2, pl.p.path());
        ringWall(pl, R - 2, giant ? 12 : 9, false);
        road(pl, s.x - R + 2, s.z, s.x + R - 2, s.z, 5, pl.p.path());
        road(pl, s.x, s.z - R + 2, s.x, s.z + R - 2, 5, pl.p.path());
        plaza(pl, 12);
        pl.free(s.x, s.z, 14);
        pl.free(s.x, s.z - R / 2, 3);
        int k = s.kingdom;
        boolean aldoria = k == Kingdom.ALDORIA.ordinal();
        // palace in the north
        int px = s.x, pz = s.z - 46;
        Buildings.palace(pl.at(px, pz, 2, 31, 23), pl.p, pl.r, 31, 23);
        pl.free(px, pz, 18);
        pl.people(px, pz, NpcRole.RULER, 1);
        pl.people(px, pz + 4, NpcRole.KNIGHT, 3);
        pl.people(px, pz - 2, NpcRole.NOBLE, 2);
        if (aldoria) anchor(pl, w, "palace", new BlockPos(px, pl.y + 1, pz));
        // temple, guild, mage tower in the east
        int tx = s.x + 30, tz = s.z - 26;
        Buildings.temple(pl.at(tx, tz, faceToward(tx, tz, s.x, s.z), 13, 19), pl.p, pl.r, 13, 19);
        pl.free(tx, tz, 11);
        pl.people(tx, tz, NpcRole.PRIEST, 1);
        if (aldoria) anchor(pl, w, "temple", new BlockPos(tx, pl.y + 1, tz));
        int gx = s.x - 26, gz = s.z + 22;
        Buildings.guildHall(pl.at(gx, gz, faceToward(gx, gz, s.x, s.z), 13, 11), pl.p, pl.r, 13, 11);
        pl.free(gx, gz, 8);
        pl.people(gx, gz, NpcRole.GUILD_MASTER, 1);
        pl.people(gx, gz, NpcRole.ADVENTURER, 2);
        if (aldoria) anchor(pl, w, "guild", new BlockPos(gx, pl.y + 1, gz));
        int mx = s.x + 44, mz = s.z + 10;
        Buildings.mageTower(pl.at(mx, mz, faceToward(mx, mz, s.x, s.z), 9, 9), pl.p, pl.r);
        pl.free(mx, mz, 6);
        pl.people(mx, mz, NpcRole.MAGE, 1);
        int bx = s.x + 26, bz = s.z + 24;
        Buildings.tavern(pl.at(bx, bz, faceToward(bx, bz, s.x, s.z), 11, 9), pl.p, pl.r, 11, 9);
        pl.free(bx, bz, 7);
        pl.people(bx, bz, NpcRole.INNKEEPER, 1);
        int sx = s.x + 14, sz = s.z + 38;
        Buildings.smithy(pl.at(sx, sz, faceToward(sx, sz, s.x, s.z), 9, 7), pl.p, pl.r, 9, 7);
        pl.free(sx, sz, 6);
        pl.people(sx, sz, NpcRole.BLACKSMITH, 1);
        int kx = s.x - 14, kz = s.z + R - 18;
        Buildings.barracks(pl.at(kx, kz, faceToward(kx, kz, s.x, s.z + R), 13, 7), pl.p, pl.r, 13, 7);
        pl.free(kx, kz, 8);
        // market stalls round the plaza
        Block[] awnings = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL, Blocks.PURPLE_WOOL};
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + Math.PI / 6;
            int x = s.x + (int) Math.round(Math.cos(a) * 17), z = s.z + (int) Math.round(Math.sin(a) * 17);
            Buildings.stall(pl.at(x, z, faceToward(x, z, s.x, s.z), 4, 4), pl.p, pl.r, awnings[i]);
            pl.free(x, z, 3);
            pl.people(x, z, NpcRole.MERCHANT, 1);
        }
        if (Kingdom.of(k).slavery) {
            int vx = s.x + 40, vz = s.z + 40;
            Buildings.slaveMarket(pl.at(vx, vz, faceToward(vx, vz, s.x, s.z), 11, 9), pl.p, pl.r);
            pl.free(vx, vz, 7);
            pl.people(vx, vz - 2, NpcRole.SLAVER, 1);
            pl.people(vx, vz + 2, NpcRole.SLAVE, 3);
        }
        if (aldoria) {
            anchor(pl, w, "square", new BlockPos(s.x + 6, pl.y + 1, s.z + 6));
            // the slums of the south west: the Mud Quarter
            int hx = s.x - 40, hz = s.z + 46;
            Buildings.familyHome(pl.at(hx, hz, faceToward(hx, hz, s.x, s.z + 46), 7, 7), pl.r);
            pl.free(hx, hz, 5);
            anchor(pl, w, "family_home", new BlockPos(hx, pl.y + 1, hz));
            for (int i = 0; i < 14; i++) {
                int x = s.x - 62 + pl.r.nextInt(40), z = s.z + 30 + pl.r.nextInt(40);
                if ((x - s.x) * (x - s.x) + (z - s.z) * (z - s.z) > (R - 10) * (R - 10)) continue;
                if (Math.abs(x - s.x) < 5 || Math.abs(z - s.z) < 5 || !pl.free(x, z, 3)) continue;
                int ww = 5 + pl.r.nextInt(2);
                Buildings.shack(pl.at(x, z, pl.r.nextInt(4), ww, 5), Palette.SLUM, pl.r, ww, 5);
                pl.people(x, z, pl.r.nextInt(3) == 0 ? NpcRole.BEGGAR : NpcRole.PEASANT, 1 + pl.r.nextInt(2));
            }
        }
        // noble mansions north-west (the high society), houses everywhere else
        for (int i = 0; i < 70; i++) {
            int x = s.x + pl.r.nextInt(2 * R - 24) - R + 12, z = s.z + pl.r.nextInt(2 * R - 24) - R + 12;
            if ((x - s.x) * (x - s.x) + (z - s.z) * (z - s.z) > (R - 14) * (R - 14)) continue;
            if (Math.abs(x - s.x) < 7 || Math.abs(z - s.z) < 7) continue;
            boolean noble = x < s.x && z < s.z - 10;
            int ww = noble ? 13 : 7 + pl.r.nextInt(3), dd = noble ? 11 : 7 + pl.r.nextInt(2);
            if (giant) { ww += 3; dd += 3; }
            if (!pl.free(x, z, Math.max(ww, dd) / 2 + (noble ? 4 : 1))) continue;
            int rot = Math.abs(x - s.x) < Math.abs(z - s.z) ? (x < s.x ? 1 : 3) : (z < s.z ? 2 : 0);
            if (noble) {
                Buildings.mansion(pl.at(x, z, rot, ww, dd), pl.p, pl.r, ww, dd);
                pl.people(x, z, NpcRole.NOBLE, 1 + pl.r.nextInt(2));
                if (Kingdom.of(k).slavery) pl.people(x, z, NpcRole.SLAVE, 1);
            } else {
                Buildings.house(pl.at(x, z, rot, ww, dd), pl.p, pl.r, ww, dd, pl.r.nextInt(3) == 0);
                pl.people(x, z, pl.r.nextInt(4) == 0 ? NpcRole.WORKER : NpcRole.PEASANT, 1 + pl.r.nextInt(2));
                if (pl.r.nextInt(3) == 0) pl.people(x, z, NpcRole.CHILD, 1);
            }
        }
        // guards at the gates and on the plaza
        for (int[] g : new int[][]{{0, -R + 6}, {0, R - 6}, {-R + 6, 0}, {R - 6, 0}}) pl.people(s.x + g[0] + 3, s.z + g[1], NpcRole.GUARD, 2);
        pl.people(s.x + 8, s.z, NpcRole.GUARD, 2);
        pl.people(s.x, s.z + 8, NpcRole.PEASANT, 2);
    }

    private static void city(Plan pl) {
        Site s = pl.site;
        int R = s.type.radius;
        terrain(pl, R + 1, pl.p.path());
        ringWall(pl, R - 2, pl.p == Palette.GIANT ? 9 : 6, pl.p == Palette.ORC || pl.p == Palette.BEAST);
        road(pl, s.x - R + 2, s.z, s.x + R - 2, s.z, 3, pl.p.path());
        road(pl, s.x, s.z - R + 2, s.x, s.z + R - 2, 3, pl.p.path());
        plaza(pl, 9);
        pl.free(s.x, s.z, 11);
        int tx = s.x + 22, tz = s.z - 20;
        Buildings.temple(pl.at(tx, tz, faceToward(tx, tz, s.x, s.z), 11, 15), pl.p, pl.r, 11, 15);
        pl.free(tx, tz, 9);
        pl.people(tx, tz, NpcRole.PRIEST, 1);
        int gx = s.x - 20, gz = s.z - 20;
        if (pl.r.nextBoolean()) { Buildings.guildHall(pl.at(gx, gz, faceToward(gx, gz, s.x, s.z), 11, 9), pl.p, pl.r, 11, 9); pl.people(gx, gz, NpcRole.GUILD_MASTER, 1); }
        else { Buildings.mageTower(pl.at(gx, gz, faceToward(gx, gz, s.x, s.z), 9, 9), pl.p, pl.r); pl.people(gx, gz, NpcRole.MAGE, 1); }
        pl.free(gx, gz, 7);
        int bx = s.x + 20, bz = s.z + 20;
        Buildings.tavern(pl.at(bx, bz, faceToward(bx, bz, s.x, s.z), 11, 9), pl.p, pl.r, 11, 9);
        pl.free(bx, bz, 7);
        pl.people(bx, bz, NpcRole.INNKEEPER, 1);
        int sx = s.x - 20, sz = s.z + 20;
        Buildings.smithy(pl.at(sx, sz, faceToward(sx, sz, s.x, s.z), 9, 7), pl.p, pl.r, 9, 7);
        pl.free(sx, sz, 6);
        pl.people(sx, sz, NpcRole.BLACKSMITH, 1);
        for (int i = 0; i < 3; i++) {
            double a = i * 2.1 + 0.4;
            int x = s.x + (int) Math.round(Math.cos(a) * 13), z = s.z + (int) Math.round(Math.sin(a) * 13);
            Buildings.stall(pl.at(x, z, faceToward(x, z, s.x, s.z), 4, 4), pl.p, pl.r, i == 0 ? Blocks.RED_WOOL : i == 1 ? Blocks.CYAN_WOOL : Blocks.YELLOW_WOOL);
            pl.free(x, z, 3);
            pl.people(x, z, NpcRole.MERCHANT, 1);
        }
        if (Kingdom.of(Math.max(0, s.kingdom)).slavery && pl.r.nextBoolean()) {
            int vx = s.x + 32, vz = s.z + 2;
            if (pl.free(vx, vz, 7)) {
                Buildings.slaveMarket(pl.at(vx, vz, 3, 11, 9), pl.p, pl.r);
                pl.people(vx, vz, NpcRole.SLAVER, 1);
                pl.people(vx, vz + 2, NpcRole.SLAVE, 2);
            }
        }
        houses(pl, R - 10, 26, true);
        pl.people(s.x, s.z - R + 6, NpcRole.GUARD, 2);
        pl.people(s.x, s.z + R - 6, NpcRole.GUARD, 2);
        pl.people(s.x + 5, s.z, NpcRole.GUARD, 1);
    }

    private static void houses(Plan pl, int maxR, int tries, boolean twoFloorsAllowed) {
        Site s = pl.site;
        boolean giant = pl.p == Palette.GIANT;
        for (int i = 0; i < tries * 3; i++) {
            double a = pl.r.nextDouble() * Math.PI * 2, d = 12 + pl.r.nextDouble() * (maxR - 12);
            int x = s.x + (int) (Math.cos(a) * d), z = s.z + (int) (Math.sin(a) * d);
            if (Math.abs(x - s.x) < 5 || Math.abs(z - s.z) < 5) continue;
            int ww = 7 + pl.r.nextInt(3), dd = 7 + pl.r.nextInt(2);
            if (giant) { ww += 3; dd += 3; }
            if (!pl.free(x, z, Math.max(ww, dd) / 2 + 1)) continue;
            Buildings.house(pl.at(x, z, faceToward(x, z, s.x, s.z), ww, dd), pl.p, pl.r, ww, dd, twoFloorsAllowed && pl.r.nextInt(3) == 0);
            pl.people(x, z, pl.r.nextInt(5) == 0 ? NpcRole.WORKER : NpcRole.PEASANT, 1 + pl.r.nextInt(2));
            if (pl.r.nextInt(3) == 0) pl.people(x, z, NpcRole.CHILD, 1);
            if (--tries <= 0) break;
        }
    }

    private static void village(Plan pl) {
        Site s = pl.site;
        int R = s.type.radius;
        terrain(pl, R, Blocks.GRASS_BLOCK);
        plaza(pl, 5);
        pl.free(s.x, s.z, 7);
        road(pl, s.x - R + 4, s.z, s.x + R - 4, s.z, 3, Blocks.DIRT_PATH);
        road(pl, s.x, s.z - R + 4, s.x, s.z + R - 4, 3, Blocks.DIRT_PATH);
        houses(pl, R - 6, 8, false);
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + Math.PI / 4;
            int x = s.x + (int) (Math.cos(a) * (R - 8)), z = s.z + (int) (Math.sin(a) * (R - 8));
            if (!pl.free(x, z, 6)) continue;
            Buildings.farm(pl.at(x, z, 0, 9, 9), pl.r, 9, 9);
            pl.people(x, z, NpcRole.PEASANT, 1);
        }
        pl.people(s.x + 3, s.z + 3, NpcRole.GUARD, 1);
        if (pl.r.nextBoolean()) pl.people(s.x - 3, s.z, NpcRole.MERCHANT, 1);
        if (pl.r.nextInt(3) == 0) pl.people(s.x, s.z - 3, NpcRole.ADVENTURER, 1);
    }

    private static void camp(Plan pl) {
        Site s = pl.site;
        terrain(pl, 16, Blocks.COARSE_DIRT);
        ringWall(pl, 15, 3, true);
        Canvas c = pl.flat();
        c.set(0, 1, 0, Blocks.CAMPFIRE);
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            int x = s.x + (int) (Math.cos(a) * 9), z = s.z + (int) (Math.sin(a) * 9);
            Buildings.tent(pl.at(x, z, faceToward(x, z, s.x, s.z), 5, 5), pl.r);
        }
        c.facing(2, 1, 2, Blocks.CHEST, net.minecraft.core.Direction.NORTH);
        BlockPos chest = new BlockPos(s.x + 2, pl.y + 1, s.z + 2);
        pl.ops.add(new Canvas.ActOp(l -> loot(l, chest, 2)));
        if (!"cult_hideout".equals(s.id)) pl.people(s.x, s.z, NpcRole.BANDIT, 4 + pl.r.nextInt(3));
    }

    private static void lair(Plan pl) {
        Site s = pl.site;
        MonsterDef boss = RpgDefs.BY_ID.get(s.boss);
        RegionId reg = boss == null ? RegionId.CURSED_RUINS : boss.regions()[0];
        Block floor, pillar, accent, glow;
        switch (reg) {
            case DRAGON_TEETH, INFERNAX -> { floor = Blocks.BLACKSTONE; pillar = Blocks.BASALT; accent = Blocks.MAGMA_BLOCK; glow = Blocks.SHROOMLIGHT; }
            case CRYSTAL_HOLLOW -> { floor = Blocks.CALCITE; pillar = Blocks.AMETHYST_BLOCK; accent = Blocks.BUDDING_AMETHYST; glow = Blocks.SEA_LANTERN; }
            case YMIRHEIM -> { floor = Blocks.PACKED_ICE; pillar = Blocks.BLUE_ICE; accent = Blocks.SNOW_BLOCK; glow = Blocks.SEA_LANTERN; }
            case GLASS_DESERT, MERIDIA -> { floor = Blocks.SMOOTH_SANDSTONE; pillar = Blocks.CHISELED_SANDSTONE; accent = Blocks.GOLD_BLOCK; glow = Blocks.GLOWSTONE; }
            case DEAD_MARSH -> { floor = Blocks.MUD; pillar = Blocks.MUDDY_MANGROVE_ROOTS; accent = Blocks.MOSS_BLOCK; glow = Blocks.OCHRE_FROGLIGHT; }
            case STORM_COAST -> { floor = Blocks.PRISMARINE_BRICKS; pillar = Blocks.DARK_PRISMARINE; accent = Blocks.PRISMARINE; glow = Blocks.SEA_LANTERN; }
            case SYLVARIEN, WOLF_FOREST, FELARIS -> { floor = Blocks.MOSSY_STONE_BRICKS; pillar = Blocks.OAK_LOG; accent = Blocks.MOSS_BLOCK; glow = Blocks.SHROOMLIGHT; }
            case NOCTHERA -> { floor = Blocks.DEEPSLATE_TILES; pillar = Blocks.CRYING_OBSIDIAN; accent = Blocks.OBSIDIAN; glow = Blocks.SOUL_LANTERN; }
            case KHAZDUR -> { floor = Blocks.DEEPSLATE_BRICKS; pillar = Blocks.POLISHED_BASALT; accent = Blocks.RAW_IRON_BLOCK; glow = Blocks.LANTERN; }
            default -> { floor = Blocks.CRACKED_STONE_BRICKS; pillar = Blocks.MOSSY_STONE_BRICKS; accent = Blocks.SOUL_SOIL; glow = Blocks.SOUL_LANTERN; }
        }
        terrain(pl, 20, floor);
        Buildings.lair(pl.flat(), pl.r, floor, pillar, accent, glow, 18);
    }

    private static void ruinSite(Plan pl) {
        Site s = pl.site;
        terrain(pl, 14, Blocks.GRASS_BLOCK);
        for (int i = 0; i < 3; i++) {
            int x = s.x + pl.r.nextInt(16) - 8, z = s.z + pl.r.nextInt(16) - 8;
            Buildings.ruin(pl.at(x, z, pl.r.nextInt(4), 7, 7), pl.p, pl.r, 7, 7);
            BlockPos chest = pl.at(x, z, 0, 7, 7).world(3, 1, 3);
        }
        BlockPos center = new BlockPos(s.x, pl.y + 1, s.z);
        pl.ops.add(new Canvas.ActOp(l -> {
            for (BlockPos bp : BlockPos.betweenClosed(center.offset(-12, -1, -12), center.offset(12, 3, 12))) {
                if (l.getBlockState(bp).is(Blocks.CHEST)) loot(l, bp.immutable(), 3);
            }
        }));
    }

    private static void landmark(Plan pl) {
        Site s = pl.site;
        int variant = 0;
        try { variant = Integer.parseInt(s.boss); } catch (NumberFormatException ignored) {}
        Canvas c = pl.flat();
        RandomSource r = pl.r;
        switch (variant) {
            case 0 -> { // colossal statue of an ancient king holding a sword
                terrain(pl, 9, Blocks.STONE_BRICKS);
                c.fill(-4, 0, -4, 4, 2, 4, Blocks.CHISELED_STONE_BRICKS);
                c.fill(-3, 3, -2, 3, 3, 2, Blocks.STONE_BRICKS);
                c.fill(-2, 4, -1, -1, 11, 1, Blocks.POLISHED_ANDESITE);
                c.fill(1, 4, -1, 2, 11, 1, Blocks.POLISHED_ANDESITE);
                c.fill(-3, 12, -2, 3, 19, 2, Blocks.POLISHED_ANDESITE);
                c.fill(-6, 18, -1, -4, 19, 1, Blocks.POLISHED_ANDESITE);
                c.fill(4, 18, -1, 6, 19, 1, Blocks.POLISHED_ANDESITE);
                c.fill(-6, 11, -1, -5, 17, 1, Blocks.POLISHED_ANDESITE);
                c.fill(5, 12, -2, 6, 17, 0, Blocks.POLISHED_ANDESITE);
                c.fill(-2, 20, -2, 2, 24, 2, Blocks.POLISHED_DIORITE);
                c.fill(-2, 25, -2, 2, 25, 2, Blocks.GOLD_BLOCK);
                for (int i = -2; i <= 2; i += 2) c.set(i, 26, 0, Blocks.GOLD_BLOCK);
                c.set(-1, 22, -3, Blocks.BLACK_CONCRETE);
                c.set(1, 22, -3, Blocks.BLACK_CONCRETE);
                c.fill(6, 4, -3, 6, 21, -3, Blocks.IRON_BLOCK);
                c.fill(5, 11, -3, 7, 11, -3, Blocks.GOLD_BLOCK);
                for (int i = 0; i < 25; i++) c.set(-4 + r.nextInt(9), r.nextInt(20), -3 + r.nextInt(6), Blocks.MOSSY_STONE_BRICKS);
                c.set(0, 3, -3, Blocks.LECTERN);
            }
            case 1 -> { // watchtower
                terrain(pl, 7, Blocks.COBBLESTONE);
                Palette p = pl.p;
                c.walls(-3, 0, -3, 3, 20, 3, p.cityWall());
                c.air(-2, 1, -2, 2, 20, 2);
                for (int y = 1; y <= 20; y++) c.set(0, y, 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.NORTH));
                c.fill(-4, 21, -4, 4, 21, 4, p.floor());
                c.set(0, 21, 2, Blocks.AIR);
                for (int i = -4; i <= 4; i += 2) { c.set(i, 22, -4, p.wallTop()); c.set(i, 22, 4, p.wallTop()); c.set(-4, 22, i, p.wallTop()); c.set(4, 22, i, p.wallTop()); }
                c.set(0, 22, 0, Blocks.CAMPFIRE);
                c.air(0, 1, -3, 0, 2, -3);
                for (int y = 4; y < 20; y += 5) { c.set(-3, y, 0, p.window() == Blocks.AIR ? Blocks.AIR : p.window()); c.set(3, y, 0, p.window() == Blocks.AIR ? Blocks.AIR : p.window()); }
                pl.people.add(new Resident(s.x, pl.y + 22, s.z + 1, s.kingdom >= 0 ? NpcRole.GUARD : NpcRole.BANDIT, 1));
                pl.people(s.x + 2, s.z - 5, s.kingdom >= 0 ? NpcRole.SOLDIER : NpcRole.BANDIT, 1);
            }
            case 2 -> { // forgotten temple
                terrain(pl, 12, Blocks.MOSSY_STONE_BRICKS);
                for (int x = -8; x <= 8; x += 4) for (int z = -10; z <= 10; z += 5) {
                    if (Math.abs(x) < 8 && Math.abs(z) < 10) continue;
                    int h = 3 + r.nextInt(6);
                    c.fill(x, 1, z, x, h, z, r.nextBoolean() ? Blocks.QUARTZ_PILLAR : Blocks.CHISELED_STONE_BRICKS);
                }
                c.fill(-3, 1, -3, 3, 1, 3, Blocks.SMOOTH_QUARTZ);
                c.fill(-1, 2, -1, 1, 2, 1, Blocks.GOLD_BLOCK);
                c.set(0, 3, 0, Blocks.ENCHANTING_TABLE);
                c.facing(2, 2, 0, Blocks.CHEST, net.minecraft.core.Direction.WEST);
                for (int i = 0; i < 30; i++) c.set(-9 + r.nextInt(19), 1, -11 + r.nextInt(23), r.nextBoolean() ? Blocks.MOSSY_COBBLESTONE : Blocks.CRACKED_STONE_BRICKS);
                BlockPos chest = c.world(2, 2, 0);
                pl.ops.add(new Canvas.ActOp(l -> loot(l, chest, 4)));
            }
            case 3 -> { // dragon bones
                terrain(pl, 14, Blocks.COARSE_DIRT);
                for (int z = -10; z <= 10; z += 3) {
                    int rib = 6 - Math.abs(z) / 3;
                    for (int a = 0; a <= 12; a++) {
                        double t = a / 12.0 * Math.PI;
                        int x = (int) Math.round(Math.cos(t) * rib), y = (int) Math.round(Math.sin(t) * (rib + 2));
                        c.set(x, y + 1, z, Blocks.BONE_BLOCK);
                    }
                }
                for (int z = -13; z <= 13; z++) c.set(0, 1 + (int) (Math.sin(z * 0.3) * 1.5 + 1.5), z, Blocks.BONE_BLOCK);
                c.fill(-2, 1, 13, 2, 4, 17, Blocks.BONE_BLOCK);
                c.air(-1, 2, 14, 1, 3, 17);
                c.set(-1, 3, 17, Blocks.MAGMA_BLOCK);
                c.set(1, 3, 17, Blocks.MAGMA_BLOCK);
                c.fill(-3, 4, 14, -3, 7, 13, Blocks.BONE_BLOCK);
                c.fill(3, 4, 14, 3, 7, 13, Blocks.BONE_BLOCK);
            }
            case 4 -> { // crystal spires
                terrain(pl, 10, Blocks.CALCITE);
                for (int i = 0; i < 9; i++) {
                    double a = i * Math.PI * 2 / 9;
                    int x = (int) Math.round(Math.cos(a) * 6), z = (int) Math.round(Math.sin(a) * 6);
                    int h = 5 + r.nextInt(10);
                    for (int y = 1; y <= h; y++) {
                        c.set(x, y, z, y % 4 == 0 ? Blocks.SEA_LANTERN : Blocks.AMETHYST_BLOCK);
                        if (y < h / 2) { c.set(x + 1, y, z, Blocks.AMETHYST_BLOCK); c.set(x, y, z + 1, Blocks.AMETHYST_BLOCK); }
                    }
                    c.set(x, h + 1, z, Blocks.AMETHYST_CLUSTER);
                }
                c.fill(-1, 1, -1, 1, 1, 1, Blocks.BUDDING_AMETHYST);
                c.set(0, 2, 0, Blocks.BEACON);
            }
            default -> { // standing stones
                terrain(pl, 11, Blocks.GRASS_BLOCK);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2 / 12;
                    int x = (int) Math.round(Math.cos(a) * 8), z = (int) Math.round(Math.sin(a) * 8);
                    int h = 3 + r.nextInt(3);
                    c.fill(x, 1, z, x, h, z, i % 3 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE);
                    if (i % 2 == 0 && i < 11) c.set(x, h + 1, z, Blocks.STONE_SLAB);
                }
                c.fill(-1, 1, -1, 1, 1, 1, Blocks.MOSSY_COBBLESTONE);
                c.set(0, 2, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
            }
        }
    }

    /** a road from one settlement to another, following the ground, with bridges over water and lamp posts */
    public static List<Canvas.Op> road(Site a, Site b) {
        List<Canvas.Op> ops = new ArrayList<>();
        Palette p = Palette.of(a.kingdom);
        net.minecraft.world.level.block.state.BlockState path = (p.path() == Blocks.SNOW_BLOCK ? Blocks.GRAVEL : p.path() == Blocks.MOSS_BLOCK ? Blocks.DIRT_PATH : p.path()).defaultBlockState();
        net.minecraft.world.level.block.state.BlockState bridge = Blocks.SPRUCE_PLANKS.defaultBlockState();
        int steps = (int) Math.sqrt(a.distSq(b.x, b.z));
        double dx = (b.x - a.x) / (double) Math.max(1, steps), dz = (b.z - a.z) / (double) Math.max(1, steps);
        double nx = -dz, nz = dx;
        for (int i = 0; i <= steps; i++) {
            double x = a.x + dx * i, z = a.z + dz * i;
            if (a.distSq(x, z) < (a.type.radius + 2) * (a.type.radius + 2) || b.distSq(x, z) < (b.type.radius + 2) * (b.type.radius + 2)) continue;
            final double fx = x + Math.sin(i * 0.02) * 6 * nx, fz = z + Math.sin(i * 0.02) * 6 * nz;
            final boolean lamp = i % 28 == 0;
            ops.add(new Canvas.ActOp(level -> {
                for (int w = -1; w <= 1; w++) {
                    int px = (int) Math.floor(fx + nx * w), pz = (int) Math.floor(fz + nz * w);
                    int floor = com.krolasyon.bosses.rpg.util.Heights.get(level, Heightmap.Types.OCEAN_FLOOR, px, pz);
                    int top = com.krolasyon.bosses.rpg.util.Heights.get(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                    if (top > floor + 1) {
                        level.setBlock(new BlockPos(px, top, pz), bridge, 2);
                        if (lamp && w == 1) {
                            level.setBlock(new BlockPos(px, top + 1, pz), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                            level.setBlock(new BlockPos(px, top + 2, pz), Blocks.LANTERN.defaultBlockState(), 2);
                        }
                        continue;
                    }
                    BlockPos g = new BlockPos(px, floor - 1, pz);
                    net.minecraft.world.level.block.state.BlockState cur = level.getBlockState(g);
                    if (cur.isAir() || !cur.getFluidState().isEmpty()) continue;
                    level.setBlock(g, path, 2);
                    for (int up = 0; up < 3; up++) {
                        BlockPos q = g.above(1 + up);
                        net.minecraft.world.level.block.state.BlockState st = level.getBlockState(q);
                        if (!st.isAir() && (st.canBeReplaced() || st.is(net.minecraft.tags.BlockTags.LEAVES) || st.is(net.minecraft.tags.BlockTags.FLOWERS))) level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                    }
                    if (lamp && w == 1) {
                        BlockPos post = g.offset((int) Math.signum(nx), 1, (int) Math.signum(nz));
                        if (level.getBlockState(post).isAir()) {
                            level.setBlock(post, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                            level.setBlock(post.above(), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                            level.setBlock(post.above(2), Blocks.LANTERN.defaultBlockState(), 2);
                        }
                    }
                }
            }));
        }
        return ops;
    }

    // ------------------------------------------------------------------ contents
    static void loot(ServerLevel l, BlockPos pos, int quality) {
        BlockEntity be = l.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity chest)) return;
        RandomSource r = l.random;
        chest.setItem(0, new ItemStack(com.krolasyon.bosses.rpg.item.RpgItems.SILVER_COIN.get(), 3 + r.nextInt(8 * quality)));
        chest.setItem(4, com.krolasyon.bosses.rpg.item.RpgLoot.randomTome(r, 1, Math.min(5, quality + 1)));
        chest.setItem(8, new ItemStack(Items.BREAD, 4));
        if (r.nextInt(3) == 0) chest.setItem(13, new ItemStack(com.krolasyon.bosses.rpg.item.RpgItems.HEALING_POTION.get(), 2));
        if (r.nextInt(4) == 0) chest.setItem(22, new ItemStack(com.krolasyon.bosses.rpg.item.RpgItems.GOLD_COIN.get(), 1 + r.nextInt(quality)));
    }

    private static void populate(ServerLevel l, Site s, List<Resident> people) {
        RandomSource r = l.random;
        Kingdom k = s.kingdom >= 0 ? Kingdom.of(s.kingdom) : null;
        for (Resident res : people) {
            for (int i = 0; i < res.count; i++) {
                RpgNpc n = RpgEntities.NPC.get().create(l);
                if (n == null) continue;
                Race race;
                if (res.role == NpcRole.SLAVE) race = Race.of(r.nextInt(Race.values().length));
                else if (res.role == NpcRole.RULER) race = k == null ? Race.HUMAN : k.race;
                else if (k == null) race = Race.of(r.nextInt(Race.values().length));
                else race = k.citizens()[r.nextInt(k.citizens().length)];
                boolean female = r.nextBoolean();
                n.setup(race, female, res.role, s.kingdom >= 0 ? s.kingdom : r.nextInt(Kingdom.COUNT), r);
                if (res.role == NpcRole.RULER && k != null) {
                    n.setCustomName(Component.literal(k.ruler));
                    n.setLook(race.ordinal(), k == Kingdom.SYLVARIEN || k == Kingdom.FELARIS || k == Kingdom.NOCTHERA, 0);
                }
                if (res.role == NpcRole.CHILD) n.setAge(RpgNpc.CHILD_TICKS * 4);
                int x = res.x + r.nextInt(3) - 1, z = res.z + r.nextInt(3) - 1;
                int y = Math.max(res.y, com.krolasyon.bosses.rpg.util.Heights.ground(l, x, z));
                if (y > res.y + 6) y = res.y;
                n.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
                n.home = new BlockPos(res.x, res.y, res.z);
                n.siteId = s.id;
                if (res.role == NpcRole.RULER || res.role == NpcRole.MERCHANT || res.role == NpcRole.SLAVER || res.role == NpcRole.SLAVE) n.setFlag(RpgNpc.F_STAY, true);
                l.addFreshEntity(n);
            }
        }
        if (s.type == Site.Type.RUIN) {
            List<MonsterDef> pool = new ArrayList<>();
            for (MonsterDef d : RpgDefs.MONSTERS) if (d.has(RpgDefs.T_UNDEAD)) pool.add(d);
            for (int i = 0; i < 4 && !pool.isEmpty(); i++) {
                EntityType<?> t = RpgEntities.typeOf(pool.get(r.nextInt(pool.size())).id());
                if (t != null) t.spawn(l, new BlockPos(s.x + r.nextInt(9) - 4, s.y + 1, s.z + r.nextInt(9) - 4), MobSpawnType.STRUCTURE);
            }
        }
    }
}
