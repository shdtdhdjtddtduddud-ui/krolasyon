package com.krolasyon.bosses.rpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class RpgKeys {
    private RpgKeys() {}

    public static final String CATEGORY = "Krolasyon RPG";
    public static final KeyMapping CAST = new KeyMapping("Büyü yap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping NEXT = new KeyMapping("Sonraki büyü", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping PREV = new KeyMapping("Önceki büyü", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping CHARACTER = new KeyMapping("Karakter", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping JOURNAL = new KeyMapping("Günlük ve görevler", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping MAP = new KeyMapping("Krallıklar haritası", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
}
