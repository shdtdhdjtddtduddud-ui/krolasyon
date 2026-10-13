package com.krolasyon.beyblade.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/** The Flame Libra itself. Rendered as the full 3D model in hand / inventory. Launch it with the launcher. */
public class LibraItem extends Item {
    /** "libra" or "ldrago": picks the tooltip, ability names and the 3D item renderer */
    private final String kind;

    public LibraItem(Properties props) {
        this(props, "libra");
    }

    public LibraItem(Properties props, String kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.librabeyblade.flame_libra.desc").withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable("item.librabeyblade.flame_libra.desc2").withStyle(ChatFormatting.GRAY));
        for (int i = 1; i <= 5; i++)
            tip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD).append(Component.translatable("ability.librabeyblade." + i).withStyle(ChatFormatting.YELLOW)));
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
