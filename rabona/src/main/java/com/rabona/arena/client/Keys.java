package com.rabona.arena.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    public static final String CAT = "key.categories.rabonaarena";
    public static final KeyMapping SHOOT1 = k("shoot1", GLFW.GLFW_KEY_R);
    public static final KeyMapping SHOOT2 = k("shoot2", GLFW.GLFW_KEY_C);
    public static final KeyMapping SHOOT3 = k("shoot3", GLFW.GLFW_KEY_X);
    public static final KeyMapping PASS = k("pass", GLFW.GLFW_KEY_V);
    public static final KeyMapping LOB = k("lob", GLFW.GLFW_KEY_B);
    public static final KeyMapping TACKLE = k("tackle", GLFW.GLFW_KEY_Z);
    public static final KeyMapping SKILL1 = k("skill1", GLFW.GLFW_KEY_G);
    public static final KeyMapping SKILL2 = k("skill2", GLFW.GLFW_KEY_H);
    public static final KeyMapping SKILL3 = k("skill3", GLFW.GLFW_KEY_J);
    public static final KeyMapping CALL = k("call", GLFW.GLFW_KEY_N);
    public static final KeyMapping SWITCH = k("switch", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping COMBO_UP = k("combo_up", GLFW.GLFW_KEY_UP);
    public static final KeyMapping COMBO_DOWN = k("combo_down", GLFW.GLFW_KEY_DOWN);
    public static final KeyMapping COMBO_LEFT = k("combo_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping COMBO_RIGHT = k("combo_right", GLFW.GLFW_KEY_RIGHT);
    public static final KeyMapping ABILITY = k("ability", GLFW.GLFW_KEY_U);
    public static final KeyMapping CELEBRATE = k("celebrate", GLFW.GLFW_KEY_I);
    public static final KeyMapping CAMERA = k("camera", GLFW.GLFW_KEY_O);
    public static final KeyMapping REPLAY = k("replay", GLFW.GLFW_KEY_L);
    public static final KeyMapping CARDS = k("cards", GLFW.GLFW_KEY_Y);
    public static final KeyMapping MOVES = k("moves", GLFW.GLFW_KEY_K);
    public static final KeyMapping MATCH = k("match", GLFW.GLFW_KEY_M);
    public static final KeyMapping SHOOT = SHOOT1;
    public static final KeyMapping[] SHOTS = {SHOOT1, SHOOT2, SHOOT3};
    public static final KeyMapping[] SKILLS = {SKILL1, SKILL2, SKILL3};
    public static final KeyMapping[] ALL = {SHOOT1, SHOOT2, SHOOT3, PASS, LOB, TACKLE, SKILL1, SKILL2, SKILL3, CALL, SWITCH, ABILITY,
            CELEBRATE, CAMERA, REPLAY, CARDS, MOVES, MATCH, COMBO_UP, COMBO_DOWN, COMBO_LEFT, COMBO_RIGHT};

    private Keys() {}

    private static KeyMapping k(String name, int key) {
        return new KeyMapping("key.rabonaarena." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CAT);
    }
}
