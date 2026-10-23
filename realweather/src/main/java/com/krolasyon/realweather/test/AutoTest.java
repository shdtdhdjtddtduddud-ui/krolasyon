package com.krolasyon.realweather.test;

import com.krolasyon.realweather.WeatherServer;
import com.krolasyon.realweather.WeatherState;
import com.krolasyon.realweather.client.ClientWeather;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
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
import java.util.function.Predicate;

/** CI-only (-Drealweather.autotest=true): farklı biyomlarda farklı havaları görüntüler, değerleri ve oyun etkilerini kaydeder. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick, stepIndex, stepStart = -1;
    private boolean worldRequested, gui, submitted;
    private volatile boolean pending;
    private final Map<String, BlockPos> spots = new HashMap<>();

    public static void init() {
        LOG.info("[WEATHERTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private void find(ServerLevel l, String name, Predicate<Holder<Biome>> p) {
        Pair<BlockPos, Holder<Biome>> r = l.findClosestBiome3d(p, BlockPos.ZERO, 6400, 32, 64);
        if (r != null) {
            BlockPos b = r.getFirst();
            spots.put(name, b);
            LOG.info("[WEATHERTEST] {} at {} ({})", name, b, r.getSecond().unwrapKey().map(ResourceKey::location).orElse(null));
        } else LOG.info("[WEATHERTEST] {} not found", name);
    }

    private static Predicate<Holder<Biome>> is(ResourceKey<Biome> k) {
        return h -> h.is(k);
    }

    /** Biyomda yere yakın bir kamera konumu; yön ve eğim derece. */
    private void go(MinecraftServer s, String spot, double up, float yaw, float pitch) {
        BlockPos b = spots.get(spot);
        if (b == null) return;
        ServerLevel l = s.overworld();
        // yükseklik haritası yalnızca yüklü chunk'ta doğrudur: önce yükle
        // ağaç altı ya da içi olmayan, açık bir nokta ara
        int bx = b.getX(), bz = b.getZ();
        search:
        for (int r = 0; r <= 40; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) for (int dz = -r; dz <= r; dz += 4) {
                int x = b.getX() + dx, z = b.getZ() + dz;
                l.getChunk(x >> 4, z >> 4);
                int a = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), c = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                boolean clear = a == c;
                for (int ox = -3; ox <= 3 && clear; ox += 3) for (int oz = -3; oz <= 3 && clear; oz += 3)
                    clear = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x + ox, z + oz) <= c + 1;
                if (clear) {
                    bx = x;
                    bz = z;
                    break search;
                }
            }
        }
        b = new BlockPos(bx, 0, bz);
        l.getChunk(bx >> 4, bz >> 4);
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, b.getX(), b.getZ());
        LOG.info("[WEATHERTEST] go {} y {}", spot, y);
        run(s, String.format(java.util.Locale.ROOT, "tp @a %.1f %.1f %.1f %.1f %.1f", b.getX() + 0.5, y + up, b.getZ() + 0.5, yaw, pitch));
    }

    private void weather(MinecraftServer s, WeatherState st) {
        WeatherServer.set(s.overworld(), st, 600, true);
    }

    private void log(String tag) {
        client(1, null, mc -> LOG.info("[WEATHERTEST] {}: state {} I {} rain {} snow {} bliz {} sand {} fog {} mist {} exp {} wind {} {} loops {}", tag,
                ClientWeather.state, f(ClientWeather.intensity), f(ClientWeather.rainAmt), f(ClientWeather.snowAmt), f(ClientWeather.blizzardAmt),
                f(ClientWeather.sandAmt), f(ClientWeather.fogAmt), f(ClientWeather.mistAmt), f(ClientWeather.exposure), f((float) ClientWeather.windX),
                f((float) ClientWeather.windZ), ClientWeather.loopReport()));
    }

    private static String f(float v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private void scene(String spot, WeatherState st, long time, double up, float yaw, float pitch, int load, String shot) {
        server(5, null, s -> {
            run(s, "time set " + time);
            weather(s, st);
            go(s, spot, up, yaw, pitch);
        });
        wait(load, null);
        log(shot);
        wait(1, shot);
    }

    private AutoTest() {
        server(0, null, s -> {
            run(s, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000",
                    "gamemode spectator @a");
            ServerLevel l = s.overworld();
            find(l, "plains", is(Biomes.PLAINS));
            find(l, "forest", h -> h.is(Biomes.FOREST) || h.is(Biomes.BIRCH_FOREST));
            find(l, "snow", h -> h.is(Biomes.SNOWY_PLAINS) || h.is(Biomes.SNOWY_TAIGA));
            find(l, "desert", is(Biomes.DESERT));
            find(l, "swamp", h -> h.is(Biomes.SWAMP) || h.is(Biomes.MANGROVE_SWAMP));
            find(l, "river", h -> h.is(BiomeTags.IS_RIVER));
        });
        scene("plains", WeatherState.CLEAR, 6000, 1.7, 30, 4, 420, "plains_clear");
        scene("plains", WeatherState.OVERCAST, 6000, 1.7, 30, 4, 200, "plains_overcast");
        scene("plains", WeatherState.FOG, 6000, 1.7, 30, 4, 260, "plains_fog");
        scene("forest", WeatherState.RAIN, 6000, 1.7, 120, 6, 420, "forest_rain");
        scene("forest", WeatherState.STORM, 6000, 1.7, 120, 6, 220, "forest_storm");
        client(1, null, mc -> mc.level.setSkyFlashTime(3));
        wait(1, "forest_lightning");
        scene("river", WeatherState.RAIN, 6000, 2.5, 0, 50, 420, "river_rain_ripples");
        scene("snow", WeatherState.RAIN, 6000, 1.7, 200, 4, 420, "snow_snowfall");
        scene("snow", WeatherState.STORM, 6000, 1.7, 200, 4, 240, "snow_blizzard");
        scene("desert", WeatherState.CLEAR, 6000, 1.7, 80, 4, 420, "desert_clear");
        scene("desert", WeatherState.STORM, 6000, 1.7, 80, 4, 240, "desert_sandstorm");
        scene("swamp", WeatherState.CLEAR, 23400, 1.7, 30, 3, 420, "swamp_dawn_mist");
        scene("plains", WeatherState.CLEAR, 17500, 1.7, 30, 2, 260, "plains_night_fireflies");
        scene("forest", WeatherState.CLEAR, 17500, 1.7, 30, 2, 260, "forest_night");
        // ---------------------------------------------------- oyun etkisi: tipide donma, kar birikmesi
        server(5, null, s -> {
            run(s, "time set 6000", "gamemode survival @a", "difficulty normal");
            weather(s, WeatherState.STORM);
            go(s, "snow", 0.1, 200, 10);
        });
        wait(200, null);
        server(5, null, s -> LOG.info("[WEATHERTEST] blizzard 10s frozen {} hp {}", player(s).getTicksFrozen(), player(s).getHealth()));
        wait(400, "freezing");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            LOG.info("[WEATHERTEST] blizzard 30s frozen {} hp {} fully {}", p.getTicksFrozen(), p.getHealth(), p.isFullyFrozen());
            ServerLevel l = s.overworld();
            int deep = 0, any = 0;
            for (int x = p.getBlockX() - 30; x <= p.getBlockX() + 30; x++)
                for (int z = p.getBlockZ() - 30; z <= p.getBlockZ() + 30; z++) {
                    BlockPos t = new BlockPos(x, l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    var st = l.getBlockState(t.below()).is(Blocks.SNOW) ? l.getBlockState(t.below()) : l.getBlockState(t);
                    if (st.is(Blocks.SNOW)) {
                        any++;
                        if (st.getValue(SnowLayerBlock.LAYERS) > 1) deep++;
                    }
                }
            LOG.info("[WEATHERTEST] snow layers around: {} columns, {} deeper than 1", any, deep);
            // ateş başında ısınma
            run(s, String.format("setblock %d %d %d minecraft:campfire", p.getBlockX() + 1, p.getBlockY(), p.getBlockZ()));
        });
        wait(200, "campfire_warm");
        server(5, null, s -> {
            LOG.info("[WEATHERTEST] by campfire frozen {}", player(s).getTicksFrozen());
            LOG.info("[WEATHERTEST] barometer: {}", WeatherServer.report(WeatherServer.data(s.overworld())).getString());
            run(s, "gamemode spectator @a");
        });
        wait(10, "END");
    }

    private static void run(MinecraftServer s, String... cmds) {
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
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
            LOG.info("[WEATHERTEST] creating world");
            LevelSettings settings = new LevelSettings("weathertest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("weathertest", settings, new WorldOptions(424242L, true, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[WEATHERTEST] in world, effects {}", mc.level.effects().getClass().getName());
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (!submitted) {
            submitted = true;
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[WEATHERTEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            if (st.server() != null) {
                pending = true;
                s.execute(() -> {
                    try {
                        st.server().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[WEATHERTEST] step failed", t);
                    }
                    pending = false;
                });
            }
        }
        if (pending) {
            stepStart = tick;
            return;
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[WEATHERTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "weather_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[WEATHERTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
            submitted = false;
        }
    }
}
