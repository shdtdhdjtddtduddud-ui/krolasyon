package com.krolasyon.labirent.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.joml.Matrix4f;

/** Overworld sky, until the sun goes out: then just a flat grey nothing. */
public class MazeSkyEffects extends DimensionSpecialEffects.OverworldEffects {
    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partial, PoseStack pose, Camera camera, Matrix4f proj, boolean foggy, Runnable setupFog) {
        return ClientState.sunGone();
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partial, PoseStack pose, double x, double y, double z, Matrix4f proj) {
        return ClientState.sunGone();
    }
}
