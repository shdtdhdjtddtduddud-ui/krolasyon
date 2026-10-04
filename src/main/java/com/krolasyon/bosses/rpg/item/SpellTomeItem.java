package com.krolasyon.bosses.rpg.item;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.util.FX;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A book that teaches one spell (stored in NBT "Spell"). */
public class SpellTomeItem extends Item {
    public SpellTomeItem(Properties p) { super(p); }

    public static ItemStack of(SpellDef s) {
        ItemStack st = new ItemStack(RpgItems.SPELL_TOME.get());
        st.getOrCreateTag().putString("Spell", s.id());
        return st;
    }

    @Nullable
    public static SpellDef spell(ItemStack st) {
        return st.hasTag() ? RpgDefs.SPELL_BY_ID.get(st.getTag().getString("Spell")) : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        SpellDef s = spell(stack);
        return Component.literal(s == null ? "Boş Büyü Kitabı" : "Büyü Kitabı: " + s.name());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        SpellDef s = spell(stack);
        if (s == null) return Rarity.COMMON;
        return s.tier() >= 5 ? Rarity.EPIC : s.tier() >= 4 ? Rarity.RARE : s.tier() >= 2 ? Rarity.UNCOMMON : Rarity.COMMON;
    }

    @Override
    public boolean isFoil(ItemStack stack) { SpellDef s = spell(stack); return s != null && s.tier() >= 4; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        SpellDef s = spell(stack);
        if (s == null) return;
        tip.add(Component.literal("§7Okul: §f" + RpgDefs.schoolName(s.school()) + " §7| Tür: §f" + PlayerMagic.shapeName(s.shape())));
        tip.add(Component.literal("§7Güç: §f" + (int) s.power() + " §7Mana: §b" + s.mana() + " §7Bekleme: §f" + String.format("%.1f", s.cooldown() / 20F) + " sn"));
        tip.add(Component.literal("§7Gereken seviye: §e" + s.requiredLevel()));
        tip.add(Component.literal("§8Öğrenmek için sağ tıkla."));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        SpellDef s = spell(st);
        if (s == null) return InteractionResultHolder.pass(st);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            PlayerRpg d = RpgWorldData.player(sp);
            if (d.spells.contains(s.id())) {
                sp.displayClientMessage(Component.literal("§eBu büyüyü zaten biliyorsun."), true);
                return InteractionResultHolder.fail(st);
            }
            if (d.level < s.requiredLevel()) {
                sp.displayClientMessage(Component.literal("§cBu büyüyü öğrenmek için " + s.requiredLevel() + ". seviye olmalısın."), true);
                return InteractionResultHolder.fail(st);
            }
            d.spells.add(s.id());
            d.selected = d.spells.size() - 1;
            if (!player.getAbilities().instabuild) st.shrink(1);
            FX.sound(sp, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
            FX.spiral(level, ParticleTypes.ENCHANT, sp.position(), 2.5, 1, 3, 60);
            FX.spiral(level, FX.dust(s.color(), 1.2F), sp.position(), 2.5, 0.8, 3, 40);
            sp.displayClientMessage(Component.literal("§d✦ Yeni büyü öğrendin: §f" + s.name() + " §7(R ile kullan, Z/X ile değiştir)"), false);
            PlayerMagic.sync(sp);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide());
    }
}
