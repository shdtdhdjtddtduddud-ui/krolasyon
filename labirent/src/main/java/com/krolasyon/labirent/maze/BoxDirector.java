package com.krolasyon.labirent.maze;

import com.krolasyon.labirent.LabConfig;
import com.krolasyon.labirent.entity.BoxElevatorEntity;
import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.registry.ModEntities;
import com.krolasyon.labirent.registry.ModItems;
import com.krolasyon.labirent.registry.ModSounds;
import com.krolasyon.labirent.team.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.UUID;

/** The Box: an elevator that rises from the dark with a new Glader (player or NPC) and supplies. */
public final class BoxDirector {
    public static final String[] NEWCOMER_NAMES = {"Zart", "Winston", "Clint", "Jeff", "Siggy", "Stan", "Jack", "Dave", "Billy",
            "Adam", "Emre", "Can", "Mert", "Arda", "Deniz", "Kaan", "Ozan", "Efe", "Toby", "Sam", "Leo", "Nate", "Hugo", "Kerem"};

    private record Arrival(@Nullable UUID player, @Nullable String npcName, boolean supplies, boolean teresa) {}

    private final MazeManager m;
    private final ArrayDeque<Arrival> queue = new ArrayDeque<>();
    @Nullable private Arrival current;
    private int phase;
    private int timer;
    @Nullable private BoxElevatorEntity cage;
    @Nullable private Entity rider;

    BoxDirector(MazeManager m) { this.m = m; }

    public boolean busy() { return current != null; }

    public void queuePlayer(ServerPlayer p) {
        for (Arrival a : queue) if (p.getUUID().equals(a.player())) return;
        if (current != null && p.getUUID().equals(current.player())) return;
        queue.add(new Arrival(p.getUUID(), null, true, false));
        p.displayClientMessage(Component.translatable("msg.labirent.waiting_box"), true);
    }

    public void queueNpc(String name, boolean supplies) { queue.add(new Arrival(null, name, supplies, false)); }

    /** the monthly Box: only crates, no new Glader */
    public void queueSupplies() { queue.add(new Arrival(null, null, true, false)); }

    public void queueTeresa() { queue.add(new Arrival(null, "Teresa", false, true)); }

