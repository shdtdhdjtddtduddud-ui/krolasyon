package com.krolasyon.advgear.gear;

import com.krolasyon.advgear.network.Net;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Sunucu tarafı kuşanım envanteri: oyuncunun kalıcı verisinde saklanır, değişince izleyen istemcilere eşitlenir. */
public final class GearData {
    private static final Map<UUID, Gear> SERVER = new HashMap<>();

    private GearData() {}

    public static final class Gear extends SimpleContainer {
        private final Player owner;
        private boolean loading;

        Gear(Player owner) {
            super(GearType.SLOTS);
            this.owner = owner;
            loading = true;
            ListTag list = root(owner).getList("Gear", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                int slot = t.getByte("Slot");
                if (slot >= 0 && slot < GearType.SLOTS) setItem(slot, ItemStack.of(t));
            }
            loading = false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof GearItem g && g.type == GearType.ofSlot(slot);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (loading) return;
            save();
            if (owner instanceof ServerPlayer sp) Net.syncGear(sp);
        }

        public void save() {
            ListTag list = new ListTag();
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack s = getItem(i);
                if (s.isEmpty()) continue;
                CompoundTag t = s.save(new CompoundTag());
                t.putByte("Slot", (byte) i);
                list.add(t);
            }
            root(owner).put("Gear", list);
        }

        public Player owner() {
            return owner;
        }
    }

    private static CompoundTag root(Player p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains("advgear")) persisted.put("advgear", new CompoundTag());
        return persisted.getCompound("advgear");
    }

    /** Oyuncunun kuşanımı (yalnızca sunucuda). */
    public static Gear get(Player p) {
        Gear g = SERVER.get(p.getUUID());
        if (g == null || g.owner != p) {
            g = new Gear(p);
            SERVER.put(p.getUUID(), g);
        }
        return g;
    }

    public static void forget(Player p) {
        Gear g = SERVER.remove(p.getUUID());
        if (g != null && g.owner == p) g.save();
    }

    public static boolean has(Player p, Item item) {
        Gear g = get(p);
        for (int i = 0; i < GearType.SLOTS; i++) if (g.getItem(i).is(item)) return true;
        return false;
    }

    public static ItemStack back(Player p) {
        return get(p).getItem(4);
    }

    /** Eşyayı uygun boş yuvaya takar. */
    public static boolean equip(Player p, ItemStack stack) {
        if (!(stack.getItem() instanceof GearItem gi)) return false;
        Gear g = get(p);
        for (int i = 0; i < GearType.SLOTS; i++) {
            if (GearType.ofSlot(i) == gi.type && g.getItem(i).isEmpty()) {
                g.setItem(i, stack.copy());
                return true;
            }
        }
        return false;
    }
}
