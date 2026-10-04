package com.krolasyon.bosses.rpg;

import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.magic.SpellCaster;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.npc.NpcFactory;
import com.krolasyon.bosses.rpg.npc.NpcRole;
import com.krolasyon.bosses.rpg.npc.RpgNpc;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.town.TownManager;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/** Dedicated-server smoke test for CI (-Dkrolasyon.servertest=true): builds towns on real terrain, spawns everything, then stops. */
public final class ServerSelfTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private int phase;
    private int phaseStart;
    private int errors;

    public static void init() {
        LOG.info("[SERVERTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new ServerSelfTest());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = e.getServer();
        ServerLevel l = s.overworld();
        RpgWorldData w = RpgWorldData.get(s);
        tick++;
        try {
            switch (phase) {
                case 0 -> {
                    if (tick < 60 || !w.initialized) return;
                    LOG.info("[SERVERTEST] world ready, {} sites", w.sites.size());
                    for (Kingdom k : new Kingdom[]{Kingdom.ALDORIA, Kingdom.KHAZDUR}) TownManager.enqueue(l, WorldMap.capital(w, k.ordinal()), w);
                    for (Site x : w.sites) if (x.type == Site.Type.VILLAGE && x.kingdom == Kingdom.ALDORIA.ordinal()) { TownManager.enqueue(l, x, w); break; }
                    for (Site x : w.sites) if (x.type == Site.Type.LAIR) { TownManager.enqueue(l, x, w); break; }
                    for (Site x : w.sites) if (x.type == Site.Type.CAMP) { TownManager.enqueue(l, x, w); break; }
                    next();
                }
                case 1 -> {
                    if (tick % 200 == 0) LOG.info("[SERVERTEST] building... jobs={}", TownManager.jobCount());
                    if (TownManager.jobCount() > 0 && tick - phaseStart < 12000) return;
                    for (Site x : w.sites) if (x.built) {
                        int n = l.getEntitiesOfClass(RpgNpc.class, new AABB(x.x - 110, -64, x.z - 110, x.x + 110, 320, x.z + 110)).size();
                        LOG.info("[SERVERTEST] built {} '{}' {} y={} ground={} npcs={}", x.id, x.name, x.type, x.y, com.krolasyon.bosses.rpg.util.Heights.ground(l, x.x, x.z), n);
                    }
                    LOG.info("[SERVERTEST] anchors {}", w.anchors);
                    next();
                }
                case 2 -> {
                    Site c = WorldMap.capital(w, Kingdom.ALDORIA.ordinal());
                    BlockPos base = new BlockPos(c.x + 200, 0, c.z + 200);
                    int y = com.krolasyon.bosses.rpg.util.Heights.ground(l, base.getX(), base.getZ());
                    int i = 0;
                    for (MonsterDef d : RpgDefs.BY_ID.values()) {
                        EntityType<RpgMonster> t = RpgEntities.typeOf(d.id());
                        if (t == null) { errors++; LOG.error("[SERVERTEST] missing type {}", d.id()); continue; }
                        BlockPos p = base.offset((i % 16) * 6, 0, (i / 16) * 6);
                        t.spawn(l, new BlockPos(p.getX(), com.krolasyon.bosses.rpg.util.Heights.ground(l, p.getX(), p.getZ()), p.getZ()), MobSpawnType.COMMAND);
                        i++;
                    }
                    for (NpcRole r : NpcRole.values()) for (Race race : Race.values()) {
                        BlockPos p = base.offset(-20 - race.ordinal() * 3, 0, r.ordinal() * 3);
                        NpcFactory.spawn(l, new BlockPos(p.getX(), com.krolasyon.bosses.rpg.util.Heights.ground(l, p.getX(), p.getZ()), p.getZ()), race, r.ordinal() % 2 == 0, r, race.ordinal(), null, null);
                    }
                    LOG.info("[SERVERTEST] spawned {} monsters and {} npcs near {} {}", i, NpcRole.values().length * Race.values().length, base, y);
                    next();
                }
                case 3 -> {
                    if (tick - phaseStart == 40) {
                        // every spell, cast by a mage
                        Site c = WorldMap.capital(w, Kingdom.ALDORIA.ordinal());
                        BlockPos p = new BlockPos(c.x + 200, 0, c.z + 260);
                        RpgNpc mage = NpcFactory.spawn(l, new BlockPos(p.getX(), com.krolasyon.bosses.rpg.util.Heights.ground(l, p.getX(), p.getZ()), p.getZ()), Race.ELF, true, NpcRole.MAGE, 1, null, null);
                        if (mage != null) for (SpellDef sp : RpgDefs.SPELLS) {
                            try { SpellCaster.cast(mage, sp, 1.0F); } catch (Exception ex) { errors++; LOG.error("[SERVERTEST] spell " + sp.id(), ex); }
                        }
                        LOG.info("[SERVERTEST] cast {} spells", RpgDefs.SPELLS.size());
                    }
                    if (tick - phaseStart < 600) return;
                    int mobs = 0, npcs = 0;
                    for (Entity en : l.getAllEntities()) { if (en instanceof RpgMonster) mobs++; if (en instanceof RpgNpc) npcs++; }
                    LOG.info("[SERVERTEST] after 30s: monsters alive={} npcs alive={}", mobs, npcs);
                    next();
                }
                default -> {
                    LOG.info("[SERVERTEST] finished errors={}", errors);
                    s.halt(false);
                    phase = -100;
                }
            }
        } catch (Exception ex) {
            errors++;
            LOG.error("[SERVERTEST] phase " + phase + " failed", ex);
            next();
        }
    }

    private void next() {
        phase++;
        phaseStart = tick;
    }
}
