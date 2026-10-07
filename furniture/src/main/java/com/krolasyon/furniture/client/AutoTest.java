package com.krolasyon.furniture.client;

import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.blockentity.ChalkboardBlockEntity;
import com.krolasyon.furniture.blockentity.PianoBlockEntity;
import com.krolasyon.furniture.registry.ModBlocks;
import com.krolasyon.furniture.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only visual self test (inactive unless -Dkrolasyon.furniture.autotest=true): builds a showroom and takes screenshots. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private Screen keep;
    private boolean showGui;

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Runnable client) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final int Y = -60;

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "kill @e[type=!player]");
        // showroom: everything faces south (towards the camera)
        server(2, null, s -> {
            ServerLevel l = s.overworld();
            for (int x = -4; x <= 18; x++) for (int z = -14; z <= -6; z++) for (int y = Y; y < Y + 5; y++) l.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            wide(l, ModBlocks.SOFA.get().defaultBlockState(), 0, -10);
            wide(l, ModBlocks.PIANO.get().defaultBlockState(), 4, -10);
            single(l, ModBlocks.CHALKBOARD.get().defaultBlockState(), 9, -10);
            single(l, ModBlocks.NIGHTSTAND.get().defaultBlockState(), 12, -10);
            single(l, ModBlocks.WARDROBE.get().defaultBlockState(), 15, -10);
            if (l.getBlockEntity(new BlockPos(9, Y, -10)) instanceof ChalkboardBlockEntity b) {
                b.setLines(new String[]{"KROLASYON", "Furniture mod", "", "2 + 2 = 4", "Hello!", ""});
            }
        });
        // overview
        cmd(5, null, "tp @p 7.5 -58 -2.5 180 12");
        wait(30, "overview");
        // each piece: 3/4 view
        cmd(3, null, "tp @p 3.2 -58.3 -6.6 160 24");
        wait(8, "sofa_front");
        cmd(3, null, "tp @p -1.5 -58.4 -7.2 215 28");
        wait(8, "sofa_side");
        cmd(3, null, "tp @p 6.6 -58.4 -6.6 170 24");
        wait(8, "piano_front");
        server(2, null, s -> notes(s, 0, 4, 7, 12, 16, 19));
        wait(4, "piano_playing");
        wait(3, "piano_playing2");
        cmd(3, null, "tp @p 9.5 -58.6 -7.0 180 18");
        wait(8, "chalkboard_front");
        cmd(3, null, "tp @p 12.5 -58.4 -7.4 160 22");
        wait(6, "nightstand_closed");
        server(1, null, s -> blockEvent(s, 12, 1));
        wait(5, "nightstand_opening");
        wait(14, "nightstand_open");
        server(1, null, s -> blockEvent(s, 12, 0));
        cmd(3, null, "tp @p 15.5 -58.2 -6.4 165 16");
        wait(6, "wardrobe_closed");
        server(1, null, s -> blockEvent(s, 15, 1));
        wait(5, "wardrobe_opening");
        wait(22, "wardrobe_open");
        server(1, null, s -> blockEvent(s, 15, 0));
        // sitting on the sofa (third person, front)
        cmd(10, null, "tp @p 0.5 -60 -6 180 0", "summon krolasyonfurniture:seat 0.5 -59.7 -9.5");
        cmd(3, null, "ride @p mount @e[type=krolasyonfurniture:seat,limit=1]");
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(4, "sofa_sit_start");
        wait(20, "sofa_sitting");
        cmd(2, null, "ride @p dismount");
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        // broom in first person
        client(1, null, () -> showGui = true);
        cmd(6, null, "tp @p 4 -60 -3 180 28", "item replace entity @p weapon.mainhand with krolasyonfurniture:broom");
        wait(25, "broom_hold");
        server(1, null, s -> s.getPlayerList().getPlayers().forEach(p -> p.swing(InteractionHand.MAIN_HAND, true)));
        wait(3, "broom_swing1");
        wait(3, "broom_swing2");
        wait(3, "broom_swing3");
        wait(3, "broom_swing4");
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        wait(6, "broom_third_hold");
        server(1, null, s -> s.getPlayerList().getPlayers().forEach(p -> p.swing(InteractionHand.MAIN_HAND, true)));
        wait(4, "broom_third_swing");
        client(2, null, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        // inventory style item showcase
        client(10, null, () -> {
            Minecraft mc = Minecraft.getInstance();
            keep = new Showcase();
            mc.setScreen(keep);
        });
        wait(8, "items_gui");
        wait(10, "END");
    }

    private static final class Showcase extends Screen {
        Showcase() { super(Component.literal("showcase")); }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            renderBackground(g);
            ItemStack[] items = {new ItemStack(ModItems.SOFA.get()), new ItemStack(ModItems.PIANO.get()), new ItemStack(ModItems.CHALKBOARD.get()),
                    new ItemStack(ModItems.NIGHTSTAND.get()), new ItemStack(ModItems.WARDROBE.get()), new ItemStack(ModItems.BROOM.get())};
            for (int i = 0; i < items.length; i++) {
                g.pose().pushPose();
                g.pose().translate(40 + (i % 3) * 200, 20 + (i / 3) * 150, 0);
                g.pose().scale(6, 6, 1);
                g.renderItem(items[i], 0, 0);
                g.pose().popPose();
            }
        }
    }

    private static void wide(ServerLevel l, BlockState base, int x, int z) {
        BlockState right = base.setValue(WideBlock.FACING, net.minecraft.core.Direction.SOUTH).setValue(WideBlock.PART, com.krolasyon.furniture.block.WidePart.RIGHT);
        l.setBlock(new BlockPos(x, Y, z), right, 3);
        l.setBlock(new BlockPos(x + 1, Y, z), right.setValue(WideBlock.PART, com.krolasyon.furniture.block.WidePart.LEFT), 3);
    }

    private static void single(ServerLevel l, BlockState base, int x, int z) {
        l.setBlock(new BlockPos(x, Y, z), base.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
    }

    private static void notes(MinecraftServer s, int... ns) {
        ServerLevel l = s.overworld();
        BlockPos p = new BlockPos(4, Y, -10);
        BlockState st = l.getBlockState(p);
        for (int n : ns) PianoBlockEntity.playNote(l, p, st, n, null);
    }

    private static void blockEvent(MinecraftServer s, int x, int open) {
        ServerLevel l = s.overworld();
        BlockPos p = new BlockPos(x, Y, -10);
        l.blockEvent(p, l.getBlockState(p).getBlock(), 1, open);
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }, null));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action, null)); }

    private void client(int wait, String shot, Runnable r) { steps.add(new Step(wait, shot, null, r)); }

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
            LevelSettings settings = new LevelSettings("ftest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ftest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && mc.screen != keep) mc.setScreen(null);
        mc.options.hideGui = keep == null && !showGui;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.client() != null) st.client().run();
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
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ftest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
