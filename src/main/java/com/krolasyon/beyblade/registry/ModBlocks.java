package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.LibraBeyblade;
import com.krolasyon.beyblade.block.StadiumBarrierBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LibraBeyblade.MODID);

    public static final RegistryObject<Block> STADIUM_BARRIER = BLOCKS.register("stadium_barrier",
            () -> new StadiumBarrierBlock(BlockBehaviour.Properties.of().strength(-1F, 3600000F).noLootTable().noOcclusion()
                    .isValidSpawn((s, l, p, t) -> false).pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> WORKBENCH = BLOCKS.register("bey_workbench",
            () -> new com.krolasyon.beyblade.workbench.WorkbenchBlock(BlockBehaviour.Properties.of().strength(2.5F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)));
}
