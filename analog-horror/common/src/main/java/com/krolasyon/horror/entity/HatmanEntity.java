package com.krolasyon.horror.entity;

import com.krolasyon.horror.compat.CompatMonster;
import com.krolasyon.horror.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The Hatman. A stalker driven by a small state machine:
 * <ul>
 *   <li>STALK - keeps out of sight behind cover, creeps closer every time the player looks away, plays sound tricks,
 *   pinches out torches, peeks around corners.</li>
 *   <li>FROZEN - while watched it locks into the photograph pose; stare too long and it either vanishes, tips its hat,
 *   steps closer when you blink, or screams and attacks.</li>
 *   <li>HUNT - sprints, leaps, claws, grabs-and-slams; crawls through low gaps; loses you only after it lost sight
 *   for a while, then SEARCHes your last known position.</li>
 * </ul>
 * Aggression builds over time (faster at night, in darkness, when stared at or hurt) and decides when stalking turns into a hunt.
 */
public class HatmanEntity extends CompatMonster {
    public static final int ROAM = 0, STALK = 1, FROZEN = 2, PEEK = 3, HUNT = 4, SEARCH = 5, EMERGE = 6;
    public static final int FLAG_CRAWL = 1;
    public static final String[] ACTIONS = {"attack_r", "attack_l", "grab", "scream", "twitch", "hat_tip", "look", "extinguish", "leap"};

    /** client hooks, installed by ClientSetup (keeps client classes off the dedicated server) */
    public static Consumer<HatmanEntity> CLIENT_TICK = e -> {};
    public static BiConsumer<HatmanEntity, Byte> CLIENT_EVENT = (e, b) -> {};
    public static final byte EV_STATIC = 70, EV_JUMPSCARE = 71;

    private static final EntityDimensions STANDING = EntityDimensions.scalable(0.8F, 3.2F);
    private static final EntityDimensions CRAWLING = EntityDimensions.scalable(0.9F, 1.4F);

    private boolean initialized;
    private Player target;
    private Vec3 lastSeen;
    private int lastSeenTick = -10000;
    private float aggression = 15F;
    private int stateTicks, observedTicks, unobservedTicks, frozenLimit = 90;
    private int actionLock, attackCooldown, leapCooldown, trickCooldown = 300, extinguishCooldown, twitchCooldown = 200;
    private int pendingHit = -1, pendingHitType, slamTimer = -1, leapTimer = -1;
    private Player grabbed;
    private BlockPos pendingLight;
    private int pendingLightTimer = -1;
    private Vec3 hideSpot;
    private int spotTimer, pathFails, standClear, noPlayerTicks, stuckTicks;
    private boolean leftArm, relocatedLowHealth;
    private Vec3 lastPos = Vec3.ZERO;
    public int clientJumpscareCooldown;

