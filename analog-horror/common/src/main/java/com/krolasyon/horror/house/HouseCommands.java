package com.krolasyon.horror.house;

import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.registry.ModEntities;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** /hatman ev (or house) builds the house in front of you; /hatman cagir spawns the Hatman in its dark bedroom. */
public final class HouseCommands {
    private HouseCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hatman").requires(s -> s.hasPermission(2))
                .then(Commands.literal("ev").executes(HouseCommands::house))
                .then(Commands.literal("house").executes(HouseCommands::house)));
    }

    private static int house(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        Vec3 p = src.getPosition();
        // put the front door 4 blocks north of the caller, who ends up facing it
        BlockPos origin = BlockPos.containing(p.x - 10, p.y, p.z - 28);
        HauntedHouse.build(level, origin);
        HatmanEntity h = ModEntities.HATMAN.get().create(level);
        if (h != null) {
            h.moveTo(origin.getX() + 6.5, origin.getY() + 7, origin.getZ() + 4.5, 180F, 0F);
            level.addFreshEntity(h);
        }
        src.sendSuccess(() -> Component.translatable("commands.krolasyonhorror.house"), true);
        return 1;
    }
}
