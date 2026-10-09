package com.krolasyon.vocations.event;

import com.krolasyon.vocations.VocationsMod;
import com.krolasyon.vocations.item.AccessoryData;
import com.krolasyon.vocations.item.AccessoryItem;
import com.krolasyon.vocations.registry.VocationItems;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/** Keeps accessory attribute bonuses in sync with what is worn, and handles taking them off. */
@Mod.EventBusSubscriber(modid = VocationsMod.MODID)
public final class AccessoryEvents {
    private AccessoryEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level.isClientSide || player.tickCount % 10 != 0) {
            return;
        }
        for (RegistryObject<AccessoryItem> object : VocationItems.ACCESSORIES) {
            AccessoryItem item = object.get();
            boolean worn = AccessoryData.get(player, item.getSlot()).is(item);
            AttributeInstance instance = player.getAttribute(item.getAttribute());
            if (instance == null) {
                continue;
            }
            if (worn) {
                if (instance.getModifier(item.modifierId()) == null) {
                    instance.addTransientModifier(item.createModifier());
                }
            } else {
                instance.removeModifier(item.modifierId());
            }
        }
    }

    /** Sneak + right-click with an empty hand returns every worn accessory to the inventory. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getLevel().isClientSide || !player.isShiftKeyDown() || !event.getItemStack().isEmpty()) {
            return;
        }
        AccessoryData.returnAll(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            AccessoryData.copyOnDeath(event.getOriginal(), event.getEntity());
        }
    }
}
