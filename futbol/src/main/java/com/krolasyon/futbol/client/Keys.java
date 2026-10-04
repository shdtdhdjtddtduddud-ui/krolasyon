package com.krolasyon.futbol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    private static final String CAT = "key.categories.krolasyonfutbol";

    public static final KeyMapping SHOOT = new KeyMapping("key.krolasyonfutbol.shoot", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CAT);
    public static final KeyMapping PASS = new KeyMapping("key.krolasyonfutbol.pass", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CAT);
    public static final KeyMapping TACKLE = new KeyMapping("key.krolasyonfutbol.tackle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping SKILL = new KeyMapping("key.krolasyonfutbol.skill", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CAT);
    public static final KeyMapping SUPER = new KeyMapping("key.krolasyonfutbol.super", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CAT);
    public static final KeyMapping MOVES = new KeyMapping("key.krolasyonfutbol.moves", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, CAT);
    public static final KeyMapping MATCH = new KeyMapping("key.krolasyonfutbol.match", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CAT);
    public static final KeyMapping CELEBRATE = new KeyMapping("key.krolasyonfutbol.celebrate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CAT);

    public static final KeyMapping SKILL2 = new KeyMapping("key.krolasyonfutbol.skill2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CAT);
    public static final KeyMapping SKILL3 = new KeyMapping("key.krolasyonfutbol.skill3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CAT);
    public static final KeyMapping REQUEST = new KeyMapping("key.krolasyonfutbol.request", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CAT);
    public static final KeyMapping CAMERA = new KeyMapping("key.krolasyonfutbol.camera", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CAT);
    public static final KeyMapping REPLAY = new KeyMapping("key.krolasyonfutbol.replay", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CAT);
    public static final KeyMapping CLUB = new KeyMapping("key.krolasyonfutbol.club", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, CAT);

    public static final KeyMapping[] ALL = {SHOOT, PASS, TACKLE, SKILL, SKILL2, SKILL3, SUPER, REQUEST, MOVES, MATCH, CELEBRATE, CAMERA, REPLAY, CLUB};

    public static String name(KeyMapping k) { return k.getTranslatedKeyMessage().getString().toUpperCase(); }
}
