package com.krolasyon.bosses.realm.client.render;

import com.krolasyon.bosses.client.model.BossModel;
import com.krolasyon.bosses.realm.client.gen.GenModels;
import com.krolasyon.bosses.realm.entity.RealmBoss;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelPart;

public class GenBossModel extends BossModel<RealmBoss> {
    private final GenModels.Info info;

    public GenBossModel(ModelPart root, GenModels.Info info) {
        super(root, info.head());
        this.info = info;
    }

    private AnimationDefinition a(int i) { return info.anims()[i]; }

    @Override protected AnimationDefinition idle() { return a(0); }
    @Override protected AnimationDefinition walk() { return a(1); }
    @Override protected AnimationDefinition run() { return a(6) != null ? a(6) : a(1); }
    @Override protected AnimationDefinition death() { return a(5); }
    @Override protected float walkSpeed() { return info.walkSpeed() * 2.0F; }
    @Override protected float runSpeed() { return info.runSpeed() * 2.0F; }

    @Override
    protected AnimationDefinition ability(int id) { return id >= 2 && id <= 4 ? a(id) : null; }
}
