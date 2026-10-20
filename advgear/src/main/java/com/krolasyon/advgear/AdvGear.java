package com.krolasyon.advgear;

import com.krolasyon.advgear.block.LockType;
import com.krolasyon.advgear.block.LockedChestBlock;
import com.krolasyon.advgear.entity.PoisonFlaskEntity;
import com.krolasyon.advgear.gear.BackpackMenu;
import com.krolasyon.advgear.gear.GearData;
import com.krolasyon.advgear.gear.GearItem;
import com.krolasyon.advgear.gear.GearMenu;
import com.krolasyon.advgear.gear.GearType;
import com.krolasyon.advgear.item.AddItemModifier;
import com.krolasyon.advgear.item.BackpackItem;
import com.krolasyon.advgear.item.CrystalKeyItem;
import com.krolasyon.advgear.item.GogglesItem;
import com.krolasyon.advgear.item.KeyItem;
import com.krolasyon.advgear.item.KeyRingItem;
import com.krolasyon.advgear.item.LockpickItem;
import com.krolasyon.advgear.item.MechanicKeyItem;
import com.krolasyon.advgear.item.PoisonFlaskItem;
import com.krolasyon.advgear.item.SpellbookItem;
import com.krolasyon.advgear.item.TonicItem;
import com.krolasyon.advgear.network.Net;
import com.krolasyon.advgear.world.LockedChestFeature;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mod(AdvGear.MODID)
public final class AdvGear {
    public static final String MODID = "advgear";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    private static RegistryObject<Item> item(String id, Supplier<Item> s) {
        RegistryObject<Item> r = ITEMS.register(id, s);
        ALL.add(r);
        return r;
    }

    private static Item.Properties p(Rarity r) {
        return new Item.Properties().rarity(r);
    }

