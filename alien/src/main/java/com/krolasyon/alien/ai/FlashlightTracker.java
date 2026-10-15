package com.krolasyon.alien.ai;

import com.krolasyon.alien.item.FlashlightItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Moves an invisible light block to where the flashlight points. */
public final class FlashlightTracker {
    private FlashlightTracker() {}

    private static final Map<UUID, BlockPos> LIGHTS = new HashMap<>();

    public static boolean isLit(Player p) {
        return lit(p.getMainHandItem()) || lit(p.getOffhandItem());
    }

    private static boolean lit(ItemStack s) {
        return s.getItem() instanceof FlashlightItem && FlashlightItem.isOn(s);
    }

    public static void tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos old = LIGHTS.get(p.getUUID());
        if (!isLit(p) || !p.isAlive() || p.isSpectator() || HidingManager.isHidden(p)) {
            if (old != null) clear(level, old);
            LIGHTS.remove(p.getUUID());
            return;
        }
        if (p.tickCount % 2 != 0) return;
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(9));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        BlockPos target = hit.getType() == HitResult.Type.MISS ? BlockPos.containing(end) : hit.getBlockPos().relative(hit.getDirection());
        if (target.equals(old)) return;
        if (!level.getBlockState(target).isAir()) {
            BlockPos head = BlockPos.containing(eye);
            target = level.getBlockState(head).isAir() ? head : null;
        }
        if (old != null) clear(level, old);
        if (target != null) {
            level.setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12), 3);
            LIGHTS.put(p.getUUID(), target.immutable());
        } else LIGHTS.remove(p.getUUID());
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.LIGHT)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    public static void remove(ServerPlayer p) {
        BlockPos old = LIGHTS.remove(p.getUUID());
        if (old != null) clear(p.serverLevel(), old);
    }
}
