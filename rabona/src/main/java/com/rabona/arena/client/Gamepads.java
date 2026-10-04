package com.rabona.arena.client;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.util.ArrayList;
import java.util.List;

/** GLFW ile bagli oyun kumandalarini (Xbox/PlayStation/genel) okur. */
public final class Gamepads {
    public static final int A = GLFW.GLFW_GAMEPAD_BUTTON_A, B = GLFW.GLFW_GAMEPAD_BUTTON_B, X = GLFW.GLFW_GAMEPAD_BUTTON_X,
            Y = GLFW.GLFW_GAMEPAD_BUTTON_Y, LB = GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, RB = GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER,
            BACK = GLFW.GLFW_GAMEPAD_BUTTON_BACK, START = GLFW.GLFW_GAMEPAD_BUTTON_START, UP = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP,
            DOWN = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN, LEFT = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT, RIGHT = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;

    /** Bir kumandanin anlik durumu. */
    public static final class Pad {
        public final int jid;
        public final String name;
        public float lx, ly, rx, ry, lt, rt;
        public final boolean[] down = new boolean[15], prev = new boolean[15];

        Pad(int jid, String name) {
            this.jid = jid;
            this.name = name;
        }

        public boolean pressed(int b) { return down[b] && !prev[b]; }

        public boolean released(int b) { return !down[b] && prev[b]; }
    }

    private static final List<Pad> PADS = new ArrayList<>();
    private static GLFWGamepadState state;
    private static int scan;

    private Gamepads() {}

    public static List<Pad> pads() { return PADS; }

    public static void poll() {
        if (state == null) state = GLFWGamepadState.calloc();
        if (scan-- <= 0) {
            scan = 40;
            List<Integer> ids = new ArrayList<>();
            for (int j = GLFW.GLFW_JOYSTICK_1; j <= GLFW.GLFW_JOYSTICK_LAST; j++) {
                if (GLFW.glfwJoystickPresent(j) && GLFW.glfwJoystickIsGamepad(j)) ids.add(j);
            }
            PADS.removeIf(p -> !ids.contains(p.jid));
            for (int j : ids) {
                if (PADS.stream().noneMatch(p -> p.jid == j)) {
                    String n = GLFW.glfwGetGamepadName(j);
                    PADS.add(new Pad(j, n == null ? "Gamepad" : n));
                }
            }
            PADS.sort((a, b) -> Integer.compare(a.jid, b.jid));
        }
        for (Pad p : PADS) {
            System.arraycopy(p.down, 0, p.prev, 0, p.down.length);
            if (!GLFW.glfwGetGamepadState(p.jid, state)) continue;
            p.lx = dz(state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X));
            p.ly = dz(state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y));
            p.rx = state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X);
            p.ry = state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
            p.lt = (state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER) + 1) / 2;
            p.rt = (state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) + 1) / 2;
            for (int b = 0; b < 15; b++) p.down[b] = state.buttons(b) == GLFW.GLFW_PRESS;
        }
    }

    private static float dz(float v) { return Math.abs(v) < 0.18f ? 0 : v; }
}
