package com.krolasyon.sololeveling.system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/** Everything the System knows about a player. Lives in a player capability and is synced to the owner. */
public class HunterData {
    public static final int SLOTS = 5;

    public boolean awakened;
    public int level = 1;
    public long exp;
    public int statPoints;
    public final int[] stats = {10, 10, 10, 10, 10};
    public float mana = 100;
    public long gold;
    public Job job = Job.NONE;
    public Rank rank = Rank.E;
    public boolean licensed;
    public Title title = Title.NONE;
    public final EnumSet<Title> titles = EnumSet.noneOf(Title.class);
    public final EnumSet<Skill> runeSkills = EnumSet.noneOf(Skill.class);
    public final Skill[] slots = new Skill[SLOTS];
    public final EnumMap<Skill, Integer> cooldowns = new EnumMap<>(Skill.class);
    public MainQuest quest = MainQuest.AWAKEN;

    // daily quest: crouches (push-ups), jumps (sit-ups), hits (squats), sprint distance in blocks (running)
    public long dailyDay = -1;
    public final int[] daily = new int[4];
    public boolean dailyDone;
    public boolean dailyRewarded;
    public int penaltyTicks;

    public Guild guild = Guild.NONE;
    public int guildRep;
    public String guildTask = "";
    public int guildTaskProgress;
    public int guildTaskGoal;

    public final Set<String> flags = new HashSet<>();
    public final Set<String> waypoints = new LinkedHashSet<>();
    public final Map<String, Integer> kills = new HashMap<>();
    public final List<ShadowRecord> shadows = new ArrayList<>();
    public boolean shadowsOut;
    public int gatesCleared;
    public int fatigue;
    public CompoundTag misc = new CompoundTag();

    // runtime (not saved, not synced)
    public transient boolean dirty = true;
    public transient int stealthTicks;
    public transient int domainTicks;
    public transient int mapTravelCooldown;

    public static final int[] DAILY_GOAL = {100, 100, 100, 1000};

    public int stat(Stat s) { return stats[s.ordinal()]; }

    public float maxMana() { return 60 + stat(Stat.INT) * 8 + level * 2; }

    public static long expForLevel(int level) {
        return (long) (60 + 30 * Math.pow(level, 1.65));
    }

    public long expToNext() { return expForLevel(level); }

    public boolean hasSkill(Skill s) {
        if (runeSkills.contains(s)) return true;
        if (level < s.unlockLevel) return false;
        return job.ordinal() >= s.job.ordinal();
    }

    public int shadowCapacity() {
        if (job == Job.NONE) return 0;
        return job.shadowSlots + stat(Stat.INT) / 5 + (hasSkill(Skill.SHADOW_PRESERVATION) ? 4 : 0);
    }

    public int cooldown(Skill s) { return cooldowns.getOrDefault(s, 0); }

