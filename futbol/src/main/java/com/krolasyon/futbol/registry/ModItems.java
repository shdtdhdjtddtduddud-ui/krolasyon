package com.krolasyon.futbol.registry;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.item.BotEggItem;
import com.krolasyon.futbol.item.FootballItem;
import com.krolasyon.futbol.item.PitchBuilderItem;
import com.krolasyon.futbol.item.WhistleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FutbolMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FutbolMod.MODID);

    public static final RegistryObject<Item> FOOTBALL = ITEMS.register("football", () -> new FootballItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> PITCH_BUILDER = ITEMS.register("pitch_builder", () -> new PitchBuilderItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> WHISTLE = ITEMS.register("whistle", () -> new WhistleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BOT_RED = ITEMS.register("bot_red", () -> new BotEggItem(Team.RED, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BOT_BLUE = ITEMS.register("bot_blue", () -> new BotEggItem(Team.BLUE, new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("futbol", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonfutbol"))
            .icon(() -> new ItemStack(FOOTBALL.get()))
            .displayItems((params, output) -> {
                output.accept(FOOTBALL.get());
                output.accept(PITCH_BUILDER.get());
                output.accept(WHISTLE.get());
                output.accept(BOT_RED.get());
                output.accept(BOT_BLUE.get());
                ModBlocks.BLOCKS.getEntries().forEach(b -> output.accept(b.get()));
            })
            .build());
}
