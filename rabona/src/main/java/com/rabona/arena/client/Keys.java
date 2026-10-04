package com.rabona.arena.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    public static final String CAT = "key.categories.rabonaarena";
    public static final KeyMapping SHOOT = k("shoot", GLFW.GLFW_KEY_R);
    public static final KeyMapping PASS = k("pass", GLFW.GLFW_KEY_V);
    public static final KeyMapping LOB = k("lob", GLFW.GLFW_KEY_B);
    public static final KeyMapping TACKLE = k("tackle", GLFW.GLFW_KEY_Z);
    public static final KeyMapping SKILL1 = k("skill1", GLFW.GLFW_KEY_G);
    public static final KeyMapping SKILL2 = k("skill2", GLFW.GLFW_KEY_H);
    public static final KeyMapping SKILL3 = k("skill3", GLFW.GLFW_KEY_J);
    public static final KeyMapping SKILL4 = k("skill4", GLFW.GLFW_KEY_N);
    public static final KeyMapping ABILITY = k("ability", GLFW.GLFW_KEY_U);
    public static final KeyMapping STYLE = k("style", GLFW.GLFW_KEY_Y);
    public static final KeyMapping CELEBRATE = k("celebrate", GLFW.GLFW_KEY_I);
    public static final KeyMapping MOVES = k("moves", GLFW.GLFW_KEY_K);
    public static final KeyMapping MATCH = k("match", GLFW.GLFW_KEY_M);
    public static final KeyMapping[] SKILLS = {SKILL1, SKILL2, SKILL3, SKILL4};
    public static final KeyMapping[] ALL = {SHOOT, PASS, LOB, TACKLE, SKILL1, SKILL2, SKILL3, SKILL4, ABILITY, STYLE, CELEBRATE, MOVES, MATCH};

    private Keys() {}

    private static KeyMapping k(String name, int key) {
        return new KeyMapping("key.rabonaarena." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CAT);
    }
}
