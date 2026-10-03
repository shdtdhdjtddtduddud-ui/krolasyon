package com.krolasyon.bosses.realm.story;

import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.net.RealmNet;
import com.krolasyon.bosses.realm.registry.RealmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Chapter progression of "The Crimson Throne" storyline. */
public final class Story {
    private Story() {}

    public static int computeChapter(ServerPlayer p, RealmData d) {
        if (d.ruler) return RealmData.CH_SOVEREIGN;
        boolean key = p.getInventory().contains(new ItemStack(RealmItems.THRONE_KEY.get()));
        if (d.sigilsEarned() >= 5 || key) return RealmData.CH_THRONE;
        if (d.sigilsEarned() >= 1) return RealmData.CH_SIGILS;
        if (d.metEnvoy) return RealmData.CH_BARGAIN;
        return d.chapter;
    }

    /** re-evaluates the chapter, announces a new one, saves and syncs */
    public static void update(ServerPlayer p, RealmData d) {
        int c = Math.max(d.chapter, computeChapter(p, d));
        if (c != d.chapter) {
            d.chapter = c;
            announce(p, c);
        }
        d.save(p);
        RealmNet.sync(p);
    }

    public static void enterRealm(ServerPlayer p) {
        RealmData d = RealmData.get(p);
        if (d.chapter < RealmData.CH_FIREBORN) {
            d.chapter = RealmData.CH_FIREBORN;
            announce(p, d.chapter);
            ItemStack book = new ItemStack(RealmItems.CHRONICLE.get());
            if (!p.getInventory().add(book)) p.drop(book, false);
        }
        update(p, d);
    }

    public static void announce(ServerPlayer p, int chapter) {
        String k = "chapter.krolasyonbosses." + chapter;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 80, 25));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(k).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.krolasyonbosses.chapter").withStyle(ChatFormatting.GOLD)));
        p.sendSystemMessage(Component.literal("§4§l✦ ").append(Component.translatable(k).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" §4§l✦")));
        p.sendSystemMessage(Component.translatable(k + ".story").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        p.sendSystemMessage(Component.literal("» ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(k + ".goal").withStyle(ChatFormatting.YELLOW)));
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 0.8F, 0.7F);
    }
}
