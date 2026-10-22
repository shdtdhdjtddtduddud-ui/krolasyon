package com.krolasyon.smoothterrain.test;

import com.krolasyon.smoothterrain.SmoothTerrain;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** CI-only (-Dsmoothterrain.autotest=true): tepelik ve dağlık bir yer bulur, yarım blokları sayar, görüntüler ve yokuşu zıplamadan yürür. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick, stepIndex, stepStart = -1;
    private boolean worldRequested, gui;
    private volatile boolean pending;
    private boolean submitted;
    private int hx, hz, mx, mz;
    private double walkY0;
    private BlockPos walkFrom, walkSlab;
    private float walkYaw;

    public static void init() {
        LOG.info("[SMOOTHTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static boolean ours(BlockState s) {
        return s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.BOTTOM || s.is(Blocks.SNOW) && s.getValue(
                net.minecraft.world.level.block.SnowLayerBlock.LAYERS) == 4;
    }

    private AutoTest() {
        server(0, null, s -> {
            run(s, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 5000",
                    "weather clear", "gamemode spectator @a");
            ServerLevel l = s.overworld();
            var gen = l.getChunkSource().getGenerator();
            var rs = l.getChunkSource().randomState();
            double bestScore = 1e9;
            int bestTop = -1000;
            for (int ci = -10; ci <= 10; ci++) {
                for (int cj = -10; cj <= 10; cj++) {
                    int cx = ci * 96, cz = cj * 96;
                    int[][] h = new int[6][6];
                    int min = 999, max = -999;
                    for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) {
                        h[i][j] = gen.getBaseHeight(cx + (i - 3) * 4, cz + (j - 3) * 4, Heightmap.Types.OCEAN_FLOOR_WG, l, rs);
                        min = Math.min(min, h[i][j]);
                        max = Math.max(max, h[i][j]);
                    }
                    int sea = gen.getSeaLevel();
                    int c = gen.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, l, rs);
                    if (c > bestTop && c < 200) {
                        bestTop = c;
                        mx = cx;
                        mz = cz;
                    }
                    if (min <= sea + 2 || max > 110) continue;
                    double diff = 0;
                    for (int i = 0; i < 5; i++) for (int j = 0; j < 6; j++) diff += Math.abs(h[i + 1][j] - h[i][j]) + Math.abs(h[j][i + 1] - h[j][i]);
                    diff /= 60.0;
                    double score = Math.abs(diff - 1.8);
                    if (score < bestScore) {
                        bestScore = score;
                        hx = cx;
                        hz = cz;
                    }
                }
            }
            LOG.info("[SMOOTHTEST] hills at {} {} (score {}) mountain at {} {} top {}", hx, hz, bestScore, mx, mz, bestTop);
            look(s, hx - 30, top(l, hx, hz) + 26, hz - 30, hx, top(l, hx, hz), hz);
        });
        wait(700, "hills_aerial");
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            stats(l, hx, hz);
            int y = top(l, hx + 12, hz + 12);
            look(s, hx + 12, y + 2.2, hz + 12, hx - 6, top(l, hx - 6, hz - 6) + 1, hz - 6);
        });
        wait(80, "hills_ground");
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            int y = top(l, hx - 14, hz + 10);
            look(s, hx - 14, y + 2.2, hz + 10, hx + 8, top(l, hx + 8, hz - 6) + 1, hz - 6);
        });
        wait(60, "hills_ground_b");
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            look(s, hx + 25, top(l, hx, hz) + 12, hz, hx, top(l, hx, hz), hz);
        });
        wait(60, "hills_side");
        // ---------------------------------------------------- yokuşu zıplamadan yürü
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            find(l);
            if (walkFrom == null) {
                LOG.info("[SMOOTHTEST] no walk spot");
                return;
            }
            run(s, "gamemode survival @a", "effect give @a minecraft:resistance infinite 4 true");
            ServerPlayer p = player(s);
            p.teleportTo(l, walkFrom.getX() + 0.5, walkFrom.getY(), walkFrom.getZ() + 0.5, walkYaw, 20);
            LOG.info("[SMOOTHTEST] walk from {} over slab {} ({}) yaw {}", walkFrom, walkSlab, l.getBlockState(walkSlab), walkYaw);
        });
        client(20, null, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            walkY0 = mc.player.getY();
            mc.player.setYRot(walkYaw);
        });
        client(1, null, mc -> KeyMapping.set(mc.options.keyUp.getKey(), true));
        wait(8, "walk_a");
        wait(8, "walk_b");
        wait(14, "walk_c");
        client(1, null, mc -> {
            KeyMapping.set(mc.options.keyUp.getKey(), false);
            LOG.info("[SMOOTHTEST] walked: y {} -> {} (gain {})", walkY0, mc.player.getY(), mc.player.getY() - walkY0);
        });
        client(5, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        // ---------------------------------------------------- dağ
        server(5, null, s -> {
            run(s, "gamemode spectator @a");
            ServerLevel l = s.overworld();
            look(s, mx - 40, top(l, mx, mz) + 30, mz - 40, mx, top(l, mx, mz), mz);
        });
        wait(700, "mountain_aerial");
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            stats(l, mx, mz);
            int y = top(l, mx + 10, mz + 10);
            look(s, mx + 10, y + 2.2, mz + 10, mx - 6, top(l, mx - 6, mz - 6) + 1, mz - 6);
        });
        wait(80, "mountain_ground");
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            look(s, mx + 30, top(l, mx, mz) + 8, mz + 5, mx, top(l, mx, mz) - 4, mz);
        });
        wait(60, "mountain_side");
        wait(10, "END");
    }

    private static int top(ServerLevel l, int x, int z) {
        return l.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, l, l.getChunkSource().randomState());
    }

    private static void stats(ServerLevel l, int cx, int cz) {
        Map<String, Integer> counts = new HashMap<>();
        int steps = 0, smoothed = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = cx - 40; x <= cx + 40; x++) {
            for (int z = cz - 40; z <= cz + 40; z++) {
                int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                p.set(x, y, z);
                BlockState s = l.getBlockState(p);
                if (ours(s)) counts.merge(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock()).toString(), 1, Integer::sum);
                int n = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + 1, z) - 1;
                BlockState ns = l.getBlockState(new BlockPos(x + 1, n, z));
                // tam blok yüksekliği (yarım blok varsa yarım)
                double a = y + (ours(s) ? 0.5 : 1), b = n + (ours(ns) ? 0.5 : 1);
                if (Math.abs(Math.round(a) - Math.round(b)) >= 1 || Math.abs(a - b) >= 0.5) {
                    if (Math.abs(a - b) <= 1.0) steps++;
                    if (Math.abs(a - b) == 0.5) smoothed++;
                }
            }
        }
        LOG.info("[SMOOTHTEST] around {} {}: slabs {} ; neighbour steps <=1 block {} of which half-block {}", cx, cz, counts, steps, smoothed);
    }

    /** Çimen yarım bloğunun önünde düz bir sütun ve arkasında bir blok yüksek sütun ara. */
    private void find(ServerLevel l) {
        int[][] dirs = {{0, 1, 0}, {0, -1, 180}, {1, 0, -90}, {-1, 0, 90}};
        for (int r = 0; r < 40 && walkFrom == null; r++) {
            for (int x = hx - r; x <= hx + r && walkFrom == null; x++) {
                for (int z = hz - r; z <= hz + r && walkFrom == null; z++) {
                    if (Math.max(Math.abs(x - hx), Math.abs(z - hz)) != r) continue;
                    int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    BlockPos s = new BlockPos(x, y, z);
                    if (!ours(l.getBlockState(s)) || l.getBlockState(s).is(Blocks.SNOW)) continue;
                    for (int[] d : dirs) {
                        BlockPos back = new BlockPos(x - d[0] * 2, 0, z - d[1] * 2), fwd = new BlockPos(x + d[0], 0, z + d[1]);
                        BlockPos back1 = new BlockPos(x - d[0], 0, z - d[1]);
                        int yb = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, back.getX(), back.getZ()) - 1;
                        int yb1 = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, back1.getX(), back1.getZ()) - 1;
                        int yf = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fwd.getX(), fwd.getZ()) - 1;
                        boolean flatBehind = yb1 == y - 1 && !ours(l.getBlockState(new BlockPos(back1.getX(), yb1, back1.getZ())));
                        BlockState f = l.getBlockState(new BlockPos(fwd.getX(), yf, fwd.getZ()));
                        if (flatBehind && yf == y && !ours(f)) {
                            walkFrom = new BlockPos(back1.getX(), y, back1.getZ());
                            walkSlab = s;
                            walkYaw = d[2];
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void run(MinecraftServer s, String... cmds) {
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void look(MinecraftServer s, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        run(s, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    private void server(int wait, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(wait, shot, a, null)); }
    private void client(int wait, String shot, Consumer<Minecraft> a) { steps.add(new Step(wait, shot, null, a)); }
    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[SMOOTHTEST] creating world");
            LevelSettings settings = new LevelSettings("smoothtest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("smoothtest", settings, new WorldOptions(20261022L, true, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[SMOOTHTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (!submitted) {
            submitted = true;
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[SMOOTHTEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            if (st.server() != null) {
                pending = true;
                s.execute(() -> {
                    try {
                        st.server().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[SMOOTHTEST] step failed", t);
                    }
                    pending = false;
                });
            }
        }
        // sunucu adımı bitmeden süre saymaya başlama
        if (pending) {
            stepStart = tick;
            return;
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[SMOOTHTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "smooth_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[SMOOTHTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
            submitted = false;
        }
    }
}
