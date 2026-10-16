package com.krolasyon.alien.client;

import com.krolasyon.alien.network.Net;

/** Client copy of server driven state the HUD needs. */
public final class ClientState {
    private ClientState() {}

    public static boolean hidden;
    public static float breath = 1F;
    public static boolean danger;
    public static boolean holding;

    public static int objStage;
    public static int objFuses;
    public static int objCountdown;

    public static void onObjective(Net.ObjectiveState s) {
        objStage = s.stage();
        objFuses = s.fuses();
        objCountdown = s.countdown();
    }

    public static void onHideState(Net.HideState s) {
        hidden = s.hidden();
        breath = s.breath();
        danger = s.danger();
        holding = s.holding();
    }
}
