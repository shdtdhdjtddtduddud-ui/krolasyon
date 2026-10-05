package com.krolasyon.sololeveling.entity;

import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.world.Dialogs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

/** Every human of the world: Association staff, guild masters, famous hunters, merchants and citizens. */
public class HunterNpc extends PathfinderMob {
    private static final EntityDataAccessor<String> ROLE = SynchedEntityData.defineId(HunterNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(HunterNpc.class, EntityDataSerializers.STRING);

    /** Roles that stand at their post and cannot be hurt. */
    public static final Set<String> STAFF = Set.of("receptionist", "woo_jinchul", "chairman", "master_hunters", "master_white_tiger", "master_fiend",
            "master_knights", "yoo_jinho", "cha_haein", "healer", "merchant", "blacksmith", "alchemist", "guide", "reporter", "jinah");
    public static final Set<String> NAMED = Set.of("woo_jinchul", "chairman", "master_hunters", "master_white_tiger", "master_fiend", "master_knights",
            "yoo_jinho", "cha_haein", "jinah");
    private static final String[] SURNAMES = {"Kim", "Lee", "Park", "Choi", "Jung", "Kang", "Cho", "Yoon", "Jang", "Lim", "Han", "Oh", "Seo", "Shin", "Kwon", "Hwang"};
    private static final String[] GIVEN = {"Minjun", "Seojun", "Doyun", "Jiho", "Haneul", "Yerin", "Seoyeon", "Jiwoo", "Hayoon", "Sumin", "Taeyang", "Jisoo",
            "Hyunwoo", "Eunji", "Dongha", "Minseo", "Junho", "Chaewon", "Sungmin", "Yuna"};

    private int idleLookTicks;

    public HunterNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ARMOR, 6);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROLE, "citizen");
        entityData.define(SKIN, "citizen_0");
    }

    public String role() { return entityData.get(ROLE); }

    public String skin() { return entityData.get(SKIN); }

    public boolean isStaff() { return STAFF.contains(role()); }

    public boolean isFighter() {
        String r = role();
        return r.equals("hunter") || r.equals("cha_haein") || r.startsWith("master_") || r.equals("woo_jinchul");
    }

    /** Picks name, skin and equipment for a role. */
    public void setupRole(String role, RandomSource r) {
        entityData.set(ROLE, role);
        String skin = switch (role) {
            case "citizen" -> "citizen_" + r.nextInt(8);
            case "hunter" -> "hunter_" + r.nextInt(6);
            default -> role;
        };
        entityData.set(SKIN, skin);
        if (NAMED.contains(role) || STAFF.contains(role)) setCustomName(Component.translatable("sololeveling.npc." + role));
        else setCustomName(Component.literal(SURNAMES[r.nextInt(SURNAMES.length)] + " " + GIVEN[r.nextInt(GIVEN.length)]));
        setCustomNameVisible(STAFF.contains(role));
        ItemStack hand = switch (role) {
            case "hunter" -> new ItemStack(r.nextBoolean() ? ModItems.HUNTER_SWORD.get() : ModItems.STEEL_DAGGER.get());
            case "cha_haein" -> new ItemStack(ModItems.HAEIN_SWORD.get());
            case "master_hunters" -> new ItemStack(ModItems.FLAME_STAFF.get());
            case "master_fiend" -> new ItemStack(ModItems.ORC_WAR_AXE.get());
            case "master_knights" -> new ItemStack(ModItems.CRIMSON_GREATSWORD.get());
            case "blacksmith" -> new ItemStack(ModItems.HUNTER_SWORD.get());
            case "alchemist" -> new ItemStack(ModItems.MANA_POTION.get());
            case "merchant" -> new ItemStack(ModItems.HEALING_POTION.get());
            case "reporter", "guide" -> new ItemStack(ModItems.NEWSPAPER.get());
            default -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.MAINHAND, hand);
        setDropChance(EquipmentSlot.MAINHAND, 0);
        if (role.equals("hunter")) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HUNTER_CHEST.get()));
            setDropChance(EquipmentSlot.CHEST, 0);
        }
        if (isFighter()) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(role.equals("hunter") ? 60 : 300);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(role.equals("hunter") ? 9 : 30);
            setHealth(getMaxHealth());
        }
        if (isStaff()) setPersistenceRequired();
        reassessGoals();
    }

    @Override
    protected void registerGoals() {
        reassessGoals();
    }

    private void reassessGoals() {
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        boolean staff = STAFF.contains(entityData == null ? "" : role());
        if (isFighterSafe()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
            targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, e -> e instanceof Enemy && !(e instanceof ShadowEntity)));
        } else {
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, SLMonster.class, 10F, 1.0, 1.3));
            goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        }
        if (!staff) {
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
            goalSelector.addGoal(6, new OpenDoorGoal(this, true));
        }
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    private boolean isFighterSafe() {
        try {
            return isFighter();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            idleLookTicks = 60;
            Dialogs.open(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (idleLookTicks > 0) idleLookTicks--;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isStaff() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (src.getEntity() instanceof Player p && !p.isCreative() && !isFighter()) return super.hurt(src, amount * 0.5F);
        return super.hurt(src, amount);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return !isStaff() && !isPersistenceRequired(); }

    @Override
    public void checkDespawn() {
        if (!isStaff() && !isPersistenceRequired() && level().getNearestPlayer(this, 96) == null && tickCount > 600) discard();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean isPushable() { return !isStaff(); }

    @Override
    public boolean canBeLeashed(Player p) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("Role", role());
        t.putString("Skin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("Role")) entityData.set(ROLE, t.getString("Role"));
        if (t.contains("Skin")) entityData.set(SKIN, t.getString("Skin"));
        reassessGoals();
    }
}
