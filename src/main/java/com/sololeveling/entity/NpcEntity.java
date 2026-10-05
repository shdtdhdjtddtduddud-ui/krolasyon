package com.sololeveling.entity;

import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Hunter-world citizens: clerks, guild masters, merchants, journalists, healers, hunters. */
public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> ROLE = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> GUILD = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, "citizen");
        entityData.define(SKIN, "citizen");
        entityData.define(GUILD, "");
    }

    public String role() { return entityData.get(ROLE); }
    public String skin() { return entityData.get(SKIN); }
    public String guild() { return entityData.get(GUILD); }

    public void setup(String role, String skin, String name, String guild) {
        entityData.set(ROLE, role);
        entityData.set(SKIN, skin);
        entityData.set(GUILD, guild);
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.0D) {
            @Override protected boolean shouldPanic() { return false; }
        });
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.7D));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        return !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !src.isCreativePlayer();
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPersistenceRequired() { return true; }
    @Override public boolean requiresCustomPersistence() { return true; }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            getLookControl().setLookAt(player);
            Net.toPlayer(sp, new Packets.OpenNpc(getId(), role(), getCustomName() == null ? "" : getCustomName().getString(), guild()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("role", role()); t.putString("skin", skin()); t.putString("guild", guild());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("role")) entityData.set(ROLE, t.getString("role"));
        if (t.contains("skin")) entityData.set(SKIN, t.getString("skin"));
        if (t.contains("guild")) entityData.set(GUILD, t.getString("guild"));
    }
}
