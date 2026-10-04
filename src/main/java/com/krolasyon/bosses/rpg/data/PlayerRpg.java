package com.krolasyon.bosses.rpg.data;

import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.*;

/** Everything the RPG layer remembers about one player. Stored in {@link RpgWorldData}. */
public class PlayerRpg {
    public static final String[] SOCIAL = {"Yoksul", "Halktan", "Hür Vatandaş", "Silahtar", "Şövalye", "Baron", "Kont", "Dük", "Hükümdar"};
    public static final String[] GUILD = {"F", "E", "D", "C", "B", "A", "S"};
    public static final int[] GUILD_XP = {0, 100, 300, 700, 1500, 3000, 6000};

    public final UUID id;
    public int level = 1;
    public long xp;
    public int statPoints = 0;
    public int str, agi, intel, vit;
    public float mana = 40;
    public final List<String> spells = new ArrayList<>();
    public int selected;
    public final Map<String, Integer> cooldowns = new HashMap<>();

    public int social = 0;
    public int guild = -1;
    public int guildXp;
    public int fame;
    public final int[] rep = new int[Kingdom.COUNT];
    public final int[] bounty = new int[Kingdom.COUNT];
    public int allegiance = -1;

    public int chapter = 0;
    public int step = 0;
    public final Set<String> flags = new HashSet<>();
    public final List<Quest> quests = new ArrayList<>();
    public int questsDone;

    @Nullable public BlockPos home;
    @Nullable public UUID spouse;
    public String spouseName = "";
    public final List<UUID> children = new ArrayList<>();
    public final List<UUID> party = new ArrayList<>();
    public final List<Bond> bonds = new ArrayList<>();

    public boolean originDone;
    public long nextFate;
    public int kills, bossKills;
    public int warScore;
    public String lastRegion = "";

    public PlayerRpg(UUID id) {
        this.id = id;
    }

    // ------------------------------------------------------------------ stats
    public int maxMana() { return 40 + intel * 8 + (level - 1) * 3; }
    public float manaRegen() { return 0.6F + intel * 0.08F + level * 0.02F; }
    public long xpForNext() { return (long) (80 * Math.pow(level, 1.55)); }
    public float spellPower() { return 1.0F + intel * 0.045F + (level - 1) * 0.02F; }
    public float meleePower() { return 1.0F + str * 0.03F + (level - 1) * 0.01F; }

    /** returns number of levels gained */
    public int addXp(long amount) {
        xp += amount;
        int gained = 0;
        while (xp >= xpForNext() && level < 100) {
            xp -= xpForNext();
            level++;
            statPoints += 3;
            gained++;
        }
        return gained;
    }

    public String guildRank() { return guild < 0 ? "-" : GUILD[Mth.clamp(guild, 0, GUILD.length - 1)]; }

    /** returns true if the rank went up */
    public boolean addGuildXp(int n) {
        if (guild < 0) return false;
        guildXp += n;
        if (guild < GUILD.length - 1 && guildXp >= GUILD_XP[guild + 1]) {
            guild++;
            return true;
        }
        return false;
    }

    public String socialTitle() { return SOCIAL[Mth.clamp(social, 0, SOCIAL.length - 1)]; }

    public void addRep(int kingdom, int n) {
        if (kingdom < 0 || kingdom >= rep.length) return;
        rep[kingdom] = Mth.clamp(rep[kingdom] + n, -1000, 1000);
    }

    public int repWith(int kingdom) { return kingdom < 0 || kingdom >= rep.length ? 0 : rep[kingdom]; }

    public static String repTitle(int r) {
        if (r <= -600) return "Kan Düşmanı";
        if (r <= -300) return "Nefret Edilen";
        if (r <= -100) return "Güvenilmez";
        if (r < 100) return "Yabancı";
        if (r < 300) return "Tanınan";
        if (r < 600) return "Saygın";
        if (r < 900) return "Onurlu";
        return "Efsane";
    }

    public boolean flag(String f) { return flags.contains(f); }
    public void setFlag(String f) { flags.add(f); }

    @Nullable
    public Bond bond(UUID npc) {
        for (Bond b : bonds) if (b.npc.equals(npc)) return b;
        return null;
    }

