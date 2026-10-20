package com.krolasyon.elvenrealm.entity;

import com.krolasyon.elvenrealm.dialogue.DialogueManager;
import com.krolasyon.elvenrealm.dialogue.Lines;
import com.krolasyon.elvenrealm.dialogue.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sylvaren elfi. Her karakter kendi EntityType'ına sahiptir ama hepsi bu sınıfı paylaşır.
 * Oyuncular elflere zarar veremez; elfler ölmez, ağır yaralanınca evlerine çekilip bir gün dinlenirler.
 */
public class ElfEntity extends PathfinderMob implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(ElfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ElfEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final byte EVENT_CAST = 60, EVENT_LUNGE = 61;
    public static final int DAY = 24000;

    public final ElfKind kind;
    @Nullable private BlockPos home;
    private long hireUntil, recoverUntil;
    private int activityTimer, abilityCd, chatterCd = 400, supportCd = 100, calmTicks;
    private boolean empowered;
    @Nullable private UUID talkingTo;
    private final Map<UUID, Long> greeted = new HashMap<>();
    /** Yalnızca istemci: büyü/hamle animasyonu sayaçları. */
    public int castAnim, lungeAnim;

    public ElfEntity(EntityType<? extends ElfEntity> type, Level level, ElfKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 0;
        this.setCanPickUpLoot(false);
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0F);
        ((net.minecraft.world.entity.ai.navigation.GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        if (!level.isClientSide) setItemSlot(EquipmentSlot.MAINHAND, idleItem());
        addGoals();
    }

    public static AttributeSupplier.Builder attributes(ElfKind k) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.health)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.MOVEMENT_SPEED, k == ElfKind.VAELOR || k == ElfKind.LIRAEL ? 0.33 : 0.3)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTIVITY, 0);
        entityData.define(OWNER, Optional.empty());
    }

    // ------------------------------------------------------------------ hedefler

    /** Mob kurucusu registerGoals'u kind atanmadan çağırır; hedefler bu yüzden kurucunun sonunda eklenir. */
    @Override
    protected void registerGoals() {}

    private void addGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        switch (kind.role) {
            case ARCHER -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 22, 16F));
            case RANGER -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 18F));
            case ARCANE -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 12F));
            case HEALER -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 50, 10F));
            default -> goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        }
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !isHired() && !isTalking() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new ActivityGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55) {
            @Override
            public boolean canUse() {
                return !isHired() && !isTalking() && getActivity() == Activity.NONE && super.canUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class, ElfEntity.class));
        targetSelector.addGoal(2, new DefendOwnerGoal());
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 8, true, false, this::wantsToFight));
    }

    private boolean wantsToFight(LivingEntity e) {
        if (!(e instanceof Enemy) || e instanceof EnderMan || e instanceof ZombifiedPiglin) return false;
        if (e instanceof Creeper && !kind.ranged() && kind.role != ElfKind.Role.ARCANE) return false;
        if (isRecovering()) return false;
        if (isHired()) return true;
        return home == null || home.distSqr(e.blockPosition()) < 24 * 24;
    }

    // ------------------------------------------------------------------ durum

    public Activity getActivity() {
        return Activity.of(entityData.get(ACTIVITY));
    }

    public void setActivity(Activity a, int ticks) {
        entityData.set(ACTIVITY, a.ordinal());
        activityTimer = ticks;
    }

    public boolean isHired() {
        return entityData.get(OWNER).isPresent();
    }

    @Nullable
    public UUID ownerId() {
        return entityData.get(OWNER).orElse(null);
    }

    @Nullable
    public Player owner() {
        UUID id = ownerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public long hireRemaining() {
        return Math.max(0, hireUntil - level().getGameTime());
    }

    public boolean isRecovering() {
        return level().getGameTime() < recoverUntil;
    }

    public long recoverRemaining() {
        return Math.max(0, recoverUntil - level().getGameTime());
    }

    public boolean isTalking() {
        return talkingTo != null;
    }

    public void setHome(BlockPos p) {
        home = p.immutable();
        if (!isHired()) restrictTo(home, 14);
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public void hire(Player p, long ticks) {
        entityData.set(OWNER, Optional.of(p.getUUID()));
        hireUntil = level().getGameTime() + ticks;
        PlayerData.setCompanion(p, getUUID());
        clearRestriction();
        setActivity(Activity.NONE, 0);
    }

    public void extend(long ticks) {
        hireUntil += ticks;
    }

    /** Sözleşmeyi bitirir; elf evine yürür. */
    public void unhire(@Nullable String lineKey) {
        Player p = owner();
        if (p != null) {
            if (getUUID().equals(PlayerData.companion(p))) PlayerData.setCompanion(p, null);
            if (lineKey != null) say(p, lineKey, false);
        }
        entityData.set(OWNER, Optional.empty());
        setTarget(null);
        if (home != null) restrictTo(home, 14);
    }

    public void startTalking(Player p) {
        talkingTo = p.getUUID();
        getNavigation().stop();
        setActivity(Activity.TALK, 1200);
    }

    public void stopTalking() {
        talkingTo = null;
        if (getActivity() == Activity.TALK) setActivity(Activity.NONE, 0);
    }

    public Component displayName() {
        return Component.literal(kind.name).withStyle(kind == ElfKind.VAELOR ? ChatFormatting.DARK_PURPLE
                : kind == ElfKind.NIMRAE ? ChatFormatting.GOLD : kind == ElfKind.CAELAN ? ChatFormatting.AQUA
                : kind == ElfKind.GUARD ? ChatFormatting.GRAY : ChatFormatting.GREEN);
    }

    /** Karakterin kendi tarzında bir satırı oyuncuya söyler (sohbet ya da eylem çubuğu). */
    public void say(Player p, String key, boolean actionBar) {
        String line = Lines.get(kind, key, random).replace("{p}", p.getName().getString());
        Component c = Component.empty().append(displayName()).append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE));
        p.displayClientMessage(c, actionBar);
        level().playSound(null, blockPosition(), voiceSound(), SoundSource.NEUTRAL, 0.5F, kind.voice + (random.nextFloat() - 0.5F) * 0.15F);
    }

    public SoundEvent voiceSound() {
        return switch (kind) {
            case VAELOR -> SoundEvents.NOTE_BLOCK_BASS.value();
            case THALION, GUARD -> SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
            case NIMRAE -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case TAVI -> SoundEvents.NOTE_BLOCK_BELL.value();
            default -> SoundEvents.NOTE_BLOCK_FLUTE.value();
        };
    }

    private ItemStack idleItem() {
        if (getActivity() == Activity.READ) return new ItemStack(Items.BOOK);
        if (getActivity() == Activity.PRACTICE || getTarget() != null) return weapon();
        return switch (kind) {
            case TAVI, THALION -> new ItemStack(Items.BOW);
            case GUARD -> new ItemStack(Items.IRON_SWORD);
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack weapon() {
        return switch (kind) {
            case LIRAEL -> new ItemStack(Items.IRON_SWORD);
            case VAELOR -> new ItemStack(Items.NETHERITE_SWORD);
            case TAVI, THALION -> new ItemStack(Items.BOW);
            case GUARD -> new ItemStack(Items.IRON_SWORD);
            case CAELAN -> new ItemStack(Items.ENCHANTED_BOOK);
            default -> ItemStack.EMPTY;
        };
    }

    // ------------------------------------------------------------------ etkileşim

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.SpawnEggItem) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            if (getTarget() != null && getTarget().isAlive()) {
                say(sp, "combat", true);
            } else {
                DialogueManager.open(sp, this);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        Entity a = src.getEntity();
        if (a instanceof ElfEntity) return false;
        if (a instanceof Player p && !src.is(DamageTypes.GENERIC_KILL)) {
            if (!level().isClientSide && random.nextInt(3) == 0) say(p, "hurt", true);
            return false;
        }
        boolean r = super.hurt(src, amount);
        if (r && !level().isClientSide && random.nextInt(6) == 0 && owner() != null) say(owner(), "hurt", true);
        return r;
    }

    /** Elfler ölmez: ağır yaralanınca evlerine ışınlanır ve bir gün dinlenir. Sözleşme biter, dostluk zedelenir. */
    @Override
    public void die(DamageSource src) {
        if (src.is(DamageTypes.GENERIC_KILL) || level().isClientSide) {
            super.die(src);
            return;
        }
        setHealth(getMaxHealth() * 0.5F);
        clearFire();
        removeAllEffects();
        setTarget(null);
        Player p = owner();
        if (p != null) {
            PlayerData.addFriendship(p, kind, -2);
            p.displayClientMessage(Component.translatable("msg.elvenrealm.fallen", displayName()).withStyle(ChatFormatting.RED), false);
            unhire(null);
        }
        recoverUntil = level().getGameTime() + DAY;
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1, getZ(), 40, 0.5, 0.8, 0.5, 0.02);
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5F, 0.6F);
        if (home != null) {
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            getNavigation().stop();
        }
        setActivity(Activity.RECOVER, (int) DAY);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof ElfEntity) return true;
        if (other instanceof Player p && p.getUUID().equals(ownerId())) return true;
        return super.isAlliedTo(other);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType type, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, type, data, tag);
        if (home == null) setHome(blockPosition());
        setPersistenceRequired();
        setItemSlot(EquipmentSlot.MAINHAND, idleItem());
        return r;
    }

    @Override
    public boolean canBeLeashed(Player p) {
        return false;
    }

    // ------------------------------------------------------------------ tik

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (castAnim > 0) castAnim--;
            if (lungeAnim > 0) lungeAnim--;
            clientParticles();
            return;
        }
        long now = level().getGameTime();
        if (activityTimer > 0 && --activityTimer == 0) setActivity(Activity.NONE, 0);
        if (getActivity() == Activity.RECOVER && !isRecovering()) setActivity(Activity.NONE, 0);
        if (abilityCd > 0) abilityCd--;
        if (supportCd > 0) supportCd--;
        if (chatterCd > 0) chatterCd--;
        setAggressive(getTarget() != null);

        // konuşma
        if (talkingTo != null) {
            Player p = level().getPlayerByUUID(talkingTo);
            if (p == null || p.distanceToSqr(this) > 64 || getActivity() != Activity.TALK) stopTalking();
            else {
                getNavigation().stop();
                getLookControl().setLookAt(p, 30F, 30F);
            }
        }

        // sözleşme
        if (isHired()) {
            if (now > hireUntil) unhire("hire_end");
            else if (hireUntil - now == 1200 && owner() != null)
                owner().displayClientMessage(Component.translatable("msg.elvenrealm.hire_soon", displayName()).withStyle(ChatFormatting.YELLOW), false);
        }

        // silah değişimi
        if (tickCount % 10 == 0) {
            if (getTarget() != null) calmTicks = 0;
            else calmTicks += 10;
            ItemStack want = getTarget() != null ? weapon() : idleItem();
            if (!ItemStack.isSameItem(getMainHandItem(), want) && (getTarget() != null || calmTicks > 60)) setItemSlot(EquipmentSlot.MAINHAND, want);
        }

        // dinlenirken yavaşça iyileş
        if (tickCount % 40 == 0 && getHealth() < getMaxHealth() && getTarget() == null) heal(1F);

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !isRecovering()) abilities(target);
        support();
        social(now);
    }

    private void abilities(LivingEntity target) {
        double d = distanceTo(target);
        switch (kind) {
            case LIRAEL -> {
                // Kılıç dansı: hedefe atılış
                if (abilityCd == 0 && d > 3.5 && d < 9 && onGround()) {
                    Vec3 dir = target.position().subtract(position()).normalize();
                    setDeltaMovement(dir.x * 1.05, 0.38, dir.z * 1.05);
                    hasImpulse = true;
                    level().broadcastEntityEvent(this, EVENT_LUNGE);
                    level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1F, 1.3F);
                    ((ServerLevel) level()).sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 2, 0.3, 0.2, 0.3, 0);
                    empowered = true;
                    abilityCd = 90;
                }
            }
            case VAELOR -> {
                // Gölge adımı: hedefin arkasına ışınlanır, ilk darbesi ağırdır
                if (abilityCd == 0 && d > 3 && d < 16) {
                    Vec3 back = Vec3.directionFromRotation(0, target.getYRot()).scale(-1.6);
                    Vec3 to = target.position().add(back);
                    if (level().noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                        ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.01);
                        teleportTo(to.x, to.y, to.z);
                        ((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                        level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.7F, 1.6F);
                        getLookControl().setLookAt(target);
                        empowered = true;
                        abilityCd = 160;
                    }
                }
            }
            case THALION -> {
                // Savaş borusu: sahibine saldıranları üstüne çeker
                Player o = owner();
                if (abilityCd == 0 && o != null) {
                    int n = 0;
                    for (Monster m : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(12))) {
                        if (m.getTarget() == o) {
                            m.setTarget(this);
                            n++;
                        }
                    }
                    if (n > 0) {
                        level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.NEUTRAL, 2F, 0.5F);
                        ((ServerLevel) level()).sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2.2, getZ(), 3, 0.3, 0.1, 0.3, 0);
                        abilityCd = 300;
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float mult = empowered ? (kind == ElfKind.VAELOR ? 1.8F : 1.4F) : 1F;
        if (empowered) {
            empowered = false;
            if (target instanceof LivingEntity le) {
                le.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (mult - 1F));
                le.invulnerableTime = 0;
            }
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
        }
        return super.doHurtTarget(target);
    }

    /** Destek yetenekleri: Nimrae iyileştirir, Caelan kalkan örer. */
    private void support() {
        if (supportCd > 0 || isRecovering()) return;
        Player o = owner();
        if (kind == ElfKind.NIMRAE) {
            LivingEntity patient = null;
            if (o != null && o.distanceToSqr(this) < 144 && o.getHealth() < o.getMaxHealth() * 0.75F) patient = o;
            else if (getHealth() < getMaxHealth() * 0.5F) patient = this;
            if (patient != null) {
                patient.heal(5F);
                level().broadcastEntityEvent(this, EVENT_CAST);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY() + 1.8, patient.getZ(), 5, 0.4, 0.3, 0.4, 0);
                ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, patient.getX(), patient.getY() + 1, patient.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
                level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1F, 1.4F);
                supportCd = 140;
            }
        } else if (kind == ElfKind.CAELAN && o != null && getTarget() != null && o.distanceToSqr(this) < 144) {
            o.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
            level().broadcastEntityEvent(this, EVENT_CAST);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ENCHANT, o.getX(), o.getY() + 1, o.getZ(), 40, 0.5, 1, 0.5, 0.5);
            level().playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 1F, 1.2F);
            supportCd = 600;
        }
    }

    /** Sohbet: yol arkadaşıyken laf atar, kentte yaklaşanlara seslenir. */
    private void social(long now) {
        Player o = owner();
        if (o != null) {
            if (chatterCd == 0 && getTarget() == null && o.distanceToSqr(this) < 256) {
                boolean night = level().isNight();
                say(o, night && random.nextInt(3) == 0 && Lines.has(kind, "night") ? "night" : "chatter", false);
                if (kind == ElfKind.TAVI) level().playSound(null, blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.8F, 1.5F);
                chatterCd = 1800 + random.nextInt(1800);
            }
            return;
        }
        if (isTalking() || isRecovering() || getTarget() != null || tickCount % 20 != 0) return;
        Player near = level().getNearestPlayer(this, 5);
        if (near == null || near.isSpectator()) return;
        Long last = greeted.get(near.getUUID());
        if (last != null && now - last < 6000) return;
        greeted.put(near.getUUID(), now);
        if (!kind.unique() && random.nextBoolean()) return;
        say(near, "approach", true);
        if (getActivity() == Activity.NONE || getActivity() == Activity.CROSS_ARMS) setActivity(Activity.WAVE, 40);
        getLookControl().setLookAt(near, 30F, 30F);
    }

    private void clientParticles() {
        if (kind == ElfKind.NIMRAE && random.nextInt(10) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.6), getY() + 0.2 + random.nextDouble() * 1.8, getRandomZ(0.6), 0, 0.01, 0);
        }
        if (getActivity() == Activity.MEDITATE && random.nextInt(4) == 0) {
            double a = tickCount * 0.15;
            level().addParticle(ParticleTypes.ENCHANT, getX() + Math.cos(a) * 0.9, getY() + 1.2, getZ() + Math.sin(a) * 0.9, 0, -0.2, 0);
        }
        if (getActivity() == Activity.READ && random.nextInt(20) == 0) {
            level().addParticle(ParticleTypes.ENCHANT, getX(), getY() + 1.6, getZ(), random.nextGaussian() * 0.3, 0.3, random.nextGaussian() * 0.3);
        }
        if (getActivity() == Activity.RECOVER && random.nextInt(25) == 0) {
            level().addParticle(ParticleTypes.CHERRY_LEAVES, getRandomX(0.8), getY() + 1.5, getRandomZ(0.8), 0, 0, 0);
        }
        if (kind == ElfKind.VAELOR && getTarget() == null && random.nextInt(30) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4), getY() + 0.1, getRandomZ(0.4), 0, 0.02, 0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CAST) castAnim = 14;
        else if (id == EVENT_LUNGE) lungeAnim = 10;
        else super.handleEntityEvent(id);
    }

    // ------------------------------------------------------------------ menzilli saldırılar

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ServerLevel sl = (ServerLevel) level();
        switch (kind) {
            case TAVI, THALION -> {
                AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), power);
                arrow.setBaseDamage(kind == ElfKind.THALION ? 4.5 : 3.0);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                if (kind == ElfKind.THALION && random.nextInt(3) == 0) arrow.setCritArrow(true);
                double dx = target.getX() - getX(), dy = target.getY(0.3333) - arrow.getY(), dz = target.getZ() - getZ();
                double h = Math.sqrt(dx * dx + dz * dz);
                arrow.shoot(dx, dy + h * 0.2, dz, kind == ElfKind.THALION ? 2.0F : 1.7F, kind == ElfKind.THALION ? 2F : 5F);
                playSound(SoundEvents.SKELETON_SHOOT, 1F, 1F / (getRandom().nextFloat() * 0.4F + 0.8F));
                level().addFreshEntity(arrow);
                if (kind == ElfKind.TAVI && random.nextInt(4) == 0) playSound(SoundEvents.PARROT_AMBIENT, 0.7F, 1.6F);
            }
            case CAELAN -> {
                // Rün oku: gözle görülür bir ışın ve büyü hasarı
                beam(sl, target, ParticleTypes.ENCHANTED_HIT);
                target.hurt(damageSources().indirectMagic(this, this), kind.damage + 3);
                level().broadcastEntityEvent(this, EVENT_CAST);
                playSound(SoundEvents.EVOKER_CAST_SPELL, 1F, 1.5F);
            }
            case NIMRAE -> {
                // Kök bağı: hedef yerine kenetlenir
                level().broadcastEntityEvent(this, EVENT_CAST);
                target.hurt(damageSources().indirectMagic(this, this), kind.damage + 1);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 3));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 0.3, target.getZ(), 30, 0.5, 0.3, 0.5, 0);
                sl.sendParticles(ParticleTypes.COMPOSTER, target.getX(), target.getY() + 0.5, target.getZ(), 20, 0.4, 0.6, 0.4, 0);
                playSound(SoundEvents.ROOTED_DIRT_PLACE, 1.4F, 0.7F);
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1F, 1.2F);
            }
            default -> {}
        }
    }

    private void beam(ServerLevel sl, LivingEntity target, ParticleOptions p) {
        Vec3 a = getEyePosition().add(getLookAngle().scale(0.5));
        Vec3 b = target.position().add(0, target.getBbHeight() * 0.55, 0);
        int n = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.lerp(b, i / (double) n);
            sl.sendParticles(p, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
        }
        sl.sendParticles(ParticleTypes.ENCHANT, b.x, b.y, b.z, 30, 0.3, 0.3, 0.3, 0.8);
    }

    // ------------------------------------------------------------------ kayıt

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        ownerOpt().ifPresent(u -> tag.putUUID("Owner", u));
        tag.putLong("HireUntil", hireUntil);
        tag.putLong("RecoverUntil", recoverUntil);
    }

    private Optional<UUID> ownerOpt() {
        return entityData.get(OWNER);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) setHome(NbtUtils.readBlockPos(tag.getCompound("Home")));
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
            clearRestriction();
        }
        hireUntil = tag.getLong("HireUntil");
        recoverUntil = tag.getLong("RecoverUntil");
    }

    // ------------------------------------------------------------------ iç hedefler

    /** Kiralayan oyuncuyu izler; çok uzaklaşırsa yanına ışınlanır. */
    private final class FollowOwnerGoal extends Goal {
        private int recalc;

        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player o = owner();
            if (o == null || o.isSpectator() || isTalking()) return false;
            double d = distanceToSqr(o);
            return getTarget() == null ? d > 25 : d > 256;
        }

        @Override
        public boolean canContinueToUse() {
            Player o = owner();
            return o != null && !getNavigation().isDone() && distanceToSqr(o) > 9 && (getTarget() == null || distanceToSqr(o) > 100);
        }

        @Override
        public void start() {
            recalc = 0;
            if (getActivity() != Activity.NONE && getActivity() != Activity.RECOVER) setActivity(Activity.NONE, 0);
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player o = owner();
            if (o == null) return;
            getLookControl().setLookAt(o, 10F, getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 10;
            if (distanceToSqr(o) > 24 * 24) teleportNear(o);
            else getNavigation().moveTo(o, distanceToSqr(o) > 100 ? 1.25 : 1.0);
        }

        private void teleportNear(Player o) {
            BlockPos base = o.blockPosition();
            for (int i = 0; i < 12; i++) {
                int x = base.getX() + random.nextInt(7) - 3, z = base.getZ() + random.nextInt(7) - 3, y = base.getY() + random.nextInt(3) - 1;
                if (Math.abs(x - base.getX()) < 2 && Math.abs(z - base.getZ()) < 2) continue;
                BlockPos p = new BlockPos(x, y, z);
                if (WalkNodeEvaluator.getBlockPathTypeStatic(level(), p.mutable()) != BlockPathTypes.WALKABLE) continue;
                if (!level().noCollision(ElfEntity.this, getBoundingBox().move(p.getCenter().subtract(position()).add(0, -0.5, 0)))) continue;
                moveTo(x + 0.5, y, z + 0.5, getYRot(), getXRot());
                getNavigation().stop();
                return;
            }
        }
    }

    /** Boşta: kılıç talimi, kitap okuma, meditasyon, diz çökme... */
    private final class ActivityGoal extends Goal {
        private Activity chosen = Activity.NONE;

        ActivityGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isRecovering()) return getActivity() == Activity.RECOVER;
            return !isHired() && !isTalking() && getTarget() == null && onGround() && getActivity() == Activity.NONE && random.nextInt(140) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            if (isRecovering()) return getActivity() == Activity.RECOVER;
            return getActivity() == chosen && chosen != Activity.NONE && getTarget() == null && !isHired() && !isTalking();
        }

        @Override
        public void start() {
            getNavigation().stop();
            if (isRecovering()) {
                chosen = Activity.RECOVER;
                return;
            }
            Activity[] set = Activity.idle(kind);
            chosen = set[random.nextInt(set.length)];
            if (level().isNight() && kind != ElfKind.GUARD && random.nextBoolean()) chosen = Activity.SIT;
            setActivity(chosen, 300 + random.nextInt(500));
            setItemSlot(EquipmentSlot.MAINHAND, idleItem());
        }

        @Override
        public void stop() {
            if (getActivity() == chosen && chosen != Activity.RECOVER) setActivity(Activity.NONE, 0);
            chosen = Activity.NONE;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            if (chosen == Activity.PRACTICE && random.nextInt(14) == 0) {
                swing(InteractionHand.MAIN_HAND);
                if (!kind.ranged()) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.3F, 1.2F + random.nextFloat() * 0.3F);
                setYRot(getYRot() + (random.nextFloat() - 0.5F) * 50F);
                yBodyRot = getYRot();
                yHeadRot = getYRot();
            }
            if (chosen == Activity.KNEEL && kind == ElfKind.TAVI && random.nextInt(80) == 0) playSound(SoundEvents.PARROT_AMBIENT, 0.6F, 1.6F);
        }
    }

    /** Sahibine saldıranlara ve sahibinin saldırdıklarına saldırır (oyuncular hariç). */
    private final class DefendOwnerGoal extends TargetGoal {
        private int lastHurtBy, lastHurt;
        private LivingEntity pick;

        DefendOwnerGoal() {
            super(ElfEntity.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player o = owner();
            if (o == null || isRecovering()) return false;
            LivingEntity a = o.getLastHurtByMob();
            if (a != null && o.getLastHurtByMobTimestamp() != lastHurtBy && ok(a)) {
                pick = a;
                return true;
            }
            LivingEntity b = o.getLastHurtMob();
            if (b != null && o.getLastHurtMobTimestamp() != lastHurt && ok(b)) {
                pick = b;
                return true;
            }
            return false;
        }

        private boolean ok(LivingEntity e) {
            return !(e instanceof Player) && !(e instanceof ElfEntity) && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
                    && !(e instanceof net.minecraft.world.entity.npc.AbstractVillager) && canAttack(e, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            Player o = owner();
            if (o != null) {
                lastHurtBy = o.getLastHurtByMobTimestamp();
                lastHurt = o.getLastHurtMobTimestamp();
            }
            mob.setTarget(pick);
            if (o != null && random.nextInt(3) == 0) say(o, "combat", true);
            super.start();
        }
    }

    public static float lerpRot(float a, float b, float t) {
        return a + Mth.wrapDegrees(b - a) * t;
    }
}
