package com.krolasyon.bosses.client.form;

import com.krolasyon.bosses.form.DemonForm;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {
    private KeyBinds() {}

    public static final String CATEGORY = "key.categories.krolasyonbosses";
    private static final int[] DEFAULTS = {GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N};
    public static final KeyMapping[] ABILITIES = new KeyMapping[DemonForm.COUNT];

    static {
        for (int i = 0; i < ABILITIES.length; i++) {
            ABILITIES[i] = new KeyMapping("key.krolasyonbosses." + DemonForm.NAMES[i], InputConstants.Type.KEYSYM, DEFAULTS[i], CATEGORY);
        }
    }
}
