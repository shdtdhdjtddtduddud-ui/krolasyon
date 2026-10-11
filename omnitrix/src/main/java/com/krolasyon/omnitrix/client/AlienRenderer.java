package com.krolasyon.omnitrix.client;

import com.krolasyon.omnitrix.Alien;
import com.krolasyon.omnitrix.Omnitrix;
import com.krolasyon.omnitrix.Transform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.IdentityHashMap;
import java.util.Map;

/** Draws a transformed player as its Higgsfield alien mesh with CPU skinning and a toon outline. */
public final class AlienRenderer {
    private AlienRenderer() {}

    private static final class Work {
        final Pose pose;
        final Matrix4f[] world;
        final float[] sp, sn;

        Work(OmxModel m) {
            pose = new Pose(m.names.length);
            world = new Matrix4f[m.names.length];
            for (int i = 0; i < world.length; i++) world[i] = new Matrix4f();
            sp = new float[m.vertexCount() * 3];
            sn = new float[m.vertexCount() * 3];
        }
    }

    private static final Map<OmxModel, Work> WORK = new IdentityHashMap<>();

    public static ResourceLocation texture(Alien a) { return new ResourceLocation(Omnitrix.MODID, "textures/alien/" + a.id + ".png"); }

    /** compute the skinned vertex positions/normals of the model for the given pose (model space, blocks) */
    static Work skin(OmxModel m, Pose p) {
        Work w = WORK.computeIfAbsent(m, Work::new);
        boolean[] done = new boolean[m.names.length];
        for (int i = 0; i < m.names.length; i++) boneMatrix(m, p, w.world, done, i);
        Matrix4f root = new Matrix4f();
        float mid = (m.minY + m.maxY) * 0.5F;
        root.translate(p.rootX, p.rootY, p.rootZ);
        root.rotateY(p.rootYaw * Mth.DEG_TO_RAD);
        root.translate(0, mid, 0);
        root.rotateX(p.rootPitch);
        root.rotateZ(p.rootRoll);
        root.translate(0, -mid, 0);
        root.scale(p.scaleX, p.scaleY, p.scaleZ);
        Matrix4f[] fin = new Matrix4f[w.world.length];
        Matrix3f[] nm = new Matrix3f[w.world.length];
        for (int i = 0; i < fin.length; i++) {
            fin[i] = new Matrix4f(root).mul(w.world[i]);
            nm[i] = new Matrix3f(fin[i]).invert().transpose();
        }
        Vector4f a = new Vector4f(), b = new Vector4f();
        Vector3f na = new Vector3f(), nb = new Vector3f();
        int n = m.vertexCount();
        for (int i = 0; i < n; i++) {
            float x = m.pos[i * 3], y = m.pos[i * 3 + 1], z = m.pos[i * 3 + 2];
            float nx = m.nrm[i * 3], ny = m.nrm[i * 3 + 1], nz = m.nrm[i * 3 + 2];
            int i0 = m.b0[i] & 0xFF, i1 = m.b1[i] & 0xFF;
            float w0 = m.w0[i];
            fin[i0].transform(a.set(x, y, z, 1));
            nm[i0].transform(na.set(nx, ny, nz));
            if (w0 < 0.999F && i1 != i0) {
                fin[i1].transform(b.set(x, y, z, 1));
                nm[i1].transform(nb.set(nx, ny, nz));
                a.lerp(b, 1 - w0);
                na.lerp(nb, 1 - w0);
            }
            na.normalize();
            w.sp[i * 3] = a.x; w.sp[i * 3 + 1] = a.y; w.sp[i * 3 + 2] = a.z;
            w.sn[i * 3] = na.x; w.sn[i * 3 + 1] = na.y; w.sn[i * 3 + 2] = na.z;
        }
        return w;
    }

    private static void boneMatrix(OmxModel m, Pose p, Matrix4f[] world, boolean[] done, int i) {
        if (done[i]) return;
        int par = m.parent[i];
        Matrix4f mat = world[i].identity();
        if (par >= 0) {
            boneMatrix(m, p, world, done, par);
            mat.set(world[par]);
        }
        float px = m.pivot[i * 3], py = m.pivot[i * 3 + 1], pz = m.pivot[i * 3 + 2];
        mat.translate(px + p.tx[i], py + p.ty[i], pz + p.tz[i]);
        mat.rotate(new Quaternionf().rotationZYX(p.rz[i], p.ry[i], p.rx[i]));
        mat.scale(p.s[i]);
        mat.translate(-px, -py, -pz);
        done[i] = true;
    }

