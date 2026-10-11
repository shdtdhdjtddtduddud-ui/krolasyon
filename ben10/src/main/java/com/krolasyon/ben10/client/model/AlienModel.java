package com.krolasyon.ben10.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Baked alien rig. Bones are looked up by name; missing bones are simply skipped by the animator. */
public class AlienModel extends HierarchicalModel<Entity> {
    private final ModelPart root;
    private final Map<String, Optional<ModelPart>> cache = new HashMap<>();

    public AlienModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    /** bone by name or null */
    public ModelPart bone(String name) {
        return cache.computeIfAbsent(name, n -> root.getAllParts().filter(p -> p.hasChild(n)).findFirst().map(p -> p.getChild(n))).orElse(null);
    }

    public void reset() {
        root.getAllParts().forEach(ModelPart::resetPose);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}
}
