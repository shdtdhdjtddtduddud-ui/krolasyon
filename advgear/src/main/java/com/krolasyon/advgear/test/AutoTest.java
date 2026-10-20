package com.krolasyon.advgear.test;

import com.krolasyon.advgear.AdvGear;
import com.krolasyon.advgear.block.LockType;
import com.krolasyon.advgear.block.LockedChestBlock;
import com.krolasyon.advgear.client.OreSight;
import com.krolasyon.advgear.gear.GearData;
import com.krolasyon.advgear.item.BackpackItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only (-Dadvgear.autotest=true): tüm eşyaların vitrini, giyilmiş eşyalar, ekranlar ve işlevler. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private boolean gui, allowScreen;
    private static final int Y = -60;

    public static void init() {
        LOG.info("[GEARTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static void wear(MinecraftServer s, RegistryObject<Item> ring1, RegistryObject<Item> ring2, RegistryObject<Item> goggles,
                             RegistryObject<Item> belt, RegistryObject<Item> back) {
        GearData.Gear g = GearData.get(player(s));
        RegistryObject<Item>[] items = new RegistryObject[]{ring1, ring2, goggles, belt, back};
        for (int i = 0; i < 5; i++) g.setItem(i, items[i] == null ? ItemStack.EMPTY : new ItemStack(items[i].get()));
    }

    private AutoTest() {
        server(0, null, s -> {
            run(s, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000",
                    "weather clear", "gamemode creative @a", "difficulty peaceful");
            // vitrin duvarı
            run(s, "fill -7 " + Y + " 4 6 " + (Y + 5) + " 4 minecraft:dark_oak_planks", "fill -7 " + (Y - 1) + " -8 6 " + (Y - 1) + " 4 minecraft:spruce_planks");
            int i = 0;
            for (RegistryObject<Item> r : AdvGear.ALL) {
                int col = i % 10, row = i / 10;
                String id = r.getId().toString();
                run(s, String.format("summon glow_item_frame %d %d 3 {Facing:2b,Fixed:1b,Invisible:0b,Item:{id:\"%s\",Count:1b}}",
                        -5 + col, Y + 4 - row, id));
                i++;
            }
            tp(s, -0.5, Y, -1.5, 0, -17);
        });
        wait(80, "showcase");
        server(5, null, s -> tp(s, -3.0, Y + 1, 1.0, 0, -12));
        wait(30, "showcase_close_left");
        server(5, null, s -> tp(s, 2.0, Y + 1, 1.0, 0, -12));
        wait(30, "showcase_close_right");

        // ------------------------------------------------ giyilen eşyalar (üçüncü şahıs)
        server(5, null, s -> {
            wear(s, AdvGear.EMERALD_EYE_RING, AdvGear.SKULL_RING, AdvGear.ENGINEER_GOGGLES, AdvGear.ALCHEMIST_BELT, AdvGear.EXPEDITION_PACK);
            tp(s, 0.5, Y, -4.5, 180, 0);
        });
        client(20, null, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.options.fov().set(32);
        });
        wait(20, "worn_front_1");
        client(20, "worn_back_1", mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server(5, null, s -> wear(s, AdvGear.GOLD_BAND, null, AdvGear.HUNTER_GOGGLES, AdvGear.TOOL_BELT, AdvGear.TRAVELER_PACK));
        client(20, "worn_back_2", mc -> {});
        client(20, "worn_front_2", mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        server(5, null, s -> wear(s, AdvGear.GOLD_BAND, AdvGear.BONE_HAND_RING, AdvGear.EXPLORER_GOGGLES, AdvGear.TRAVELER_BELT, AdvGear.SATCHEL));
        client(20, "worn_front_3", mc -> {});
        client(20, "worn_back_3", mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        server(5, null, s -> {
            ServerPlayer p = player(s);
            LOG.info("[GEARTEST] effects: nightvision {} speed modifier {} luck {}", p.hasEffect(MobEffects.NIGHT_VISION),
                    p.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().size(), p.getAttributeValue(Attributes.LUCK));
        });

        // ------------------------------------------------ kuşanım ekranı ve çanta
        client(5, null, mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(70);
            allowScreen = true;
            gui = true;
        });
        server(5, null, s -> AdvGear.openGear(player(s)));
        wait(30, "gear_screen");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            p.closeContainer();
            ItemStack bag = new ItemStack(AdvGear.EXPEDITION_PACK.get());
            p.getInventory().setItem(0, bag);
            p.getInventory().selected = 0;
            ((BackpackItem) bag.getItem()).open(p, p.getInventory().getItem(0), 0);
            for (int k = 0; k < 9; k++) p.containerMenu.getSlot(k).set(new ItemStack(new Item[]{Items.TORCH, Items.BREAD, Items.IRON_INGOT,
                    Items.ARROW, Items.OAK_LOG, Items.COAL, Items.MAP, Items.COMPASS, Items.APPLE}[k], 1 + k * 7));
        });
        wait(30, "backpack_screen");
        server(5, null, s -> {
            player(s).closeContainer();
            LOG.info("[GEARTEST] backpack saved items {}", com.krolasyon.advgear.gear.BackpackContainer.count(player(s).getInventory().getItem(0)));
        });
        client(5, null, mc -> {
            allowScreen = false;
            mc.setScreen(null);
        });

        // ------------------------------------------------ eldeki 3D eşyalar
        server(5, null, s -> {
            tp(s, 0.5, Y, -5.5, 0, 10);
            player(s).getInventory().setItem(0, new ItemStack(AdvGear.ENGINEER_GOGGLES.get()));
        });
        wait(20, "held_goggles");
        server(5, null, s -> player(s).getInventory().setItem(0, new ItemStack(AdvGear.TRAVELER_PACK.get())));
        wait(20, "held_backpack");
        server(5, null, s -> player(s).getInventory().setItem(0, new ItemStack(AdvGear.APPRENTICE_SPELLBOOK.get())));
        wait(20, "held_book_closed");
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), true));
        wait(14, "book_casting");
        client(2, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), false));
        wait(6, "firebolt");
        server(5, null, s -> player(s).getInventory().setItem(0, new ItemStack(AdvGear.BLACK_GRIMOIRE.get())));
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), true));
        wait(24, "grimoire_casting");
        client(2, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), false));
        wait(4, "grimoire_storm");
        client(10, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        server(5, null, s -> player(s).getInventory().setItem(0, new ItemStack(AdvGear.TRAVELER_TOME.get())));
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), true));
        wait(16, "tome_third_person");
        client(2, null, mc -> {
            KeyMapping.set(mc.options.keyUse.getKey(), false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        wait(10, null);

        // ------------------------------------------------ iksirler
        server(5, null, s -> {
            run(s, "time set 6000");
            tp(s, 0.5, Y, -5.5, 0, 10);
            player(s).getInventory().setItem(0, new ItemStack(AdvGear.HEART_TONIC.get()));
        });
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), true));
        wait(12, "drinking");
        wait(26, null);
        client(5, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), false));
        server(5, null, s -> LOG.info("[GEARTEST] after tonic regeneration {}", player(s).hasEffect(MobEffects.REGENERATION)));
        server(5, null, s -> {
            player(s).getInventory().setItem(0, new ItemStack(AdvGear.BLACK_DEATH.get()));
            tp(s, 0.5, Y, -5.5, 0, 35);
        });
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), true));
        client(2, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), false));
        wait(30, "poison_cloud");

        // ------------------------------------------------ kilitli sandıklar
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            run(s, "kill @e[type=minecraft:area_effect_cloud]");
            LockType[] types = LockType.values();
            for (int k = 0; k < 3; k++) {
                l.setBlock(new BlockPos(-2 + k * 2, Y, 0), AdvGear.LOCKED_CHEST.get().defaultBlockState()
                        .setValue(LockedChestBlock.FACING, Direction.NORTH).setValue(LockedChestBlock.LOCK, types[k]), 3);
            }
            tp(s, 0.5, Y + 0.5, -3.2, 0, 22);
            player(s).getInventory().setItem(0, new ItemStack(AdvGear.IRON_KEY.get()));
        });
        wait(30, "locked_chests");
        client(5, null, mc -> {
            allowScreen = true;
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(-1.5, Y + 0.5, 0.05), Direction.NORTH, new BlockPos(-2, Y, 0), false));
        });
        wait(30, "unlocked_loot");
        server(5, null, s -> {
            player(s).closeContainer();
            LOG.info("[GEARTEST] chest after key: {}", s.overworld().getBlockState(new BlockPos(-2, Y, 0)).getBlock());
        });
        client(5, null, mc -> {
            allowScreen = false;
            mc.setScreen(null);
        });
        // anahtarsız: kilitli mesajı
        server(5, null, s -> player(s).getInventory().setItem(0, ItemStack.EMPTY));
        client(5, null, mc -> {
            gui = true;
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(2.5, Y + 0.5, 0.05), Direction.NORTH, new BlockPos(2, Y, 0), false));
        });
        wait(10, "locked_message");
        client(2, null, mc -> gui = false);

        // ------------------------------------------------ cevher görüşü ve avcı gözlüğü
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            run(s, "fill -6 " + (Y) + " 8 6 " + (Y + 4) + " 14 minecraft:stone");
            BlockPos[] ores = {new BlockPos(-3, Y + 1, 11), new BlockPos(0, Y + 2, 12), new BlockPos(3, Y, 10), new BlockPos(1, Y + 3, 13), new BlockPos(-1, Y, 9)};
            net.minecraft.world.level.block.Block[] kinds = {Blocks.DIAMOND_ORE, Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.EMERALD_ORE, Blocks.REDSTONE_ORE};
            for (int k = 0; k < ores.length; k++) l.setBlock(ores[k], kinds[k].defaultBlockState(), 3);
            wear(s, null, null, AdvGear.ENGINEER_GOGGLES, null, null);
            tp(s, 0.5, Y, 6.5, 0, 12);
        });
        client(20, "ore_sight_off", mc -> {});
        client(20, "ore_sight_on", mc -> OreSight.force = true);
        client(2, null, mc -> OreSight.force = false);
        server(5, null, s -> {
            run(s, "difficulty easy", "summon zombie 0 " + Y + " 18 {NoAI:1b,PersistenceRequired:1b}", "summon skeleton 3 " + Y + " 18 {NoAI:1b,PersistenceRequired:1b}");
            wear(s, null, null, AdvGear.HUNTER_GOGGLES, null, null);
        });
        wait(50, "hunter_glow");

        // ------------------------------------------------ dünya üretimi: kilitli sandık var mı?
        server(5, null, s -> {
            run(s, "difficulty peaceful");
            LOG.info("[GEARTEST] gear slots {}", GearData.get(player(s)).getItem(2));
        });
        wait(20, "END");
    }

    private static void run(MinecraftServer s, String... cmds) {
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void tp(MinecraftServer s, double x, double y, double z, float yaw, float pitch) {
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
            LOG.info("[GEARTEST] creating world");
            GameRules rules = new GameRules();
            LevelSettings settings = new LevelSettings("geartest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("geartest", settings, new WorldOptions(77L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !allowScreen) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 60;
            LOG.info("[GEARTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[GEARTEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            if (st.server() != null) s.execute(() -> {
                try {
                    st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[GEARTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[GEARTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "gear_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[GEARTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
