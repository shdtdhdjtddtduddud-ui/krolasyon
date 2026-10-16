package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.item.LauncherItem;
import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.item.RivalLibraItem;
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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LibraBeyblade.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LibraBeyblade.MODID);

    public static final RegistryObject<Item> LIBRA = ITEMS.register("flame_libra",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> LAUNCHER = ITEMS.register("libra_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), LIBRA, ModEntities.LIBRA));
    public static final RegistryObject<Item> RIVAL = ITEMS.register("rival_libra",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.LIBRA));

    public static final RegistryObject<Item> LDRAGO = ITEMS.register("ldrago_destruction",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "ldrago"));
    public static final RegistryObject<Item> LDRAGO_LAUNCHER = ITEMS.register("ldrago_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), LDRAGO, ModEntities.LDRAGO));
    public static final RegistryObject<Item> RIVAL_LDRAGO = ITEMS.register("rival_ldrago",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.LDRAGO));

    public static final RegistryObject<Item> LEONE = ITEMS.register("rock_leone",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "leone"));
    public static final RegistryObject<Item> LEONE_LAUNCHER = ITEMS.register("leone_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), LEONE, ModEntities.LEONE));
    public static final RegistryObject<Item> RIVAL_LEONE = ITEMS.register("rival_leone",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.LEONE));

    public static final RegistryObject<Item> EAGLE = ITEMS.register("earth_eagle",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "eagle"));
    public static final RegistryObject<Item> EAGLE_LAUNCHER = ITEMS.register("eagle_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), EAGLE, ModEntities.EAGLE));
    public static final RegistryObject<Item> RIVAL_EAGLE = ITEMS.register("rival_eagle",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.EAGLE));

    public static final RegistryObject<Item> PEGASUS = ITEMS.register("storm_pegasus",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "pegasus"));
    public static final RegistryObject<Item> PEGASUS_LAUNCHER = ITEMS.register("pegasus_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), PEGASUS, ModEntities.PEGASUS));
    public static final RegistryObject<Item> RIVAL_PEGASUS = ITEMS.register("rival_pegasus",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.PEGASUS));

    public static final RegistryObject<Item> TEMPO = ITEMS.register("twisted_tempo",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "tempo"));
    public static final RegistryObject<Item> TEMPO_LAUNCHER = ITEMS.register("tempo_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), TEMPO, ModEntities.TEMPO));
    public static final RegistryObject<Item> RIVAL_TEMPO = ITEMS.register("rival_tempo",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.TEMPO));

    public static final RegistryObject<Item> VARIARES = ITEMS.register("variares",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "variares"));
    public static final RegistryObject<Item> VARIARES_LAUNCHER = ITEMS.register("variares_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), VARIARES, ModEntities.VARIARES));
    public static final RegistryObject<Item> RIVAL_VARIARES = ITEMS.register("rival_variares",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.VARIARES));

    public static final RegistryObject<Item> BEFFAL = ITEMS.register("evil_beffal",
            () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "beffal"));
    public static final RegistryObject<Item> BEFFAL_LAUNCHER = ITEMS.register("beffal_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), BEFFAL, ModEntities.BEFFAL));
    public static final RegistryObject<Item> RIVAL_BEFFAL = ITEMS.register("rival_beffal",
            () -> new RivalLibraItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.BEFFAL));

    /** the ten ordinary beyblades, in {@link com.krolasyon.beyblade.entity.CommonBeybladeEntity#VARIANTS} order */
    public static final java.util.List<RegistryObject<Item>> COMMONS = new java.util.ArrayList<>();

    static {
        for (int i = 0; i < com.krolasyon.beyblade.entity.CommonBeybladeEntity.VARIANTS.length; i++) {
            final int v = i;
            COMMONS.add(ITEMS.register("common_" + com.krolasyon.beyblade.entity.CommonBeybladeEntity.VARIANTS[i].id(),
                    () -> new LibraItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), "common_" + v)));
        }
    }

    public static final RegistryObject<Item> STANDARD_LAUNCHER = ITEMS.register("standard_launcher",
            () -> new LauncherItem(new Item.Properties().stacksTo(1), null, null));

    public static final RegistryObject<Item> STADIUM = ITEMS.register("stadium",
            () -> new com.krolasyon.beyblade.item.StadiumItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("beyblade", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.librabeyblade"))
            .icon(() -> new ItemStack(LIBRA.get()))
            .displayItems((params, output) -> {
                output.accept(STANDARD_LAUNCHER.get());
                for (RegistryObject<Item> c : COMMONS) output.accept(c.get());
                output.accept(LIBRA.get());
                output.accept(LAUNCHER.get());
                output.accept(RIVAL.get());
                output.accept(LDRAGO.get());
                output.accept(LDRAGO_LAUNCHER.get());
                output.accept(RIVAL_LDRAGO.get());
                output.accept(LEONE.get());
                output.accept(LEONE_LAUNCHER.get());
                output.accept(RIVAL_LEONE.get());
                output.accept(EAGLE.get());
                output.accept(EAGLE_LAUNCHER.get());
                output.accept(RIVAL_EAGLE.get());
                output.accept(PEGASUS.get());
                output.accept(PEGASUS_LAUNCHER.get());
                output.accept(RIVAL_PEGASUS.get());
                output.accept(TEMPO.get());
                output.accept(TEMPO_LAUNCHER.get());
                output.accept(RIVAL_TEMPO.get());
                output.accept(VARIARES.get());
                output.accept(VARIARES_LAUNCHER.get());
                output.accept(RIVAL_VARIARES.get());
                output.accept(BEFFAL.get());
                output.accept(BEFFAL_LAUNCHER.get());
                output.accept(RIVAL_BEFFAL.get());
                output.accept(STADIUM.get());
            })
            .build());
}
