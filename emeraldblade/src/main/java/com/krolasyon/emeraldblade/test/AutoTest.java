package com.krolasyon.emeraldblade.test;

import com.krolasyon.emeraldblade.entity.RootSnareEntity;
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

/** CI-only (-Demeraldblade.autotest=true): shows the blade in a frame, in hand, charging, the spike wave and a root snare. */
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
        LOG.info("[BLADETEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "difficulty normal",
                "fill -4 -61 -7 4 -55 -7 minecraft:polished_deepslate",
                "summon item_frame -1 -58 -6 {Facing:3b,Fixed:1b,Invisible:1b,ItemRotation:1b,Item:{id:\"emeraldblade:emerald_root_blade\",Count:1b}}",
                "summon item_frame 1 -58 -6 {Facing:3b,Fixed:1b,Invisible:1b,ItemRotation:0b,Item:{id:\"emeraldblade:emerald_root_blade\",Count:1b}}");
        cmd(80, "frame_close", "tp @a -0.5 -58.3 -4.6 180 0");
        cmd(30, "frame_two", "tp @a 0.5 -58 -2.6 180 0");
        cmd(5, null, "clear @a", "give @a emeraldblade:emerald_root_blade 1", "tp @a 0.5 -60 6 180 0");
        client(5, null, mc -> { gui = true; mc.player.getInventory().selected = 0; });
        wait(30, "hand_idle");
        client(5, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(30, "third_front");
        client(5, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        wait(25, "third_back");
        // gerçek sağ tık ile şarj
        cmd(5, null, "summon husk 0.5 -60 -1 {NoAI:1b}", "summon husk 0.5 -60 -5 {NoAI:1b}", "summon husk 1.5 -60 -9 {NoAI:1b}",
                "summon husk -0.5 -60 -13 {NoAI:1b}", "tp @a 0.5 -60 3.5 180 0");
        client(5, null, mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        wait(36, "charging_back");
        client(3, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(3, "charging_front");
        client(1, null, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.gameMode.releaseUsingItem(mc.player);
        });
        wait(5, "wave_a");
        wait(5, "wave_b");
        wait(6, "wave_c");
        cmd(5, "wave_side", "tp @a 9 -58 -4 90 15");
        wait(20, "wave_side_b");
        // kök tuzağı
        client(2, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        cmd(10, null, "kill @e[type=husk]", "summon husk 0.5 -60 -2 {NoAI:1b,Health:100f,Attributes:[{Name:\"generic.max_health\",Base:100d}]}",
                "tp @a 0.5 -59 1.5 180 15");
        server(12, null, s -> {
            ServerLevel l = s.overworld();
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            for (Husk h : l.getEntitiesOfClass(Husk.class, new AABB(-5, -62, -6, 5, -55, 2))) RootSnareEntity.snare(l, h, p);
        });
        wait(4, "snare_grow");
        wait(10, "snare_full");
        client(2, null, mc -> {
            for (var e : mc.level.entitiesForRendering()) {
                if (e instanceof Husk h) mc.gameMode.attack(mc.player, h);
            }
        });
        wait(8, "hit");
        server(2, null, s -> {
            for (Husk h : s.overworld().getEntitiesOfClass(Husk.class, new AABB(-5, -62, -6, 5, -55, 2))) {
                LOG.info("[BLADETEST] husk health after hit {}", h.getHealth());
            }
        });
        client(2, null, mc -> gui = false);
        cmd(40, "night_frame", "time set 18000", "tp @a -0.5 -58.3 -4.6 180 0");
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
            LOG.info("[BLADETEST] creating world");
            LevelSettings settings = new LevelSettings("bladetest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("bladetest", settings, new WorldOptions(9L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[BLADETEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[BLADETEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    for (String c : st.commands()) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
                    if (st.server() != null) st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[BLADETEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[BLADETEST] finished");
                    mc.stop();
                    return;
                }
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    int spikes = s.overworld().getEntities(com.krolasyon.emeraldblade.EmeraldBlade.SPIKE.get(), e -> true).size();
                    int snares = s.overworld().getEntities(com.krolasyon.emeraldblade.EmeraldBlade.SNARE.get(), e -> true).size();
                    List<LivingEntity> husks = new ArrayList<>(s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -20, 20, -40, 20)));
                    LOG.info("[BLADETEST] state {} spikes={} snares={} husks={} playerHp={}", st.shot(), spikes, snares, husks.size(),
                            s.getPlayerList().getPlayers().get(0).getHealth());
                });
                String name = "blade_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[BLADETEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
