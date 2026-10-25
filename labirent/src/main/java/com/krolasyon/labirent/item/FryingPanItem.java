package com.krolasyon.labirent.item;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.team.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/** Frypan's pan. Use it on a lit campfire / furnace / smoker to cook the raw food you carry. Hurts, too. */
public class FryingPanItem extends SwordItem {
    private static final Map<Item, Item> COOK = Map.of(Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
            Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON, Items.POTATO, Items.BAKED_POTATO,
            Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.RABBIT, Items.COOKED_RABBIT);

    public FryingPanItem(Properties p) { super(Tiers.IRON, 3, -2.8F, p); }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level level = c.getLevel();
        BlockState s = level.getBlockState(c.getClickedPos());
        boolean hot = s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
        Player p = c.getPlayer();
        if (!hot || p == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        MazeManager m = MazeManager.get();
        boolean cook = m != null && m.state.role(p.getUUID()) == Role.ASCI;
        int budget = cook ? 64 : 2, done = 0;
        for (int k = 0; k < p.getInventory().getContainerSize() && budget > 0; k++) {
            ItemStack in = p.getInventory().getItem(k);
            Item out = COOK.get(in.getItem());
            if (out == null) continue;
            int n = Math.min(budget, in.getCount());
            in.shrink(n);
            budget -= n;
            done += n;
            ItemStack res = new ItemStack(out, n);
            if (!p.getInventory().add(res)) p.drop(res, false);
        }
        if (cook && takeVeg(p)) {
            p.getInventory().add(new ItemStack(com.krolasyon.labirent.registry.ModItems.GLADE_STEW.get()));
            done++;
        }
        if (done == 0) {
            p.displayClientMessage(Component.translatable("msg.labirent.pan_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        BlockPos pos = c.getClickedPos();
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.6F, 1.6F);
        ((ServerLevel) level).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
        p.displayClientMessage(Component.translatable("msg.labirent.pan_cooked", done), true);
        p.getCooldowns().addCooldown(this, cook ? 20 : 100);
        return InteractionResult.CONSUME;
    }

    private static boolean takeVeg(Player p) {
        int carrots = p.getInventory().countItem(Items.CARROT);
        int potatoes = p.getInventory().countItem(Items.BAKED_POTATO);
        if (carrots < 1 || potatoes < 1) return false;
        for (Item it : new Item[]{Items.CARROT, Items.BAKED_POTATO})
            for (int k = 0; k < p.getInventory().getContainerSize(); k++)
                if (p.getInventory().getItem(k).is(it)) {
                    p.getInventory().getItem(k).shrink(1);
                    break;
                }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.labirent.frying_pan.tip").withStyle(ChatFormatting.GRAY));
    }
}