    public HatmanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 140.0)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 72.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
    }

    // ------------------------------------------------------------------ compat hooks
    @Override
    protected EntityDimensions sizeFor(Pose pose) {
        return isCrawling() ? CRAWLING : STANDING;
    }

    @Override
    protected String[] actionNames() { return ACTIONS; }

    @Override
    public String loopAnimation(boolean moving) {
        if (this.isDeadOrDying()) return "death";
        int s = getState();
        if (s == EMERGE) return "emerge";
        if (isCrawling()) return moving ? "crawl" : "crawl_idle";
        return switch (s) {
            case FROZEN -> "frozen";
            case PEEK -> "peek";
            case HUNT -> moving ? "run" : "idle";
            case STALK -> moving ? "stalk" : "idle";
            default -> moving ? "walk" : "idle";
        };
    }

    public boolean isCrawling() { return (getFlags() & FLAG_CRAWL) != 0; }

    private void setCrawling(boolean c) {
        if (c == isCrawling()) return;
        setFlags(c ? getFlags() | FLAG_CRAWL : getFlags() & ~FLAG_CRAWL);
        this.refreshDimensions();
        this.getNavigation().stop();
    }

    public float getAggression() { return aggression; }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            CLIENT_TICK.accept(this);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EV_STATIC || id == EV_JUMPSCARE) CLIENT_EVENT.accept(this, id);
        else super.handleEntityEvent(id);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!initialized) {
            initialized = true;
            setState(EMERGE);
            this.playSound(ModSounds.EMERGE.get(), 2.0F, 1.0F);
        }
        stateTicks++;
        if (actionLock > 0) actionLock--;
        if (attackCooldown > 0) attackCooldown--;
        if (leapCooldown > 0) leapCooldown--;
        if (trickCooldown > 0) trickCooldown--;
        if (extinguishCooldown > 0) extinguishCooldown--;
        if (twitchCooldown > 0) twitchCooldown--;
        tickTimers();

        if (getState() == EMERGE) {
            this.getNavigation().stop();
            if (stateTicks >= 60) setState(STALK);
            return;
        }
        if (this.tickCount % 10 == 0) manageCrawl();

        Player p = findTarget();
        if (p == null) {
            if (++noPlayerTicks > 1200) this.discard();
            roam();
            return;
        }
        noPlayerTicks = 0;
        double dist = this.distanceTo(p);
        boolean see = this.hasLineOfSight(p);
        if (see) {
            lastSeen = p.position();
            lastSeenTick = tickCount;
        } else if (p.isSprinting() && dist < 24 || dist < 6) {     // it hears you
            lastSeen = p.position();
            lastSeenTick = tickCount;
        }
        boolean observed = isObservedBy(p);
        updateAggression(p, observed, dist);

        // creatures of the dark: it will not linger in open daylight
        if (getState() != HUNT && level().isDay() && level().canSeeSky(this.blockPosition()) && !observed && stateTicks > 40) {
            vanish(true);
            return;
        }

        switch (getState()) {
            case STALK -> stalk(p, dist, see, observed);
            case FROZEN -> frozen(p, dist, see, observed);
            case PEEK -> peek(p, dist, observed);
            case HUNT -> hunt(p, dist, see);
            case SEARCH -> search(p, dist, see);
            default -> setState(STALK);
        }
        lastPos = this.position();
    }

    private void tickTimers() {
        if (pendingHit >= 0 && --pendingHit < 0) resolveHit();
        if (slamTimer >= 0 && --slamTimer < 0) resolveSlam();
        if (leapTimer >= 0 && --leapTimer < 0 && target != null) {
            Vec3 d = target.position().subtract(this.position());
            Vec3 h = new Vec3(d.x, 0, d.z).normalize();
            this.setDeltaMovement(h.x * 1.25, 0.48, h.z * 1.25);
            this.hasImpulse = true;
        }
        if (pendingLightTimer >= 0 && --pendingLightTimer < 0 && pendingLight != null) {
            BlockState bs = level().getBlockState(pendingLight);
            if (isLight(bs)) {
                level().destroyBlock(pendingLight, true);
                level().playSound(null, pendingLight, ModSounds.EXTINGUISH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                ((ServerLevel) level()).sendParticles(ParticleTypes.SMOKE, pendingLight.getX() + 0.5, pendingLight.getY() + 0.6, pendingLight.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.01);
            }
            pendingLight = null;
        }
    }

    private void setState(int s) {
        if (getState() != s) {
            super.setSyncedState(s);
            stateTicks = 0;
            observedTicks = 0;
            unobservedTicks = 0;
        }
    }

    public int getState() { return getSyncedState(); }

    private Player findTarget() {
        if (target != null && target.isAlive() && !target.isSpectator() && !target.isCreative() && target.level() == level()
                && this.distanceToSqr(target) < 96 * 96) return target;
        target = null;
        double best = 80 * 80;
        for (Player pl : level().players()) {
            if (pl.isSpectator() || pl.isCreative() || !pl.isAlive()) continue;
            double d = this.distanceToSqr(pl);
            if (d < best) { best = d; target = pl; }
        }
        return target;
    }

    private void updateAggression(Player p, boolean observed, double dist) {
        float a = 0.025F;
        if (level().isNight()) a += 0.03F;
        if (level().getMaxLocalRawBrightness(p.blockPosition()) <= 4) a += 0.06F;
        if (level().isDay() && level().canSeeSky(p.blockPosition())) a -= 0.06F;
        if (dist < 10) a += 0.05F;
        aggression = Mth.clamp(aggression + a, 0F, 100F);
    }

    /** true when the player is actually looking at it (cone + clear line of sight + it is not hidden in pitch darkness) */
    public boolean isObservedBy(Player p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1F);
        double dist = this.distanceTo(p);
        if (dist > 14 && level().getMaxLocalRawBrightness(this.blockPosition().above()) <= 1
                && !p.hasEffect(MobEffects.NIGHT_VISION)) return false;
        for (double h : new double[]{0.25, 0.55, 0.85}) {
            Vec3 pt = this.position().add(0, this.getBbHeight() * h, 0);
            Vec3 to = pt.subtract(eye);
            double len = to.length();
            if (len < 1e-3) return true;
            if (look.dot(to.scale(1 / len)) < 0.64) continue;
            if (level().clip(new ClipContext(eye, pt, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS)
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ states
    private void roam() {
        if (getState() != STALK) setState(STALK);
        if (this.getNavigation().isDone() && random.nextInt(120) == 0) {
            Vec3 v = this.position().add(random.nextInt(21) - 10, 0, random.nextInt(21) - 10);
            this.getNavigation().moveTo(v.x, v.y, v.z, 0.55);
        }
    }

    private void stalk(Player p, double dist, boolean see, boolean observed) {
        if (observed && dist < 56) {
            this.getNavigation().stop();
            frozenLimit = 60 + random.nextInt(80);
            setState(FROZEN);
            return;
        }
        if (aggression >= 100F || dist < 4.5) {
            startHunt(p, dist < 4.5);
            return;
        }
        float want = Mth.lerp(aggression / 100F, 26F, 8F);
        if (--spotTimer <= 0 || hideSpot == null || this.getNavigation().isDone() && this.position().distanceTo(hideSpot) > 2.5) {
            spotTimer = 40 + random.nextInt(30);
            Vec3 s = findHideSpot(p, want, 4F);
            if (s != null) {
                hideSpot = s;
                if (!this.getNavigation().moveTo(s.x, s.y, s.z, isCrawling() ? 0.7 : 0.62)) pathFails++;
                else pathFails = 0;
            } else if (dist > want + 6) {
                if (!this.getNavigation().moveTo(p, 0.6)) pathFails++;
            }
        }
        if (this.getNavigation().isDone()) {
            this.getLookControl().setLookAt(p, 20F, 20F);
            if (dist < 20 && !see && stateTicks > 60 && random.nextInt(80) == 0) {
                setState(PEEK);
                return;
            }
            if (twitchCooldown == 0 && random.nextInt(200) == 0) action("twitch", 20, ModSounds.TWITCH.get());
            else if (random.nextInt(260) == 0) action("look", 30, null);
        }
        tryExtinguish(observed);
        trySoundTrick(p, dist, observed);
    }

    private void frozen(Player p, double dist, boolean see, boolean observed) {
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        if (observed) {
            observedTicks++;
            unobservedTicks = 0;
            aggression = Math.min(100F, aggression + 0.3F);
        } else if (++unobservedTicks > 4) {
            // you blinked - it is closer now
            if (aggression > 55 && random.nextFloat() < 0.45F) {
                Vec3 s = findHideSpot(p, (float) Math.max(5, dist * 0.55), 3F);
                if (s != null) this.randomTeleport(s.x, s.y, s.z, false);
            }
            setState(STALK);
            return;
        }
        if (dist < 6 && observedTicks > 15) {
            startHunt(p, true);
            return;
        }
        if (observedTicks > frozenLimit) {
            if (aggression >= 60) {
                startHunt(p, true);
            } else if (dist < 22 && random.nextFloat() < 0.4F) {
                action("hat_tip", 40, ModSounds.HAT_TIP.get());
                frozenLimit = observedTicks + 45;
                aggression += 10;
            } else {
                vanish(false);
                setState(STALK);
            }
        }
    }

    private void peek(Player p, double dist, boolean observed) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(p, 30F, 30F);
        if (observed) {
            observedTicks++;
            if (observedTicks > 10) {
                // caught peeking: slip back behind cover
                spotTimer = 0;
                setState(STALK);
            }
        } else if (stateTicks > 70 + random.nextInt(60)) {
            setState(STALK);
        }
    }

    private void startHunt(Player p, boolean scream) {
        target = p;
        setState(HUNT);
        aggression = 100F;
        if (scream) {
            action("scream", 44, ModSounds.SCREAM.get());
            if (this.distanceTo(p) < 12) this.level().broadcastEntityEvent(this, EV_JUMPSCARE);
        }
    }

    private void hunt(Player p, double dist, boolean see) {
        if (actionLock > 0) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(p, 40F, 40F);
            return;
        }
        if (dist > 72 || !see && tickCount - lastSeenTick > 200) {
            setState(SEARCH);
            return;
        }
        this.getLookControl().setLookAt(p, 40F, 40F);
        double speed = isCrawling() ? 1.15 : 1.45;
        if (tickCount % 5 == 0) {
            boolean ok = see ? this.getNavigation().moveTo(p, speed)
                    : lastSeen != null && this.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, speed);
            Path path = this.getNavigation().getPath();
            if (!ok || path != null && !path.canReach()) pathFails++;
            else pathFails = 0;
        }
        // stuck behind something it cannot path through: phase to a hidden spot near the player
        if (this.position().distanceToSqr(lastPos) < 0.0004 && dist > 4) stuckTicks++;
        else stuckTicks = 0;
        if (stuckTicks > 60 || pathFails > 6 && !isCrawling() && dist < 30) {
            Vec3 s = findHideSpot(p, 6F, 3F);
            if (s != null) {
                vanish(false);
                this.randomTeleport(s.x, s.y, s.z, false);
            }
            stuckTicks = 0;
            pathFails = 0;
        }
        if (this.onGround() && see && dist > 5 && dist < 11 && leapCooldown == 0 && random.nextInt(12) == 0) {
            leapCooldown = 90;
            action("leap", 14, ModSounds.LEAP.get());
            leapTimer = 5;
            return;
        }
        double reach = 3.4 + p.getBbWidth() / 2;
        if (see && dist < reach && attackCooldown == 0) {
            if (random.nextFloat() < 0.22F && grabbed == null) {
                action("grab", 30, ModSounds.GRAB.get());
                pendingHit = 11;
                pendingHitType = 2;
                attackCooldown = 50;
            } else {
                leftArm = !leftArm;
                action(leftArm ? "attack_l" : "attack_r", 10, ModSounds.SWIPE.get());
                pendingHit = 7;
                pendingHitType = 1;
                attackCooldown = 18;
            }
        }
    }

    private void search(Player p, double dist, boolean see) {
        if (see && dist < 40) {
            if (aggression > 50) startHunt(p, false);
            else setState(STALK);
            return;
        }
        if (lastSeen != null && this.position().distanceTo(lastSeen) > 2.5) {
            if (tickCount % 10 == 0) this.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, 0.9);
        } else if (stateTicks % 70 == 10) {
            action("look", 30, ModSounds.AMBIENT.get());
        }
        if (stateTicks > 320) {
            aggression = 45F;
            setState(STALK);
        }
    }

    // ------------------------------------------------------------------ combat
    private void resolveHit() {
        Player p = target;
        if (p == null || !p.isAlive() || this.distanceTo(p) > 4.6 + p.getBbWidth()) return;
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (pendingHitType == 2) {
            if (p.hurt(this.damageSources().mobAttack(this), dmg * 0.5F)) {
                grabbed = p;
                Vec3 up = this.position().add(this.getLookAngle().scale(1.2)).subtract(p.position());
                p.setDeltaMovement(up.x * 0.25, 0.95, up.z * 0.25);
                p.hurtMarked = true;
                slamTimer = 14;
                this.level().broadcastEntityEvent(this, EV_JUMPSCARE);
            }
        } else if (p.hurt(this.damageSources().mobAttack(this), dmg)) {
            this.playSound(ModSounds.HIT.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
            Vec3 kb = p.position().subtract(this.position()).normalize();
            p.setDeltaMovement(p.getDeltaMovement().add(kb.x * 0.5, 0.15, kb.z * 0.5));
            p.hurtMarked = true;
            aggression -= 6F;
        }
        // hit and run: once it has drawn enough blood it melts back into the dark
        if (p.getHealth() < p.getMaxHealth() * 0.35F && random.nextFloat() < 0.4F) {
            aggression = 20F;
            vanish(false);
            Vec3 s = findHideSpot(p, 30F, 6F);
            if (s != null) this.randomTeleport(s.x, s.y, s.z, false);
            setState(STALK);
        }
    }

    private void resolveSlam() {
        Player p = grabbed;
        grabbed = null;
        if (p == null || !p.isAlive() || this.distanceTo(p) > 7) return;
        p.setDeltaMovement(0, -1.6, 0);
        p.hurtMarked = true;
        p.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3F);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        this.playSound(ModSounds.HIT.get(), 1.4F, 0.6F);
    }

    private void action(String name, int lock, SoundEvent sound) {
        playAction(name);
        actionLock = Math.max(actionLock, lock);
        if (sound != null) this.playSound(sound, 1.6F, 0.95F + random.nextFloat() * 0.1F);
        if (name.equals("twitch")) twitchCooldown = 300 + random.nextInt(400);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (getState() == EMERGE) amount *= 0.4F;
        boolean r = super.hurt(src, amount);
        if (r && !level().isClientSide() && this.isAlive()) {
            if (src.getEntity() instanceof Player pl && !pl.isCreative() && !pl.isSpectator()) {
                lastSeen = pl.position();
                lastSeenTick = tickCount;
                if (getState() != HUNT) startHunt(pl, true);
            }
            if (!relocatedLowHealth && this.getHealth() < this.getMaxHealth() * 0.3F) {
                // retreats into the dark to heal once
                relocatedLowHealth = true;
                Player p = target;
                vanish(false);
                if (p != null) {
                    Vec3 s = findHideSpot(p, 34F, 8F);
                    if (s != null) this.randomTeleport(s.x, s.y, s.z, false);
                }
                this.heal(this.getMaxHealth() * 0.3F);
                aggression = 30F;
                setState(STALK);
            }
        }
        return r;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource src) { return false; }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 52 && !this.level().isClientSide() && !this.isRemoved()) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 30, 0.6, 0.4, 0.6, 0.02);
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------ tricks
    private void vanish(boolean despawn) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.6, getZ(), 40, 0.4, 1.2, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.6, getZ(), 20, 0.3, 1.0, 0.3, 0.02);
        }
        this.playSound(ModSounds.VANISH.get(), 1.4F, 1.0F);
        this.level().broadcastEntityEvent(this, EV_STATIC);
        this.getNavigation().stop();
        if (despawn) this.discard();
    }

    private void trySoundTrick(Player p, double dist, boolean observed) {
        if (trickCooldown > 0 || observed || dist > 48) return;
        trickCooldown = 400 + random.nextInt(700);
        Vec3 behind = p.position().subtract(p.getViewVector(1F).multiply(1, 0, 1).normalize().scale(3 + random.nextInt(4)));
        SoundEvent s = switch (random.nextInt(4)) {
            case 0 -> ModSounds.KNOCK.get();
            case 1 -> ModSounds.FAKE_STEP.get();
            case 2 -> ModSounds.WHISPER.get();
            default -> ModSounds.DISTANT_CALL.get();
        };
        level().playSound(null, behind.x, behind.y + 1, behind.z, s, SoundSource.HOSTILE, 0.9F, 0.95F + random.nextFloat() * 0.1F);
    }

    private static boolean isLight(BlockState s) {
        return s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH)
                || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN) || s.is(BlockTags.CANDLES);
    }

    private void tryExtinguish(boolean observed) {
        if (observed || extinguishCooldown > 0 || pendingLight != null || this.tickCount % 20 != 0) return;
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        BlockPos c = this.blockPosition();
        for (BlockPos bp : BlockPos.betweenClosed(c.offset(-3, 0, -3), c.offset(3, 4, 3))) {
            if (isLight(level().getBlockState(bp))) {
                pendingLight = bp.immutable();
                pendingLightTimer = 13;
                extinguishCooldown = 60;
                this.getNavigation().stop();
                this.getLookControl().setLookAt(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
                action("extinguish", 26, null);
                return;
            }
        }
    }

    /** a reachable standing spot about {@code want} blocks from the player that the player cannot see, preferring behind them and darkness */
    private Vec3 findHideSpot(Player p, float want, float spread) {
        Vec3 look = p.getViewVector(1F).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
        look = look.normalize();
        Vec3 best = null;
        double bestScore = -1e9;
        EntityDimensions dim = this.getDimensions(this.getPose());
        for (int i = 0; i < 14; i++) {
            double ang = random.nextDouble() * Math.PI * 2;
            double r = want + (random.nextDouble() * 2 - 1) * spread;
            double x = p.getX() + Math.cos(ang) * r, z = p.getZ() + Math.sin(ang) * r;
            Vec3 pos = groundAt(x, p.getY(), z, dim);
            if (pos == null) continue;
            Vec3 eye = p.getEyePosition();
            Vec3 head = pos.add(0, dim.height * 0.8, 0);
            boolean visible = level().clip(new ClipContext(eye, head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
            Vec3 dir = pos.subtract(p.position()).multiply(1, 0, 1).normalize();
            double behind = -dir.dot(look);
            int light = level().getMaxLocalRawBrightness(BlockPos.containing(pos).above());
            double score = -Math.abs(r - want) - (visible ? 40 : 0) + behind * 6 - light * 0.4 - Math.abs(pos.y - p.getY()) * 0.8;
            if (score > bestScore) {
                Path path = this.getNavigation().createPath(BlockPos.containing(pos), 1);
                if (path == null || !path.canReach()) continue;
                bestScore = score;
                best = pos;
            }
        }
        return best;
    }

    private Vec3 groundAt(double x, double y, double z, EntityDimensions dim) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = 5; dy >= -8; dy--) {
            m.set(x, y + dy, z);
            BlockPos below = m.below();
            if (level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) continue;
            if (!level().getFluidState(m).isEmpty()) continue;
            AABB box = dim.makeBoundingBox(m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
            if (level().noCollision(this, box)) return new Vec3(m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
        }
        return null;
    }

    private void manageCrawl() {
        if (isCrawling()) {
            AABB stand = STANDING.makeBoundingBox(getX(), getY(), getZ());
            standClear = level().noCollision(this, stand) ? standClear + 10 : 0;
            if (standClear >= 40) setCrawling(false);
        } else if (pathFails >= 2 && (getState() == HUNT || getState() == STALK || getState() == SEARCH)) {
            setCrawling(true);
            pathFails = 0;
            standClear = 0;
        }
    }

    // ------------------------------------------------------------------ sounds / misc
    @Override
    protected SoundEvent getAmbientSound() {
        return getState() == FROZEN || getState() == PEEK ? null : ModSounds.AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() { return 180; }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return ModSounds.HURT.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.DEATH.get(); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        float v = getState() == HUNT ? 0.9F : 0.25F;
        this.playSound(ModSounds.STEP.get(), v, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    protected float getSoundVolume() { return 1.4F; }

    @Override
    public boolean removeWhenFarAway(double d) { return getState() != HUNT; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Initialized", initialized);
        tag.putFloat("Aggression", aggression);
        tag.putBoolean("Relocated", relocatedLowHealth);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        initialized = tag.getBoolean("Initialized");
        aggression = tag.getFloat("Aggression");
        relocatedLowHealth = tag.getBoolean("Relocated");
    }

    // ------------------------------------------------------------------ debug / autotest
    public void debugState(int s, float aggr) {
        this.aggression = aggr;
        initialized = true;
        setState(s);
    }

    public void debugAction(String name) { action(name, 30, null); }

    public void debugCrawl(boolean c) { setCrawling(c); }
}
