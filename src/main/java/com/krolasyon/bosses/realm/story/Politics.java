package com.krolasyon.bosses.realm.story;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.entity.EnvoyEntity;
import com.krolasyon.bosses.realm.net.RealmNet;
import com.krolasyon.bosses.realm.registry.RealmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Reputation, tributes, quests, alliances and conquests of the five kingdoms. */
public final class Politics {
    private Politics() {}

    // ------------------------------------------------------------------ envoy
    public static void openEnvoy(ServerPlayer p, EnvoyEntity env) {
        RealmData d = RealmData.get(p);
        Faction f = env.faction();
        if (!d.metEnvoy) {
            d.metEnvoy = true;
            Story.update(p, d);
        }
        RealmNet.openEnvoy(p, env.getId(), f.ordinal(), greeting(d, f));
    }

    public static Component greeting(RealmData d, Faction f) {
        if (d.ruler) return Component.translatable("dialog.krolasyonbosses.ruler");
        if (d.conquered[f.ordinal()]) return Component.translatable("dialog.krolasyonbosses.conquered");
        return Component.translatable("dialog.krolasyonbosses." + f.id + "." + d.standing(f).tier());
    }

    public static void envoyAction(ServerPlayer p, int entityId, int action) {
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof EnvoyEntity env) || !env.isAlive() || env.distanceToSqr(p) > 100) return;
        Faction f = env.faction();
        RealmData d = RealmData.get(p);
        Component reply = switch (action) {
            case RealmNet.EnvoyAction.TRIBUTE -> tribute(p, d, f);
            case RealmNet.EnvoyAction.QUEST -> quest(p, d, f);
            case RealmNet.EnvoyAction.ALLIANCE -> alliance(p, d, f);
            default -> greeting(d, f);
        };
        env.gesture();
        Story.update(p, d);
        RealmNet.openEnvoy(p, env.getId(), f.ordinal(), reply);
    }

    static int tributeValue(Faction f, ItemStack s) {
        if (s.is(Items.GOLD_INGOT)) return 1;
        if (s.is(Items.GOLD_BLOCK)) return 9;
        if (s.is(Items.EMERALD)) return 2;
        if (s.is(Items.DIAMOND)) return 4;
        if (s.is(Items.NETHERITE_INGOT)) return 20;
        if (s.is(RealmItems.TYRANT_HEART.get())) return 25;
        for (Faction g : Faction.ALL) if (f.atWarWith(g) && s.is(RealmItems.essence(g))) return 2;
        return 0;
    }

    static Component tribute(ServerPlayer p, RealmData d, Faction f) {
        ItemStack s = p.getMainHandItem();
        int v = tributeValue(f, s);
        if (v <= 0) return Component.translatable("dialog.krolasyonbosses.tribute_bad");
        int room = 100 - d.rep(f);
        int take = Math.max(1, Math.min(s.getCount(), (Math.min(40, room) + v - 1) / v));
        int gain = Math.min(room, take * v);
        s.shrink(take);
        d.addRep(f, gain);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);
        return Component.translatable("dialog.krolasyonbosses.tribute_ok", gain);
    }

    static Component quest(ServerPlayer p, RealmData d, Faction f) {
        if (d.questGiver == f.ordinal() && d.questProgress >= RealmData.QUEST_NEED) {
            d.questGiver = -1;
            d.questTarget = -1;
            d.questProgress = 0;
            d.addRep(f, 15);
            give(p, new ItemStack(RealmItems.ARCANE_CRYSTAL.get(), 2));
            give(p, new ItemStack(RealmItems.essence(f), 3));
            if (p.getRandom().nextInt(3) == 0) give(p, new ItemStack(randomTome(p)));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.9F);
            return Component.translatable("dialog.krolasyonbosses.quest_reward");
        }
        if (d.questGiver == f.ordinal() && d.questTarget >= 0) {
            Faction tgt = Faction.byId(d.questTarget);
            return Component.translatable("gui.krolasyonbosses.quest_active", tgt == null ? "?" : tgt.title(), d.questProgress, RealmData.QUEST_NEED);
        }
        List<Faction> enemies = new ArrayList<>();
        for (Faction g : Faction.ALL) if (f.atWarWith(g)) enemies.add(g);
        Faction tgt = enemies.get(p.getRandom().nextInt(enemies.size()));
        d.questGiver = f.ordinal();
        d.questTarget = tgt.ordinal();
        d.questProgress = 0;
        return Component.translatable("dialog.krolasyonbosses.quest_given", tgt.title());
    }

    static Component alliance(ServerPlayer p, RealmData d, Faction f) {
        if (d.ruler) return Component.translatable("dialog.krolasyonbosses.ruler");
        if (d.conquered[f.ordinal()]) return Component.translatable("dialog.krolasyonbosses.conquered");
        if (d.allied[f.ordinal()]) return Component.translatable("dialog.krolasyonbosses.already");
        if (d.rep(f) < 50) return Component.translatable("dialog.krolasyonbosses.alliance_no");
        d.allied[f.ordinal()] = true;
        give(p, new ItemStack(RealmItems.sigil(f)));
        blessing(p, f);
        for (Faction g : Faction.ALL) {
            if (f.atWarWith(g) && !d.bound(g)) {
                d.addRep(g, -15);
                p.sendSystemMessage(Component.translatable("message.krolasyonbosses.betrayal", g.title()).withStyle(ChatFormatting.DARK_RED));
            }
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        return Component.translatable(f == Faction.SOUL ? "dialog.krolasyonbosses.alliance_soul" : "dialog.krolasyonbosses.alliance_ok");
    }

    // ------------------------------------------------------------------ war
    public static void onKill(ServerPlayer p, Faction f, boolean envoy) {
        RealmData d = RealmData.get(p);
        d.addRep(f, envoy ? -25 : -2);
        for (Faction g : Faction.ALL) if (f.atWarWith(g)) d.addRep(g, 1);
        if (d.questTarget == f.ordinal() && d.questProgress < RealmData.QUEST_NEED) {
            d.questProgress++;
            p.displayClientMessage(d.questProgress >= RealmData.QUEST_NEED
                    ? Component.translatable("message.krolasyonbosses.quest_done").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.krolasyonbosses.quest_progress", d.questProgress, RealmData.QUEST_NEED).withStyle(ChatFormatting.GOLD), true);
        }
        d.save(p);
        RealmNet.sync(p);
    }

    /** the lord of a kingdom was slain by this player */
    public static void conquer(ServerPlayer p, Faction f) {
        RealmData d = RealmData.get(p);
        boolean first = !d.conquered[f.ordinal()];
        d.conquered[f.ordinal()] = true;
        d.rep[f.ordinal()] = Math.max(d.rep(f), 30);
        for (Faction g : Faction.ALL) {
            if (f.alliedWith(g)) d.addRep(g, -20);
            if (f.atWarWith(g)) d.addRep(g, 20);
        }
        if (first && !d.allied[f.ordinal()]) give(p, new ItemStack(RealmItems.sigil(f)));
        MutableComponent msg = Component.translatable("message.krolasyonbosses.lord_defeated", f.lord(), f.title());
        p.server.getPlayerList().broadcastSystemMessage(msg.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        if (first) blessing(p, f);
        Story.update(p, d);
    }

    public static void crown(ServerPlayer p) {
        RealmData d = RealmData.get(p);
        boolean first = !d.ruler;
        d.ruler = true;
        for (int i = 0; i < 5; i++) d.rep[i] = 100;
        if (first) {
            give(p, new ItemStack(RealmItems.SOVEREIGN_CROWN.get()));
            p.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.krolasyonbosses.tyrant_defeated", p.getDisplayName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        }
        Story.update(p, d);
    }

    static void blessing(ServerPlayer p, Faction f) {
        p.sendSystemMessage(Component.translatable("message.krolasyonbosses.blessing",
                Component.translatable("blessing.krolasyonbosses." + f.id)).withStyle(f.chat));
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    static Item randomTome(ServerPlayer p) {
        Item[] t = {RealmItems.TOME_FIREBALL.get(), RealmItems.TOME_BLOOD_LANCE.get(), RealmItems.TOME_SHADOW_STEP.get(),
                RealmItems.TOME_SOUL_SHIELD.get(), RealmItems.TOME_LAVA_WAVE.get(), RealmItems.TOME_CHAIN_LIGHTNING.get()};
        return t[p.getRandom().nextInt(t.length)];
    }
}
