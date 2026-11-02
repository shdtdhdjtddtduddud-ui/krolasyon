package com.krolasyon.horror.registry;

import com.krolasyon.horror.HorrorMod;
import com.krolasyon.horror.block.CounterBlock;
import com.krolasyon.horror.block.CrtTvBlock;
import com.krolasyon.horror.block.LampBlock;
import com.krolasyon.horror.block.PropBlock;
import com.krolasyon.horror.block.DecorBlock;
import com.krolasyon.horror.block.FacingDecorBlock;
import com.krolasyon.horror.block.SconceBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Furniture and finishes for the haunted house the story takes place in. */
public final class ModBlocks {
    public static final DeferredRegister<Block> REG = DeferredRegister.create(ForgeRegistries.BLOCKS, HorrorMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HorrorMod.MODID);
    public static final List<RegistryObject<Item>> ITEMS = new ArrayList<>();

    private static BlockBehaviour.Properties wall() { return BlockBehaviour.Properties.of().strength(1.2F).sound(SoundType.WOOD); }

    private static BlockBehaviour.Properties soft() { return BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.WOOL); }

    private static BlockBehaviour.Properties prop() { return BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOD).noOcclusion(); }

    private static final double[][] CARPET = {{0, 0, 0, 16, 1, 16}};

    public static final RegistryObject<Block> WALLPAPER_FLORAL = reg("wallpaper_floral", () -> new Block(wall()));
    public static final RegistryObject<Block> WALLPAPER_FLORAL_STAINED = reg("wallpaper_floral_stained", () -> new Block(wall()));
    public static final RegistryObject<Block> WALL_CREAM = reg("wall_cream", () -> new Block(wall()));
    public static final RegistryObject<Block> WALL_OLIVE = reg("wall_olive", () -> new Block(wall()));
    public static final RegistryObject<Block> CEILING_PLASTER = reg("ceiling_plaster", () -> new Block(wall()));

    public static final RegistryObject<Block> CARPET_BROWN_DIRTY = reg("carpet_brown_dirty", () -> new DecorBlock(soft(), Block.box(0, 0, 0, 16, 1, 16)));
    public static final RegistryObject<Block> CARPET_BEIGE = reg("carpet_beige", () -> new DecorBlock(soft(), Block.box(0, 0, 0, 16, 1, 16)));
    public static final RegistryObject<Block> RUG_PERSIAN = reg("rug_persian", () -> new DecorBlock(soft(), Block.box(0, 0, 0, 16, 1, 16)));
    public static final RegistryObject<Block> RUG_PERSIAN_EDGE = reg("rug_persian_edge", () -> new FacingDecorBlock(soft(), false, CARPET));
    public static final RegistryObject<Block> RUNNER_RUG = reg("runner_rug", () -> new FacingDecorBlock(soft(), false, CARPET));

    public static final RegistryObject<Block> CRT_TV = reg("crt_tv", () -> new CrtTvBlock(prop().lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 7 : 0),
            new double[][]{{0, 0, 0, 16, 7, 16}, {1, 7, 3, 15, 19, 14}}));
    public static final RegistryObject<Block> ARMCHAIR = reg("armchair", () -> new FacingDecorBlock(prop().sound(SoundType.WOOL), false,
            new double[][]{{1, 0, 1, 15, 8, 15}, {1, 8, 12, 15, 20, 15.5}, {0, 3, 1, 2.5, 12, 14}, {13.5, 3, 1, 16, 12, 14}}));
    public static final RegistryObject<Block> COFFEE_TABLE = reg("coffee_table", () -> new DecorBlock(prop(), Block.box(1, 0, 1, 15, 10, 15)));
    public static final RegistryObject<Block> PICTURE_SKETCH = reg("picture_sketch", () -> new FacingDecorBlock(prop().noCollission(), true, new double[][]{{2, 2, 15, 14, 14, 16}}, null));
    public static final RegistryObject<Block> PICTURE_PORTRAIT = reg("picture_portrait", () -> new FacingDecorBlock(prop().noCollission(), true, new double[][]{{3, 1, 15, 13, 15, 16}}, null));
    public static final RegistryObject<Block> PICTURE_LANDSCAPE = reg("picture_landscape", () -> new FacingDecorBlock(prop().noCollission(), true, new double[][]{{1, 4, 15, 15, 13, 16}}, null));
    public static final RegistryObject<Block> CANDLE_SCONCE = reg("candle_sconce", () -> new SconceBlock(prop().noCollission().lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 10 : 0).sound(SoundType.METAL),
            new double[][]{{6, 3, 9, 10, 14, 16}}));
    public static final RegistryObject<Block> CEILING_LAMP = reg("ceiling_lamp", () -> new LampBlock(prop().noCollission().lightLevel(s -> s.getValue(LampBlock.LIT) ? 9 : 0).sound(SoundType.GLASS),
            Block.box(4.5, 11, 4.5, 11.5, 16, 11.5)));
    public static final RegistryObject<Block> BULB_FIXTURE = reg("bulb_fixture", () -> new LampBlock(prop().noCollission().lightLevel(s -> s.getValue(LampBlock.LIT) ? 6 : 0).sound(SoundType.GLASS),
            Block.box(6, 10, 6, 10, 16, 10)));
    public static final RegistryObject<Block> BALLOON_WHITE = reg("balloon_white", () -> new DecorBlock(soft().noOcclusion().noCollission(), Block.box(3, 4, 3, 13, 16, 13), null));
    public static final RegistryObject<Block> BALLOON_GOLD = reg("balloon_gold", () -> new DecorBlock(soft().noOcclusion().noCollission(), Block.box(3, 4, 3, 13, 16, 13), null));
    public static final RegistryObject<Block> BANISTER = reg("banister", () -> new FacingDecorBlock(prop(), false,
            new double[][]{{0, 0, 6, 16, 15, 10}}, new double[][]{{0, 0, 6, 16, 24, 10}}));
    public static final RegistryObject<Block> BANISTER_POST = reg("banister_post", () -> new DecorBlock(prop(), Block.box(4, 0, 4, 12, 17, 12), Block.box(4, 0, 4, 12, 24, 12)));

    // ---- story props
    public static final RegistryObject<Block> NOTE_PAPER = reg("note_paper", () -> PropBlock.notes(soft().noOcclusion().noCollission().sound(SoundType.WOOL)));
    public static final RegistryObject<Block> STORY_ITEM = reg("story_item", () -> PropBlock.items(soft().noOcclusion().noCollission()));
    public static final RegistryObject<Block> VCR = reg("vcr", () -> new FacingDecorBlock(prop(), false, new double[][]{{1, 0, 3, 15, 4, 13}}));
    public static final RegistryObject<Block> FUSE_BOX = reg("fuse_box", () -> CounterBlock.fuseBox(prop().sound(SoundType.METAL)));
    public static final RegistryObject<Block> DIAL = reg("dial", () -> CounterBlock.dial(prop().sound(SoundType.METAL)));
    public static final RegistryObject<Block> STRAW_HAT = reg("straw_hat", () -> new DecorBlock(soft().noOcclusion(), Block.box(0, 0, 0, 16, 4, 16), null));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("house", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.krolasyonhorror.house"))
            .icon(() -> new ItemStack(CRT_TV.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.HATMAN_SPAWN_EGG.get());
                for (RegistryObject<Item> i : ITEMS) out.accept(i.get());
            }).build());

    private static RegistryObject<Block> reg(String name, Supplier<Block> sup) {
        RegistryObject<Block> b = REG.register(name, sup);
        ITEMS.add(ModItems.REG.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    public static boolean isHouseFloor(Block b) {
        return b == CARPET_BROWN_DIRTY.get() || b == CARPET_BEIGE.get() || b == RUG_PERSIAN.get() || b == RUG_PERSIAN_EDGE.get()
                || b == RUNNER_RUG.get();
    }

    private ModBlocks() {}
}