    /** full alien render at the player's position (poseStack already at the entity origin) */
    public static void render(AbstractClientPlayer pl, Alien alien, ClientState.Look look, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        OmxModel m = OmxModel.get(alien.model());
        if (m == null) return;
        Work w = WORK.computeIfAbsent(m, Work::new);
        AlienAnimator.animate(m, w.pose, alien, pl, look, pt);
        skin(m, w.pose);
        Minecraft mc = Minecraft.getInstance();
        float since = mc.level == null ? 99 : mc.level.getGameTime() - look.transformTime + pt;
        // grow-in after the omnitrix flash
        float grow = 1F, tint = 0F;
        if (since < Transform.TRANSFORM_ANIM) {
            float t = Mth.clamp((since - 12) / (Transform.TRANSFORM_ANIM - 12F), 0, 1);
            grow = t < 0.7F ? Mth.lerp(t / 0.7F, 0.25F, 1.12F) : Mth.lerp((t - 0.7F) / 0.3F, 1.12F, 1F);
            tint = 1 - t;
        }
        float bodyYaw = Mth.rotLerp(pt, pl.yBodyRotO, pl.yBodyRot);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        ps.scale(grow, grow, grow);
        if (pl.isFallFlying()) ps.mulPose(Axis.XP.rotationDegrees(70));
        int overlay = LivingEntityRenderer.getOverlayCoords(pl, 0F);
        int lt = alien == Alien.HEATBLAST ? 0xF000F0 : light;
        float r = Mth.lerp(tint, 1F, 0.35F), g = 1F, b = Mth.lerp(tint, 1F, 0.45F);
        boolean ghost = look.phase() || (alien == Alien.GHOSTFREAK && look.blocking);
        boolean invisible = pl.isInvisible() && !ghost;
        if (!invisible) {
            if (!ghost) drawMesh(m, w, ps, buf.getBuffer(OmniRenderTypes.OUTLINE), lt, overlay, 0F, 0F, 0F, 1F, 0.018F * alien.height / 2F);
            RenderType rt = ghost ? RenderType.entityTranslucent(texture(alien)) : RenderType.entityCutoutNoCull(texture(alien));
            drawMesh(m, w, ps, buf.getBuffer(rt), lt, overlay, r, g, b, ghost ? 0.35F : 1F, 0F);
            AlienExtras.render(pl, alien, look, m, w.pose, w.sp, ps, buf, lt, pt);
        }
        ps.popPose();
    }

    public static final ResourceLocation WATCH_MODEL = new ResourceLocation(Omnitrix.MODID, "models/alien/omnitrix.omx");
    public static final ResourceLocation WATCH_TEX = new ResourceLocation(Omnitrix.MODID, "textures/alien/omnitrix.png");

    /** the Higgsfield Omnitrix mesh, centred on its bounding box and scaled to `size` blocks tall */
    public static boolean renderWatch(PoseStack ps, MultiBufferSource buf, int light, float size, boolean outline) {
        OmxModel m = OmxModel.get(WATCH_MODEL);
        if (m == null) return false;
        Work w = WORK.computeIfAbsent(m, Work::new);
        w.pose.reset();
        skin(m, w.pose);
        ps.pushPose();
        float s = size / Math.max(0.01F, m.maxY - m.minY);
        ps.scale(s, s, s);
        ps.translate(0, -(m.maxY + m.minY) / 2, 0);
        if (outline) drawMesh(m, w, ps, buf.getBuffer(OmniRenderTypes.OUTLINE), light, OverlayTexture.NO_OVERLAY, 0, 0, 0, 1, 0.02F);
        drawMesh(m, w, ps, buf.getBuffer(RenderType.entityCutoutNoCull(WATCH_TEX)), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1, 0);
        ps.popPose();
        return true;
    }

    /** rest-pose alien for GUIs; hologram = flat green (omnitrix dial projection) */
    public static void renderPreview(Alien alien, PoseStack ps, MultiBufferSource buf, float height, float age, boolean hologram) {
        OmxModel m = OmxModel.get(alien.model());
        if (m == null) return;
        Work w = WORK.computeIfAbsent(m, Work::new);
        w.pose.reset();
        int arm = m.bone("arm_l"), armR = m.bone("arm_r");
        if (arm >= 0) w.pose.rz[arm] = 0.08F + Mth.sin(age * 0.08F) * 0.04F;
        if (armR >= 0) w.pose.rz[armR] = -0.08F - Mth.sin(age * 0.08F) * 0.04F;
        skin(m, w.pose);
        ps.pushPose();
        float s = height / Math.max(0.01F, m.maxY - m.minY);
        ps.scale(s, s, s);
        ps.translate(0, -(m.maxY + m.minY) / 2, 0);
        if (hologram) {
            drawMesh(m, w, ps, buf.getBuffer(RenderType.entityTranslucent(texture(alien))), 0xF000F0, OverlayTexture.NO_OVERLAY, 0.25F, 1F, 0.3F, 0.85F, 0);
        } else {
            drawMesh(m, w, ps, buf.getBuffer(OmniRenderTypes.OUTLINE), 0xF000F0, OverlayTexture.NO_OVERLAY, 0, 0, 0, 1, 0.015F * alien.height);
            drawMesh(m, w, ps, buf.getBuffer(RenderType.entityCutoutNoCull(texture(alien))), 0xF000F0, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1, 0);
        }
        ps.popPose();
    }

