package com.krolasyon.bosses.realm.world;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.registry.RealmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RealmFeatures {
    private RealmFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, KrolasyonBosses.MODID);

    interface Placer { boolean place(WorldGenLevel level, BlockPos origin, RandomSource r); }

    static final class Simple extends Feature<NoneFeatureConfiguration> {
        private final Placer placer;

        Simple(Placer placer) {
            super(NoneFeatureConfiguration.CODEC);
            this.placer = placer;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            return placer.place(ctx.level(), ctx.origin(), ctx.random());
        }
    }

    private static RegistryObject<Feature<NoneFeatureConfiguration>> reg(String name, Placer p) {
        return FEATURES.register(name, () -> new Simple(p));
    }

    static boolean solidGround(WorldGenLevel l, BlockPos p) {
        BlockState below = l.getBlockState(p.below());
        return below.isSolid() && below.getFluidState().isEmpty() && p.getY() > l.getSeaLevel();
    }

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PORTAL_RUIN = reg("portal_ruin", (l, o, r) -> {
        if (!solidGround(l, o)) return false;
        Builders.portalRuin(l, o, r, r.nextFloat() < 0.75F);
        return true;
    });

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ASH_KEEP = keep("ash_keep", Faction.ASH);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BLOOD_KEEP = keep("blood_keep", Faction.BLOOD);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SHADOW_KEEP = keep("shadow_keep", Faction.SHADOW);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LEGION_KEEP = keep("legion_keep", Faction.LEGION);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SOUL_KEEP = keep("soul_keep", Faction.SOUL);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> THRONE_RUINS = reg("throne_ruins", (l, o, r) -> {
        if (!solidGround(l, o)) return false;
        Builders.keep(l, o, r, null);
        return true;
    });

    private static RegistryObject<Feature<NoneFeatureConfiguration>> keep(String name, Faction f) {
        return reg(name, (l, o, r) -> {
            if (!solidGround(l, o)) return false;
            Builders.keep(l, o, r, f);
            KeepGuards.spawn(l, o, r, f);
            return true;
        });
    }

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CRYSTAL_SPIRE = reg("crystal_spire", (l, o, r) -> {
        if (!solidGround(l, o)) return false;
        Builders.crystalSpire(l, o, r);
        return true;
    });
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BONE_RIBS = reg("bone_ribs", (l, o, r) -> {
        if (!solidGround(l, o)) return false;
        Builders.boneRibs(l, o, r);
        return true;
    });
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> WAR_SPIKES = reg("war_spikes", (l, o, r) -> {
        if (!solidGround(l, o)) return false;
        Builders.warSpikes(l, o, r);
        return true;
    });
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BLOOD_POOL = reg("blood_pool", (l, o, r) -> {
        if (!solidGround(l, o) || !l.getBlockState(o.below()).is(RealmBlocks.BLOOD_MOSS.get())) return false;
        Builders.bloodPool(l, o, r);
        return true;
    });
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CHARRED_TREE = reg("charred_tree", (l, o, r) -> {
        if (!solidGround(l, o)) return false;
        Builders.charredTree(l, o, r);
        return true;
    });
}
