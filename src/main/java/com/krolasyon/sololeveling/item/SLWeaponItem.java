package com.krolasyon.sololeveling.item;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.entity.SLProjectile;
import com.krolasyon.sololeveling.registry.ModSounds;
import com.krolasyon.sololeveling.system.HunterCapability;
import com.krolasyon.sololeveling.system.HunterData;
import com.krolasyon.sololeveling.system.SkillExecutor;
import com.krolasyon.sololeveling.system.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

/** Every Solo Leveling weapon: daggers, swords, staves and the Orb of Avarice. */
public class SLWeaponItem extends SwordItem {
    public enum Fx { NONE, VENOM, ARMOR_BREAK, FROST, LIGHTNING, DRAGON_FLAME, HEAVY, SWIFT, FIRE, MANA }

    public enum Active { NONE, THUNDER, DRAGON_WAVE, DASH_SLASH, FIREBALL, MANA_BOLT, GROUND_SLAM, SHADOW_SLASH }

    public final boolean dagger;
    public final Fx fx;
    public final Active active;
    public final int activeCooldown;
    private final String rankLabel;

    public SLWeaponItem(int durability, int damage, float speed, boolean dagger, Fx fx, Active active, int activeCooldown, String rankLabel, Rarity rarity) {
        super(new SimpleTier(durability, damage), 0, speed, new Properties().rarity(rarity).fireResistant());
        this.dagger = dagger;
        this.fx = fx;
        this.active = active;
        this.activeCooldown = activeCooldown;
        this.rankLabel = rankLabel;
    }

