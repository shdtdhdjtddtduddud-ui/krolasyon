package com.krolasyon.bosses.rpg.data;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/** A side quest handed out by an NPC (or a guild board). */
public class Quest {
    public enum Type { KILL, KILL_ANY, BOSS, FETCH, DELIVER, VISIT, BOUNTY }

    public Type type = Type.KILL;
    public String title = "";
    public String target = "";
    public String targetName = "";
    public int count = 1;
    public int progress;
    @Nullable public UUID giver;
    public String giverName = "";
    public int kingdom = -1;
    public int rewardCopper;
    public int rewardRep;
    public int rewardGuild;
    public int rewardXp;
    public long x, z;
    public boolean guildQuest;

    public boolean done() { return progress >= count; }

    public String describe() {
        String p = " (" + Math.min(progress, count) + "/" + count + ")";
        return switch (type) {
            case KILL -> targetName + " avla" + p;
            case KILL_ANY -> "Herhangi " + count + " canavar öldür" + p;
            case BOSS -> targetName + " adlı canavarı yen";
            case FETCH -> count + " adet " + targetName + " getir" + p;
            case DELIVER -> targetName + " teslim et" + p;
            case VISIT -> targetName + " yerine git";
            case BOUNTY -> targetName + " adlı haydudu yakala" + p;
        };
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Type", type.name());
        t.putString("Title", title);
        t.putString("Target", target);
        t.putString("TargetName", targetName);
        t.putInt("Count", count);
        t.putInt("Progress", progress);
        if (giver != null) t.putUUID("Giver", giver);
        t.putString("GiverName", giverName);
        t.putInt("Kingdom", kingdom);
        t.putIntArray("Reward", new int[]{rewardCopper, rewardRep, rewardGuild, rewardXp});
        t.putLong("X", x);
        t.putLong("Z", z);
        t.putBoolean("Guild", guildQuest);
        return t;
    }

    public static Quest load(CompoundTag t) {
        Quest q = new Quest();
        try { q.type = Type.valueOf(t.getString("Type")); } catch (IllegalArgumentException ignored) {}
        q.title = t.getString("Title");
        q.target = t.getString("Target");
        q.targetName = t.getString("TargetName");
        q.count = Math.max(1, t.getInt("Count"));
        q.progress = t.getInt("Progress");
        if (t.hasUUID("Giver")) q.giver = t.getUUID("Giver");
        q.giverName = t.getString("GiverName");
        q.kingdom = t.getInt("Kingdom");
        int[] r = t.getIntArray("Reward");
        if (r.length == 4) { q.rewardCopper = r[0]; q.rewardRep = r[1]; q.rewardGuild = r[2]; q.rewardXp = r[3]; }
        q.x = t.getLong("X");
        q.z = t.getLong("Z");
        q.guildQuest = t.getBoolean("Guild");
        return q;
    }
}
