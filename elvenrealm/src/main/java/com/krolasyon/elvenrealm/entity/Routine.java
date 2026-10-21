package com.krolasyon.elvenrealm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sylvaren sakinlerinin günlük programı: her karakterin saate göre gittiği yer, oradaki işi ve yürüyüş hızı.
 * Konumlar kent merkezine göreli (x, teras, z); teras 0 = aşağı avlu, 1 = orta teras, 2 = kale terası, 3 = ön sur.
 */
public final class Routine {
    private Routine() {}

    /**
     * Bir görev: varılacak nokta (ya da devriye yolu), bakış, varınca yapılacak iş.
     * key ayrıca "duty_" + key replik anahtarıdır.
     */
    public record Task(String key, Vec3 pos, float yaw, @Nullable Vec3 look, Activity act, double speed,
                       @Nullable List<Vec3> patrol, boolean snap, boolean escort) {}

    public static final int A = 0, B = 1, C = 2, WALL = 3;

    /** Muhafız nöbet yerleri (eski kentlerde muhafızın evine en yakın olan seçilir). */
    public static final Map<String, int[]> GUARD_POSTS = Map.of(
            "gate_l", new int[]{-4, A, 28}, "gate_r", new int[]{4, A, 28},
            "terrace", new int[]{7, B, -27}, "castle_l", new int[]{-3, C, -60}, "castle_r", new int[]{3, C, -60},
            "royal_l", new int[]{-3, C, -76}, "royal_r", new int[]{3, C, -76},
            "wall_l", new int[]{-30, WALL, 31}, "wall_r", new int[]{30, WALL, 31}, "patrol_a", new int[]{0, A, 20});

    /** Kentteki mutlak duruş noktası (blok ortası, ayak yüksekliği). */
    public static Vec3 at(BlockPos city, double x, int terrace, double z) {
        int y = terrace == WALL ? city.getY() + 10 : city.getY() + 8 * terrace + 1;
        return new Vec3(city.getX() + x + 0.5, y, city.getZ() + z + 0.5);
    }

    public static String nearestGuardPost(BlockPos city, BlockPos home) {
        String best = "gate_l";
        double bd = Double.MAX_VALUE;
        for (var e : GUARD_POSTS.entrySet()) {
            int[] p = e.getValue();
            double d = at(city, p[0], p[1], p[2]).distanceToSqr(Vec3.atCenterOf(home));
            if (d < bd) {
                bd = d;
                best = e.getKey();
            }
        }
        return best;
    }

    private static Task stay(String key, Vec3 pos, float yaw, Activity act, double speed) {
        return new Task(key, pos, yaw, null, act, speed, null, false, false);
    }

    private static Task face(String key, Vec3 pos, Vec3 look, Activity act, double speed) {
        return new Task(key, pos, 0, look, act, speed, null, false, false);
    }

    private static Task patrol(String key, double speed, Vec3... pts) {
        return new Task(key, pts[0], 0, null, Activity.STAND, speed, List.of(pts), false, false);
    }

