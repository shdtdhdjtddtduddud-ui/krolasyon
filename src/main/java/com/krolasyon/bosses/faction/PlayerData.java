package com.krolasyon.bosses.faction;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Everything the realm remembers about a player: reputation, oaths, quest stages, sigils, mana. Stored in the player's persistent data. */
public final class PlayerData {
    private PlayerData() {}

    public static final String ROOT = "krolasyon";
    public static final int HOSTILE_BELOW = 0;
    public static final int ALLY_AT = 50;
    public static final int MAX_REP = 100;
    public static final int MIN_REP = -100;

    public static final int OATH_NONE = 0, OATH_ALLY = 1, OATH_CONQUEROR = 2;

    public static CompoundTag root(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(ROOT, 10)) pd.put(ROOT, new CompoundTag());
        return pd.getCompound(ROOT);
    }

    public static boolean initialised(Player p) { return root(p).getBoolean("init"); }

    /** called the first time a player sets foot in Azrakor: everybody sees an outsider */
    public static void init(Player p) {
        CompoundTag r = root(p);
        if (r.getBoolean("init")) return;
        r.putBoolean("init", true);
        for (Faction f : Faction.HOUSES) r.putInt("rep_" + f.id, -20);
        r.putInt("rep_outcast", 0);
    }

    public static int rep(Player p, Faction f) {
        CompoundTag r = root(p);
        return r.contains("rep_" + f.id) ? r.getInt("rep_" + f.id) : (f.isHouse() ? -20 : 0);
    }

    private static void setRep(Player p, Faction f, int v) {
        root(p).putInt("rep_" + f.id, Math.max(MIN_REP, Math.min(MAX_REP, v)));
    }

    /** raises/lowers reputation; allies of that house follow at a third of the amount, its enemies at minus a third */
    public static void addRep(Player p, Faction f, int delta, boolean ripple) {
        if (!f.isHouse() || delta == 0) return;
        int before = rep(p, f);
        setRep(p, f, before + delta);
        if (ripple) {
            int third = delta / 3;
            if (third != 0) {
                for (Faction o : Faction.HOUSES) {
                    if (f.isAllyOf(o)) setRep(p, o, rep(p, o) + third);
                    else if (f.isEnemyOf(o)) setRep(p, o, rep(p, o) - third);
                }
            }
        }
        if (p instanceof ServerPlayer sp) {
            int after = rep(p, f);
            if (after != before) {
                sp.displayClientMessage(Component.literal((delta > 0 ? "▲ " : "▼ ") + f.trName + ": " + after)
                        .withStyle(s -> s.withColor(f.color)), true);
                com.krolasyon.bosses.net.Net.syncTo(sp);
            }
        }
    }

    public static boolean isHostileTo(Player p, Faction f) {
        return f.isHouse() && rep(p, f) < HOSTILE_BELOW;
    }

    public static boolean isAlly(Player p, Faction f) {
        return f.isHouse() && rep(p, f) >= ALLY_AT;
    }

    public static int oath(Player p, Faction f) { return root(p).getInt("oath_" + f.id); }

    public static void setOath(Player p, Faction f, int v) {
        root(p).putInt("oath_" + f.id, v);
        if (p instanceof ServerPlayer sp) com.krolasyon.bosses.net.Net.syncTo(sp);
    }

    public static int stage(Player p, Faction f) { return root(p).getInt("stage_" + f.id); }

    public static void setStage(Player p, Faction f, int v) {
        root(p).putInt("stage_" + f.id, v);
        if (p instanceof ServerPlayer sp) com.krolasyon.bosses.net.Net.syncTo(sp);
    }

    public static int counter(Player p, String key) { return root(p).getInt("c_" + key); }

    public static void addCounter(Player p, String key, int d) { root(p).putInt("c_" + key, root(p).getInt("c_" + key) + d); }

    public static void setCounter(Player p, String key, int v) { root(p).putInt("c_" + key, v); }

    public static boolean flag(Player p, String key) { return root(p).getBoolean("f_" + key); }

    public static void setFlag(Player p, String key, boolean v) {
        root(p).putBoolean("f_" + key, v);
        if (p instanceof ServerPlayer sp) com.krolasyon.bosses.net.Net.syncTo(sp);
    }

    public static boolean isSovereign(Player p) { return flag(p, "sovereign"); }

    /** number of houses that have been won over (alliance) or broken (conquest) */
    public static int sigils(Player p) {
        int n = 0;
        for (Faction f : Faction.HOUSES) if (oath(p, f) != OATH_NONE) n++;
        return n;
    }

    // ---- mana
    public static int maxMana(Player p) { return 100 + (isSovereign(p) ? 100 : 0) + sigils(p) * 10; }

    public static float mana(Player p) {
        CompoundTag r = root(p);
        return r.contains("mana") ? r.getFloat("mana") : maxMana(p);
    }

    public static void setMana(Player p, float v) { root(p).putFloat("mana", Math.max(0, Math.min(maxMana(p), v))); }

    public static boolean useMana(Player p, float cost) {
        if (p.isCreative()) return true;
        float m = mana(p);
        if (m < cost) return false;
        setMana(p, m - cost);
        return true;
    }

    public static CompoundTag copyForSync(Player p) { return root(p).copy(); }

    public static void load(Player p, CompoundTag t) { p.getPersistentData().put(ROOT, t.copy()); }
}
