package com.krolasyon.beyblade.world;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;

/** Starter kit on first join, and beyblades hidden in vanilla chests (the legendary ones only rarely). */
@Mod.EventBusSubscriber(modid = LibraBeyblade.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModEvents {
    private ModEvents() {}

    private static final String STARTER = LibraBeyblade.MODID + ":starter_given";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player p = event.getEntity();
        if (p.level().isClientSide()) return;
        CompoundTag root = p.getPersistentData();
        CompoundTag keep = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (keep.getBoolean(STARTER)) return;
        keep.putBoolean(STARTER, true);
        root.put(Player.PERSISTED_NBT_TAG, keep);
        int v = Math.floorMod(p.getUUID().hashCode(), ModItems.COMMONS.size());
        ItemStack bey = new ItemStack(ModItems.COMMONS.get(v).get());
        ItemStack launcher = new ItemStack(ModItems.STANDARD_LAUNCHER.get());
        if (!p.getInventory().add(launcher)) p.drop(launcher, false);
        if (!p.getInventory().add(bey)) p.drop(bey, false);
        p.sendSystemMessage(Component.translatable("msg.librabeyblade.starter", bey.getHoverName()).withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("msg.librabeyblade.starter2").withStyle(ChatFormatting.GRAY));
    }

    private static final Set<String> SPECIAL_TABLES = Set.of("chests/ancient_city", "chests/end_city_treasure", "chests/bastion_treasure",
            "chests/buried_treasure", "chests/woodland_mansion", "chests/stronghold_corridor", "chests/desert_pyramid", "chests/jungle_temple",
            "chests/simple_dungeon", "chests/abandoned_mineshaft", "chests/shipwreck_treasure");
    private static final Set<String> COMMON_TABLES = Set.of("chests/simple_dungeon", "chests/abandoned_mineshaft", "chests/village/village_weaponsmith",
            "chests/village/village_toolsmith", "chests/village/village_plains_house", "chests/village/village_desert_house",
            "chests/village/village_taiga_house", "chests/village/village_savanna_house", "chests/village/village_snowy_house",
            "chests/pillager_outpost", "chests/shipwreck_supply", "chests/desert_pyramid", "chests/jungle_temple", "chests/stronghold_corridor");

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        if (!id.getNamespace().equals("minecraft")) return;
        String path = id.getPath();
        if (SPECIAL_TABLES.contains(path)) {
            // about a 5% chance (15% in the hardest structures) of one legendary beyblade
            int empty = path.contains("ancient_city") || path.contains("end_city") || path.contains("bastion") ? 28 : 95;
            LootPool.Builder b = LootPool.lootPool().name("librabeyblade_legendary").setRolls(ConstantValue.exactly(1));
            for (Item it : List.of(ModItems.LIBRA.get(), ModItems.LDRAGO.get(), ModItems.LEONE.get(), ModItems.EAGLE.get(), ModItems.PEGASUS.get(), ModItems.TEMPO.get()))
                b.add(LootItem.lootTableItem(it).setWeight(1));
            b.add(EmptyLootItem.emptyItem().setWeight(empty));
            event.getTable().addPool(b.build());
        }
        if (COMMON_TABLES.contains(path)) {
            LootPool.Builder b = LootPool.lootPool().name("librabeyblade_common").setRolls(ConstantValue.exactly(1));
            for (RegistryObject<Item> it : ModItems.COMMONS) b.add(LootItem.lootTableItem(it.get()).setWeight(2));
            b.add(LootItem.lootTableItem(ModItems.STANDARD_LAUNCHER.get()).setWeight(4));
            b.add(EmptyLootItem.emptyItem().setWeight(60));
            event.getTable().addPool(b.build());
        }
    }
}
