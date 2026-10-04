package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.Archetype;
import com.krolasyon.bosses.rpg.entity.Summon;
import com.krolasyon.bosses.rpg.mob.RpgAnimatable;
import com.krolasyon.bosses.rpg.mob.RpgMonster;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RpgClient {
    private RpgClient() {}

    public static final Map<Archetype, ModelLayerLocation> LAYERS = new EnumMap<>(Archetype.class);

    static {
        for (Archetype a : Archetype.values()) LAYERS.put(a, new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "rpg_" + a.name().toLowerCase()), "main"));
    }

    public static ResourceLocation tex(String name) {
        return new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/rpg/" + name + ".png");
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(LAYERS.get(Archetype.HUMANOID), RpgModels.Humanoid::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.BRUTE), RpgModels.Brute::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.QUADRUPED), RpgModels.Quadruped::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.ARACHNID), RpgModels.Arachnid::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.INSECT), RpgModels.Insect::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.FLYER), RpgModels.Flyer::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.SLIME), RpgModels.Slime::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.SERPENT), RpgModels.Serpent::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.GOLEM), RpgModels.Golem::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.FLOATER), RpgModels.Floater::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.TREANT), RpgModels.Treant::layer);
        e.registerLayerDefinition(LAYERS.get(Archetype.CRUSTACEAN), RpgModels.Crustacean::layer);
        e.registerLayerDefinition(NpcRenderer.FEATURES, NpcRenderer::featuresLayer);
    }

    @SubscribeEvent
    public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent e) {
        e.register(RpgKeys.CAST);
        e.register(RpgKeys.NEXT);
        e.register(RpgKeys.PREV);
        e.register(RpgKeys.CHARACTER);
        e.register(RpgKeys.JOURNAL);
        e.register(RpgKeys.MAP);
    }

    @SubscribeEvent
    public static void overlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("rpg_hud", RpgHud::render);
    }

    @SubscribeEvent
    public static void itemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> {
            if (tint != 1) return -1;
            com.krolasyon.bosses.rpg.def.SpellDef s = com.krolasyon.bosses.rpg.item.SpellTomeItem.spell(stack);
            return s == null ? 0xFFFFFF : s.color();
        }, com.krolasyon.bosses.rpg.item.RpgItems.SPELL_TOME.get());
        e.register((stack, tint) -> {
            if (tint != 1 || !stack.hasTag()) return -1;
            com.krolasyon.bosses.rpg.def.MonsterDef d = RpgDefs.BY_ID.get(stack.getTag().getString("Boss"));
            return d == null ? 0xFFFFFF : d.eyeColor();
        }, com.krolasyon.bosses.rpg.item.RpgItems.SEAL.get());
    }

    public static <T extends LivingEntity & RpgAnimatable> RpgMobModel<T> model(Archetype a, ModelPart p) {
        return switch (a) {
            case HUMANOID -> new RpgModels.Humanoid<>(p);
            case BRUTE -> new RpgModels.Brute<>(p);
            case QUADRUPED -> new RpgModels.Quadruped<>(p);
            case ARACHNID -> new RpgModels.Arachnid<>(p);
            case INSECT -> new RpgModels.Insect<>(p);
            case FLYER -> new RpgModels.Flyer<>(p);
            case SLIME -> new RpgModels.Slime<>(p);
            case SERPENT -> new RpgModels.Serpent<>(p);
            case GOLEM -> new RpgModels.Golem<>(p);
            case FLOATER -> new RpgModels.Floater<>(p);
            case TREANT -> new RpgModels.Treant<>(p);
            case CRUSTACEAN -> new RpgModels.Crustacean<>(p);
        };
    }

    private static boolean translucent(MonsterDef d) {
        return d.arch() == Archetype.SLIME || "ghost".equals(pattern(d));
    }

    /** ghostly monsters are drawn see-through; the texture generator writes alpha for them */
    private static String pattern(MonsterDef d) {
        return GHOSTS.contains(d.id()) ? "ghost" : "";
    }

    private static final java.util.Set<String> GHOSTS = java.util.Set.of("will_o_wisp", "sand_wraith", "wind_elemental", "wraith", "banshee",
            "thunder_jelly", "pirate_ghost", "ice_wraith", "highway_ghost");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (Map.Entry<String, RegistryObject<EntityType<RpgMonster>>> en : RpgEntities.MOBS.entrySet()) {
            MonsterDef d = RpgDefs.BY_ID.get(en.getKey());
            ResourceLocation t = tex(d.id());
            ResourceLocation g = d.glow() ? tex(d.id() + "_glow") : null;
            e.registerEntityRenderer(en.getValue().get(), (EntityRendererProvider.Context ctx) -> new RpgMobRenderer<RpgMonster>(ctx,
                    model(d.arch(), ctx.bakeLayer(LAYERS.get(d.arch()))), Math.min(3.0F, d.hitWidth() * 0.55F),
                    x -> t, g == null ? null : x -> g, RpgMonster::renderScale, translucent(d)));
        }
        e.registerEntityRenderer(RpgEntities.SUMMON.get(), (EntityRendererProvider.Context ctx) -> new SummonRenderer(ctx));
        e.registerEntityRenderer(RpgEntities.MAGIC_BOLT.get(), MagicBoltRenderer::new);
        e.registerEntityRenderer(RpgEntities.NPC.get(), NpcRenderer::new);
    }

    /** summons can be any archetype, so the renderer swaps models per kind */
    static class SummonRenderer extends net.minecraft.client.renderer.entity.EntityRenderer<Summon> {
        private final Map<Summon.Kind, RpgMobRenderer<Summon>> byKind = new EnumMap<>(Summon.Kind.class);

        SummonRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            for (Summon.Kind k : Summon.Kind.values()) {
                ResourceLocation t = tex(k.texture());
                ResourceLocation glow = tex(k.texture() + "_glow");
                byKind.put(k, new RpgMobRenderer<Summon>(ctx, model(k.arch, ctx.bakeLayer(LAYERS.get(k.arch))), 0.5F, x -> t, x -> glow,
                        x -> k == Summon.Kind.TREANT || k == Summon.Kind.BLOOD_GOLEM ? 0.8F : 1.0F, k == Summon.Kind.SPIRIT_WOLF || k == Summon.Kind.SPIRIT_WARRIOR || k == Summon.Kind.MIRROR));
            }
        }

        @Override
        public void render(Summon e, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
            byKind.get(e.kind()).render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Summon e) { return tex(e.kind().texture()); }
    }
}