    public void tick(ServerLevel level, int day, int dayPhase) {
        MazeState st = m.state;
        if (current == null) {
            if (!m.gladeReady()) {
                if (m.server.getTickCount() % 40 == 0)
                    for (Arrival a : queue)
                        if (a.player() != null) {
                            ServerPlayer p = m.server.getPlayerList().getPlayer(a.player());
                            if (p != null) p.displayClientMessage(Component.translatable("msg.labirent.building_pct", (int) (m.builder.progress() * 100)), true);
                        }
                return;
            }
            if (queue.isEmpty() && st.lastArrivalDay >= 0 && !st.has(MazeState.EV_TERESA)
                    && day - st.lastArrivalDay >= LabConfig.MONTH_DAYS.get() && dayPhase >= 6000 && dayPhase < 9000) {
                st.lastArrivalDay = day;
                st.setDirty();
                queueSupplies();
            }
            if (queue.isEmpty()) return;
            current = queue.poll();
            if (current.player() != null && m.server.getPlayerList().getPlayer(current.player()) == null) {
                current = null;
                return;
            }
            phase = 0;
            timer = 0;
            if (st.lastArrivalDay < 0) {
                st.lastArrivalDay = day;
                st.setDirty();
            }
        }
        timer++;
        BlockPos top = new BlockPos(0, MazeLayout.GROUND + 1, 0);
        switch (phase) {
            case 0 -> { // alarm, open the shaft, load the cage
                if (timer == 1) {
                    Glade.closeBoxDoors(level);
                    Glade.setBoxFloor(level, false);
                    cage = new BoxElevatorEntity(ModEntities.BOX_ELEVATOR.get(), level);
                    cage.moveTo(-0.0, Glade.SHAFT_BOTTOM, -0.0, 0F, 0F);
                    cage.setTarget(Glade.BOX_FLOOR + 1);
                    level.addFreshEntity(cage);
                    if (current.player() != null) {
                        ServerPlayer p = m.server.getPlayerList().getPlayer(current.player());
                        if (p != null) {
                            p.teleportTo(level, 0, Glade.SHAFT_BOTTOM + 0.2, 0, 0F, -10F);
                            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0, false, false));
                            Net.title(p, Component.translatable("title.labirent.wake"), Component.translatable("title.labirent.wake.sub"), 10, 70, 20);
                            rider = p;
                        }
                    } else if (current.npcName() != null) {
                        GladerEntity g = new GladerEntity(ModEntities.GLADER.get(), level);
                        g.moveTo(0, Glade.SHAFT_BOTTOM + 0.2, 0, 0F, 0F);
                        boolean teresa = current.teresa();
                        g.setup(current.npcName(), teresa ? GladerEntity.SKIN_TERESA : level.random.nextInt(GladerEntity.SKINS), Role.SERBEST);
                        g.setNewcomer(true);
                        if (teresa) g.setTeresa();
                        level.addFreshEntity(g);
                        rider = g;
                    }
                    for (ServerPlayer p : level.players())
                        if (p != rider) Net.title(p, Component.translatable("title.labirent.box"), Component.translatable("title.labirent.box.sub"), 5, 50, 15);
                }
                if (timer == 6 && rider != null && cage != null) rider.startRiding(cage, true);
                if (timer % 40 == 1) level.playSound(null, top, ModSounds.BOX_ALARM.get(), SoundSource.BLOCKS, 3F, 1F);
                alarmLights(level);
                if (timer >= 12 && cage != null) {
                    cage.go();
                    level.playSound(null, top, ModSounds.BOX_RISE.get(), SoundSource.BLOCKS, 2.5F, 1F);
                    phase = 1;
                    timer = 0;
                }
            }
            case 1 -> {
                if (timer % 40 == 1) level.playSound(null, top, ModSounds.BOX_ALARM.get(), SoundSource.BLOCKS, 3F, 1F);
                alarmLights(level);
                if (rider instanceof ServerPlayer p && timer % 20 == 0) Net.shake(p, 0.15F, 22);
                if (cage == null || !cage.isAlive() || cage.arrived() || timer > 600) {
                    phase = 2;
                    timer = 0;
                }
            }
            case 2 -> {
                if (timer == 1) arrive(level);
                if (timer == 60 && rider instanceof GladerEntity g && g.isAlive()) {
                    g.teleportTo(-0.5, MazeLayout.GROUND + 1, 4.5);
                    g.setNewcomer(false);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, -0.5, MazeLayout.GROUND + 1.5, 4.5, 12, 0.3, 0.5, 0.3, 0.02);
                    for (ServerPlayer p : level.players()) p.sendSystemMessage(Component.translatable("msg.labirent.pulled_out", g.getGladerName()));
                    if (g.isTeresa()) m.story.onTeresaOut(g);
                }
                if (timer >= 80) {
                    current = null;
                    rider = null;
                    cage = null;
                }
            }
            default -> current = null;
        }
    }

    private void alarmLights(ServerLevel level) {
        if (m.server.getTickCount() % 4 != 0) return;
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1F, 0.1F, 0.05F), 2.0F);
        for (int k = 0; k < 4; k++) {
            double x = (k & 1) == 0 ? -3.4 : 2.4, z = (k & 2) == 0 ? -3.4 : 2.4;
            level.sendParticles(red, x + 0.5, MazeLayout.GROUND + 1.3, z + 0.5, 2, 0.1, 0.1, 0.1, 0);
        }
    }

    private void arrive(ServerLevel level) {
        MazeState st = m.state;
        Glade.setBoxFloor(level, true);
        if (cage != null) {
            cage.release();
            if (rider != null) rider.stopRiding();
            cage.discard();
        }
        if (rider != null) rider.teleportTo(-0.5, Glade.BOX_FLOOR + 1, -0.5);
        Glade.openBoxDoors(level);
        level.playSound(null, 0, MazeLayout.GROUND, 0, ModSounds.BOX_ARRIVE.get(), SoundSource.BLOCKS, 3F, 1F);
        if (current.supplies()) supplies(level);
        st.arrivals++;
        st.setDirty();
        if (rider instanceof ServerPlayer p) {
            st.arrived.add(p.getUUID());
            st.setDirty();
            p.setRespawnPosition(Dims.MAZE, Glade.SPAWN, 180F, true, false);
            p.getInventory().add(new ItemStack(ModItems.JOURNAL.get()));
            p.getInventory().add(new ItemStack(ModItems.RUNNER_MAP.get()));
            if (m.isCaptain(p)) {
                p.getInventory().add(new ItemStack(ModItems.CAPTAIN_BATON.get()));
                Net.title(p, Component.translatable("title.labirent.captain"), Component.translatable("title.labirent.captain.sub"), 10, 80, 20);
            } else {
                Net.title(p, Component.translatable("title.labirent.greenie"), Component.translatable("title.labirent.greenie.sub", p.getGameProfile().getName()), 10, 70, 20);
            }
            Net.sendFullMap(p, st);
            m.story.onPlayerArrived(p);
        }
        for (ServerPlayer p : level.players()) {
            if (p == rider) continue;
            if (rider == null) p.sendSystemMessage(Component.translatable("msg.labirent.box_supplies"));
            else p.sendSystemMessage(Component.translatable("msg.labirent.box_arrived",
                    rider instanceof GladerEntity g ? g.getGladerName() : rider.getName().getString()));
        }
    }

    private void supplies(ServerLevel level) {
        RandomSource r = level.random;
        BlockPos chest = new BlockPos(1, Glade.BOX_FLOOR + 1, 1);
        BlockPos barrel = new BlockPos(-2, Glade.BOX_FLOOR + 1, 1);
        if (!level.getBlockState(chest).is(Blocks.CHEST))
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.WEST), 3);
        if (!level.getBlockState(barrel).is(Blocks.BARREL)) level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), 3);
        boolean first = m.state.arrivals == 0;
        put(level, chest, Items.BREAD, 8 + r.nextInt(8));
        put(level, chest, Items.POTATO, 6 + r.nextInt(8));
        put(level, chest, Items.CARROT, 6 + r.nextInt(8));
        put(level, chest, Items.WHEAT_SEEDS, 10);
        put(level, chest, Items.BEEF, 4 + r.nextInt(6));
        put(level, chest, Items.CHICKEN, 4 + r.nextInt(4));
        put(level, chest, Items.APPLE, 4 + r.nextInt(6));
        put(level, chest, ModItems.GRIEF_SERUM.get(), first ? 3 : 1 + r.nextInt(2));
        put(level, chest, ModItems.BANDAGE.get(), 4 + r.nextInt(4));
        put(level, chest, Items.TORCH, 16);
        put(level, barrel, Items.OAK_LOG, 16 + r.nextInt(16));
        put(level, barrel, Items.IRON_INGOT, 3 + r.nextInt(5));
        put(level, barrel, Items.WHITE_WOOL, 4);
        put(level, barrel, Items.STRING, 6);
        put(level, barrel, Items.LEATHER, 4);
        Item[] tools = {ModItems.MACHETE.get(), ModItems.SPEAR.get(), ModItems.FRYING_PAN.get(), Items.IRON_HOE, Items.IRON_AXE, Items.SHEARS};
        put(level, barrel, tools[r.nextInt(tools.length)], 1);
        if (first) {
            put(level, barrel, ModItems.SPEAR.get(), 2);
            put(level, barrel, ModItems.MACHETE.get(), 1);
            put(level, barrel, ModItems.FRYING_PAN.get(), 1);
        }
    }

    private static void put(ServerLevel level, BlockPos pos, Item item, int count) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container c)) return;
        ItemStack s = new ItemStack(item, count);
        for (int k = 0; k < c.getContainerSize() && !s.isEmpty(); k++) {
            ItemStack in = c.getItem(k);
            if (in.isEmpty()) {
                c.setItem(k, s.copy());
                s.setCount(0);
            } else if (ItemStack.isSameItemSameTags(in, s) && in.getCount() < in.getMaxStackSize()) {
                int add = Math.min(s.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(add);
                s.shrink(add);
            }
        }
        c.setChanged();
    }
}
