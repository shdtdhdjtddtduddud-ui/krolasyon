package com.sololeveling.event;

import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.*;
import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.*;
import com.sololeveling.skill.SkillExec;
import com.sololeveling.system.Travel;
import com.sololeveling.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Headless server self-test, enabled with -Dsololeveling.autotest=true (used by CI only). */
@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class AutoTest {
    private AutoTest() {}

    private static final Logger LOG = LogManager.getLogger("AUTOTEST");
    private static final boolean ON = Boolean.getBoolean("sololeveling.autotest");
    private static int tick = 0;
    private static int step = 0;
    private static int fails = 0;

    private static void ok(String s) { LOG.info("[OK] " + s); }
    private static void fail(String s, Throwable t) {
        fails++;
        LOG.error("[FAIL] " + s + (t == null ? "" : " :: " + t));
        if (t != null) t.printStackTrace();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        tick++;
        if (tick < 40 || tick % 5 != 0) return;
        try {
            switch (step++) {
                case 0 -> entities(server);
                case 1 -> hunterWorld(server);
                case 2 -> dungeons(server);
                case 3 -> player(server);
                case 4 -> {
                    LOG.info("=== AUTOTEST DONE fails=" + fails);
                    server.halt(false);
                }
                default -> { }
            }
        } catch (Throwable t) {
            fail("step " + (step - 1), t);
        }
    }

    private static void entities(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        BlockPos at = ow.getSharedSpawnPos().above(20);
        int n = 0;
        for (var o : ModEntities.ENTITIES.getEntries()) {
            EntityType<?> t = o.get();
            try {
                Entity en = t.create(ow);
                if (en == null) { fail("create " + o.getId(), null); continue; }
                en.setPos(at.getX(), at.getY(), at.getZ());
                if (en instanceof LivingEntity le) {
                    float hp = le.getMaxHealth();
                    if (hp <= 0) fail("hp " + o.getId(), null);
                }
                if (en instanceof GateEntity g) g.setup("E", "goblin_cave", false, GateEntity.ENTRANCE);
                if (!(en instanceof GateEntity)) { ow.addFreshEntity(en); en.discard(); }
                n++;
            } catch (Throwable ex) { fail("entity " + o.getId(), ex); }
        }
        ok("entity types created: " + n);
        for (var o : ModItems.ITEMS.getEntries()) o.get();
        ok("items: " + ModItems.ITEMS.getEntries().size() + " blocks: " + ModBlocks.BLOCKS.getEntries().size());
    }

    private static void hunterWorld(MinecraftServer server) {
        ServerLevel hw = server.getLevel(ModDimensions.HUNTER_WORLD);
        if (hw == null) { fail("hunter_world dimension missing", null); return; }
        ok("hunter_world loaded");
        long t0 = System.currentTimeMillis();
        for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 5; cz++) hw.getChunk(cx, cz);
        ok("seoul chunks generated in " + (System.currentTimeMillis() - t0) + "ms");
        BlockPos pad = new BlockPos(4, ModDimensions.CITY_Y + 1, 44);
        if (hw.getBlockState(pad).is(ModBlocks.get("teleport_pad"))) ok("teleport pad present");
        else fail("teleport pad missing, found " + hw.getBlockState(pad), null);
        int npcs = hw.getEntitiesOfClass(NpcEntity.class, new net.minecraft.world.phys.AABB(-60, 60, -60, 90, 100, 90)).size();
        if (npcs > 5) ok("npcs spawned: " + npcs); else fail("npcs too few: " + npcs, null);
        if (!hw.getBlockState(new BlockPos(3, ModDimensions.CITY_Y, 3)).isAir()) ok("city ground present");
        GateEntity g = GateManager.spawnGate(hw, 10.5, ModDimensions.CITY_Y + 1, 60.5, "D", "temple", false, true);
        if (g != null) ok("gate spawned in hunter world");
        // count tall buildings
        int tall = 0;
        for (int x = -60; x < 60; x += 5) for (int z = -60; z < 60; z += 5) if (hw.getBlockState(new BlockPos(x, ModDimensions.CITY_Y + 30, z)).is(Blocks.AIR) == false) tall++;
        ok("blocks above y+30 sample count: " + tall);
    }

    private static void dungeons(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        ServerLevel dl = server.getLevel(ModDimensions.DUNGEON);
        if (dl == null) { fail("dungeon dimension missing", null); return; }
        for (Content.ThemeDef t : Content.THEMES) {
            try {
                String rank = t.ranks().length() > 0 ? t.ranks().substring(0, 1) : "E";
                GateEntity g = ModEntities.GATE.get().create(ow);
                g.moveTo(0.5, 100, 0.5);
                g.setup(rank, t.id(), false, GateEntity.ENTRANCE);
                long t0 = System.currentTimeMillis();
                DungeonManager.Inst inst = DungeonManager.ensure(ow, g);
                if (inst == null) { fail("dungeon " + t.id() + " null", null); continue; }
                int mobs = dl.getEntitiesOfClass(SLMonster.class, new net.minecraft.world.phys.AABB(inst.minX, 0, inst.minZ, inst.maxX, 200, inst.maxZ)).size();
                boolean solid = !dl.getBlockState(inst.spawn.below()).isAir();
                if (mobs > 3 && solid) ok("dungeon " + t.id() + " built in " + (System.currentTimeMillis() - t0) + "ms, mobs=" + mobs + " floor=" + dl.getBlockState(inst.spawn.below()).getBlock());
                else fail("dungeon " + t.id() + " mobs=" + mobs + " solid=" + solid, null);
            } catch (Throwable ex) { fail("dungeon " + t.id(), ex); }
        }
    }

    private static void player(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        FakePlayer fp = FakePlayerFactory.getMinecraft(ow);
        fp.setPos(0, 100, 0);
        SLPlayer d = ModCaps.get(fp);
        d.level = 100;
        for (Content.SkillDef s : Content.SKILLS) d.skills.add(s.id());
        d.mana = 100000;
        d.stats[3] = 500;
        int ok = 0;
        for (Content.SkillDef s : Content.SKILLS) {
            try { SkillExec.cast(fp, s.id()); ok++; } catch (Throwable ex) { fail("skill " + s.id(), ex); }
        }
        ok("skills executed: " + ok);
        // shadow extraction with a corpse
        try {
            SLMonster m = ModEntities.MONSTERS.get("goblin").get().create(ow);
            m.moveTo(fp.getX() + 2, fp.getY(), fp.getZ());
            ow.addFreshEntity(m);
            com.sololeveling.system.Corpses.add(m);
            d.skills.add("shadow_extraction");
            for (int i = 0; i < 12; i++) { fp.getPersistentData(); SkillExec.cast(fp, "shadow_extraction"); }
            int sh = ow.getEntitiesOfClass(ShadowEntity.class, fp.getBoundingBox().inflate(30)).size();
            ok("shadow extraction ran, shadows=" + sh);
        } catch (Throwable ex) { fail("extraction", ex); }
        // quests
        try {
            com.sololeveling.system.Quests.checkDaily(fp);
            ok("daily quest ok");
        } catch (Throwable ex) { fail("quests", ex); }
        // news
        try {
            com.sololeveling.system.NewsManager.add(server, 0, net.minecraft.network.chat.Component.literal("autotest"));
            ok("news ok");
        } catch (Throwable ex) { fail("news", ex); }
        // economy / actions / xp / advancements / dungeon clear
        try {
            com.sololeveling.system.XpHandler.giveXp(fp, 1_000_000L);
            ok("xp -> level " + d.level);
            com.sololeveling.system.Quests.refreshOffers(fp);
            com.sololeveling.system.Quests.accept(fp, 0);
            d.gold = 100000;
            for (String[] a : new String[][]{{"stat", "0", "5"}, {"buy", "hp_potion_small", "2"}, {"heal", "", "0"}, {"guild_join", "hunters", "0"}, {"recall", "", "0"}, {"gates", "", "0"}, {"news", "", "0"}, {"register", "", "0"}, {"claim_quest", "", "0"}}) {
                try { com.sololeveling.system.UiActions.handle(fp, a[0], a[1], Integer.parseInt(a[2])); } catch (Throwable ex) { fail("action " + a[0], ex); }
            }
            ok("ui actions ran, gold=" + d.gold + " guild=" + d.guild);
            GateEntity tg = ModEntities.GATE.get().create(ow);
            tg.moveTo(30.5, 100, 30.5);
            tg.setup("D", "temple", false, GateEntity.ENTRANCE);
            DungeonManager.Inst inst = DungeonManager.ensure(ow, tg);
            ServerLevel dl = server.getLevel(ModDimensions.DUNGEON);
            fp.setPos(inst.boss.getX(), inst.boss.getY(), inst.boss.getZ());
            for (SLMonster m : dl.getEntitiesOfClass(SLMonster.class, new net.minecraft.world.phys.AABB(inst.minX, 0, inst.minZ, inst.maxX, 200, inst.maxZ))) {
                if (m.def.boss()) { m.kill(); ok("boss killed -> cleared=" + inst.cleared); }
            }
        } catch (Throwable ex) { fail("economy/clear", ex); }
        // gate enter/exit with fake player
        try {
            GateEntity g = GateManager.spawnGate(ow, 5, ow.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 5, 5), 5, "E", "goblin_cave", false, false);
            GateManager.enter(fp, g);
            ok("gate enter -> dim " + fp.level().dimension().location());
        } catch (Throwable ex) { fail("gate enter", ex); }
    }
}
