package com.krolasyon.bosses.rpg.item;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.RpgDefs.SwordPassive;
import com.krolasyon.bosses.rpg.def.SwordDef;
import com.krolasyon.bosses.rpg.magic.SwordSkills;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Named sword with an on-hit passive and a right-click skill. */
public class RpgSwordItem extends SwordItem {
    public static final Tier MYTHIC = new Tier() {
        @Override public int getUses() { return 3200; }
        @Override public float getSpeed() { return 10.0F; }
        @Override public float getAttackDamageBonus() { return 5.0F; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 22; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.NETHERITE_INGOT); }
    };

    public final SwordDef def;

    public RpgSwordItem(SwordDef def) {
        super(tier(def.tier()), Math.round(def.damage()), def.speed(), new Item.Properties().rarity(def.tier() >= 5 ? Rarity.EPIC : def.tier() >= 4 ? Rarity.RARE : def.tier() >= 3 ? Rarity.UNCOMMON : Rarity.COMMON).fireResistant());
        this.def = def;
    }

    public static Tier tier(int t) {
        return switch (t) {
            case 0 -> Tiers.WOOD;
            case 1 -> Tiers.STONE;
            case 2 -> Tiers.IRON;
            case 3 -> Tiers.DIAMOND;
            case 4 -> Tiers.NETHERITE;
            default -> MYTHIC;
        };
    }

    public int skillCooldown() { return SwordSkills.cooldown(def.skill()); }

    @Override
    public Component getName(ItemStack stack) {
        ChatFormatting c = def.tier() >= 5 ? ChatFormatting.LIGHT_PURPLE : def.tier() >= 4 ? ChatFormatting.GOLD : def.tier() >= 3 ? ChatFormatting.AQUA : ChatFormatting.WHITE;
        return Component.literal(def.name()).withStyle(c);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Pasif: §f" + SwordSkills.passiveName(def.passive())));
        tip.add(Component.literal("§7Yetenek (sağ tık): §e" + SwordSkills.skillName(def.skill())));
        tip.add(Component.literal("§8" + SwordSkills.skillDesc(def.skill())));
        tip.add(Component.literal("§7Bekleme: §f" + skillCooldown() / 20 + " sn"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            PlayerRpg data = RpgWorldData.player(sp);
            float power = data.meleePower() * (1.0F + def.tier() * 0.15F);
            SwordSkills.use(sp, this, def.skill(), power);
            player.getCooldowns().addCooldown(this, skillCooldown());
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
        }
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker.level().isClientSide()) return r;
        SwordPassive p = def.passive();
        float bonus = 0;
        switch (p) {
            case BURN -> target.setSecondsOnFire(4);
            case FREEZE -> { Combat.effect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 1); target.setTicksFrozen(Math.min(target.getTicksFrozen() + 60, 240)); }
            case SHOCK -> { if (attacker.getRandom().nextInt(4) == 0) { Combat.effect(target, RpgEffects.STUN.get(), 20, 0); FX.send(target.level(), ParticleTypes.ELECTRIC_SPARK, target.position().add(0, 1, 0), 15, 0.4, 0.1); } }
            case POISON -> Combat.effect(target, MobEffects.POISON, 80, 1);
            case LIFESTEAL -> attacker.heal(2.0F);
            case WITHER -> Combat.effect(target, MobEffects.WITHER, 60, 1);
            case BLEED -> Combat.effect(target, RpgEffects.BLEED.get(), 100, 0);
            case KNOCKUP -> { target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0)); target.hurtMarked = true; }
            case HOLY -> { if (target.getMobType() == MobType.UNDEAD) bonus = 6; Combat.effect(target, MobEffects.GLOWING, 60, 0); }
            case EXECUTE -> { if (target.getHealth() < target.getMaxHealth() * 0.25F) bonus = 8; }
            case CRIT -> { if (attacker.getRandom().nextInt(4) == 0) { bonus = 5; FX.send(target.level(), ParticleTypes.CRIT, target.position().add(0, 1, 0), 15, 0.4, 0.2); } }
            case MANA_STEAL -> { if (attacker instanceof ServerPlayer sp) { PlayerRpg d = RpgWorldData.player(sp); d.mana = Math.min(d.maxMana(), d.mana + 3); } }
            case SLOW -> Combat.effect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
            case BLIND -> { if (attacker.getRandom().nextInt(3) == 0) Combat.effect(target, MobEffects.BLINDNESS, 40, 0); }
            case WEAKEN -> Combat.effect(target, MobEffects.WEAKNESS, 100, 1);
            case GIANT_SLAYER -> { if (target.getBbHeight() > 2.6F || target.getMaxHealth() > 60) bonus = 6; }
            case DEMON_SLAYER -> { if (target.fireImmune()) bonus = 7; }
            case UNDEAD_SLAYER -> { if (target.getMobType() == MobType.UNDEAD) bonus = 8; }
            case DRAGON_SLAYER -> { if (target instanceof RpgMonster m && (m.def().part(com.krolasyon.bosses.rpg.def.RpgDefs.P_WINGS) || m.isBoss())) bonus = 10; }
            default -> {}
        }
        if (bonus > 0) {
            target.invulnerableTime = 0;
            Combat.magic(attacker, null, target, bonus);
        }
        return r;
    }
}
