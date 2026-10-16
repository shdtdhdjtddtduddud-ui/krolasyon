package com.krolasyon.beyblade.parts;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.item.LibraItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The five parts assembled in one beyblade, with their upgrade levels (0..5). Stored on the beyblade item as
 * {Parts:{Bolt:"bolt_x",BoltLv:2,...}}; a missing key means the stock part, an empty id means the slot is empty.
 */
public final class Loadout {
    public static final int MAX_LEVEL = 5;
    /** stat bonus per upgrade level */
    public static final float LEVEL_BONUS = 0.07F;

    private final BeyPart[] parts = new BeyPart[PartType.values().length];
    private final int[] levels = new int[PartType.values().length];

    @Nullable public BeyPart get(PartType t) { return parts[t.ordinal()]; }
    public int level(PartType t) { return levels[t.ordinal()]; }

    public void set(PartType t, @Nullable BeyPart p, int lvl) {
        parts[t.ordinal()] = p;
        levels[t.ordinal()] = p == null ? 0 : Math.max(0, Math.min(MAX_LEVEL, lvl));
    }

    public boolean complete() {
        for (BeyPart p : parts) if (p == null) return false;
        return true;
    }

    public static Loadout stock(String kind) {
        Loadout l = new Loadout();
        String[] ids = BeyPart.stock(kind);
        for (PartType t : PartType.values()) l.set(t, BeyPart.get(ids[t.ordinal()]), 0);
        return l;
    }

    /** the beyblade item's parts (stock parts where nothing was changed) */
    public static Loadout of(ItemStack bey) {
        String kind = bey.getItem() instanceof LibraItem li ? li.kind() : "libra";
        Loadout l = stock(kind);
        CompoundTag t = V.tag(bey);
        if (t == null || !t.contains("Parts")) return l;
        CompoundTag p = t.getCompound("Parts");
        for (PartType ty : PartType.values()) {
            if (!p.contains(ty.key)) continue;
            String id = p.getString(ty.key);
            l.set(ty, id.isEmpty() ? null : BeyPart.get(id), p.getInt(ty.key + "Lv"));
        }
        return l;
    }

    /** writes one slot into the beyblade item */
    public static void write(ItemStack bey, PartType ty, @Nullable BeyPart part, int lvl) {
        V.editTag(bey, t -> {
            CompoundTag p = t.getCompound("Parts");
            p.putString(ty.key, part == null ? "" : part.id());
            p.putInt(ty.key + "Lv", part == null ? 0 : lvl);
            t.put("Parts", p);
        });
    }

    /** "wheel;track;tip;ring;bolt" for the client renderer */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (PartType t : new PartType[]{PartType.WHEEL, PartType.TRACK, PartType.TIP, PartType.RING, PartType.BOLT}) {
            if (sb.length() > 0) sb.append(';');
            BeyPart p = get(t);
            sb.append(p == null ? "" : p.id());
        }
        return sb.toString();
    }

    public static Loadout decode(String kind, String s) {
        Loadout l = stock(kind);
        if (s == null || s.isEmpty()) return l;
        String[] a = s.split(";", -1);
        PartType[] order = {PartType.WHEEL, PartType.TRACK, PartType.TIP, PartType.RING, PartType.BOLT};
        for (int i = 0; i < order.length && i < a.length; i++) if (!a[i].isEmpty() && BeyPart.get(a[i]) != null) l.set(order[i], BeyPart.get(a[i]), 0);
        return l;
    }

    // ---------------- stats ----------------
    private float sum(java.util.function.ToIntFunction<BeyPart> f) {
        float s = 0;
        for (int i = 0; i < parts.length; i++) if (parts[i] != null) s += f.applyAsInt(parts[i]) * (1F + LEVEL_BONUS * levels[i]);
        return s;
    }

    public float attack() { return sum(BeyPart::attack); }
    public float defense() { return sum(BeyPart::defense); }
    public float stamina() { return sum(BeyPart::stamina); }
    public float speed() { return sum(BeyPart::speed); }

    public float weight() {
        float w = 0;
        for (int i = 0; i < parts.length; i++) if (parts[i] != null) w += parts[i].weight() * (1F + 0.02F * levels[i]);
        return w;
    }

    /** spin track height in tenths of a millimetre (145 = 14.5 mm) */
    public int height() {
        BeyPart t = get(PartType.TRACK);
        return t == null ? 145 : t.height();
    }

    public BeyPart.Tip tip() {
        BeyPart t = get(PartType.TIP);
        return t == null ? BeyPart.Tip.NONE : t.tip();
    }

    public BeyPart.Trait trackTrait() {
        BeyPart t = get(PartType.TRACK);
        return t == null ? BeyPart.Trait.NONE : t.trait();
    }

    /** stat total -> multiplier around 1 (35 is a typical stock total) */
    public static float mul(float stat) { return Math.max(0.7F, Math.min(1.5F, 1F + (stat - 35F) / 140F)); }

    /** which battle type this combination leans to: 0 attack, 1 defense, 2 stamina, 3 balance */
    public int leaning() {
        float a = attack() + speed() * 0.4F, d = defense(), s = stamina();
        float max = Math.max(a, Math.max(d, s)), min = Math.min(a, Math.min(d, s));
        if (max - min < 10) return 3;
        return max == a ? 0 : max == d ? 1 : 2;
    }

    public int partsRarityScore() {
        int r = 0;
        for (BeyPart p : parts) if (p != null) r += p.rarity();
        return r;
    }
}
