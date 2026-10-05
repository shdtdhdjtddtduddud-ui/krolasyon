package com.sololeveling.event;

import com.sololeveling.SoloLeveling;
import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModDimensions;
import com.sololeveling.registry.ModEntities;
import com.sololeveling.system.NewsManager;
import com.sololeveling.system.PlayerSync;
import com.sololeveling.system.Sys;
import com.sololeveling.world.Regions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class WorldEvents {
    private WorldEvents() {}

    /** entering a region of the Hunter World discovers it (unlocks fast travel) */
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || sp.tickCount % 40 != 0) return;
        if (sp.level().dimension() != ModDimensions.HUNTER_WORLD) return;
        Content.RegionDef r = Regions.at((int) sp.getX(), (int) sp.getZ());
        if (r == null) return;
        SLPlayer d = ModCaps.get(sp);
        if (d.discovered.add(r.id())) {
            Sys.notify(sp, Sys.INFO, Component.translatable("gui.sololeveling.region_discovered"), Component.translatable("region.sololeveling." + r.id()));
            NewsManager.add(sp.getServer(), NewsManager.GENERAL, Component.translatable("news.sololeveling.discovered", sp.getDisplayName(), Component.translatable("region.sololeveling." + r.id())));
            PlayerSync.sync(sp);
        }
    }

    /** no natural hostile spawns inside the cities */
    @SubscribeEvent
    public static void check(MobSpawnEvent.PositionCheck e) {
        if (e.getLevel().getLevel().dimension() != ModDimensions.HUNTER_WORLD) return;
        if (e.getSpawnType() != MobSpawnType.NATURAL && e.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        if (!(e.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)) return;
        if (Regions.at((int) e.getX(), (int) e.getZ()) != null) e.setResult(Event.Result.DENY);
    }

    @Mod.EventBusSubscriber(modid = SoloLeveling.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void placements(SpawnPlacementRegisterEvent e) {
            for (String id : new String[]{"goblin", "orc", "hell_hound"}) {
                e.register(ModEntities.MONSTERS.get(id).get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (type, level, reason, pos, rnd) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, rnd) && Regions.at(pos.getX(), pos.getZ()) == null,
                        SpawnPlacementRegisterEvent.Operation.REPLACE);
            }
        }
    }
}