    // yüzükler
    public static final RegistryObject<Item> GOLD_BAND = item("gold_band", () -> new GearItem(GearType.RING, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SAPPHIRE_RING = item("sapphire_ring", () -> new GearItem(GearType.RING, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SKULL_RING = item("skull_ring", () -> new GearItem(GearType.RING, p(Rarity.RARE)));
    public static final RegistryObject<Item> BONE_HAND_RING = item("bone_hand_ring", () -> new GearItem(GearType.RING, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EMERALD_EYE_RING = item("emerald_eye_ring", () -> new GearItem(GearType.RING, p(Rarity.RARE)));
    // gözlükler
    public static final RegistryObject<Item> EXPLORER_GOGGLES = item("explorer_goggles", () -> new GogglesItem(0, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ENGINEER_GOGGLES = item("engineer_goggles", () -> new GogglesItem(1, p(Rarity.RARE)));
    public static final RegistryObject<Item> HUNTER_GOGGLES = item("hunter_goggles", () -> new GogglesItem(2, p(Rarity.RARE)));
    // çantalar
    public static final RegistryObject<Item> SATCHEL = item("satchel", () -> new BackpackItem(2, 0, p(Rarity.COMMON)));
    public static final RegistryObject<Item> TRAVELER_PACK = item("traveler_pack", () -> new BackpackItem(3, 1, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EXPEDITION_PACK = item("expedition_pack", () -> new BackpackItem(4, 2, p(Rarity.RARE)));
    // iksirler
    public static final RegistryObject<Item> HEART_TONIC = item("heart_tonic", () -> new TonicItem(p(Rarity.UNCOMMON), 8F, () -> ParticleTypes.HEART,
            () -> List.of(new MobEffectInstance(MobEffects.REGENERATION, 200, 1))));
    public static final RegistryObject<Item> OCEAN_DRAUGHT = item("ocean_draught", () -> new TonicItem(p(Rarity.UNCOMMON), 0F, () -> ParticleTypes.BUBBLE_POP,
            () -> List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 3600), new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 3600),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 3600))));
    public static final RegistryObject<Item> BLACK_DEATH = item("black_death", () -> new PoisonFlaskItem(p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> STARLIGHT_ELIXIR = item("starlight_elixir", () -> new TonicItem(p(Rarity.RARE), 0F, () -> ParticleTypes.END_ROD,
            () -> List.of(new MobEffectInstance(MobEffects.SLOW_FALLING, 3600), new MobEffectInstance(MobEffects.JUMP, 3600, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600))));
    public static final RegistryObject<Item> SUN_ESSENCE = item("sun_essence", () -> new TonicItem(p(Rarity.RARE), 0F, () -> ParticleTypes.FLAME,
            () -> List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4800), new MobEffectInstance(MobEffects.DIG_SPEED, 4800, 1),
                    new MobEffectInstance(MobEffects.SATURATION, 20, 2))));
    // büyü kitapları
    public static final RegistryObject<Item> APPRENTICE_SPELLBOOK = item("apprentice_spellbook",
            () -> new SpellbookItem(SpellbookItem.Spell.FIREBOLT, 0, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TRAVELER_TOME = item("traveler_tome", () -> new SpellbookItem(SpellbookItem.Spell.BLINK, 1, p(Rarity.RARE)));
    public static final RegistryObject<Item> BLACK_GRIMOIRE = item("black_grimoire", () -> new SpellbookItem(SpellbookItem.Spell.STORM, 2, p(Rarity.EPIC)));
    // kemerler
    public static final RegistryObject<Item> TRAVELER_BELT = item("traveler_belt", () -> new GearItem(GearType.BELT, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TOOL_BELT = item("tool_belt", () -> new GearItem(GearType.BELT, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ALCHEMIST_BELT = item("alchemist_belt", () -> new GearItem(GearType.BELT, p(Rarity.RARE)));
    // anahtarlar
    public static final RegistryObject<Item> IRON_KEY = item("iron_key", () -> new KeyItem(LockType.IRON, p(Rarity.COMMON)));
    public static final RegistryObject<Item> DUNGEON_KEY = item("dungeon_key", () -> new KeyItem(LockType.BONE, p(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> HEART_KEY = item("heart_key", () -> new KeyItem(LockType.HEART, p(Rarity.RARE)));
    public static final RegistryObject<Item> MECHANIC_KEY = item("mechanic_key", () -> new MechanicKeyItem(p(Rarity.COMMON)));
    public static final RegistryObject<Item> THORN_LOCKPICK = item("thorn_lockpick", () -> new LockpickItem(p(Rarity.COMMON)));
    public static final RegistryObject<Item> CRYSTAL_KEY = item("crystal_key", () -> new CrystalKeyItem(p(Rarity.EPIC)));
    public static final RegistryObject<Item> KEY_RING = item("key_ring", () -> new KeyRingItem(p(Rarity.COMMON)));

    public static final RegistryObject<Block> LOCKED_CHEST = BLOCKS.register("locked_chest", () -> new LockedChestBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(40F, 1200F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> LOCKED_CHEST_ITEM = item("locked_chest", () -> new BlockItem(LOCKED_CHEST.get(), p(Rarity.UNCOMMON)));

    public static final RegistryObject<EntityType<PoisonFlaskEntity>> POISON_FLASK_ENTITY = ENTITIES.register("black_death",
            () -> EntityType.Builder.<PoisonFlaskEntity>of(PoisonFlaskEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build("black_death"));

    public static final RegistryObject<MenuType<GearMenu>> GEAR_MENU = MENUS.register("gear", () -> new MenuType<>(GearMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK_2 = MENUS.register("backpack_2",
            () -> new MenuType<>((id, inv) -> new BackpackMenu(backpackMenu(2), id, inv, 2), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK_3 = MENUS.register("backpack_3",
            () -> new MenuType<>((id, inv) -> new BackpackMenu(backpackMenu(3), id, inv, 3), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK_4 = MENUS.register("backpack_4",
            () -> new MenuType<>((id, inv) -> new BackpackMenu(backpackMenu(4), id, inv, 4), FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LOCKED_CHEST_FEATURE =
            FEATURES.register("locked_chest", () -> new LockedChestFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.advgear")).icon(() -> new ItemStack(EXPEDITION_PACK.get()))
            .displayItems((params, out) -> ALL.forEach(r -> out.accept(r.get()))).build());

    public static MenuType<BackpackMenu> backpackMenu(int rows) {
        return rows <= 2 ? BACKPACK_2.get() : rows == 3 ? BACKPACK_3.get() : BACKPACK_4.get();
    }

    public AdvGear() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        FEATURES.register(bus);
        TABS.register(bus);
        LOOT_MODIFIERS.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Net::register));
        IEventBus f = MinecraftForge.EVENT_BUS;
        f.addListener(AdvGear::tick);
        f.addListener(AdvGear::onHurt);
        f.addListener(AdvGear::onXp);
        f.addListener(AdvGear::breakSpeed);
        f.addListener(AdvGear::effectAdded);
        f.addListener(AdvGear::drops);
        f.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) Net.syncGear(sp);
        });
        f.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> GearData.forget(e.getEntity()));
        f.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) Net.syncGear(sp);
        });
        f.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) Net.syncGear(sp);
        });
        f.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getEntity() instanceof ServerPlayer sp && e.getTarget() instanceof Player other) Net.sendTo(sp, other);
        });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.advgear.client.ClientSetup.init(bus);
            if (Boolean.getBoolean("advgear.autotest")) com.krolasyon.advgear.test.AutoTest.init();
        }
    }

    public static void openGear(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GearMenu(id, inv, GearData.get(pl)), Component.translatable("gui.advgear.gear")));
    }

    // ------------------------------------------------------------------ etkiler

    private static final UUID LUCK_ID = UUID.fromString("8b6f0c3e-5d1a-4c4f-9a61-1f3a2b7d9e01");
    private static final UUID SPEED_ID = UUID.fromString("8b6f0c3e-5d1a-4c4f-9a61-1f3a2b7d9e02");

    private static void modifier(Player p, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id, String name, double amount,
                                 AttributeModifier.Operation op, boolean want) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (want && !has) inst.addTransientModifier(new AttributeModifier(id, name, amount, op));
        else if (!want && has) inst.removeModifier(id);
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer p)) return;
        if (p.isSpectator()) return;
        // Kemik El Yüzüğü: eşya ve deneyim mıknatısı
        if (p.tickCount % 2 == 0 && !p.isShiftKeyDown() && GearData.has(p, BONE_HAND_RING.get())) {
            Vec3 c = p.position().add(0, 0.8, 0);
            for (ItemEntity it : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(7))) {
                if (it.hasPickUpDelay()) continue;
                Vec3 d = c.subtract(it.position());
                it.setDeltaMovement(it.getDeltaMovement().scale(0.6).add(d.normalize().scale(0.18)));
            }
            for (ExperienceOrb orb : p.level().getEntitiesOfClass(ExperienceOrb.class, p.getBoundingBox().inflate(7))) {
                orb.setDeltaMovement(c.subtract(orb.position()).normalize().scale(0.3));
            }
        }
        if (p.tickCount % 10 != 0) return;
        modifier(p, Attributes.LUCK, LUCK_ID, "advgear gold band", 1, AttributeModifier.Operation.ADDITION, GearData.has(p, GOLD_BAND.get()));
        modifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "advgear traveler belt", 0.12, AttributeModifier.Operation.MULTIPLY_BASE,
                GearData.has(p, TRAVELER_BELT.get()));
        if (GearData.has(p, SAPPHIRE_RING.get()) && p.isInWater()) {
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
        }
        if (GearData.has(p, EMERALD_EYE_RING.get())) {
            if (p.hasEffect(MobEffects.POISON)) p.removeEffect(MobEffects.POISON);
            if (p.tickCount % 100 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1F);
        }
        if (GearData.has(p, EXPLORER_GOGGLES.get())) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, true, false, true));
        if (p.tickCount % 20 == 0 && GearData.has(p, HUNTER_GOGGLES.get())) {
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(28), e -> e instanceof Enemy)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
            }
        }
    }

    /** Kurukafa Yüzüğü: yakın dövüş hasarının %15'i kadar can çalar. */
    private static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof ServerPlayer p && GearData.has(p, SKULL_RING.get())) {
            p.heal(event.getAmount() * 0.15F);
            ((net.minecraft.server.level.ServerLevel) p.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, event.getEntity().getX(),
                    event.getEntity().getY(0.6), event.getEntity().getZ(), 3, 0.2, 0.2, 0.2, 0.1);
        }
    }

    /** Altın Bant: deneyim küreleri %30 fazla verir. */
    private static void onXp(PlayerXpEvent.PickupXp event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide && GearData.has(p, GOLD_BAND.get())) {
            ExperienceOrb orb = event.getOrb();
            orb.value = Math.max(orb.value + 1, Math.round(orb.value * 1.3F));
        }
    }

    /** Alet Kemeri: kazma/kesme %25 hızlı. */
    private static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        boolean has = p.level().isClientSide ? com.krolasyon.advgear.client.ClientGear.get(p, 3).is(TOOL_BELT.get()) : GearData.has(p, TOOL_BELT.get());
        if (has) event.setNewSpeed(event.getNewSpeed() * 1.25F);
    }

    private static final ThreadLocal<Boolean> EXTENDING = ThreadLocal.withInitial(() -> false);

    /** Simyacı Kemeri: içilen olumlu iksirler %50 uzun sürer. */
    private static void effectAdded(MobEffectEvent.Added event) {
        if (EXTENDING.get() || !(event.getEntity() instanceof ServerPlayer p)) return;
        MobEffectInstance m = event.getEffectInstance();
        if (m.isAmbient() || !m.getEffect().isBeneficial() || m.getDuration() < 100 || m.isInfiniteDuration()) return;
        if (!GearData.has(p, ALCHEMIST_BELT.get())) return;
        MobEffectInstance longer = new MobEffectInstance(m.getEffect(), (int) (m.getDuration() * 1.5F), m.getAmplifier(), false, m.isVisible(), m.showIcon());
        p.server.execute(() -> {
            EXTENDING.set(true);
            try {
                p.addEffect(longer);
            } finally {
                EXTENDING.set(false);
            }
        });
    }

    /** Ölünce kuşanım da düşer (envanteri koru açık değilse). */
    private static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        GearData.Gear g = GearData.get(p);
        for (int i = 0; i < g.getContainerSize(); i++) {
            ItemStack s = g.getItem(i);
            if (s.isEmpty()) continue;
            ItemEntity it = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), s.copy());
            it.setDefaultPickUpDelay();
            event.getDrops().add(it);
            g.setItem(i, ItemStack.EMPTY);
        }
    }
}
