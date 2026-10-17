package com.krolasyon.mirror;

import com.krolasyon.mirror.block.MirrorBlock;
import com.krolasyon.mirror.block.MirrorBlockEntity;
import com.krolasyon.mirror.entity.ReflectionEntity;
import com.krolasyon.mirror.world.MergeManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MirrorWorld.MODID)
public class MirrorWorld {
    public static final String MODID = "mirrorworld";

    /** The world on the other side of the glass: same seed and generator as the overworld. */
    public static final ResourceKey<Level> MIRROR_DIM = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MODID, "mirror"));

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<MirrorBlock> MIRROR = BLOCKS.register("magic_mirror", () -> new MirrorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5F, 6.0F).sound(SoundType.GLASS).noOcclusion()
            .pushReaction(PushReaction.BLOCK).lightLevel(s -> 4)));
    public static final RegistryObject<Item> MIRROR_ITEM = ITEMS.register("magic_mirror",
            () -> new DoubleHighBlockItem(MIRROR.get(), new Item.Properties().stacksTo(16)));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MirrorBlockEntity>> MIRROR_BE = BLOCK_ENTITIES.register("magic_mirror",
            () -> BlockEntityType.Builder.of(MirrorBlockEntity::new, MIRROR.get()).build(null));
    public static final RegistryObject<EntityType<ReflectionEntity>> REFLECTION = ENTITIES.register("reflection",
            () -> EntityType.Builder.<ReflectionEntity>of(ReflectionEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build(MODID + ":reflection"));

    public MirrorWorld() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::onAttributes);
        bus.addListener(this::onTabs);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(MergeManager.class);
        if (Boolean.getBoolean("mirrorworld.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.mirror.client.AutoTest::init);
        }
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(REFLECTION.get(), ReflectionEntity.createAttributes().build());
    }

    private void onTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(MIRROR_ITEM);
    }
}
