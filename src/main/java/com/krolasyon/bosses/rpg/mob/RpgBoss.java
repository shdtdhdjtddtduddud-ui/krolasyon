package com.krolasyon.bosses.rpg.mob;

import com.krolasyon.bosses.rpg.def.RpgDefs.Ability;
import com.krolasyon.bosses.rpg.util.FX;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Data-driven boss: boss bar, second phase at half health, faster ability rotation and a long death. */
public class RpgBoss extends RpgMonster {
    private static final EntityDataAccessor<Boolean> PHASE2 = SynchedEntityData.defineId(RpgBoss.class, EntityDataSerializers.BOOLEAN);
    public static final byte EVENT_PHASE2 = 90;
    public static final int DEATH_TICKS = 70;

    private final ServerBossEvent bossEvent;
    public int phaseFlash;

    public RpgBoss(EntityType<? extends RpgMonster> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(Component.literal(def().name()), color(), BossEvent.BossBarOverlay.NOTCHED_12);
        this.bossEvent.setDarkenScreen(true);
        this.xpReward = 200 + (int) def().hp() / 2;
        this.setPersistenceRequired();
    }

    private BossEvent.BossBarColor color() {
        int c = def().eyeColor();
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        if (r > 200 && g > 200 && b > 200) return BossEvent.BossBarColor.WHITE;
        if (r > g && r > b) return g > 150 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED;
        if (g > r && g > b) return BossEvent.BossBarColor.GREEN;
        if (b > r && b > g) return r > 150 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.BLUE;
        return BossEvent.BossBarColor.PINK;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE2, false);
    }

    public boolean isPhase2() { return this.entityData.get(PHASE2); }

    @Override
    public float powerMultiplier() { return isPhase2() ? 1.3F : 1.0F; }

    @Override
    protected float cooldownScale() { return isPhase2() ? 0.6F : 0.85F; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean canChangeDimensions() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(4.0); }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        this.bossEvent.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        this.bossEvent.removePlayer(p);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!isPhase2() && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.entityData.set(PHASE2, true);
            this.level().broadcastEntityEvent(this, EVENT_PHASE2);
            FX.sound(this, SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.7F);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
            this.heal(this.getMaxHealth() * 0.05F);
            FX.burstRing(level(), FX.dust(def().eyeColor(), 3.0F), this.position().add(0, 1, 0), 60, 0.8);
            FX.burstRing(level(), ParticleTypes.EXPLOSION, this.position().add(0, 1, 0), 10, 0.5);
            for (LivingEntity e : com.krolasyon.bosses.rpg.util.Combat.victims(this, this.position(), 8)) {
                com.krolasyon.bosses.rpg.util.Combat.knock(this.position(), e, 1.6, 0.6);
            }
            for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40))) {
                p.displayClientMessage(Component.literal("§4§l" + def().name() + " öfkeleniyor!"), true);
            }
            // pick summon early in phase two if available
            Ability[] ab = def().abilities();
            for (int i = 0; i < ab.length; i++) if (ab[i] == Ability.SUMMON && this.getTarget() != null) { startCast(i, this.getTarget()); break; }
        }
        if (this.getTarget() == null && this.tickCount % 100 == 0 && this.getHealth() < this.getMaxHealth()) this.heal(this.getMaxHealth() * 0.02F);
    }

    @Override
    protected void ambientFx() {
        super.ambientFx();
        if (isPhase2() && this.tickCount % 2 == 0) {
            double a = this.tickCount * 0.25;
            double r = this.getBbWidth() * 0.8 + 0.4;
            this.level().addParticle(FX.dust(def().eyeColor(), 1.4F), this.getX() + Math.cos(a) * r, this.getY() + 0.2, this.getZ() + Math.sin(a) * r, 0, 0.12, 0);
        }
        if (phaseFlash > 0) phaseFlash--;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_PHASE2) phaseFlash = 40;
        else super.handleEntityEvent(id);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        Vec3 c = this.position().add(0, this.getBbHeight() * 0.5, 0);
        if (this.level().isClientSide()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(FX.dust(def().eyeColor(), 2.0F), c.x + (random.nextDouble() - 0.5) * getBbWidth() * 2, c.y + (random.nextDouble() - 0.5) * getBbHeight(),
                        c.z + (random.nextDouble() - 0.5) * getBbWidth() * 2, 0, 0.1, 0);
            }
            if (this.deathTime % 10 == 0) this.level().addParticle(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 0, 0, 0);
        } else if (this.deathTime % 15 == 0) {
            FX.sound(this, SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F + this.deathTime / 100F);
        }
        if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            FX.send(level(), ParticleTypes.EXPLOSION_EMITTER, c, 3, getBbWidth() * 0.5, 0);
            FX.burstRing(level(), ParticleTypes.END_ROD, c, 60, 0.5);
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        this.bossEvent.setProgress(0F);
        this.casting = -1;
        if (!this.level().isClientSide()) {
            for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(64))) {
                p.displayClientMessage(Component.literal("§6§l" + def().name() + " yenildi!"), false);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phase2", isPhase2());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(PHASE2, tag.getBoolean("Phase2"));
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }
}
