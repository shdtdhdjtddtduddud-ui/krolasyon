package com.krolasyon.beyblade.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

/** Client side calls that differ between game versions (1.20.1 implementation). */
public final class VClient {
    private VClient() {}

    /** one full vertex; {@code n} null means the normal is used as is */
    public static void vertex(VertexConsumer vc, Matrix4f m, @Nullable Matrix3f n, float x, float y, float z, float r, float g, float b, float a,
                              float u, float v, int overlay, int light, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light);
        if (n != null) vc.normal(n, nx, ny, nz).endVertex();
        else vc.normal(nx, ny, nz).endVertex();
    }

    /** partial tick of the frame being rendered */
    public static float partialTick() { return Minecraft.getInstance().getFrameTime(); }
}
