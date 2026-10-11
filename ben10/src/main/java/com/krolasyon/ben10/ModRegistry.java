package com.krolasyon.ben10;

import com.krolasyon.ben10.block.TempBlock;
import com.krolasyon.ben10.entity.AlienProjectile;
import com.krolasyon.ben10.entity.DebrisEntity;
import com.krolasyon.ben10.entity.FxEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Ben10.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Ben10.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Ben10.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Ben10.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ben10.MODID);

    public static final RegistryObject<Item> OMNITRIX = ITEMS.register("omnitrix",
            () -> new OmnitrixItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Block> CRYSTAL = BLOCKS.register("crystal_wall", () -> new TempBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.EMERALD).strength(1.5F, 6F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 6).noLootTable(), 200, true));
    public static final RegistryObject<Block> FROST = BLOCKS.register("frost_ice", () -> new TempBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.ICE).strength(0.6F, 1F).sound(SoundType.GLASS).noOcclusion().friction(0.98F).noLootTable(), 140, false));

    public static final RegistryObject<EntityType<AlienProjectile>> PROJECTILE = ENTITIES.register("alien_projectile",
            () -> EntityType.Builder.<AlienProjectile>of(AlienProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("alien_projectile"));
    public static final RegistryObject<EntityType<DebrisEntity>> DEBRIS = ENTITIES.register("debris",
            () -> EntityType.Builder.<DebrisEntity>of(DebrisEntity::new, MobCategory.MISC).sized(0.4F, 0.4F)
                    .clientTrackingRange(6).updateInterval(2).fireImmune().build("debris"));
    public static final RegistryObject<EntityType<FxEntity>> FX = ENTITIES.register("fx",
            () -> EntityType.Builder.<FxEntity>of(FxEntity::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("fx"));

    public static final RegistryObject<SoundEvent> S_TRANSFORM = sound("transform");
    public static final RegistryObject<SoundEvent> S_DETRANSFORM = sound("detransform");
    public static final RegistryObject<SoundEvent> S_TIMEOUT = sound("timeout");
    public static final RegistryObject<SoundEvent> S_BEEP = sound("beep");
    public static final RegistryObject<SoundEvent> S_DIAL = sound("dial");
    public static final RegistryObject<SoundEvent> S_DIAL_OPEN = sound("dial_open");
    public static final RegistryObject<SoundEvent> S_SONIC = sound("sonic");
    public static final RegistryObject<SoundEvent> S_WHOOSH = sound("whoosh");

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolben10"))
            .icon(() -> new ItemStack(OMNITRIX.get()))
            .displayItems((params, out) -> out.accept(OMNITRIX.get()))
            .build());

    private ModRegistry() {}

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Ben10.MODID, name)));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
    }
}