    /** a subset of the mesh (bones in the mask) for the first person arm */
    public static void renderArm(AbstractClientPlayer pl, Alien alien, ClientState.Look look, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        OmxModel m = OmxModel.get(alien.model());
        if (m == null) return;
        Work w = WORK.computeIfAbsent(m, Work::new);
        AlienAnimator.animate(m, w.pose, alien, pl, look, pt);
        int arm = m.bone("arm_r"), fore = m.bone("forearm_r");
        if (arm < 0) return;
        // hold the arm straight forward for the camera
        w.pose.rx[arm] = -Mth.HALF_PI * 0.92F;
        w.pose.ry[arm] = 0.05F;
        w.pose.rz[arm] = 0;
        w.pose.rootPitch = w.pose.rootRoll = w.pose.rootYaw = 0;
        w.pose.scaleX = w.pose.scaleY = w.pose.scaleZ = 1;
        if (fore >= 0) w.pose.rx[fore] = Math.min(w.pose.rx[fore], -0.1F);
        skin(m, w.pose);
        boolean[] mask = new boolean[m.names.length];
        for (int i = 0; i < m.names.length; i++) {
            String n = m.names[i];
            mask[i] = n.equals("arm_r") || n.equals("forearm_r") || n.equals("arm2_r") || n.equals("forearm2_r");
        }
        float sx = m.pivot[arm * 3], sy = m.pivot[arm * 3 + 1], sz = m.pivot[arm * 3 + 2];
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180));
        float scale = 0.75F / Math.max(0.6F, alien.height * 0.45F);
        ps.scale(scale, scale, scale);
        ps.translate(-sx, -sy, -sz);
        int lt = alien == Alien.HEATBLAST ? 0xF000F0 : light;
        drawMesh(m, w, ps, buf.getBuffer(OmniRenderTypes.OUTLINE), lt, OverlayTexture.NO_OVERLAY, 0, 0, 0, 1, 0.02F, mask);
        drawMesh(m, w, ps, buf.getBuffer(RenderType.entityCutoutNoCull(texture(alien))), lt, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1, 0, mask);
        ps.popPose();
    }

    static void drawMesh(OmxModel m, Work w, PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a, float inflate) {
        drawMesh(m, w, ps, vc, light, overlay, r, g, b, a, inflate, null);
    }

    static void drawMesh(OmxModel m, Work w, PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a, float inflate, boolean[] mask) {
        Matrix4f mat = ps.last().pose();
        Matrix3f nmat = ps.last().normal();
        int[] t = m.tris;
        for (int i = 0; i < t.length; i += 3) {
            if (mask != null && !(mask[m.b0[t[i]] & 0xFF] && mask[m.b0[t[i + 1]] & 0xFF] && mask[m.b0[t[i + 2]] & 0xFF])) continue;
            vert(m, w, vc, mat, nmat, t[i], light, overlay, r, g, b, a, inflate);
            vert(m, w, vc, mat, nmat, t[i + 1], light, overlay, r, g, b, a, inflate);
            vert(m, w, vc, mat, nmat, t[i + 2], light, overlay, r, g, b, a, inflate);
            vert(m, w, vc, mat, nmat, t[i + 2], light, overlay, r, g, b, a, inflate);
        }
    }

    private static void vert(OmxModel m, Work w, VertexConsumer vc, Matrix4f mat, Matrix3f nmat, int i, int light, int overlay,
                             float r, float g, float b, float a, float inflate) {
        float nx = w.sn[i * 3], ny = w.sn[i * 3 + 1], nz = w.sn[i * 3 + 2];
        vc.vertex(mat, w.sp[i * 3] + nx * inflate, w.sp[i * 3 + 1] + ny * inflate, w.sp[i * 3 + 2] + nz * inflate)
                .color(r, g, b, a).uv(m.uv[i * 2], m.uv[i * 2 + 1]).overlayCoords(overlay).uv2(light)
                .normal(nmat, nx, ny, nz).endVertex();
    }

    /** skinned position of a bone pivot (for effects attached to hands etc.), model space before the yaw rotation */
    public static Vector3f bonePoint(AbstractClientPlayer pl, Alien alien, String bone, float along) {
        OmxModel m = OmxModel.get(alien.model());
        if (m == null) return null;
        Work w = WORK.get(m);
        int i = m.bone(bone);
        if (w == null || i < 0) return null;
        Vector4f v = new Vector4f(m.pivot[i * 3], m.pivot[i * 3 + 1] - along, m.pivot[i * 3 + 2], 1);
        w.world[i].transform(v);
        return new Vector3f(v.x, v.y, v.z);
    }
}
