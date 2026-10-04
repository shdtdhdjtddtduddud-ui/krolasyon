package com.krolasyon.bosses.rpg.world;

import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** Geography of the RPG world: where kingdoms are, which region a position belongs to, and where settlements stand. */
public final class WorldMap {
    private WorldMap() {}

    private static final String[] TOWN_PREFIX = {"Kuzey", "Güney", "Eski", "Yeni", "Yukarı", "Aşağı", "Kızıl", "Ak", "Kara", "Gök", "Taş", "Altın"};
    private static final String[] TOWN_ROOT = {"Pınar", "Köprü", "Yaka", "Dere", "Tepe", "Kale", "Ova", "Liman", "Çeşme", "Orman", "Bayır", "Yurt", "Ocak", "Han"};
    private static final String[] ELF_TOWN = {"Ithilien", "Caras Galadon", "Elenrod", "Sílmaren", "Nimloth", "Aelvarin", "Lindorë"};
    private static final String[] DWARF_TOWN = {"Demirocak", "Taşkapı", "Örsdağ", "Kömürkuyu", "Barak Varr", "Kazad Toruk"};
    private static final String[] DEMON_TOWN = {"Kükürt Çukuru", "Kemik Kalesi", "Alev Kapısı", "Lav Nehri", "Kızıl Uçurum"};
    private static final String[] GIANT_TOWN = {"Buzkale", "Rüzgarçatı", "Kar Ocağı", "Dev Merdiveni", "Ayaz Yurdu"};
    private static final String[] ORC_TOWN = {"Kafatası Tepesi", "Kan Pınarı", "Demirdiş Obası", "Kurt Ağılı", "Savaş Davulu"};
    private static final String[] BEAST_TOWN = {"Yele Ağacı", "Pençe Pınarı", "Ay Ulusu", "Sessiz Patika", "Kuyruk Vadisi"};
    private static final String[] HALF_TOWN = {"Yuvarlakköy", "Çayırhan", "Tuzliman", "Altınköprü", "Ballıdere"};
    private static final String[] DARK_TOWN = {"Örümcek Ini", "Gölge Kuyusu", "Yarımay Kalesi", "Fısıltı Mağarası", "Kara Kökler"};

    /** biome the capital of a kingdom should sit in (searched near its nominal position) */
    private static Predicate<Holder<Biome>> preferred(Kingdom k) {
        return switch (k) {
            case ALDORIA, VALDREN -> b -> b.is(Tags.Biomes.IS_PLAINS) || b.is(BiomeTags.IS_FOREST) && !b.is(Biomes.DARK_FOREST);
            case SYLVARIEN -> b -> b.is(BiomeTags.IS_FOREST) && !b.is(Biomes.DARK_FOREST) || b.is(Biomes.CHERRY_GROVE);
            case KHAZDUR -> b -> b.is(BiomeTags.IS_MOUNTAIN) || b.is(Biomes.STONY_PEAKS) || b.is(Biomes.WINDSWEPT_HILLS);
            case INFERNAX -> b -> b.is(BiomeTags.IS_BADLANDS) || b.is(Tags.Biomes.IS_DESERT);
            case YMIRHEIM -> b -> b.is(Tags.Biomes.IS_SNOWY) && !b.is(BiomeTags.IS_OCEAN);
            case GORMASH -> b -> b.is(BiomeTags.IS_SAVANNA) || b.is(BiomeTags.IS_BADLANDS);
            case FELARIS -> b -> b.is(BiomeTags.IS_JUNGLE) || b.is(BiomeTags.IS_SAVANNA);
            case MERIDIA -> b -> b.is(BiomeTags.IS_BEACH) || b.is(Tags.Biomes.IS_PLAINS);
            case NOCTHERA -> b -> b.is(Biomes.DARK_FOREST) || b.is(BiomeTags.IS_TAIGA);
        };
    }

