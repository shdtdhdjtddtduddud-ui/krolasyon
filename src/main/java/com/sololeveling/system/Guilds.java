package com.sololeveling.system;

import com.sololeveling.player.SLPlayer;

public final class Guilds {
    private Guilds() {}

    public static final String[] IDS = {"hunters", "ahjin", "white_tiger"};
    public static final int JOIN_LEVEL = 10;
    public static final int JOIN_COST = 500;

    public static boolean valid(String id) {
        for (String s : IDS) if (s.equals(id)) return true;
        return false;
    }

    public static double xpMultiplier(SLPlayer d) { return d.guild.equals("hunters") ? 1.05 : 1.0; }
    public static double manaRegenMultiplier(SLPlayer d) { return d.guild.equals("hunters") ? 1.25 : 1.0; }
    public static double damageMultiplier(SLPlayer d) { return d.guild.equals("ahjin") ? 1.10 : 1.0; }
    public static double speedBonus(SLPlayer d) { return d.guild.equals("white_tiger") ? 0.10 : 0.0; }
    public static double armorBonus(SLPlayer d) { return d.guild.equals("white_tiger") ? 2.0 : 0.0; }
}
