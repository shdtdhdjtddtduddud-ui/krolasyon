package com.krolasyon.labirent.net;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.team.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/** Handles captain orders and other requests coming from clients. */
public final class ServerHooks {
    private ServerHooks() {}

    public static final int ORDER_SAY = 0, ORDER_RETURN = 1, ORDER_RUNNERS = 2, ORDER_DEFEND = 3, ORDER_GATHER = 4, ORDER_WORK = 5;

    static boolean canCommand(@Nullable ServerPlayer p, MazeManager m) {
        return p != null && (m.isCaptain(p) || p.hasPermissions(2));
    }

    public static void order(Net.Order msg, @Nullable ServerPlayer sender) {
        MazeManager m = MazeManager.get();
        if (m == null || sender == null) return;
        if (!canCommand(sender, m)) {
            sender.displayClientMessage(Component.translatable("msg.labirent.not_captain").withStyle(ChatFormatting.RED), true);
            return;
        }
        issue(m, sender, msg.order(), msg.text());
    }

    public static void issue(MazeManager m, ServerPlayer captain, int order, String text) {
        ServerLevel level = m.level();
        if (level == null) return;
        Component title = Component.translatable("order.labirent.title").withStyle(ChatFormatting.GOLD);
        Component body = order == ORDER_SAY ? Component.literal(text).withStyle(ChatFormatting.WHITE)
                : Component.translatable("order.labirent." + order).withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer p : m.server.getPlayerList().getPlayers()) {
            Net.title(p, title, body, 5, 60, 15);
            p.sendSystemMessage(Component.literal("[" + captain.getGameProfile().getName() + "] ").withStyle(ChatFormatting.GOLD).append(body));
            p.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
        if (order == ORDER_SAY) return;
        for (GladerEntity g : allGladers(level)) {
            switch (order) {
                case ORDER_RETURN -> g.setOrder(GladerEntity.Order.RETURN, null);
                case ORDER_RUNNERS -> { if (g.getRole() == Role.KOSUCU) g.sendRunning(); }
                case ORDER_DEFEND -> g.setOrder(GladerEntity.Order.DEFEND, null);
                case ORDER_GATHER -> g.setOrder(GladerEntity.Order.GATHER, captain.blockPosition());
                case ORDER_WORK -> g.setOrder(GladerEntity.Order.WORK, null);
                default -> {}
            }
            g.ackOrder(captain);
        }
    }

    public static List<GladerEntity> allGladers(ServerLevel level) {
        AABB all = new AABB(MazeLayout.ORIGIN - 30, 0, MazeLayout.ORIGIN - 30, MazeLayout.ORIGIN + MazeLayout.SIZE + 30, 256,
                MazeLayout.ORIGIN + MazeLayout.SIZE + 30);
        return level.getEntitiesOfClass(GladerEntity.class, all);
    }

    public static void setRole(Net.SetRole msg, @Nullable ServerPlayer sender) {
        MazeManager m = MazeManager.get();
        if (m == null || sender == null) return;
        if (!canCommand(sender, m)) {
            sender.displayClientMessage(Component.translatable("msg.labirent.not_captain").withStyle(ChatFormatting.RED), true);
            return;
        }
        Role r = Role.byId(msg.role());
        ServerLevel level = m.level();
        if (level != null && level.getEntity(msg.target()) instanceof GladerEntity g) {
            g.setRole(r);
            m.story.onRoleAssigned(r);
            sender.displayClientMessage(Component.translatable("msg.labirent.role_set", g.getGladerName(), r.title()), true);
            return;
        }
        if (m.state.roles.containsKey(msg.target())) {
            m.setRole(msg.target(), r);
            String name = m.state.names.getOrDefault(msg.target(), "?");
            sender.displayClientMessage(Component.translatable("msg.labirent.role_set", name, r.title()), true);
            ServerPlayer target = m.server.getPlayerList().getPlayer(msg.target());
            if (target != null) {
                Net.title(target, Component.translatable("title.labirent.new_role"), r.title(), 5, 60, 15);
                target.sendSystemMessage(r.description().withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static void npcCommand(Net.NpcCommand msg, @Nullable ServerPlayer sender) {
        MazeManager m = MazeManager.get();
        if (m == null || sender == null) return;
        Entity e = sender.level().getEntity(msg.entityId());
        if (!(e instanceof GladerEntity g) || e.distanceToSqr(sender) > 32 * 32) return;
        if (!canCommand(sender, m)) {
            sender.displayClientMessage(Component.translatable("msg.labirent.not_captain").withStyle(ChatFormatting.RED), true);
            return;
        }
        int c = msg.cmd();
        if (c >= 10) {
            Role r = Role.byId(c - 10);
            g.setRole(r);
            m.story.onRoleAssigned(r);
            sender.displayClientMessage(Component.translatable("msg.labirent.role_set", g.getGladerName(), r.title()), true);
        } else {
            switch (c) {
                case 0 -> g.setOrder(GladerEntity.Order.FOLLOW, null);
                case 1 -> g.setOrder(GladerEntity.Order.STAY, g.blockPosition());
                case 2 -> g.setOrder(GladerEntity.Order.WORK, null);
                case 3 -> g.setOrder(GladerEntity.Order.RETURN, null);
                case 4 -> g.setHome(g.blockPosition());
                default -> {}
            }
            g.setFollowTarget(sender.getUUID());
        }
        g.ackOrder(sender);
    }

    public static void terminal(Net.TerminalCode msg, @Nullable ServerPlayer sender) {
        MazeManager m = MazeManager.get();
        if (m == null || sender == null) return;
        m.story.enterCode(sender, msg.order());
    }

    public static void transfer(Net.Transfer msg, @Nullable ServerPlayer sender) {
        MazeManager m = MazeManager.get();
        if (m == null || sender == null || !canCommand(sender, m)) return;
        ServerPlayer target = m.server.getPlayerList().getPlayer(msg.target());
        if (target == null) return;
        m.state.captain = target.getUUID();
        m.state.captainName = target.getGameProfile().getName();
        m.state.setDirty();
        target.getInventory().add(new net.minecraft.world.item.ItemStack(com.krolasyon.labirent.registry.ModItems.CAPTAIN_BATON.get()));
        for (ServerPlayer p : m.server.getPlayerList().getPlayers())
            p.sendSystemMessage(Component.translatable("msg.labirent.new_captain", target.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
    }

    public static boolean inGlade(Entity e) { return Glade.inside(e.getX(), e.getZ()); }
}