    /** biome test for each wild region, used both to classify positions and to place lairs */
    private static boolean wildMatches(RegionId r, Holder<Biome> b) {
        return switch (r) {
            case CURSED_RUINS -> b.is(Biomes.DARK_FOREST) || b.is(Tags.Biomes.IS_SPOOKY);
            case DEAD_MARSH -> b.is(Tags.Biomes.IS_SWAMP) || b.is(Biomes.MANGROVE_SWAMP);
            case GLASS_DESERT -> b.is(Tags.Biomes.IS_DESERT) || b.is(BiomeTags.IS_BADLANDS);
            case DRAGON_TEETH -> b.is(BiomeTags.IS_MOUNTAIN) || b.is(Tags.Biomes.IS_PEAK) || b.is(Biomes.WINDSWEPT_HILLS) || b.is(Biomes.WINDSWEPT_GRAVELLY_HILLS);
            case STORM_COAST -> b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH) || b.is(Biomes.STONY_SHORE);
            case WHISPER_STEPPE -> b.is(Tags.Biomes.IS_PLAINS) || b.is(BiomeTags.IS_SAVANNA);
            case CRYSTAL_HOLLOW -> b.is(Biomes.LUSH_CAVES) || b.is(Biomes.DRIPSTONE_CAVES) || b.is(Biomes.DEEP_DARK);
            case WOLF_FOREST -> b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA) || b.is(BiomeTags.IS_JUNGLE);
            default -> false;
        };
    }

    public static final RegionId[] WILD = {RegionId.CURSED_RUINS, RegionId.DEAD_MARSH, RegionId.GLASS_DESERT, RegionId.DRAGON_TEETH,
            RegionId.STORM_COAST, RegionId.CRYSTAL_HOLLOW, RegionId.WHISPER_STEPPE, RegionId.WOLF_FOREST};

    // ------------------------------------------------------------------ queries
    /** kingdom whose land contains the position, or -1 */
    public static int kingdomAt(RpgWorldData d, double x, double z) {
        int best = -1;
        double bestScore = 1.0;
        for (Kingdom k : Kingdom.values()) {
            double dx = x - d.capX[k.ordinal()], dz = z - d.capZ[k.ordinal()];
            double s = Math.sqrt(dx * dx + dz * dz) / k.radius;
            if (s < bestScore) { bestScore = s; best = k.ordinal(); }
        }
        return best;
    }

    public static RegionId regionAt(ServerLevel level, BlockPos pos) {
        RpgWorldData d = RpgWorldData.get(level);
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return level.dimension() == net.minecraft.world.level.Level.NETHER ? RegionId.INFERNAX : RegionId.CRYSTAL_HOLLOW;
        }
        if (pos.getY() < 35 && !level.canSeeSky(pos)) return RegionId.CRYSTAL_HOLLOW;
        int k = kingdomAt(d, pos.getX(), pos.getZ());
        if (k >= 0) return RegionId.values()[k];
        Holder<Biome> b = level.getBiome(pos);
        for (RegionId r : WILD) if (wildMatches(r, b)) return r;
        return RegionId.WOLF_FOREST;
    }

    @Nullable
    public static Site nearestSite(RpgWorldData d, double x, double z, double maxDist, @Nullable Site.Type type) {
        Site best = null;
        double bd = maxDist * maxDist;
        for (Site s : d.sites) {
            if (type != null && s.type != type) continue;
            double dd = s.distSq(x, z);
            if (dd < bd) { bd = dd; best = s; }
        }
        return best;
    }

    /** settlement whose walls contain the position */
    @Nullable
    public static Site siteAt(RpgWorldData d, double x, double z) {
        for (Site s : d.sites) {
            if (s.type == Site.Type.LAIR || s.type == Site.Type.RUIN || s.type == Site.Type.LANDMARK) continue;
            double r = s.type.radius + 4;
            if (s.distSq(x, z) < r * r) return s;
        }
        return null;
    }

    public static Site capital(RpgWorldData d, int kingdom) {
        for (Site s : d.sites) if (s.type == Site.Type.CAPITAL && s.kingdom == kingdom) return s;
        return d.sites.get(0);
    }

    // ------------------------------------------------------------------ world creation
    public static void init(ServerLevel level, RpgWorldData d) {
        if (d.initialized) return;
        BlockPos spawn = level.getSharedSpawnPos();
        d.origin = spawn;
        RandomSource r = RandomSource.create(level.getSeed() ^ 0x5EEDL);
        for (Kingdom k : Kingdom.values()) {
            int nx = spawn.getX() + k.dx, nz = spawn.getZ() + k.dz;
            BlockPos at = new BlockPos(nx, 80, nz);
            if (k != Kingdom.ALDORIA) {
                try {
                    Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(preferred(k), at, 900, 48, 64);
                    if (found != null) at = found.getFirst();
                } catch (Exception ignored) {}
            } else {
                at = new BlockPos(spawn.getX() + 6, 80, spawn.getZ() + 6);
            }
            d.capX[k.ordinal()] = at.getX();
            d.capZ[k.ordinal()] = at.getZ();
        }
        for (Kingdom k : Kingdom.values()) {
            Site cap = new Site();
            cap.id = "cap_" + k.name().toLowerCase();
            cap.name = k.capital;
            cap.type = Site.Type.CAPITAL;
            cap.kingdom = k.ordinal();
            cap.x = d.capX[k.ordinal()];
            cap.z = d.capZ[k.ordinal()];
            cap.seed = r.nextLong();
            d.sites.add(cap);
            int towns = 2, villages = 3;
            double a0 = r.nextDouble() * Math.PI * 2;
            for (int i = 0; i < towns + villages; i++) {
                Site s = new Site();
                boolean town = i < towns;
                s.type = town ? Site.Type.CITY : Site.Type.VILLAGE;
                s.kingdom = k.ordinal();
                double a = a0 + i * Math.PI * 2 / (towns + villages) + (r.nextDouble() - 0.5) * 0.5;
                double dist = (town ? 0.42 : 0.62) * k.radius + r.nextDouble() * 80;
                s.x = (int) (cap.x + Math.cos(a) * dist);
                s.z = (int) (cap.z + Math.sin(a) * dist);
                s.name = townName(k, r);
                s.id = (town ? "city_" : "vil_") + k.name().toLowerCase() + "_" + i;
                s.seed = r.nextLong();
                d.sites.add(s);
            }
        }
        // bandit camps and ruins on the roads between kingdoms
        for (int i = 0; i < 16; i++) {
            double a = r.nextDouble() * Math.PI * 2, dist = 500 + r.nextDouble() * 3000;
            int x = (int) (spawn.getX() + Math.cos(a) * dist), z = (int) (spawn.getZ() + Math.sin(a) * dist);
            if (nearestSite(d, x, z, 220, null) != null) continue;
            Site s = new Site();
            s.type = i % 3 == 0 ? Site.Type.RUIN : Site.Type.CAMP;
            s.kingdom = -1;
            s.x = x; s.z = z;
            s.seed = r.nextLong();
            s.name = s.type == Site.Type.RUIN ? "Unutulmuş " + TOWN_ROOT[r.nextInt(TOWN_ROOT.length)] + " Harabesi" : TOWN_PREFIX[r.nextInt(TOWN_PREFIX.length)] + " Haydut Kampı";
            s.id = (s.type == Site.Type.RUIN ? "ruin_" : "camp_") + i;
            d.sites.add(s);
        }
        // landmarks: statues, watchtowers, forgotten temples, dragon bones, crystal spires, standing stones
        String[] landmarkNames = {"Kadim Kral Heykeli", "Gözcü Kulesi", "Terk Edilmiş Tapınak", "Ejder İskeleti", "Kristal Sütunlar", "Dikili Taşlar"};
        int placed = 0;
        for (int i = 0; i < 80 && placed < 30; i++) {
            double a = r.nextDouble() * Math.PI * 2, dist = 300 + r.nextDouble() * 3600;
            int x = (int) (spawn.getX() + Math.cos(a) * dist), z = (int) (spawn.getZ() + Math.sin(a) * dist);
            if (nearestSite(d, x, z, 160, null) != null) continue;
            Site s = new Site();
            s.type = Site.Type.LANDMARK;
            s.seed = r.nextLong();
            int variant = (int) Math.floorMod(s.seed, landmarkNames.length);
            s.kingdom = kingdomAt(d, x, z);
            s.x = x;
            s.z = z;
            s.name = landmarkNames[variant];
            s.boss = String.valueOf(variant);
            s.id = "landmark_" + placed;
            d.sites.add(s);
            placed++;
        }
        // one lair per boss, in a place of its home region
        int li = 0;
        for (MonsterDef b : RpgDefs.BOSSES) {
            RegionId reg = b.regions()[0];
            Site s = new Site();
            s.type = Site.Type.LAIR;
            s.boss = b.id();
            s.bossAlive = true;
            s.name = b.name() + " İni";
            s.id = "lair_" + b.id();
            s.seed = r.nextLong();
            BlockPos at = null;
            if (!reg.wild()) {
                int k = reg.kingdom;
                double a = r.nextDouble() * Math.PI * 2;
                double dist = Kingdom.of(k).radius * (0.75 + r.nextDouble() * 0.15);
                at = new BlockPos((int) (d.capX[k] + Math.cos(a) * dist), 80, (int) (d.capZ[k] + Math.sin(a) * dist));
            } else {
                for (int tries = 0; tries < 6 && at == null; tries++) {
                    double a = r.nextDouble() * Math.PI * 2, dist = 1400 + r.nextDouble() * 2600;
                    BlockPos probe = new BlockPos((int) (spawn.getX() + Math.cos(a) * dist), reg == RegionId.CRYSTAL_HOLLOW ? 0 : 80, (int) (spawn.getZ() + Math.sin(a) * dist));
                    try {
                        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> wildMatches(reg, h), probe, 900, 48, 64);
                        if (found != null && kingdomAt(d, found.getFirst().getX(), found.getFirst().getZ()) < 0) at = found.getFirst();
                    } catch (Exception ignored) {}
                }
                if (at == null) {
                    double a = li * 2.4, dist = 2200 + li * 37;
                    at = new BlockPos((int) (spawn.getX() + Math.cos(a) * dist), 80, (int) (spawn.getZ() + Math.sin(a) * dist));
                }
            }
            s.x = at.getX();
            s.z = at.getZ();
            s.kingdom = reg.kingdom;
            d.sites.add(s);
            li++;
        }
        // politics: the old rivalries of the world
        int A = Kingdom.ALDORIA.ordinal(), S = Kingdom.SYLVARIEN.ordinal(), K = Kingdom.KHAZDUR.ordinal(), I = Kingdom.INFERNAX.ordinal(),
                Y = Kingdom.YMIRHEIM.ordinal(), G = Kingdom.GORMASH.ordinal(), F = Kingdom.FELARIS.ordinal(), V = Kingdom.VALDREN.ordinal(),
                M = Kingdom.MERIDIA.ordinal(), N = Kingdom.NOCTHERA.ordinal();
        d.changeRelation(A, S, 20); d.changeRelation(A, K, 35); d.changeRelation(A, V, -60); d.changeRelation(A, I, -70);
        d.changeRelation(S, N, -80); d.changeRelation(S, F, 40); d.changeRelation(K, Y, -40); d.changeRelation(K, M, 50);
        d.changeRelation(I, N, 30); d.changeRelation(I, G, 20); d.changeRelation(G, F, -50); d.changeRelation(V, I, 15);
        d.changeRelation(M, A, 30); d.changeRelation(Y, G, -20); d.changeRelation(V, M, 10);
        for (int i = 0; i < Kingdom.COUNT; i++) d.changeRelation(i, I, i == I ? 0 : -20);
        d.setWar(A, V, true);
        d.setWar(S, N, true);
        d.news("Aldoria ile Valdren arasında sınır savaşı sürüyor.");
        d.news("Elfler ve kara elfler arasındaki kadim kan davası yeniden alevlendi.");
        d.news("Kül Topraklarından gelen iblis akınları köyleri tedirgin ediyor.");
        d.initialized = true;
        d.setDirty();
    }

    private static String townName(Kingdom k, RandomSource r) {
        String[] pool = switch (k.race) {
            case ELF -> ELF_TOWN; case DWARF -> DWARF_TOWN; case DEMON -> DEMON_TOWN; case GIANT -> GIANT_TOWN; case ORC -> ORC_TOWN;
            case BEASTKIN -> BEAST_TOWN; case HALFLING -> HALF_TOWN; case DARK_ELF -> DARK_TOWN; default -> null;
        };
        if (pool != null) return pool[r.nextInt(pool.length)];
        return TOWN_PREFIX[r.nextInt(TOWN_PREFIX.length)] + TOWN_ROOT[r.nextInt(TOWN_ROOT.length)].toLowerCase();
    }
}
