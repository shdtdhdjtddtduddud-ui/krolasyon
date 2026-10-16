package com.krolasyon.beyblade.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Client side version dependent calls, Minecraft 1.21.1 implementation. */
public final class VClient {
    private VClient() {}

    public static void vertex(VertexConsumer vc, Matrix4f m, @Nullable Matrix3f n, float x, float y, float z, float r, float g, float b, float a,
                              float u, float v, int overlay, int light, float nx, float ny, float nz) {
        if (n != null) {
            Vector3f t = n.transform(new Vector3f(nx, ny, nz));
            if (t.lengthSquared() > 1.0E-8F) t.normalize();
            nx = t.x;
            ny = t.y;
            nz = t.z;
        }
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }

    public static float partialTick() { return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true); }
}
