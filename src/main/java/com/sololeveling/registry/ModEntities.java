package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.*;
import com.sololeveling.gen.Content;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SoloLeveling.MODID);

    public static final Map<String, RegistryObject<EntityType<SLMonster>>> MONSTERS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<EntityType<ShadowEntity>>> SHADOWS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<EntityType<NpcEntity>>> NPCS = new LinkedHashMap<>();

    public static final RegistryObject<EntityType<GateEntity>> GATE = ENTITIES.register("gate",
            () -> EntityType.Builder.<GateEntity>of(GateEntity::new, MobCategory.MISC).sized(3.4F, 4.8F).fireImmune()
                    .clientTrackingRange(16).updateInterval(20).noSummon().build("gate"));

    public static final RegistryObject<EntityType<MagicBoltEntity>> MAGIC_BOLT = ENTITIES.register("magic_bolt",
            () -> EntityType.Builder.<MagicBoltEntity>of(MagicBoltEntity::new, MobCategory.MISC).sized(0.4F, 0.4F)
                    .clientTrackingRange(8).updateInterval(1).noSummon().build("magic_bolt"));

    static {
        for (Content.MonsterDef d : Content.MONSTERS) {
            MONSTERS.put(d.id(), ENTITIES.register(d.id(), () -> {
                EntityType.Builder<SLMonster> b = EntityType.Builder.<SLMonster>of((t, l) -> new SLMonster(t, l, d), MobCategory.MONSTER)
                        .sized(d.width(), d.height()).clientTrackingRange(d.boss() ? 16 : 10);
                if (d.id().equals("hell_hound") || d.id().equals("cerberus") || d.id().equals("kamish") || d.id().equals("demon_knight")) b.fireImmune();
                return b.build(d.id());
            }));
        }
        for (Content.ShadowDef d : Content.SHADOWS) {
            SHADOWS.put(d.id(), ENTITIES.register(d.id(), () ->
                    EntityType.Builder.<ShadowEntity>of((t, l) -> new ShadowEntity(t, l, d), MobCategory.CREATURE)
                            .sized(d.width(), d.height()).clientTrackingRange(10).build(d.id())));
        }
        for (String r : Content.NPC_ROLES) {
            NPCS.put(r, ENTITIES.register("npc_" + r, () ->
                    EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.MISC).sized(0.6F, 1.95F)
                            .clientTrackingRange(10).build("npc_" + r)));
        }
    }
}
