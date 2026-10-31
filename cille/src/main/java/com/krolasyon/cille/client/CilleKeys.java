package com.krolasyon.cille.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Cille battle command keys (all rebindable under Options > Controls > Cille) */
public final class CilleKeys {
    private CilleKeys() {}

    public static final String CATEGORY = "key.categories.cille";

    public static final KeyMapping MENU = key("menu", GLFW.GLFW_KEY_R);
    public static final KeyMapping SELECT = key("select", GLFW.GLFW_KEY_N);
    public static final KeyMapping ATTACK = key("attack", GLFW.GLFW_KEY_V);
    public static final KeyMapping STOP = key("stop", GLFW.GLFW_KEY_G);
    public static final KeyMapping DEFEND = key("defend", GLFW.GLFW_KEY_H);
    public static final KeyMapping RETREAT = key("retreat", GLFW.GLFW_KEY_B);
    public static final KeyMapping MOVE = key("move", GLFW.GLFW_KEY_X);

    /** ALT + 1..9, ALT + 0 : ability slots of the selected Cille */
    public static final KeyMapping[] ABILITIES = new KeyMapping[10];

    static {
        int[] codes = {GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
                GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0};
        for (int i = 0; i < 10; i++)
            ABILITIES[i] = new KeyMapping("key.cille.slot" + (i + 1), KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, codes[i], CATEGORY);
    }

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.cille." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public static KeyMapping[] all() {
        KeyMapping[] base = {MENU, SELECT, ATTACK, STOP, DEFEND, RETREAT, MOVE};
        KeyMapping[] out = new KeyMapping[base.length + ABILITIES.length];
        System.arraycopy(base, 0, out, 0, base.length);
        System.arraycopy(ABILITIES, 0, out, base.length, ABILITIES.length);
        return out;
    }

    public static String slotLabel(int i) { return i < 9 ? "ALT+" + (i + 1) : (i == 9 ? "ALT+0" : ""); }
}
