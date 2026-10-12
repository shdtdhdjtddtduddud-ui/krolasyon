package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.Config;
import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.entity.DebrisEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side helpers shared by all alien powers: targeting, damage, particles, world interaction. */
public final class Fx {
    private Fx() {}

    // ---------------- particles & sound ----------------

    public static void particles(ServerLevel l, ParticleOptions p, double x, double y, double z, int n, double dx, double dy, double dz, double speed) {
        l.sendParticles(p, x, y, z, n, dx, dy, dz, speed);
    }

    public static void particles(ServerLevel l, ParticleOptions p, Vec3 v, int n, double spread, double speed) {
        l.sendParticles(p, v.x, v.y, v.z, n, spread, spread, spread, speed);
    }

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F), size);
    }

    public static void sound(ServerLevel l, Vec3 at, SoundEvent s, float vol, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** particles along a straight line */
    public static void line(ServerLevel l, ParticleOptions p, Vec3 a, Vec3 b, double step, double spread) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        int n = Math.max(1, (int) (len / step));
        for (int i = 0; i <= n; i++) {
            Vec3 v = a.add(d.scale(i / (double) n));
            l.sendParticles(p, v.x, v.y, v.z, 1, spread, spread, spread, 0);
        }
    }

    /** horizontal ring of particles */
    public static void ring(ServerLevel l, ParticleOptions p, Vec3 c, double r, int n, double up) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
            l.sendParticles(p, x, c.y, z, 0, Math.cos(a) * 0.2, up, Math.sin(a) * 0.2, 1.0);
        }
    }

    /** a point relative to the player's view: side > 0 = right hand side */
    public static Vec3 rel(Player p, double forward, double side, double down) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = Vec3.directionFromRotation(0, p.getYRot());
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        return p.getEyePosition().add(look.scale(forward)).add(right.scale(side)).add(0, -down, 0);
    }

    public static Vec3 flatLook(Player p) {
        return Vec3.directionFromRotation(0, p.getYRot());
    }

    // ---------------- targeting ----------------

    public static boolean valid(ServerPlayer p, Entity e) {
        if (e == p || !e.isAlive() || !(e instanceof LivingEntity le) || e instanceof ArmorStand) return false;
        if (e.isSpectator()) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return false;
        if (e instanceof Player other && (other.isCreative() || !p.canHarmPlayer(other))) return false;
        if (le.isPassengerOfSameVehicle(p)) return false;
        OmniData d = OmniData.get(p);
        return d.targetId != e.getId() || d.ability < 0;
    }

    public static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r))) {
            if (valid(p, e) && e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(c) <= r + e.getBbWidth()) out.add(e);
        }
        return out;
    }

    /** entities inside a cone in front of the player */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double halfAngleDeg) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngleDeg));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : around(p, eye, range)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double len = to.length();
            if (len < 1e-3 || to.normalize().dot(look) >= cos || len < 1.2) out.add(e);
        }
        return out;
    }

    /** entities near a segment */
    public static List<LivingEntity> beam(ServerPlayer p, Vec3 a, Vec3 b, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        AABB box = new AABB(a, b).inflate(radius + 1);
        Vec3 ab = b.subtract(a);
        double L2 = Math.max(1e-6, ab.lengthSqr());
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!valid(p, e)) continue;
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
            double t = Mth.clamp(c.subtract(a).dot(ab) / L2, 0, 1);
            if (c.distanceTo(a.add(ab.scale(t))) <= radius + e.getBbWidth() / 2 + e.getBbHeight() / 3) out.add(e);
        }
        return out;
    }

    public static LivingEntity nearestInFront(ServerPlayer p, double range, double halfAngle) {
        return cone(p, range, halfAngle).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    /** the entity the player is looking at (generous aim assist), or null */
    public static LivingEntity aimed(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = rayEnd(p, range);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : beam(p, eye, end, 1.6)) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
            double along = c.subtract(eye).dot(look);
            double off = c.subtract(eye.add(look.scale(along))).length();
            double score = off * 4 + along * 0.2;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** where the player's view ray hits a block (or the max range point) */
    public static Vec3 rayEnd(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    public static BlockHitResult rayBlock(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        return p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
    }

    /** y of the ground surface at/below a position (searching a few blocks) */
    public static double groundY(ServerLevel l, double x, double y, double z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y + 1), Mth.floor(z));
        for (int i = 0; i < 8; i++) {
            BlockState s = l.getBlockState(m);
            if (!s.getCollisionShape(l, m).isEmpty()) return m.getY() + s.getCollisionShape(l, m).max(net.minecraft.core.Direction.Axis.Y);
            m.move(0, -1, 0);
        }
        return y;
    }

    // ---------------- damage ----------------

    public static boolean hit(ServerPlayer p, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        return e.hurt(p.damageSources().playerAttack(p), amount * Config.dmg());
    }

    public static boolean hitMagic(ServerPlayer p, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        return e.hurt(p.damageSources().indirectMagic(p, p), amount * Config.dmg());
    }

    public static boolean hitFire(ServerPlayer p, LivingEntity e, float amount, int fireSeconds) {
        e.invulnerableTime = 0;
        boolean ok = e.hurt(p.damageSources().playerAttack(p), amount * Config.dmg());
        if (!e.fireImmune()) e.setSecondsOnFire(fireSeconds);
        return ok;
    }

    public static void knock(LivingEntity e, Vec3 from, double horiz, double up) {
        Vec3 d = e.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1e-4) d = new Vec3(e.getRandom().nextDouble() - 0.5, 0, e.getRandom().nextDouble() - 0.5);
        d = d.normalize();
        double res = 1 - Mth.clamp(e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE), 0, 1) * 0.7;
        e.setDeltaMovement(e.getDeltaMovement().scale(0.3).add(d.x * horiz * res, up * res, d.z * horiz * res));
        e.hurtMarked = true;
    }

    public static void push(LivingEntity e, Vec3 v) {
        e.setDeltaMovement(v);
        e.hurtMarked = true;
    }

    /** radial ground impact: damage, knock-up, block debris, shake */
    public static void shockwave(ServerPlayer p, Vec3 c, double r, float dmg, double horiz, double up, int debris) {
        ServerLevel l = p.serverLevel();
        for (LivingEntity e : around(p, c, r)) {
            double f = 1 - Math.min(1, e.distanceTo(p) / (r + 1)) * 0.5;
            hit(p, e, (float) (dmg * f));
            knock(e, c, horiz * f, up * f);
        }
        for (int i = 0; i < debris; i++) {
            double a = l.random.nextDouble() * Math.PI * 2, rr = 1 + l.random.nextDouble() * r * 0.8;
            double x = c.x + Math.cos(a) * rr, z = c.z + Math.sin(a) * rr;
            BlockPos bp = BlockPos.containing(x, groundY(l, x, c.y + 1, z) - 0.5, z);
            BlockState s = l.getBlockState(bp);
            if (s.isAir() || !s.getFluidState().isEmpty()) continue;
            DebrisEntity.spawn(l, new Vec3(x, bp.getY() + 1.05, z), s,
                    new Vec3(Math.cos(a) * 0.15, 0.45 + l.random.nextDouble() * 0.45, Math.sin(a) * 0.15));
        }
        for (int k = 1; k <= 3; k++) {
            double rr = r * k / 3.0;
            int n = (int) (rr * 10);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                double x = c.x + Math.cos(a) * rr, z = c.z + Math.sin(a) * rr;
                BlockPos bp = BlockPos.containing(x, groundY(l, x, c.y + 1, z) - 0.5, z);
                BlockState s = l.getBlockState(bp);
                if (!s.isAir()) l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), x, bp.getY() + 1.1, z, 2, 0.2, 0.1, 0.2, 0.2);
            }
        }
        l.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, r * 0.3, 0.1, r * 0.3, 0);
        com.krolasyon.ben10.net.Net.shake(p, r * 4, (float) Math.min(1.0, r / 7.0));
    }

    // ---------------- world interaction ----------------

    public static boolean grief() {
        return Config.grief();
    }

    public static boolean soft(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.IMPERMEABLE)
                || s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) || s.is(Blocks.DEAD_BUSH)
                || s.is(Blocks.SNOW) || s.is(BlockTags.WOOL_CARPETS) || s.is(Blocks.GLASS_PANE) || s.is(BlockTags.CROPS) || s.is(Blocks.VINE)
                || s.is(Blocks.COBWEB) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.SWEET_BERRY_BUSH);
    }

    /** breaks fragile blocks (glass, leaves, plants) in a sphere */
    public static int shatter(ServerLevel l, Vec3 c, double r, int max) {
        if (!grief()) return 0;
        int n = 0;
        int R = (int) Math.ceil(r);
        BlockPos o = BlockPos.containing(c);
        for (BlockPos bp : BlockPos.betweenClosed(o.offset(-R, -R, -R), o.offset(R, R, R))) {
            if (n >= max) break;
            if (bp.distToCenterSqr(c) > r * r) continue;
            BlockState s = l.getBlockState(bp);
            if (soft(s) && !s.is(ModRegistry.CRYSTAL.get())) {
                l.destroyBlock(bp, true);
                n++;
            }
        }
        return n;
    }

    /** Heatblast heat: ignite, melt ice/snow, bake sand and clay, evaporate water */
    public static void heat(ServerLevel l, BlockPos bp, boolean strong) {
        if (!grief()) return;
        BlockState s = l.getBlockState(bp);
        if (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.FROSTED_ICE) || s.is(ModRegistry.FROST.get())) {
            l.setBlockAndUpdate(bp, Blocks.WATER.defaultBlockState());
        } else if (s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW)) {
            l.removeBlock(bp, false);
        } else if (s.is(Blocks.SNOW_BLOCK)) {
            l.setBlockAndUpdate(bp, Blocks.WATER.defaultBlockState());
        } else if (strong && (s.is(Blocks.SAND) || s.is(Blocks.RED_SAND))) {
            l.setBlockAndUpdate(bp, Blocks.GLASS.defaultBlockState());
        } else if (strong && s.is(Blocks.CLAY)) {
            l.setBlockAndUpdate(bp, Blocks.TERRACOTTA.defaultBlockState());
        } else if (strong && s.is(Blocks.COBBLESTONE)) {
            l.setBlockAndUpdate(bp, Blocks.STONE.defaultBlockState());
        } else if (s.getFluidState().is(Fluids.WATER) && s.getFluidState().isSource() && strong) {
            l.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
            l.sendParticles(ParticleTypes.CLOUD, bp.getX() + 0.5, bp.getY() + 0.8, bp.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.02);
        } else if (s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(BlockTags.LEAVES)) {
            l.setBlockAndUpdate(bp, BaseFireBlock.getState(l, bp));
        } else if (s.isAir()) {
            BlockPos below = bp.below();
            if (l.getBlockState(below).isFaceSturdy(l, below, net.minecraft.core.Direction.UP) && l.random.nextFloat() < (strong ? 0.5F : 0.25F)) {
                l.setBlockAndUpdate(bp, BaseFireBlock.getState(l, bp));
            }
        }
    }

    /** gentle body heat: only melts snow and ice (never ignites) */
    public static void melt(ServerLevel l, BlockPos bp) {
        if (!grief()) return;
        BlockState s = l.getBlockState(bp);
        if (s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE) || s.is(ModRegistry.FROST.get())) l.setBlockAndUpdate(bp, Blocks.WATER.defaultBlockState());
        else if (s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW)) l.removeBlock(bp, false);
    }

    /** Big Chill frost: freeze water, lava -> obsidian, extinguish fire, place snow */
    public static void frost(ServerLevel l, BlockPos bp, boolean snow) {
        if (!grief()) return;
        BlockState s = l.getBlockState(bp);
        FluidState f = s.getFluidState();
        if (f.is(Fluids.WATER) && f.isSource() && s.is(Blocks.WATER)) {
            l.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
            l.scheduleTick(bp, Blocks.FROSTED_ICE, Mth.nextInt(l.random, 160, 260));
        } else if (f.is(Fluids.LAVA)) {
            l.setBlockAndUpdate(bp, f.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
            l.sendParticles(ParticleTypes.LARGE_SMOKE, bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.02);
        } else if (s.is(BlockTags.FIRE)) {
            l.removeBlock(bp, false);
        } else if (snow && s.isAir()) {
            BlockPos below = bp.below();
            if (Blocks.SNOW.defaultBlockState().canSurvive(l, bp) && l.getBlockState(below).isFaceSturdy(l, below, net.minecraft.core.Direction.UP)) {
                l.setBlockAndUpdate(bp, Blocks.SNOW.defaultBlockState());
            }
        }
    }

    /** sets a temporary block (crystal / frost) only where there is air or a replaceable plant */
    public static boolean temp(ServerLevel l, BlockPos bp, Block b) {
        if (!grief()) return false;
        BlockState s = l.getBlockState(bp);
        if (s.isAir() || s.canBeReplaced()) {
            if (!s.getFluidState().isEmpty() && !s.is(Blocks.WATER)) return false;
            l.setBlockAndUpdate(bp, b.defaultBlockState());
            return true;
        }
        return false;
    }

    /** blocks around a point for "ground" effects */
    public static Iterable<BlockPos> disk(Vec3 c, double r, int dy0, int dy1) {
        List<BlockPos> out = new ArrayList<>();
        int R = (int) Math.ceil(r);
        BlockPos o = BlockPos.containing(c);
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = dy0; dy <= dy1; dy++) out.add(o.offset(dx, dy, dz));
            }
        return out;
    }
}
