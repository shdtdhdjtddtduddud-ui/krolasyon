package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.def.SwordDef;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.magic.SpellCaster;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

/** Every person of the world: villagers, merchants, guards, nobles, kings, slaves, bandits, companions, spouses, children. */
public class RpgNpc extends PathfinderMob implements Merchant, Combat.Allegiance {
    private static final EntityDataAccessor<Byte> RACE = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> GENDER = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ROLE = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> KINGDOM = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(RpgNpc.class, EntityDataSerializers.INT);

    public static final int F_SLAVE = 1, F_FOLLOW = 2, F_SPOUSE = 4, F_STAY = 8, F_DISTRESSED = 16, F_HOSTILE = 32, F_PARTY = 64, F_HELPER = 128, F_FAMILY = 256;
    public static final int CHILD_TICKS = 24000 * 5;
    private static final java.util.Set<String> PROTECTED = java.util.Set.of("mother", "father", "sister", "captain", "priest", "lyra");

    @Nullable private UUID leader;
    private final Map<UUID, Relation> relations = new HashMap<>();
    public String storyId = "";
    @Nullable public BlockPos home;
    public String siteId = "";
    /** kindness, bravery, greed, pride: 0..100 */
    public final int[] traits = new int[4];
    @Nullable private MerchantOffers offers;
    @Nullable private Player tradingPlayer;
    public int wealth;
    public int distressTicks;
    public long leaveAt;
    @Nullable public UUID talkingTo;
    public int talkTicks;
    private int castCooldown = 60;
    public long lastHelped;

