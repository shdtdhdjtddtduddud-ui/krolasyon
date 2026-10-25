package com.krolasyon.labirent.entity.ai;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.ServerHooks;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/** What each Glader actually does on the job. Called every ~20 ticks while working in the Glade. */
public final class Jobs {
    private Jobs() {}

    private static final Map<Item, Item> COOK = Map.of(
            Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.CHICKEN, Items.COOKED_CHICKEN,
            Items.MUTTON, Items.COOKED_MUTTON, Items.POTATO, Items.BAKED_POTATO, Items.COD, Items.COOKED_COD);

    public static void work(GladerEntity g, ServerLevel level, MazeManager m) {
        switch (g.getRole()) {
            case ASCI -> cook(g, level);
            case BAHCIVAN -> farm(g, level);
            case KASAP -> slice(g, level);
            case SIFACI -> heal(g, level, m);
            case BEKCI -> guard(g, level);
            case KOSUCU -> rest(g, level, Glade.MAP_TABLE.offset(3, 0, 6), 4);
            default -> rest(g, level, g.home(), 7);
        }
    }

    private static boolean walkTo(GladerEntity g, BlockPos p, double r) {
        if (g.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) <= r * r) return true;
        if (g.getNavigation().isDone()) g.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.0);
        return false;
    }

    private static void rest(GladerEntity g, ServerLevel level, BlockPos home, int radius) {
        if (!walkTo(g, home, radius + 2)) return;
        if (g.getRandom().nextInt(8) == 0 && g.getNavigation().isDone()) {
            BlockPos t = home.offset(g.getRandom().nextInt(radius * 2 + 1) - radius, 0, g.getRandom().nextInt(radius * 2 + 1) - radius);
            g.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 0.7);
        }
    }

    // ------------------------------------------------------------------ cook
    private static void cook(GladerEntity g, ServerLevel level) {
        if (!walkTo(g, g.home(), 3)) return;
        g.getLookControl().setLookAt(Glade.KITCHEN_FIRE.getX() + 0.5, Glade.KITCHEN_FIRE.getY() + 0.5, Glade.KITCHEN_FIRE.getZ() + 0.5);
        if (g.tickCount % 60 < 20) g.swing(InteractionHand.MAIN_HAND);
        if (g.tickCount % 100 < 20)
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, Glade.KITCHEN_FIRE.getX() + 0.5, Glade.KITCHEN_FIRE.getY() + 1, Glade.KITCHEN_FIRE.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
        if (g.tickCount % 400 >= 20) return;
        Container chest = container(level, Glade.KITCHEN_CHEST);
        Container box = container(level, new BlockPos(1, Glade.BOX_FLOOR + 1, 1));
        Container garden = container(level, Glade.GARDEN_CHEST);
        Container slicer = container(level, Glade.SLICER_CHEST);
        if (chest == null) return;
        // collect raw food from the box, the gardens and the Blood House
        for (Container src : new Container[]{box, garden, slicer}) {
            if (src == null) continue;
            for (int k = 0; k < src.getContainerSize(); k++) {
                ItemStack s = src.getItem(k);
                if (COOK.containsKey(s.getItem()) || s.is(Items.WHEAT) || s.is(Items.CARROT) || s.is(Items.BEETROOT)) {
                    ItemStack left = insert(chest, s.copy());
                    src.setItem(k, left);
                }
            }
            src.setChanged();
        }
        boolean cooked = false;
        // turn raw food into meals
        for (int k = 0; k < chest.getContainerSize(); k++) {
            ItemStack s = chest.getItem(k);
            Item out = COOK.get(s.getItem());
            if (out != null) {
                int n = Math.min(4, s.getCount());
                s.shrink(n);
                insert(chest, new ItemStack(out, n));
                cooked = true;
            }
        }
        if (take(chest, Items.WHEAT, 3)) { insert(chest, new ItemStack(ModItems.GLADE_BREAD.get(), 2)); cooked = true; }
        if (take(chest, Items.CARROT, 2) || take(chest, Items.BEETROOT, 3)) { insert(chest, new ItemStack(ModItems.GLADE_STEW.get(), 1)); cooked = true; }
        if (count(chest, ModItems.GLADE_STEW.get()) + count(chest, ModItems.FRYPAN_BREAKFAST.get()) < 6 && g.getRandom().nextInt(4) == 0) {
            insert(chest, new ItemStack(g.getRandom().nextBoolean() ? ModItems.GLADE_STEW.get() : ModItems.FRYPAN_BREAKFAST.get()));
            cooked = true;
        }
        chest.setChanged();
        if (cooked) level.playSound(null, Glade.KITCHEN_FIRE, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1F, 1F);
    }

    /** the cook hands a meal to a hungry Glader */
    public static boolean serveMeal(GladerEntity g, ServerPlayer p) {
        if (!(g.level() instanceof ServerLevel level)) return false;
        Container chest = container(level, Glade.KITCHEN_CHEST);
        Item[] meals = {ModItems.FRYPAN_BREAKFAST.get(), ModItems.GLADE_STEW.get(), ModItems.GLADE_BREAD.get(), Items.COOKED_BEEF, Items.BAKED_POTATO};
        for (Item meal : meals) {
            if (chest != null && take(chest, meal, 1)) {
                chest.setChanged();
                p.getInventory().add(new ItemStack(meal));
                g.say(p, Component.translatable("talk.labirent.serve", new ItemStack(meal).getHoverName()));
                return true;
            }
        }
        if (g.getRandom().nextInt(3) == 0) {
            p.getInventory().add(new ItemStack(ModItems.GLADE_BREAD.get()));
            g.say(p, Component.translatable("talk.labirent.serve_last"));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ gardener
    private static void farm(GladerEntity g, ServerLevel level) {
        BlockPos target = null;
        for (int tries = 0; tries < 12 && target == null; tries++) {
            BlockPos p = new BlockPos(7 + g.getRandom().nextInt(21), Glade.Y, 7 + g.getRandom().nextInt(21));
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CropBlock c && c.isMaxAge(s)) target = p;
        }
        if (target == null) {
            if (g.inventory.isEmpty()) rest(g, level, g.home(), 6);
            else deposit(g, level, Glade.GARDEN_CHEST);
            return;
        }
        if (!walkTo(g, target, 2.5)) return;
        BlockState s = level.getBlockState(target);
        if (s.getBlock() instanceof CropBlock c && c.isMaxAge(s)) {
            for (ItemStack drop : Block.getDrops(s, level, target, null)) g.inventory.addItem(drop);
            level.setBlock(target, c.getStateForAge(0), 3);
            level.levelEvent(2001, target, Block.getId(s));
            g.swing(InteractionHand.MAIN_HAND);
            // a gardener's touch makes neighbours grow
            BlockPos n = target.offset(g.getRandom().nextInt(5) - 2, 0, g.getRandom().nextInt(5) - 2);
            BlockState ns = level.getBlockState(n);
            if (ns.getBlock() instanceof CropBlock nc && !nc.isMaxAge(ns)) {
                level.setBlock(n, nc.getStateForAge(Math.min(nc.getMaxAge(), nc.getAge(ns) + 1)), 3);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, n.getX() + 0.5, n.getY() + 0.5, n.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
            }
        }
        if (g.inventory.getItem(8).getCount() > 0 || countAll(g) > 40) deposit(g, level, Glade.GARDEN_CHEST);
    }

    // ------------------------------------------------------------------ slicer
    private static void slice(GladerEntity g, ServerLevel level) {
        BlockPos pen = new BlockPos(-21, Glade.Y, 22);
        if (!g.inventory.isEmpty()) {
            deposit(g, level, Glade.SLICER_CHEST);
            return;
        }
        if (!walkTo(g, pen, 5)) return;
        if (g.tickCount % 600 >= 20) {
            rest(g, level, pen, 4);
            return;
        }
        AABB box = new AABB(-28, Glade.Y - 1, 17, -15, Glade.Y + 3, 28);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, box);
        Class<?>[] kinds = {Cow.class, Pig.class, Sheep.class, Chicken.class};
        for (Class<?> k : kinds) {
            List<Animal> of = animals.stream().filter(k::isInstance).toList();
            if (of.size() > 5) {
                Animal victim = of.stream().filter(a -> !a.isBaby()).findFirst().orElse(null);
                if (victim != null) {
                    g.swing(InteractionHand.MAIN_HAND);
                    victim.hurt(level.damageSources().mobAttack(g), 1000F);
                    level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, victim.getBoundingBox().inflate(2))
                            .forEach(ie -> { g.inventory.addItem(ie.getItem().copy()); ie.discard(); });
                    return;
                }
            } else if (of.size() >= 2) {
                Animal a = of.get(0), b = of.get(1);
                if (!a.isBaby() && !b.isBaby() && a.canFallInLove() && b.canFallInLove()) {
                    a.setInLove(null);
                    b.setInLove(null);
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------ med-jack
    private static void heal(GladerEntity g, ServerLevel level, MazeManager m) {
        List<LivingEntity> patients = level.getEntitiesOfClass(LivingEntity.class, g.getBoundingBox().inflate(28),
                e -> e != g && (e instanceof Player || e instanceof GladerEntity) && e.isAlive()
                        && (e.hasEffect(ModEffects.VENOM.get()) || e.getHealth() < e.getMaxHealth() * 0.7F));
        if (patients.isEmpty()) {
            if (!hasSerum(g)) fetchSerum(g, level);
            else rest(g, level, g.home(), 4);
            return;
        }
        LivingEntity p = patients.get(0);
        if (!walkTo(g, p.blockPosition(), 2.2)) {
            g.getNavigation().moveTo(p, 1.2);
            return;
        }
        g.swing(InteractionHand.MAIN_HAND);
        g.getLookControl().setLookAt(p);
        if (p.hasEffect(ModEffects.VENOM.get())) {
            if (hasSerum(g)) {
                takeFromInv(g, ModItems.GRIEF_SERUM.get());
                com.krolasyon.labirent.item.SerumItem.cure(p, g);
            } else fetchSerum(g, level);
        } else {
            p.heal(4F);
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 3, 0.3, 0.2, 0.3, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 0.8F, 1.2F);
        }
    }

    private static boolean hasSerum(GladerEntity g) { return g.inventory.hasAnyMatching(s -> s.is(ModItems.GRIEF_SERUM.get())); }

    private static void fetchSerum(GladerEntity g, ServerLevel level) {
        Container c = container(level, Glade.MED_CHEST);
        if (c != null && take(c, ModItems.GRIEF_SERUM.get(), 1)) {
            c.setChanged();
            g.inventory.addItem(new ItemStack(ModItems.GRIEF_SERUM.get()));
            return;
        }
        Container box = container(level, new BlockPos(1, Glade.BOX_FLOOR + 1, 1));
        if (box != null && take(box, ModItems.GRIEF_SERUM.get(), 1)) {
            box.setChanged();
            g.inventory.addItem(new ItemStack(ModItems.GRIEF_SERUM.get()));
        }
    }

    // ------------------------------------------------------------------ keeper of the doors
    private static void guard(GladerEntity g, ServerLevel level) {
        List<GrieverEntity> threats = level.getEntitiesOfClass(GrieverEntity.class, g.getBoundingBox().inflate(24));
        if (!threats.isEmpty()) {
            g.setTarget(threats.get(0));
            return;
        }
        rest(g, level, g.home(), 2);
    }

    // ------------------------------------------------------------------ beds
    @Nullable
    public static BlockPos claimBed(GladerEntity g, ServerLevel level) {
        if (g.bed() != null) return g.bed();
        List<GladerEntity> all = ServerHooks.allGladers(level);
        outer:
        for (BlockPos b : Glade.BEDS) {
            for (GladerEntity o : all) if (o != g && b.equals(o.bed())) continue outer;
            g.setBed(b);
            return b;
        }
        return null;
    }

    // ------------------------------------------------------------------ container helpers
    @Nullable
    public static Container container(ServerLevel level, BlockPos p) {
        BlockEntity be = level.getBlockEntity(p);
        return be instanceof Container c ? c : null;
    }

    public static ItemStack insert(Container c, ItemStack s) {
        for (int k = 0; k < c.getContainerSize() && !s.isEmpty(); k++) {
            ItemStack in = c.getItem(k);
            if (in.isEmpty()) {
                c.setItem(k, s.copy());
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameTags(in, s) && in.getCount() < in.getMaxStackSize()) {
                int add = Math.min(s.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(add);
                s.shrink(add);
            }
        }
        return s;
    }

    public static boolean take(Container c, Item item, int n) {
        if (count(c, item) < n) return false;
        for (int k = 0; k < c.getContainerSize() && n > 0; k++) {
            ItemStack s = c.getItem(k);
            if (s.is(item)) {
                int t = Math.min(n, s.getCount());
                s.shrink(t);
                n -= t;
            }
        }
        return true;
    }

    public static int count(Container c, Item item) {
        int n = 0;
        for (int k = 0; k < c.getContainerSize(); k++) if (c.getItem(k).is(item)) n += c.getItem(k).getCount();
        return n;
    }

    private static int countAll(GladerEntity g) {
        int n = 0;
        for (int k = 0; k < g.inventory.getContainerSize(); k++) n += g.inventory.getItem(k).getCount();
        return n;
    }

    private static void takeFromInv(GladerEntity g, Item item) {
        for (int k = 0; k < g.inventory.getContainerSize(); k++)
            if (g.inventory.getItem(k).is(item)) {
                g.inventory.getItem(k).shrink(1);
                return;
            }
    }

    private static void deposit(GladerEntity g, ServerLevel level, BlockPos chestPos) {
        if (!walkTo(g, chestPos, 2.5)) return;
        Container c = container(level, chestPos);
        if (c == null) return;
        for (int k = 0; k < g.inventory.getContainerSize(); k++) g.inventory.setItem(k, insert(c, g.inventory.getItem(k)));
        c.setChanged();
        level.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1F);
    }
}
