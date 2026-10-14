package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * A beyblade item (special or common). Rendered as the full 3D model in hand / inventory. Launch it with its own
 * launcher or the Standard Launcher. Carries its level, experience and battle mode in NBT.
 */
public class LibraItem extends Item {
    /** "libra", "ldrago", "leone", "eagle", "pegasus" or "common_N": picks the tooltip, ability names and the 3D renderer */
    private final String kind;
    private final int variant;

    public LibraItem(Properties props) {
        this(props, "libra");
    }

    public LibraItem(Properties props, String kind) {
        super(props);
        this.kind = kind;
        this.variant = kind.startsWith("common_") ? Integer.parseInt(kind.substring(7)) : -1;
    }

    public String kind() { return kind; }

    /** common beyblade variant, or -1 for the legendary ones */
    public int variant() { return variant; }

    public EntityType<? extends BeybladeEntity> spawnType() {
        if (variant >= 0) return ModEntities.COMMON.get();
        return switch (kind) {
            case "ldrago" -> ModEntities.LDRAGO.get();
            case "leone" -> ModEntities.LEONE.get();
            case "eagle" -> ModEntities.EAGLE.get();
            case "pegasus" -> ModEntities.PEGASUS.get();
            case "tempo" -> ModEntities.TEMPO.get();
            case "variares" -> ModEntities.VARIARES.get();
            default -> ModEntities.LIBRA.get();
        };
    }

    public static int level(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.contains("BeyLevel") ? Math.max(1, t.getInt("BeyLevel")) : 1;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return level(stack) >= 10; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String base = getDescriptionId();
        int lvl = level(stack);
        CompoundTag t = stack.getTag();
        int xp = t != null ? t.getInt("BeyXp") : 0;
        int mode = t != null && t.contains("BeyMode") ? t.getInt("BeyMode") : -1;
        if (variant >= 0) {
            CommonBeybladeEntity.Variant v = CommonBeybladeEntity.VARIANTS[variant];
            if (mode < 0) mode = v.mode();
            tip.add(Component.translatable("item.librabeyblade.common.desc", Component.translatable("mode.librabeyblade." + BeybladeEntity.MODE_IDS[v.mode()]))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            ChatFormatting col = switch (kind) {
                case "ldrago" -> ChatFormatting.RED;
                case "eagle" -> ChatFormatting.LIGHT_PURPLE;
                case "pegasus" -> ChatFormatting.AQUA;
                case "tempo" -> ChatFormatting.WHITE;
                case "variares" -> ChatFormatting.DARK_RED;
                default -> ChatFormatting.GREEN;
            };
            tip.add(Component.translatable("item.librabeyblade.legendary").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tip.add(Component.translatable(base + ".desc").withStyle(col));
        }
        tip.add(Component.translatable("item.librabeyblade.level", lvl, xp, BeybladeEntity.xpNeeded(lvl)).withStyle(ChatFormatting.YELLOW));
        if (mode >= 0) tip.add(Component.translatable("item.librabeyblade.mode", Component.translatable("mode.librabeyblade." + BeybladeEntity.MODE_IDS[mode]))
                .withStyle(ChatFormatting.BLUE));
        if (variant >= 0) {
            CommonBeybladeEntity.Variant v = CommonBeybladeEntity.VARIANTS[variant];
            tip.add(Component.translatable("item.librabeyblade.common.desc2").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD).append(Component.translatable("ability.librabeyblade.common.1").withStyle(ChatFormatting.YELLOW)));
            tip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD).append(Component.translatable("ability.librabeyblade.common.2").withStyle(ChatFormatting.YELLOW)));
            tip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD).append(Component.translatable("ability.librabeyblade.common.3." + BeybladeEntity.MODE_IDS[v.mode()])
                    .withStyle(lvl >= CommonBeybladeEntity.BURST_UNLOCK ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY))
                    .append(lvl >= CommonBeybladeEntity.BURST_UNLOCK ? Component.empty() : Component.translatable("item.librabeyblade.unlock", CommonBeybladeEntity.BURST_UNLOCK).withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }
        tip.add(Component.translatable(base + ".desc2").withStyle(ChatFormatting.GRAY));
        String ab = kind.equals("libra") ? "ability.librabeyblade." : "ability.librabeyblade." + kind + ".";
        for (int i = 1; i <= 5; i++)
            tip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD).append(Component.translatable(ab + i).withStyle(ChatFormatting.YELLOW)));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.krolasyon.beyblade.client.LibraItemRenderer.get(kind);
            }
        });
    }
}
