package com.krolasyon.bosses.rpg.registry;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.entity.MagicBolt;
import com.krolasyon.bosses.rpg.entity.Summon;
import com.krolasyon.bosses.rpg.mob.RpgBoss;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.npc.RpgNpc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RpgEntities {
    private RpgEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KrolasyonBosses.MODID);

    public static final Map<String, RegistryObject<EntityType<RpgMonster>>> MOBS = new LinkedHashMap<>();
    private static final Map<EntityType<?>, MonsterDef> DEF_BY_TYPE = new IdentityHashMap<>();

    public static final RegistryObject<EntityType<MagicBolt>> MAGIC_BOLT = ENTITIES.register("magic_bolt",
            () -> EntityType.Builder.<MagicBolt>of(MagicBolt::new, MobCategory.MISC).sized(0.4F, 0.4F).fireImmune()
                    .clientTrackingRange(8).updateInterval(1).build("magic_bolt"));

    public static final RegistryObject<EntityType<RpgNpc>> NPC = ENTITIES.register("npc",
            () -> EntityType.Builder.<RpgNpc>of(RpgNpc::new, MobCategory.CREATURE).sized(0.6F, 1.95F)
                    .clientTrackingRange(10).build("npc"));

    public static final RegistryObject<EntityType<Summon>> SUMMON = ENTITIES.register("summon",
            () -> EntityType.Builder.<Summon>of(Summon::new, MobCategory.MISC).sized(0.8F, 1.6F)
                    .clientTrackingRange(10).build("summon"));

    static {
        for (MonsterDef d : RpgDefs.MONSTERS) MOBS.put(d.id(), ENTITIES.register(d.id(), () -> build(d)));
        for (MonsterDef d : RpgDefs.BOSSES) MOBS.put(d.id(), ENTITIES.register(d.id(), () -> build(d)));
    }

    private static EntityType<RpgMonster> build(MonsterDef d) {
        EntityType.Builder<RpgMonster> b = d.boss()
                ? EntityType.Builder.<RpgMonster>of(RpgBoss::new, MobCategory.MONSTER)
                : EntityType.Builder.<RpgMonster>of(RpgMonster::new, MobCategory.MONSTER);
        b.sized(d.hitWidth(), d.hitHeight()).clientTrackingRange(d.boss() ? 16 : 10);
        if (d.has(RpgDefs.T_FIRE_IMMUNE)) b.fireImmune();
        EntityType<RpgMonster> type = b.build(d.id());
        DEF_BY_TYPE.put(type, d);
        return type;
    }

    public static MonsterDef defOf(EntityType<?> type) {
        MonsterDef d = DEF_BY_TYPE.get(type);
        if (d == null) throw new IllegalStateException("No monster definition for " + type);
        return d;
    }

    @Nullable
    public static EntityType<RpgMonster> typeOf(String id) {
        RegistryObject<EntityType<RpgMonster>> r = MOBS.get(id);
        return r != null && r.isPresent() ? r.get() : null;
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        for (Map.Entry<String, RegistryObject<EntityType<RpgMonster>>> e : MOBS.entrySet()) {
            event.put(e.getValue().get(), RpgMonster.attributes(RpgDefs.BY_ID.get(e.getKey())).build());
        }
        event.put(NPC.get(), RpgNpc.createAttributes().build());
        event.put(SUMMON.get(), Summon.createAttributes().build());
    }
}
