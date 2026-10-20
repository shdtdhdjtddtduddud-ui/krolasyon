package com.krolasyon.elvenrealm.world;

import com.krolasyon.elvenrealm.entity.ElfKind;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sylvaren'in yapı planı: üç teras (aşağı avlu, orta teras, kale terası), surlar, kapı kuleleri, hendek ve köprü,
 * çeşme, evler, kütüphane, okçuluk alanı, ay tapınağı, kale ve kuleler, şelaleler. Bloklar chunk kovalarına
 * yazılır; dev ağaçlar ayrıca {@link GiantTree} ilkel şekilleriyle tanımlanır. Koordinatlar mutlak.
 */
public final class CityPlan {
    public record Npc(ElfKind kind, int x, int y, int z) {}

    public final CityLayout.City city;
    public final int y0, cx, cz;
    public final int yA, yB, yC;
    private final Map<Long, Map<Long, BlockState>> chunks = new HashMap<>();
    public final List<GiantTree> trees = new ArrayList<>();
    public final List<Npc> npcs = new ArrayList<>();
    private final long seed;

    // palet
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    private static final BlockState SAND = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState CUT = Blocks.CUT_SANDSTONE.defaultBlockState();
    private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState ROOF = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
    private static final BlockState ROOF2 = Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState();
    private static final BlockState PLANK = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState LOG = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState FALL = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState FENCE = Blocks.SPRUCE_FENCE.defaultBlockState();

    public CityPlan(CityLayout.City city, int y0) {
        this.city = city;
        this.y0 = y0;
        this.cx = city.cx();
        this.cz = city.cz();
        this.seed = city.seed();
        this.yA = y0;
        this.yB = y0 + 8;
        this.yC = y0 + 16;
        build();
    }

    /** Merkeze göreli z için hedef zemin yüksekliği (en üst katı blok). */
    public int level(int rz) {
        if (rz >= -24) return yA;
        if (rz >= -56) return yB;
        return yC;
    }

