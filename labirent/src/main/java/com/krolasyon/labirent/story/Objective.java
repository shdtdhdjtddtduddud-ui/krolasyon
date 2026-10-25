package com.krolasyon.labirent.story;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Team-wide story objectives, grouped by chapter. */
public enum Objective {
    TALK_NEWT(0, 1),
    READ_JOURNAL(0, 1),
    WATCH_DOORS(0, 1),

    ASSIGN_RUNNER(1, 1),
    ASSIGN_COOK(1, 1),
    EAT_MEAL(1, 1),

    MAP_10(2, 10),
    MAZE_TIME(2, 180),

    KILL_GRIEVER(3, 1),
    BEN_FATE(3, 1),

    MAP_30(4, 30),
    FIND_STONES(4, 4),

    TALK_TERESA(5, 1),
    SURVIVE_NIGHTS(5, 3),

    CODE_WORDS(6, 8),
    REMEMBER(6, 1),

    GET_CORE(7, 1),
    ENTER_HOLE(7, 1),
    ENTER_CODE(7, 1);

    public final int chapter;
    public final int target;

    Objective(int chapter, int target) {
        this.chapter = chapter;
        this.target = target;
    }

    public MutableComponent text() { return Component.translatable("objective.labirent." + name().toLowerCase()); }
}
