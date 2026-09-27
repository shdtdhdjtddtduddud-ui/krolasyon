package com.krolasyon.bosses.morph;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server logic of one form: its five abilities, passives and transformation effects. */
public interface FormAbilities {
    void tick(ServerPlayer p, MorphServer.State s, ServerLevel sl, int id, int t);

    /** every tick while transformed (after the shared passives) */
    default void passive(ServerPlayer p, MorphServer.State s, ServerLevel sl) {}

    /** cocoon effects before the burst */
    void transformTick(ServerPlayer p, ServerLevel sl, int t);

    /** the burst when the form emerges */
    void transformBurst(ServerPlayer p, ServerLevel sl);
}