    // ------------------------------------------------------------------ nbt
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("Id", id);
        t.putInt("Level", level);
        t.putLong("Xp", xp);
        t.putInt("Points", statPoints);
        t.putIntArray("Stats", new int[]{str, agi, intel, vit});
        t.putFloat("Mana", mana);
        ListTag sp = new ListTag();
        for (String s : spells) sp.add(StringTag.valueOf(s));
        t.put("Spells", sp);
        t.putInt("Selected", selected);
        t.putInt("Social", social);
        t.putInt("Guild", guild);
        t.putInt("GuildXp", guildXp);
        t.putInt("Fame", fame);
        t.putIntArray("Rep", rep);
        t.putIntArray("Bounty", bounty);
        t.putInt("Allegiance", allegiance);
        t.putInt("Chapter", chapter);
        t.putInt("Step", step);
        ListTag fl = new ListTag();
        for (String s : flags) fl.add(StringTag.valueOf(s));
        t.put("Flags", fl);
        ListTag q = new ListTag();
        for (Quest x : quests) q.add(x.save());
        t.put("Quests", q);
        t.putInt("QuestsDone", questsDone);
        if (home != null) t.putLong("Home", home.asLong());
        if (spouse != null) t.putUUID("Spouse", spouse);
        t.putString("SpouseName", spouseName);
        t.put("Children", uuids(children));
        t.put("Party", uuids(party));
        ListTag b = new ListTag();
        for (Bond x : bonds) b.add(x.save());
        t.put("Bonds", b);
        t.putBoolean("Origin", originDone);
        t.putLong("NextFate", nextFate);
        t.putInt("Kills", kills);
        t.putInt("BossKills", bossKills);
        t.putInt("WarScore", warScore);
        return t;
    }

    public static PlayerRpg load(CompoundTag t) {
        PlayerRpg p = new PlayerRpg(t.getUUID("Id"));
        p.level = Math.max(1, t.getInt("Level"));
        p.xp = t.getLong("Xp");
        p.statPoints = t.getInt("Points");
        int[] st = t.getIntArray("Stats");
        if (st.length == 4) { p.str = st[0]; p.agi = st[1]; p.intel = st[2]; p.vit = st[3]; }
        p.mana = t.getFloat("Mana");
        for (Tag x : t.getList("Spells", Tag.TAG_STRING)) p.spells.add(x.getAsString());
        p.selected = t.getInt("Selected");
        p.social = t.getInt("Social");
        p.guild = t.contains("Guild") ? t.getInt("Guild") : -1;
        p.guildXp = t.getInt("GuildXp");
        p.fame = t.getInt("Fame");
        copy(t.getIntArray("Rep"), p.rep);
        copy(t.getIntArray("Bounty"), p.bounty);
        p.allegiance = t.contains("Allegiance") ? t.getInt("Allegiance") : -1;
        p.chapter = t.getInt("Chapter");
        p.step = t.getInt("Step");
        for (Tag x : t.getList("Flags", Tag.TAG_STRING)) p.flags.add(x.getAsString());
        for (Tag x : t.getList("Quests", Tag.TAG_COMPOUND)) p.quests.add(Quest.load((CompoundTag) x));
        p.questsDone = t.getInt("QuestsDone");
        if (t.contains("Home")) p.home = BlockPos.of(t.getLong("Home"));
        if (t.hasUUID("Spouse")) p.spouse = t.getUUID("Spouse");
        p.spouseName = t.getString("SpouseName");
        readUuids(t.getList("Children", Tag.TAG_INT_ARRAY), p.children);
        readUuids(t.getList("Party", Tag.TAG_INT_ARRAY), p.party);
        for (Tag x : t.getList("Bonds", Tag.TAG_COMPOUND)) p.bonds.add(Bond.load((CompoundTag) x));
        p.originDone = t.getBoolean("Origin");
        p.nextFate = t.getLong("NextFate");
        p.kills = t.getInt("Kills");
        p.bossKills = t.getInt("BossKills");
        p.warScore = t.getInt("WarScore");
        return p;
    }

    private static void copy(int[] from, int[] to) {
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    private static ListTag uuids(List<UUID> l) {
        ListTag t = new ListTag();
        for (UUID u : l) t.add(net.minecraft.nbt.NbtUtils.createUUID(u));
        return t;
    }

    private static void readUuids(ListTag t, List<UUID> out) {
        for (Tag x : t) out.add(net.minecraft.nbt.NbtUtils.loadUUID(x));
    }

    // ------------------------------------------------------------------ nested records
    /** an NPC whose fate is tied to the player: saved, freed, wronged... they may show up again anywhere */
    public static class Bond {
        public UUID npc;
        public String name = "";
        public int race, gender, variant, kingdom = -1;
        public String kind = "saved";
        public long since;
        public int helps;
        public boolean dead;
        public long lastSeen;

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Npc", npc);
            t.putString("Name", name);
            t.putIntArray("Look", new int[]{race, gender, variant, kingdom});
            t.putString("Kind", kind);
            t.putLong("Since", since);
            t.putInt("Helps", helps);
            t.putBoolean("Dead", dead);
            t.putLong("LastSeen", lastSeen);
            return t;
        }

        public static Bond load(CompoundTag t) {
            Bond b = new Bond();
            b.npc = t.getUUID("Npc");
            b.name = t.getString("Name");
            int[] l = t.getIntArray("Look");
            if (l.length == 4) { b.race = l[0]; b.gender = l[1]; b.variant = l[2]; b.kingdom = l[3]; }
            b.kind = t.getString("Kind");
            b.since = t.getLong("Since");
            b.helps = t.getInt("Helps");
            b.dead = t.getBoolean("Dead");
            b.lastSeen = t.getLong("LastSeen");
            return b;
        }
    }
}
