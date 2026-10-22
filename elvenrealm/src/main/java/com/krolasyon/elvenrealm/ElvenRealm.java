package com.krolasyon.elvenrealm;

import com.krolasyon.elvenrealm.dialogue.DialogueManager;
import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.krolasyon.elvenrealm.entity.ElfKind;
import com.krolasyon.elvenrealm.item.SilverLeafItem;
import com.krolasyon.elvenrealm.network.Net;
import com.krolasyon.elvenrealm.world.CityLayout;
import com.krolasyon.elvenrealm.world.ElvenCityFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

@Mod(ElvenRealm.MODID)
public final class ElvenRealm {
    public static final String MODID = "elvenrealm";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);

    public static final Map<ElfKind, RegistryObject<EntityType<ElfEntity>>> TYPES = new EnumMap<>(ElfKind.class);
    public static final Map<ElfKind, RegistryObject<Item>> EGGS = new EnumMap<>(ElfKind.class);

    public static final RegistryObject<Item> SILVER_LEAF = ITEMS.register("silver_leaf",
            () -> new SilverLeafItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ELVEN_CITY =
            FEATURES.register("elven_city", () -> new ElvenCityFeature(NoneFeatureConfiguration.CODEC));

    static {
        int[][] colors = {{0x4F8A3A, 0xE8C46A}, {0x1C1C22, 0xC9CBD6}, {0x3F6B3A, 0xECE8DC}, {0x6B4A35, 0xF2F0EA}, {0x4C7A3E, 0xB8BCC2},
                {0x3D5F33, 0x6B5034}, {0x245A34, 0xE0B84A}, {0x2E5A33, 0xD9B45A},
                {0x7A5C3E, 0xD8C8A0}, {0x5A6E3A, 0x9C7A4E}, {0x2A2F5C, 0xC8D2F0}, {0xF2C6D6, 0x6E9A4A}};
        for (ElfKind k : ElfKind.values()) {
            RegistryObject<EntityType<ElfEntity>> t = ENTITIES.register("elf_" + k.id, () -> EntityType.Builder
                    .<ElfEntity>of((type, level) -> new ElfEntity(type, level, k), MobCategory.MISC)
                    .sized(0.6F * k.scale, 1.8F * k.scale).clientTrackingRange(10).build("elf_" + k.id));
            TYPES.put(k, t);
            int[] c = colors[k.ordinal()];
            EGGS.put(k, ITEMS.register("elf_" + k.id + "_spawn_egg", () -> new ForgeSpawnEggItem(t, c[0], c[1], new Item.Properties())));
        }
    }

    public ElvenRealm() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::creativeTabs);
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(ElvenRealm::announce);
        MinecraftForge.EVENT_BUS.addListener(ElvenRealm::onDeath);
        MinecraftForge.EVENT_BUS.addListener(ElvenRealm::onAttack);
        MinecraftForge.EVENT_BUS.addListener(ElvenRealm::onJoin);
        MinecraftForge.EVENT_BUS.addListener(ElvenRealm::noMonsters);
        MinecraftForge.EVENT_BUS.addListener(ElvenRealm::commands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener(ElvenRealm::clientSetup);
            com.krolasyon.elvenrealm.client.ClientRegistry.init(bus);
            if (Boolean.getBoolean("elvenrealm.autotest")) com.krolasyon.elvenrealm.test.AutoTest.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        for (ElfKind k : ElfKind.values()) event.put(TYPES.get(k).get(), ElfEntity.attributes(k).build());
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) for (ElfKind k : ElfKind.values()) event.accept(EGGS.get(k));
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(SILVER_LEAF);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(SILVER_LEAF.get(), new ResourceLocation("angle"),
                new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) -> {
                    CompoundTag t = stack.getTag();
                    if (t == null || !t.contains("CityX") || level.dimension() != Level.OVERWORLD) return null;
                    return GlobalPos.of(Level.OVERWORLD, new BlockPos(t.getInt("CityX"), 64, t.getInt("CityZ")));
                })));
    }

    /** Av görevleri: oyuncunun ya da kiralık elfinin öldürdükleri. */
    private static void onDeath(LivingDeathEvent event) {
        Entity k = event.getSource().getEntity();
        if (k instanceof ServerPlayer p) DialogueManager.onKill(p, event.getEntity().getType());
        else if (k instanceof ElfEntity elf && elf.owner() instanceof ServerPlayer p) DialogueManager.onKill(p, event.getEntity().getType());
    }

    /** Elfler dostlarını yaralamaz (oklar ve süpürme darbeleri dahil). */
    private static void onAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ElfEntity)) return;
        Entity t = event.getEntity();
        if (t instanceof Player || t instanceof ElfEntity || t instanceof AbstractVillager || t instanceof IronGolem) event.setCanceled(true);
    }

    /** Kent canavarları elfleri de hedef alır: kent savunması gerçek bir savaş olur. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity e = event.getEntity();
        if (e instanceof Zombie z && !(e instanceof ZombifiedPiglin)) z.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(z, ElfEntity.class, true));
        else if (e instanceof AbstractSkeleton s) s.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(s, ElfEntity.class, true));
        else if (e instanceof AbstractIllager i) i.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(i, ElfEntity.class, true));
    }

    private static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("elfgorev").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            p.sendSystemMessage(Component.literal("— Sylvaren Günlüğü —").withStyle(ChatFormatting.GOLD));
            for (Component c : DialogueManager.journal(p)) p.sendSystemMessage(c);
            return 1;
        }));
    }

    /** Oyuncu Sylvaren'e girince ekranda başlık. */
    /** Kentin içinde canavar doğmaz (doğal doğum, devriye, takviye); spawner ve yumurtalar etkilenmez. */
    private static void noMonsters(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)) return;
        switch (event.getSpawnType()) {
            case NATURAL, CHUNK_GENERATION, PATROL, REINFORCEMENT, JOCKEY, EVENT -> {}
            default -> { return; }
        }
        ServerLevel level = event.getLevel().getLevel();
        if (level.dimension() != Level.OVERWORLD) return;
        int x = (int) Math.floor(event.getX()), z = (int) Math.floor(event.getZ());
        CityLayout.City c = CityLayout.nearest(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, x, z, 1);
        if (c == null) return;
        int dx = x - c.cx(), dz = z - c.cz();
        if (Math.abs(dx) <= CityLayout.MAX_X + CityLayout.BLEND + 16 && dz >= CityLayout.MIN_Z - CityLayout.BLEND - 16 && dz <= CityLayout.MAX_Z + 40)
            event.setSpawnCancelled(true);
    }

    private static void announce(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        if (!(p.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        CityLayout.City c = CityLayout.nearest(level.getSeed(), level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level,
                p.getBlockX(), p.getBlockZ(), 1);
        boolean inside = c != null && CityLayout.inside(c, p.getBlockX(), p.getBlockZ());
        CompoundTag data = p.getPersistentData();
        boolean was = data.getBoolean("elvenrealm_inside");
        if (inside && !was) {
            com.krolasyon.elvenrealm.world.CityCensus.ensure(level, c);
            var type = CityLayout.type(level.getSeed(), c, level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level);
            ChatFormatting color = switch (type) {
                case MOON -> ChatFormatting.AQUA;
                case BLOSSOM -> ChatFormatting.LIGHT_PURPLE;
                case COMMON -> ChatFormatting.GREEN;
                default -> ChatFormatting.GOLD;
            };
            p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 80, 25));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(type.subtitle).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(type.name(c.seed())).withStyle(color)));
            level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1F, 0.7F);
        }
        data.putBoolean("elvenrealm_inside", inside);
    }
}
