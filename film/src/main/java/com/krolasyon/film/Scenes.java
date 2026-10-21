package com.krolasyon.film;

import com.krolasyon.advgear.AdvGear;
import com.krolasyon.advgear.block.LockType;
import com.krolasyon.advgear.block.LockedChestBlock;
import com.krolasyon.advgear.gear.GearData;
import com.krolasyon.aethercore.AetherCore;
import com.krolasyon.aethercore.block.AetherCoreBlockEntity;
import com.krolasyon.aethercore.world.ShrineFeature;
import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.elvenrealm.ElvenRealm;
import com.krolasyon.elvenrealm.dialogue.DialogueManager;
import com.krolasyon.elvenrealm.dialogue.PlayerData;
import com.krolasyon.elvenrealm.entity.Activity;
import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.krolasyon.elvenrealm.entity.ElfKind;
import com.krolasyon.elvenrealm.world.CityLayout;
import com.krolasyon.skyislands.world.Island;
import com.krolasyon.skyislands.world.IslandLayout;
import com.krolasyon.stormray.entity.StormRayEntity;
import com.krolasyon.swordvale.world.SwordShape;
import com.krolasyon.swordvale.world.ValeLayout;
import com.krolasyon.sylvanarchery.item.QuiverItem;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

import static com.krolasyon.film.Cam.v;
import static com.krolasyon.film.Director.f;

/** Videonun senaryosu: Sylvaren'in kuşatılması etrafında tüm modları gösteren bölümler. */
public final class Scenes {
    private Scenes() {}

    // ------------------------------------------------------------------ müzik (oyunun kendi parçaları)
    private static final String MUS = "minecraft:sounds/music/";
    private static final String[] M_INTRO = {MUS + "game/stand_tall.ogg", MUS + "game/wending.ogg"};
    private static final String[] M_ISLANDS = {MUS + "game/floating_dream.ogg", MUS + "game/comforting_memories.ogg"};
    private static final String[] M_RAY = {"minecraft:sounds/records/otherside.ogg", MUS + "game/an_ordinary_day.ogg"};
    private static final String[] M_CITY = {MUS + "game/echo_in_the_wind.ogg", MUS + "game/left_to_bloom.ogg", MUS + "game/bromeliad.ogg"};
    private static final String[] M_GEAR = {"minecraft:sounds/records/pigstep.ogg", MUS + "game/a_familiar_room.ogg"};
    private static final String[] M_VALE = {MUS + "game/ancestry.ogg", MUS + "game/wending.ogg"};
    private static final String[] M_SIEGE = {MUS + "game/nether/nether_wastes/rubedo.ogg", MUS + "game/nether/rubedo.ogg", MUS + "game/nether/nether1.ogg"};
    private static final String[] M_BOSS = {MUS + "game/end/boss.ogg", MUS + "game/nether/soulsand_valley/so_below.ogg"};
    private static final String[] M_OUTRO = {MUS + "game/one_more_day.ogg", MUS + "game/stand_tall.ogg"};

    // ------------------------------------------------------------------ vurgu sesleri
    private static final String S_THUNDER = "minecraft:sounds/ambient/weather/thunder1.ogg";
    private static final String S_BOOM = "minecraft:sounds/random/explode1.ogg";
    private static final String S_WHOOSH = "minecraft:sounds/item/trident/riptide1.ogg";
    private static final String S_CHIME = "minecraft:sounds/block/amethyst/resonate1.ogg";
    private static final String S_BEACON = "minecraft:sounds/block/beacon/activate.ogg";
    private static final String S_GROWL = "minecraft:sounds/mob/enderdragon/growl1.ogg";
    private static final String S_BELL = "minecraft:sounds/block/bell/bell_use01.ogg";

    private static final int GOLD = 0xF2C14E, RED = 0xE04848;

    // ------------------------------------------------------------------ konum kısayolları
    private static Vec3 C(Director d, double x, double y, double z) { return v(d.w.cx + x, d.w.y0 + y, d.w.cz + z); }
    private static Vec3 I(Director d, double x, double y, double z) { return v(d.w.isl.cx() + x, d.w.iy + y, d.w.isl.cz() + z); }
    private static Vec3 F(Director d, double x, double y, double z) { return v(d.w.fx + x, d.w.fy + y, d.w.fz + z); }
    private static Vec3 V(Director d, double x, double y, double z) { return v(d.w.vale.cx() + x, d.w.vy + y, d.w.vale.cz() + z); }
    private static int R(Director d) { return d.w.isl.radius(); }

    private static Vec3 pt(Director d, String k) {
        Vec3 p = d.w.pts.get(k);
        return p == null ? F(d, 0, 0, 0) : p;
    }

    // ------------------------------------------------------------------ dünya hazırlığı (sunucu iş parçacığı)

    static void init(Director d) {
        ServerLevel l = d.lvl();
        ChunkGenerator gen = l.getChunkSource().getGenerator();
        RandomState rs = l.getChunkSource().randomState();
        long seed = l.getSeed();
        d.run("gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "gamerule announceAdvancements false", "gamerule doInsomnia false", "gamerule doPatrolSpawning false",
                "gamerule doTraderSpawning false", "gamerule showDeathMessages false", "gamerule mobGriefing false",
                "gamerule keepInventory true", "gamerule doFireTick false", "gamerule sendCommandFeedback false",
                "time set 23300", "weather clear 1000000", "difficulty normal");
        W w = d.w;
        CityLayout.City c = CityLayout.nearest(seed, gen, rs, l, 0, 0, 6);
        if (c == null) c = CityLayout.nearest(seed, gen, rs, l, 0, 0, 12);
        w.cx = c.cx();
        w.cz = c.cz();
        w.y0 = CityLayout.base(c, gen, rs, l);
        w.fx = w.cx;
        w.fz = w.cz + 108;
        w.fy = w.y0 + 1;
        Director.LOG.info("[FILM] city {} {} base {}", w.cx, w.cz, w.y0);

        Island best = null;
        double bd = Double.MAX_VALUE;
        int ci0 = Math.floorDiv(w.cx, IslandLayout.CELL), cj0 = Math.floorDiv(w.cz, IslandLayout.CELL);
        for (int pass = 0; pass < 2 && best == null; pass++) {
            for (int ci = ci0 - 7; ci <= ci0 + 7; ci++) {
                for (int cj = cj0 - 7; cj <= cj0 + 7; cj++) {
                    for (Island is : IslandLayout.cell(seed, ci, cj)) {
                        if (is.satellite() || is.radius() < (pass == 0 ? 18 : 12)) continue;
                        if (blocked(w, is)) continue;
                        double dist = Math.hypot(is.cx() - w.cx, is.cz() - (w.cz - 35));
                        if (dist < 190 || dist > (pass == 0 ? 480 : 700)) continue;
                        double score = dist - (is.giantTree() ? 140 : 0) - is.radius() * 2;
                        if (score < bd) {
                            bd = score;
                            best = is;
                        }
                    }
                }
            }
        }
        if (best == null) best = IslandLayout.nearestMain(seed, w.cx + 300, w.cz, 12, 10);
        w.isl = best;
        int g = gen.getBaseHeight(best.cx(), best.cz(), Heightmap.Types.WORLD_SURFACE_WG, l, rs);
        w.iy = best.baseY() - best.depth() * 2 - 10 > g ? best.baseY() : g + best.depth() * 2 + 12;
        Director.LOG.info("[FILM] island {} lifted base {}", best, w.iy);

        ValeLayout.Vale v = ValeLayout.nearest(seed, gen, rs, w.cx, w.cz, 6);
        if (v == null) v = ValeLayout.nearest(seed, gen, rs, 0, 0, 8);
        w.vale = v;
        if (v != null) {
            w.vy = ValeLayout.ground(gen, rs, l, v.cx(), v.cz());
            float bl = 0;
            for (ValeLayout.Sword sw : ValeLayout.swords(v)) {
                if (sw.length() <= bl || !ValeLayout.biomeOk(gen, rs, sw.x(), sw.z())) continue;
                int gy = ValeLayout.ground(gen, rs, l, sw.x(), sw.z());
                if (gy <= l.getSeaLevel() + 1) continue;
                bl = sw.length();
                w.giant = new SwordShape(sw, gy);
            }
        }
        Director.LOG.info("[FILM] vale {} giant {}", v, w.giant == null ? null : w.giant.s);
    }

