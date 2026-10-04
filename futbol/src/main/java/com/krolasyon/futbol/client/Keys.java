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

    public static final KeyMapping[] ALL = {SHOOT, PASS, TACKLE, SKILL, SUPER, MOVES, MATCH, CELEBRATE};

    public static String name(KeyMapping k) { return k.getTranslatedKeyMessage().getString().toUpperCase(); }
}
