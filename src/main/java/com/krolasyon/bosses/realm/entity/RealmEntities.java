package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Realm;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RealmEntities {
    private RealmEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KrolasyonBosses.MODID);
    public static final Map<String, RegistryObject<? extends EntityType<? extends Mob>>> TYPES = new LinkedHashMap<>();
    private static final Map<EntityType<?>, MobSpec> SPECS = new HashMap<>();
    private static final Map<String, MobSpec> BY_ID = new HashMap<>();

    public static final RegistryObject<EntityType<SpellProjectile>> SPELL = ENTITIES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(8).updateInterval(1).fireImmune().build("spell_projectile"));

    static {
        for (MobSpec s : GenSpecs.ALL) {
            BY_ID.put(s.id(), s);
            switch (s.kind()) {
                case GROUND -> TYPES.put(s.id(), reg(s, RealmMob::new));
                case FLYING -> TYPES.put(s.id(), reg(s, RealmFlyingMob::new));
                case ENVOY -> TYPES.put(s.id(), reg(s, EnvoyEntity::new));
                case LORD, TYRANT -> TYPES.put(s.id(), reg(s, RealmBoss::new));
            }
        }
    }

    private static <T extends Mob> RegistryObject<EntityType<T>> reg(MobSpec s, EntityType.EntityFactory<T> factory) {
        MobCategory cat = s.kind() == MobSpec.Kind.ENVOY ? MobCategory.CREATURE : MobCategory.MONSTER;
        return ENTITIES.register(s.id(), () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(factory, cat).sized(s.width(), s.height())
                    .clientTrackingRange(s.boss() ? 12 : 9);
            if (s.fireImmune()) b.fireImmune();
            EntityType<T> t = b.build(Realm.rl(s.id()).toString());
            SPECS.put(t, s);
            return t;
        });
    }

    public static MobSpec spec(EntityType<?> type) {
        MobSpec s = SPECS.get(type);
        if (s == null) throw new IllegalStateException("no realm spec for " + type);
        return s;
    }

    @Nullable
    public static MobSpec spec(String id) { return BY_ID.get(id); }

    @Nullable
    @SuppressWarnings("unchecked")
    public static EntityType<? extends Mob> type(String id) {
        RegistryObject<? extends EntityType<? extends Mob>> r = TYPES.get(id);
        return r == null ? null : r.get();
    }

    public static AttributeSupplier.Builder attributes(MobSpec s) {
        AttributeSupplier.Builder b = Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, s.health())
                .add(Attributes.ATTACK_DAMAGE, s.damage())
                .add(Attributes.ARMOR, s.armor())
                .add(Attributes.MOVEMENT_SPEED, s.speed())
                .add(Attributes.KNOCKBACK_RESISTANCE, s.knockbackResist())
                .add(Attributes.FOLLOW_RANGE, s.boss() ? 48.0 : 28.0);
        if (s.flying()) b.add(Attributes.FLYING_SPEED, s.speed() * 1.6);
        return b;
    }

    public static void onAttributes(EntityAttributeCreationEvent e) {
        for (MobSpec s : GenSpecs.ALL) {
            EntityType<? extends Mob> t = type(s.id());
            if (t == null) continue;
            if (s.boss()) e.put(t, com.krolasyon.bosses.entity.BossEntity.createMonsterAttributes()
                    .add(Attributes.MAX_HEALTH, s.health()).add(Attributes.ATTACK_DAMAGE, s.damage()).add(Attributes.ARMOR, s.armor())
                    .add(Attributes.ARMOR_TOUGHNESS, 4.0).add(Attributes.MOVEMENT_SPEED, s.speed())
                    .add(Attributes.KNOCKBACK_RESISTANCE, s.knockbackResist()).add(Attributes.FOLLOW_RANGE, 48.0).build());
            else e.put(t, attributes(s).build());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent e) {
        for (MobSpec s : GenSpecs.ALL) {
            if (s.boss()) continue;
            EntityType t = type(s.id());
            if (t == null) continue;
            if (s.kind() == MobSpec.Kind.ENVOY) {
                e.register(t, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (type, level, reason, pos, r) -> level.getBlockState(pos.below()).isSolid(), SpawnPlacementRegisterEvent.Operation.REPLACE);
            } else {
                e.register(t, s.flying() ? SpawnPlacements.Type.NO_RESTRICTIONS : SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (type, level, reason, pos, r) -> Monster.checkAnyLightMonsterSpawnRules((EntityType<? extends Monster>) type, level, reason, pos, r),
                        SpawnPlacementRegisterEvent.Operation.REPLACE);
            }
        }
    }
}
