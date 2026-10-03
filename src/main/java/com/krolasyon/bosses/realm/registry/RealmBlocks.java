package com.krolasyon.bosses.realm.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.block.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class RealmBlocks {
    private RealmBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, KrolasyonBosses.MODID);
    /** blocks that get a plain BlockItem */
    public static final List<RegistryObject<? extends Block>> WITH_ITEM = new ArrayList<>();

    public static final TagKey<Block> REALM_SOIL = TagKey.create(Registries.BLOCK, Realm.rl("realm_soil"));

    private static <T extends Block> RegistryObject<T> reg(String name, Supplier<T> s) {
        RegistryObject<T> r = BLOCKS.register(name, s);
        WITH_ITEM.add(r);
        return r;
    }

    private static BlockBehaviour.Properties stone(MapColor c, float hard) {
        return BlockBehaviour.Properties.of().mapColor(c).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(hard, 6.0F);
    }

    private static BlockBehaviour.Properties soil(MapColor c, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(0.6F).sound(s);
    }

    // terrain
    public static final RegistryObject<Block> INFERNAL_STONE = reg("infernal_stone", () -> new Block(stone(MapColor.NETHER, 1.6F).sound(SoundType.NETHERRACK)));
    public static final RegistryObject<Block> INFERNAL_BRICKS = reg("infernal_bricks", () -> new Block(stone(MapColor.NETHER, 2.2F).sound(SoundType.NETHER_BRICKS)));
    public static final RegistryObject<Block> CHISELED_INFERNAL_BRICKS = reg("chiseled_infernal_bricks", () -> new Block(stone(MapColor.NETHER, 2.2F).sound(SoundType.NETHER_BRICKS)));
    public static final RegistryObject<Block> ASH_SOIL = reg("ash_soil", () -> new Block(soil(MapColor.COLOR_GRAY, SoundType.SAND)));
    public static final RegistryObject<Block> BLOOD_MOSS = reg("blood_moss", () -> new Block(soil(MapColor.CRIMSON_NYLIUM, SoundType.NYLIUM)));
    public static final RegistryObject<Block> SHADOW_TURF = reg("shadow_turf", () -> new Block(soil(MapColor.COLOR_PURPLE, SoundType.NYLIUM)));
    public static final RegistryObject<Block> SCORCHED_EARTH = reg("scorched_earth", () -> new Block(soil(MapColor.COLOR_BLACK, SoundType.BASALT)
            .lightLevel(s -> 3).emissiveRendering((s, l, p) -> true)));
    public static final RegistryObject<Block> CRIMSON_SAND = reg("crimson_sand", () -> new SandBlock(0x8A2A20, soil(MapColor.COLOR_RED, SoundType.SAND).strength(0.5F)));
    public static final RegistryObject<Block> COAGULATED_BLOOD = reg("coagulated_blood", () -> new Block(soil(MapColor.COLOR_RED, SoundType.HONEY_BLOCK)
            .speedFactor(0.45F).jumpFactor(0.6F)));

    // portal
    public static final RegistryObject<Block> CURSED_OBSIDIAN = reg("cursed_obsidian", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(35.0F, 1200.0F).lightLevel(s -> 5)));
    public static final RegistryObject<Block> REALM_PORTAL = BLOCKS.register("realm_portal", () -> new RealmPortalBlock(BlockBehaviour.Properties.of()
            .noCollission().strength(-1.0F).sound(SoundType.GLASS).lightLevel(s -> 12).noLootTable().pushReaction(PushReaction.BLOCK)));

    // ores & metal
    public static final RegistryObject<Block> INFERNAL_STEEL_ORE = reg("infernal_steel_ore", () -> new DropExperienceBlock(stone(MapColor.NETHER, 3.0F)
            .sound(SoundType.NETHER_GOLD_ORE), UniformInt.of(1, 3)));
    public static final RegistryObject<Block> ARCANE_CRYSTAL_ORE = reg("arcane_crystal_ore", () -> new DropExperienceBlock(stone(MapColor.COLOR_MAGENTA, 3.0F)
            .sound(SoundType.AMETHYST).lightLevel(s -> 7), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> INFERNAL_STEEL_BLOCK = reg("infernal_steel_block", () -> new Block(stone(MapColor.COLOR_RED, 5.0F).sound(SoundType.NETHERITE_BLOCK)));

    // trees
    public static final RegistryObject<RotatedPillarBlock> BLOOD_STEM = reg("blood_stem", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.CRIMSON_STEM).strength(2.0F).sound(SoundType.STEM)));
    public static final RegistryObject<Block> BLOOD_CAP = reg("blood_cap", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.WART_BLOCK)));
    public static final RegistryObject<Block> BLOOD_PLANKS = reg("blood_planks", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.CRIMSON_STEM).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD)));
    public static final RegistryObject<RotatedPillarBlock> SHADOW_STEM = reg("shadow_stem", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(2.0F).sound(SoundType.STEM)));
    public static final RegistryObject<Block> SHADOW_CRYSTAL = reg("shadow_crystal", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE).strength(1.2F).sound(SoundType.AMETHYST).lightLevel(s -> 12).noOcclusion()));
    public static final RegistryObject<Block> SHADOW_PLANKS = reg("shadow_planks", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD)));

    // flora
    public static final RegistryObject<Block> EMBER_FLOWER = reg("ember_flower", () -> new RealmPlantBlock(plant().lightLevel(s -> 10),
            () -> ParticleTypes.SMALL_FLAME, false));
    public static final RegistryObject<Block> SOUL_LILY = reg("soul_lily", () -> new RealmPlantBlock(plant().lightLevel(s -> 9),
            () -> ParticleTypes.SOUL, false));
    public static final RegistryObject<Block> BLOOD_THORN = reg("blood_thorn", () -> new RealmPlantBlock(plant().lightLevel(s -> 3),
            () -> ParticleTypes.CRIMSON_SPORE, true));
    public static final RegistryObject<Block> SHADOW_FERN = reg("shadow_fern", () -> new RealmPlantBlock(plant().lightLevel(s -> 6),
            () -> ParticleTypes.PORTAL, false));

    // decor & story
    public static final RegistryObject<Block> INFERNAL_LANTERN = reg("infernal_lantern", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_ORANGE).strength(0.8F).sound(SoundType.SHROOMLIGHT).lightLevel(s -> 15)));
    public static final RegistryObject<Block> LORD_ALTAR = reg("lord_altar", () -> new LordAltarBlock(stone(MapColor.NETHER, 50.0F)
            .strength(50.0F, 1200.0F).lightLevel(s -> 10).noOcclusion()));
    public static final RegistryObject<Block> CRIMSON_THRONE = reg("crimson_throne", () -> new CrimsonThroneBlock(stone(MapColor.COLOR_RED, 50.0F)
            .strength(50.0F, 1200.0F).lightLevel(s -> 8).noOcclusion()));

    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.ROOTS)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }
}
