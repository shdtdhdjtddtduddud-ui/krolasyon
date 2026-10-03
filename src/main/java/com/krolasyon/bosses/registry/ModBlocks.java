package com.krolasyon.bosses.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.block.HellPortalBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, KrolasyonBosses.MODID);

    public static final Map<String, RegistryObject<Block>> ALL = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();

    static {
        for (Object[] row : BlockTable.ROWS) {
            String id = (String) row[0];
            float hardness = (Float) row[1], resistance = (Float) row[2];
            int light = (Integer) row[3];
            RegistryObject<Block> b = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of()
                    .strength(hardness, resistance).requiresCorrectToolForDrops().lightLevel(s -> light)
                    .sound(id.contains("lamp") ? SoundType.GLASS : (id.equals("hellgate_stone") ? SoundType.DEEPSLATE : (hardness < 1.2F ? SoundType.SAND : SoundType.STONE)))));
            ALL.put(id, b);
            ITEMS.put(id, ModItems.ITEMS.register(id, () -> new BlockItem(b.get(), new Item.Properties().rarity(id.equals("hellgate_stone") ? Rarity.RARE : Rarity.COMMON))));
        }
    }

    public static final RegistryObject<Block> HELL_PORTAL = BLOCKS.register("hell_portal", () -> new HellPortalBlock(BlockBehaviour.Properties.of()
            .noCollission().strength(-1.0F).sound(SoundType.GLASS).lightLevel(s -> 13).noLootTable()));

    public static Block get(String id) { return ALL.get(id).get(); }

    public static RegistryObject<Block> reg(String id) { return ALL.get(id); }
}
