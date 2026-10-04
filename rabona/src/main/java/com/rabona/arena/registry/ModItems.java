package com.rabona.arena.registry;

import com.rabona.arena.RabonaArena;
import com.rabona.arena.game.Team;
import com.rabona.arena.item.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RabonaArena.MODID);

    /** Top desenleri: klasik, altin, alev, neon, galaksi, gece yarisi. */
    public static final String[] BALL_SKINS = {"classic", "gold", "inferno", "neon", "galaxy", "rabona"};
    public static final List<RegistryObject<Item>> BALLS = new ArrayList<>();

    static {
        for (int i = 0; i < BALL_SKINS.length; i++) {
            final int skin = i;
            BALLS.add(ITEMS.register("ball_" + BALL_SKINS[i], () -> new BallItem(new Item.Properties().stacksTo(16)
                    .rarity(skin == 0 ? Rarity.COMMON : skin < 3 ? Rarity.UNCOMMON : Rarity.EPIC), skin)));
        }
    }

    public static final RegistryObject<Item> STADIUM_BUILDER = ITEMS.register("stadium_builder",
            () -> new StadiumBuilderItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WHISTLE = ITEMS.register("whistle",
            () -> new WhistleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BOT_CARD_RED = ITEMS.register("bot_card_red",
            () -> new BotCardItem(new Item.Properties().stacksTo(16), Team.RED));
    public static final RegistryObject<Item> BOT_CARD_BLUE = ITEMS.register("bot_card_blue",
            () -> new BotCardItem(new Item.Properties().stacksTo(16), Team.BLUE));
    public static final RegistryObject<Item> CLEATS = ITEMS.register("golden_cleats",
            () -> new CleatsItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC)));

    static void blockItem(String name, RegistryObject<Block> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
