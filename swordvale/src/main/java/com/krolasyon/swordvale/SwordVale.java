package com.krolasyon.swordvale;

import com.krolasyon.swordvale.item.ValeCompassItem;
import com.krolasyon.swordvale.world.SwordValeFeature;
import com.krolasyon.swordvale.world.ValeLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SwordVale.MODID)
public final class SwordVale {
    public static final String MODID = "swordvale";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);

    public static final RegistryObject<Block> RUNE_STONE = BLOCKS.register("rune_stone", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).strength(1.8F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 6)));
    public static final RegistryObject<Item> RUNE_STONE_ITEM = ITEMS.register("rune_stone", () -> new BlockItem(RUNE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RUNE_SHARD = ITEMS.register("rune_shard", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> VALE_COMPASS = ITEMS.register("vale_compass",
            () -> new ValeCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SWORD_VALE =
            FEATURES.register("sword_vale", () -> new SwordValeFeature(NoneFeatureConfiguration.CODEC));

    public SwordVale() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener(this::creativeTabs);
        MinecraftForge.EVENT_BUS.addListener(SwordVale::announce);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener(SwordVale::clientSetup);
            if (Boolean.getBoolean("swordvale.autotest")) com.krolasyon.swordvale.test.AutoTest.init();
        }
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(VALE_COMPASS);
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(RUNE_SHARD);
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(RUNE_STONE_ITEM);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(VALE_COMPASS.get(), new ResourceLocation("angle"),
                new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) -> {
                    CompoundTag t = stack.getTag();
                    if (t == null || !t.contains("ValeX") || level.dimension() != Level.OVERWORLD) return null;
                    return GlobalPos.of(Level.OVERWORLD, new BlockPos(t.getInt("ValeX"), 64, t.getInt("ValeZ")));
                })));
    }

    /** Oyuncu bir vadiye girince ekranda başlık gösterir. */
    private static void announce(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        if (!(p.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        ValeLayout.Vale v = ValeLayout.nearest(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(),
                p.getBlockX(), p.getBlockZ(), 1);
        boolean inside = v != null && ValeLayout.influence(v, p.getBlockX(), p.getBlockZ()) > 0.05;
        CompoundTag data = p.getPersistentData();
        boolean was = data.getBoolean("swordvale_inside");
        if (inside && !was) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 70, 25));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.swordvale.sub").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.swordvale.vale").withStyle(ChatFormatting.GREEN)));
            level.playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 0.8F, 0.6F);
        }
        data.putBoolean("swordvale_inside", inside);
    }
}
