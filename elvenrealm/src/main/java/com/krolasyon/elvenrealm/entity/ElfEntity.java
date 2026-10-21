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
import java.util.List;
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
    /** Kentteki günlük program: kent merkezi (x, aşağı avlu zemini, z) ve görev yeri. */
    @Nullable private BlockPos city;
    private String post = "";
    private boolean cityChecked;
    /** Şu anki görevin adı (boş: programda değil). */
    public String routineKey = "";
    @Nullable private UUID lordId;
    private int lordScan, bowCd;
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
        goalSelector.addGoal(4, new RoutineGoal());
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !hasRoutine() && !isHired() && !isTalking() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new ActivityGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55) {
            @Override
            public boolean canUse() {
                return !hasRoutine() && !isHired() && !isTalking() && getActivity() == Activity.NONE && super.canUse();
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
        if (city != null) return city.distSqr(e.blockPosition()) < 110 * 110;
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
        if (!isHired()) restrictHome();
    }

    private void restrictHome() {
        if (city != null) restrictTo(city, 130);
        else if (home != null) restrictTo(home, 14);
    }

    /** Elfi bir Sylvaren kentinin günlük programına bağlar. */
    public void setCity(BlockPos center, String post) {
        this.city = center.immutable();
        this.post = post == null ? "" : post;
        this.cityChecked = true;
        if (!isHired()) restrictHome();
    }

    public boolean hasRoutine() {
        return city != null;
    }

    public String post() {
        return post;
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
        restrictHome();
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
        return Component.literal(kind.name).withStyle(kind == ElfKind.AERENDIL ? ChatFormatting.YELLOW : kind == ElfKind.VAELOR ? ChatFormatting.DARK_PURPLE
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
            case AERENDIL -> SoundEvents.NOTE_BLOCK_GUITAR.value();
            case TAVI -> SoundEvents.NOTE_BLOCK_BELL.value();
            default -> SoundEvents.NOTE_BLOCK_FLUTE.value();
        };
    }

    private ItemStack idleItem() {
        if (getActivity() == Activity.READ) return new ItemStack(Items.BOOK);
        if (getActivity() == Activity.PRACTICE || getTarget() != null) return weapon();
        if (kind == ElfKind.AERENDIL) return getActivity() == Activity.SIT || getActivity() == Activity.PRAY ? ItemStack.EMPTY : new ItemStack(Items.BLAZE_ROD);
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
            case AERENDIL -> new ItemStack(Items.BLAZE_ROD);
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
        if (bowCd > 0) bowCd--;
        setAggressive(getTarget() != null);
        if (city == null && !cityChecked && tickCount > 40 && !isHired()) inferCity();

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
        // lord geçerken muhafızlar selam durur, diğerleri eğilir
        if (kind != ElfKind.AERENDIL && bowCd == 0 && hasRoutine() && !getActivity().seated()) {
            ElfEntity lord = lord();
            if (lord != null && lord.distanceToSqr(this) < 49 && !lord.getNavigation().isDone()) {
                setActivity(kind == ElfKind.GUARD ? Activity.SALUTE : Activity.BOW, 45);
                getLookControl().setLookAt(lord, 30F, 30F);
                bowCd = 900;
                Player p = level().getNearestPlayer(this, 12);
                if (p != null && kind == ElfKind.GUARD && random.nextInt(3) == 0) say(p, "lord_pass", true);
                return;
            }
        }
        Player hear = level().getNearestPlayer(this, 8);
        if (hear != null && !hear.isSpectator() && chatterCd == 0 && !routineKey.isEmpty() && Lines.has(kind, "duty_" + routineKey)
                && random.nextInt(5) == 0) {
            say(hear, "duty_" + routineKey, !kind.unique());
            chatterCd = 2400 + random.nextInt(2400);
            return;
        }
        Player near = level().getNearestPlayer(this, 5);
        if (near == null || near.isSpectator()) return;
        Long last = greeted.get(near.getUUID());
        if (last != null && now - last < 6000) return;
        greeted.put(near.getUUID(), now);
        if (!kind.unique() && random.nextBoolean()) return;
        say(near, "approach", true);
        if (kind == ElfKind.GUARD && getActivity() == Activity.STAND) setActivity(Activity.SALUTE, 40);
        else if (kind == ElfKind.AERENDIL) {
            if (!getActivity().seated()) setActivity(Activity.WAVE, 30);
        } else if (getActivity() == Activity.NONE || getActivity() == Activity.CROSS_ARMS || getActivity() == Activity.STAND) setActivity(Activity.WAVE, 40);
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
            case AERENDIL -> {
                // Yıldız ışını: Kadim Ağaçların ışığı
                beam(sl, target, ParticleTypes.END_ROD);
                target.hurt(damageSources().indirectMagic(this, this), kind.damage + 4);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                level().broadcastEntityEvent(this, EVENT_CAST);
                playSound(SoundEvents.BEACON_POWER_SELECT, 1F, 1.6F);
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
        if (city != null) tag.put("City", NbtUtils.writeBlockPos(city));
        tag.putString("Post", post);
        tag.putBoolean("CityChecked", cityChecked);
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
        cityChecked = tag.getBoolean("CityChecked");
        if (tag.contains("City")) setCity(NbtUtils.readBlockPos(tag.getCompound("City")), tag.getString("Post"));
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
            return !hasRoutine() && !isHired() && !isTalking() && getTarget() == null && onGround() && getActivity() == Activity.NONE && random.nextInt(140) == 0;
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

    /** Eski kentlerdeki (programsız) sakinler: evleri bir kentin içindeyse programa bağlanır. */
    private void inferCity() {
        cityChecked = true;
        if (home == null || !(level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        var gen = sl.getChunkSource().getGenerator();
        var rs = sl.getChunkSource().randomState();
        com.krolasyon.elvenrealm.world.CityLayout.City c = com.krolasyon.elvenrealm.world.CityLayout.nearest(sl.getSeed(), gen, rs, sl,
                home.getX(), home.getZ(), 1);
        if (c == null || !com.krolasyon.elvenrealm.world.CityLayout.inside(c, home.getX(), home.getZ())) return;
        BlockPos center = new BlockPos(c.cx(), com.krolasyon.elvenrealm.world.CityLayout.base(c, gen, rs, sl), c.cz());
        setCity(center, kind.unique() ? kind.id : Routine.nearestGuardPost(center, home));
    }

    /** Aynı kentin lordu (önbellekli). */
    @Nullable
    ElfEntity lord() {
        if (lordId != null && level() instanceof ServerLevel sl && sl.getEntity(lordId) instanceof ElfEntity l && l.isAlive()
                && l.distanceToSqr(this) < 96 * 96) return l;
        if (--lordScan > 0) return null;
        lordScan = 100;
        lordId = null;
        List<ElfEntity> list = level().getEntitiesOfClass(ElfEntity.class, getBoundingBox().inflate(80), x -> x.kind == ElfKind.AERENDIL);
        if (list.isEmpty()) return null;
        lordId = list.get(0).getUUID();
        return list.get(0);
    }

    /** Kentte günlük program: görev yerine (merdivenlerden geçerek) yürür, orada işini yapar; devriyeler yollarında gidip gelir. */
    private final class RoutineGoal extends Goal {
        @Nullable private Routine.Task task;
        private List<Vec3> hops = List.of();
        private int hop, recheck, repath, stuck, pause, idx, dir = 1, microCd, glance;
        private Vec3 lastPos = Vec3.ZERO;

        RoutineGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean free() {
            return city != null && !isHired() && !isTalking() && getTarget() == null && !isRecovering() && !isPassenger();
        }

        @Override
        public boolean canUse() {
            return free();
        }

        @Override
        public boolean canContinueToUse() {
            return free();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            task = null;
            recheck = 0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
            if (!getActivity().gesture() && getActivity() != Activity.RECOVER) setActivity(Activity.NONE, 0);
            task = null;
            routineKey = "";
        }

        @Override
        public void tick() {
            if (--recheck <= 0) {
                Routine.Task t = Routine.of(ElfEntity.this, city, post, level().getDayTime());
                if (t.escort()) t = escort(t);
                recheck = t.escort() ? 10 : 60;
                if (task == null || !t.key().equals(task.key()) || (t.escort() && t.pos().distanceToSqr(task.pos()) > 1.0)) begin(t);
            }
            if (task == null) return;
            if (task.patrol() != null) patrolTick();
            else if (travel(task.pos(), task.speed())) arrive(task.yaw(), task.look(), task.act(), task.pos());
        }

        private Routine.Task escort(Routine.Task t) {
            ElfEntity l = lord();
            if (l == null || l.routineKey.isEmpty() || l.routineKey.equals("throne")) return t;
            float side = post.endsWith("_l") ? -1.7F : 1.7F;
            Vec3 fw = Vec3.directionFromRotation(0, l.getYRot());
            Vec3 right = new Vec3(-fw.z, 0, fw.x);
            Vec3 p = l.position().subtract(fw.scale(2.3)).add(right.scale(side));
            return new Routine.Task("escort", p, l.getYRot(), null, Activity.STAND, 0.62, null, false, true);
        }

        private void begin(Routine.Task t) {
            boolean sameRoute = task != null && t.key().equals(task.key());
            task = t;
            routineKey = t.key();
            if (!sameRoute) {
                idx = 0;
                dir = 1;
                pause = 0;
            }
            Vec3 first = t.patrol() != null ? t.patrol().get(idx) : t.pos();
            hops = Routine.hops(city, position(), first);
            hop = 0;
            stuck = 0;
            repath = 0;
        }

        private double horiz(Vec3 p) {
            double dx = p.x - getX(), dz = p.z - getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** Ara duraklar üzerinden yürür; true = son noktaya varıldı. */
        private boolean travel(Vec3 dest, double speed) {
            if (hops.isEmpty()) hops = List.of(dest);
            while (hop < hops.size() - 1 && horiz(hops.get(hop)) < 2.0) hop++;
            Vec3 h = hops.get(Math.min(hop, hops.size() - 1));
            boolean last = hop >= hops.size() - 1;
            if (last && horiz(dest) < (task != null && task.snap() ? 0.7 : 1.3) && Math.abs(dest.y - getY()) < 2.5) return true;
            if (getActivity() != Activity.NONE && !getActivity().gesture()) setActivity(Activity.NONE, 0);
            if (--repath <= 0 || getNavigation().isDone()) {
                getNavigation().moveTo(h.x, h.y, h.z, speed);
                repath = 30;
            }
            if (tickCount % 20 == 0) {
                stuck = position().distanceToSqr(lastPos) < 0.09 ? stuck + 20 : 0;
                lastPos = position();
                boolean watched = level().getNearestPlayer(ElfEntity.this, 24) != null;
                if ((stuck >= 200 && !watched) || stuck >= 500) {
                    teleportTo(h.x, h.y, h.z);
                    getNavigation().stop();
                    stuck = 0;
                }
            }
            return false;
        }

        private void arrive(float yaw, @Nullable Vec3 look, Activity act, Vec3 dest) {
            getNavigation().stop();
            if (task.snap() && horiz(dest) > 0.08) setPos(dest.x, Math.max(getY(), dest.y), dest.z);
            float want = look != null ? (float) (Mth.atan2(look.z - getZ(), look.x - getX()) * (180F / Math.PI)) - 90F : yaw;
            setYRot(Mth.approachDegrees(getYRot(), want, 12F));
            yBodyRot = getYRot();
            Player near = level().getNearestPlayer(ElfEntity.this, 6);
            boolean busy = act.seated() || act == Activity.PRAY || act == Activity.READ || act == Activity.PRACTICE || act == Activity.KNEEL;
            if (near != null && !near.isSpectator() && !busy) getLookControl().setLookAt(near, 20F, 20F);
            else if (look != null) getLookControl().setLookAt(look.x, look.y + 1.0, look.z);
            else glance(want);
            if (!getActivity().gesture()) {
                if (getActivity() != act) {
                    setActivity(act, 400);
                    setItemSlot(EquipmentSlot.MAINHAND, idleItem());
                } else if (activityTimer < 40) activityTimer = 400;
            }
            micro(act);
        }

        /** Nöbetçiler ara sıra sağa sola bakar. */
        private void glance(float yaw) {
            if (--glance <= 0) glance = 60 + random.nextInt(140);
            float off = glance > 40 ? 0F : (glance > 20 ? 50F : -50F) * (getId() % 2 == 0 ? 1 : -1);
            Vec3 d = Vec3.directionFromRotation(0, yaw + off).scale(6);
            getLookControl().setLookAt(getX() + d.x, getEyeY(), getZ() + d.z);
        }

        private void micro(Activity act) {
            if (microCd > 0) microCd--;
            if (act == Activity.PRACTICE && !kind.ranged() && random.nextInt(14) == 0) {
                swing(InteractionHand.MAIN_HAND);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.3F, 1.2F + random.nextFloat() * 0.3F);
            }
            if (act == Activity.PRACTICE && kind == ElfKind.TAVI && "archery".equals(routineKey) && microCd == 0) {
                // nişan tahtalarına ok atar
                int k = random.nextInt(4);
                Vec3 tgt = Routine.at(city, 15 + k * 6, Routine.B, -49).add(0, 1.5, 0);
                getLookControl().setLookAt(tgt.x, tgt.y, tgt.z);
                net.minecraft.world.entity.projectile.Arrow a = new net.minecraft.world.entity.projectile.Arrow(level(), ElfEntity.this);
                a.pickup = AbstractArrow.Pickup.DISALLOWED;
                double dx = tgt.x - getX(), dy = tgt.y - a.getY(), dz = tgt.z - getZ();
                a.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.06, dz, 1.9F, 3F);
                level().addFreshEntity(a);
                playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.1F);
                microCd = 50 + random.nextInt(40);
            }
            if (act == Activity.KNEEL && kind == ElfKind.TAVI && random.nextInt(80) == 0) playSound(SoundEvents.PARROT_AMBIENT, 0.6F, 1.6F);
        }

        private void patrolTick() {
            List<Vec3> pts = task.patrol();
            Vec3 target = pts.get(idx);
            if (pause > 0) {
                getNavigation().stop();
                pause--;
                float yaw = routineKey.equals("wall") ? 0F : getYRot();
                Activity act = routineKey.equals("market") ? Activity.TALK : routineKey.equals("prowl") ? Activity.CROSS_ARMS : Activity.STAND;
                if (!getActivity().gesture() || getActivity() == Activity.TALK) {
                    if (getActivity() != act) setActivity(act, pause + 5);
                }
                setYRot(Mth.approachDegrees(getYRot(), yaw, 10F));
                yBodyRot = getYRot();
                glance(yaw);
                if (pause == 0) {
                    if (pts.size() == 2) {
                        if (idx + dir < 0 || idx + dir >= pts.size()) dir = -dir;
                        idx += dir;
                    } else idx = (idx + 1) % pts.size();
                    hops = Routine.hops(city, position(), pts.get(idx));
                    hop = 0;
                    if (getActivity() == act) setActivity(Activity.NONE, 0);
                }
                return;
            }
            if (travel(target, task.speed())) pause = 40 + random.nextInt(90);
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
