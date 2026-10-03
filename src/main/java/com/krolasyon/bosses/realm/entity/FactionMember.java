package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.realm.Faction;

import javax.annotation.Nullable;

/** an inhabitant of one of the five kingdoms */
public interface FactionMember {
    @Nullable
    Faction faction();

    /** kingdom lord (killing it conquers the kingdom) */
    default boolean isLord() { return false; }

    /** the Tyrant of the Crimson Throne */
    default boolean isTyrant() { return false; }

    default boolean isEnvoy() { return false; }
}
