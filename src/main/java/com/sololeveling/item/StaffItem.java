package com.sololeveling.item;

import com.sololeveling.client.ClientHooks;
import com.sololeveling.entity.MagicBoltEntity;
import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModEntities;
import com.sololeveling.system.Sys;
import com.sololeveling.util.Ranks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StaffItem extends Item {
    public final Content.StaffDef def;

    public StaffItem(Content.StaffDef def) {
        super(new Item.Properties().stacksTo(1).durability(1500).rarity(Ranks.rarity(def.rank())).fireResistant());
        this.def = def;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer sp = (ServerPlayer) player;
        SLPlayer d = ModCaps.get(sp);
        if (d.level < def.level()) { Sys.warn(sp, "gui.sololeveling.level_too_low", def.level()); return InteractionResultHolder.fail(stack); }
        if (d.mana < def.mana()) { Sys.warn(sp, "gui.sololeveling.no_mana"); return InteractionResultHolder.fail(stack); }
        d.mana -= def.mana();
        float dmg = def.damage() + d.intel() * 0.5F + d.level * 0.15F;
        MagicBoltEntity bolt = new MagicBoltEntity(ModEntities.MAGIC_BOLT.get(), level);
        bolt.setOwner(sp);
        bolt.setup(def.kind(), dmg);
        bolt.setPos(sp.getX(), sp.getEyeY() - 0.2, sp.getZ());
        bolt.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 2.2F, 0.4F);
        level.addFreshEntity(bolt);
        level.playSound(null, sp.blockPosition(), def.kind().equals("ice") ? SoundEvents.SNOWBALL_THROW : SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.getCooldowns().addCooldown(this, 12);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.empty().append(Ranks.tag(def.rank())).append(Component.literal("  "))
                .append(Component.translatable("gui.sololeveling.req_level", def.level())
                        .withStyle(ClientHooks.playerLevel() >= def.level() ? ChatFormatting.GRAY : ChatFormatting.RED)));
        tip.add(Component.translatable("item.sololeveling." + def.id() + ".desc").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("gui.sololeveling.mana_cost", def.mana()).withStyle(ChatFormatting.BLUE));
    }
}
