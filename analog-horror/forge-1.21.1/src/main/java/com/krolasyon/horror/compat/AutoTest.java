package com.krolasyon.horror.compat;

import com.mojang.logging.LogUtils;

/** The scripted visual test runs on the 1.20.1 build; on 1.21.1 only the load smoke test is used. */
public final class AutoTest {
    private AutoTest() {}

    public static void init() { LogUtils.getLogger().info("[AUTOTEST] 1.21.1 build loaded"); }
}
