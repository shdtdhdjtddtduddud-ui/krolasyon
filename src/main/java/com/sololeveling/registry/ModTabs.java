package com.sololeveling.registry;

import com.sololeveling.SoloLeveling;
import com.sololeveling.gen.Content;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SoloLeveling.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sololeveling.main"))
            .icon(() -> new ItemStack(ModItems.get("shadow_monarch_sword")))
            .displayItems((p, out) -> {
                for (Content.WeaponDef d : Content.WEAPONS) out.accept(ModItems.get(d.id()));
                for (Content.StaffDef d : Content.STAVES) out.accept(ModItems.get(d.id()));
                for (var e : ModItems.ARMOR.values()) out.accept(e.get());
                for (Content.PotionDef d : Content.POTIONS) out.accept(ModItems.get(d.id()));
            }).build());

    public static final RegistryObject<CreativeModeTab> GEAR = TABS.register("gear", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sololeveling.gear"))
            .icon(() -> new ItemStack(ModItems.get("mana_crystal_a")))
            .displayItems((p, out) -> {
                for (Content.CrystalDef d : Content.CRYSTALS) out.accept(ModItems.get(d.id()));
                for (String id : Content.MISC) out.accept(ModItems.get(id));
                for (Content.BlockDef d : Content.BLOCKS) out.accept(ModItems.get(d.id()));
            }).build());

    public static final RegistryObject<CreativeModeTab> SPAWN = TABS.register("spawn", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sololeveling.spawn"))
            .icon(() -> new ItemStack(ModItems.get("igris_spawn_egg")))
            .displayItems((p, out) -> {
                for (var e : ModItems.EGGS.values()) out.accept(e.get());
            }).build());
}
