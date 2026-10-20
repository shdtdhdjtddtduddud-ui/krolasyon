package com.krolasyon.skyislands.world;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Elven Realm yüklüyse Sylvaren kentlerinin yerini (yansıma ile) sorar; gök adaları kentlerin üstünde oluşmaz.
 * Kent konumu yalnızca dünya tohumundan hesaplandığı için her chunk aynı kararı verir.
 */
final class ElvenCities {
    private static final int MAX_X = 60, MIN_Z = -118, MAX_Z = 48, PAD = 40;
    private static boolean init;
    private static Method near, cx, cz;

    private ElvenCities() {}

    private static synchronized void init() {
        if (init) return;
        init = true;
        if (!ModList.get().isLoaded("elvenrealm")) return;
        try {
            Class<?> layout = Class.forName("com.krolasyon.elvenrealm.world.CityLayout");
            Class<?> city = Class.forName("com.krolasyon.elvenrealm.world.CityLayout$City");
            near = layout.getMethod("near", long.class, int.class, int.class, int.class, int.class);
            cx = city.getMethod("cx");
            cz = city.getMethod("cz");
        } catch (ReflectiveOperationException e) {
            near = null;
        }
    }

    /** Ada (taç dahil) bir elf kentinin üstüne taşar mı? */
    static boolean blocks(long seed, Island is) {
        init();
        if (near == null) return false;
        int r = is.reach() + PAD;
        try {
            Object[] cities = (Object[]) near.invoke(null, seed, is.cx() - r, is.cz() - r, is.cx() + r, is.cz() + r);
            for (Object c : cities) {
                int dx = is.cx() - (int) cx.invoke(c), dz = is.cz() - (int) cz.invoke(c);
                if (Math.abs(dx) <= MAX_X + r && dz >= MIN_Z - r && dz <= MAX_Z + r) return true;
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            near = null;
        }
        return false;
    }
}
