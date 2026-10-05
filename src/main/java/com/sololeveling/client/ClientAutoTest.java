package com.sololeveling.client;

import com.sololeveling.client.gui.NpcScreen;
import com.sololeveling.client.gui.SystemScreen;
import com.sololeveling.gen.Content;
import com.sololeveling.registry.ModItems;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** CI-only visual self test (-Dsololeveling.clienttest=true): builds a world, stages every model / city / dungeon / UI and takes screenshots. */
public final class ClientAutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick, stepIndex, stepStart, inWorld = -1;
    private boolean worldRequested;
    private final List<Step> steps = new ArrayList<>();

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Consumer<Minecraft> clientAction, boolean gui) {}

    public static void init() {
        LOG.info("[AUTOTEST] client test enabled");
        MinecraftForge.EVENT_BUS.register(new ClientAutoTest());
    }

    private static String wrap(String c) {
        return c.startsWith("sl ") ? "execute as @p at @p run " + c : c;
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), wrap(c));
        }, null, false));
    }

    private void cmdGui(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), wrap(c));
        }, null, true));
    }

    private void screen(int wait, String shot, Consumer<Minecraft> open) {
        steps.add(new Step(wait, shot, null, open, true));
    }

    private static String itemCmd(String id, double x, double y, double z, double scale, String mode) {
        return String.format(Locale.ROOT, "summon minecraft:item_display %.2f %.2f %.2f {item:{id:\"sololeveling:%s\",Count:1b},item_display:\"%s\",billboard:\"fixed\",brightness:{block:15,sky:15},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}",
                x, y, z, id, mode, scale, scale, scale);
    }

    private ClientAutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000", "weather clear",
                "difficulty normal", "gamemode creative @a", "sl level 70", "sl gold 5000");
        // ---------------- showcase platform (y=250) with a backdrop wall
        cmd(10, null, "tp @a 0 151 12 180 0", "fill -30 149 -20 240 149 20 minecraft:gray_concrete", "fill -30 150 -6 240 162 -6 minecraft:light_gray_concrete");
        cmd(160, null);
        // ---------------- items (3D models)
        List<String> weapons = new ArrayList<>(), misc = new ArrayList<>(), armor = new ArrayList<>(), blocks = new ArrayList<>();
        for (Content.WeaponDef d : Content.WEAPONS) weapons.add(d.id());
        for (Content.StaffDef d : Content.STAVES) weapons.add(d.id());
        for (Content.PotionDef d : Content.POTIONS) misc.add(d.id());
        for (Content.CrystalDef d : Content.CRYSTALS) misc.add(d.id());
        misc.add("shadow_essence");
        for (String m : Content.MISC) if (!m.equals("shadow_essence")) misc.add(m);
        for (var e : ModItems.ARMOR.keySet()) armor.add(e);
        for (Content.BlockDef b : Content.BLOCKS) blocks.add(b.id());
        itemShots("weapons", weapons, 150);
        itemShots("misc", misc, 150);
        itemShots("armor", armor, 150);
        itemShots("blocks", blocks, 150);
        // ---------------- creature lineup in the overworld sky platform
        creatureShots();
        // ---------------- armor stands wearing the sets
        List<String> st = new ArrayList<>();
        String[] sets = {"hunter", "knight", "ice", "monarch"};
        String[] weaps = {"hunter_dagger", "igris_blade", "ice_elf_staff", "shadow_monarch_sword"};
        for (int i = 0; i < 4; i++) {
            st.add(String.format(Locale.ROOT, "summon minecraft:armor_stand %d 150 0 {ShowArms:1b,NoGravity:1b,Rotation:[%df,0f],HandItems:[{id:\"sololeveling:%s\",Count:1b},{}],ArmorItems:[{id:\"sololeveling:%s_boots\",Count:1b},{id:\"sololeveling:%s_leggings\",Count:1b},{id:\"sololeveling:%s_chestplate\",Count:1b},{id:\"sololeveling:%s_helmet\",Count:1b}]}",
                    200 + i * 3, -10 + i * 8, weaps[i], sets[i], sets[i], sets[i], sets[i]));
        }
        cmd(10, null, "kill @e[type=!player]", "tp @a 204.5 151 8 180 0");
        cmd(5, null, st.toArray(new String[0]));
        cmd(30, "armor_stands");
        // ---------------- UI screens
        for (int i = 0; i < 6; i++) {
            final int tab = i;
            screen(20, "ui_tab" + i, mc -> mc.setScreen(new SystemScreen(tab)));
        }
        for (String r : Content.NPC_ROLES) screen(12, "ui_npc_" + r, mc -> mc.setScreen(new NpcScreen(-1, r, "Test NPC", "hunters")));
        screen(5, null, mc -> mc.setScreen(null));
        // ---------------- HUD & level-up fx
        cmdGui(40, "hud", "tp @a 0 251 12 180 0", "sl level 71");
        cmdGui(25, "hud_levelup");
        // ---------------- Hunter World city
        cmd(5, null, "kill @e[type=!player]", "sl region seoul");
        cmd(220, null);
        cmd(5, "city_plaza", "tp @a 4.5 74 36.5 0 8");
        cmd(40, "city_plaza2", "tp @a 4.5 80 60 180 12");
        cmd(40, "city_hq", "tp @a 3.5 80 40 0 -25");
        cmd(60, "city_hq_far", "tp @a 3.5 120 140 0 -20");
        cmd(80, "city_skyline", "tp @a -70 150 -70 -135 30");
        cmd(60, "city_street", "tp @a -2 74 20 0 3");
        cmd(80, "city_street2", "tp @a 44 74 20 90 3");
        cmd(60, "city_guild_hunters", "tp @a -37 74 22 180 5");
        cmd(60, "city_guild_hall_inside", "tp @a -37 74 5 0 0");
        cmd(60, "city_hq_lobby", "tp @a 3 74 12 180 0");
        cmd(40, "city_gangnam_arrive", "sl region gangnam");
        cmd(200, null);
        cmd(5, "city_gangnam", "tp @a 624.5 80 400 180 12");
        cmd(5, null, "sl region jeju");
        cmd(220, null);
        cmd(5, "jeju", "tp @a 964.5 78 -870 180 10");
        cmd(40, "jeju_far", "tp @a 964.5 110 -800 0 40");
        // ---------------- gate in front of the city, day + night
        cmd(5, null, "sl region seoul");
        cmd(150, null, "tp @a 4.5 74 20 0 2", "sl gate S demon_castle", "sl gate B venom_swamp red");
        cmd(80, "gate_day", "tp @a 4.5 74 8 0 4");
        cmd(5, null, "time set 18000");
        cmd(40, "gate_night", "tp @a 4.5 74 8 0 4");
        cmd(5, null, "time set 6000");
        // ---------------- skills
        cmd(5, null, "tp @a 4.5 74 0 0 3", "summon sololeveling:goblin 4 74 6 {NoAI:1b}", "summon sololeveling:orc 8 74 8 {NoAI:1b}", "summon sololeveling:orc 0 74 8 {NoAI:1b}");
        cmd(20, null, "sl cast violent_slash");
        cmd(8, "skill_slash");
        cmd(40, null, "sl cast rulers_authority");
        cmd(10, "skill_rulers");
        cmd(30, null, "sl cast dragons_fear");
        cmd(10, "skill_fear");
        cmd(40, null, "kill @e[type=sololeveling:goblin]");
        cmd(10, null, "sl cast shadow_extraction");
        cmdGui(30, "skill_arise");
        cmd(30, null, "sl cast domain_of_monarch");
        cmd(40, "skill_domain");
        cmd(5, null, "kill @e[type=!player]", "sl cast shadow_step");
        // ---------------- dungeons
        String[][] dungeons = {{"goblin_cave", "E"}, {"temple", "D"}, {"venom_swamp", "B"}, {"ice_cave", "A"}, {"hell_den", "A"}, {"ant_nest", "S"}, {"demon_castle", "S"}, {"dragon_lair", "N"}};
        for (String[] d : dungeons) {
            cmd(5, null, "sl dungeon " + d[0] + " " + d[1]);
            cmd(100, "dg_" + d[0] + "_entry", "tp @a ~ ~ ~ -90 5");
            cmd(40, "dg_" + d[0] + "_hall");
            cmd(5, null, "sl bossroom");
            cmd(70, "dg_" + d[0] + "_boss", "tp @a ~ ~ ~ -90 4");
            cmd(40, "dg_" + d[0] + "_boss2");
        }
        cmd(20, "END");
    }

    private void itemShots(String name, List<String> ids, int y0) {
        int perShot = 18;
        for (int s = 0; s * perShot < ids.size(); s++) {
            List<String> cmds = new ArrayList<>();
            cmds.add("kill @e[type=minecraft:item_display]");
            for (int i = 0; i < perShot && s * perShot + i < ids.size(); i++) {
                String id = ids.get(s * perShot + i);
                int row = i / 9, col = i % 9;
                double x = (col - 4) * 2.2, y = y0 + 3.2 - row * 3.0;
                cmds.add(itemCmd(id, x, y, -2, 2.0, "gui"));
            }
            cmd(8, null, "tp @a 0 151 12 180 0");
            cmd(3, null, cmds.toArray(new String[0]));
            cmd(14, name + "_" + s);
        }
    }

    private void creatureShots() {
        String nbt = "{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[0f,0f]}";
        String[][] groups = {
                {"goblin", "orc", "stone_soldier", "venom_ant", "ice_elf", "hell_hound"},
                {"statue_of_god", "kasaka", "baruka", "cerberus"},
                {"giant_ant", "demon_knight", "igris", "ant_king"},
                {"kamish"},
                {"shadow_soldier", "shadow_mage", "shadow_wolf", "shadow_tank", "shadow_igris", "shadow_beru"},
                {"npc_receptionist", "npc_guild_master", "npc_merchant", "npc_journalist", "npc_healer", "npc_hunter", "npc_citizen"}};
        double[] dist = {12, 17, 15, 26, 13, 12};
        for (int g = 0; g < groups.length; g++) {
            List<String> c = new ArrayList<>();
            c.add("kill @e[type=!player]");
            int n = groups[g].length;
            double sp = g == 1 ? 7 : (g == 3 ? 0 : (g == 2 ? 6 : 3.2));
            for (int i = 0; i < n; i++) {
                double x = (i - (n - 1) / 2.0) * sp;
                c.add(String.format(Locale.ROOT, "summon sololeveling:%s %.1f 150 0 %s", groups[g][i], x, nbt));
            }
            cmd(5, null, "tp @a 0 151 " + dist[g] + " 180 4");
            cmd(3, null, c.toArray(new String[0]));
            cmd(30, "creatures_" + g);
            if (g != 5) {
                // attack pose: damage dealer next to them? just a second angle
                cmd(2, null, "tp @a " + (-dist[g] * 0.6) + " 153 " + (dist[g] * 0.8) + " 150 12");
                cmd(10, "creatures_" + g + "_side");
            }
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
            LevelSettings settings = new LevelSettings("sltest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("sltest", settings, new WorldOptions(20241005L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 120;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (!st.gui() && mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !st.gui();
        if (tick == stepStart) {
            if (st.action() != null) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    try { st.action().accept(s); } catch (Throwable t) { LOG.error("[AUTOTEST] step failed", t); }
                });
            }
            if (st.clientAction() != null) st.clientAction().accept(mc);
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "sl_" + String.format("%03d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(), msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
