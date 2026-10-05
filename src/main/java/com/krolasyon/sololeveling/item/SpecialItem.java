package com.krolasyon.sololeveling.item;

import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.system.*;
import com.krolasyon.sololeveling.world.DungeonManager;
import com.krolasyon.sololeveling.world.NewsManager;
import com.krolasyon.sololeveling.world.Regions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Keys, stones, boxes and papers with a right-click behaviour. */
public class SpecialItem extends Item {
    public enum Kind { RETURN_STONE, INSTANT_DUNGEON_KEY, DEMON_CASTLE_KEY, RANDOM_BOX, HUNTER_LICENSE, NEWSPAPER, RUNE_STONE, MATERIAL, MAGIC_STONE }

    public final Kind kind;
    @Nullable
    public final Skill rune;
    public final int value;

    public SpecialItem(Kind kind, Skill rune, int value, Properties props) {
        super(props);
        this.kind = kind;
        this.rune = rune;
        this.value = value;
    }

    @Override
    public boolean isFoil(ItemStack s) { return kind == Kind.RUNE_STONE || kind == Kind.DEMON_CASTLE_KEY; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.MATERIAL || kind == Kind.MAGIC_STONE) return InteractionResultHolder.pass(stack);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        HunterData d = HunterCapability.get(p);
        boolean consume = false;
        switch (kind) {
            case RETURN_STONE -> {
                if (p.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
                Regions.teleport(p, "seoul_plaza");
                p.getCooldowns().addCooldown(this, 20 * 30);
            }
            case INSTANT_DUNGEON_KEY -> {
                if (DungeonManager.get(p.server).enterInstant(p)) consume = true;
            }
            case DEMON_CASTLE_KEY -> {
                if (d.job == Job.NONE) {
                    Sys.warn(p, "region.locked_job");
                    return InteractionResultHolder.fail(stack);
                }
                d.waypoints.add("demon_castle");
                d.markDirty();
                Regions.teleport(p, "demon_castle");
            }
            case RANDOM_BOX -> {
                consume = true;
                openBox(p);
            }
            case HUNTER_LICENSE -> {
                p.sendSystemMessage(Component.translatable("sololeveling.license.line", p.getName(), Component.translatable(d.rank.langKey()), d.level,
                        Component.translatable(d.guild.langKey())).withStyle(ChatFormatting.AQUA));
            }
            case NEWSPAPER -> NewsManager.get(p.server).open(p);
            case RUNE_STONE -> {
                if (rune == null) break;
                if (d.hasSkill(rune)) {
                    Sys.warn(p, "rune.known");
                    return InteractionResultHolder.fail(stack);
                }
                d.runeSkills.add(rune);
                Progression.autoSlot(d);
                d.markDirty();
                Sys.notify(p, Sys.REWARD, Sys.t("skill.acquired"), Sys.t("skill." + rune.id()));
                consume = true;
            }
            default -> {}
        }
        if (consume && !p.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    private static void openBox(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        var r = p.getRandom();
        int roll = r.nextInt(100);
        List<Supplier<? extends Item>> pool;
        if (roll < 40) pool = List.of(ModItems.HEALING_POTION, ModItems.MANA_POTION, ModItems.FATIGUE_POTION, ModItems.ANTIDOTE);
        else if (roll < 70) pool = List.of(ModItems.GREATER_HEALING_POTION, ModItems.GREATER_MANA_POTION, ModItems.STRENGTH_ELIXIR, ModItems.AGILITY_ELIXIR, ModItems.PERCEPTION_DRAUGHT, ModItems.STEALTH_TONIC);
        else if (roll < 85) pool = List.of(ModItems.INSTANT_DUNGEON_KEY, ModItems.MAGIC_STONE_C, ModItems.MAGIC_STONE_B);
        else if (roll < 96) pool = List.of(ModItems.RUNE_STONE_BLOODLUST, ModItems.RUNE_STONE_MUTILATION, ModItems.RUNE_STONE_STEALTH, ModItems.STEEL_DAGGER, ModItems.KNIGHT_KILLER);
        else pool = List.of(ModItems.ELIXIR_OF_LIFE, ModItems.HOLY_WATER, ModItems.RUNE_STONE_DAGGER_STORM, ModItems.BARUKA_DAGGER);
        ItemStack got = new ItemStack(pool.get(r.nextInt(pool.size())).get(), 1);
        int gold = 20 + r.nextInt(80) + d.level * 3;
        d.gold += gold;
        d.markDirty();
        Progression.give(p, got);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
        Sys.notify(p, Sys.REWARD, Sys.t("random_box.title"), Sys.t("random_box.body", got.getHoverName(), gold));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (kind == Kind.RUNE_STONE && rune != null)
            tip.add(Component.translatable("sololeveling.tooltip.rune", Component.translatable(rune.langKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (kind == Kind.MAGIC_STONE || kind == Kind.MATERIAL)
            tip.add(Component.translatable("sololeveling.tooltip.value", value).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
