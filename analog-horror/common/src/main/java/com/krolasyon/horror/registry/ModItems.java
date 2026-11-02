package com.krolasyon.horror.registry;

import com.krolasyon.horror.HorrorMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REG = DeferredRegister.create(ForgeRegistries.ITEMS, HorrorMod.MODID);

    public static final RegistryObject<Item> HATMAN_SPAWN_EGG = REG.register("hatman_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HATMAN, 0x17120F, 0xB89C6A, new Item.Properties()));

    // story items (index matches the story_item block's "item" property)
    public static final String[] STORY_IDS = {"tape_birthday", "tape_party", "master_tape", "fuse", "stair_key", "attic_key", "lighter", "eviction_notice"};
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] STORY = new RegistryObject[STORY_IDS.length];

    static {
        for (int i = 0; i < STORY_IDS.length; i++) STORY[i] = REG.register(STORY_IDS[i], () -> new Item(new Item.Properties().stacksTo(16)));
    }

    public static final int TAPE_BIRTHDAY = 0, TAPE_PARTY = 1, MASTER_TAPE = 2, FUSE = 3, STAIR_KEY = 4, ATTIC_KEY = 5, LIGHTER = 6, EVICTION_NOTICE = 7;

    private ModItems() {}
}
