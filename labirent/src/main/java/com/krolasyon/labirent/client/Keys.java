package com.krolasyon.labirent.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    public static final String CAT = "key.categories.labirent";
    public static final KeyMapping MAP = new KeyMapping("key.labirent.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CAT);
    public static final KeyMapping CAPTAIN = new KeyMapping("key.labirent.captain", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CAT);
    public static final KeyMapping JOURNAL = new KeyMapping("key.labirent.journal", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CAT);
    public static final KeyMapping MINIMAP = new KeyMapping("key.labirent.minimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CAT);
}
