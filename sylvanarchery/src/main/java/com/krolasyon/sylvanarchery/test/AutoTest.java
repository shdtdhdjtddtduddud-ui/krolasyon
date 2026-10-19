package com.krolasyon.sylvanarchery.test;

import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only (-Dsylvanarchery.autotest=true): shows the blade in a frame, in hand, charging, the spike wave and a root snare. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, String[] commands, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private boolean gui;

    public static void init() {
        LOG.info("[ARCHTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final String QUIVER = "sylvanarchery:sylvan_quiver{Arrows:[{id:\"sylvanarchery:sylvan_arrow\",Count:64b},{id:\"sylvanarchery:sylvan_arrow\",Count:64b}]}";

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode survival @a", "difficulty normal", "effect give @a resistance 9999 4 true",
                "fill -4 -61 -5 4 -56 -5 minecraft:spruce_planks", "fill -6 -61 -26 6 -55 -26 minecraft:target",
                "summon item_frame -2 -58 -4 {Facing:3b,Fixed:1b,Invisible:1b,Item:{id:\"sylvanarchery:sylvan_bow\",Count:1b}}",
                "summon item_frame 0 -58 -4 {Facing:3b,Fixed:1b,Invisible:1b,ItemRotation:1b,Item:{id:\"sylvanarchery:sylvan_arrow\",Count:1b}}",
                "summon item_frame 2 -58 -4 {Facing:3b,Fixed:1b,Invisible:1b,Item:{id:\"sylvanarchery:sylvan_quiver\",Count:1b,tag:{Arrows:[{id:\"sylvanarchery:sylvan_arrow\",Count:64b}]}}}");
        cmd(80, "frames", "tp @a 0.5 -59.2 -1.6 180 0");
        cmd(20, "frame_bow", "tp @a -1.5 -59 -2.9 180 0");
        cmd(20, "frame_quiver", "tp @a 2.5 -59 -2.9 180 0");
        cmd(5, null, "fill -4 -61 -5 4 -56 -5 minecraft:air", "kill @e[type=item_frame]", "clear @a",
                "give @a sylvanarchery:sylvan_bow 1", "give @a " + QUIVER, "item replace entity @a weapon.offhand with minecraft:bow",
                "summon husk -1 -60 -12 {NoAI:1b,Health:200f,Attributes:[{Name:\"generic.max_health\",Base:200d}]}",
                "summon husk 2 -60 -16 {NoAI:1b,Health:200f,Attributes:[{Name:\"generic.max_health\",Base:200d}]}",
                "tp @a 0.5 -60 2 180 0");
        client(5, null, mc -> { gui = true; mc.player.getInventory().selected = 0; });
        wait(30, "hand_idle");
        client(2, null, mc -> {
            net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        });
        wait(5, "pull_mid");
        wait(15, "pull_full");
        client(3, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        wait(3, "pull_third_back");
        client(3, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(3, "pull_third_front");
        client(1, null, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
        });
        wait(3, "shot_flight");
        wait(30, "shot_hit");
        // eğilerek üçlü yaylım
        client(2, null, mc -> {
            net.minecraft.client.KeyMapping.set(mc.options.keyShift.getKey(), true);
            net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        });
        wait(22, null);
        client(1, null, mc -> net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false));
        wait(4, "volley_flight");
        client(2, null, mc -> net.minecraft.client.KeyMapping.set(mc.options.keyShift.getKey(), false));
        wait(30, null);
        cmd(5, null, "fill -3 -61 -5 3 -57 -5 minecraft:target", "tp @a 0.5 -60 2 180 0");
        client(2, null, mc -> {
            net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        });
        wait(20, null);
        client(1, null, mc -> net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false));
        wait(2, "close_flight");
        wait(25, null);
        cmd(20, "close_stuck_side", "tp @a 3.5 -58.6 -3.2 90 20");
        cmd(5, null, "fill -3 -61 -5 3 -57 -5 minecraft:air");
        cmd(30, "arrows_stuck_side", "tp @a 7 -59 -14 90 10");
        cmd(20, "arrows_stuck_close", "tp @a 0.5 -59.5 -9.5 180 10");
        client(3, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        cmd(30, "quiver_back", "tp @a 0.5 -60 2 180 15");
        client(3, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(25, "quiver_front");
        client(3, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        server(5, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            LOG.info("[ARCHTEST] quiver arrows left {}", com.krolasyon.sylvanarchery.item.QuiverItem.count(
                    com.krolasyon.sylvanarchery.item.QuiverItem.find(p)));
            for (Husk h : s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -30, 20, -40, 10))) {
                LOG.info("[ARCHTEST] husk health {}", h.getHealth());
            }
        });
        wait(20, "END");
    }

    private void cmd(int wait, String shot, String... c) { steps.add(new Step(wait, shot, c, null, null)); }
    private void server(int wait, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(wait, shot, new String[0], a, null)); }
    private void client(int wait, String shot, Consumer<Minecraft> a) { steps.add(new Step(wait, shot, new String[0], null, a)); }
    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, new String[0], null, null)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[ARCHTEST] creating world");
            LevelSettings settings = new LevelSettings("archtest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("archtest", settings, new WorldOptions(9L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[ARCHTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[ARCHTEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    for (String c : st.commands()) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
                    if (st.server() != null) st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[ARCHTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[ARCHTEST] finished");
                    mc.stop();
                    return;
                }
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    int spikes = s.overworld().getEntities(com.krolasyon.sylvanarchery.SylvanArchery.ARROW_ENTITY.get(), e -> true).size();
                    int snares = com.krolasyon.sylvanarchery.item.QuiverItem.count(
                            com.krolasyon.sylvanarchery.item.QuiverItem.find(s.getPlayerList().getPlayers().get(0)));
                    List<LivingEntity> husks = new ArrayList<>(s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -20, 20, -40, 20)));
                    LOG.info("[ARCHTEST] state {} arrows={} quiver={} husks={} playerHp={}", st.shot(), spikes, snares, husks.size(),
                            s.getPlayerList().getPlayers().get(0).getHealth());
                });
                String name = "arch_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[ARCHTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
