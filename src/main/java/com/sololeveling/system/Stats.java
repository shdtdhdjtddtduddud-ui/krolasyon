package com.sololeveling.system;

import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/** Turns System stats into vanilla attribute modifiers. */
public final class Stats {
    private Stats() {}

    private static final UUID U_HP = UUID.fromString("5a1c0001-0000-4000-8000-000000000001");
    private static final UUID U_DMG = UUID.fromString("5a1c0001-0000-4000-8000-000000000002");
    private static final UUID U_SPD = UUID.fromString("5a1c0001-0000-4000-8000-000000000003");
    private static final UUID U_ASPD = UUID.fromString("5a1c0001-0000-4000-8000-000000000004");
    private static final UUID U_ARM = UUID.fromString("5a1c0001-0000-4000-8000-000000000005");
    private static final UUID U_REACH = UUID.fromString("5a1c0001-0000-4000-8000-000000000006");
    private static final UUID U_KB = UUID.fromString("5a1c0001-0000-4000-8000-000000000007");
    private static final UUID U_LUCK = UUID.fromString("5a1c0001-0000-4000-8000-000000000008");

    private static void set(Player p, Attribute a, UUID id, String name, double v, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(id);
        if (old != null) {
            if (old.getAmount() == v && old.getOperation() == op) return;
            inst.removeModifier(id);
        }
        if (v != 0) inst.addTransientModifier(new AttributeModifier(id, name, v, op));
    }

    public static void apply(Player p) {
        SLPlayer d = ModCaps.get(p);
        float hpBefore = p.getHealth();
        set(p, Attributes.MAX_HEALTH, U_HP, "sl_vit", (d.vit() - 10) * 2.0 + d.level * 1.0, AttributeModifier.Operation.ADDITION);
        set(p, Attributes.ATTACK_DAMAGE, U_DMG, "sl_str", (d.str() - 10) * 0.35 + d.level * 0.12, AttributeModifier.Operation.ADDITION);
        set(p, Attributes.MOVEMENT_SPEED, U_SPD, "sl_agi", Math.min(1.0, (d.agi() - 10) * 0.004 + d.level * 0.0005), AttributeModifier.Operation.MULTIPLY_BASE);
        set(p, Attributes.ATTACK_SPEED, U_ASPD, "sl_aspd", (d.agi() - 10) * 0.006, AttributeModifier.Operation.MULTIPLY_BASE);
        set(p, Attributes.ARMOR, U_ARM, "sl_arm", (d.vit() - 10) * 0.15, AttributeModifier.Operation.ADDITION);
        set(p, Attributes.KNOCKBACK_RESISTANCE, U_KB, "sl_kb", Math.min(0.5, d.level / 400.0), AttributeModifier.Operation.ADDITION);
        set(p, Attributes.LUCK, U_LUCK, "sl_luck", (d.sense() - 10) * 0.1, AttributeModifier.Operation.ADDITION);
        set(p, ForgeMod.ENTITY_REACH.get(), U_REACH, "sl_reach", (d.sense() - 10) * 0.03, AttributeModifier.Operation.ADDITION);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        if (d.mana > d.maxMana()) d.mana = d.maxMana();
    }
}
