package com.krolasyon.futbol.entity;

import com.krolasyon.futbol.game.BotBrain;
import com.krolasyon.futbol.game.BotNames;
import com.krolasyon.futbol.game.Team;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** AI football player ("bot"). Uses the same move system and animations as real players. */
public class FootballerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Byte> DATA_TEAM = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_NUMBER = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_KEEPER = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_ROLE = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_RARITY = SynchedEntityData.defineId(FootballerEntity.class, EntityDataSerializers.BYTE);

    public static final int SKINS = 8;

    // stats 0..1
    public float statSpeed = 0.7F, statShot = 0.7F, statPass = 0.7F, statDribble = 0.7F, statDefend = 0.7F, statKeeper = 0.5F;
    public int slot;
    public String baseName = "Bot";
    public final BotBrain brain = new BotBrain(this);

    public FootballerEntity(EntityType<? extends FootballerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setMaxUpStep(0.6F);
        if (!level.isClientSide) randomize();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TEAM, (byte) Team.RED.ordinal());
        this.entityData.define(DATA_NUMBER, 10);
        this.entityData.define(DATA_SKIN, 0);
        this.entityData.define(DATA_KEEPER, false);
        this.entityData.define(DATA_ROLE, (byte) com.krolasyon.futbol.game.Role.MID.ordinal());
        this.entityData.define(DATA_RARITY, (byte) -1);
    }

    private void randomize() {
        statSpeed = 0.5F + random.nextFloat() * 0.45F;
        statShot = 0.5F + random.nextFloat() * 0.45F;
        statPass = 0.5F + random.nextFloat() * 0.45F;
        statDribble = 0.5F + random.nextFloat() * 0.45F;
        statDefend = 0.5F + random.nextFloat() * 0.45F;
        statKeeper = 0.55F + random.nextFloat() * 0.4F;
        baseName = BotNames.random(random);
        this.entityData.set(DATA_SKIN, random.nextInt(SKINS));
        applySpeed();
    }

    public void applySpeed() {
        var a = getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) a.setBaseValue(0.205 + statSpeed * 0.035);
    }

    public Team getFootTeam() { return Team.byId(this.entityData.get(DATA_TEAM)); }

    public void setFootTeam(Team t) {
        this.entityData.set(DATA_TEAM, (byte) t.ordinal());
        refreshName();
    }

    public int getNumber() { return this.entityData.get(DATA_NUMBER); }

    public void setNumber(int n) {
        this.entityData.set(DATA_NUMBER, n);
        refreshName();
    }

    public int getSkin() { return this.entityData.get(DATA_SKIN); }

    public boolean isKeeper() { return this.entityData.get(DATA_KEEPER); }

    public com.krolasyon.futbol.game.Role getRole() { return com.krolasyon.futbol.game.Role.byId(this.entityData.get(DATA_ROLE)); }

    public void setRole(com.krolasyon.futbol.game.Role r) {
        this.entityData.set(DATA_ROLE, (byte) r.ordinal());
        refreshName();
    }

    /** -1 = plain bot, otherwise the rarity of the player card it represents */
    public int getRarity() { return this.entityData.get(DATA_RARITY); }

    public void setSkin(int skin) { this.entityData.set(DATA_SKIN, skin); }

    /** turn this bot into the player described by a card */
    public void applyCard(com.krolasyon.futbol.game.CardData.Card c) {
        baseName = c.name;
        statSpeed = c.stats[0] / 100F;
        statShot = c.stats[1] / 100F;
        statPass = c.stats[2] / 100F;
        statDribble = c.stats[3] / 100F;
        statDefend = c.stats[4] / 100F;
        statKeeper = c.stats[5] / 100F;
        this.entityData.set(DATA_SKIN, c.skin);
        this.entityData.set(DATA_RARITY, (byte) c.rarity);
        applySpeed();
        refreshName();
    }

    public void setKeeper(boolean k) {
        this.entityData.set(DATA_KEEPER, k);
        if (k) {
            statKeeper = Math.max(statKeeper, 0.75F);
            this.entityData.set(DATA_ROLE, (byte) com.krolasyon.futbol.game.Role.GK.ordinal());
        }
        refreshName();
    }

    public void refreshName() {
        Team t = getFootTeam();
        String role = " (" + getRole().abbr + ")";
        int r = getRarity();
        net.minecraft.ChatFormatting star = r == 3 ? net.minecraft.ChatFormatting.LIGHT_PURPLE : r == 2 ? net.minecraft.ChatFormatting.GOLD : t.chat;
        setCustomName(Component.literal((r >= 2 ? "★ " : "") + getNumber() + " " + baseName + role).withStyle(star));
        setCustomNameVisible(false);
    }

    @Override
    protected void registerGoals() {}

    /** client-only replay driver (goal replays drive fake copies of players) */
    public java.util.function.Consumer<net.minecraft.world.entity.Entity> replayDriver;

    @Override
    public void tick() {
        if (replayDriver != null) replayDriver.accept(this);
        super.tick();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        brain.tick();
    }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && p.isCreative()) return super.hurt(source, amount);
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        return false;
    }

    @Override
    public boolean isPushable() { return true; }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dim) { return 1.62F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("FTeam", (byte) getFootTeam().ordinal());
        tag.putInt("FNumber", getNumber());
        tag.putInt("FSkin", getSkin());
        tag.putBoolean("FKeeper", isKeeper());
        tag.putInt("FSlot", slot);
        tag.putByte("FRole", this.entityData.get(DATA_ROLE));
        tag.putByte("FRarity", this.entityData.get(DATA_RARITY));
        tag.putString("FName", baseName);
        tag.putFloat("SSpeed", statSpeed);
        tag.putFloat("SShot", statShot);
        tag.putFloat("SPass", statPass);
        tag.putFloat("SDribble", statDribble);
        tag.putFloat("SDefend", statDefend);
        tag.putFloat("SKeeper", statKeeper);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FTeam")) {
            this.entityData.set(DATA_TEAM, tag.getByte("FTeam"));
            this.entityData.set(DATA_NUMBER, tag.getInt("FNumber"));
            this.entityData.set(DATA_SKIN, tag.getInt("FSkin"));
            this.entityData.set(DATA_KEEPER, tag.getBoolean("FKeeper"));
            slot = tag.getInt("FSlot");
            this.entityData.set(DATA_ROLE, tag.getByte("FRole"));
            this.entityData.set(DATA_RARITY, tag.getByte("FRarity"));
            baseName = tag.getString("FName");
            statSpeed = tag.getFloat("SSpeed");
            statShot = tag.getFloat("SShot");
            statPass = tag.getFloat("SPass");
            statDribble = tag.getFloat("SDribble");
            statDefend = tag.getFloat("SDefend");
            statKeeper = tag.getFloat("SKeeper");
            applySpeed();
        }
    }
}
