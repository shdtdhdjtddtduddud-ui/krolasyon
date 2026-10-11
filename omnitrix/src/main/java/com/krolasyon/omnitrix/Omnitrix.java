package com.krolasyon.omnitrix;

import com.krolasyon.omnitrix.net.Net;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Omnitrix.MODID)
public class Omnitrix {
    public static final String MODID = "omnitrix";

    public Omnitrix() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.ITEMS.register(bus);
        ModRegistry.ENTITIES.register(bus);
        ModRegistry.SOUNDS.register(bus);
        ModRegistry.PARTICLES.register(bus);
        bus.addListener(OmniCapability::register);
        bus.addListener(this::setup);
        bus.addListener(this::tabs);
        MinecraftForge.EVENT_BUS.addGenericListener(net.minecraft.world.entity.Entity.class, OmniCapability::attach);
        MinecraftForge.EVENT_BUS.addListener(OmniCapability::clone);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        if (Boolean.getBoolean("omnitrix.autotest")) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> com.krolasyon.omnitrix.client.AutoTest::init);
        }
    }

    private void setup(FMLCommonSetupEvent e) { e.enqueueWork(Net::register); }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || e.getTabKey() == CreativeModeTabs.COMBAT) e.accept(ModRegistry.OMNITRIX.get());
    }

    /** /omnitrix transform <alien> | revert | ability <0-3> | recharge | remove  (ops) */
    private void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("omnitrix").requires(s -> s.hasPermission(2))
                .then(Commands.literal("transform").then(Commands.argument("alien", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(c, "alien");
                    for (Alien a : Alien.VALUES) {
                        if (a.id.equals(id)) {
                            OmniData d = OmniCapability.get(p);
                            d.wearing = true;
                            d.recharge = 0;
                            Transform.transform(p, a.ordinal());
                            return 1;
                        }
                    }
                    c.getSource().sendFailure(Component.literal("unknown alien " + id));
                    return 0;
                })))
                .then(Commands.literal("revert").executes(c -> { Transform.revert(c.getSource().getPlayerOrException(), false); return 1; }))
                .then(Commands.literal("ability").then(Commands.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    OmniData d = OmniCapability.get(p);
                    java.util.Arrays.fill(d.cooldowns, 0);
                    Powers.use(p, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "slot"));
                    return 1;
                })))
                .then(Commands.literal("recharge").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    OmniData d = OmniCapability.get(p);
                    d.recharge = 0;
                    java.util.Arrays.fill(d.cooldowns, 0);
                    if (d.transformed()) d.timer = OmniData.TRANSFORM_TIME;
                    Net.sync(p);
                    return 1;
                }))
                .then(Commands.literal("remove").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    OmniData d = OmniCapability.get(p);
                    if (d.transformed()) Transform.revert(p, false);
                    if (d.wearing) {
                        d.wearing = false;
                        p.getInventory().add(new ItemStack(ModRegistry.OMNITRIX.get()));
                    }
                    Net.sync(p);
                    return 1;
                })));
    }
}
