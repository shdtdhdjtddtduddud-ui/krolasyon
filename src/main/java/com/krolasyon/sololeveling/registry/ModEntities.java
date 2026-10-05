package com.krolasyon.sololeveling.registry;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.entity.*;
import com.krolasyon.sololeveling.world.GateEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REG = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SoloLeveling.MODID);
    public static final EnumMap<MobKind, RegistryObject<EntityType<SLMonster>>> MOBS = new EnumMap<>(MobKind.class);
    private static final Map<EntityType<?>, MobKind> KINDS = new HashMap<>();

    static {
        for (MobKind k : MobKind.values()) {
            MOBS.put(k, REG.register(k.id(), () -> {
                EntityType.Builder<SLMonster> b = EntityType.Builder.<SLMonster>of((type, level) -> Monsters.create(k, type, level), MobCategory.MONSTER)
                        .sized(k.width, k.height).clientTrackingRange(k.boss ? 16 : 10);
                if (k.boss || k.category == MobKind.Category.DEMON || k.category == MobKind.Category.CONSTRUCT) b.fireImmune();
                return b.build(k.id());
            }));
        }
    }

    public static final RegistryObject<EntityType<ShadowEntity>> SHADOW = REG.register("shadow_soldier",
            () -> EntityType.Builder.<ShadowEntity>of(ShadowEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).fireImmune().build("shadow_soldier"));
    public static final RegistryObject<EntityType<HunterNpc>> NPC = REG.register("hunter_npc",
            () -> EntityType.Builder.<HunterNpc>of(HunterNpc::new, MobCategory.MISC).sized(0.6F, 1.9F).clientTrackingRange(10).build("hunter_npc"));
    public static final RegistryObject<EntityType<GateEntity>> GATE = REG.register("gate",
            () -> EntityType.Builder.<GateEntity>of(GateEntity::new, MobCategory.MISC).sized(3.0F, 5.0F).clientTrackingRange(16).fireImmune().build("gate"));
    public static final RegistryObject<EntityType<SLProjectile>> PROJECTILE = REG.register("magic_projectile",
            () -> EntityType.Builder.<SLProjectile>of(SLProjectile::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(6).updateInterval(2).build("magic_projectile"));

    private ModEntities() {}

    public static MobKind kindOf(EntityType<?> type) {
        MobKind k = kindOrNull(type);
        return k == null ? MobKind.GOBLIN : k;
    }

    public static MobKind kindOrNull(EntityType<?> type) {
        if (KINDS.isEmpty()) MOBS.forEach((k, ro) -> { if (ro.isPresent()) KINDS.put(ro.get(), k); });
        return KINDS.get(type);
    }

    public static EntityType<SLMonster> mob(MobKind k) { return MOBS.get(k).get(); }

    public static void attributes(EntityAttributeCreationEvent e) {
        for (MobKind k : MobKind.values()) e.put(MOBS.get(k).get(), SLMonster.attributes(k).build());
        e.put(SHADOW.get(), ShadowEntity.createAttributes().build());
        e.put(NPC.get(), HunterNpc.createAttributes().build());
    }
}
