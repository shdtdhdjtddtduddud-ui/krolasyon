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

/** /hatman ev|house builds the house; /hatman hikaye|story starts the story (singleplayer); /hatman stop leaves it. */
public final class HouseCommands {
    private HouseCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("hatman")
                .then(Commands.literal("ev").requires(s -> s.hasPermission(2)).executes(HouseCommands::house))
                .then(Commands.literal("house").requires(s -> s.hasPermission(2)).executes(HouseCommands::house))
                .then(Commands.literal("story").executes(HouseCommands::story))
                .then(Commands.literal("hikaye").executes(HouseCommands::story))
                .then(Commands.literal("stop").executes(c -> {
                    com.krolasyon.horror.story.StoryDirector.stop(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int story(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        com.krolasyon.horror.story.StoryDirector.start(ctx.getSource().getPlayerOrException());
        return 1;
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
