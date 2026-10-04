package com.rabona.arena.entity;

import com.rabona.arena.game.Athlete;
import com.rabona.arena.game.BotBrain;
import com.rabona.arena.game.MoveLogic;
import com.rabona.arena.game.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Bot futbolcu. Takim formasi, numarasi, yetenek puani ve yapay zekasi vardir. */
public class FootballerEntity extends PathfinderMob {
    public static final int SKINS = 8;

    private static final EntityDataAccessor<Integer> TEAM = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NUMBER = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKILL = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);

    private final BotBrain brain = new BotBrain(this);
    private String baseName = "Bot";

    public FootballerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setMaxUpStep(0.6f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 64);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TEAM, Team.RED.ordinal());
        entityData.define(NUMBER, 7);
        entityData.define(SKIN, 0);
        entityData.define(SLOT, 1);
        entityData.define(SKILL, 75);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    public void setup(Team t, int number, int skin, String name, int skill) {
        entityData.set(TEAM, t.ordinal());
        entityData.set(NUMBER, number);
        entityData.set(SKIN, skin);
        entityData.set(SKILL, Math.min(99, skill));
        baseName = name;
        refreshName();
    }

    private void refreshName() {
        Team t = getSquad();
        setCustomName(Component.literal(getNumber() + " ").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                .append(Component.literal(baseName).withStyle(t.chat))
                .append(Component.literal(" [" + getSkill() + "]").withStyle(ChatFormatting.GOLD)));
        setCustomNameVisible(true);
    }

    public Team getSquad() { return Team.byId(entityData.get(TEAM)); }

    public int getNumber() { return entityData.get(NUMBER); }

    public void setNumber(int n) {
        entityData.set(NUMBER, n);
        refreshName();
    }

    public int getSkinId() { return entityData.get(SKIN); }

    public int getSlot() { return entityData.get(SLOT); }

    public void setSlot(int s) { entityData.set(SLOT, s); }

    public int getSkill() { return entityData.get(SKILL); }

    public boolean isKeeper() { return getSlot() == 0; }

    public String getBaseName() { return baseName; }

    public void onBallBlocked(Vec3 v) {}

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        brain.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) MoveLogic.tick(this);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypes.FELL_OUT_OF_WORLD) || src.is(DamageTypes.GENERIC_KILL)) return super.hurt(src, amount);
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean canBeLeashed(Player p) { return false; }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            Athlete a = Athlete.of(this);
            player.displayClientMessage(Component.translatable("msg.rabonaarena.bot_card", getDisplayName(),
                    Component.translatable("role.rabonaarena." + roleKey()), getSkill(), a.goals, a.assists, (int) a.stamina), false);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public String roleKey() {
        int s = getSlot();
        if (s == 0) return "gk";
        if (s == 1 || s == 5 || s == 6 || s == 10) return "def";
        if (s == 2 || s == 8 || s == 9) return "fwd";
        return "mid";
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && reason.shouldDestroy()) Athlete.remove(getUUID());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("RTeam", entityData.get(TEAM));
        t.putInt("RNumber", getNumber());
        t.putInt("RSkin", getSkinId());
        t.putInt("RSlot", getSlot());
        t.putInt("RSkill", getSkill());
        t.putString("RName", baseName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setup(Team.byId(t.getInt("RTeam")), t.getInt("RNumber"), t.getInt("RSkin"), t.getString("RName"), t.getInt("RSkill"));
        setSlot(t.getInt("RSlot"));
    }
}
