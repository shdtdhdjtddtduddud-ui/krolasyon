package com.sololeveling.system;

import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.util.Ranks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class XpHandler {
    private XpHandler() {}

    public static void giveXp(ServerPlayer sp, long amount) {
        SLPlayer d = ModCaps.get(sp);
        amount = (long) (amount * Guilds.xpMultiplier(d));
        String before = d.rank();
        int gained = d.addXp(Math.max(1, amount));
        if (gained > 0) {
            Stats.apply(sp);
            d.mana = d.maxMana();
            sp.heal(sp.getMaxHealth() * 0.3F);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            Sys.notify(sp, Sys.LEVELUP, Component.translatable("gui.sololeveling.level_up"), Component.translatable("gui.sololeveling.level_up_body", d.level, gained * 5));
            Net.toPlayer(sp, new Packets.Fx("levelup", d.level));
            for (Content.SkillDef s : Content.SKILLS) {
                if (d.level >= s.level() && !d.hasSkill(s.id())) {
                    d.skills.add(s.id());
                    Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.new_skill"), Component.translatable("skill.sololeveling." + s.id()));
                }
            }
            String after = d.rank();
            if (!after.equals(before)) {
                NewsManager.add(sp.getServer(), NewsManager.HUNTER, Component.translatable("news.sololeveling.rank_up", sp.getDisplayName(), Ranks.tag(after)));
                Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.rank_up"), Ranks.tag(after));
            }
        }
        d.title = titleFor(d.rank());
        PlayerSync.sync(sp);
    }

    public static String titleFor(String rank) {
        return switch (rank) {
            case "E" -> "weakest";
            case "D" -> "wolf";
            case "C" -> "dagger";
            case "B" -> "shadow_hunter";
            case "A" -> "assassin";
            case "S" -> "sovereign";
            default -> "monarch";
        };
    }

    /** rank index equivalent of a creature with this much max health */
    public static int rankIdxForHealth(float maxHp) {
        if (maxHp < 22) return 0;
        if (maxHp < 50) return 1;
        if (maxHp < 100) return 2;
        if (maxHp < 250) return 3;
        if (maxHp < 800) return 4;
        if (maxHp < 2500) return 5;
        return 6;
    }
}
