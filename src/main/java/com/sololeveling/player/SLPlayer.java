package com.sololeveling.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** All persistent Solo Leveling data of one player (the "System"). */
public class SLPlayer {
    public static final String[] STAT_IDS = {"str", "agi", "vit", "int", "sense"};

    public boolean awakened = false;
    public int level = 1;
    public long xp = 0;
    public int points = 0;
    public int[] stats = {10, 10, 10, 10, 10};
    public float mana = 100;
    public int fatigue = 0;
    public long gold = 0;
    public String job = "none";
    public String title = "Wolf Hunter";
    public String guild = "";
    public final Set<String> skills = new HashSet<>();
    public int kills = 0;
    public int gatesCleared = 0;
    public int shadowsExtracted = 0;
    public final Set<String> discovered = new HashSet<>();

    // daily quest
    public long dailyDay = -1;
    public int dqKills = 0;
    public float dqDist = 0;
    public int dqGates = 0;
    public boolean dqClaimed = false;
    public int dqKillTarget = 20;
    public int dqDistTarget = 1000;

    // bounty quests (accepted)
    public final List<Quest> quests = new ArrayList<>();
    public long bountyDay = -1;
    public final List<Quest> offers = new ArrayList<>();

    public static class Quest {
        public String type = "kill";   // kill | gate | collect
        public String target = "goblin";
        public int need = 5;
        public int have = 0;
        public int rewardGold = 100;
        public int rewardXp = 100;
        public String rewardItem = "";
        public int rewardCount = 0;
        public String title = "";

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("type", type); t.putString("target", target); t.putInt("need", need); t.putInt("have", have);
            t.putInt("rg", rewardGold); t.putInt("rx", rewardXp); t.putString("ri", rewardItem); t.putInt("rc", rewardCount);
            t.putString("title", title);
            return t;
        }

        public static Quest load(CompoundTag t) {
            Quest q = new Quest();
            q.type = t.getString("type"); q.target = t.getString("target"); q.need = t.getInt("need"); q.have = t.getInt("have");
            q.rewardGold = t.getInt("rg"); q.rewardXp = t.getInt("rx"); q.rewardItem = t.getString("ri"); q.rewardCount = t.getInt("rc");
            q.title = t.getString("title");
            return q;
        }

        public boolean done() { return have >= need; }
    }

    // ------------------------------------------------------------------ derived
    public int stat(int i) { return stats[i]; }
    public int str() { return stats[0]; }
    public int agi() { return stats[1]; }
    public int vit() { return stats[2]; }
    public int intel() { return stats[3]; }
    public int sense() { return stats[4]; }

    public static long xpNeeded(int level) {
        return (long) (40 + 12.0 * level + 0.9 * level * level);
    }

    public long xpNeeded() { return xpNeeded(level); }

    public static String rankOf(int level) {
        if (level >= 100) return "N";
        if (level >= 80) return "S";
        if (level >= 60) return "A";
        if (level >= 40) return "B";
        if (level >= 25) return "C";
        if (level >= 10) return "D";
        return "E";
    }

    public String rank() { return rankOf(level); }

    public float maxMana() { return 50 + intel() * 10 + level * 5; }

    public float manaRegenPerSecond() { return 1.0F + intel() * 0.12F + level * 0.05F; }

    public int maxShadows() { return 3 + intel() / 4 + level / 6; }

    public boolean hasSkill(String id) { return skills.contains(id); }

    /** @return number of levels gained */
    public int addXp(long amount) {
        xp += amount;
        int gained = 0;
        while (xp >= xpNeeded() && level < 200) {
            xp -= xpNeeded();
            level++;
            points += 5;
            gained++;
        }
        return gained;
    }

    public float addMana(float v) {
        mana = Mth.clamp(mana + v, 0, maxMana());
        return mana;
    }

    // ------------------------------------------------------------------ nbt
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("awakened", awakened);
        t.putInt("level", level); t.putLong("xp", xp); t.putInt("points", points);
        t.putIntArray("stats", stats);
        t.putFloat("mana", mana); t.putInt("fatigue", fatigue); t.putLong("gold", gold);
        t.putString("job", job); t.putString("title", title); t.putString("guild", guild);
        ListTag sk = new ListTag();
        for (String s : skills) sk.add(StringTag.valueOf(s));
        t.put("skills", sk);
        ListTag dis = new ListTag();
        for (String s : discovered) dis.add(StringTag.valueOf(s));
        t.put("discovered", dis);
        t.putInt("kills", kills); t.putInt("gates", gatesCleared); t.putInt("shadowsX", shadowsExtracted);
        t.putLong("dqDay", dailyDay); t.putInt("dqKills", dqKills); t.putFloat("dqDist", dqDist); t.putInt("dqGates", dqGates);
        t.putBoolean("dqClaimed", dqClaimed); t.putInt("dqKT", dqKillTarget); t.putInt("dqDT", dqDistTarget);
        ListTag qs = new ListTag();
        for (Quest q : quests) qs.add(q.save());
        t.put("quests", qs);
        t.putLong("bountyDay", bountyDay);
        ListTag of = new ListTag();
        for (Quest q : offers) of.add(q.save());
        t.put("offers", of);
        return t;
    }

    public void load(CompoundTag t) {
        awakened = t.getBoolean("awakened");
        level = Math.max(1, t.getInt("level")); xp = t.getLong("xp"); points = t.getInt("points");
        int[] s = t.getIntArray("stats");
        for (int i = 0; i < 5; i++) stats[i] = (s.length > i) ? s[i] : 10;
        mana = t.contains("mana") ? t.getFloat("mana") : maxMana();
        fatigue = t.getInt("fatigue"); gold = t.getLong("gold");
        job = t.contains("job") ? t.getString("job") : "none";
        title = t.contains("title") ? t.getString("title") : "Wolf Hunter";
        guild = t.getString("guild");
        skills.clear();
        for (Tag e : t.getList("skills", Tag.TAG_STRING)) skills.add(e.getAsString());
        discovered.clear();
        for (Tag e : t.getList("discovered", Tag.TAG_STRING)) discovered.add(e.getAsString());
        kills = t.getInt("kills"); gatesCleared = t.getInt("gates"); shadowsExtracted = t.getInt("shadowsX");
        dailyDay = t.contains("dqDay") ? t.getLong("dqDay") : -1; dqKills = t.getInt("dqKills"); dqDist = t.getFloat("dqDist"); dqGates = t.getInt("dqGates");
        dqClaimed = t.getBoolean("dqClaimed"); dqKillTarget = t.contains("dqKT") ? t.getInt("dqKT") : 20; dqDistTarget = t.contains("dqDT") ? t.getInt("dqDT") : 1000;
        quests.clear();
        for (Tag e : t.getList("quests", Tag.TAG_COMPOUND)) quests.add(Quest.load((CompoundTag) e));
        bountyDay = t.contains("bountyDay") ? t.getLong("bountyDay") : -1;
        offers.clear();
        for (Tag e : t.getList("offers", Tag.TAG_COMPOUND)) offers.add(Quest.load((CompoundTag) e));
    }

    public void copyFrom(SLPlayer o) { load(o.save()); }
}
