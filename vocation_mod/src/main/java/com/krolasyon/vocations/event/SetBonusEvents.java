package com.krolasyon.vocations.event;

import com.krolasyon.vocations.VocationsMod;
import com.krolasyon.vocations.item.VocationArmorMaterial;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Wearing all four pieces of one vocation keeps that vocation's set effect active. */
@Mod.EventBusSubscriber(modid = VocationsMod.MODID)
public final class SetBonusEvents {
    private static final int REFRESH_INTERVAL = 40;
    private static final int EFFECT_DURATION = 60;

    private SetBonusEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level.isClientSide || player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }
        VocationArmorMaterial set = fullSet(player);
        if (set != null) {
            player.addEffect(new MobEffectInstance(set.getSetBonus(), EFFECT_DURATION, 0, true, false));
        }
    }

    private static VocationArmorMaterial fullSet(Player player) {
        VocationArmorMaterial set = null;
        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armor)) {
                return null;
            }
            if (!(armor.getMaterial() instanceof VocationArmorMaterial material)) {
                return null;
            }
            if (set != null && set != material) {
                return null;
            }
            set = material;
        }
        return set;
    }
}
