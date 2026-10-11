package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Alien;
import com.krolasyon.omnitrix.OmniCapability;
import com.krolasyon.omnitrix.OmniData;
import com.krolasyon.omnitrix.Powers;
import com.krolasyon.omnitrix.Transform;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** CI-only visual self test (-Domnitrix.autotest=true): dial screen, every transformation, ability and block stance. */
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
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "gamerule mobGriefing false",
                "time set 1000", "weather clear", "difficulty normal", "gamemode creative @a", "tp @a 0 -60 0 180 8", "kill @e[type=!player]");
        server(5, null, s -> { OmniData d = OmniCapability.get(player(s)); d.wearing = true; com.krolasyon.omnitrix.net.Net.sync(player(s)); });
        client(4, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(20, "human_watch");
        client(2, null, () -> {
            Minecraft.getInstance().options.hideGui = false;
            keepScreen = true;
            ClientEvents.watchAnim = Minecraft.getInstance().level.getGameTime();
            Minecraft.getInstance().setScreen(new OmnitrixScreen());
        });
        wait(14, "dial_open");
        client(2, null, () -> { if (Minecraft.getInstance().screen instanceof OmnitrixScreen os) os.autoSelect(5, false); });
        wait(10, "dial_fourarms");
        client(2, null, () -> { if (Minecraft.getInstance().screen instanceof OmnitrixScreen os) os.autoSelect(0, true); });
        wait(10, "dial_confirm");
        client(10, null, () -> { keepScreen = false; Minecraft.getInstance().setScreen(null); });
        for (Alien a : Alien.VALUES) alien(a);
        // first person with the arm
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        transform(Alien.HEATBLAST);
        wait(40, "fp_heatblast");
        ability(0);
        wait(5, "fp_heatblast_fireball");
        transform(Alien.FOURARMS);
        wait(40, "fp_fourarms");
        server(2, null, s -> Transform.revert(player(s), false));
        wait(12, "revert_flash");
        wait(30, "END");
    }

    private void alien(Alien a) {
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        mobs();
        transform(a);
        wait(16, a.id + "_transform");
        wait(30, a.id + "_front");
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        wait(6, a.id + "_back");
        for (int i = 0; i < 4; i++) {
            mobs();
            wait(12, null);
            sideCamera(true);
            ability(i);
            int[] at = switch (a.ability(i).duration) {
                default -> new int[]{Math.max(4, a.ability(i).duration / 2), a.ability(i).duration + 6};
            };
            wait(at[0], a.id + "_ab" + i + "_a");
            wait(at[1] - at[0], a.id + "_ab" + i + "_b");
            wait(10, null);
            sideCamera(false);
        }
        server(2, null, s -> Powers.setBlocking(player(s), true));
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(14, a.id + "_block");
        server(2, null, s -> Powers.setBlocking(player(s), false));
        cmd(2, null, "effect clear @a");
    }

    private void transform(Alien a) {
        server(0, null, s -> {
            ServerPlayer p = player(s);
            OmniData d = OmniCapability.get(p);
            d.recharge = 0;
            Transform.transform(p, a.ordinal());
            LOG.info("[AUTOTEST] transform {}", a.id);
        });
    }

    private void ability(int slot) {
        server(0, null, s -> {
            ServerPlayer p = player(s);
            OmniData d = OmniCapability.get(p);
            Arrays.fill(d.cooldowns, 0);
            Powers.use(p, slot);
            LOG.info("[AUTOTEST] ability {} {} -> cast {}", d.current(), slot, d.castAbility);
        });
    }

    private void mobs() {
        cmd(4, null, "kill @e[type=!player]", "tp @a 0 -60 0 180 8",
                "summon minecraft:zombie 0 -60 -6 {PersistenceRequired:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",Count:1b}],Rotation:[0f,0f]}",
                "summon minecraft:zombie -3 -60 -8 {PersistenceRequired:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",Count:1b}],Rotation:[0f,0f]}",
                "summon minecraft:husk 3 -60 -8 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:skeleton -1 -60 -11 {PersistenceRequired:1b,ArmorItems:[{},{},{},{id:\"minecraft:leather_helmet\",Count:1b}],Rotation:[0f,0f]}",
                "summon minecraft:spider 2 -60 -12 {PersistenceRequired:1b,Rotation:[0f,0f]}",
                "summon minecraft:armor_stand 11 -57.4 -4 {Invisible:1b,NoGravity:1b,Marker:1b,Rotation:[101f,13f]}");
    }

    /** ability shots are taken from the side: from behind, the player hides everything that flies along the look line */
    private void sideCamera(boolean on) {
        client(1, null, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            if (!on) {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                return;
            }
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof ArmorStand) {
                    mc.setCameraEntity(e);
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    return;
                }
            }
            LOG.warn("[AUTOTEST] no side camera entity");
        });
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
            LevelSettings settings = new LevelSettings("otest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("otest", settings, new WorldOptions(42L, false, false),
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
                Screenshot.grab(mc.gameDirectory, "otest_" + String.format("%03d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
