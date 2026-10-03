package com.krolasyon.bosses.client.model;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.entity.mob.MobSpec;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Model of any generic Azrakor creature; bones and animations come from its JSON file. */
public class MobModel extends BossModel<HellMob> {
    private final MobSpec spec;
    private final Map<String, AnimationDefinition> anims;

    public static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, id), "main");
    }

    public MobModel(ModelPart root, MobSpec spec) {
        super(root, spec.head);
        this.spec = spec;
        this.anims = MobModelLoader.anims(spec.id);
    }

    @Override protected AnimationDefinition idle() { return anims.get("idle"); }
    @Override protected AnimationDefinition walk() { return anims.getOrDefault("walk", anims.get("idle")); }
    @Override protected AnimationDefinition run() { return anims.getOrDefault("run", walk()); }
    @Override protected AnimationDefinition death() { return anims.getOrDefault("death", anims.get("idle")); }
    @Override protected float walkSpeed() { return (float) spec.walkAnim; }
    @Override protected float runSpeed() { return (float) spec.runAnim; }

    @Override
    protected AnimationDefinition ability(int id) {
        if (id < 0 || id >= spec.abilities.size()) return null;
        return anims.get(spec.abilities.get(id).anim);
    }
}
