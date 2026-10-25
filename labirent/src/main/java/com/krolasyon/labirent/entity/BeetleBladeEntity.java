package com.krolasyon.labirent.entity;

import com.krolasyon.labirent.registry.ModItems;
import com.krolasyon.labirent.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** WICKED's spy: a little mechanical lizard with a red eye that scuttles along the walls and watches. */
public class BeetleBladeEntity extends PathfinderMob {
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(BeetleBladeEntity.class, EntityDataSerializers.BYTE);

    public BeetleBladeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_FLAGS, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 3.5F, 1.2, 1.6));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.05F));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12F, 0.6F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return new WallClimberNavigation(this, level); }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            byte f = entityData.get(DATA_FLAGS);
            f = horizontalCollision ? (byte) (f | 1) : (byte) (f & ~1);
            Player p = level().getNearestPlayer(this, 10);
            boolean watching = p != null && hasLineOfSight(p);
            f = watching ? (byte) (f | 2) : (byte) (f & ~2);
            entityData.set(DATA_FLAGS, f);
        }
    }

    public boolean isClimbing() { return (entityData.get(DATA_FLAGS) & 1) != 0; }

    public boolean isWatching() { return (entityData.get(DATA_FLAGS) & 2) != 0; }

    @Override
    public boolean onClimbable() { return isClimbing(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (random.nextInt(3) == 0) playSound(ModSounds.BEETLE_SCUTTLE.get(), 0.4F, 1F + random.nextFloat() * 0.3F);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean hit) {
        super.dropCustomDeathLoot(src, looting, hit);
        spawnAtLocation(new ItemStack(ModItems.BEETLE_EYE.get()));
    }

    @Override
    public boolean removeWhenFarAway(double d) { return d > 96 * 96; }
}
