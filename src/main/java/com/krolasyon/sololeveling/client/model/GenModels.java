package com.krolasyon.sololeveling.client.model;

import com.krolasyon.sololeveling.entity.MobKind;
import net.minecraft.client.model.geom.builders.*;

public final class GenModels {
    public enum RigType { BIPED, QUAD, SERPENT, CENTIPEDE }
    public record Rig(RigType type, float walkFreq, float walkAmp, float lean, boolean twoHanded, float scale, float shadow) {}
    public static LayerDefinition create(MobKind k) { MeshDefinition m = new MeshDefinition(); return LayerDefinition.create(m, 64, 64); }
    public static String[] bones(MobKind k) { return new String[0]; }
    public static Rig rig(MobKind k) { return new Rig(RigType.BIPED, 0.6F, 1F, 0F, false, 1F, 0.5F); }
}