    public void markDirty() { dirty = true; }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("awakened", awakened);
        t.putInt("level", level);
        t.putLong("exp", exp);
        t.putInt("points", statPoints);
        t.putIntArray("stats", stats);
        t.putFloat("mana", mana);
        t.putLong("gold", gold);
        t.putInt("job", job.ordinal());
        t.putInt("rank", rank.ordinal());
        t.putBoolean("licensed", licensed);
        t.putInt("title", title.ordinal());
        int tm = 0;
        for (Title x : titles) tm |= 1 << x.ordinal();
        t.putInt("titles", tm);
        long rm = 0;
        for (Skill x : runeSkills) rm |= 1L << x.ordinal();
        t.putLong("runes", rm);
        int[] sl = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) sl[i] = slots[i] == null ? -1 : slots[i].ordinal();
        t.putIntArray("slots", sl);
        CompoundTag cd = new CompoundTag();
        cooldowns.forEach((k, v) -> { if (v > 0) cd.putInt(k.id(), v); });
        t.put("cd", cd);
        t.putInt("quest", quest.ordinal());
        t.putLong("dailyDay", dailyDay);
        t.putIntArray("daily", daily);
        t.putBoolean("dailyDone", dailyDone);
        t.putBoolean("dailyRewarded", dailyRewarded);
        t.putInt("penalty", penaltyTicks);
        t.putInt("guild", guild.ordinal());
        t.putInt("guildRep", guildRep);
        t.putString("guildTask", guildTask);
        t.putInt("guildTaskProgress", guildTaskProgress);
        t.putInt("guildTaskGoal", guildTaskGoal);
        ListTag fl = new ListTag();
        for (String f : flags) fl.add(StringTag.valueOf(f));
        t.put("flags", fl);
        ListTag wp = new ListTag();
        for (String f : waypoints) wp.add(StringTag.valueOf(f));
        t.put("waypoints", wp);
        CompoundTag k = new CompoundTag();
        kills.forEach(k::putInt);
        t.put("kills", k);
        ListTag sh = new ListTag();
        for (ShadowRecord r : shadows) sh.add(r.save());
        t.put("shadows", sh);
        t.putBoolean("shadowsOut", shadowsOut);
        t.putInt("gates", gatesCleared);
        t.putInt("fatigue", fatigue);
        t.put("misc", misc);
        return t;
    }

    public void load(CompoundTag t) {
        awakened = t.getBoolean("awakened");
        level = Math.max(1, t.getInt("level"));
        exp = t.getLong("exp");
        statPoints = t.getInt("points");
        int[] st = t.getIntArray("stats");
        if (st.length == 5) System.arraycopy(st, 0, stats, 0, 5);
        mana = t.getFloat("mana");
        gold = t.getLong("gold");
        job = Job.byOrdinal(t.getInt("job"));
        rank = Rank.byOrdinal(t.getInt("rank"));
        licensed = t.getBoolean("licensed");
        title = Title.byOrdinal(t.getInt("title"));
        titles.clear();
        int tm = t.getInt("titles");
        for (Title x : Title.values()) if ((tm & (1 << x.ordinal())) != 0) titles.add(x);
        runeSkills.clear();
        long rm = t.getLong("runes");
        for (Skill x : Skill.values()) if ((rm & (1L << x.ordinal())) != 0) runeSkills.add(x);
        int[] sl = t.getIntArray("slots");
        for (int i = 0; i < SLOTS; i++) slots[i] = i < sl.length ? Skill.byOrdinal(sl[i]) : null;
        cooldowns.clear();
        CompoundTag cd = t.getCompound("cd");
        for (String key : cd.getAllKeys()) {
            Skill s = Skill.byId(key);
            if (s != null) cooldowns.put(s, cd.getInt(key));
        }
        quest = MainQuest.byOrdinal(t.getInt("quest"));
        dailyDay = t.contains("dailyDay") ? t.getLong("dailyDay") : -1;
        int[] d = t.getIntArray("daily");
        Arrays.fill(daily, 0);
        if (d.length == 4) System.arraycopy(d, 0, daily, 0, 4);
        dailyDone = t.getBoolean("dailyDone");
        dailyRewarded = t.getBoolean("dailyRewarded");
        penaltyTicks = t.getInt("penalty");
        guild = Guild.byOrdinal(t.getInt("guild"));
        guildRep = t.getInt("guildRep");
        guildTask = t.getString("guildTask");
        guildTaskProgress = t.getInt("guildTaskProgress");
        guildTaskGoal = t.getInt("guildTaskGoal");
        flags.clear();
        for (Tag x : t.getList("flags", Tag.TAG_STRING)) flags.add(x.getAsString());
        waypoints.clear();
        for (Tag x : t.getList("waypoints", Tag.TAG_STRING)) waypoints.add(x.getAsString());
        kills.clear();
        CompoundTag k = t.getCompound("kills");
        for (String key : k.getAllKeys()) kills.put(key, k.getInt(key));
        shadows.clear();
        for (Tag x : t.getList("shadows", Tag.TAG_COMPOUND)) shadows.add(ShadowRecord.load((CompoundTag) x));
        shadowsOut = t.getBoolean("shadowsOut");
        gatesCleared = t.getInt("gates");
        fatigue = t.getInt("fatigue");
        misc = t.getCompound("misc");
        dirty = true;
    }

    public void copyFrom(HunterData o) { load(o.save()); }

    /** A stored shadow soldier. */
    public static class ShadowRecord {
        public UUID id;
        public String type;
        public String name;
        public int level;
        public boolean named;
        public CompoundTag extra = new CompoundTag();

        public ShadowRecord(UUID id, String type, String name, int level, boolean named) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.level = level;
            this.named = named;
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            t.putString("type", type);
            t.putString("name", name);
            t.putInt("level", level);
            t.putBoolean("named", named);
            t.put("extra", extra);
            return t;
        }

        public static ShadowRecord load(CompoundTag t) {
            ShadowRecord r = new ShadowRecord(t.hasUUID("id") ? t.getUUID("id") : UUID.randomUUID(), t.getString("type"),
                    t.getString("name"), t.getInt("level"), t.getBoolean("named"));
            r.extra = t.getCompound("extra");
            return r;
        }
    }
}