    private static boolean blocked(W w, Island is) {
        int r = is.reach() + 40;
        int dx = is.cx() - w.cx, dz = is.cz() - w.cz;
        return Math.abs(dx) <= CityLayout.MAX_X + r && dz >= CityLayout.MIN_Z - r && dz <= CityLayout.MAX_Z + r;
    }

    /** Kapı önündeki alanı ağaçlardan arındırıp kent zemini seviyesinde düz bir meydana çevirir (sunucu). */
    private static void clearField(Director d) {
        ServerLevel l = d.lvl();
        W w = d.w;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = w.fx - 46, x1 = w.fx + 46, z0 = w.fz - 38, z1 = w.fz + 40;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int top = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y > top - 60; y--) {
                    p.set(x, y, z);
                    BlockState b = l.getBlockState(p);
                    if (b.isAir()) continue;
                    if (removable(b)) {
                        l.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                        continue;
                    }
                    break;
                }
            }
        }
        int g = w.y0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int edge = Math.min(Math.min(x - x0, x1 - x), Math.min(z1 - z, z - z0 + 4));
                float wt = Math.min(1F, edge / 9F);
                int ng = d.ground(x, z) - 1;
                int h = Math.round(g + (ng - g) * (1F - wt));
                int topClear = Math.max(ng, h) + 2;
                for (int y = h + 1; y <= topClear + 40 && y < l.getMaxBuildHeight(); y++) {
                    p.set(x, y, z);
                    if (!l.getBlockState(p).isAir()) l.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                p.set(x, h, z);
                l.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2 | 16);
                for (int y = h - 1; y >= h - 6; y--) {
                    p.set(x, y, z);
                    BlockState b = l.getBlockState(p);
                    if (b.isAir() || !b.getFluidState().isEmpty() || removable(b)) l.setBlock(p, Blocks.DIRT.defaultBlockState(), 2 | 16);
                }
            }
        }
        w.fy = g + 1;
        Director.LOG.info("[FILM] field flattened at {}", w.fy);
    }

    private static boolean removable(BlockState b) {
        return b.is(BlockTags.LOGS) || b.is(BlockTags.LEAVES) || b.is(BlockTags.REPLACEABLE) || b.is(BlockTags.FLOWERS)
                || b.is(BlockTags.SAPLINGS) || b.is(Blocks.MOSS_CARPET) || b.is(Blocks.BEE_NEST) || b.is(Blocks.MUSHROOM_STEM)
                || b.is(Blocks.BROWN_MUSHROOM_BLOCK) || b.is(Blocks.RED_MUSHROOM_BLOCK) || b.is(Blocks.SWEET_BERRY_BUSH)
                || b.is(Blocks.BAMBOO) || b.is(Blocks.VINE) || b.is(Blocks.SNOW) || b.is(Blocks.PUMPKIN) || b.is(Blocks.MELON);
    }

    // ------------------------------------------------------------------ ortak eylemler

    private static void spectate(Director d) { d.cmd("gamemode spectator @a"); }

    /** Oyuncuyu hayatta kalma kipinde verilen noktaya (zemine) koyar. */
    private static void stand(Director d, double x, double z, float yaw, float pitch) {
        d.server(l -> {
            int y = d.ground((int) Math.floor(x), (int) Math.floor(z));
            d.run("gamemode survival @a");
            ServerPlayer p = d.sp();
            if (p.isPassenger()) p.stopRiding();
            p.teleportTo(l, x, y, z, yaw, pitch);
            p.setDeltaMovement(Vec3.ZERO);
            d.w.pts.put("me", v(x, y, z));
            d.run("effect give @a minecraft:resistance infinite 2 true", "effect give @a minecraft:saturation infinite 0 true");
        });
    }

    private static void gear(Director d, String ring1, String ring2, String goggles, String belt, String back) {
        d.server(l -> {
            GearData.Gear g = GearData.get(d.sp());
            String[] s = {ring1, ring2, goggles, belt, back};
            for (int i = 0; i < 5; i++) g.setItem(i, s[i] == null ? ItemStack.EMPTY : Director.item("advgear:" + s[i], 1));
        });
    }

    private static void hotbar(Director d, String... ids) {
        d.server(l -> {
            ServerPlayer p = d.sp();
            for (int i = 0; i < 9; i++) p.getInventory().setItem(i, ItemStack.EMPTY);
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == null) continue;
                if (ids[i].equals("quiver")) {
                    ItemStack q = Director.item("sylvanarchery:sylvan_quiver", 1);
                    for (int k = 0; k < 3; k++) QuiverItem.insert(q, Director.item("sylvanarchery:sylvan_arrow", 64));
                    p.getInventory().setItem(i, q);
                } else {
                    p.getInventory().setItem(i, Director.item(ids[i], ids[i].endsWith("arrow") ? 64 : ids[i].endsWith("black_death") ? 3 : 1));
                }
            }
        });
    }

    private static void killFilm(Director d) {
        d.cmd("kill @e[tag=film]", "kill @e[type=item]", "kill @e[type=minecraft:arrow]", "kill @e[type=sylvanarchery:sylvan_arrow]",
                "kill @e[type=minecraft:area_effect_cloud]", "kill @e[type=minecraft:experience_orb]");
    }

    private static final String[] HORDE = {"minecraft:husk", "minecraft:zombie", "minecraft:skeleton", "minecraft:husk", "minecraft:spider",
            "minecraft:vindicator", "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:husk", "minecraft:pillager",
            "minecraft:zombie", "minecraft:skeleton", "minecraft:husk"};

    /** Sunucu: alanın güneyinde bir sürü yaratık; anahtarları h0..hN. */
    private static void horde(Director d, int n, double z0, double spread, boolean dayProof) {
        d.server(l -> {
            for (int i = 0; i < n; i++) {
                String t = HORDE[i % HORDE.length];
                if (dayProof && (t.endsWith("zombie") || t.endsWith("skeleton"))) t = t.endsWith("zombie") ? "minecraft:husk" : "minecraft:stray";
                double x = d.w.fx + (i - n / 2.0) * spread + (i % 3) * 0.7;
                double z = d.w.fz + z0 + (i % 4) * 2.5;
                int y = d.ground((int) Math.floor(x), (int) Math.floor(z));
                d.spawn("h" + i, t, x + 0.5, y, z + 0.5, 180F, e -> {
                    if (e instanceof Mob m) {
                        m.setTarget(d.sp());
                        if (m.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && (m.getType().toString().contains("zombie")
                                || m.getType().toString().contains("skeleton"))) m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    }
                });
            }
        });
    }

    /** İstemci: canlı sürü üyelerinin ortalaması. */
    private static Vec3 hordeCenter(Director d, int n, Vec3 fallback) {
        double x = 0, y = 0, z = 0;
        int k = 0;
        for (int i = 0; i < n; i++) {
            Entity e = d.ce("h" + i);
            if (e == null || !e.isAlive()) continue;
            Vec3 p = Director.at(e, d.partial());
            x += p.x;
            y += p.y;
            z += p.z;
            k++;
        }
        return k == 0 ? fallback : v(x / k, y / k + 1.0, z / k);
    }

    /** İstemci: oyuncuya en yakın canlı sürü üyesi. */
    private static Entity nearest(Director d, int n, boolean far) {
        Entity best = null;
        double bd = far ? -1 : Double.MAX_VALUE;
        Vec3 me = d.me();
        for (int i = 0; i < n; i++) {
            Entity e = d.ce("h" + i);
            if (e == null || !e.isAlive()) continue;
            double dist = e.position().distanceToSqr(me);
            if (far ? dist > bd && dist < 30 * 30 : dist < bd) {
                bd = dist;
                best = e;
            }
        }
        return best;
    }

    private static float[] aimEntity(Director d, Entity e, float soft) {
        if (e == null) return null;
        return d.aimAt(Director.at(e, d.partial()).add(0, e.getBbHeight() * 0.6, 0), soft);
    }

    private static float[] aimKey(Director d, String key, float soft) { return aimEntity(d, d.ce(key), soft); }

    /** İstemci: sağ tık basılı tut / bırak. */
    private static void use(Director d, boolean down) { d.key(d.mc().options.keyUse, down); }

    private static void interact(Director d, String key) {
        Entity e = d.ce(key);
        if (e == null) return;
        d.mc().gameMode.interact(d.lp(), e, InteractionHand.MAIN_HAND);
        d.lp().swing(InteractionHand.MAIN_HAND);
    }

    private static ElfEntity elf(Director d, String key) {
        Entity e = d.se(key);
        return e instanceof ElfEntity el ? el : null;
    }

    private static BossEntity boss(Director d, String key) {
        Entity e = d.se(key);
        return e instanceof BossEntity b ? b : null;
    }

    private static void force(Director d, String bossKey, int ability, String targetKey) {
        d.server(l -> {
            BossEntity b = boss(d, bossKey);
            if (b == null || !b.isAlive()) return;
            LivingEntity t = targetKey == null ? d.sp() : d.se(targetKey) instanceof LivingEntity le ? le : d.sp();
            b.debugForceAbility(ability, t);
        });
    }

    private static void lightning(Director d, Vec3 p) {
        d.cmd(f("summon minecraft:lightning_bolt %.1f %.1f %.1f", p.x, p.y, p.z));
    }

    private static void firework(Director d, Vec3 p, int c1, int c2, int type) {
        d.cmd(f("summon minecraft:firework_rocket %.1f %.1f %.1f {LifeTime:%d,FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,"
                + "tag:{Fireworks:{Flight:2b,Explosions:[{Type:%db,Flicker:1b,Trail:1b,Colors:[I;%d,%d],FadeColors:[I;16777215]}]}}}}",
                p.x, p.y, p.z, 28 + type * 3, type, c1, c2));
    }

    /** Kiralık bir dost elf (sunucu). */
    private static void ally(Director d, ElfKind k, String key, double x, double z, boolean hire) {
        d.server(l -> {
            int y = d.ground((int) Math.floor(x), (int) Math.floor(z));
            ElfEntity e = ElvenRealm.TYPES.get(k).get().create(l);
            if (e == null) return;
            e.moveTo(x, y, z, 180F, 0F);
            e.setHome(BlockPos.containing(x, y, z));
            e.finalizeSpawn(l, l.getCurrentDifficultyAt(e.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
            e.addTag("cast");
            l.addFreshEntity(e);
            d.ids.put(key, e.getUUID());
            if (hire) e.hire(d.sp(), 20L * 60 * 60);
        });
    }

    private static void ray(Director d, Vec3 p, boolean ride) {
        d.server(l -> {
            Entity e = d.se("ray");
            StormRayEntity r;
            if (e instanceof StormRayEntity sr && sr.isAlive()) r = sr;
            else {
                r = (StormRayEntity) d.spawn("ray", "stormray:storm_ray", p.x, p.y, p.z, 180F, x -> {
                    x.removeTag("film");
                    x.addTag("cast");
                });
                if (r == null) return;
            }
            ServerPlayer sp = d.sp();
            if (!r.isTame()) r.tame(sp);
            r.setSaddled(true);
            r.setNoAi(false);
            if (sp.isPassenger()) sp.stopRiding();
            r.ejectPassengers();
            r.teleportTo(p.x, p.y, p.z);
            r.setDeltaMovement(Vec3.ZERO);
            if (ride) {
                d.run("gamemode survival @a");
                sp.teleportTo(l, p.x, p.y + 1, p.z, r.getYRot(), 0F);
                sp.startRiding(r, true);
            }
        });
    }

    // ------------------------------------------------------------------ senaryo

    public static List<Shot> build() {
        List<Shot> s = new ArrayList<>();

        // ============================================================ GİRİŞ
        s.add(new Shot("intro", 260).fade(50, 0).music(0.55F, M_INTRO)
                .title("KROLASYON", "Bir Fantastik Dünya Macerası", 40, 200, 3, GOLD)
                .accent(40, S_THUNDER, 0.55F, 0.7F).accent(40, S_BEACON, 0.7F, 0.8F)
                .setup(d -> {
                    spectate(d);
                    d.cmd("time set 23300", "weather clear 1000000");
                })
                .every((d, t) -> d.server(l -> l.setDayTime(l.getDayTime() + 3)))
                .cam((d, t, u) -> Cam.move(C(d, 0, 120, 230), C(d, 0, 90, 118), C(d, 0, 30, -30), C(d, 0, 36, -60), u, 0, 0, 70)));

        s.add(new Shot("intro_reveal", 220)
                .every((d, t) -> d.server(l -> l.setDayTime(l.getDayTime() + 3)))
                .cam((d, t, u) -> {
                    Vec3 p = Cam.spline(List.of(C(d, 28, 6, 42), C(d, 18, 34, 14), C(d, 4, 70, -14), C(d, -6, 98, -30)), Cam.soft(u));
                    Vec3 look = Cam.lerp(C(d, 0, 25, -60), I(d, 0, 10, 0), Cam.ease(u));
                    return Cam.look(p, look, 0, 72);
                }));

        // ============================================================ GÖK ADALARI
        s.add(new Shot("isl_orbit", 280).fade(15, 0).music(0.5F, M_ISLANDS)
                .chapter("GÖK ADALARI", "Bulutların üzerindeki kayıp topraklar", 25, 120)
                .accent(25, S_WHOOSH, 0.6F, 0.8F).accent(25, S_CHIME, 0.8F, 0.9F)
                .setup(d -> {
                    spectate(d);
                    d.cmd("time set 6000");
                })
                .cam((d, t, u) -> Cam.orbit(I(d, 0, 4, 0), R(d) + 52, 18, 200, 275, u, v(0, -4, 0), 70)));

        s.add(new Shot("isl_under", 200)
                .cam((d, t, u) -> {
                    int dep = d.w.isl.depth();
                    return Cam.move(I(d, -R(d) - 30, -dep - 28, -25), I(d, R(d) + 30, -dep - 12, 25),
                            I(d, -10, -dep * 0.6, 0), I(d, 10, -dep * 0.4, 0), u, -8, 8, 74);
                }));

        s.add(new Shot("isl_tree", 220)
                .cam((d, t, u) -> Cam.move(I(d, 15, 4, 17), I(d, 32, 62, 36), I(d, 0, 12, 0), I(d, 0, 34, 0), u, 0, 0, 72)));

        s.add(new Shot("isl_top", 200)
                .cam((d, t, u) -> Cam.move(I(d, -R(d) - 12, 10, -R(d) * 0.3), I(d, R(d) + 12, 15, R(d) * 0.3),
                        I(d, 0, 4, 0), I(d, R(d) + 40, -2, R(d) * 0.5), u, 4, -4, 76)));

        s.add(new Shot("isl_seed", 190).play()
                .setup(d -> {
                    double x = d.w.isl.cx() - R(d) * 0.55 + 0.5, z = d.w.isl.cz() + 0.5;
                    stand(d, x, z, 90F, 15F);
                    hotbar(d, "skyislands:island_seed", "skyislands:island_seed");
                    d.select(0);
                })
                .aim((d, t, u) -> new float[]{90F - 34F * Cam.ease((u - 0.45F) / 0.3F), 14F - 26F * Cam.ease(u / 0.35F) + 8F * Cam.ease((u - 0.5F) / 0.3F)})
                .at(45, d -> d.click(d.mc().options.keyUse))
                .at(125, d -> d.click(d.mc().options.keyUse)));

        s.add(new Shot("isl_seed_orbit", 160).view(CameraType.THIRD_PERSON_BACK)
                .cam((d, t, u) -> Cam.orbit(pt(d, "me").add(0, 1, 0), 9, 3, 140, 230, u, v(0, 0.6, 0), 66)));

        // ============================================================ FIRTINA VATOZU
        s.add(new Shot("ray_wild", 240).fade(12, 0).music(0.45F, M_RAY)
                .chapter("FIRTINA VATOZU", "Şimşek kanatlı göklerin efendisi", 25, 120)
                .accent(25, S_THUNDER, 0.5F, 1.1F)
                .setup(d -> {
                    spectate(d);
                    d.server(l -> {
                        Vec3 a = I(d, -R(d) - 18, 8, 0), b = I(d, -R(d) - 26, 14, 12);
                        d.spawn("wray1", "stormray:storm_ray", a.x, a.y, a.z, 90F, null);
                        d.spawn("wray2", "stormray:storm_ray", b.x, b.y, b.z, 120F, null);
                    });
                }).settle(80)
                .cam((d, t, u) -> {
                    Vec3 r = d.pos("wray1", I(d, -R(d) - 18, 8, 0));
                    double a = Math.toRadians(20 + 90 * Cam.soft(u));
                    return Cam.look(r.add(Math.cos(a) * 9, 2.5, Math.sin(a) * 9), r.add(0, 0.3, 0), 0, 70);
                }));

        s.add(new Shot("ray_tame", 200).play()
                .setup(d -> {
                    double x = d.w.isl.cx() - R(d) * 0.55 + 0.5, z = d.w.isl.cz() + 3.5;
                    stand(d, x, z, 90F, 5F);
                    hotbar(d, "minecraft:cod", "minecraft:saddle", null);
                    d.select(0);
                    d.server(l -> {
                        Vec3 me = pt(d, "me");
                        d.spawn("ray", "stormray:storm_ray", me.x - 4.5, me.y + 1.2, me.z, -90F, e -> {
                            e.removeTag("film");
                            e.addTag("cast");
                            ((Mob) e).setNoAi(true);
                        });
                    });
                })
                .aim((d, t, u) -> aimKey(d, "ray", 0.25F))
                .at(20, d -> interact(d, "ray"))
                .at(45, d -> interact(d, "ray"))
                .at(62, d -> d.server(l -> {
                    if (d.se("ray") instanceof StormRayEntity r && !r.isTame()) {
                        r.tame(d.sp());
                        l.broadcastEntityEvent(r, (byte) 7);
                        l.playSound(null, r.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 1.4F);
                    }
                }))
                .at(95, d -> d.select(1))
                .at(105, d -> interact(d, "ray"))
                .at(140, d -> d.select(2))
                .at(150, d -> d.server(l -> {
                    if (d.se("ray") instanceof StormRayEntity r) {
                        r.setSaddled(true);
                        r.setNoAi(false);
                    }
                }))
                .at(152, d -> interact(d, "ray")));

        s.add(new Shot("ray_flight", 420).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> d.server(l -> {
                    ServerPlayer sp = d.sp();
                    if (!(sp.getVehicle() instanceof StormRayEntity)) {
                        Entity e = d.se("ray");
                        Vec3 p = e != null ? e.position() : I(d, -R(d) - 6, 6, 0);
                        ray(d, p, true);
                    }
                }))
                .at(0, d -> {
                    d.key(d.mc().options.keyUp, true);
                    d.key(d.mc().options.keySprint, true);
                })
                .aim((d, t, u) -> {
                    double dx = d.w.cx - d.w.isl.cx(), dz = d.w.cz - d.w.isl.cz();
                    float toCity = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float yaw = toCity - 25F + 50F * (float) Math.sin(u * Math.PI * 1.2);
                    float pitch = -4F + 5F * (float) Math.sin(u * Math.PI * 2);
                    return new float[]{yaw, pitch};
                })
                .cam((d, t, u) -> {
                    Vec3 r = d.pos("ray", d.me());
                    float yaw = d.lp().getYRot();
                    Vec3 fw = Cam.dir(yaw, 0), side = v(-fw.z, 0, fw.x);
                    Vec3 p = r.subtract(fw.scale(10)).add(0, 3.6, 0).add(side.scale(4 * Math.sin(u * Math.PI)));
                    return Cam.look(p, r.add(fw.scale(8)), 10F * (float) Math.cos(u * Math.PI * 1.2), 76);
                }));

        s.add(new Shot("ray_zap", 220).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> d.server(l -> {
                    double hx = d.w.isl.cx() + R(d) * 0.35, hz = d.w.isl.cz();
                    int hy = d.ground((int) hx, (int) hz);
                    d.w.pts.put("zap", v(hx, hy, hz));
                    for (int i = 0; i < 5; i++) {
                        double a = i * Math.PI * 2 / 5;
                        double x = hx + Math.cos(a) * 4, z = hz + Math.sin(a) * 4;
                        d.spawn("z" + i, "minecraft:husk", x, d.ground((int) Math.floor(x), (int) Math.floor(z)), z, 90F, null);
                    }
                    ray(d, v(hx - 12, hy + 6, hz), true);
                }))
                .aim((d, t, u) -> d.aimAt(pt(d, "zap").add(0, 1, 0), 0.2F))
                .cam((d, t, u) -> {
                    Vec3 r = d.pos("ray", d.me()), z = pt(d, "zap");
                    Vec3 mid = Cam.lerp(r, z, 0.5);
                    Vec3 dir = z.subtract(r).normalize();
                    Vec3 perp = v(-dir.z, 0, dir.x);
                    return Cam.look(mid.add(perp.scale(15 - 3 * u)).add(0, 4, 0), mid.add(0, 0.5, 0), 0, 70);
                }));

        // ============================================================ SYLVAREN
        s.add(new Shot("city_aerial", 300).fade(15, 0).music(0.5F, M_CITY)
                .chapter("SYLVAREN", "Elflerin kadim ormankenti", 25, 130)
                .accent(25, S_BELL, 0.5F, 0.7F).accent(25, S_CHIME, 0.7F, 1.2F)
                .setup(d -> {
                    d.server(l -> {
                        ServerPlayer sp = d.sp();
                        if (sp.isPassenger()) sp.stopRiding();
                        d.run("kill @e[tag=film]");
                        clearField(d);
                    });
                    spectate(d);
                    d.cmd("time set 4500");
                }).settle(80)
                .cam((d, t, u) -> Cam.orbit(C(d, 0, 18, -40), 135, 68, 60, 140, u, v(0, 0, 0), 70)));

        s.add(new Shot("city_walk", 260).play()
                .setup(d -> {
                    stand(d, d.w.cx + 0.5, d.w.cz + 42.5, 180F, 0F);
                    hotbar(d);
                    d.select(8);
                    ally(d, ElfKind.TAVI, "tavi_wave", d.w.cx + 4.5, d.w.cz + 22.5, false);
                    d.server(l -> {
                        if (elf(d, "tavi_wave") != null) {
                            elf(d, "tavi_wave").setActivity(Activity.WAVE, 100000);
                            elf(d, "tavi_wave").setYRot(0F);
                        }
                    });
                })
                .at(0, d -> d.key(d.mc().options.keyUp, true))
                .at(215, d -> d.key(d.mc().options.keyUp, false))
                .aim((d, t, u) -> new float[]{180F + 28F * (float) Math.sin(u * Math.PI * 2.2), -4F + 6F * (float) Math.sin(u * Math.PI * 3)}));

        s.add(new Shot("city_castle", 200)
                .setup(d -> spectate(d))
                .cam((d, t, u) -> Cam.move(C(d, 42, 14, -28), C(d, 28, 30, -55), C(d, 0, 30, -80), C(d, -10, 46, -96), u, 0, 0, 70)));

        s.add(new Shot("city_waterfall", 160)
                .cam((d, t, u) -> Cam.move(C(d, -30, 4, 2), C(d, -18, 7, -6), C(d, -20, 6, -26), C(d, -20, 7, -26), u, 0, 0, 68)));

        String[] names = {"Lirael · Kılıç Dansçısı", "Vaelor · Gölge Kılıcı", "Caelan · Bilge Yazıcı", "Nimrae · Ay Rahibesi",
                "Tavi · Genç Okçu", "Thalion · Korucu Komutanı"};
        ElfKind[] kinds = {ElfKind.LIRAEL, ElfKind.VAELOR, ElfKind.CAELAN, ElfKind.NIMRAE, ElfKind.TAVI, ElfKind.THALION};
        Activity[] poses = {Activity.PRACTICE, Activity.CROSS_ARMS, Activity.READ, Activity.MEDITATE, Activity.WAVE, Activity.KNEEL};
        Shot gallery = new Shot("elf_gallery", 270)
                .setup(d -> {
                    spectate(d);
                    for (int i = 0; i < 6; i++) ally(d, kinds[i], "elf" + i, d.w.fx - 7.5 + i * 3, d.w.fz - 26.5, false);
                    d.server(l -> {
                        for (int i = 0; i < 6; i++) {
                            ElfEntity e = elf(d, "elf" + i);
                            if (e == null) continue;
                            e.setNoAi(true);
                            e.setYRot(0F);
                            e.yBodyRot = 0F;
                            e.yHeadRot = 0F;
                            e.setActivity(poses[i], 100000);
                            if (i == 2) e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
                            if (i == 0) e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                        }
                    });
                }).settle(60)
                .cam((d, t, u) -> Cam.move(F(d, -11, 1.7, -21.5), F(d, 9, 1.9, -21.5), F(d, -8, 1.3, -26.5), F(d, 9, 1.3, -26.5), u, 0, 0, 62));
        for (int i = 0; i < 6; i++) gallery.title(names[i], "", 18 + i * 40, 42, 2, 0xF4E6C0);
        s.add(gallery);

        s.add(new Shot("dialogue", 270).play().screen()
                .setup(d -> {
                    stand(d, d.w.fx - 7.5 + 0.5, d.w.fz - 23.0, 180F, 8F);
                    d.server(l -> {
                        ElfEntity e = elf(d, "elf0");
                        if (e != null) e.setActivity(Activity.NONE, 0);
                    });
                })
                .aim((d, t, u) -> aimKey(d, "elf0", 0.2F))
                .at(10, d -> d.server(l -> {
                    if (elf(d, "elf0") != null) DialogueManager.open(d.sp(), elf(d, "elf0"));
                }))
                .at(150, d -> d.server(l -> {
                    if (elf(d, "elf0") != null) DialogueManager.choose(d.sp(), elf(d, "elf0").getId(), 3);
                }))
                .at(250, d -> d.server(l -> {
                    if (elf(d, "elf0") != null) DialogueManager.choose(d.sp(), elf(d, "elf0").getId(), 0);
                })));

        s.add(new Shot("hire", 200).play().screen()
                .at(5, d -> d.server(l -> {
                    ServerPlayer p = d.sp();
                    ElfEntity e = elf(d, "elf0");
                    if (e == null) return;
                    PlayerData.addFriendship(p, ElfKind.LIRAEL, 7);
                    PlayerData.finishQuest(p, ElfKind.LIRAEL);
                    p.getInventory().add(new ItemStack(Items.EMERALD, 64));
                    DialogueManager.open(p, e);
                }))
                .at(45, d -> d.server(l -> {
                    if (elf(d, "elf0") != null) DialogueManager.choose(d.sp(), elf(d, "elf0").getId(), 5);
                }))
                .at(175, d -> d.mc().setScreen(null))
                .aim((d, t, u) -> aimKey(d, "elf0", 0.2F)));

        s.add(new Shot("lirael_follow", 200).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> d.server(l -> {
                    for (int i = 0; i < 6; i++) {
                        ElfEntity e = elf(d, "elf" + i);
                        if (e == null) continue;
                        e.setNoAi(false);
                        e.setActivity(Activity.NONE, 0);
                        if (i == 0 || i == 4 || i == 5) {
                            if (!e.isHired()) e.hire(d.sp(), 20L * 60 * 60);
                        }
                    }
                }))
                .at(0, d -> d.key(d.mc().options.keyUp, true))
                .at(175, d -> d.key(d.mc().options.keyUp, false))
                .aim((d, t, u) -> new float[]{0F, 2F})
                .cam((d, t, u) -> {
                    Vec3 me = d.me();
                    return Cam.look(me.add(-6 + 2 * u, 2.0, 5.5), me.add(0, 1.2, -1.5), 0, 64);
                }));

        // ============================================================ MACERACI KUŞANIMI
        s.add(new Shot("gear_intro", 220).fade(12, 0).music(0.42F, M_GEAR).view(CameraType.THIRD_PERSON_BACK)
                .chapter("MACERACI KUŞANIMI", "Yüzükler, iksirler, büyü kitapları, anahtarlar", 20, 130)
                .accent(20, S_CHIME, 0.7F, 1.4F)
                .setup(d -> {
                    stand(d, d.w.fx + 0.5, d.w.fz + 0.5, 0F, 0F);
                    gear(d, "emerald_eye_ring", "skull_ring", "explorer_goggles", "alchemist_belt", "expedition_pack");
                    hotbar(d, "advgear:heart_tonic");
                    d.cmd("xp add @a 300 levels", "xp add @a 60000 points", "time set 5000");
                })
                .aim((d, t, u) -> new float[]{0F, 0F})
                .cam((d, t, u) -> Cam.orbit(pt(d, "me").add(0, 1.1, 0), 4.2, 0.6, 30, 200, u, v(0, 0.35, 0), 58)));

        s.add(new Shot("gear_screen", 110).play().screen()
                .at(5, d -> d.server(l -> AdvGear.openGear(d.sp())))
                .at(100, d -> d.server(l -> d.sp().closeContainer())));

        s.add(new Shot("tonic", 140).play()
                .setup(d -> {
                    d.server(l -> d.sp().setHealth(9F));
                    d.select(0);
                })
                .aim((d, t, u) -> new float[]{40F * (float) Math.sin(Math.min(t, 36) * 0.3F) * (1 - Math.min(1F, t / 40F)), 22F})
                .at(45, d -> use(d, true))
                .at(90, d -> use(d, false)));

        s.add(new Shot("poison", 160).play()
                .setup(d -> {
                    hotbar(d, "advgear:black_death");
                    d.select(0);
                    horde(d, 5, 16, 2.4, true);
                })
                .aim((d, t, u) -> d.aimAt(hordeCenter(d, 5, F(d, 0, 1, 16)).add(0, 2.5, 0), 0.25F))
                .at(40, d -> d.click(d.mc().options.keyUse))
                .at(100, d -> d.click(d.mc().options.keyUse)));

        s.add(new Shot("firebolt", 150).play()
                .setup(d -> {
                    hotbar(d, "advgear:apprentice_spellbook");
                    d.select(0);
                })
                .aim((d, t, u) -> aimEntity(d, nearest(d, 5, false), 0.3F))
                .at(20, d -> use(d, true)).at(38, d -> use(d, false))
                .at(80, d -> use(d, true)).at(98, d -> use(d, false)));

        s.add(new Shot("grimoire", 170).play()
                .setup(d -> {
                    killFilm(d);
                    hotbar(d, "advgear:black_grimoire");
                    d.select(0);
                    horde(d, 6, 14, 2.2, true);
                })
                .aim((d, t, u) -> d.aimAt(hordeCenter(d, 6, F(d, 0, 1, 14)), 0.25F))
                .at(25, d -> use(d, true)).at(55, d -> use(d, false)));

        s.add(new Shot("blink", 130).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> {
                    killFilm(d);
                    stand(d, d.w.fx + 0.5, d.w.fz + 0.5, 0F, 0F);
                    hotbar(d, "advgear:traveler_tome");
                    d.select(0);
                })
                .aim((d, t, u) -> new float[]{0F, -2F})
                .at(15, d -> use(d, true)).at(36, d -> use(d, false))
                .cam((d, t, u) -> Cam.look(pt(d, "me").add(9, 2.2, 5 + 3 * u), pt(d, "me").add(0, 1.2, 6), 0, 66)));

        s.add(new Shot("goggles", 160).play()
                .setup(d -> {
                    stand(d, d.w.fx + 0.5, d.w.fz + 0.5, 0F, 40F);
                    gear(d, "emerald_eye_ring", "skull_ring", "engineer_goggles", "alchemist_belt", "expedition_pack");
                    hotbar(d);
                    d.select(8);
                    d.server(l -> {
                        Vec3 me = pt(d, "me");
                        Block[] ores = {Blocks.DIAMOND_ORE, Blocks.GOLD_ORE, Blocks.EMERALD_ORE, Blocks.IRON_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE,
                                Blocks.DIAMOND_ORE, Blocks.COPPER_ORE};
                        for (int i = 0; i < ores.length; i++) {
                            BlockPos p = BlockPos.containing(me.x - 4 + (i * 3) % 9, me.y - 3 - (i % 4) * 2, me.z + 3 + i * 1.3);
                            l.setBlock(p, ores[i].defaultBlockState(), 3);
                        }
                    });
                })
                .aim((d, t, u) -> new float[]{-30F + 60F * Cam.soft(u), 42F - 10F * (float) Math.sin(u * Math.PI)}));

        s.add(new Shot("chests", 180).play().screen()
                .setup(d -> {
                    stand(d, d.w.fx + 0.5, d.w.fz + 0.5, 0F, 24F);
                    hotbar(d, "advgear:iron_key");
                    d.select(0);
                    d.server(l -> {
                        Vec3 me = pt(d, "me");
                        Block chest = ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("advgear", "locked_chest"));
                        if (chest == null) return;
                        LockType[] types = LockType.values();
                        for (int k = 0; k < 3; k++) {
                            BlockPos p = BlockPos.containing(me.x - 2 + k * 2, me.y, me.z + 3);
                            l.setBlock(p, chest.defaultBlockState().setValue(LockedChestBlock.FACING, Direction.NORTH).setValue(LockedChestBlock.LOCK, types[k]), 3);
                            d.w.pts.put("chest" + k, Vec3.atLowerCornerOf(p));
                        }
                    });
                })
                .aim((d, t, u) -> d.aimAt(pt(d, "chest0").add(0.5, 0.5, 0.1), 0.25F))
                .at(40, d -> {
                    Vec3 c = pt(d, "chest0");
                    BlockPos bp = BlockPos.containing(c.x, c.y, c.z);
                    d.mc().gameMode.useItemOn(d.lp(), InteractionHand.MAIN_HAND,
                            new BlockHitResult(new Vec3(c.x + 0.5, c.y + 0.5, c.z + 0.02), Direction.NORTH, bp, false));
                    d.lp().swing(InteractionHand.MAIN_HAND);
                })
                .at(130, d -> d.server(l -> d.sp().closeContainer())));

        // ============================================================ KILIÇLAR VADİSİ
        s.add(new Shot("vale_over", 280).fade(15, 0).music(0.5F, M_VALE)
                .chapter("KILIÇLAR VADİSİ", "Devlerin unutulmuş savaş alanı", 25, 130)
                .accent(25, S_BOOM, 0.35F, 0.5F).accent(25, S_BELL, 0.45F, 0.5F)
                .setup(d -> {
                    spectate(d);
                    d.cmd("time set 5200");
                }).settle(80)
                .cam((d, t, u) -> d.w.vale == null ? null
                        : Cam.orbit(V(d, 0, 0, 0), d.w.vale.radius() + 40, 78, 0, 65, u, v(0, 0, 0), 72)));

        s.add(new Shot("vale_giant", 220)
                .cam((d, t, u) -> {
                    SwordShape g = d.w.giant;
                    if (g == null) return null;
                    double[] a = g.axisPoint(g.s.length() * (0.04 + 0.86 * Cam.soft(u)));
                    double ang = g.s.twist() + Math.PI / 2;
                    Vec3 c = v(a[0], a[1], a[2]);
                    Vec3 p = c.add(Math.cos(ang) * 10, 1.5, Math.sin(ang) * 10);
                    return Cam.look(p, c.add(0, 4, 0), 0, 70);
                }));

        s.add(new Shot("vale_runes", 160)
                .setup(d -> d.cmd("time set 13300"))
                .cam((d, t, u) -> {
                    SwordShape g = d.w.giant;
                    if (g == null) return null;
                    double[] a = g.axisPoint(g.s.length() * 0.35);
                    double ang = g.s.twist() + Math.PI / 2 + 0.35 * (u - 0.5);
                    Vec3 c = v(a[0], a[1], a[2]);
                    return Cam.look(c.add(Math.cos(ang) * (7.5 - u), -1.2, Math.sin(ang) * (7.5 - u)), c, 0, 66);
                }));

        s.add(new Shot("vale_walk", 180).play()
                .setup(d -> {
                    d.cmd("time set 5200");
                    if (d.w.vale != null) stand(d, d.w.vale.cx() + 6.5, d.w.vale.cz() + 6.5, 150F, 0F);
                    hotbar(d, "swordvale:vale_compass");
                    d.select(0);
                })
                .at(0, d -> d.key(d.mc().options.keyUp, true))
                .at(165, d -> d.key(d.mc().options.keyUp, false))
                .aim((d, t, u) -> new float[]{150F + 35F * (float) Math.sin(u * Math.PI * 1.6), -14F + 10F * (float) Math.sin(u * Math.PI)}));

        // ============================================================ KUŞATMA
        s.add(new Shot("siege_horde", 260).fade(15, 0).music(0.55F, M_SIEGE)
                .chapter("KUŞATMA", "Karanlık, Sylvaren'in kapısına dayandı", 25, 120)
                .accent(25, S_THUNDER, 0.6F, 0.6F).accent(25, S_GROWL, 0.35F, 0.6F)
                .setup(d -> {
                    killFilm(d);
                    d.cmd("time set 12900");
                    stand(d, d.w.fx + 0.5, d.w.fz - 27.5, 0F, 0F);
                    gear(d, "emerald_eye_ring", "skull_ring", "hunter_goggles", "tool_belt", "traveler_pack");
                    hotbar(d, "emeraldblade:emerald_root_blade", "sylvanarchery:sylvan_bow", "quiver", "advgear:black_grimoire");
                    d.select(0);
                    horde(d, 14, 34, 2.3, false);
                }).settle(60)
                .cam((d, t, u) -> Cam.move(F(d, -7, 1.0, 50), F(d, 4, 2.6, 34), F(d, 0, 2, -20), F(d, 0, 3, -26), u, -3, 2, 70)));

        s.add(new Shot("blade_wave", 190).play()
                .setup(d -> d.select(0))
                .aim((d, t, u) -> d.aimAt(hordeCenter(d, 14, F(d, 0, 1, 20)).add(0, -0.8, 0), 0.25F))
                .at(15, d -> use(d, true)).at(52, d -> use(d, false))
                .at(100, d -> use(d, true)).at(137, d -> use(d, false)));

        s.add(new Shot("blade_cine", 140).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> {
                    killFilm(d);
                    horde(d, 10, 16, 2.0, false);
                    stand(d, d.w.fx + 0.5, d.w.fz - 6.5, 0F, 0F);
                })
                .aim((d, t, u) -> d.aimAt(hordeCenter(d, 10, F(d, 0, 1, 12)).add(0, -0.8, 0), 0.3F))
                .at(10, d -> use(d, true)).at(46, d -> use(d, false))
                .cam((d, t, u) -> {
                    Vec3 me = pt(d, "me");
                    return Cam.look(me.add(10 - 2 * u, 2.4, 2 + 4 * u), me.add(0, 1, 10), 0, 68);
                }));

        s.add(new Shot("blade_melee", 210).play()
                .aim((d, t, u) -> aimEntity(d, nearest(d, 10, false), 0.35F))
                .every((d, t) -> {
                    Entity n = nearest(d, 10, false);
                    boolean far = n != null && n.distanceTo(d.lp()) > 2.8F;
                    d.key(d.mc().options.keyUp, far);
                    if (t % 11 == 5 && n != null && !far) d.click(d.mc().options.keyAttack);
                }));

        s.add(new Shot("bow", 200).play()
                .setup(d -> {
                    killFilm(d);
                    horde(d, 8, 22, 3.2, false);
                    stand(d, d.w.fx + 0.5, d.w.fz - 10.5, 0F, 0F);
                    d.select(1);
                })
                .aim((d, t, u) -> aimEntity(d, nearest(d, 8, true), 0.3F))
                .every((d, t) -> {
                    int k = t % 34;
                    if (k == 6) use(d, true);
                    if (k == 26) use(d, false);
                }));

        s.add(new Shot("bow_volley", 150).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> d.select(1))
                .at(0, d -> d.key(d.mc().options.keyShift, true))
                .aim((d, t, u) -> d.aimAt(hordeCenter(d, 8, F(d, 0, 1, 14)).add(0, 0.6, 0), 0.3F))
                .at(10, d -> use(d, true)).at(32, d -> use(d, false))
                .at(70, d -> use(d, true)).at(92, d -> use(d, false))
                .at(140, d -> d.key(d.mc().options.keyShift, false))
                .cam((d, t, u) -> {
                    Vec3 me = d.me();
                    float yaw = d.lp().getYRot();
                    Vec3 fw = Cam.dir(yaw, 0), side = v(-fw.z, 0, fw.x);
                    return Cam.look(me.subtract(fw.scale(3.2)).add(side.scale(-1.3)).add(0, 2.1, 0), me.add(fw.scale(12)).add(0, 1.2, 0), 0, 70);
                }));

        s.add(new Shot("elves_fight", 210)
                .setup(d -> {
                    killFilm(d);
                    spectate(d);
                    d.server(l -> {
                        Entity li = d.se("elf0");
                        Vec3 c = li != null ? li.position() : F(d, 0, 0, 0);
                        d.w.pts.put("elfc", c);
                        for (int i = 0; i < 6; i++) {
                            double a = i * Math.PI / 3;
                            double x = c.x + Math.cos(a) * 7, z = c.z + Math.sin(a) * 7;
                            d.spawn("h" + i, i % 2 == 0 ? "minecraft:zombie" : "minecraft:vindicator", x, d.ground((int) Math.floor(x), (int) Math.floor(z)), z, 0F, e -> {
                                if (e instanceof Mob m && d.se("elf0") instanceof LivingEntity le) m.setTarget(le);
                            });
                        }
                    });
                })
                .cam((d, t, u) -> {
                    Vec3 c = d.pos("elf0", pt(d, "elfc"));
                    return Cam.orbit(c, 7.5, 2.4, 0, 120, u, v(0, 1.2, 0), 66);
                }));

        s.add(new Shot("ray_strike", 230).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> {
                    killFilm(d);
                    horde(d, 9, 8, 2.6, false);
                    ray(d, F(d, 0, 13, -14), true);
                })
                .aim((d, t, u) -> d.aimAt(hordeCenter(d, 9, F(d, 0, 0, 10)), 0.15F))
                .at(80, d -> d.key(d.mc().options.keyUp, true))
                .at(110, d -> d.key(d.mc().options.keyUp, false))
                .cam((d, t, u) -> {
                    Vec3 r = d.pos("ray", d.me());
                    Vec3 c = hordeCenter(d, 9, F(d, 0, 0, 10));
                    return Cam.look(r.add(-9 + 4 * u, 3, -7), Cam.lerp(r, c, 0.6), 0, 72);
                }));

        s.add(new Shot("core_shield", 260).view(CameraType.THIRD_PERSON_BACK)
                .accent(40, S_BEACON, 0.9F, 0.9F)
                .setup(d -> {
                    killFilm(d);
                    stand(d, d.w.fx + 0.5, d.w.fz - 14.5, 0F, 0F);
                    d.server(l -> {
                        BlockPos sh = BlockPos.containing(d.w.fx, d.ground(d.w.fx, d.w.fz - 10) - 1, d.w.fz - 10);
                        ShrineFeature.build(l, sh, l.random, false);
                        BlockPos core = sh.above();
                        if (!(l.getBlockEntity(core) instanceof AetherCoreBlockEntity)) l.setBlock(core, AetherCore.AETHER_CORE.get().defaultBlockState(), 3);
                        d.w.pts.put("core", Vec3.atCenterOf(core));
                        if (l.getBlockEntity(core) instanceof AetherCoreBlockEntity be) be.addEnergy(20 * 60 * 30);
                    });
                    horde(d, 12, 22, 2.4, false);
                })
                .at(40, d -> d.server(l -> {
                    if (l.getBlockEntity(BlockPos.containing(pt(d, "core"))) instanceof AetherCoreBlockEntity be) be.setActive(true);
                }))
                .aim((d, t, u) -> new float[]{0F, -5F})
                .cam((d, t, u) -> Cam.move(pt(d, "core").add(20, 9, 26), pt(d, "core").add(-14, 6, 24), pt(d, "core").add(0, 2, 4),
                        pt(d, "core").add(0, 3, 0), u, 0, 0, 72)));

        // ============================================================ FİNAL: BOSSLAR
        s.add(new Shot("boss_arrive", 260).fade(15, 0).music(0.6F, M_BOSS).hud(1)
                .title("REVENGE", "İntikamın Efendisi", 95, 120, 1, RED)
                .accent(60, S_GROWL, 0.5F, 0.6F)
                .setup(d -> {
                    killFilm(d);
                    d.server(l -> {
                        Vec3 c = pt(d, "core");
                        if (l.getBlockEntity(BlockPos.containing(c)) instanceof AetherCoreBlockEntity be) be.setActive(false);
                    });
                    d.cmd("time set 12700", "weather clear 1000000");
                    stand(d, d.w.fx + 0.5, d.w.fz - 4.5, 0F, -6F);
                    hotbar(d, "emeraldblade:emerald_root_blade", "sylvanarchery:sylvan_bow", "quiver", "advgear:black_grimoire");
                    d.select(0);
                    ally(d, ElfKind.THALION, "thalion", d.w.fx - 5.5, d.w.fz - 8.5, true);
                }).settle(60)
                .at(20, d -> lightning(d, F(d, 4, 0, 13)))
                .at(36, d -> lightning(d, F(d, -4, 0, 15)))
                .at(52, d -> lightning(d, F(d, 0, 0, 14)))
                .at(55, d -> d.server(l -> d.spawn("revenge", "krolasyonbosses:revenge", d.w.fx + 0.5, d.ground(d.w.fx, d.w.fz + 14), d.w.fz + 14.5, 180F, null)))
                .cam((d, t, u) -> Cam.move(F(d, 6, 1.8, 0), F(d, 3, 2.2, 5), F(d, 0, 2.6, 14), F(d, 0, 2.4, 14), u, -4, 0, 70)));

        s.add(new Shot("boss_wave", 190).play()
                .setup(d -> d.select(0))
                .aim((d, t, u) -> aimKey(d, "revenge", 0.3F))
                .at(15, d -> use(d, true)).at(52, d -> use(d, false))
                .every((d, t) -> {
                    if (t > 80 && t % 13 == 0) {
                        Entity b = d.ce("revenge");
                        if (b != null && b.distanceTo(d.lp()) < 4.5F) d.click(d.mc().options.keyAttack);
                    }
                }));

        s.add(new Shot("boss_ability", 170).hud(1).view(CameraType.THIRD_PERSON_BACK)
                .at(10, d -> force(d, "revenge", 1, null))
                .at(90, d -> force(d, "revenge", 2, null))
                .cam((d, t, u) -> Cam.orbit(d.pos("revenge", F(d, 0, 0, 14)), 13, 4, 200, 290, u, v(0, 2.5, 0), 70)));

        s.add(new Shot("boss_storm", 160).play()
                .setup(d -> d.select(3))
                .aim((d, t, u) -> aimKey(d, "revenge", 0.3F))
                .at(15, d -> use(d, true)).at(46, d -> use(d, false))
                .at(70, d -> force(d, "revenge", 3, null)));

        s.add(new Shot("boss_elves", 190).hud(1).view(CameraType.THIRD_PERSON_BACK)
                .setup(d -> d.server(l -> {
                    Entity b = d.se("revenge");
                    if (!(b instanceof LivingEntity lb)) return;
                    for (String k : new String[]{"elf0", "elf4", "elf5", "thalion"}) {
                        if (d.se(k) instanceof Mob m) {
                            m.setTarget(lb);
                            Vec3 p = lb.position().add((k.hashCode() % 5) - 2, 0, -5);
                            m.teleportTo(p.x, d.ground((int) Math.floor(p.x), (int) Math.floor(p.z)), p.z);
                        }
                    }
                }))
                .at(60, d -> force(d, "revenge", 0, "elf0"))
                .cam((d, t, u) -> {
                    Vec3 b = d.pos("revenge", F(d, 0, 0, 14)), e = b.add(0, 0, -6);
                    Vec3 mid = Cam.lerp(b, e, 0.5);
                    Vec3 dir = b.subtract(e);
                    Vec3 perp = v(-dir.z, 0, dir.x).normalize();
                    return Cam.look(mid.add(perp.scale(12)).add(0, 3.5, 0).add(dir.normalize().scale(-3 + 6 * u)), mid.add(0, 1.8, 0), 0, 70);
                }));

        s.add(new Shot("demon_arrive", 220).hud(1)
                .title("KALP İBLİSİ", "Kanla beslenen kadim iblis", 45, 120, 1, RED)
                .accent(25, S_BOOM, 0.6F, 0.6F)
                .at(15, d -> lightning(d, F(d, 8, 0, 18)))
                .at(25, d -> d.server(l -> {
                    d.spawn("demon", "krolasyonbosses:heart_demon", d.w.fx + 8.5, d.ground(d.w.fx + 8, d.w.fz + 18), d.w.fz + 18.5, 160F, null);
                    l.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, d.w.fx + 8.5, d.w.fy + 1, d.w.fz + 18.5, 2, 1, 1, 1, 0);
                }))
                .at(110, d -> force(d, "demon", 0, null))
                .cam((d, t, u) -> {
                    Vec3 b = d.pos("demon", F(d, 8, 0, 18));
                    return Cam.look(b.add(-6 + 3 * u, 2.0, -9 + 2 * u), b.add(0, 2.4, 0), 3 - 3 * u, 70);
                }));

        s.add(new Shot("boss_volley", 190).play()
                .setup(d -> {
                    d.select(1);
                    stand(d, d.w.fx + 0.5, d.w.fz - 4.5, 0F, 0F);
                })
                .at(0, d -> d.key(d.mc().options.keyShift, true))
                .aim((d, t, u) -> aimKey(d, "demon", 0.3F))
                .every((d, t) -> {
                    int k = t % 40;
                    if (k == 6) use(d, true);
                    if (k == 28) use(d, false);
                })
                .at(180, d -> d.key(d.mc().options.keyShift, false)));

        s.add(new Shot("boss_ray", 230).view(CameraType.THIRD_PERSON_BACK).hud(1)
                .setup(d -> ray(d, F(d, -4, 11, 2), true))
                .aim((d, t, u) -> aimKey(d, u < 0.5F ? "revenge" : "demon", 0.12F))
                .at(60, d -> d.key(d.mc().options.keyUp, true))
                .at(90, d -> d.key(d.mc().options.keyUp, false))
                .at(150, d -> d.key(d.mc().options.keyUp, true))
                .at(170, d -> d.key(d.mc().options.keyUp, false))
                .cam((d, t, u) -> {
                    Vec3 r = d.pos("ray", d.me());
                    float yaw = d.lp().getYRot();
                    Vec3 fw = Cam.dir(yaw, 0), side = v(-fw.z, 0, fw.x);
                    return Cam.look(r.subtract(fw.scale(8)).add(side.scale(3)).add(0, 3.2, 0), r.add(fw.scale(10)).add(0, -3, 0), -6, 74);
                }));

        s.add(new Shot("boss_chaos", 230).hud(1)
                .setup(d -> {
                    stand(d, d.w.fx + 0.5, d.w.fz - 4.5, 0F, 0F);
                    spectate(d);
                })
                .at(15, d -> force(d, "revenge", 4, "thalion"))
                .at(55, d -> force(d, "demon", 4, "elf0"))
                .at(110, d -> force(d, "revenge", 5, "elf4"))
                .at(160, d -> force(d, "demon", 5, "thalion"))
                .cam((d, t, u) -> {
                    Vec3 a = d.pos("revenge", F(d, 0, 0, 14)), b = d.pos("demon", F(d, 8, 0, 18));
                    return Cam.orbit(Cam.lerp(a, b, 0.5), Math.max(12, a.distanceTo(b) * 0.7 + 8), 6, 160, 260, u, v(0, 2, 0), 72);
                }));

        s.add(new Shot("boss_finish", 250).play().hud(2)
                .setup(d -> {
                    stand(d, d.w.fx + 0.5, d.w.fz + 8.5, 0F, 0F);
                    d.select(0);
                })
                .aim((d, t, u) -> aimKey(d, t < 175 ? "revenge" : "demon", 0.3F))
                .every((d, t) -> {
                    Entity b = d.ce(t < 175 ? "revenge" : "demon");
                    if (b == null || !b.isAlive()) {
                        d.key(d.mc().options.keyUp, false);
                        return;
                    }
                    boolean far = b.distanceTo(d.lp()) > 3.6F;
                    d.key(d.mc().options.keyUp, far && t < 165);
                    if (!far && t % 8 == 0) d.click(d.mc().options.keyAttack);
                })
                .at(140, d -> d.bossMortal = true)
                .at(150, d -> d.server(l -> {
                    if (boss(d, "revenge") != null) boss(d, "revenge").kill();
                })));

        s.add(new Shot("demon_death", 160).hud(1).view(CameraType.THIRD_PERSON_BACK)
                .accent(25, S_BOOM, 0.7F, 0.45F).accent(25, S_THUNDER, 0.5F, 0.8F)
                .at(25, d -> d.server(l -> {
                    if (boss(d, "demon") != null) boss(d, "demon").kill();
                }))
                .cam((d, t, u) -> Cam.orbit(d.pos("demon", F(d, 8, 0, 18)), 11, 3.0, 90, 160, u, v(0, 2.2, 0), 66)));

        s.add(new Shot("victory", 240).play().hud(2)
                .title("ZAFER!", "Sylvaren kurtuldu", 30, 140, 1, GOLD)
                .accent(30, S_BELL, 0.6F, 1.0F)
                .setup(d -> {
                    killFilm(d);
                    d.cmd("weather clear 1000000", "time set 22800");
                    stand(d, d.w.fx + 0.5, d.w.fz - 8.5, 0F, -30F);
                    d.select(8);
                    d.server(l -> {
                        for (String k : new String[]{"elf0", "elf4", "elf5", "thalion"}) {
                            if (d.se(k) instanceof ElfEntity e) {
                                e.setTarget(null);
                                e.setActivity(Activity.WAVE, 400);
                            }
                        }
                    });
                })
                .every((d, t) -> d.server(l -> l.setDayTime(l.getDayTime() + 4)))
                .at(20, d -> firework(d, F(d, -6, 0, 0), 16766720, 5635925, 1))
                .at(40, d -> firework(d, F(d, 6, 0, 2), 11141290, 16733525, 4))
                .at(60, d -> firework(d, F(d, 0, 0, 6), 5636095, 16777045, 1))
                .at(85, d -> firework(d, F(d, -9, 0, 4), 16733695, 5635925, 2))
                .at(110, d -> firework(d, F(d, 8, 0, -2), 16766720, 16777215, 1))
                .at(135, d -> firework(d, F(d, -3, 0, 8), 5635925, 16766720, 4))
                .aim((d, t, u) -> new float[]{-20F + 40F * Cam.soft(u), -38F + 8F * (float) Math.sin(u * Math.PI)}));

        // ============================================================ KAPANIŞ
        String[] credits = {"Elf Diyarı: Sylvaren", "Gök Adaları", "Fırtına Vatozu", "Maceracı Kuşanımı", "Zümrüt Kök Kılıcı",
                "Orman Bekçisi Okçuluk Seti", "Kılıçlar Vadisi", "Eterik Çekirdek", "Krolasyon Bossları"};
        Shot outro = new Shot("outro_fly", 380).fade(15, 0).music(0.55F, M_OUTRO)
                .setup(d -> {
                    spectate(d);
                    d.cmd("time set 23400");
                })
                .every((d, t) -> d.server(l -> l.setDayTime(l.getDayTime() + 3)))
                .cam((d, t, u) -> {
                    List<Vec3> pts = List.of(C(d, 0, 30, 150), C(d, 0, 38, 70), C(d, 0, 52, -25), C(d, 0, 70, -110), C(d, 0, 90, -190));
                    Vec3 p = Cam.spline(pts, Cam.soft(u));
                    Vec3 ahead = Cam.spline(pts, Math.min(1F, Cam.soft(u) + 0.12F));
                    return Cam.look(p, ahead.add(0, -45 - 40 * u, 0), 0, 74);
                });
        for (int i = 0; i < credits.length; i++) outro.title(credits[i], "", 30 + i * 38, 40, 2, 0xF4E6C0);
        s.add(outro);

        s.add(new Shot("outro_logo", 200).fade(0, 70)
                .title("KROLASYON", "Macera seni bekliyor...", 20, 175, 3, GOLD)
                .accent(20, S_BOOM, 0.4F, 0.5F).accent(20, S_BEACON, 0.6F, 0.7F)
                .every((d, t) -> d.server(l -> l.setDayTime(l.getDayTime() + 3)))
                .cam((d, t, u) -> Cam.move(C(d, 70, 75, 95), C(d, 95, 105, 125), C(d, 0, 20, -40), C(d, 0, 25, -50), u, 0, 0, 72)));

        return s;
    }
}
