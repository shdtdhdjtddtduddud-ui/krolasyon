package com.krolasyon.futbol.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class PitchData extends SavedData {
    public Pitch pitch;

    public static PitchData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PitchData::load, PitchData::new, "krolasyonfutbol_pitch");
    }

    private static PitchData load(CompoundTag tag) {
        PitchData d = new PitchData();
        if (tag.contains("pitch")) d.pitch = Pitch.load(tag.getCompound("pitch"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (pitch != null) tag.put("pitch", pitch.save());
        return tag;
    }
}
