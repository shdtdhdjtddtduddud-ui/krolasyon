package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Ability;
import com.krolasyon.omnitrix.Alien;
import com.krolasyon.omnitrix.OmniData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/** Client mirror: full data of the local player + the appearance of every tracked player. */
public final class ClientState {
    public static final OmniData SELF = new OmniData();

    public static final class Look {
        public int alien = -1;
        public boolean wearing, blocking;
        public int flags;
        public int cast = -1;
        public long castTime, transformTime = -1000, detransformTime = -1000;
        /** alien shown during the revert animation */
        public int lastAlien = -1;

        public Alien alien() { return Alien.byId(alien); }

        public boolean flying() { return (flags & 1) != 0; }

        public boolean phase() { return (flags & 2) != 0; }

        public boolean puddle() { return (flags & 4) != 0; }

        public boolean overdrive() { return (flags & 8) != 0; }

        public boolean blades() { return (flags & 16) != 0; }

        /** progress 0..1 of the current ability animation, -1 if none */
        public float castProgress(float pt) {
            Ability a = Ability.byId(cast);
            Minecraft mc = Minecraft.getInstance();
            if (a == null || mc.level == null) return -1;
            float t = (mc.level.getGameTime() - castTime + pt) / a.duration;
            return t >= 0 && t <= 1 ? t : -1;
        }
    }

    private static final Map<Integer, Look> LOOKS = new HashMap<>();
    private static final Look NONE = new Look();

    private ClientState() {}

    public static Look look(Entity e) {
        Look l = LOOKS.get(e.getId());
        return l == null ? NONE : l;
    }

    public static void onSync(CompoundTag tag) { SELF.load(tag); }

    public static void onLook(int entity, CompoundTag t) {
        Look l = LOOKS.computeIfAbsent(entity, k -> new Look());
        int prev = l.alien;
        l.alien = t.getByte("A");
        if (prev >= 0 && l.alien < 0) l.lastAlien = prev;
        l.wearing = t.getBoolean("W");
        l.blocking = t.getBoolean("B");
        l.flags = t.getByte("F");
        int cast = t.getByte("C");
        long ct = t.getLong("CT");
        if (cast >= 0 && ct != l.castTime) {
            l.cast = cast;
            l.castTime = ct;
        }
        l.transformTime = t.getLong("TT");
        l.detransformTime = t.getLong("DT");
    }

    public static void clear() {
        LOOKS.clear();
        SELF.load(new CompoundTag());
        SELF.alien = -1;
    }
}
