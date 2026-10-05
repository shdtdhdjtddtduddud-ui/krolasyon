package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.world.NewsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Experience, level ups, stat attributes, rank assessment and the main quest chain. */
public final class Progression {
    private static final UUID STR_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1001");
    private static final UUID AGI_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1002");
    private static final UUID AGI_ATK_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1003");
    private static final UUID VIT_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1004");
    private static final UUID VIT_T_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1005");
    private static final UUID PER_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1006");
    private static final UUID MON_ID = UUID.fromString("5d1b0e3a-6c1e-4b8f-9a51-2f8e1c0a1007");

    private Progression() {}

    public static int expFor(LivingEntity e) {
        if (e instanceof SLMonster m) return m.kind.xp;
        if (e instanceof Player) return 0;
        double hp = e.getMaxHealth();
        int base = (int) Math.round(hp * 0.7);
        if (e instanceof net.minecraft.world.entity.monster.Enemy) base = (int) (base * 1.4);
        if (e instanceof net.minecraft.world.entity.boss.wither.WitherBoss || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
            base *= 6;
        return Math.max(1, base);
    }

    public static void addExp(ServerPlayer p, long amount) {
        HunterData d = HunterCapability.get(p);
        if (!d.awakened || amount <= 0) return;
        amount = Math.round(amount * com.krolasyon.sololeveling.world.GuildManager.expBonus(d));
        d.exp += amount;
        int before = d.level;
        while (d.exp >= d.expToNext() && d.level < 150) {
            d.exp -= d.expToNext();
            d.level++;
            d.statPoints += 5;
        }
        if (d.level != before) {
            applyAttributes(p, d);
            p.setHealth(p.getMaxHealth());
            d.mana = d.maxMana();
            p.getFoodData().eat(20, 1F);
            Sys.notify(p, Sys.LEVEL, Sys.t("level_up.title"), Sys.t("level_up.body", d.level, d.statPoints));
            for (Skill s : Skill.values()) {
                if (s.unlockLevel > before && s.unlockLevel <= d.level && d.hasSkill(s))
                    Sys.notify(p, Sys.REWARD, Sys.t("skill.acquired"), Sys.t("skill." + s.id()));
            }
            autoSlot(d);
            if (d.level >= 40 && d.job == Job.NONE && !d.flags.contains("job_hint")) {
                d.flags.add("job_hint");
                Sys.notify(p, Sys.QUEST, Sys.t("quest.title"), Sys.t("job_change.available"));
            }
            if (d.level / 25 > before / 25) NewsManager.get(p.server).playerMilestone(p, d.level);
            checkQuest(p);
        }
        d.markDirty();
    }

    /** Puts newly unlocked active skills into empty quick slots. */
    public static void autoSlot(HunterData d) {
        outer:
        for (Skill s : Skill.values()) {
            if (!s.active || !d.hasSkill(s)) continue;
            for (Skill x : d.slots) if (x == s) continue outer;
            for (int i = 0; i < HunterData.SLOTS; i++) {
                if (d.slots[i] == null) {
                    d.slots[i] = s;
                    continue outer;
                }
            }
        }
    }

    public static void applyAttributes(Player p, HunterData d) {
        if (!d.awakened) {
            remove(p, Attributes.ATTACK_DAMAGE, STR_ID);
            remove(p, Attributes.MOVEMENT_SPEED, AGI_ID);
            remove(p, Attributes.ATTACK_SPEED, AGI_ATK_ID);
            remove(p, Attributes.MAX_HEALTH, VIT_ID);
            remove(p, Attributes.ARMOR_TOUGHNESS, VIT_T_ID);
            remove(p, Attributes.LUCK, PER_ID);
            return;
        }
        double monarch = d.title == Title.SHADOW_MONARCH ? 1.1 : 1.0;
        set(p, Attributes.ATTACK_DAMAGE, STR_ID, "sl_str", (d.stat(Stat.STR) - 10) * 0.25 * monarch, AttributeModifier.Operation.ADDITION);
        set(p, Attributes.MOVEMENT_SPEED, AGI_ID, "sl_agi", Math.min(0.9, (d.stat(Stat.AGI) - 10) * 0.004), AttributeModifier.Operation.MULTIPLY_BASE);
        set(p, Attributes.ATTACK_SPEED, AGI_ATK_ID, "sl_agi_atk", Math.min(3, (d.stat(Stat.AGI) - 10) * 0.012), AttributeModifier.Operation.ADDITION);
        set(p, Attributes.MAX_HEALTH, VIT_ID, "sl_vit", Math.min(900, (d.stat(Stat.VIT) - 10) * 0.6 + (d.level - 1) * 0.4) * monarch, AttributeModifier.Operation.ADDITION);
        set(p, Attributes.ARMOR_TOUGHNESS, VIT_T_ID, "sl_vit_t", Math.min(20, (d.stat(Stat.VIT) - 10) * 0.04), AttributeModifier.Operation.ADDITION);
        set(p, Attributes.LUCK, PER_ID, "sl_per", d.stat(Stat.PER) * 0.05, AttributeModifier.Operation.ADDITION);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void set(Player p, Attribute a, UUID id, String name, double v, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null && cur.getAmount() == v) return;
        if (cur != null) inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, name, v, op));
    }

    private static void remove(Player p, Attribute a, UUID id) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst != null && inst.getModifier(id) != null) inst.removeModifier(id);
    }

    public static void addStat(ServerPlayer p, Stat s, int n) {
        HunterData d = HunterCapability.get(p);
        n = Math.min(n, d.statPoints);
        if (n <= 0) return;
        d.statPoints -= n;
        d.stats[s.ordinal()] += n;
        applyAttributes(p, d);
        d.markDirty();
    }

    /** Hunter Association re-awakening test. */
    public static void reassess(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        int sum = 0;
        for (int v : d.stats) sum += v;
        int effective = Math.max(d.level, (sum - 50) / 5);
        Rank old = d.rank;
        Rank r = Rank.forLevel(effective);
        if (r.ordinal() < old.ordinal()) r = old;
        d.rank = r;
        d.licensed = true;
        d.markDirty();
        Sys.notify(p, Sys.REWARD, Sys.t("association.title"), Sys.t("association.assessed", Component.translatable(r.langKey())));
        if (r != old && r.ordinal() >= Rank.B.ordinal()) NewsManager.get(p.server).rankUp(p, r);
        checkQuest(p);
    }

    public static void checkQuest(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        for (int guard = 0; guard < 3; guard++) {
            if (!done(d, d.quest)) return;
            MainQuest q = d.quest;
            d.quest = q.next();
            d.gold += q.goldReward;
            d.statPoints += q == MainQuest.AWAKEN ? 0 : 3;
            Sys.notify(p, Sys.QUEST, Sys.t("quest.complete"), Component.translatable(q.langKey()).append(" ").append(Sys.t("quest.reward", q.goldReward)));
            reward(p, q);
            if (d.quest != MainQuest.DONE)
                Sys.notify(p, Sys.QUEST, Sys.t("quest.new"), Component.translatable(d.quest.langKey()));
            d.markDirty();
        }
    }

    private static void reward(ServerPlayer p, MainQuest q) {
        HunterData d = HunterCapability.get(p);
        switch (q) {
            case DOUBLE_DUNGEON -> {
                d.titles.add(Title.ONE_WHO_OVERCAME);
                give(p, new ItemStack(ModItems.RANDOM_BOX.get(), 2));
            }
            case KASAKA -> give(p, new ItemStack(ModItems.RUNE_STONE_STEALTH.get()));
            case JOB_CHANGE -> {
                give(p, new ItemStack(ModItems.DEMON_CASTLE_KEY.get()));
                give(p, new ItemStack(ModItems.ELIXIR_OF_LIFE.get()));
            }
            case DEMON_CASTLE -> d.titles.add(Title.DEMON_HUNTER);
            case JEJU -> d.titles.add(Title.ANT_EXTERMINATOR);
            case MONARCH -> {
                d.job = Job.SHADOW_MONARCH;
                d.titles.add(Title.SHADOW_MONARCH);
                d.title = Title.SHADOW_MONARCH;
                applyAttributes(p, d);
                give(p, new ItemStack(ModItems.KAMISH_WRATH.get()));
                NewsManager.get(p.server).monarch(p);
            }
            default -> {}
        }
    }

    public static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private static boolean done(HunterData d, MainQuest q) {
        return switch (q) {
            case AWAKEN -> d.awakened;
            case REGISTER -> d.licensed;
            case FIRST_GATE -> d.gatesCleared >= 1;
            case DOUBLE_DUNGEON -> d.flags.contains("cleared_double_dungeon");
            case REACH_20 -> d.level >= 20;
            case KASAKA -> d.kills.getOrDefault(MobKind.KASAKA.id(), 0) > 0;
            case JOB_CHANGE -> d.job != Job.NONE;
            case RED_GATE -> d.kills.getOrDefault(MobKind.BARUKA.id(), 0) > 0;
            case ORC_CHIEFTAIN -> d.kills.getOrDefault(MobKind.KARGALGAN.id(), 0) > 0;
            case DEMON_CASTLE -> d.kills.getOrDefault(MobKind.BARAN.id(), 0) > 0;
            case JEJU -> d.kills.getOrDefault(MobKind.BERU.id(), 0) > 0;
            case MONARCH -> d.level >= 100;
            case DONE -> false;
        };
    }
}
