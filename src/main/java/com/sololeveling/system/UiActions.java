package com.sololeveling.system;

import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModBlocks;
import com.sololeveling.registry.ModDimensions;
import com.sololeveling.registry.ModItems;
import com.sololeveling.world.GateManager;
import com.sololeveling.world.Regions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Server side handler of every button the player can press in the System / NPC screens. */
public final class UiActions {
    private UiActions() {}

    public static void handle(ServerPlayer sp, String action, String arg, int n) {
        SLPlayer d = ModCaps.get(sp);
        switch (action) {
            case "stat" -> {
                int idx;
                try { idx = Integer.parseInt(arg); } catch (NumberFormatException e) { return; }
                if (idx < 0 || idx > 4) return;
                int amt = Math.max(1, Math.min(n, d.points));
                if (d.points <= 0) return;
                d.stats[idx] += amt;
                d.points -= amt;
                Stats.apply(sp);
                PlayerSync.sync(sp);
            }
            case "claim_daily" -> Quests.claimDaily(sp);
            case "accept_quest" -> Quests.accept(sp, n);
            case "claim_quest" -> Quests.claim(sp, n);
            case "abandon_quest" -> Quests.abandon(sp, n);
            case "refresh_offers" -> Quests.refreshOffers(sp);
            case "travel" -> travel(sp, d, arg);
            case "guild_join" -> {
                if (!Guilds.valid(arg)) return;
                if (d.level < Guilds.JOIN_LEVEL) { Sys.warn(sp, "gui.sololeveling.guild_level", Guilds.JOIN_LEVEL); return; }
                if (!d.guild.isEmpty()) { Sys.warn(sp, "gui.sololeveling.guild_already"); return; }
                if (d.gold < Guilds.JOIN_COST) { Sys.warn(sp, "gui.sololeveling.no_gold"); return; }
                d.gold -= Guilds.JOIN_COST;
                d.guild = arg;
                Stats.apply(sp);
                Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.guild_joined"), Component.translatable("guild.sololeveling." + arg));
                NewsManager.add(sp.getServer(), NewsManager.HUNTER, Component.translatable("news.sololeveling.guild_join", sp.getDisplayName(), Component.translatable("guild.sololeveling." + arg)));
                PlayerSync.sync(sp);
            }
            case "guild_leave" -> {
                if (d.guild.isEmpty()) return;
                d.guild = "";
                Stats.apply(sp);
                PlayerSync.sync(sp);
            }
            case "heal" -> {
                long cost = 50 + d.level * 5L;
                if (d.gold < cost) { Sys.warn(sp, "gui.sololeveling.no_gold"); return; }
                d.gold -= cost;
                sp.setHealth(sp.getMaxHealth());
                d.mana = d.maxMana();
                d.fatigue = 0;
                sp.removeAllEffects();
                Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.healed"));
                PlayerSync.sync(sp);
            }
            case "buy" -> {
                Integer price = Shop.PRICES.get(arg);
                if (price == null) return;
                int cnt = Math.max(1, Math.min(n, 16));
                long total = (long) price * cnt;
                if (d.gold < total) { Sys.warn(sp, "gui.sololeveling.no_gold"); return; }
                d.gold -= total;
                ItemStack st = new ItemStack(ModItems.get(arg), cnt);
                if (!sp.getInventory().add(st)) sp.drop(st, false);
                Sys.notify(sp, Sys.INFO, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.bought", cnt, Component.translatable("item.sololeveling." + arg), total));
                PlayerSync.sync(sp);
            }
            case "gates" -> {
                Net.toPlayer(sp, new Packets.Gates(GateManager.activeTag(sp.level().dimension().location().toString())));
            }
            case "news" -> NewsManager.send(sp);
            case "register" -> {
                if (sp.getInventory().countItem(ModItems.get("hunter_license")) == 0) {
                    sp.getInventory().add(new ItemStack(ModItems.get("hunter_license")));
                    Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.license_given"));
                } else Sys.info(sp, "gui.sololeveling.license_have");
            }
            case "sync" -> PlayerSync.sync(sp);
            default -> { }
        }
    }

    private static boolean padNear(ServerPlayer sp, int r) {
        BlockPos c = sp.blockPosition();
        var pad = ModBlocks.get("teleport_pad");
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r)))
            if (sp.level().getBlockState(p).is(pad)) return true;
        return false;
    }

    private static void travel(ServerPlayer sp, SLPlayer d, String id) {
        ServerLevel cur = sp.serverLevel();
        if (cur.dimension() == ModDimensions.DUNGEON) { Sys.warn(sp, "gui.sololeveling.no_travel_dungeon"); return; }
        boolean pad = padNear(sp, 6);
        boolean map = sp.getMainHandItem().getItem() == ModItems.get("world_map") || sp.getOffhandItem().getItem() == ModItems.get("world_map");
        if (!pad && !map) { Sys.warn(sp, "gui.sololeveling.need_pad"); return; }
        long cost = pad ? 0 : 100;
        if (d.gold < cost) { Sys.warn(sp, "gui.sololeveling.no_gold"); return; }
        if (id.equals("overworld")) {
            ServerLevel ow = sp.getServer().overworld();
            BlockPos sp0 = ow.getSharedSpawnPos();
            int y = Travel.surfaceY(ow, sp0.getX(), sp0.getZ());
            d.gold -= cost;
            Travel.teleport(sp, ow, sp0.getX() + 0.5, y, sp0.getZ() + 0.5, 0F);
            PlayerSync.sync(sp);
            return;
        }
        Content.RegionDef r = Regions.find(id);
        if (r == null) return;
        if (!d.discovered.contains(id) && !id.equals("seoul")) { Sys.warn(sp, "gui.sololeveling.region_unknown"); return; }
        ServerLevel hw = sp.getServer().getLevel(ModDimensions.HUNTER_WORLD);
        if (hw == null) { Sys.warn(sp, "gui.sololeveling.no_dimension"); return; }
        d.gold -= cost;
        double[] a = Regions.arrival(r);
        Travel.teleport(sp, hw, a[0], a[1], a[2], 180F);
        d.discovered.add(id);
        sp.setRespawnPosition(ModDimensions.HUNTER_WORLD, BlockPos.containing(a[0], a[1], a[2]), 180F, true, false);
        Sys.notify(sp, Sys.INFO, Component.translatable("gui.sololeveling.region_arrived"), Component.translatable("region.sololeveling." + id));
        PlayerSync.sync(sp);
    }
}
