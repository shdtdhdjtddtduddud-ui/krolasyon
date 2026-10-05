package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.SoloLeveling;
import com.krolasyon.sololeveling.block.RegionPortalBlockEntity;
import com.krolasyon.sololeveling.entity.HunterNpc;
import com.krolasyon.sololeveling.registry.ModBlocks;
import com.krolasyon.sololeveling.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, SoloLeveling.MODID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CITY = FEATURES.register("city", () -> new CityFeature());

    private ModWorldgen() {}

    /** Generates one chunk of Seoul. Placed once per chunk by the seoul biome. */
    static class CityFeature extends Feature<NoneFeatureConfiguration> {
        CityFeature() { super(NoneFeatureConfiguration.CODEC); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            BlockPos o = ctx.origin();
            int cx = o.getX() & ~15, cz = o.getZ() & ~15;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            CityPlan.Writer w = new CityPlan.Writer() {
                @Override
                public void set(int x, int y, int z, BlockState s) {
                    if ((x & ~15) != cx || (z & ~15) != cz || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return;
                    level.setBlock(m.set(x, y, z), s, 2);
                }

                @Override
                public void portal(int x, int y, int z, String target) {
                    if ((x & ~15) != cx || (z & ~15) != cz) return;
                    level.setBlock(m.set(x, y, z), ModBlocks.REGION_PORTAL.get().defaultBlockState(), 2);
                    if (level.getBlockEntity(m) instanceof RegionPortalBlockEntity be) be.target = target;
                }

                @Override
                public void npc(int x, int y, int z, String role, float yaw) {
                    if ((x & ~15) != cx || (z & ~15) != cz) return;
                    HunterNpc n = ModEntities.NPC.get().create(level.getLevel());
                    if (n == null) return;
                    n.moveTo(x + 0.5, y, z + 0.5, yaw, 0);
                    n.setYHeadRot(yaw);
                    n.setupRole(role, level.getRandom());
                    n.setPersistenceRequired();
                    n.finalizeSpawn(level, level.getCurrentDifficultyAt(m.set(x, y, z)), MobSpawnType.STRUCTURE, null, null);
                    level.addFreshEntity(n);
                }
            };
            for (int dx = 0; dx < 16; dx++)
                for (int dz = 0; dz < 16; dz++) CityPlan.column(w, cx + dx, cz + dz);
            return true;
        }
    }
}
