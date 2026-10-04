package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.rpg.client.gui.CharacterScreen;
import com.krolasyon.bosses.rpg.client.gui.JournalScreen;
import com.krolasyon.bosses.rpg.client.gui.MapScreen;
import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.Ability;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.def.SwordDef;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.item.RpgSwordItem;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.magic.SpellCaster;
import com.krolasyon.bosses.rpg.magic.SwordSkills;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.npc.NpcDialog;
import com.krolasyon.bosses.rpg.npc.NpcFactory;
import com.krolasyon.bosses.rpg.npc.NpcRole;
import com.krolasyon.bosses.rpg.npc.RpgNpc;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

/** CI-only visual test of the RPG layer (-Dkrolasyon.autotest=true). Saves screenshots into run/screenshots. */
public final class RpgAutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Predicate<MinecraftServer> until, Supplier<Screen> screen, boolean gui) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick, stepIndex, stepStart = -1;
    private boolean worldRequested, inWorld;
    private boolean condDone;
    private volatile boolean condResult;
    private int errors;

    public static void init() {
        LOG.info("[AUTOTEST] RPG test enabled");
        MinecraftForge.EVENT_BUS.register(new RpgAutoTest());
    }

    private void server(int delay, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(delay, shot, a, null, null, false)); }
    private void shotGui(int delay, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(delay, shot, a, null, null, true)); }
    private void until(int timeout, Predicate<MinecraftServer> p) { steps.add(new Step(timeout, null, null, p, null, false)); }
    private void screen(int delay, String shot, Supplier<Screen> s) { steps.add(new Step(delay, shot, null, null, s, true)); }

    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private static void cmd(MinecraftServer s, String... cs) {
        for (String c : cs) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void look(ServerPlayer p, double x, double y, double z, float yaw, float pitch) {
        p.teleportTo(p.serverLevel(), x, y, z, yaw, pitch);
    }

    private static void clearArea(ServerLevel l, ServerPlayer p) {
        for (Entity e : l.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(70), e -> !(e instanceof ServerPlayer))) e.discard();
    }

    private static int ground(ServerLevel l, int x, int z) { return com.krolasyon.bosses.rpg.util.Heights.ground(l, x, z); }

    private static Site cap(MinecraftServer s, Kingdom k) { return WorldMap.capital(RpgWorldData.get(s), k.ordinal()); }

    private RpgAutoTest() {
        server(0, null, s -> cmd(s, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty normal", "gamemode creative @a"));
        // the capital of Aldoria is built around the spawn and the story places the player in the family hovel
        until(3600, s -> {
            RpgWorldData w = RpgWorldData.get(s);
            Site c = cap(s, Kingdom.ALDORIA);
            if (s.getTickCount() % 100 == 0) LOG.info("[AUTOTEST] waiting for Solmere built={} anchors={}", c.built, w.anchors.keySet());
            return c.built && w.anchor("family_home") != null && RpgWorldData.player(player(s)).originDone;
        });
        shotGui(60, "origin_home", s -> {
            ServerLevel l = s.overworld();
            Site c = cap(s, Kingdom.ALDORIA);
            int n = l.getEntitiesOfClass(RpgNpc.class, new AABB(c.x - 120, -100, c.z - 120, c.x + 120, 320, c.z + 120)).size();
            LOG.info("[AUTOTEST] Solmere at {} {} {} npcs={} sites={}", c.x, c.y, c.z, n, RpgWorldData.get(s).sites.size());
            for (Site x : RpgWorldData.get(s).sites) LOG.info("[AUTOTEST] site {} {} {} at {} {}", x.id, x.name, x.type, x.x, x.z);
        });
        shotGui(30, "dialog_mother", s -> {
            ServerPlayer p = player(s);
            RpgNpc mom = NpcFactory.findStory(p.serverLevel(), p.blockPosition(), "mother", 40);
            LOG.info("[AUTOTEST] mother {}", mom);
            if (mom != null) NpcDialog.open(p, mom, "root");
        });
        server(5, null, s -> NpcDialog.close(player(s)));
        server(40, "home_outside", s -> {
            BlockPos h = RpgWorldData.get(s).anchor("family_home");
            ServerPlayer p = player(s);
            look(p, h.getX() + 0.5, h.getY() + 3, h.getZ() + 14.5, 180, 12);
        });
        server(40, "square", s -> {
            Site c = cap(s, Kingdom.ALDORIA);
            look(player(s), c.x + 0.5, c.y + 3, c.z + 22.5, 180, 8);
        });
        server(50, "palace", s -> {
            Site c = cap(s, Kingdom.ALDORIA);
            look(player(s), c.x + 0.5, c.y + 6, c.z - 18.5, 180, -4);
        });
        server(40, "temple_guild", s -> {
            Site c = cap(s, Kingdom.ALDORIA);
            look(player(s), c.x + 8.5, c.y + 4, c.z - 8.5, -120, 4);
        });
        server(60, "capital_aerial", s -> {
            Site c = cap(s, Kingdom.ALDORIA);
            look(player(s), c.x + 0.5, c.y + 95, c.z + 110.5, 180, 45);
        });
        server(60, "capital_top", s -> {
            Site c = cap(s, Kingdom.ALDORIA);
            look(player(s), c.x + 0.5, c.y + 150, c.z + 0.5, 180, 89);
        });
        server(40, "slums", s -> {
            BlockPos h = RpgWorldData.get(s).anchor("family_home");
            look(player(s), h.getX() + 16.5, h.getY() + 10, h.getZ() - 14.5, 135, 25);
        });
        // UI
        server(20, null, s -> {
            ServerPlayer p = player(s);
            PlayerRpg d = RpgWorldData.player(p);
            d.level = 12;
            d.statPoints = 6;
            for (SpellDef sp : RpgDefs.SPELLS) if (sp.tier() <= 3 && d.spells.size() < 30) d.spells.add(sp.id());
            d.guild = 2;
            d.addRep(0, 260);
            d.addRep(1, -120);
            PlayerMagic.sync(p);
        });
        screen(30, "ui_character", CharacterScreen::new);
        screen(30, "ui_journal", JournalScreen::new);
        screen(30, "ui_map", MapScreen::new);
        // story and dialogue logic: talk, gift, the first story steps, a shop
        shotGui(30, "story_steps", s -> {
            ServerPlayer p = player(s);
            RpgNpc mom = NpcFactory.findStory(p.serverLevel(), p.blockPosition(), "mother", 80);
            if (mom == null) { LOG.warn("[AUTOTEST] no mother"); return; }
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.POPPY, 3));
            NpcDialog.choose(p, mom.getId(), "talk");
            NpcDialog.choose(p, mom.getId(), "gift");
            NpcDialog.choose(p, mom.getId(), "story_bread");
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BREAD, 3));
            NpcDialog.choose(p, mom.getId(), "story_give_bread");
            RpgNpc dad = NpcFactory.findStory(p.serverLevel(), p.blockPosition(), "father", 80);
            if (dad != null) NpcDialog.choose(p, dad.getId(), "story_sword");
            PlayerRpg d = RpgWorldData.player(p);
            LOG.info("[AUTOTEST] story chapter={} step={} objective={}", d.chapter, d.step, com.krolasyon.bosses.rpg.story.Story.objective(d));
        });
        server(5, null, s -> NpcDialog.close(player(s)));
        shotGui(30, "ui_trade", s -> {
            ServerPlayer p = player(s);
            Site c = cap(s, Kingdom.ALDORIA);
            RpgNpc m = NpcFactory.spawn(p.serverLevel(), new BlockPos(c.x + 3, c.y + 1, c.z + 20), Race.DWARF, false, NpcRole.BLACKSMITH, 0, null, null);
            look(p, c.x + 3.5, c.y + 1, c.z + 23.5, 180, 0);
            if (m != null) {
                NpcDialog.choose(p, m.getId(), "quest");
                m.openTrade(p);
                LOG.info("[AUTOTEST] quests={} offers={}", RpgWorldData.player(p).quests.size(), m.getOffers().size());
            }
        });
        server(5, null, s -> player(s).closeContainer());
        // warm up the gallery area so its chunks are loaded on the client
        server(80, null, s -> {
            Site g = gallerySite(s);
            int y = ground(s.overworld(), g.x, g.z);
            look(player(s), g.x + 0.5, y + 2, g.z + 12.5, 180, 8);
        });
        // monster gallery, far from town
        List<MonsterDef> mobs = new ArrayList<>(RpgDefs.MONSTERS);
        for (int i = 0; i < mobs.size(); i += 8) {
            List<MonsterDef> batch = mobs.subList(i, Math.min(mobs.size(), i + 8));
            int idx = i / 8;
            server(45, "mobs_" + String.format("%02d", idx), s -> gallery(s, batch, false));
        }
        List<MonsterDef> bosses = new ArrayList<>(RpgDefs.BOSSES);
        for (int i = 0; i < bosses.size(); i += 3) {
            List<MonsterDef> batch = bosses.subList(i, Math.min(bosses.size(), i + 3));
            int idx = i / 3;
            server(50, "boss_" + String.format("%02d", idx), s -> gallery(s, batch, true));
        }
        // abilities with effects
        String[][] fx = {{"drake_whelp", "FIRE_BREATH"}, {"frost_troll", "FROST_NOVA"}, {"storm_elemental", "THUNDERSTORM"}, {"ash_golem", "METEOR"},
                {"lich_acolyte", "SHADOW_BOLT"}, {"crystal_spider", "ARCANE_MISSILES"}, {"stonehorn_rhino", "CHARGE"}, {"banshee", "SONIC_SCREAM"},
                {"mushroom_brute", "SPORE_BURST"}, {"dust_devil", "VORTEX_PULL"}, {"swamp_hag", "SUMMON"}, {"gem_golem", "SHOCKWAVE"}};
        for (String[] f : fx) {
            server(1, null, s -> abilitySetup(s, f[0], f[1]));
            server(14, "ability_" + f[0], s -> {});
            server(18, "ability_" + f[0] + "_b", s -> {});
        }
        String[][] bossFx = {{"dragon_queen_vaelith", "METEOR"}, {"lich_king_valdemar", "DARK_PULSE"}, {"demon_lord_malakar", "FIRE_NOVA"}, {"storm_khan_tengrak", "THUNDERSTORM"}};
        for (String[] f : bossFx) {
            server(1, null, s -> abilitySetup(s, f[0], f[1]));
            server(26, "bossfx_" + f[0], s -> {});
            server(20, "bossfx_" + f[0] + "_b", s -> {});
        }
        // people of the world
        server(40, "npcs_men", s -> people(s, false));
        server(40, "npcs_women", s -> people(s, true));
        server(40, "npcs_roles", s -> roles(s));
        // spells and sword skills
        String[] spells = {"fireball", "chain_lightning", "meteor_shower", "frost_nova", "black_hole", "summon_wolves", "arcane_beam", "earth_spikes", "blizzard", "angel_summon", "tidal_wave", "judgement"};
        for (String sp : spells) {
            server(1, null, s -> castSetup(s, sp));
            server(16, "spell_" + sp, s -> {});
        }
        String[] swords = {"flame_tongue", "frostmourne", "stormcaller", "soul_reaper", "void_reaver", "crown_of_kings"};
        for (String sw : swords) {
            server(1, null, s -> swordSetup(s, sw));
            server(14, "sword_" + sw, s -> {});
        }
        // other cultures: build an elven, a dwarven and a demon capital
        for (Kingdom k : new Kingdom[]{Kingdom.SYLVARIEN, Kingdom.KHAZDUR, Kingdom.INFERNAX, Kingdom.YMIRHEIM}) {
            server(1, null, s -> {
                clearArea(s.overworld(), player(s));
                Site c = cap(s, k);
                look(player(s), c.x + 0.5, Math.max(c.y, ground(s.overworld(), c.x, c.z)) + 120, c.z + 120.5, 180, 40);
            });
            until(2400, s -> cap(s, k).built);
            server(120, null, s -> {
                Site c = cap(s, k);
                look(player(s), c.x + 0.5, c.y + 90, c.z + 105.5, 180, 42);
            });
            server(60, "capital_" + k.name().toLowerCase(), s -> {
                Site c = cap(s, k);
                look(player(s), c.x + 0.5, c.y + 90, c.z + 105.5, 180, 42);
            });
            server(40, "street_" + k.name().toLowerCase(), s -> {
                Site c = cap(s, k);
                look(player(s), c.x + 0.5, c.y + 3, c.z + 26.5, 180, 6);
            });
        }
        server(20, "END", s -> {});
    }

    private static Site gallerySite(MinecraftServer s) {
        Site c = cap(s, Kingdom.ALDORIA);
        Site g = new Site();
        g.x = c.x + 260;
        g.z = c.z;
        return g;
    }

    private static void gallery(MinecraftServer s, List<MonsterDef> batch, boolean boss) {
        ServerLevel l = s.overworld();
        ServerPlayer p = player(s);
        Site g = gallerySite(s);
        int y = ground(l, g.x, g.z);
        clearArea(l, p);
        float total = 0;
        for (MonsterDef d : batch) total += Math.max(1.5F, d.hitWidth()) + 1.5F;
        float x = g.x - total / 2;
        float maxH = 0;
        for (MonsterDef d : batch) {
            float w = Math.max(1.5F, d.hitWidth()) + 1.5F;
            EntityType<RpgMonster> t = RpgEntities.typeOf(d.id());
            if (t != null) {
                RpgMonster m = t.create(l);
                if (m != null) {
                    double my = d.flying() ? y + 1.5 : y;
                    m.moveTo(x + w / 2, my, g.z, 0, 0);
                    m.setYRot(0);
                    m.yBodyRot = 0;
                    m.yHeadRot = 0;
                    m.setNoAi(true);
                    m.setInvulnerable(true);
                    m.setPersistenceRequired();
                    l.addFreshEntity(m);
                }
            }
            maxH = Math.max(maxH, d.hitHeight());
            x += w;
        }
        double dist = boss ? Math.max(total * 0.8, maxH * 2.2) + 5 : Math.max(total * 0.62, maxH * 1.6) + 3;
        look(p, g.x + 0.5, y + maxH * 0.55 + 0.5, g.z + dist, 180, 8);
        StringBuilder b = new StringBuilder();
        for (MonsterDef d : batch) b.append(d.id()).append(' ');
        LOG.info("[AUTOTEST] gallery {}", b);
    }

    private static void abilitySetup(MinecraftServer s, String id, String ability) {
        ServerLevel l = s.overworld();
        ServerPlayer p = player(s);
        Site g = gallerySite(s);
        int y = ground(l, g.x, g.z);
        clearArea(l, p);
        Husk h = EntityType.HUSK.create(l);
        if (h == null) return;
        h.moveTo(g.x, y, g.z + 9, 180, 0);
        h.setNoAi(true);
        h.setInvulnerable(true);
        l.addFreshEntity(h);
        EntityType<RpgMonster> t = RpgEntities.typeOf(id);
        if (t == null) return;
        RpgMonster m = t.create(l);
        if (m == null) return;
        m.moveTo(g.x, y + (m.def().flying() ? 2 : 0), g.z - 3 - m.def().hitWidth() / 2, 0, 0);
        l.addFreshEntity(m);
        Ability[] ab = m.def().abilities();
        int idx = 0;
        for (int i = 0; i < ab.length; i++) if (ab[i].name().equals(ability)) idx = i;
        m.forceCast(idx, h);
        float size = m.def().hitHeight();
        look(p, g.x - 9 - size, y + 2 + size * 0.4, g.z + 3, -115, 10);
        LOG.info("[AUTOTEST] ability {} {}", id, ability);
    }

    private static void people(MinecraftServer s, boolean female) {
        ServerLevel l = s.overworld();
        ServerPlayer p = player(s);
        Site g = gallerySite(s);
        int y = ground(l, g.x, g.z);
        clearArea(l, p);
        NpcRole[] roles = {NpcRole.PEASANT, NpcRole.KNIGHT, NpcRole.MAGE, NpcRole.NOBLE, NpcRole.MERCHANT, NpcRole.GUARD, NpcRole.ADVENTURER, NpcRole.PRIEST, NpcRole.RULER};
        Race[] races = Race.values();
        for (int i = 0; i < races.length; i++) {
            RpgNpc n = NpcFactory.spawn(l, new BlockPos(g.x - 12 + i * 3, y, g.z), races[i], female, roles[i], i, null, null);
            if (n != null) { n.setNoAi(true); n.setYRot(0); n.yBodyRot = 0; n.yHeadRot = 0; }
        }
        look(p, g.x + 0.5, y + 2.5, g.z + 13, 180, 6);
    }

    private static void roles(MinecraftServer s) {
        ServerLevel l = s.overworld();
        ServerPlayer p = player(s);
        Site g = gallerySite(s);
        int y = ground(l, g.x, g.z);
        clearArea(l, p);
        NpcRole[] roles = {NpcRole.BEGGAR, NpcRole.WORKER, NpcRole.BLACKSMITH, NpcRole.INNKEEPER, NpcRole.GUILD_MASTER, NpcRole.SLAVER, NpcRole.SLAVE, NpcRole.BANDIT, NpcRole.SOLDIER, NpcRole.CHILD};
        for (int i = 0; i < roles.length; i++) {
            RpgNpc n = NpcFactory.spawn(l, new BlockPos(g.x - 13 + i * 3, y, g.z), Race.HUMAN, i % 2 == 0, roles[i], 0, null, null);
            if (n != null) {
                n.setNoAi(true); n.setYRot(0); n.yBodyRot = 0; n.yHeadRot = 0;
                if (roles[i] == NpcRole.CHILD) n.setAge(RpgNpc.CHILD_TICKS);
            }
        }
        look(p, g.x + 0.5, y + 2.5, g.z + 13, 180, 6);
    }

    private static void castSetup(MinecraftServer s, String spell) {
        ServerLevel l = s.overworld();
        ServerPlayer p = player(s);
        Site g = gallerySite(s);
        int y = ground(l, g.x, g.z);
        clearArea(l, p);
        for (int i = -1; i <= 1; i++) {
            Husk h = EntityType.HUSK.create(l);
            if (h == null) continue;
            h.moveTo(g.x + i * 3, y, g.z - 12, 0, 0);
            h.setNoAi(true);
            l.addFreshEntity(h);
        }
        look(p, g.x + 0.5, y, g.z + 0.5, 180, 4);
        SpellDef sp = RpgDefs.SPELL_BY_ID.get(spell);
        p.setXRot(4);
        p.setYRot(180);
        p.setYHeadRot(180);
        if (sp != null) {
            try {
                SpellCaster.cast(p, sp, 1.5F);
            } catch (Throwable t) {
                LOG.error("[AUTOTEST] spell failed " + spell, t);
            }
        }
        // camera: step aside so the effect is visible
        s.execute(() -> look(p, g.x - 7.5, y + 3, g.z + 3.5, -150, 14));
    }

    private static void swordSetup(MinecraftServer s, String sword) {
        ServerLevel l = s.overworld();
        ServerPlayer p = player(s);
        Site g = gallerySite(s);
        int y = ground(l, g.x, g.z);
        clearArea(l, p);
        for (int i = -1; i <= 1; i++) {
            Husk h = EntityType.HUSK.create(l);
            if (h == null) continue;
            h.moveTo(g.x + i * 2.5, y, g.z - 5, 0, 0);
            h.setNoAi(true);
            l.addFreshEntity(h);
        }
        look(p, g.x + 0.5, y, g.z + 0.5, 180, 4);
        ItemStack st = new ItemStack(RpgItems.sword(sword));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, st);
        if (st.getItem() instanceof RpgSwordItem it) {
            try {
                SwordSkills.use(p, it, it.def.skill(), 1.5F);
            } catch (Throwable t) {
                LOG.error("[AUTOTEST] sword failed " + sword, t);
            }
        }
        s.execute(() -> look(p, g.x - 7.5, y + 3, g.z + 4.5, -140, 16));
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
            LevelSettings settings = new LevelSettings("ktest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ktest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (!inWorld) {
            inWorld = true;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        MinecraftServer s = mc.getSingleplayerServer();
        if (st.screen() == null && !(mc.screen instanceof com.krolasyon.bosses.rpg.client.gui.DialogScreen && st.gui())) {
            if (mc.screen != null) mc.setScreen(null);
        }
        mc.options.hideGui = !st.gui();
        if (tick == stepStart) {
            if (st.action() != null) s.execute(() -> {
                try {
                    st.action().accept(s);
                } catch (Throwable t) {
                    errors++;
                    LOG.error("[AUTOTEST] step failed", t);
                }
            });
            if (st.screen() != null) mc.setScreen(st.screen().get());
            condDone = false;
        }
        if (st.until() != null) {
            if (!condDone) {
                condDone = true;
                s.execute(() -> {
                    try {
                        condResult = st.until().test(s);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] condition failed", t);
                        condResult = true;
                    }
                    condDone = false;
                });
            }
            if (condResult || tick - stepStart >= st.delay()) {
                if (!condResult) LOG.warn("[AUTOTEST] condition timed out at step {}", stepIndex);
                condResult = false;
                stepIndex++;
                stepStart = tick + 1;
            }
            return;
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished errors={}", errors);
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ktest_" + String.format("%03d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
