package com.krolasyon.shadertest;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * CI-only: builds a small fantasy scene in a flat world, screenshots it at several times of day and in
 * every dimension with the shader pack active, then quits.
 */
@Mod("shadertest")
public final class ShaderTestMod {
    private static final Logger LOG = LogUtils.getLogger();

    public ShaderTestMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) MinecraftForge.EVENT_BUS.register(new Runner());
    }

    record Step(int delay, String shot, String[] commands) {}

    static final class Runner {
        private final List<Step> steps = new ArrayList<>();
        private int tick;
        private boolean worldRequested;
        private int stepIndex;
        private int stepStart = -1;

        Runner() {
            cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "time set 6000", "weather clear", "gamemode creative @a");
            // gölet, ağaçlar, çiçekler, ışık kaynakları, kristaller, vitray
            cmd(5, null,
                    "fill -7 -63 -24 7 -61 -12 minecraft:water",
                    "fill -3 -61 -19 -1 -61 -17 minecraft:grass_block",
                    "place feature minecraft:fancy_oak -13 -60 -18",
                    "place feature minecraft:birch -10 -60 -28",
                    "place feature minecraft:cherry 12 -60 -20",
                    "place feature minecraft:fancy_oak 15 -60 -32",
                    "place feature minecraft:spruce -18 -60 -34",
                    "place feature minecraft:azalea_tree 4 -60 -36");
            cmd(5, null,
                    "fill -11 -60 -9 -5 -60 -4 minecraft:poppy",
                    "fill 5 -60 -9 11 -60 -4 minecraft:cornflower",
                    "fill -11 -60 -11 -5 -60 -10 minecraft:allium",
                    "fill 5 -60 -11 11 -60 -10 minecraft:pink_tulip",
                    "fill -16 -60 -14 -9 -60 -6 minecraft:grass replace minecraft:air",
                    "fill 9 -60 -16 16 -60 -8 minecraft:grass replace minecraft:air",
                    "setblock -2 -60 -18 minecraft:red_mushroom",
                    "setblock -1 -60 -17 minecraft:brown_mushroom",
                    "setblock 3 -60 -9 minecraft:torch",
                    "setblock -3 -60 -9 minecraft:lantern",
                    "fill 9 -60 -24 9 -58 -24 minecraft:glowstone",
                    "setblock -9 -60 -24 minecraft:sea_lantern",
                    "fill -1 -60 -28 1 -60 -28 minecraft:amethyst_block",
                    "setblock 0 -59 -28 minecraft:amethyst_cluster",
                    "fill -3 -60 -40 3 -56 -40 minecraft:magenta_stained_glass",
                    "fill -20 -60 -44 20 -54 -44 minecraft:stone",
                    "tp @a 0 -58.5 4 180 -10");
            wait(200, "noon");
            cmd(60, "morning", "time set 23500", "tp @a 0 -58.5 4 -120 -6");
            cmd(60, "sunrise_scene", "time set 0", "tp @a 0 -58.5 4 180 -10");
            cmd(60, "sunset", "time set 12400", "tp @a 0 -58.5 4 100 -5");
            cmd(60, "sunset_scene", "time set 12400", "tp @a 0 -58.5 4 180 -10");
            cmd(60, "dusk", "time set 13300", "tp @a 0 -58.5 4 180 -10");
            cmd(80, "night", "time set 18000", "tp @a 0 -58.5 4 180 -10");
            cmd(60, "night_sky", "tp @a 0 -58.5 4 180 -50");
            cmd(60, "night_sky2", "tp @a 0 -58.5 4 0 -35");
            cmd(60, "night_close", "tp @a -6 -59 -6 200 -15");
            cmd(60, "underwater", "time set 6000", "tp @a 0 -62.5 -14 180 5");
            cmd(60, "noon_side", "tp @a 22 -57 -10 120 -12");
            cmd(80, "rain", "weather rain", "tp @a 0 -58.5 4 180 -10");
            cmd(200, "nether", "weather clear", "execute in minecraft:the_nether run tp @a 0 80 0 0 10");
            cmd(240, "end", "execute in minecraft:the_end run tp @a 100 52 0 90 5");
            wait(20, "END");
        }

        private void cmd(int wait, String shot, String... commands) { steps.add(new Step(wait, shot, commands)); }

        private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, new String[0])); }

        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            tick++;
            mc.options.pauseOnLostFocus = false;
            if (mc.getOverlay() != null) return;
            if (!worldRequested && tick > 40 && mc.level == null) {
                worldRequested = true;
                LOG.info("[SHADERTEST] creating world");
                LevelSettings settings = new LevelSettings("shadertest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                        new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("shadertest", settings, new WorldOptions(7L, false, false),
                        reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            if (mc.screen != null) mc.setScreen(null);
            mc.options.hideGui = true;
            if (stepStart < 0) {
                stepStart = tick + 100;
                LOG.info("[SHADERTEST] in world");
            }
            if (tick < stepStart || stepIndex >= steps.size()) return;
            Step st = steps.get(stepIndex);
            if (tick == stepStart && st.commands().length > 0) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    for (String c : st.commands()) {
                        try {
                            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
                        } catch (Throwable t) {
                            LOG.error("[SHADERTEST] command failed: {}", c, t);
                        }
                    }
                });
            }
            if (tick - stepStart >= st.delay()) {
                if (st.shot() != null) {
                    if (st.shot().equals("END")) {
                        LOG.info("[SHADERTEST] finished");
                        mc.stop();
                        return;
                    }
                    String name = "shader_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                    Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                            msg -> LOG.info("[SHADERTEST] screenshot {} ({})", st.shot(), mc.fpsString));
                }
                stepIndex++;
                stepStart = tick + 1;
            }
        }
    }
}
