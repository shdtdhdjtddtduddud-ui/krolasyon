package com.krolasyon.sololeveling.registry;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.block.NewsBoardBlock;
import com.krolasyon.sololeveling.block.RegionPortalBlock;
import com.krolasyon.sololeveling.block.RegionPortalBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> REG = DeferredRegister.create(ForgeRegistries.BLOCKS, SoloLeveling.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SoloLeveling.MODID);
    public static final List<RegistryObject<? extends Item>> ITEMS = new ArrayList<>();
    public static final List<RegistryObject<Block>> SIMPLE = new ArrayList<>();

    // city
    public static final RegistryObject<Block> ASPHALT = simple("asphalt", MapColor.COLOR_GRAY, 1.5F, SoundType.STONE, 0);
    public static final RegistryObject<Block> ROAD_LINE = simple("road_line", MapColor.COLOR_YELLOW, 1.5F, SoundType.STONE, 0);
    public static final RegistryObject<Block> ROAD_CROSSING = simple("road_crossing", MapColor.SNOW, 1.5F, SoundType.STONE, 0);
    public static final RegistryObject<Block> SIDEWALK = simple("sidewalk", MapColor.STONE, 1.5F, SoundType.STONE, 0);
    public static final RegistryObject<Block> CONCRETE_PANEL = simple("concrete_panel", MapColor.QUARTZ, 2F, SoundType.STONE, 0);
    public static final RegistryObject<Block> DARK_PANEL = simple("dark_panel", MapColor.COLOR_BLACK, 2F, SoundType.STONE, 0);
    public static final RegistryObject<Block> GLASS_FACADE = simple("glass_facade", MapColor.COLOR_LIGHT_BLUE, 1F, SoundType.GLASS, 0);
    public static final RegistryObject<Block> OFFICE_WINDOW = simple("office_window", MapColor.COLOR_YELLOW, 1F, SoundType.GLASS, 13);
    public static final RegistryObject<Block> DARK_WINDOW = simple("dark_window", MapColor.COLOR_BLUE, 1F, SoundType.GLASS, 0);
    public static final RegistryObject<Block> NEON_BLUE = simple("neon_blue", MapColor.COLOR_LIGHT_BLUE, 0.8F, SoundType.GLASS, 15);
    public static final RegistryObject<Block> NEON_RED = simple("neon_red", MapColor.COLOR_RED, 0.8F, SoundType.GLASS, 15);
    public static final RegistryObject<Block> NEON_PURPLE = simple("neon_purple", MapColor.COLOR_PURPLE, 0.8F, SoundType.GLASS, 15);
    public static final RegistryObject<Block> NEON_GOLD = simple("neon_gold", MapColor.GOLD, 0.8F, SoundType.GLASS, 15);
    public static final RegistryObject<Block> ASSOCIATION_EMBLEM = simple("association_emblem", MapColor.COLOR_BLUE, 2F, SoundType.METAL, 10);
    public static final RegistryObject<Block> EMBLEM_HUNTERS = simple("emblem_hunters", MapColor.GOLD, 2F, SoundType.METAL, 10);
    public static final RegistryObject<Block> EMBLEM_WHITE_TIGER = simple("emblem_white_tiger", MapColor.SNOW, 2F, SoundType.METAL, 10);
    public static final RegistryObject<Block> EMBLEM_FIEND = simple("emblem_fiend", MapColor.COLOR_RED, 2F, SoundType.METAL, 10);
    public static final RegistryObject<Block> EMBLEM_KNIGHTS = simple("emblem_knights", MapColor.COLOR_BLUE, 2F, SoundType.METAL, 10);
    public static final RegistryObject<Block> EMBLEM_AHJIN = simple("emblem_ahjin", MapColor.COLOR_PURPLE, 2F, SoundType.METAL, 10);
    // dungeons
    public static final RegistryObject<Block> DUNGEON_BRICKS = simple("dungeon_bricks", MapColor.DEEPSLATE, 4F, SoundType.DEEPSLATE_BRICKS, 0);
    public static final RegistryObject<Block> RUNE_BRICKS = simple("rune_bricks", MapColor.COLOR_CYAN, 4F, SoundType.DEEPSLATE_BRICKS, 8);
    public static final RegistryObject<Block> DUNGEON_FLOOR = simple("dungeon_floor", MapColor.DEEPSLATE, 4F, SoundType.DEEPSLATE_TILES, 0);
    public static final RegistryObject<Block> TEMPLE_STONE = simple("temple_stone", MapColor.SAND, 4F, SoundType.STONE, 0);
    public static final RegistryObject<Block> TEMPLE_PILLAR = simple("temple_pillar", MapColor.SAND, 4F, SoundType.STONE, 0);
    public static final RegistryObject<Block> DEMON_BRICKS = simple("demon_bricks", MapColor.NETHER, 4F, SoundType.NETHER_BRICKS, 0);
    public static final RegistryObject<Block> HIVE_WALL = simple("hive_wall", MapColor.TERRACOTTA_BROWN, 3F, SoundType.MUD_BRICKS, 0);
    public static final RegistryObject<Block> MANA_CRYSTAL_ORE = simple("mana_crystal_ore", MapColor.COLOR_LIGHT_BLUE, 5F, SoundType.AMETHYST, 9);

    public static final RegistryObject<Block> REGION_PORTAL = register("region_portal", () -> new RegionPortalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE).noCollission().strength(-1F, 3600000F).lightLevel(s -> 15).sound(SoundType.GLASS).noOcclusion().noLootTable()));
    public static final RegistryObject<Block> NEWS_BOARD = register("news_board", () -> new NewsBoardBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(2F).lightLevel(s -> 10).sound(SoundType.METAL).noOcclusion()));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<RegionPortalBlockEntity>> REGION_PORTAL_BE = BE.register("region_portal",
            () -> BlockEntityType.Builder.of(RegionPortalBlockEntity::new, REGION_PORTAL.get()).build(null));

    private ModBlocks() {}

    private static RegistryObject<Block> simple(String id, MapColor color, float strength, SoundType sound, int light) {
        RegistryObject<Block> o = register(id, () -> new Block(BlockBehaviour.Properties.of().mapColor(color).strength(strength, strength * 3)
                .sound(sound).lightLevel(s -> light).requiresCorrectToolForDrops()));
        SIMPLE.add(o);
        return o;
    }

    private static <T extends Block> RegistryObject<T> register(String id, Supplier<T> s) {
        RegistryObject<T> o = REG.register(id, s);
        ITEMS.add(ModItems.REG.register(id, () -> new BlockItem(o.get(), new Item.Properties())));
        return o;
    }
}