    record SimpleTier(int uses, float bonus) implements Tier {
        @Override public int getUses() { return uses; }
        @Override public float getSpeed() { return 8F; }
        @Override public float getAttackDamageBonus() { return bonus - 1; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(com.krolasyon.sololeveling.registry.ModItems.MAGIC_STONE_C.get()); }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        Level l = attacker.level();
        switch (fx) {
            case VENOM -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                if (attacker.getRandom().nextFloat() < 0.2F) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 6));
            }
            case ARMOR_BREAK -> {
                if (target.getArmorValue() > 6 || (target instanceof SLMonster m && m.kind.category == MobKind.Category.KNIGHT)) {
                    target.invulnerableTime = 0;
                    target.hurt(attacker.damageSources().magic(), getDamage() * 0.5F);
                }
            }
            case FROST -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            case LIGHTNING -> {
                if (attacker.getRandom().nextFloat() < 0.25F && l instanceof ServerLevel sl) {
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4), e -> e != attacker && e != target && e instanceof net.minecraft.world.entity.monster.Enemy)) {
                        e.hurt(attacker.damageSources().lightningBolt(), getDamage() * 0.5F);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
                    }
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                }
            }
            case DRAGON_FLAME -> {
                target.setSecondsOnFire(4);
                if (l instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1, target.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
            }
            case HEAVY -> {
                Vec3 d = target.position().subtract(attacker.position()).normalize();
                target.push(d.x * 0.8, 0.25, d.z * 0.8);
            }
            case FIRE -> target.setSecondsOnFire(5);
            default -> {}
        }
        return r;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (active == Active.NONE) return super.use(level, player, hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            HunterData d = HunterCapability.get(sp);
            float cost = 12;
            if (d.mana < cost && !sp.isCreative()) {
                sp.displayClientMessage(Component.translatable("sololeveling.skill.no_mana").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (!sp.isCreative()) d.mana -= cost;
            d.markDirty();
            activate(sp, (ServerLevel) level, d);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, activeCooldown);
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void activate(ServerPlayer p, ServerLevel l, HunterData d) {
        Vec3 look = p.getLookAngle();
        float magic = SkillExecutor.magic(d);
        switch (active) {
            case THUNDER -> {
                LivingEntity t = SkillExecutor.lookTarget(p, 28);
                Vec3 at = t != null ? t.position() : p.position().add(look.scale(10));
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(l);
                if (b != null) {
                    b.moveTo(at.x, at.y, at.z);
                    b.setVisualOnly(true);
                    l.addFreshEntity(b);
                }
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3), e -> SkillExecutor.isEnemy(p, e)))
                    e.hurt(p.damageSources().indirectMagic(p, p), getDamage() * 0.8F + magic * 0.5F);
            }
            case DRAGON_WAVE -> {
                for (int i = 1; i <= 12; i++) {
                    int k = i;
                    com.krolasyon.sololeveling.system.Scheduler.later(i, () -> {
                        Vec3 q = p.getEyePosition().add(look.scale(k * 1.2));
                        l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y - 0.4, q.z, 10, 0.5, 0.5, 0.5, 0.02);
                        l.sendParticles(new DustParticleOptions(new Vector3f(0.4F, 0.3F, 1F), 2F), q.x, q.y - 0.4, q.z, 6, 0.4, 0.4, 0.4, 0);
                        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(q, q).inflate(1.6), e -> SkillExecutor.isEnemy(p, e))) {
                            if (e.invulnerableTime > 0) continue;
                            e.hurt(p.damageSources().indirectMagic(p, p), getDamage() * 1.1F + d.stat(Stat.STR) * 0.2F);
                            e.setSecondsOnFire(5);
                        }
                    });
                }
                l.playSound(null, p.blockPosition(), ModSounds.ROAR.get(), SoundSource.PLAYERS, 1.4F, 1.3F);
            }
            case DASH_SLASH -> {
                Vec3 dir = new Vec3(look.x, 0, look.z).normalize();
                Vec3 end = p.position().add(dir.scale(6));
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(p.position(), end).inflate(1.3), e -> SkillExecutor.isEnemy(p, e))) {
                    e.hurt(p.damageSources().playerAttack(p), getDamage() * 1.3F);
                    l.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 2, 0.2, 0.2, 0.2, 0);
                }
                p.setDeltaMovement(dir.scale(1.8).add(0, 0.1, 0));
                p.hurtMarked = true;
                l.playSound(null, p.blockPosition(), ModSounds.DASH.get(), SoundSource.PLAYERS, 1F, 1.3F);
            }
            case FIREBALL -> {
                SLProjectile.shoot(p, SLProjectile.Kind.FIREBALL, look, 1.4, 6 + magic * 1.2F).explode = 3F;
                l.playSound(null, p.blockPosition(), ModSounds.FIRE.get(), SoundSource.PLAYERS, 1F, 1F);
            }
            case MANA_BOLT -> {
                SLProjectile.shoot(p, SLProjectile.Kind.MANA_BOLT, look, 1.8, 5 + magic).pierce = 1;
                l.playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.6F);
            }
            case GROUND_SLAM -> {
                Vec3 c = p.position().add(look.multiply(2.5, 0, 2.5));
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(3.5, 1.5, 3.5), e -> SkillExecutor.isEnemy(p, e))) {
                    e.hurt(p.damageSources().playerAttack(p), getDamage() * 1.2F);
                    e.push(0, 0.6, 0);
                    e.hurtMarked = true;
                }
                l.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 4, 1.2, 0.1, 1.2, 0);
                l.playSound(null, p.blockPosition(), ModSounds.SLAM.get(), SoundSource.PLAYERS, 1.2F, 1F);
            }
            case SHADOW_SLASH -> {
                SLProjectile pr = SLProjectile.shoot(p, SLProjectile.Kind.FLAME_SLASH, look, 1.5, getDamage() * 1.2F);
                pr.pierce = 4;
                l.playSound(null, p.blockPosition(), ModSounds.SLASH.get(), SoundSource.PLAYERS, 1F, 0.8F);
            }
            default -> {}
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("sololeveling.tooltip.rank", rankLabel).withStyle(ChatFormatting.AQUA));
        String key = getDescriptionId() + ".desc";
        tip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        if (active != Active.NONE) tip.add(Component.translatable("sololeveling.tooltip.active." + active.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
