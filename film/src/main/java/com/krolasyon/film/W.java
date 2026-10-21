package com.krolasyon.film;

import com.krolasyon.skyislands.world.Island;
import com.krolasyon.swordvale.world.SwordShape;
import com.krolasyon.swordvale.world.ValeLayout;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Senaryonun kullandığı, dünya tohumundan bulunan yerler. */
public final class W {
    public volatile boolean ready;
    /** Elf kenti merkezi ve taban yüksekliği. */
    public int cx, cz, y0;
    /** Kapı önündeki savaş alanı (temizlenmiş) merkezi ve zemini. */
    public int fx, fz, fy;
    /** Büyük gök adası (yükseltilmiş taban ile). */
    public Island isl;
    public int iy;
    public ValeLayout.Vale vale;
    public SwordShape giant;
    public int vy;
    /** Çekimler arasında paylaşılan noktalar. */
    public final Map<String, Vec3> pts = new ConcurrentHashMap<>();
}
