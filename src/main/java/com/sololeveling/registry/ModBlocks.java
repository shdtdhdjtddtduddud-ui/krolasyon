package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import com.sololeveling.block.BoardBlock;
import com.sololeveling.block.TeleportPadBlock;
import com.sololeveling.gen.Content;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SoloLeveling.MODID);
    public static final Map<String, RegistryObject<Block>> BY_ID = new LinkedHashMap<>();

    static {
        for (Content.BlockDef d : Content.BLOCKS) {
            BlockBehaviour.Properties p = BlockBehaviour.Properties.of()
                    .strength(d.hardness(), 18.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(s -> d.light());
            switch (d.kind()) {
                case "pillar" -> BY_ID.put(d.id(), BLOCKS.register(d.id(), () -> new RotatedPillarBlock(p)));
                case "pad" -> BY_ID.put(d.id(), BLOCKS.register(d.id(), () -> new TeleportPadBlock(p.noOcclusion())));
                case "board" -> BY_ID.put(d.id(), BLOCKS.register(d.id(), () -> new BoardBlock(p.noOcclusion().sound(SoundType.WOOD), d.id())));
                default -> BY_ID.put(d.id(), BLOCKS.register(d.id(), () -> new Block(p)));
            }
        }
    }

    public static Block get(String id) { return BY_ID.get(id).get(); }
}
