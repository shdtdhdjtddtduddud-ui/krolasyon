package com.krolasyon.cille.client;

import com.krolasyon.cille.entity.KabkabaEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Kabkaba battle command keys (all rebindable under Options > Controls > Cille) */
public final class CilleKeys {
    private CilleKeys() {}

    public static final String CATEGORY = "key.categories.cille";

    public static final KeyMapping MENU = key("menu", GLFW.GLFW_KEY_R);
    public static final KeyMapping ATTACK = key("attack", GLFW.GLFW_KEY_V);
    public static final KeyMapping STOP = key("stop", GLFW.GLFW_KEY_G);
    public static final KeyMapping DEFEND = key("defend", GLFW.GLFW_KEY_H);
    public static final KeyMapping RETREAT = key("retreat", GLFW.GLFW_KEY_B);
    public static final KeyMapping MOVE = key("move", GLFW.GLFW_KEY_X);

    /** ALT + 1..7 : abilities */
    public static final KeyMapping[] ABILITIES = {
            alt("black_hole", GLFW.GLFW_KEY_1), alt("blink", GLFW.GLFW_KEY_2), alt("drain", GLFW.GLFW_KEY_3),
            alt("reflect", GLFW.GLFW_KEY_4), alt("shadow", GLFW.GLFW_KEY_5), alt("guard", GLFW.GLFW_KEY_6), alt("slam", GLFW.GLFW_KEY_7)};
    public static final int[] ABILITY_IDS = {KabkabaEntity.BLACK_HOLE, KabkabaEntity.BLINK, KabkabaEntity.DRAIN,
            KabkabaEntity.REFLECT, KabkabaEntity.SHADOW, KabkabaEntity.GUARD, KabkabaEntity.SLAM};

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.cille." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    private static KeyMapping alt(String name, int code) {
        return new KeyMapping("key.cille.ability." + name, KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public static KeyMapping[] all() {
        KeyMapping[] base = {MENU, ATTACK, STOP, DEFEND, RETREAT, MOVE};
        KeyMapping[] out = new KeyMapping[base.length + ABILITIES.length];
        System.arraycopy(base, 0, out, 0, base.length);
        System.arraycopy(ABILITIES, 0, out, base.length, ABILITIES.length);
        return out;
    }
}
