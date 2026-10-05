package com.krolasyon.sololeveling.item;

import com.krolasyon.sololeveling.SoloLeveling;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Armor sets with full-set bonuses and custom 3D models. */
public class SLArmorItem extends ArmorItem {
    public enum Mat implements ArmorMaterial {
        HUNTER(28, new int[]{3, 6, 7, 3}, 1F, 0F, 12),
        HIGH_ORC(34, new int[]{3, 7, 8, 3}, 2F, 0.05F, 10),
        CRIMSON_KNIGHT(40, new int[]{4, 8, 9, 4}, 3.5F, 0.1F, 15),
        SHADOW_MONARCH(55, new int[]{5, 9, 10, 5}, 4.5F, 0.15F, 25);

        private final int durability;
        private final int[] prot; // boots, legs, chest, helmet
        private final float toughness, knockback;
        private final int ench;

        Mat(int durability, int[] prot, float toughness, float knockback, int ench) {
            this.durability = durability;
            this.prot = prot;
            this.toughness = toughness;
            this.knockback = knockback;
            this.ench = ench;
        }

        @Override public int getDurabilityForType(Type t) { return durability * new int[]{11, 16, 15, 13}[t.ordinal()]; }

        @Override public int getDefenseForType(Type t) {
            return switch (t) {
                case HELMET -> prot[3];
                case CHESTPLATE -> prot[2];
                case LEGGINGS -> prot[1];
                case BOOTS -> prot[0];
            };
        }

        @Override public int getEnchantmentValue() { return ench; }
        @Override public SoundEvent getEquipSound() { return this == HIGH_ORC ? SoundEvents.ARMOR_EQUIP_LEATHER : SoundEvents.ARMOR_EQUIP_NETHERITE; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(com.krolasyon.sololeveling.registry.ModItems.MAGIC_STONE_B.get()); }
        @Override public String getName() { return SoloLeveling.MODID + ":" + name().toLowerCase(Locale.ROOT); }
        @Override public float getToughness() { return toughness; }
        @Override public float getKnockbackResistance() { return knockback; }
    }

    public final Mat mat;

    public SLArmorItem(Mat mat, Type type, Rarity rarity) {
        super(mat, type, new Properties().rarity(rarity).fireResistant());
        this.mat = mat;
    }

    public static boolean fullSet(Player p, Mat m) {
        for (ItemStack s : p.getArmorSlots()) if (!(s.getItem() instanceof SLArmorItem a) || a.mat != m) return false;
        return true;
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player p) {
        if (level.isClientSide || type != Type.CHESTPLATE || p.tickCount % 40 != 0 || !fullSet(p, mat)) return;
        switch (mat) {
            case HUNTER -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            case HIGH_ORC -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            case CRIMSON_KNIGHT -> {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            }
            case SHADOW_MONARCH -> {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false, true));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("sololeveling.armor." + mat.name().toLowerCase(Locale.ROOT) + ".set").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String n = mat.name().toLowerCase(Locale.ROOT);
        return SoloLeveling.MODID + ":textures/models/armor/" + n + (slot == EquipmentSlot.LEGS ? "_layer_2" : "_layer_1") + ".png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.sololeveling.client.ClientHooks.armorExtensions(mat));
    }
}
