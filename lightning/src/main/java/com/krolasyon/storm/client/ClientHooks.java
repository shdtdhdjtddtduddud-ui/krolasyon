package com.krolasyon.storm.client;

import com.krolasyon.storm.Skill;
import com.krolasyon.storm.StormData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;
import java.util.Set;

/** Client entry points called from packets/items, plus the local mirror of the player's skill data. */
public final class ClientHooks {
    public static final StormData DATA = new StormData();
    public static boolean synced;
    /** smoothed values for the HUD */
    public static float shownEnergy = -1;
    static float trauma, flash;

    private ClientHooks() {}

    public static void openTree() { Minecraft.getInstance().setScreen(new SkillTreeScreen()); }

    public static void onSync(CompoundTag tag) {
        Set<Skill> before = EnumSet.copyOf(DATA.unlocked.isEmpty() ? EnumSet.noneOf(Skill.class) : DATA.unlocked);
        DATA.load(tag);
        if (synced) {
            for (Skill s : DATA.unlocked) if (!before.contains(s)) SkillTreeScreen.onUnlocked(s);
        }
        synced = true;
    }

    public static void onAnim(int entity, int skill) {
        Skill s = Skill.byId(skill);
        if (s == null) return;
        CastAnim.start(entity, s);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == entity) {
            shake(switch (s) {
                case THUNDER_NOVA -> 0.35F;
                case LIGHTNING_SPEAR, THUNDERSTORM, STORM_AVATAR -> 0.25F;
                default -> 0.1F;
            });
        }
    }

    public static void shake(float amount) { trauma = Math.min(1F, trauma + amount); }

    public static void flash(float amount) { flash = Math.max(flash, amount); }
}
