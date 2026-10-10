package com.krolasyon.voidtree.client;

import com.krolasyon.voidtree.Skill;
import com.krolasyon.voidtree.SkillLogic;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI-only visual self test (inactive unless -Dvoidtree.autotest=true): flat world, skill tree screenshots,
 * then casts every skill at a group of zombies and screenshots the effects and animations.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private boolean keepScreen;

    record Step(int delay, String shot, Consumer<MinecraftServer> server, Runnable client) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 13500", "weather clear", "difficulty normal", "gamemode creative @a", "tp @a 0 -60 0 180 8", "effect clear @a",
                "kill @e[type=!player]", "xp add @a 100 levels");
        client(5, null, () -> { Minecraft.getInstance().options.hideGui = false; keepScreen = true; ClientHooks.openTree(); });
        wait(30, "tree_locked");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            for (Skill k : new Skill[]{Skill.SHADOW_DASH, Skill.VOID_MANTLE, Skill.SHADOW_CLAWS, Skill.SOUL_PRISON}) SkillLogic.unlock(p, k.ordinal());
        });
        wait(8, "tree_partial_burst");
        wait(30, "tree_partial");
        cmd(5, null, "void unlockall @a");
        wait(40, "tree_full");
        client(5, null, () -> {
            keepScreen = false;
            Minecraft.getInstance().setScreen(null);
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        wait(20, "hud");

        skill(Skill.SHADOW_DASH, 2, 8);
        skill(Skill.PHANTOM_FORM, 4, 20);
        skill(Skill.VOID_VORTEX, 12, 50);
        skill(Skill.SPIRIT_TWISTER, 12, 40);
        skill(Skill.SHADOW_CLAWS, 3, 9);
        skill(Skill.SOUL_PRISON, 8, 40);
        skill(Skill.SHADOW_RAIN, 12, 30);
        skill(Skill.BLACK_HOLE, 30, 119);
        skill(Skill.RIFT_WALK, 3, 10);
        skill(Skill.ASCENSION, 15, 40);
        skill(Skill.VOID_SEAL, 15, 50);
        skill(Skill.SHADOW_CLONES, 10, 60);
        skill(Skill.COSMIC_COLLAPSE, 35, 71);
        // animations from the front
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        for (Skill k : new Skill[]{Skill.SHADOW_CLAWS, Skill.BLACK_HOLE, Skill.ASCENSION, Skill.SHADOW_CLONES, Skill.SOUL_PRISON}) {
            mobs();
            castNow(k);
            int[] at = switch (k.anim) {
                case CLAWS -> new int[]{2, 6};
                case HOLE -> new int[]{10, 16};
                case ASCEND -> new int[]{7, 25};
                case SUMMON -> new int[]{6, 12};
                default -> new int[]{4, 9};
            };
            wait(at[0], "anim_" + k.id + "_a");
            wait(at[1] - at[0], "anim_" + k.id + "_b");
            wait(40, null);
        }
        // first person
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        mobs();
        castNow(Skill.SHADOW_CLAWS);
        wait(3, "fp_claws");
        wait(40, null);
        castNow(Skill.BLACK_HOLE);
        wait(25, "fp_black_hole");
        wait(110, null);
        wait(20, "END");
    }

    private void mobs() {
        cmd(6, null, "kill @e[type=!player]", "tp @a 0 -60 0 180 8", "effect clear @a",
                "summon minecraft:zombie 0 -60 -6 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:zombie -3 -60 -9 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:husk 3 -60 -9 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:skeleton -1 -60 -12 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:spider 2 -60 -13 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:zombie 5 -60 -5 {PersistenceRequired:1b,Rotation:[0f,0f]}");
    }

    private void castNow(Skill k) {
        server(0, null, s -> {
            ServerPlayer p = player(s);
            boolean ok = SkillLogic.cast(p, k, true);
            LOG.info("[AUTOTEST] cast {} -> {}", k.id, ok);
        });
    }

    private void skill(Skill k, int first, int second) {
        mobs();
        wait(14, null);
        castNow(k);
        wait(first, k.id + "_a");
        wait(second - first, k.id + "_b");
        wait(50, null);
    }

    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }, null));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action, null)); }

    private void client(int wait, String shot, Runnable action) { steps.add(new Step(wait, shot, null, action)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[AUTOTEST] creating world");
            LevelSettings settings = new LevelSettings("stest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("stest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !keepScreen) mc.setScreen(null);
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.client() != null) st.client().run();
            if (st.server() != null) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    try {
                        st.server().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] step failed", t);
                    }
                });
            }
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "stest_" + String.format("%03d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
