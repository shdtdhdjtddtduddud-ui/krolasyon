package com.krolasyon.horror.compat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;

/** Version glue (1.21.1 / GeckoLib 4.5+): synced state, pose-dependent size and the GeckoLib controllers. */
public abstract class CompatMonster extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CompatMonster.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(CompatMonster.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> loops = new HashMap<>();

    protected CompatMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, 0);
        builder.define(FLAGS, 0);
    }

    protected int getSyncedState() { return this.entityData.get(STATE); }

    protected void setSyncedState(int s) { this.entityData.set(STATE, s); }

    public int getFlags() { return this.entityData.get(FLAGS); }

    protected void setFlags(int f) { this.entityData.set(FLAGS, f); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FLAGS.equals(key)) this.refreshDimensions();
    }

    protected abstract EntityDimensions sizeFor(Pose pose);

    public abstract String loopAnimation(boolean moving);

    protected abstract String[] actionNames();

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) { return sizeFor(pose); }

    protected void playAction(String name) { this.triggerAnim("action", name); }

    private boolean clientMoving() {
        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        return dx * dx + dz * dz > 2.5E-4;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "move", 5, state -> {
            String n = loopAnimation(clientMoving());
            RawAnimation raw = loops.computeIfAbsent(n, k -> k.equals("death") || k.equals("emerge")
                    ? RawAnimation.begin().thenPlayAndHold("animation.hatman." + k)
                    : RawAnimation.begin().thenLoop("animation.hatman." + k));
            return state.setAndContinue(raw);
        }));
        AnimationController<CompatMonster> act = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String a : actionNames()) act.triggerableAnim(a, RawAnimation.begin().thenPlay("animation.hatman." + a));
        reg.add(act);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
