package com.krolasyon.bosses.rpg;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.mob.RegionSpawner;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.npc.*;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.story.Story;
import com.krolasyon.bosses.rpg.town.TownManager;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.util.Tasks;
import com.krolasyon.bosses.rpg.util.TempBlocks;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RpgEvents {
    private RpgEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent e) {
        RpgWorldData d = RpgWorldData.get(e.getServer());
        WorldMap.init(e.getServer().overworld(), d);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent e) {
        TempBlocks.flush();
        Tasks.clear();
        TownManager.clear();
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel sl)) return;
        Tasks.tick(sl);
        TempBlocks.tick(sl);
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        RpgWorldData w = RpgWorldData.get(sl);
        if (!w.initialized) return;
        TownManager.tick(sl, w);
        if (sl.getGameTime() % 20 == 0) TownManager.scan(sl, w);
        long day = sl.getDayTime() / 24000L;
        if (w.day != day) {
            if (w.day >= 0) WarLogic.daily(sl, w);
            w.day = day;
            w.setDirty();
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        RpgWorldData w = RpgWorldData.get(p.server);
        if (!w.initialized) return;
        PlayerMagic.tick(p);
        PlayerRpg d = RpgWorldData.player(p);
        if (p.tickCount % 20 == 0) {
            Story.tick(p, d);
            Fate.tick(p, d, w);
            RegionId region = WorldMap.regionAt(p.serverLevel(), p.blockPosition());
            Site site = WorldMap.siteAt(w, p.getX(), p.getZ());
            String label = site != null ? site.name : region.title;
            long lastTitle = p.getPersistentData().getLong("KrolasyonRegionTitle");
            if (!label.equals(d.lastRegion) && p.level().getGameTime() - lastTitle > 200) {
                p.getPersistentData().putLong("KrolasyonRegionTitle", p.level().getGameTime());
                d.lastRegion = label;
                String sub = site != null ? (site.kingdom >= 0 ? Kingdom.of(site.kingdom).title + " • " : "") + site.type.title
                        : (region.wild() ? "Vahşi Topraklar • Tehlike " + "☠".repeat(region.danger) : Kingdom.of(region.kingdom).title);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 40, 15));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("§7" + sub)));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("§e" + label)));
                PlayerMagic.sync(p);
            }
        }
        if (p.tickCount % 120 == 0 && p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) RegionSpawner.tick(p);
        if (p.tickCount % 3600 == 1800 && p.getRandom().nextInt(4) == 0 && d.originDone) WarLogic.patrols(p, w, d);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpg d = RpgWorldData.player(p);
        PlayerMagic.applyAttributes(p, d);
        PlayerMagic.sync(p);
        if (d.originDone) p.sendSystemMessage(Component.literal("§6Krolasyon §7— " + Story.chapterTitle(d) + ": §f" + Story.objective(d)));
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        PlayerRpg d = RpgWorldData.player(p);
        PlayerMagic.applyAttributes(p, d);
        p.setHealth(p.getMaxHealth());
        PlayerMagic.sync(p);
    }

    @Nullable
    private static ServerPlayer credit(@Nullable Entity killer, ServerLevel level) {
        if (killer instanceof ServerPlayer p) return p;
        if (killer == null) return null;
        java.util.UUID u = Combat.leaderOf(killer);
        return u == null ? null : level.getServer().getPlayerList().getPlayer(u);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (!(v.level() instanceof ServerLevel sl)) return;
        if (v instanceof RpgMonster m) {
            if (m.isBoss()) TownManager.onBossKilled(sl, m);
            ServerPlayer p = credit(e.getSource().getEntity(), sl);
            if (p == null) return;
            PlayerRpg d = RpgWorldData.player(p);
            MonsterDef def = m.def();
            long xp = (long) ((def.hp() / 2 + def.atk() * 2) * (m.isSmall() ? 0.3F : 1.0F) * (def.boss() ? 3 : 1));
            int lv = d.addXp(xp);
            d.kills++;
            if (d.guild >= 0 && d.addGuildXp(def.boss() ? 150 : 1)) p.sendSystemMessage(Component.literal("§6§lLonca rütben yükseldi: " + d.guildRank() + " sınıfı!"));
            if (def.boss()) {
                d.bossKills++;
                d.fame += 25;
                int k = WorldMap.kingdomAt(RpgWorldData.get(sl), v.getX(), v.getZ());
                if (k >= 0) d.addRep(k, 120);
                for (int i = 0; i < Kingdom.COUNT; i++) d.addRep(i, 15);
                RpgWorldData.get(sl).news(p.getName().getString() + ", efsanevi " + def.name() + " canavarını yendi!");
                p.server.getPlayerList().broadcastSystemMessage(Component.literal("§6§l⚔ " + p.getName().getString() + ", " + def.name() + " canavarını yendi!"), false);
            }
            NpcQuests.onKill(p, def, false);
            Story.onKill(p, d, def, WorldMap.regionAt(sl, v.blockPosition()));
            if (lv > 0) {
                p.sendSystemMessage(Component.literal("§e§l★ Seviye atladın! Seviye " + d.level + " §7(+" + lv * 3 + " stat puanı — K tuşu ile dağıt)"));
                FX.column(sl, ParticleTypes.TOTEM_OF_UNDYING, p.position(), 2.2, 0.8, 40);
                p.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, p.getSoundSource(), 1.0F, 1.0F);
                PlayerMagic.applyAttributes(p, d);
            }
            if (xp > 0) p.displayClientMessage(Component.literal("§a+" + xp + " TP"), true);
            PlayerMagic.sync(p);
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide()) return;
        if (v.hasEffect(RpgEffects.DIVINE_SHIELD.get())) {
            e.setAmount(e.getAmount() * 0.15F);
            FX.sphere(v.level(), ParticleTypes.END_ROD, v.position().add(0, 1, 0), 1.2, 12);
        }
        Entity src = e.getSource().getEntity();
        if (src instanceof LivingEntity att) {
            if (att.hasEffect(RpgEffects.BLOOD_PACT.get())) att.heal(e.getAmount() * 0.25F);
            if (v.hasEffect(RpgEffects.THORN_AURA.get()) && e.getSource().getDirectEntity() == att && att != v) {
                att.hurt(v.damageSources().thorns(v), 2.0F);
            }
        }
    }

    // ------------------------------------------------------------------ commands
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("rpg").requires(s -> s.hasPermission(2))
                .then(Commands.literal("level").then(Commands.argument("n", IntegerArgumentType.integer(1, 100)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PlayerRpg d = RpgWorldData.player(p);
                    int n = IntegerArgumentType.getInteger(c, "n");
                    d.statPoints += Math.max(0, n - d.level) * 3;
                    d.level = n;
                    PlayerMagic.applyAttributes(p, d);
                    PlayerMagic.sync(p);
                    return 1;
                })))
                .then(Commands.literal("spells").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PlayerRpg d = RpgWorldData.player(p);
                    for (SpellDef s : RpgDefs.SPELLS) if (!d.spells.contains(s.id())) d.spells.add(s.id());
                    d.mana = d.maxMana();
                    PlayerMagic.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal(RpgDefs.SPELLS.size() + " büyü öğrenildi."), false);
                    return 1;
                }))
                .then(Commands.literal("mob").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(RpgDefs.BY_ID.keySet(), b)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            EntityType<?> t = RpgEntities.typeOf(StringArgumentType.getString(c, "id"));
                            if (t != null) t.spawn(p.serverLevel(), p.blockPosition().offset(3, 0, 3), MobSpawnType.COMMAND);
                            return 1;
                        })))
                .then(Commands.literal("sites").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgWorldData w = RpgWorldData.get(p.server);
                    java.util.List<Site> l = new java.util.ArrayList<>(w.sites);
                    l.sort(java.util.Comparator.comparingDouble(s -> s.distSq(p.getX(), p.getZ())));
                    for (int i = 0; i < Math.min(12, l.size()); i++) {
                        Site s = l.get(i);
                        p.sendSystemMessage(Component.literal("§e" + s.id + " §f" + s.name + " §7(" + s.type.title + ") " + s.x + ", " + s.z + (s.built ? " §a✔" : "")));
                    }
                    return 1;
                }))
                .then(Commands.literal("tp").then(Commands.argument("site", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(RpgWorldData.get(c.getSource().getServer()).sites.stream().map(s -> s.id), b)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Site s = RpgWorldData.get(p.server).site(StringArgumentType.getString(c, "site"));
                            if (s == null) return 0;
                            int y = com.krolasyon.bosses.rpg.util.Heights.ground(p.serverLevel(), s.x + 4, s.z + 4);
                            p.teleportTo(p.serverLevel(), s.x + 4.5, Math.max(y, s.y == Integer.MIN_VALUE ? y : s.y + 1), s.z + 4.5, p.getYRot(), p.getXRot());
                            return 1;
                        })))
                .then(Commands.literal("story").then(Commands.argument("chapter", IntegerArgumentType.integer(0, 9)).then(Commands.argument("step", IntegerArgumentType.integer(0, 9)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PlayerRpg d = RpgWorldData.player(p);
                    d.chapter = IntegerArgumentType.getInteger(c, "chapter");
                    d.step = IntegerArgumentType.getInteger(c, "step");
                    d.originDone = true;
                    PlayerMagic.sync(p);
                    return 1;
                }))))
                .then(Commands.literal("coins").then(Commands.argument("copper", IntegerArgumentType.integer(1, 100000)).executes(c -> {
                    NpcQuests.giveCoins(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "copper"));
                    return 1;
                })))
                .then(Commands.literal("rep").then(Commands.argument("kingdom", IntegerArgumentType.integer(0, Kingdom.COUNT - 1)).then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    RpgWorldData.player(p).addRep(IntegerArgumentType.getInteger(c, "kingdom"), IntegerArgumentType.getInteger(c, "amount"));
                    PlayerMagic.sync(p);
                    return 1;
                }))))
                .then(Commands.literal("npc").then(Commands.argument("role", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(NpcRole.values()).map(Enum::name), b)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            NpcRole role;
                            try { role = NpcRole.valueOf(StringArgumentType.getString(c, "role").toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException ex) { return 0; }
                            int k = Math.max(0, WorldMap.kingdomAt(RpgWorldData.get(p.server), p.getX(), p.getZ()));
                            Kingdom kk = Kingdom.of(k);
                            NpcFactory.spawn(p.serverLevel(), p.blockPosition().offset(2, 0, 2), kk.citizens()[p.getRandom().nextInt(kk.citizens().length)], p.getRandom().nextBoolean(), role, k, null, null);
                            return 1;
                        })))
        );
    }
}
