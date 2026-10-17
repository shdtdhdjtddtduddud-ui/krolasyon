package com.krolasyon.mirror.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** The other you, after the worlds merged. Wanders around the house it built and talks when you click it. */
public class ReflectionEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int LINES = 8;
    private String ownerName = "";
    @Nullable private BlockPos home;

    public ReflectionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public void setOwner(@Nullable UUID id, String name) {
        entityData.set(OWNER, Optional.ofNullable(id));
        ownerName = name == null ? "" : name;
        if (!ownerName.isEmpty()) setCustomName(Component.translatable("entity.mirrorworld.reflection.of", ownerName));
    }

    @Nullable
    public UUID getOwnerId() {
        return entityData.get(OWNER).orElse(null);
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, 7);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) {
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            swing(InteractionHand.MAIN_HAND);
            player.sendSystemMessage(Component.translatable("mirrorworld.reflection.says", getDisplayName(),
                    Component.translatable("mirrorworld.reflection.line" + random.nextInt(LINES))).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID id = getOwnerId();
        if (id != null) tag.putUUID("Owner", id);
        tag.putString("OwnerName", ownerName);
        if (home != null) tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        ownerName = tag.getString("OwnerName");
        if (tag.contains("Home")) setHome(BlockPos.of(tag.getLong("Home")));
    }
}
