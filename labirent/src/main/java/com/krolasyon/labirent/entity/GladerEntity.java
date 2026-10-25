package com.krolasyon.labirent.entity;

import com.krolasyon.labirent.entity.ai.GladerBrainGoal;
import com.krolasyon.labirent.item.CaptainBatonItem;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.registry.ModItems;
import com.krolasyon.labirent.team.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/** A fellow Glader. Works a job, obeys the captain, runs the maze, sleeps at night and can get stung. */
public class GladerEntity extends PathfinderMob {
    public static final int SKINS = 9;
    public static final int SKIN_TERESA = 9;
    public static final String[] SKIN_NAMES = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};

    private static final EntityDataAccessor<String> DATA_NAME = SynchedEntityData.defineId(GladerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(GladerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(GladerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_NEWCOMER = SynchedEntityData.defineId(GladerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STUNG = SynchedEntityData.defineId(GladerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ORDER = SynchedEntityData.defineId(GladerEntity.class, EntityDataSerializers.INT);

    public enum Order { WORK, FOLLOW, STAY, RETURN, GATHER, DEFEND }

    public final SimpleContainer inventory = new SimpleContainer(9);
    @Nullable private BlockPos home;
    @Nullable private BlockPos orderPos;
    @Nullable private UUID followTarget;
    @Nullable private BlockPos bed;
    private boolean teresa;
    private int talkCooldown;
    private String story = "";

    public GladerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(false);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_NAME, "Ayrıklı");
        entityData.define(DATA_SKIN, 0);
        entityData.define(DATA_ROLE, 0);
        entityData.define(DATA_NEWCOMER, false);
        entityData.define(DATA_STUNG, false);
        entityData.define(DATA_ORDER, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() { return !isSleeping() && super.canUse(); }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, GrieverEntity.class, 10F, 1.1, 1.45) {
            @Override
            public boolean canUse() { return getRole() != Role.BEKCI && getOrder() != Order.DEFEND && getHealth() < getMaxHealth() * 0.9F && super.canUse(); }
        });
        goalSelector.addGoal(3, new GladerBrainGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GladerEntity.class, Player.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, GrieverEntity.class, 10, true, false,
                e -> getRole() == Role.BEKCI || getOrder() == Order.DEFEND || distanceToSqr(e) < 36));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, BeetleBladeEntity.class, 40, true, false, e -> getRole() == Role.BEKCI));
    }

    // ------------------------------------------------------------------ data
    public void setup(String name, int skin, Role role) {
        entityData.set(DATA_NAME, name);
        entityData.set(DATA_SKIN, skin);
        setRole(role);
    }

    public String getGladerName() { return entityData.get(DATA_NAME); }

    public int getSkin() { return entityData.get(DATA_SKIN); }

    public Role getRole() { return Role.byId(entityData.get(DATA_ROLE)); }

    public void setRole(Role r) {
        entityData.set(DATA_ROLE, r.ordinal());
        equipFor(r);
        if (r == Role.SIFACI && !inventory.hasAnyMatching(s -> s.is(ModItems.GRIEF_SERUM.get())))
            inventory.addItem(new ItemStack(ModItems.GRIEF_SERUM.get()));
        home = null;
    }

    private void equipFor(Role r) {
        ItemStack hand = switch (r) {
            case BEKCI -> new ItemStack(ModItems.SPEAR.get());
            case KOSUCU -> new ItemStack(ModItems.MACHETE.get());
            case ASCI -> new ItemStack(ModItems.FRYING_PAN.get());
            case BAHCIVAN -> new ItemStack(Items.IRON_HOE);
            case KASAP -> new ItemStack(Items.IRON_AXE);
            case SIFACI -> new ItemStack(ModItems.BANDAGE.get());
            default -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.MAINHAND, hand);
        setDropChance(EquipmentSlot.MAINHAND, 0F);
        double dmg = r == Role.BEKCI ? 7 : r == Role.KOSUCU ? 5 : 3;
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(r == Role.KOSUCU ? 0.36 : 0.32);
    }

    public boolean isNewcomer() { return entityData.get(DATA_NEWCOMER); }

    public void setNewcomer(boolean b) { entityData.set(DATA_NEWCOMER, b); }

    public boolean isStung() { return entityData.get(DATA_STUNG); }

    public void setStung(boolean b) { entityData.set(DATA_STUNG, b); }

    public boolean isTeresa() { return teresa; }

    public void setTeresa() {
        teresa = true;
        story = "teresa";
    }

    public String storyId() { return story; }

    public void setStoryId(String s) { story = s; }

    public Order getOrder() { return Order.values()[Math.max(0, Math.min(Order.values().length - 1, entityData.get(DATA_ORDER)))]; }

    public void setOrder(Order o, @Nullable BlockPos pos) {
        entityData.set(DATA_ORDER, o.ordinal());
        orderPos = pos;
    }

    @Nullable public BlockPos orderPos() { return orderPos; }

    @Nullable public UUID followTarget() { return followTarget; }

    public void setFollowTarget(UUID id) { followTarget = id; }

    public BlockPos home() {
        if (home == null) home = defaultHome();
        return home;
    }

    public void setHome(BlockPos p) { home = p; }

    @Nullable public BlockPos bed() { return bed; }

    public void setBed(@Nullable BlockPos b) { bed = b; }

    private BlockPos defaultHome() {
        return switch (getRole()) {
            case ASCI -> Glade.KITCHEN_FIRE.offset(0, 0, 2);
            case BAHCIVAN -> new BlockPos(14, Glade.Y, 12);
            case KASAP -> new BlockPos(14, Glade.Y, 24);
            case SIFACI -> new BlockPos(14, Glade.Y, -16);
            case BEKCI -> Glade.doorInside(Math.abs(getUUID().hashCode()) % 4);
            case KOSUCU -> new BlockPos(-17, Glade.Y, -12);
            default -> Glade.GATHER.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
        };
    }

    // ------------------------------------------------------------------ display
    @Override
    public Component getDisplayName() {
        MutableComponent c = Component.literal(getGladerName()).withStyle(ChatFormatting.WHITE);
        if (isStung()) return c.append(Component.literal(" ☠").withStyle(ChatFormatting.DARK_GREEN));
        if (isNewcomer()) return c.append(Component.literal(" [").append(Component.translatable("label.labirent.greenie")).append("]").withStyle(ChatFormatting.GREEN));
        return c.append(Component.literal(" [").withStyle(getRole().color)).append(getRole().title()).append(Component.literal("]").withStyle(getRole().color));
    }

    @Override
    public boolean shouldShowName() { return true; }

    @Override
    public boolean isCustomNameVisible() { return true; }

    // ------------------------------------------------------------------ interaction
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof CaptainBatonItem || held.is(ModItems.GRIEF_SERUM.get()) || held.is(ModItems.BANDAGE.get()))
            return InteractionResult.PASS; // handled by the item
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (isSleeping()) {
                sp.displayClientMessage(Component.translatable("msg.labirent.sleeping", getGladerName()), true);
                return InteractionResult.CONSUME;
            }
            MazeManager m = MazeManager.get();
            if (m != null) m.story.onTalk(sp, this);
            if (getRole() == Role.ASCI && sp.getFoodData().getFoodLevel() < 18 && com.krolasyon.labirent.entity.ai.Jobs.serveMeal(this, sp)) {
                return InteractionResult.CONSUME;
            }
            if (talkCooldown <= 0) {
                talkCooldown = 30;
                say(sp, Component.translatable(lineKey()));
            }
        }
        return InteractionResult.CONSUME;
    }

    private String lineKey() {
        if (isStung()) return "talk.labirent.stung." + random.nextInt(3);
        if (isNewcomer()) return "talk.labirent.newcomer." + random.nextInt(3);
        if (!story.isEmpty()) {
            MazeManager m = MazeManager.get();
            int ch = m == null ? 0 : m.state.chapter;
            return "talk.labirent." + story + "." + Math.min(ch, 8);
        }
        return "talk.labirent." + getRole().id + "." + random.nextInt(4);
    }

    public void say(Player to, Component line) {
        to.sendSystemMessage(Component.literal("<" + getGladerName() + "> ").withStyle(getRole().color).append(line.copy().withStyle(ChatFormatting.WHITE)));
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, getSoundSource(), 0.5F, 1.3F + random.nextFloat() * 0.2F);
    }

    public void ackOrder(Player captain) {
        if (random.nextFloat() < 0.35F && distanceToSqr(captain) < 30 * 30) say(captain, Component.translatable("talk.labirent.ack." + random.nextInt(4)));
    }

    public void sendRunning() { GladerBrainGoal.forceRun(this); }

    // ------------------------------------------------------------------ life
    @Override
    public void aiStep() {
        super.aiStep();
        updateSwingTime();
        if (talkCooldown > 0) talkCooldown--;
        if (!level().isClientSide) {
            boolean venom = hasEffect(ModEffects.VENOM.get());
            if (venom != isStung()) setStung(venom);
            if (tickCount % 80 == 0 && getHealth() < getMaxHealth() && !venom) heal(1F);
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isSleeping() && !level().isClientSide) stopSleeping();
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide) {
            MazeManager m = MazeManager.get();
            if (m != null) m.story.onGladerDied(this, src);
            for (int k = 0; k < inventory.getContainerSize(); k++) spawnAtLocation(inventory.getItem(k));
            inventory.clearContent();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean canBeLeashed(Player p) { return false; }

    // ------------------------------------------------------------------ save
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("GladerName", getGladerName());
        t.putInt("Skin", getSkin());
        t.putInt("Role", getRole().ordinal());
        t.putBoolean("Newcomer", isNewcomer());
        t.putInt("Order", getOrder().ordinal());
        t.putBoolean("Teresa", teresa);
        t.putString("Story", story);
        if (home != null) t.put("Home", NbtUtils.writeBlockPos(home));
        if (orderPos != null) t.put("OrderPos", NbtUtils.writeBlockPos(orderPos));
        if (bed != null) t.put("Bed", NbtUtils.writeBlockPos(bed));
        if (followTarget != null) t.putUUID("Follow", followTarget);
        t.put("Inv", inventory.createTag());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(DATA_NAME, t.getString("GladerName"));
        entityData.set(DATA_SKIN, t.getInt("Skin"));
        entityData.set(DATA_ROLE, t.getInt("Role"));
        equipFor(getRole());
        setNewcomer(t.getBoolean("Newcomer"));
        entityData.set(DATA_ORDER, t.getInt("Order"));
        teresa = t.getBoolean("Teresa");
        story = t.getString("Story");
        if (t.contains("Home")) home = NbtUtils.readBlockPos(t.getCompound("Home"));
        if (t.contains("OrderPos")) orderPos = NbtUtils.readBlockPos(t.getCompound("OrderPos"));
        if (t.contains("Bed")) bed = NbtUtils.readBlockPos(t.getCompound("Bed"));
        if (t.hasUUID("Follow")) followTarget = t.getUUID("Follow");
        inventory.fromTag(t.getList("Inv", 10));
    }

    /** used by the screen packet */
    public void openFor(ServerPlayer p) { Net.open(p, Net.SCREEN_NPC, getId()); }
}
