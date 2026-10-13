package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI-only visual self test (inactive unless -Dlibra.autotest=true): flat world, close-ups of the model, a rival battle,
 * every ability forced in turn, an owned Libra with the HUD, recall and defeat. Saves screenshots, then quits.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private boolean showGui;

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui, Consumer<Minecraft> clientAction) {
        Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui) { this(delay, shot, action, gui, null); }
    }

    private boolean allowScreen;

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        BeybladeEntity.rivalAbilities = false;
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final String A = "summon librabeyblade:leone 0 -60 -6 {Spin:3000f,MaxSpin:3000f,Tags:[\"a\"]}";
    private static final String B = "summon librabeyblade:eagle 3.5 -60 -9 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"b\"]}";

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty peaceful", "gamemode spectator @a", "kill @e[type=!player]",
                "fill -6 -60 -2 6 -60 2 minecraft:smooth_stone");
        // ---- the ten common beyblades in a row
        cmd(4, null, "summon librabeyblade:common -4 -59 0 {Variant:0,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common -3 -59 0 {Variant:1,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common -2 -59 0 {Variant:2,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common -1 -59 0 {Variant:3,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common 0 -59 0 {Variant:4,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common 1 -59 0 {Variant:5,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common 2 -59 0 {Variant:6,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common 3 -59 0 {Variant:7,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common 4 -59 0 {Variant:8,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "summon librabeyblade:common 5 -59 0 {Variant:9,Spin:3000f,MaxSpin:3000f,Frozen:1b}",
                "tp @a 0.5 -56.6 3.2 180 45");
        wait(30, "commons_row");
        cmd(6, "commons_close", "tp @a -1.5 -57.8 1.6 180 40");
        // ---- contact battle on flat ground: Leone rams Eagle with the storm cage
        cmd(10, null, "kill @e[type=!player]", "fill -6 -60 -2 6 -60 2 minecraft:air", "tp @a 1.5 -55 6 180 22", A, B);
        wait(15, null);
        force(5);
        wait(20, "cage_ram");
        wait(25, "cage_tornado_a");
        wait(30, "cage_tornado_b");
        wait(60, null);
        // ---- big stadium, free battle with commons and legends touching and grinding
        cmd(10, null, "kill @e[type=!player]", "summon librabeyblade:stadium 0.5 -60 -14.5", "tp @a 0.5 -47 9 180 40",
                "summon librabeyblade:pegasus -4 -57 -14.5 {Spin:3000f,MaxSpin:3000f}", "summon librabeyblade:common 5 -57 -14.5 {Variant:0,Spin:3000f,MaxSpin:3000f}",
                "summon librabeyblade:ldrago 0.5 -57 -19 {Spin:3000f,MaxSpin:3000f}", "summon librabeyblade:common 0.5 -57 -10 {Variant:7,Spin:3000f,MaxSpin:3000f}");
        wait(40, "stadium_big");
        cmd(4, null, "tp @a 0.5 -56 -6 180 35");
        wait(16, "stadium_contact_a");
        wait(12, "stadium_contact_b");
        wait(30, null);
        // ---- match vs AI with the scoreboard and the owned HUD
        cmd(4, null, "kill @e[type=librabeyblade:common]", "kill @e[type=librabeyblade:pegasus]", "kill @e[type=librabeyblade:ldrago]",
                "gamemode creative @a", "tp @a 0.5 -60 2.7 180 25");
        server(10, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            for (com.krolasyon.beyblade.entity.StadiumEntity st : p.level().getEntitiesOfClass(com.krolasyon.beyblade.entity.StadiumEntity.class, p.getBoundingBox().inflate(40)))
                st.debugStartVsAI(p);
        });
        steps.add(new Step(90, null, null, true));
        server(4, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            ItemStack st = new ItemStack(ModItems.COMMONS.get(3).get());
            st.getOrCreateTag().putInt("BeyLevel", 7);
            BeybladeEntity.launch(com.krolasyon.beyblade.registry.ModEntities.COMMON.get(), p.level(), p, st,
                    p.getEyePosition().add(0, 0.5, -1), new Vec3(0, 0.45, -0.5), 1F);
        });
        wait(50, "match_hud");
        cmd(4, null, "tp @a 0.5 -52 -2 180 40");
        wait(40, "match_battle");
        client(4, "command_screen", mc -> mc.setScreen(new CommandScreen()));
        client(4, null, mc -> mc.setScreen(null));
        cmd(4, null, "item replace entity @a weapon.mainhand with librabeyblade:common_thunder_viper", "item replace entity @a hotbar.1 with librabeyblade:standard_launcher");
        wait(16, "item_hand");
        steps.add(new Step(2, null, null, false));
        wait(20, "END");
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }, null));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action, null)); }

    private void client(int wait, String shot, Consumer<Minecraft> action) { steps.add(new Step(wait, shot, null, null, action)); }

    private void force(int ability) {
        server(1, null, s -> each(s, "a", b -> {
            LOG.info("[AUTOTEST] force ability {} spin={}", ability, b.getSpin());
            b.debugForceAbility(ability);
        }));
    }

    private static void each(MinecraftServer s, String tag, Consumer<BeybladeEntity> c) {
        ServerLevel l = s.overworld();
        for (BeybladeEntity e : l.getEntitiesOfClass(BeybladeEntity.class, new AABB(-80, -80, -80, 80, 40, 80))) {
            if (tag == null || e.getTags().contains(tag)) c.accept(e);
        }
    }

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
            LevelSettings settings = new LevelSettings("ltest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ltest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !(mc.screen instanceof CommandScreen)) mc.setScreen(null);
        mc.options.hideGui = !showGui;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.gui() != null) showGui = st.gui();
            if (st.clientAction() != null) st.clientAction().accept(mc);
            if (st.action() != null) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    try {
                        st.action().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] step failed", t);
                    }
                });
            }
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> each(srv, null, b -> LOG.info("[AUTOTEST] state pos={} spin={} state={} ability={} rival={} shot={}",
                        b.position(), b.getSpin(), b.getState(), b.getAbility(), b.isRival(), st.shot())));
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ktest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
