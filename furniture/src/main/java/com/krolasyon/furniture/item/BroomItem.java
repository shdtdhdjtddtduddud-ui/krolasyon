package com.krolasyon.furniture.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A sweeping broom. Right click: sweeps the area in front of you - pulls dropped items to you, shoves monsters back
 * (small damage) and clears cobwebs and snow layers around you. Also a decent knockback stick.
 */
public class BroomItem extends Item {
    private static final UUID KNOCKBACK_UUID = UUID.fromString("5c6a1d26-3d5e-4a69-9b30-7a2f4d3e8a11");
    private final Multimap<Attribute, AttributeModifier> attributes;

    public BroomItem(Properties props) {
        super(props);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 2.0D, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.0D, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_UUID, "Weapon modifier", 1.2D, AttributeModifier.Operation.ADDITION));
        attributes = b.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributes : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(Blocks.COBWEB) ? 15.0F : 1.0F;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        player.getCooldowns().addCooldown(this, 14);
        player.swing(hand, true);
        if (level instanceof ServerLevel server) {
            sweep(server, player);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void sweep(ServerLevel level, Player player) {
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 center = player.position().add(look.scale(2.0D)).add(0.0D, 0.6D, 0.0D);
        AABB area = new AABB(center, center).inflate(2.6D, 1.3D, 2.6D);
        List<Entity> found = level.getEntities(player, area, e -> e instanceof LivingEntity || e instanceof ItemEntity);
        for (Entity e : found) {
            if (e instanceof ItemEntity item) {
                Vec3 to = player.position().subtract(item.position());
                item.setDeltaMovement(to.normalize().scale(0.5D).add(0.0D, 0.18D, 0.0D));
                item.hurtMarked = true;
            } else if (e instanceof LivingEntity le && !(e instanceof Player)) {
                if (le instanceof Enemy) {
                    le.hurt(level.damageSources().playerAttack(player), 2.0F);
                    le.knockback(1.1D, player.getX() - le.getX(), player.getZ() - le.getZ());
                } else {
                    le.knockback(0.35D, player.getX() - le.getX(), player.getZ() - le.getZ());
                }
            }
        }
        BlockPos feet = player.blockPosition();
        int cleaned = 0;
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-3, -1, -3), feet.offset(3, 2, 3))) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.COBWEB)) {
                level.destroyBlock(p, false);
                Block.popResource(level, p, new ItemStack(Items.STRING));
                cleaned++;
            } else if (st.is(Blocks.SNOW)) {
                int layers = st.getValue(SnowLayerBlock.LAYERS);
                level.removeBlock(p, false);
                Block.popResource(level, p, new ItemStack(Items.SNOWBALL, layers));
                cleaned++;
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.35F);
        if (cleaned > 0) level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, player.getY() + 1.0D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.POOF, center.x, player.getY() + 0.15D, center.z, 10, 1.0D, 0.05D, 1.0D, 0.02D);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new com.krolasyon.furniture.client.BroomItemExt());
    }
}