    public Map<Long, BlockState> chunk(int chunkX, int chunkZ) {
        return chunks.get(((long) chunkX << 32) ^ (chunkZ & 0xffffffffL));
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static int unpackX(long p) {
        return (int) (p >> 38);
    }

    public static int unpackZ(long p) {
        return (int) (p << 26 >> 38);
    }

    public static int unpackY(long p) {
        return (int) (p << 52 >> 52);
    }

    // ------------------------------------------------------------------ yazma yardımcıları (göreli x/z, mutlak y)

    private void set(int rx, int y, int rz, BlockState s) {
        int x = cx + rx, z = cz + rz;
        long ck = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
        chunks.computeIfAbsent(ck, k -> new HashMap<>()).put(pack(x, y, z), s);
    }

    private BlockState get(int rx, int y, int rz) {
        int x = cx + rx, z = cz + rz;
        Map<Long, BlockState> m = chunks.get(((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL));
        return m == null ? null : m.get(pack(x, y, z));
    }

    private double h(int salt, int x, int y, int z) {
        return Noise.hash(seed + salt, x, y, z);
    }

    private BlockState aged(int x, int y, int z) {
        double r = h(1, x, y, z);
        return r < 0.22 ? MOSSY : r < 0.3 ? CRACKED : BRICK;
    }

    private void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    private void agedBox(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, aged(x, y, z));
    }

    private static BlockState stair(BlockState base, Direction facing, boolean top) {
        return base.setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState wallX(BlockState w) {
        return w.setValue(WallBlock.EAST_WALL, WallSide.LOW).setValue(WallBlock.WEST_WALL, WallSide.LOW).setValue(WallBlock.UP, false);
    }

    private static BlockState wallZ(BlockState w) {
        return w.setValue(WallBlock.NORTH_WALL, WallSide.LOW).setValue(WallBlock.SOUTH_WALL, WallSide.LOW).setValue(WallBlock.UP, false);
    }

    private void lanternPost(int x, int g, int z) {
        set(x, g + 1, z, FENCE);
        set(x, g + 2, z, FENCE);
        set(x, g + 3, z, LANTERN);
    }

    // ------------------------------------------------------------------ ana plan

    private void build() {
        ground();
        moatAndBridge();
        outerWalls();
        retainingWalls();
        fountain(0, 8);
        avenue();
        // aşağı avlu evleri
        house(-40, 18, 11, 9, yA, 1, Direction.EAST);
        house(-41, 2, 9, 11, yA, 2, Direction.EAST);
        house(-38, -14, 13, 9, yA, 1, Direction.EAST);
        house(40, 18, 11, 9, yA, 2, Direction.WEST);
        house(41, 2, 9, 11, yA, 1, Direction.WEST);
        house(38, -14, 13, 9, yA, 2, Direction.WEST);
        house(-22, 21, 9, 7, yA, 1, Direction.SOUTH);
        trainingYard(22, 8);
        // orta teras
        library(-28, -42);
        archeryRange(24, -40);
        house(-48, -32, 9, 9, yB, 1, Direction.EAST);
        house(48, -32, 9, 9, yB, 1, Direction.WEST);
        // şelaleler (orta → aşağı, kale → orta)
        waterfall(-20, -25, yB, yA, 3);
        waterfall(20, -25, yB, yA, 3);
        waterfall(-34, -57, yC, yB, 4);
        waterfall(34, -57, yC, yB, 4);
        // kale terası
        castle();
        moonShrine(34, -76);
        gardenPond(-32, -76);
        // kuleler (surlarda)
        tower(-56, 31, 5, yA - 2, yA + 16, true);
        tower(56, 31, 5, yA - 2, yA + 16, true);
        tower(-8, 31, 4, yA - 1, yA + 14, true);
        tower(8, 31, 4, yA - 1, yA + 14, true);
        tower(-56, -24, 5, yA - 1, yA + 22, true);
        tower(56, -24, 5, yA - 1, yA + 22, true);
        tower(-56, -57, 5, yB - 1, yB + 22, true);
        tower(56, -57, 5, yB - 1, yB + 22, true);
        tower(-56, -114, 5, yC - 1, yC + 16, true);
        tower(56, -114, 5, yC - 1, yC + 16, true);
        gate();
        // dev ağaçlar
        trees.add(new GiantTree(seed + 101, cx - 38, yC, cz - 100));
        trees.add(new GiantTree(seed + 202, cx + 38, yC, cz - 100));

        // sakinler
        npcs.add(new Npc(ElfKind.LIRAEL, cx + 20, yA + 1, cz + 8));
        npcs.add(new Npc(ElfKind.VAELOR, cx - 14, yA + 1, cz + 24));
        npcs.add(new Npc(ElfKind.THALION, cx + 13, yA + 1, cz + 24));
        npcs.add(new Npc(ElfKind.CAELAN, cx - 17, yB + 1, cz - 44));
        npcs.add(new Npc(ElfKind.TAVI, cx + 22, yB + 1, cz - 34));
        npcs.add(new Npc(ElfKind.NIMRAE, cx + 34, yC + 1, cz - 76));
        npcs.add(new Npc(ElfKind.GUARD, cx - 5, yA + 1, cz + 27));
        npcs.add(new Npc(ElfKind.GUARD, cx + 5, yA + 1, cz + 27));
        npcs.add(new Npc(ElfKind.GUARD, cx + 7, yB + 1, cz - 27));
        npcs.add(new Npc(ElfKind.GUARD, cx - 4, yC + 1, cz - 60));
    }

    /** Zemin döşemeleri (teras yüzeylerinin üstü feature tarafından çimle kaplanır; burada sadece taş zeminler). */
    private void ground() {
        // meydan
        for (int x = -11; x <= 11; x++) {
            for (int z = -3; z <= 19; z++) {
                double d = Math.hypot(x, z - 8);
                if (d > 11.2) continue;
                double r = h(2, x, 0, z);
                BlockState s;
                if (d > 10.2) s = Math.floorMod((int) Math.floor(Math.atan2(z - 8, x) * 5), 2) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : CHISELED;
                else s = r < 0.2 ? MOSSY : r < 0.3 ? CRACKED : r < 0.55 ? BRICK : Blocks.POLISHED_ANDESITE.defaultBlockState();
                set(x, yA, z, s);
            }
        }
    }

    private void avenue() {
        BlockState[] mix = {BRICK, BRICK, MOSSY, Blocks.POLISHED_ANDESITE.defaultBlockState(), CRACKED};
        for (int z = -23; z <= 33; z++) {
            for (int x = -3; x <= 3; x++) {
                if (get(x, yA, z) != null) continue;
                set(x, yA, z, mix[(int) (h(3, x, 0, z) * mix.length)]);
            }
            if (z % 8 == 0 && z < 28 && z > -12 && (z < -4 || z > 20)) {
                lanternPost(-5, yA, z);
                lanternPost(5, yA, z);
            }
        }
        for (int z = -55; z <= -25; z++) {
            for (int x = -3; x <= 3; x++) set(x, yB, z, mix[(int) (h(3, x, 1, z) * mix.length)]);
            if (z % 8 == 0 && z > -47) {
                lanternPost(-5, yB, z);
                lanternPost(5, yB, z);
            }
        }
        for (int z = -64; z <= -57; z++) for (int x = -3; x <= 3; x++) set(x, yC, z, mix[(int) (h(3, x, 2, z) * mix.length)]);
        // yan yollar (evlere)
        for (int x = -36; x <= 36; x++) {
            if (Math.abs(x) < 4) continue;
            for (int z = 0; z <= 1; z++) if (get(x, yA, z + 1) == null) set(x, yA, z + 1, Blocks.GRAVEL.defaultBlockState());
            for (int z = -1; z <= 0; z++) if (get(x, yB, z - 40) == null) set(x, yB, z - 40, Blocks.GRAVEL.defaultBlockState());
        }
    }

    private void moatAndBridge() {
        for (int x = -58; x <= 58; x++) {
            for (int z = 35; z <= 44; z++) {
                boolean edge = z == 35 || z == 44 || Math.abs(x) == 58;
                for (int y = yA - 3; y <= yA; y++) {
                    if (edge) set(x, y, z, aged(x, y, z));
                    else if (y == yA - 3) set(x, y, z, Blocks.GRAVEL.defaultBlockState());
                    else if (y == yA) set(x, y, z, AIR);
                    else set(x, y, z, WATER);
                }
                if (!edge && h(4, x, 0, z) < 0.04) set(x, yA, z, Blocks.LILY_PAD.defaultBlockState());
            }
        }
        // köprü: taş tabliye, altında kemer, korkuluklar
        for (int z = 34; z <= 45; z++) {
            for (int x = -3; x <= 3; x++) {
                set(x, yA, z, aged(x, yA, z));
                if (z >= 36 && z <= 43 && Math.abs(x) <= 3) {
                    boolean arch = Math.abs(z - 39.5) < 2.6 && Math.abs(x) <= 3;
                    set(x, yA - 1, z, arch ? AIR : aged(x, yA - 1, z));
                    if (arch) set(x, yA - 2, z, WATER);
                }
            }
            set(-3, yA + 1, z, wallZ(Blocks.STONE_BRICK_WALL.defaultBlockState()));
            set(3, yA + 1, z, wallZ(Blocks.STONE_BRICK_WALL.defaultBlockState()));
        }
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{34, 45}) {
                set(x, yA + 1, z, BRICK);
                set(x, yA + 2, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                set(x, yA + 3, z, LANTERN);
            }
        }
    }

    private void outerWalls() {
        // ön sur
        for (int x = -56; x <= 56; x++) {
            for (int z = 30; z <= 32; z++) {
                for (int y = yA - 2; y <= yA + 9; y++) set(x, y, z, aged(x, y, z));
                if ((z == 30 || z == 32) && Math.floorMod(x, 2) == 0) set(x, yA + 10, z, aged(x, yA + 10, z));
            }
            if (Math.floorMod(x, 12) == 6) set(x, yA + 5, 32, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
        }
        // yan surlar: aşağı avlu, orta teras, kale terası
        for (int side : new int[]{-1, 1}) {
            for (int t = 54; t <= 56; t++) {
                int x = side * t;
                for (int z = -24; z <= 32; z++) {
                    for (int y = yA - 2; y <= yA + 9; y++) set(x, y, z, aged(x, y, z));
                    if ((t == 54 || t == 56) && Math.floorMod(z, 2) == 0) set(x, yA + 10, z, aged(x, yA + 10, z));
                }
                for (int z = -56; z <= -25; z++) {
                    for (int y = yA; y <= yB + 7; y++) set(x, y, z, aged(x, y, z));
                    if ((t == 54 || t == 56) && Math.floorMod(z, 2) == 0) set(x, yB + 8, z, aged(x, yB + 8, z));
                }
                for (int z = -116; z <= -57; z++) {
                    for (int y = yB; y <= yC + 6; y++) set(x, y, z, aged(x, y, z));
                    if ((t == 54 || t == 56) && Math.floorMod(z, 2) == 0) set(x, yC + 7, z, aged(x, yC + 7, z));
                }
            }
        }
        // arka sur
        for (int x = -56; x <= 56; x++) {
            for (int z = -116; z <= -114; z++) {
                for (int y = yC - 3; y <= yC + 6; y++) set(x, y, z, aged(x, y, z));
                if ((z == -116 || z == -114) && Math.floorMod(x, 2) == 0) set(x, yC + 7, z, aged(x, yC + 7, z));
            }
        }
    }

    private void gate() {
        // kemerli geçit
        for (int z = 29; z <= 33; z++) {
            for (int x = -3; x <= 3; x++) {
                int top = Math.abs(x) <= 1 ? yA + 6 : Math.abs(x) == 2 ? yA + 5 : yA + 4;
                for (int y = yA + 1; y <= top; y++) set(x, y, z, AIR);
                set(x, yA, z, aged(x, yA, z));
            }
        }
        for (int x = -3; x <= 3; x++) set(x, yA + 6, 33, Blocks.IRON_BARS.defaultBlockState());
        // kapı üstü kemer taşları
        for (int x = -4; x <= 4; x++) set(x, yA + 7, 33, CHISELED);
        set(0, yA + 8, 33, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        // flamalar yerine ışık
        set(-5, yA + 4, 34, Blocks.WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.SOUTH));
        set(5, yA + 4, 34, Blocks.WALL_TORCH.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, Direction.SOUTH));
    }

    /** Teras istinat duvarları, korkuluklar ve büyük merdivenler. */
    private void retainingWalls() {
        for (int x = -53; x <= 53; x++) {
            // A → B (yüz z=-25..-26)
            for (int z = -26; z <= -25; z++) for (int y = yA - 1; y <= yB; y++) set(x, y, z, aged(x, y, z));
            if (Math.abs(x) > 5) set(x, yB + 1, -25, wallX(Blocks.STONE_BRICK_WALL.defaultBlockState()));
            // B → C (yüz z=-57..-58)
            for (int z = -58; z <= -57; z++) for (int y = yB - 1; y <= yC; y++) set(x, y, z, aged(x, y, z));
            if (Math.abs(x) > 5) set(x, yC + 1, -57, wallX(Blocks.STONE_BRICK_WALL.defaultBlockState()));
            // sarmaşıklar
            for (int y = yA + 2; y <= yB - 1; y++) if (h(5, x, y, -24) < 0.12) set(x, y, -24, Blocks.VINE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.VineBlock.NORTH, true));
            for (int y = yB + 2; y <= yC - 1; y++) if (h(5, x, y, -56) < 0.12) set(x, y, -56, Blocks.VINE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.VineBlock.NORTH, true));
        }
        stairs(-16, yA, Direction.NORTH);
        stairs(-48, yB, Direction.NORTH);
        // korkuluk sonlarına fener
        for (int x : new int[]{-6, 6}) {
            set(x, yB + 1, -25, BRICK);
            set(x, yB + 2, -25, LANTERN);
            set(x, yC + 1, -57, BRICK);
            set(x, yC + 2, -57, LANTERN);
        }
    }

    /** Bir teras basamağı: z0'dan kuzeye 8 basamak, x ∈ [-4,4], yanlarda taş korkuluk. */
    private void stairs(int z0, int g, Direction up) {
        BlockState st = stair(Blocks.STONE_BRICK_STAIRS.defaultBlockState(), up, false);
        for (int k = 1; k <= 8; k++) {
            int z = z0 - (k - 1);
            for (int x = -4; x <= 4; x++) {
                for (int y = g; y < g + k; y++) set(x, y, z, BRICK);
                set(x, g + k, z, st);
                for (int y = g + k + 1; y <= g + 10; y++) set(x, y, z, AIR);
            }
            for (int x : new int[]{-5, 5}) {
                for (int y = g; y <= g + k; y++) set(x, y, z, aged(x, y, z));
                set(x, g + k + 1, z, wallZ(Blocks.STONE_BRICK_WALL.defaultBlockState()));
            }
        }
        // sahanlık ve duvardaki geçit
        for (int z = z0 - 8; z >= z0 - 10; z--)
            for (int x = -4; x <= 4; x++) {
                set(x, g + 8, z, BRICK);
                for (int y = g + 9; y <= g + 11; y++) set(x, y, z, AIR);
            }
    }

    private void fountain(int fx, int fz) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = Math.hypot(x, z);
                if (d > 6.2) continue;
                if (d > 5.0) {
                    set(fx + x, yA, fz + z, CHISELED);
                    set(fx + x, yA + 1, fz + z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
                } else {
                    set(fx + x, yA - 2, fz + z, BRICK);
                    set(fx + x, yA - 1, fz + z, WATER);
                    set(fx + x, yA, fz + z, WATER);
                }
            }
        }
        for (int y = yA - 1; y <= yA + 3; y++) set(fx, y, fz, y == yA + 3 ? CHISELED : Blocks.STONE_BRICK_WALL.defaultBlockState());
        set(fx, yA + 4, fz, WATER);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int y = yA + 1; y <= yA + 3; y++) set(fx + d.getStepX(), y, fz + d.getStepZ(), FALL);
        }
        // çeşme çevresinde çiçek tarhları
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4 + Math.PI / 8;
            int x = (int) Math.round(Math.cos(ang) * 8.5), z = (int) Math.round(Math.sin(ang) * 8.5);
            set(fx + x, yA, fz + z, Blocks.GRASS_BLOCK.defaultBlockState());
            set(fx + x, yA + 1, fz + z, Blocks.FLOWERING_AZALEA.defaultBlockState());
        }
    }

    /** Ev: taş temel, kalsit sıva, ladin dikmeler, mavi camlı pencereler, turuncu beşik çatı, içeride fener. */
    private void house(int hx, int hz, int w, int d, int g, int floors, Direction door) {
        int x0 = hx - w / 2, x1 = x0 + w - 1, z0 = hz - d / 2, z1 = z0 + d - 1;
        int top = g + floors * 5;
        long hs = seed + hx * 31L + hz * 17L;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                set(x, g, z, wall ? aged(x, g, z) : PLANK);
                for (int y = g + 1; y <= top; y++) {
                    if (!wall) {
                        set(x, y, z, (y - g) % 5 == 0 ? PLANK : AIR);
                        continue;
                    }
                    int ly = (y - g) % 5;
                    BlockState s;
                    if (corner) s = LOG;
                    else if (ly == 0) s = Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState();
                    else if (y == g + 1) s = aged(x, y, z);
                    else {
                        boolean along = z == z0 || z == z1;
                        int k = along ? x - x0 : z - z0;
                        int len = along ? w : d;
                        boolean win = (ly == 2 || ly == 3) && k > 1 && k < len - 2 && k % 3 == 1;
                        s = win ? GLASS : CALCITE;
                    }
                    set(x, y, z, s);
                }
            }
        }
        // kapı
        int dx = door.getStepX(), dz = door.getStepZ();
        int doorX = dx > 0 ? x1 : dx < 0 ? x0 : hx, doorZ = dz > 0 ? z1 : dz < 0 ? z0 : hz;
        set(doorX, g + 1, doorZ, AIR);
        set(doorX, g + 2, doorZ, AIR);
        set(doorX, g + 3, doorZ, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
        set(doorX + dx, g, doorZ + dz, BRICK);
        int sx = dz != 0 ? 1 : 0, sz = dx != 0 ? 1 : 0;
        set(doorX + dx + sx, g + 3, doorZ + dz + sz, Blocks.WALL_TORCH.defaultBlockState()
                .setValue(net.minecraft.world.level.block.WallTorchBlock.FACING, door));
        // iç süs
        set(x0 + 1, g + 1, z0 + 1, LANTERN);
        set(x1 - 1, g + 1, z1 - 1, Noise.hash(hs, 1, 2, 3) < 0.5 ? Blocks.BARREL.defaultBlockState() : Blocks.CRAFTING_TABLE.defaultBlockState());
        set(x1 - 1, g + 1, z0 + 1, Blocks.BOOKSHELF.defaultBlockState());
        if (floors > 1) set(x0 + 1, g + 6, z1 - 1, LANTERN);
        // merdiven yerine üst kata ladder
        if (floors > 1) for (int y = g + 1; y <= g + 5; y++) {
            set(x0 + 1, y, hz, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.EAST));
        }
        gableRoof(x0, z0, x1, z1, top);
        // pencere altı çiçeklikleri
        if (h(6, hx, g, hz) < 0.7) {
            for (int x = x0 + 2; x <= x1 - 2; x += 3) {
                if (get(x, g + 2, z1) == GLASS) {
                    set(x, g + 1, z1 + 1, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, Half.TOP)
                            .setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.SOUTH)
                            .setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false));
                }
            }
        }
    }

    /** Beşik çatı: uzun eksen boyunca mahya, turuncu basamaklar, 1 blok saçak. */
    private void gableRoof(int x0, int z0, int x1, int z1, int top) {
        boolean alongX = (x1 - x0) >= (z1 - z0);
        BlockState st = Blocks.SMOOTH_RED_SANDSTONE_STAIRS.defaultBlockState();
        if (alongX) {
            int depth = z1 - z0 + 3;
            int half = depth / 2;
            for (int i = 0; i < half; i++) {
                int y = top + 1 + i;
                for (int x = x0 - 1; x <= x1 + 1; x++) {
                    set(x, y, z0 - 1 + i, stair(st, Direction.SOUTH, false));
                    set(x, y, z1 + 1 - i, stair(st, Direction.NORTH, false));
                    if (i > 0) {
                        // alttaki dolgu
                        set(x, y - 1, z0 - 1 + i, ROOF2);
                        set(x, y - 1, z1 + 1 - i, ROOF2);
                    }
                }
                // üçgen alınlıklar
                for (int z = z0 + i; z <= z1 - i; z++) {
                    set(x0, y, z, CALCITE);
                    set(x1, y, z, CALCITE);
                }
            }
            if (depth % 2 == 1) {
                int z = z0 - 1 + half;
                for (int x = x0 - 1; x <= x1 + 1; x++) set(x, top + 1 + half, z, Blocks.SMOOTH_RED_SANDSTONE_SLAB.defaultBlockState());
                set(x0, top + half, z, CALCITE);
                set(x1, top + half, z, CALCITE);
            }
            // tepe pencereleri
            set(x0, top + 2, (z0 + z1) / 2, GLASS);
            set(x1, top + 2, (z0 + z1) / 2, GLASS);
        } else {
            int depth = x1 - x0 + 3;
            int half = depth / 2;
            for (int i = 0; i < half; i++) {
                int y = top + 1 + i;
                for (int z = z0 - 1; z <= z1 + 1; z++) {
                    set(x0 - 1 + i, y, z, stair(st, Direction.EAST, false));
                    set(x1 + 1 - i, y, z, stair(st, Direction.WEST, false));
                    if (i > 0) {
                        set(x0 - 1 + i, y - 1, z, ROOF2);
                        set(x1 + 1 - i, y - 1, z, ROOF2);
                    }
                }
                for (int x = x0 + i; x <= x1 - i; x++) {
                    set(x, y, z0, CALCITE);
                    set(x, y, z1, CALCITE);
                }
            }
            if (depth % 2 == 1) {
                int x = x0 - 1 + half;
                for (int z = z0 - 1; z <= z1 + 1; z++) set(x, top + 1 + half, z, Blocks.SMOOTH_RED_SANDSTONE_SLAB.defaultBlockState());
                set(x, top + half, z0, CALCITE);
                set(x, top + half, z1, CALCITE);
            }
            set((x0 + x1) / 2, top + 2, z0, GLASS);
            set((x0 + x1) / 2, top + 2, z1, GLASS);
        }
    }

    /**
     * Yuvarlak kule: taş kaide, kumtaşı gövde, dar mavi camlı pencereler, tepede cam bir halka ve sivri turuncu külah.
     */
    private void tower(int tx, int tz, int r, int base, int top, boolean cone) {
        double rr = r + 0.5;
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double d = Math.hypot(x, z);
                if (d > rr) continue;
                boolean shell = d > rr - 1.3;
                double ang = Math.atan2(z, x);
                for (int y = base; y <= top; y++) {
                    BlockState s;
                    if (!shell) {
                        s = (y - base) % 7 == 0 || y == base ? PLANK : AIR;
                        if (y == top) s = PLANK;
                    } else if (y < base + 4) s = aged(tx + x, y, tz + z);
                    else {
                        int ly = y - base;
                        boolean slit = ly % 7 >= 3 && ly % 7 <= 4 && near(ang, 8, 0.16);
                        boolean ring = y >= top - 4 && y <= top - 2 && near(ang, Math.max(8, r * 3), 0.22);
                        s = slit || ring ? GLASS : y == top - 5 || y == top - 1 ? CUT : SAND;
                    }
                    set(tx + x, y, tz + z, s);
                }
                // mazgallı taç (külahsız kuleler) ya da saçak
                if (shell && !cone && Math.floorMod(x + z, 2) == 0) set(tx + x, top + 1, tz + z, CUT);
            }
        }
        if (cone) cone(tx, tz, r + 1, top + 1, (int) Math.round(r * 2.4 + 3));
        set(tx, base + 1, tz, LANTERN);
    }

    private static boolean near(double ang, int n, double tol) {
        double step = Math.PI * 2 / n;
        double m = Math.abs(((ang % step) + step) % step);
        return m < tol * step * 2 || m > step - tol * step * 2;
    }

    /** Sivri külah: halkalar yükseldikçe daralır; uçta altın ve paratoner. */
    private void cone(int tx, int tz, int r, int base, int height) {
        for (int k = 0; k <= height; k++) {
            double rad = (r + 0.6) * (1 - k / (double) (height + 1));
            int R = (int) Math.ceil(rad);
            for (int x = -R; x <= R; x++) {
                for (int z = -R; z <= R; z++) {
                    double d = Math.hypot(x, z);
                    if (d > rad || d < rad - 1.6) continue;
                    BlockState s = k == 0 ? CUT : (k % 4 == 3 ? Blocks.RED_TERRACOTTA.defaultBlockState() : ROOF);
                    set(tx + x, base + k, tz + z, s);
                }
            }
        }
        set(tx, base + height + 1, tz, Blocks.GOLD_BLOCK.defaultBlockState());
        set(tx, base + height + 2, tz, Blocks.LIGHTNING_ROD.defaultBlockState());
    }

    private void waterfall(int wx, int faceZ, int top, int bottom, int width) {
        int half = width / 2;
        for (int x = wx - half; x <= wx - half + width - 1; x++) {
            // üst kanal
            for (int z = faceZ - 7; z <= faceZ; z++) {
                set(x, top, z, WATER);
                set(x, top - 1, z, BRICK);
                set(x, top + 1, z, AIR);
            }
            // akan sütun
            for (int y = bottom + 1; y <= top; y++) set(x, y, faceZ + 1, FALL);
            // alt havuz
            for (int z = faceZ + 1; z <= faceZ + 6; z++) {
                set(x, bottom, z, WATER);
                set(x, bottom - 1, z, WATER);
                set(x, bottom - 2, z, BRICK);
            }
        }
        // kanal ve havuz kenarları
        for (int z = faceZ - 8; z <= faceZ; z++) {
            set(wx - half - 1, top, z, aged(wx - half - 1, top, z));
            set(wx - half + width, top, z, aged(wx - half + width, top, z));
        }
        for (int x = wx - half; x <= wx - half + width - 1; x++) set(x, top, faceZ - 8, aged(x, top, faceZ - 8));
        for (int z = faceZ + 1; z <= faceZ + 7; z++) {
            for (int x = wx - half - 1; x <= wx - half + width; x++) {
                boolean rim = z == faceZ + 7 || x == wx - half - 1 || x == wx - half + width;
                if (rim) {
                    set(x, bottom, z, CHISELED);
                    set(x, bottom - 1, z, BRICK);
                }
            }
        }
        // kanal başında kaynak süsü
        set(wx, top + 1, faceZ - 8, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
        set(wx, top + 2, faceZ - 8, LANTERN);
    }

    private void trainingYard(int yx, int yz) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -5; z <= 5; z++) {
                boolean edge = Math.abs(x) == 6 || Math.abs(z) == 5;
                set(yx + x, yA, yz + z, Noise.hash(seed + 7, x, 0, z) < 0.3 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState());
                if (edge && !(z == 5 && Math.abs(x) <= 1) && !(x == -6 && Math.abs(z) <= 1)) set(yx + x, yA + 1, yz + z, FENCE);
            }
        }
        // kukla: saman + balkabağı
        for (int[] p : new int[][]{{3, -2}, {-2, -3}, {3, 3}}) {
            set(yx + p[0], yA + 1, yz + p[1], Blocks.HAY_BLOCK.defaultBlockState());
            set(yx + p[0], yA + 2, yz + p[1], Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, Direction.WEST));
        }
        set(yx - 5, yA + 1, yz - 4, Blocks.BARREL.defaultBlockState());
        set(yx + 6, yA + 2, yz - 5, LANTERN);
        set(yx - 6, yA + 2, yz + 5, LANTERN);
    }

    private void library(int lx, int lz) {
        int w = 17, d = 13;
        int x0 = lx - w / 2, x1 = x0 + w - 1, z0 = lz - d / 2, z1 = z0 + d - 1;
        house(lx, lz, w, d, yB, 2, Direction.EAST);
        // iç duvarlar kitaplık, ortada kürsü ve büyü masası
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int y = yB + 1; y <= yB + 3; y++) {
                set(x, y, z0 + 1, Blocks.BOOKSHELF.defaultBlockState());
                set(x, y, z1 - 1, Blocks.BOOKSHELF.defaultBlockState());
            }
        }
        set(lx, yB + 1, lz, Blocks.ENCHANTING_TABLE.defaultBlockState());
        set(lx - 3, yB + 1, lz, Blocks.LECTERN.defaultBlockState());
        set(lx + 3, yB + 1, lz, Blocks.LECTERN.defaultBlockState());
        set(lx, yB + 4, lz, LANTERN.setValue(LanternBlock.HANGING, true));
        // kütüphanenin yanında yuvarlak okuma kulesi
        tower(x0 - 4, lz, 4, yB, yB + 20, true);
        // giriş kemeri ve oturma bankları
        for (int z = lz - 2; z <= lz + 2; z += 4) set(x1 + 2, yB + 1, z, stair(Blocks.SPRUCE_STAIRS.defaultBlockState(), Direction.WEST, false));
    }

    private void archeryRange(int ax, int az) {
        for (int x = -12; x <= 12; x++) {
            for (int z = -10; z <= 10; z++) {
                boolean edge = Math.abs(x) == 12 || Math.abs(z) == 10;
                if (edge && !(z == 10 && Math.abs(x) <= 1)) set(ax + x, yB + 1, az + z, FENCE);
            }
        }
        for (int x = -9; x <= 9; x += 6) {
            set(ax + x, yB + 1, az - 9, Blocks.HAY_BLOCK.defaultBlockState());
            set(ax + x, yB + 2, az - 9, Blocks.TARGET.defaultBlockState());
            set(ax + x, yB + 3, az - 9, Blocks.HAY_BLOCK.defaultBlockState());
        }
        for (int x = -10; x <= 10; x++) set(ax + x, yB, az + 6, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
        set(ax + 11, yB + 2, az + 9, LANTERN);
        set(ax - 11, yB + 2, az + 9, LANTERN);
        // ok fıçıları
        set(ax - 8, yB + 1, az + 8, Blocks.BARREL.defaultBlockState());
        set(ax - 7, yB + 1, az + 8, Blocks.FLETCHING_TABLE.defaultBlockState());
    }

    private void moonShrine(int sx, int sz) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = Math.hypot(x, z);
                if (d > 6.3) continue;
                set(sx + x, yC, sz + z, d < 1 ? Blocks.SEA_LANTERN.defaultBlockState() : d < 4.5
                        ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.POLISHED_DIORITE.defaultBlockState());
                if (d >= 1.5 && d < 4.5 && h(8, x, 0, z) < 0.35) set(sx + x, yC + 1, sz + z, Blocks.LILY_OF_THE_VALLEY.defaultBlockState());
            }
        }
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4;
            int x = (int) Math.round(Math.cos(ang) * 5.2), z = (int) Math.round(Math.sin(ang) * 5.2);
            for (int y = yC + 1; y <= yC + 4; y++) set(sx + x, y, sz + z, Blocks.QUARTZ_PILLAR.defaultBlockState());
            set(sx + x, yC + 5, sz + z, Blocks.AMETHYST_BLOCK.defaultBlockState());
            set(sx + x, yC + 6, sz + z, Blocks.END_ROD.defaultBlockState());
        }
    }

    private void gardenPond(int px, int pz) {
        for (int x = -7; x <= 7; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = Math.hypot(x / 1.2, z);
                if (d > 6) continue;
                if (d > 4.8) set(px + x, yC, pz + z, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                else {
                    set(px + x, yC, pz + z, WATER);
                    set(px + x, yC - 1, pz + z, WATER);
                    set(px + x, yC - 2, pz + z, Blocks.CLAY.defaultBlockState());
                    if (h(9, x, 0, z) < 0.12) set(px + x, yC + 1, pz + z, Blocks.LILY_PAD.defaultBlockState());
                }
            }
        }
        // kemerli küçük köşk
        for (int[] p : new int[][]{{-9, -3}, {-9, 3}, {-13, -3}, {-13, 3}}) {
            for (int y = yC + 1; y <= yC + 4; y++) set(px + p[0], y, pz + p[1], Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        cone(px - 11, pz, 4, yC + 5, 7);
        set(px - 11, yC + 4, pz, LANTERN.setValue(LanternBlock.HANGING, true));
    }

    /** Kale: ana salon (beşik çatılı), önde iki kule, arkada dev yuvarlak kule (donjon), arka köşelerde ince kuleler. */
    private void castle() {
        int x0 = -13, x1 = 13, z0 = -80, z1 = -62;
        int top = yC + 13;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                set(x, yC, z, wall ? aged(x, yC, z) : Blocks.POLISHED_ANDESITE.defaultBlockState());
                for (int y = yC + 1; y <= top; y++) {
                    if (!wall) {
                        set(x, y, z, y == yC + 7 && (Math.abs(x) > 6 || z < -74) ? PLANK : AIR);
                        continue;
                    }
                    boolean along = z == z0 || z == z1;
                    int k = along ? x - x0 : z - z0;
                    int ly = y - yC;
                    boolean win = !along && k % 4 == 2 && ((ly >= 3 && ly <= 5) || (ly >= 9 && ly <= 11));
                    BlockState s = y <= yC + 2 ? aged(x, y, z) : y == top || ly == 7 ? CUT : win ? GLASS : SAND;
                    set(x, y, z, s);
                }
            }
        }
        // büyük kemerli kapı ve gül pencere
        for (int x = -2; x <= 2; x++) for (int y = yC + 1; y <= yC + (Math.abs(x) == 2 ? 4 : 5); y++) set(x, y, z1, AIR);
        for (int x = -3; x <= 3; x++) set(x, yC + 6, z1, CHISELED);
        for (int x = -3; x <= 3; x++) {
            for (int y = yC + 7; y <= yC + 12; y++) {
                double d = Math.hypot(x, y - (yC + 9.5));
                if (d < 3.1) set(x, y, z1, d < 1 ? Blocks.YELLOW_STAINED_GLASS.defaultBlockState() : GLASS);
            }
        }
        // iç süslemeler: kırmızı halı, tahtlar, sütunlar
        for (int z = z0 + 1; z <= z1 - 1; z++) for (int x = -1; x <= 1; x++) set(x, yC + 1, z, Blocks.GREEN_CARPET.defaultBlockState());
        for (int z = z0 + 3; z <= z1 - 3; z += 4) {
            for (int x : new int[]{-6, 6}) for (int y = yC + 1; y <= yC + 6; y++) set(x, y, z, Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        set(0, yC + 1, z0 + 2, stair(Blocks.QUARTZ_STAIRS.defaultBlockState(), Direction.NORTH, false));
        set(-2, yC + 1, z0 + 2, Blocks.LANTERN.defaultBlockState());
        set(2, yC + 1, z0 + 2, Blocks.LANTERN.defaultBlockState());
        for (int z = z0 + 2; z <= z1 - 2; z += 5) set(0, yC + 6, z, LANTERN.setValue(LanternBlock.HANGING, true));
        gableRoof(x0, z0, x1, z1, top);
        // kuleler
        tower(-16, -63, 5, yC - 1, yC + 24, true);
        tower(16, -63, 5, yC - 1, yC + 24, true);
        tower(0, -90, 10, yC - 1, yC + 34, true);
        tower(-19, -97, 4, yC - 1, yC + 30, true);
        tower(19, -97, 4, yC - 1, yC + 30, true);
        tower(-15, -82, 3, yC - 1, yC + 27, true);
        tower(15, -82, 3, yC - 1, yC + 27, true);
        // salondan donjona geçit
        for (int z = -81; z >= -82; z--) for (int x = -1; x <= 1; x++) for (int y = yC + 1; y <= yC + 3; y++) set(x, y, z, AIR);
        // kuleler arası korkuluklu köprüler
        bridge(-16, -16, -63, -82, yC + 18);
        bridge(16, 16, -63, -82, yC + 18);
        // kale terası ön korkuluğu ve fenerler
        for (int x = -30; x <= 30; x += 10) lanternPost(x == 0 ? 8 : x, yC, -59);
    }

    /** İki kule arasında, z boyunca korkuluklu taş köprü. */
    private void bridge(int xa, int xb, int za, int zb, int y) {
        int x = (xa + xb) / 2;
        for (int z = Math.min(za, zb); z <= Math.max(za, zb); z++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (get(x + dx, y, z) != null && get(x + dx, y, z) != AIR && !get(x + dx, y, z).is(Blocks.SPRUCE_PLANKS)) continue;
                set(x + dx, y, z, BRICK);
                set(x + dx, y - 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            }
            set(x - 2, y + 1, z, wallZ(Blocks.STONE_BRICK_WALL.defaultBlockState()));
            set(x + 2, y + 1, z, wallZ(Blocks.STONE_BRICK_WALL.defaultBlockState()));
            set(x - 2, y, z, BRICK);
            set(x + 2, y, z, BRICK);
        }
    }

    /** Bir sütun kentin şekillendirilmiş alanında mı? (ağırlık 0..1) */
    public double weight(int x, int z) {
        int rx = x - cx, rz = z - cz;
        double d = Math.max(0, Math.max(Math.abs(rx) - CityLayout.MAX_X, Math.max(rz - CityLayout.MAX_Z, CityLayout.MIN_Z - rz)));
        if (d >= CityLayout.BLEND) return 0;
        double t = 1 - d / CityLayout.BLEND;
        return t * t * (3 - 2 * t);
    }

    public int target(int x, int z) {
        int rz = Math.max(CityLayout.MIN_Z, Math.min(CityLayout.MAX_Z, z - cz));
        return level(rz);
    }

    public boolean footprint(int x, int z) {
        int rx = x - cx, rz = z - cz;
        return Math.abs(rx) <= CityLayout.MAX_X && rz >= CityLayout.MIN_Z && rz <= CityLayout.MAX_Z;
    }

    public static BlockState grass() {
        return GRASS;
    }

    public static BlockState dirt() {
        return DIRT;
    }
}
