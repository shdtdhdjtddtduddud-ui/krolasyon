package com.krolasyon.bosses.realm.data;

import com.krolasyon.bosses.realm.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Everything the realm remembers about one player: reputation, alliances, conquests, story chapter, mana and quest.
 * Lives in the player's persistent NBT (copied on respawn) and is mirrored to the client.
 */
public class RealmData {
    public static final String KEY = "KrolasyonRealm";
    public static final int CH_RIFT = 0, CH_FIREBORN = 1, CH_BARGAIN = 2, CH_SIGILS = 3, CH_THRONE = 4, CH_SOVEREIGN = 5;
    public static final float MAX_MANA = 100F;

    public final int[] rep = new int[5];
    public final boolean[] allied = new boolean[5];
    public final boolean[] conquered = new boolean[5];
    public int chapter;
    public boolean ruler;
    public float mana = MAX_MANA;
    public boolean metEnvoy;
    // quest
    public int questGiver = -1, questTarget = -1, questProgress;
    public static final int QUEST_NEED = 8;
    public long lastHorn, lastDash, lastBanner, lastSoulRegen;

    public static RealmData get(Player p) {
        RealmData d = new RealmData();
        d.load(p.getPersistentData().getCompound(KEY));
        return d;
    }

    public void save(Player p) { p.getPersistentData().put(KEY, write()); }

    public CompoundTag write() {
        CompoundTag t = new CompoundTag();
        t.putIntArray("Rep", rep);
        byte a = 0, c = 0;
        for (int i = 0; i < 5; i++) {
            if (allied[i]) a |= (byte) (1 << i);
            if (conquered[i]) c |= (byte) (1 << i);
        }
        t.putByte("Allied", a);
        t.putByte("Conquered", c);
        t.putInt("Chapter", chapter);
        t.putBoolean("Ruler", ruler);
        t.putFloat("Mana", mana);
        t.putBoolean("MetEnvoy", metEnvoy);
        t.putInt("QGiver", questGiver);
        t.putInt("QTarget", questTarget);
        t.putInt("QProgress", questProgress);
        t.putLong("LastHorn", lastHorn);
        t.putLong("LastDash", lastDash);
        t.putLong("LastBanner", lastBanner);
        t.putLong("LastSoulRegen", lastSoulRegen);
        return t;
    }

    public void load(CompoundTag t) {
        int[] r = t.getIntArray("Rep");
        for (int i = 0; i < 5 && i < r.length; i++) rep[i] = r[i];
        byte a = t.getByte("Allied"), c = t.getByte("Conquered");
        for (int i = 0; i < 5; i++) {
            allied[i] = (a & (1 << i)) != 0;
            conquered[i] = (c & (1 << i)) != 0;
        }
        chapter = t.getInt("Chapter");
        ruler = t.getBoolean("Ruler");
        mana = t.contains("Mana") ? t.getFloat("Mana") : MAX_MANA;
        metEnvoy = t.getBoolean("MetEnvoy");
        questGiver = t.contains("QGiver") ? t.getInt("QGiver") : -1;
        questTarget = t.contains("QTarget") ? t.getInt("QTarget") : -1;
        questProgress = t.getInt("QProgress");
        lastHorn = t.getLong("LastHorn");
        lastDash = t.getLong("LastDash");
        lastBanner = t.getLong("LastBanner");
        lastSoulRegen = t.getLong("LastSoulRegen");
    }

    // ------------------------------------------------------------------ politics
    public int rep(Faction f) { return rep[f.ordinal()]; }

    public void addRep(Faction f, int amount) { rep[f.ordinal()] = Math.max(-100, Math.min(100, rep[f.ordinal()] + amount)); }

    /** the kingdom has bound itself to the player: alliance, conquest or sovereignty */
    public boolean bound(Faction f) { return ruler || allied[f.ordinal()] || conquered[f.ordinal()]; }

    /** do this kingdom's soldiers leave the player alone? */
    public boolean peaceful(Faction f) { return bound(f) || rep(f) >= 20; }

    public int sigilsEarned() {
        int n = 0;
        for (int i = 0; i < 5; i++) if (allied[i] || conquered[i]) n++;
        return n;
    }

    public Standing standing(Faction f) {
        if (ruler) return Standing.SWORN;
        if (conquered[f.ordinal()]) return Standing.CONQUERED;
        if (allied[f.ordinal()]) return Standing.ALLIED;
        int r = rep(f);
        if (r <= -50) return Standing.HOSTILE;
        if (r < 0) return Standing.WARY;
        if (r < 20) return Standing.NEUTRAL;
        return Standing.FRIENDLY;
    }

    @Nullable
    public Faction questTargetFaction() { return Faction.byId(questTarget); }

    public enum Standing {
        HOSTILE("hostile", 0xFF4040), WARY("wary", 0xFF9A40), NEUTRAL("neutral", 0xC8C8C8), FRIENDLY("friendly", 0x80FF80),
        ALLIED("allied", 0x40E0FF), CONQUERED("conquered", 0xFFD040), SWORN("sworn", 0xFFD040);

        public final String id;
        public final int color;

        Standing(String id, int color) {
            this.id = id;
            this.color = color;
        }

        /** dialogue tier 0..3 */
        public int tier() {
            return switch (this) {
                case HOSTILE -> 0;
                case WARY, NEUTRAL -> 1;
                case FRIENDLY -> 2;
                default -> 3;
            };
        }
    }
}