    public RpgNpc(EntityType<? extends RpgNpc> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 0).add(Attributes.ATTACK_KNOCKBACK, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RACE, (byte) 0);
        this.entityData.define(GENDER, (byte) 0);
        this.entityData.define(ROLE, (byte) 0);
        this.entityData.define(KINGDOM, (byte) 0);
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(FLAGS, 0);
        this.entityData.define(AGE, 0);
    }

    // ------------------------------------------------------------------ identity
    public Race race() { return Race.of(this.entityData.get(RACE)); }
    public boolean female() { return this.entityData.get(GENDER) == 1; }
    public NpcRole role() { return NpcRole.of(this.entityData.get(ROLE)); }
    public int kingdomId() { return this.entityData.get(KINGDOM); }
    public int variant() { return this.entityData.get(VARIANT); }
    public boolean flag(int f) { return (this.entityData.get(FLAGS) & f) != 0; }
    public void setFlag(int f, boolean on) {
        int v = this.entityData.get(FLAGS);
        this.entityData.set(FLAGS, on ? v | f : v & ~f);
    }
    public int age() { return this.entityData.get(AGE); }
    public boolean isChildNpc() { return age() > 0; }

    public void setRole(NpcRole r) {
        this.entityData.set(ROLE, (byte) r.ordinal());
        equipForRole(this.random);
        refreshStats();
    }

    public void setKingdom(int k) { this.entityData.set(KINGDOM, (byte) k); }
    public void setAge(int ticks) { this.entityData.set(AGE, ticks); refreshDimensions(); }
    public void setLook(int race, boolean female, int variant) {
        this.entityData.set(RACE, (byte) race);
        this.entityData.set(GENDER, (byte) (female ? 1 : 0));
        this.entityData.set(VARIANT, (byte) variant);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (RACE.equals(key) || AGE.equals(key)) this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        Race r = race();
        float child = isChildNpc() ? 0.55F : 1.0F;
        return EntityDimensions.scalable(0.6F * Math.min(1.6F, r.width), 1.95F * r.height * child);
    }

    public float renderScaleY() { return race().height * (isChildNpc() ? 0.55F : 1.0F); }
    public float renderScaleXZ() { return race().width * (isChildNpc() ? 0.6F : 1.0F); }

    public String npcName() { return this.hasCustomName() ? this.getCustomName().getString() : role().title; }

    @Override
    public Component getTypeName() { return Component.literal(race().title + " " + role().title); }

    /** full setup of a freshly created NPC */
    public void setup(Race race, boolean female, NpcRole role, int kingdom, RandomSource r) {
        setLook(race.ordinal(), female, r.nextInt(2));
        setKingdom(kingdom);
        this.entityData.set(ROLE, (byte) role.ordinal());
        this.setCustomName(Component.literal(race.randomName(r, female)));
        for (int i = 0; i < 4; i++) traits[i] = r.nextInt(101);
        if (role == NpcRole.BANDIT || role == NpcRole.SLAVER) traits[0] = r.nextInt(30);
        if (role == NpcRole.PRIEST) traits[0] = 70 + r.nextInt(31);
        if (role == NpcRole.MERCHANT) traits[2] = 50 + r.nextInt(51);
        if (role == NpcRole.NOBLE || role == NpcRole.RULER) traits[3] = 60 + r.nextInt(41);
        wealth = switch (role) { case BEGGAR, SLAVE -> 0; case PEASANT, CHILD, FAMILY -> 5; case NOBLE -> 400; case RULER -> 5000; case MERCHANT -> 200; default -> 40; };
        if (role == NpcRole.BANDIT) setFlag(F_HOSTILE, true);
        if (role == NpcRole.SLAVE) setFlag(F_SLAVE, true);
        equipForRole(r);
        refreshStats();
    }

    public void refreshStats() {
        Race r = race();
        NpcRole role = role();
        float hp = r.health * switch (role) { case GUARD, SOLDIER -> 1.5F; case KNIGHT -> 2.5F; case RULER -> 3.0F; case GUILD_MASTER -> 2.5F; case ADVENTURER, BANDIT -> 1.4F; case CHILD -> 0.5F; default -> 1.0F; };
        float atk = switch (role) { case KNIGHT, GUILD_MASTER, RULER -> 6; case GUARD, SOLDIER, ADVENTURER -> 4; case BANDIT, SLAVER -> 3; default -> 1; } * (r == Race.GIANT ? 1.8F : r == Race.ORC ? 1.3F : 1.0F);
        Objects.requireNonNull(getAttribute(Attributes.MAX_HEALTH)).setBaseValue(hp);
        Objects.requireNonNull(getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(atk);
        Objects.requireNonNull(getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(r == Race.GIANT ? 0.33 : r == Race.HALFLING || r == Race.DWARF ? 0.28 : 0.31);
        Objects.requireNonNull(getAttribute(Attributes.ARMOR)).setBaseValue(r == Race.GIANT ? 6 : r == Race.DWARF ? 3 : 0);
        this.setHealth(this.getMaxHealth());
    }

    private void equip(EquipmentSlot slot, ItemStack st) {
        this.setItemSlot(slot, st);
        this.setDropChance(slot, 0.0F);
    }

    public void equipForRole(RandomSource r) {
        for (EquipmentSlot s : EquipmentSlot.values()) this.setItemSlot(s, ItemStack.EMPTY);
        switch (role()) {
            case GUARD -> {
                equip(EquipmentSlot.MAINHAND, new ItemStack(r.nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE));
                equip(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                equip(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                equip(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            }
            case SOLDIER -> {
                equip(EquipmentSlot.MAINHAND, new ItemStack(r.nextInt(3) == 0 ? Items.IRON_AXE : Items.IRON_SWORD));
                equip(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                equip(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                equip(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            }
            case KNIGHT -> {
                equip(EquipmentSlot.MAINHAND, new ItemStack(RpgItems.sword(r.nextBoolean() ? "knight_oath" : "aldorian_longsword")));
                equip(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                equip(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                equip(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                equip(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                equip(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
            case RULER -> {
                equip(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                equip(EquipmentSlot.MAINHAND, new ItemStack(RpgItems.sword("crown_of_kings")));
            }
            case NOBLE -> { if (r.nextInt(3) == 0) equip(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET)); }
            case BANDIT, SLAVER -> {
                equip(EquipmentSlot.MAINHAND, new ItemStack(r.nextBoolean() ? Items.IRON_SWORD : Items.STONE_SWORD));
                ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
                ((DyeableLeatherItem) Items.LEATHER_CHESTPLATE).setColor(chest, 0x2A2420);
                equip(EquipmentSlot.CHEST, chest);
                this.setDropChance(EquipmentSlot.MAINHAND, 0.08F);
            }
            case ADVENTURER, GUILD_MASTER -> {
                List<SwordDef> pool = new ArrayList<>();
                for (SwordDef s : RpgDefs.SWORDS) if (s.tier() <= (role() == NpcRole.GUILD_MASTER ? 4 : 2)) pool.add(s);
                equip(EquipmentSlot.MAINHAND, new ItemStack(RpgItems.sword(pool.get(r.nextInt(pool.size())).id())));
                equip(EquipmentSlot.CHEST, new ItemStack(r.nextBoolean() ? Items.CHAINMAIL_CHESTPLATE : Items.LEATHER_CHESTPLATE));
            }
            case MAGE -> equip(EquipmentSlot.MAINHAND, new ItemStack(RpgItems.STAFFS.get(r.nextInt(3)).get()));
            case BLACKSMITH -> equip(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            case PRIEST -> equip(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
            case WORKER -> equip(EquipmentSlot.MAINHAND, new ItemStack(r.nextBoolean() ? Items.IRON_SHOVEL : Items.IRON_HOE));
            default -> {}
        }
    }

    // ------------------------------------------------------------------ relations
    public Relation rel(UUID player) {
        return relations.computeIfAbsent(player, k -> {
            Relation r = new Relation();
            r.affinity = (traits[0] - 50) / 10;
            r.trust = 15 + traits[0] / 5;
            return r;
        });
    }

    @Nullable public Relation relIfAny(UUID player) { return relations.get(player); }

    @Nullable @Override
    public UUID leader() { return leader; }

    public void setLeader(@Nullable UUID p) { this.leader = p; }

    @Override
    public int kingdom() { return kingdomId(); }

    public boolean isFighter() { return (role().fighter || flag(F_PARTY) || flag(F_HELPER)) && !isChildNpc() && !flag(F_SLAVE) || flag(F_HOSTILE); }

    public boolean isLawEnforcer() {
        NpcRole r = role();
        return r == NpcRole.GUARD || r == NpcRole.KNIGHT || r == NpcRole.SOLDIER || r == NpcRole.RULER;
    }

    @Override
    public boolean hostileTo(LivingEntity e) {
        if (!e.isAlive() || e == this || isChildNpc()) return false;
        if (leader != null && (leader.equals(e.getUUID()) || leader.equals(Combat.leaderOf(e)))) return false;
        if (e instanceof Enemy && !(e instanceof Combat.Allegiance)) return !flag(F_HOSTILE) && isFighter();
        if (flag(F_HOSTILE)) {
            if (e instanceof RpgNpc n) return !n.flag(F_HOSTILE) && n.role() != NpcRole.SLAVE;
            return e instanceof Player p && !p.isCreative() && !p.isSpectator() || e instanceof AbstractVillager;
        }
        if (!isFighter()) return false;
        if (e instanceof Player p) {
            if (p.isCreative() || p.isSpectator() || !(level() instanceof ServerLevel sl)) return false;
            Relation r = relations.get(p.getUUID());
            if (r != null && r.enemy && (traits[1] > 40 || isLawEnforcer())) return true;
            if (isLawEnforcer() && leader == null) {
                PlayerRpg d = RpgWorldData.player(p);
                int k = kingdomId();
                if (d.bounty[k] >= 150 || d.rep[k] <= -350) return true;
                RpgWorldData w = RpgWorldData.get(sl);
                return d.allegiance >= 0 && w.atWar(d.allegiance, k) && (getPersistentData().getBoolean("Raider") || com.krolasyon.bosses.rpg.world.WorldMap.kingdomAt(w, getX(), getZ()) == k);
            }
            return false;
        }
        if (e instanceof RpgNpc n) {
            if (n.flag(F_HOSTILE)) return isLawEnforcer() || leader != null;
            if (leader != null && n.leader == null && level() instanceof ServerLevel sl) {
                // party members fight soldiers of kingdoms at war with their leader
                Player lp = sl.getPlayerByUUID(leader);
                return lp != null && n.getTarget() == lp;
            }
            if (isLawEnforcer() && n.isLawEnforcer() && level() instanceof ServerLevel sl)
                return RpgWorldData.get(sl).atWar(kingdomId(), n.kingdomId());
        }
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity e) {
        if (leader != null && (leader.equals(e.getUUID()) || leader.equals(Combat.leaderOf(e)))) return true;
        if (e instanceof RpgNpc n && !flag(F_HOSTILE) && !n.flag(F_HOSTILE) && n.kingdomId() == kingdomId() && leader == null && n.leader == null) return true;
        return super.isAlliedTo(e);
    }

    // ------------------------------------------------------------------ ai
    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanOpenDoors(true);
        nav.setCanPassDoors(true);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoldGoal(this));
        this.goalSelector.addGoal(2, new CastGoal(this));
        this.goalSelector.addGoal(2, new FighterMeleeGoal(this));
        this.goalSelector.addGoal(3, new CivilianPanicGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, net.minecraft.world.entity.monster.Monster.class, 10.0F, 0.7, 0.85) {
            @Override public boolean canUse() { return !isFighter() && super.canUse(); }
        });
        this.goalSelector.addGoal(4, new FollowLeaderGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.7));
        this.goalSelector.addGoal(6, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.55) {
            @Override public boolean canUse() { return !flag(F_STAY) && !flag(F_FOLLOW) && super.canUse(); }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new InteractGoal(this, RpgNpc.class, 6.0F, 0.02F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return isFighter() && super.canUse(); }
        }.setAlertOthers(RpgNpc.class));
        this.targetSelector.addGoal(2, new DefendLeaderGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false, this::hostileTo) {
            @Override public boolean canUse() { return isFighter() && super.canUse(); }
        });
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (talkTicks > 0) talkTicks--;
        if (castCooldown > 0) castCooldown--;
        if (isChildNpc()) {
            int a = age() - 1;
            if (a <= 0) {
                setAge(0);
                refreshStats();
                if (leader != null && level() instanceof ServerLevel sl) {
                    Player p = sl.getPlayerByUUID(leader);
                    if (p != null) p.sendSystemMessage(Component.literal("§d" + npcName() + " büyüdü ve artık bir yetişkin! Seninle maceraya atılmaya hazır."));
                }
            } else if (this.tickCount % 20 == 0) {
                this.entityData.set(AGE, a - 19);
            }
        }
        if (home != null && !flag(F_FOLLOW) && leader == null) {
            if (!this.hasRestriction()) this.restrictTo(home, isLawEnforcer() ? 48 : 28);
        } else if (this.hasRestriction() && (flag(F_FOLLOW) || leader != null)) {
            this.clearRestriction();
        }
        if (leaveAt > 0 && level().getGameTime() > leaveAt) {
            FX.send(level(), ParticleTypes.POOF, position().add(0, 1, 0), 20, 0.4, 0.05);
            if (flag(F_HELPER) && leader != null && level() instanceof ServerLevel sl) {
                Player p = sl.getPlayerByUUID(leader);
                if (p != null) p.displayClientMessage(Component.literal("§7" + npcName() + " yoluna devam ediyor. \"Kendine iyi bak!\""), false);
            }
            this.discard();
            return;
        }
        if (flag(F_DISTRESSED) && --distressTicks <= 0) setFlag(F_DISTRESSED, false);
        if (this.tickCount % 40 == 0 && getHealth() < getMaxHealth() && getTarget() == null) heal(1.0F);
        if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
        if (getTarget() instanceof Player p && !hostileTo(p) && getLastHurtByMob() != p) setTarget(null);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean requiresCustomPersistence() { return true; }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (flag(F_HOSTILE)) return InteractionResult.PASS;
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            this.talkingTo = sp.getUUID();
            this.talkTicks = 200;
            this.getNavigation().stop();
            NpcDialog.open(sp, this, "root");
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!storyId.isEmpty() && PROTECTED.contains(storyId) && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (src.getEntity() != null && leader != null && leader.equals(src.getEntity().getUUID()) && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (src.getEntity() instanceof Player p && !p.isShiftKeyDown()) return false;
        }
        boolean r = super.hurt(src, amount);
        if (r && !level().isClientSide() && src.getEntity() instanceof ServerPlayer p) NpcEvents.onHurtByPlayer(this, p, amount);
        return r;
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide()) NpcEvents.onDeath(this, src);
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        boolean r = super.doHurtTarget(e);
        if (r) this.swing(InteractionHand.MAIN_HAND);
        return r;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (flag(F_DISTRESSED)) return SoundEvents.VILLAGER_HURT;
        Race r = race();
        if (r == Race.GIANT || r == Race.ORC) return null;
        return this.random.nextInt(4) == 0 ? (female() ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_AMBIENT) : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return race() == Race.GIANT || race() == Race.ORC ? SoundEvents.RAVAGER_HURT : SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return race() == Race.GIANT ? SoundEvents.RAVAGER_DEATH : SoundEvents.PLAYER_DEATH; }

    @Override
    public float getVoicePitch() {
        float base = female() ? 1.2F : 0.95F;
        if (isChildNpc()) base += 0.4F;
        if (race() == Race.GIANT) base = 0.6F;
        if (race() == Race.DWARF || race() == Race.ORC) base -= 0.15F;
        return base + (this.random.nextFloat() - 0.5F) * 0.1F;
    }

    // ------------------------------------------------------------------ merchant
    @Override public void setTradingPlayer(@Nullable Player p) { this.tradingPlayer = p; }
    @Nullable @Override public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) offers = NpcTrades.create(this);
        return offers;
    }

    @Override public void overrideOffers(MerchantOffers o) { this.offers = o; }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        if (tradingPlayer != null) {
            Relation r = rel(tradingPlayer.getUUID());
            if (r.affinity < 40) r.add(1, 1);
        }
        FX.send(level(), ParticleTypes.HAPPY_VILLAGER, position().add(0, getBbHeight() + 0.3, 0), 3, 0.3, 0.02);
    }

    @Override public void notifyTradeUpdated(ItemStack st) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide(); }

    public void restockOffers() {
        if (offers != null) for (MerchantOffer o : offers) o.resetUses();
    }

    public void openTrade(ServerPlayer p) {
        MerchantOffers o = getOffers();
        int discount = NpcTrades.discountPercent(this, p);
        for (MerchantOffer of : o) {
            of.resetSpecialPriceDiff();
            int base = of.getBaseCostA().getCount();
            int diff = -(int) Math.floor(base * discount / 100.0);
            if (diff != 0) of.addToSpecialPriceDiff(diff);
        }
        this.setTradingPlayer(p);
        this.openTradingScreen(p, Component.literal(npcName() + " — " + role().title), 1);
    }

    // ------------------------------------------------------------------ spells for mages
    public boolean tryCast(LivingEntity target) {
        if (castCooldown > 0 || role() != NpcRole.MAGE && !(flag(F_PARTY) && getMainHandItem().getItem() instanceof com.krolasyon.bosses.rpg.item.StaffItem)) return false;
        List<SpellDef> pool = new ArrayList<>();
        for (SpellDef s : RpgDefs.SPELLS) {
            if (s.tier() > 3) continue;
            switch (s.shape()) { case BOLT, BALL, CHAIN, STRIKE -> pool.add(s); default -> {} }
        }
        SpellDef s = pool.get(Math.floorMod(getUUID().hashCode() + this.tickCount / 200, pool.size()));
        this.getLookControl().setLookAt(target, 60, 60);
        this.lookAt(target, 60, 60);
        this.setXRot((float) -Math.toDegrees(Math.atan2(target.getEyeY() - getEyeY(), Math.sqrt(distanceToSqr(target.getX(), getY(), target.getZ())))));
        SpellCaster.cast(this, s, 0.8F);
        this.swing(InteractionHand.MAIN_HAND);
        castCooldown = 50 + this.random.nextInt(30);
        return true;
    }

    // ------------------------------------------------------------------ save
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putByte("Race", this.entityData.get(RACE));
        t.putByte("Gender", this.entityData.get(GENDER));
        t.putByte("Role", this.entityData.get(ROLE));
        t.putByte("Kingdom", this.entityData.get(KINGDOM));
        t.putByte("Variant", this.entityData.get(VARIANT));
        t.putInt("Flags", this.entityData.get(FLAGS));
        t.putInt("Age", age());
        if (leader != null) t.putUUID("Leader", leader);
        ListTag rl = new ListTag();
        for (Map.Entry<UUID, Relation> e : relations.entrySet()) {
            CompoundTag x = e.getValue().save();
            x.putUUID("P", e.getKey());
            rl.add(x);
        }
        t.put("Relations", rl);
        t.putString("Story", storyId);
        if (home != null) t.putLong("Home", home.asLong());
        t.putString("Site", siteId);
        t.putIntArray("Traits", traits);
        if (offers != null) t.put("Offers", offers.createTag());
        t.putInt("Wealth", wealth);
        t.putLong("LeaveAt", leaveAt);
        t.putLong("LastHelped", lastHelped);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        this.entityData.set(RACE, t.getByte("Race"));
        this.entityData.set(GENDER, t.getByte("Gender"));
        this.entityData.set(ROLE, t.getByte("Role"));
        this.entityData.set(KINGDOM, t.getByte("Kingdom"));
        this.entityData.set(VARIANT, t.getByte("Variant"));
        this.entityData.set(FLAGS, t.getInt("Flags"));
        this.entityData.set(AGE, t.getInt("Age"));
        leader = t.hasUUID("Leader") ? t.getUUID("Leader") : null;
        relations.clear();
        for (Tag x : t.getList("Relations", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            if (c.hasUUID("P")) relations.put(c.getUUID("P"), Relation.load(c));
        }
        storyId = t.getString("Story");
        home = t.contains("Home") ? BlockPos.of(t.getLong("Home")) : null;
        siteId = t.getString("Site");
        int[] tr = t.getIntArray("Traits");
        System.arraycopy(tr, 0, traits, 0, Math.min(tr.length, 4));
        if (t.contains("Offers")) offers = new MerchantOffers(t.getCompound("Offers"));
        wealth = t.getInt("Wealth");
        leaveAt = t.getLong("LeaveAt");
        lastHelped = t.getLong("LastHelped");
        refreshDimensions();
    }

    // ------------------------------------------------------------------ goals
    static class HoldGoal extends Goal {
        private final RpgNpc npc;

        HoldGoal(RpgNpc npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return npc.getTarget() == null && (npc.talkTicks > 0 || npc.tradingPlayer != null || npc.flag(F_STAY) && !npc.flag(F_FOLLOW));
        }

        @Override
        public void tick() {
            npc.getNavigation().stop();
            Player p = npc.tradingPlayer;
            if (p == null && npc.talkingTo != null) p = npc.level().getPlayerByUUID(npc.talkingTo);
            if (p != null && npc.talkTicks > 0) npc.getLookControl().setLookAt(p, 30, 30);
        }
    }

    static class FighterMeleeGoal extends MeleeAttackGoal {
        private final RpgNpc npc;

        FighterMeleeGoal(RpgNpc npc) {
            super(npc, 1.15, true);
            this.npc = npc;
        }

        @Override public boolean canUse() { return npc.isFighter() && npc.role() != NpcRole.MAGE && super.canUse(); }
        @Override public boolean canContinueToUse() { return npc.isFighter() && super.canContinueToUse(); }
    }

    static class CastGoal extends Goal {
        private final RpgNpc npc;

        CastGoal(RpgNpc npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = npc.getTarget();
            return t != null && t.isAlive() && (npc.role() == NpcRole.MAGE || npc.getMainHandItem().getItem() instanceof com.krolasyon.bosses.rpg.item.StaffItem);
        }

        @Override
        public void tick() {
            LivingEntity t = npc.getTarget();
            if (t == null) return;
            double d = npc.distanceToSqr(t);
            npc.getLookControl().setLookAt(t, 30, 30);
            if (d > 14 * 14 || !npc.getSensing().hasLineOfSight(t)) npc.getNavigation().moveTo(t, 1.0);
            else if (d < 5 * 5) {
                net.minecraft.world.phys.Vec3 away = npc.position().subtract(t.position()).normalize().scale(4);
                npc.getNavigation().moveTo(npc.getX() + away.x, npc.getY(), npc.getZ() + away.z, 1.1);
            } else npc.getNavigation().stop();
            if (d < 20 * 20 && npc.getSensing().hasLineOfSight(t)) npc.tryCast(t);
        }

        @Override public boolean requiresUpdateEveryTick() { return true; }
    }

    static class CivilianPanicGoal extends PanicGoal {
        private final RpgNpc npc;

        CivilianPanicGoal(RpgNpc npc) {
            super(npc, 0.9);
            this.npc = npc;
        }

        @Override public boolean canUse() { return !npc.isFighter() && super.canUse(); }
    }

    static class FollowLeaderGoal extends Goal {
        private final RpgNpc npc;
        private int repath;

        FollowLeaderGoal(RpgNpc npc) {
            this.npc = npc;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Nullable
        private Player leaderPlayer() {
            return npc.leader == null ? null : npc.level().getPlayerByUUID(npc.leader);
        }

        @Override
        public boolean canUse() {
            if (!npc.flag(F_FOLLOW) || npc.flag(F_STAY)) return false;
            Player p = leaderPlayer();
            return p != null && !p.isSpectator() && npc.distanceToSqr(p) > 9 && (npc.getTarget() == null || npc.distanceToSqr(p) > 400);
        }

        @Override
        public void start() { repath = 0; }

        @Override
        public void tick() {
            Player p = leaderPlayer();
            if (p == null) return;
            npc.getLookControl().setLookAt(p, 10, npc.getMaxHeadXRot());
            if (npc.distanceToSqr(p) > 28 * 28 && p.onGround()) {
                npc.teleportTo(p.getX() + npc.random.nextInt(3) - 1, p.getY(), p.getZ() + npc.random.nextInt(3) - 1);
                npc.getNavigation().stop();
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                npc.getNavigation().moveTo(p, npc.distanceToSqr(p) > 100 ? 1.25 : 1.0);
            }
        }

        @Override
        public void stop() { npc.getNavigation().stop(); }
    }

    static class DefendLeaderGoal extends TargetGoal {
        private final RpgNpc npc;
        @Nullable private LivingEntity found;

        DefendLeaderGoal(RpgNpc npc) {
            super(npc, false);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            if (npc.leader == null || !npc.isFighter()) return false;
            Player p = npc.level().getPlayerByUUID(npc.leader);
            if (p == null) return false;
            LivingEntity t = p.getLastHurtByMob();
            if (t == null || !t.isAlive() || p.tickCount - p.getLastHurtByMobTimestamp() > 200) t = p.getLastHurtMob();
            if (t == null || !t.isAlive() || t == npc || Combat.sameSide(npc, t)) return false;
            if (t instanceof RpgNpc other && other.leader != null && other.leader.equals(npc.leader)) return false;
            found = t;
            return true;
        }

        @Override
        public void start() {
            npc.setTarget(found);
            super.start();
        }
    }
}
