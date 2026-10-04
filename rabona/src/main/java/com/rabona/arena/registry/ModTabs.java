package com.rabona.arena.registry;

import com.rabona.arena.RabonaArena;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RabonaArena.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rabonaarena"))
            .icon(() -> new ItemStack(ModItems.BALLS.get(0).get()))
            .displayItems((params, out) -> {
                ModItems.ITEMS.getEntries().forEach(i -> out.accept(i.get()));
            })
            .build());
}
