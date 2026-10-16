package com.krolasyon.beyblade.workbench;

import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.item.PartItem;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.registry.ModBlocks;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModMenus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Beyblade Workbench: put a beyblade in the middle slot and its five parts show up around it. Take a part out to
 * remove it from the beyblade, put another part in to fit it; a material in the bottom slot upgrades a part (up to ★5).
 * The part slots are a view of the beyblade's parts, so nothing but the beyblade and the material is ever stored here.
 */
public class WorkbenchMenu extends AbstractContainerMenu {
    public static final int BEY = 0, PART0 = 1, MATERIAL = 6, SIZE = 7;
    /** slot positions in the screen (x, y) */
    public static final int[][] POS = {{62, 76}, {62, 30}, {22, 54}, {102, 54}, {22, 100}, {102, 100}, {62, 128}};
    public static final int INV_X = 48, INV_Y = 166;

    /** upgrade material for each level step (0->1 ... 4->5) and how many it takes */
    public static final Item[] UPGRADE_ITEM = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT};
    public static final int[] UPGRADE_COUNT = {4, 4, 2, 2, 1};

    private final SimpleContainer bench = new SimpleContainer(SIZE);
    private final ContainerLevelAccess access;
    private boolean syncing;

    public WorkbenchMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public WorkbenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.WORKBENCH.get(), id);
        this.access = access;
        addSlot(new BeySlot(bench, BEY, POS[0][0], POS[0][1]));
        for (PartType t : PartType.values()) addSlot(new PartSlot(bench, PART0 + t.ordinal(), POS[1 + t.ordinal()][0], POS[1 + t.ordinal()][1], t));
        addSlot(new Slot(bench, MATERIAL, POS[6][0], POS[6][1]) {
            @Override public boolean mayPlace(ItemStack s) { for (Item i : UPGRADE_ITEM) if (s.is(i)) return true; return false; }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
    }

    public ItemStack bey() { return bench.getItem(BEY); }

    public ItemStack partStack(PartType t) { return bench.getItem(PART0 + t.ordinal()); }

    public ItemStack material() { return bench.getItem(MATERIAL); }

    /** the parts currently in the beyblade (for the stats panel) */
    public Loadout loadout() { return bey().isEmpty() ? null : Loadout.of(bey()); }

    /** the beyblade went in: lay its parts out in the part slots */
    private void unpack() {
        syncing = true;
        ItemStack b = bey();
        Loadout l = b.isEmpty() ? null : Loadout.of(b);
        for (PartType t : PartType.values()) {
            BeyPart p = l == null ? null : l.get(t);
            ItemStack s = ItemStack.EMPTY;
            if (p != null) {
                s = new ItemStack(ModItems.part(p.id()));
                if (l.level(t) > 0) PartItem.setLevel(s, l.level(t));
            }
            bench.setItem(PART0 + t.ordinal(), s);
        }
        syncing = false;
    }

    /** a part slot changed: write it into the beyblade */
    private void pack(PartType t) {
        if (syncing || bey().isEmpty()) return;
        ItemStack s = partStack(t);
        BeyPart p = s.getItem() instanceof PartItem pi ? pi.part() : null;
        Loadout.write(bey(), t, p, p == null ? 0 : PartItem.level(s));
    }

    class BeySlot extends Slot {
        BeySlot(Container c, int i, int x, int y) { super(c, i, x, y); }

        @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof LibraItem; }

        @Override public int getMaxStackSize() { return 1; }

        @Override
        public void setChanged() {
            super.setChanged();
            unpack();
        }
    }

    class PartSlot extends Slot {
        final PartType type;

        PartSlot(Container c, int i, int x, int y, PartType type) {
            super(c, i, x, y);
            this.type = type;
        }

        @Override public boolean mayPlace(ItemStack s) { return !bey().isEmpty() && s.getItem() instanceof PartItem pi && pi.part().type() == type; }

        @Override public int getMaxStackSize() { return 1; }

        @Override
        public void setChanged() {
            super.setChanged();
            pack(type);
        }
    }

    // ---------------- upgrades (buttons 0..4 = part slots) ----------------
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= PartType.values().length) return false;
        PartType t = PartType.values()[id];
        ItemStack s = partStack(t);
        if (!(s.getItem() instanceof PartItem) || bey().isEmpty()) return false;
        int lvl = PartItem.level(s);
        if (lvl >= Loadout.MAX_LEVEL) return false;
        ItemStack m = material();
        if (!m.is(UPGRADE_ITEM[lvl]) || m.getCount() < UPGRADE_COUNT[lvl]) return false;
        m.shrink(UPGRADE_COUNT[lvl]);
        bench.setItem(MATERIAL, m.isEmpty() ? ItemStack.EMPTY : m);
        ItemStack up = s.copy();
        PartItem.setLevel(up, lvl + 1);
        bench.setItem(PART0 + t.ordinal(), up);
        pack(t);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.3F + 0.1F * lvl));
        broadcastChanges();
        return true;
    }

    /** whether the material slot can pay for the next level of this part */
    public boolean canUpgrade(PartType t) {
        ItemStack s = partStack(t);
        if (!(s.getItem() instanceof PartItem)) return false;
        int lvl = PartItem.level(s);
        return lvl < Loadout.MAX_LEVEL && material().is(UPGRADE_ITEM[lvl]) && material().getCount() >= UPGRADE_COUNT[lvl];
    }

    // ---------------- shift click ----------------
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int invStart = SIZE, invEnd = SIZE + 36;
        if (index < SIZE) {
            if (!moveItemStackTo(s, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (s.getItem() instanceof LibraItem && bey().isEmpty()) {
            if (!moveItemStackTo(s, BEY, BEY + 1, false)) return ItemStack.EMPTY;
        } else if (s.getItem() instanceof PartItem pi) {
            int target = PART0 + pi.part().type().ordinal();
            if (bey().isEmpty() || this.slots.get(target).hasItem() || !moveItemStackTo(s, target, target + 1, false)) return ItemStack.EMPTY;
        } else if (this.slots.get(MATERIAL).mayPlace(s)) {
            if (!moveItemStackTo(s, MATERIAL, MATERIAL + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WORKBENCH.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // only the beyblade (with its parts inside) and the material go back; the part slots are just a view
        syncing = true;
        for (int i = PART0; i < PART0 + 5; i++) bench.setItem(i, ItemStack.EMPTY);
        syncing = false;
        access.execute((level, pos) -> clearContainer(player, bench));
    }
}