    /** Gün içi saat (0 = gün doğumu) ile görev. */
    public static Task of(ElfEntity e, BlockPos c, String post, long dayTime) {
        int t = (int) (dayTime % 24000L);
        if (t < 0) t += 24000;
        boolean night = t >= 13000 && t < 23000;
        return switch (e.kind) {
            case AERENDIL -> lord(c, t);
            case THALION -> {
                if (t < 2500) yield stay("gate", at(c, 0, A, 25), 0F, Activity.CROSS_ARMS, 0.6);
                if (t < 6000) yield face("train", at(c, 22, A, 15), at(c, 22, A, 8), Activity.CROSS_ARMS, 0.6);
                if (t < 8500) yield patrol("patrol", 0.55, at(c, 0, A, 22), at(c, -24, A, 10), at(c, 0, A, -10), at(c, 0, B, -34),
                        at(c, 22, B, -33), at(c, 0, B, -34), at(c, 0, A, -10), at(c, 24, A, 14));
                if (t < 11000) yield face("council", at(c, -4, C, -74), at(c, 0, C, -78), Activity.CROSS_ARMS, 0.6);
                if (t < 13000) yield stay("gate", at(c, 0, A, 25), 0F, Activity.CROSS_ARMS, 0.6);
                yield stay("night", at(c, 0, A, 25), 0F, Activity.STAND, 0.6);
            }
            case LIRAEL -> {
                if (t < 3500) yield face("train", at(c, 22, A, 8), at(c, 25, A, 6).add(0, 1, 0), Activity.PRACTICE, 0.8);
                if (t < 6500) yield stay("fountain", at(c, 0, A, 14).add(0, 0.5, 0), 0F, Activity.SIT, 0.8);
                if (t < 9000) yield patrol("market", 0.75, at(c, -24, A, 12), at(c, -28, A, -6), at(c, -10, A, 2),
                        at(c, 10, A, 2), at(c, 28, A, -6), at(c, 30, A, 14));
                if (t < 12000) yield face("train", at(c, 22, A, 8), at(c, 25, A, 6).add(0, 1, 0), Activity.PRACTICE, 0.8);
                if (t < 13500) yield stay("fountain", at(c, 0, A, 14).add(0, 0.5, 0), 0F, Activity.SIT, 0.8);
                yield home(e, "rest", Activity.SIT, 0.7);
            }
            case VAELOR -> {
                if (night) yield patrol("prowl", 0.55, at(c, -11, A, 25), at(c, -30, A, 10), at(c, -30, A, -12), at(c, -12, A, -18),
                        at(c, 12, A, -18), at(c, 30, A, -12), at(c, 30, A, 10), at(c, 11, A, 25));
                if (t >= 6000 && t < 9000) yield face("critic", at(c, 33, B, -36), at(c, 24, B, -35), Activity.CROSS_ARMS, 0.6);
                yield stay("lurk", at(c, -11, A, 25), 0F, Activity.CROSS_ARMS, 0.6);
            }
            case CAELAN -> {
                if (t < 8500) yield stay("read", at(c, -31, B, -41), 180F, Activity.READ, 0.5);
                if (t < 11000) yield face("council", at(c, 4, C, -74), at(c, 0, C, -78), Activity.CROSS_ARMS, 0.5);
                if (t < 13000) yield stay("pond", at(c, -43, C, -76), 90F, Activity.READ, 0.5);
                yield stay("rest", at(c, -26, B, -41), 180F, Activity.SIT, 0.5);
            }
            case NIMRAE -> {
                if (t >= 1500 && t < 3500) yield face("bless", at(c, 0, A, 1), at(c, 0, A, 8).add(0, 1, 0), Activity.PRAY, 0.45);
                if (t >= 3500 && t < 5200) yield face("pond", at(c, -32, C, -69), at(c, -32, C, -76), Activity.KNEEL, 0.45);
                yield face("pray", at(c, 34, C, -76), at(c, 34, C, -70), night ? Activity.PRAY : Activity.MEDITATE, 0.45);
            }
            case TAVI -> {
                if (night) yield home(e, "rest", Activity.SIT, 0.8);
                if (t >= 6000 && t < 8000) yield face("birds", at(c, -32, C, -68), at(c, -32, C, -74), Activity.KNEEL, 0.85);
                if (t >= 11000) yield stay("fountain", at(c, -5, A, 11).add(0, 0.5, 0), -60F, Activity.SIT, 0.85);
                yield stay("archery", at(c, 24, B, -35), 180F, Activity.PRACTICE, 0.85);
            }
            case GUARD -> guard(e, c, post, night);
        };
    }

    private static Task home(ElfEntity e, String key, Activity act, double speed) {
        BlockPos h = e.home();
        Vec3 p = h == null ? e.position() : Vec3.atBottomCenterOf(h);
        return stay(key, p, e.getYRot(), act, speed);
    }

