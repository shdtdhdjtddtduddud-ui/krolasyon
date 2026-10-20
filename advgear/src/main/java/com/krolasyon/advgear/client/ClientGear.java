package com.krolasyon.advgear.client;

import com.krolasyon.advgear.gear.GearType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** İstemcide bilinen kuşanımlar (görünüm için). */
public final class ClientGear {
    private static final Map<Integer, ItemStack[]> GEAR = new HashMap<>();
    private static final ItemStack[] NONE = new ItemStack[GearType.SLOTS];

    static {
        for (int i = 0; i < NONE.length; i++) NONE[i] = ItemStack.EMPTY;
    }

    private ClientGear() {}

    public static void set(int id, ItemStack[] s) {
        GEAR.put(id, s);
    }

    public static ItemStack[] of(Entity e) {
        return GEAR.getOrDefault(e.getId(), NONE);
    }

    public static ItemStack get(Entity e, int slot) {
        return of(e)[slot];
    }

    public static void clear() {
        GEAR.clear();
    }
}
