package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Race;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

/** Marriage and children. */
public final class FamilyLogic {
    private FamilyLogic() {}

    public static String propose(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d) {
        if (d.spouse != null) return "Sen zaten evlisin! Bunu nasıl teklif edebilirsin?";
        if (npc.flag(RpgNpc.F_SPOUSE)) return "Ben zaten evliyim.";
        if (npc.role() == NpcRole.RULER) return "Bir hükümdarın evliliği devlet meselesidir. Senin gibi biriyle... asla.";
        if (npc.role() == NpcRole.NOBLE && d.social < 4) return "Bir soylu, unvanı olmayan biriyle evlenemez. En azından şövalye olmalısın.";
        if (rel.affinity < 80) return rel.affinity < 40 ? "Ne?! Seni daha yeni tanıyorum!" : "Sana karşı bir şeyler hissediyorum ama... henüz hazır değilim. Biraz daha zaman ver.";
        if (rel.trust < 60) return "Seni seviyorum, ama sana tam olarak güvenebilir miyim, bilmiyorum...";
        if (NpcQuests.countItem(p, RpgItems.RING.get()) <= 0) return "Yüzük olmadan mı?";
        NpcQuests.takeItem(p, RpgItems.RING.get(), 1);
        rel.married = true;
        rel.add(20, 20);
        d.spouse = npc.getUUID();
        d.spouseName = npc.npcName();
        npc.setLeader(p.getUUID());
        npc.setFlag(RpgNpc.F_SPOUSE, true);
        npc.setFlag(RpgNpc.F_FOLLOW, false);
        npc.setFlag(RpgNpc.F_STAY, false);
        if (d.home != null) npc.home = d.home;
        d.addRep(npc.kingdomId(), 30);
        d.fame += 10;
        Bonds.add(p, npc, "friend");
        FX.sphere(p.level(), ParticleTypes.HEART, npc.position().add(0, 1, 0), 2.5, 40);
        p.level().playSound(null, npc.blockPosition(), SoundEvents.BELL_BLOCK, p.getSoundSource(), 1.5F, 1.0F);
        p.server.getPlayerList().broadcastSystemMessage(Component.literal("§d♥ " + p.getName().getString() + " ve " + npc.npcName() + " evlendi! Çanlar onlar için çalıyor."), false);
        return "Evet! Evet, bin kere evet! ... Seninle bir ömür geçirmek istiyorum." + (d.home == null ? "\n§8(Bir Ev Tapusu alıp evini belirlersen eşin orada yaşar.)" : "");
    }

    public static String child(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d) {
        if (!rel.married) return "...";
        if (d.home == null) return "Önce bir yuvamız olmalı. Bir Ev Tapusu bul ve evimizi belirle.";
        if (npc.blockPosition().distSqr(d.home) > 24 * 24) return "Burada mı? Önce eve gidelim.";
        long day = p.level().getDayTime() / 24000L;
        long next = npc.getPersistentData().getLong("NextChildDay");
        if (day < next) return "Daha yeni bir bebeğimiz oldu! Biraz soluklanalım.";
        if (d.children.size() >= 6) return "Evimiz zaten çocuk sesleriyle dolu!";
        boolean night = p.level().isNight();
        if (!night) return "Şimdi mi? Gün ortasında? Akşam olsun... *göz kırpar*";
        npc.getPersistentData().putLong("NextChildDay", day + 3);
        RandomSource r = p.getRandom();
        RpgNpc baby = RpgEntities.NPC.get().create(p.serverLevel());
        if (baby == null) return "...";
        Race race = r.nextBoolean() ? npc.race() : Race.HUMAN;
        boolean female = r.nextBoolean();
        baby.setup(race, female, NpcRole.CHILD, npc.kingdomId(), r);
        String first = baby.npcName().split(" ")[0];
        baby.setCustomName(Component.literal(first + " " + p.getName().getString() + "oğlu"));
        if (female) baby.setCustomName(Component.literal(first + " " + p.getName().getString() + "kızı"));
        baby.setAge(RpgNpc.CHILD_TICKS);
        baby.setFlag(RpgNpc.F_FAMILY, true);
        baby.setLeader(p.getUUID());
        baby.home = d.home;
        Relation br = baby.rel(p.getUUID());
        br.affinity = 100;
        br.trust = 100;
        BlockPos h = d.home;
        baby.moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 0, 0);
        p.serverLevel().addFreshEntity(baby);
        d.children.add(baby.getUUID());
        FX.sphere(p.level(), ParticleTypes.HEART, baby.position().add(0, 0.5, 0), 1.5, 30);
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, p.getSoundSource(), 0.8F, 1.8F);
        p.sendSystemMessage(Component.literal("§d♥ Bir bebeğiniz oldu: " + baby.npcName() + " (" + race.title + ")! 5 gün içinde büyüyecek."));
        return "Ona bak... Ne kadar küçük, ne kadar güzel. Bizim çocuğumuz.";
    }
}