    private static Task lord(BlockPos c, int t) {
        Vec3 throne = at(c, 0, C, -78).add(0, 0.5, 0);
        if (t < 4500) return new Task("throne", throne, 0F, null, Activity.SIT, 0.4, null, true, false);
        if (t < 5200) return face("walk", at(c, -38, C, -76), at(c, -32, C, -76), Activity.CROSS_ARMS, 0.4);
        if (t < 5900) return face("shrine", at(c, 34, C, -71), at(c, 34, C, -76), Activity.PRAY, 0.4);
        if (t < 6500) return face("walk", at(c, 18, C, -60), at(c, 18, A, 0), Activity.CROSS_ARMS, 0.4);
        if (t < 11000) return new Task("throne", throne, 0F, null, Activity.SIT, 0.4, null, true, false);
        if (t < 12600) return stay("balcony", at(c, 0, C, -59), 0F, Activity.CROSS_ARMS, 0.4);
        return new Task("throne", throne, 0F, null, Activity.SIT, 0.4, null, true, false);
    }

    private static Task guard(ElfEntity e, BlockPos c, String post, boolean night) {
        int[] p = GUARD_POSTS.get(post);
        if (p == null) return home(e, "post", Activity.STAND, 0.55);
        Vec3 spot = at(c, p[0], p[1], p[2]);
        return switch (post) {
            case "wall_l" -> patrol("wall", 0.42, at(c, -15, WALL, 31), at(c, -48, WALL, 31));
            case "wall_r" -> patrol("wall", 0.42, at(c, 15, WALL, 31), at(c, 48, WALL, 31));
            case "patrol_a" -> patrol("patrol", 0.5, at(c, 0, A, 20), at(c, -24, A, 12), at(c, -28, A, -6), at(c, -10, A, -18),
                    at(c, 10, A, -18), at(c, 28, A, -6), at(c, 30, A, 14));
            case "royal_l", "royal_r" -> new Task("royal", spot, 0F, null, Activity.STAND, 0.6, null, false, true);
            default -> stay("post", spot, 0F, Activity.STAND, 0.55);
        };
    }

    /** Teras kodu (göreli konumdan). */
    public static int terrace(BlockPos c, Vec3 p) {
        double rz = p.z - c.getZ(), ry = p.y - c.getY();
        if (ry > 6.5 && rz > 28 && rz < 34) return WALL;
        if (rz > -24.5) return A;
        if (rz > -56.5) return B;
        return C;
    }

    /**
     * Bir noktadan diğerine ara duraklar: teras değişiyorsa merdivenlerden geçer, uzun düz yolları 18 blokluk parçalara böler
     * (yol bulucu yaklaşık 28 blok ötesini aramaz).
     */
    public static List<Vec3> hops(BlockPos c, Vec3 from, Vec3 to) {
        List<Vec3> pts = new ArrayList<>();
        int tf = terrace(c, from), tt = terrace(c, to);
        if (tf != WALL && tt != WALL) {
            while (tf != tt) {
                if (tt > tf) {
                    if (tf == A) {
                        pts.add(at(c, 0, A, -12));
                        pts.add(at(c, 0, B, -28));
                    } else {
                        pts.add(at(c, 0, B, -44));
                        pts.add(at(c, 0, C, -60));
                    }
                    tf++;
                } else {
                    if (tf == C) {
                        pts.add(at(c, 0, C, -60));
                        pts.add(at(c, 0, B, -44));
                    } else {
                        pts.add(at(c, 0, B, -28));
                        pts.add(at(c, 0, A, -12));
                    }
                    tf--;
                }
            }
        }
        pts.add(to);
        List<Vec3> out = new ArrayList<>();
        Vec3 cur = from;
        for (Vec3 p : pts) {
            double d = Math.hypot(p.x - cur.x, p.z - cur.z);
            int n = (int) Math.ceil(d / 18.0);
            for (int i = 1; i < n; i++) out.add(cur.lerp(p, i / (double) n));
            out.add(p);
            cur = p;
        }
        return out;
    }
}
