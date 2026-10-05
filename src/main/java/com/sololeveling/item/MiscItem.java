package com.sololeveling.item;

import com.sololeveling.gen.Content;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.system.PlayerSync;
import com.sololeveling.system.Sys;
import com.sololeveling.world.GateManager;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Utility items with right-click behaviours (coins, keys, scrolls, license, map, news, rune). */
public class MiscItem extends Item {
    public final String id;

    public MiscItem(String id) {
        super(props(id));
        this.id = id;
    }

    private static Item.Properties props(String id) {
        Item.Properties p = new Item.Properties();
        switch (id) {
            case "hunter_license", "world_map", "hunter_news" -> p.stacksTo(1);
            case "red_gate_key", "skill_scroll", "rune_stone" -> p.rarity(Rarity.EPIC);
            case "dungeon_key", "shadow_essence" -> p.rarity(Rarity.RARE);
            default -> { }
        }
        return p;
    }

    @Override
    public boolean isFoil(ItemStack s) {
        return id.equals("red_gate_key") || id.equals("skill_scroll") || id.equals("rune_stone") || id.equals("shadow_essence");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
        SLPlayer d = ModCaps.get(sp);
        switch (id) {
            case "gold_coin" -> {
                int n = sp.isShiftKeyDown() ? st.getCount() : 1;
                d.gold += 10L * n;
                st.shrink(n);
                level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.6F);
                PlayerSync.sync(sp);
            }
            case "dungeon_key" -> {
                if (GateManager.openKeyGate(sp, false)) { st.shrink(1); }
            }
            case "red_gate_key" -> {
                if (GateManager.openKeyGate(sp, true)) { st.shrink(1); }
            }
            case "skill_scroll" -> {
                List<String> pool = new ArrayList<>();
                for (Content.SkillDef s : Content.SKILLS) if (!d.hasSkill(s.id())) pool.add(s.id());
                if (pool.isEmpty()) {
                    Sys.info(sp, "gui.sololeveling.no_skill_left");
                    return InteractionResultHolder.fail(st);
                }
                String pick = pool.get(sp.getRandom().nextInt(pool.size()));
                d.skills.add(pick);
                st.shrink(1);
                Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.new_skill"), Component.translatable("skill.sololeveling." + pick));
                level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1F, 1F);
                PlayerSync.sync(sp);
            }
            case "hunter_license" -> Net.toPlayer(sp, new Packets.OpenSystem(0));
            case "world_map" -> Net.toPlayer(sp, new Packets.OpenSystem(3));
            case "hunter_news" -> Net.toPlayer(sp, new Packets.OpenSystem(4));
            case "rune_stone" -> {
                d.points += 1;
                st.shrink(1);
                Sys.notify(sp, Sys.REWARD, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.points_gain", 1));
                PlayerSync.sync(sp);
            }
            default -> { }
        }
        return InteractionResultHolder.consume(st);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.sololeveling." + id + ".desc").withStyle(ChatFormatting.DARK_AQUA));
    }
}
