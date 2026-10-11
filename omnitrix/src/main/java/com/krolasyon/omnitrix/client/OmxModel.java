package com.krolasyon.omnitrix.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Skinned triangle mesh exported by gen/build_models.py from the Higgsfield GLBs. */
public final class OmxModel {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, OmxModel> CACHE = new HashMap<>();

    public final String[] names;
    public final int[] parent;
    public final float[] pivot;
    public final float[] pos, uv, nrm, w0;
    public final byte[] b0, b1;
    public final int[] tris;
    public final float minY, maxY;

    private OmxModel(ByteBuffer b) {
        if (b.getInt() != 0x31584D4F) throw new IllegalStateException("not an OMX1 model");
        int n = b.getInt();
        names = new String[n];
        parent = new int[n];
        pivot = new float[n * 3];
        for (int i = 0; i < n; i++) {
            byte[] s = new byte[b.getShort()];
            b.get(s);
            names[i] = new String(s, StandardCharsets.UTF_8);
            parent[i] = b.getInt();
            pivot[i * 3] = b.getFloat();
            pivot[i * 3 + 1] = b.getFloat();
            pivot[i * 3 + 2] = b.getFloat();
        }
        int v = b.getInt();
        pos = new float[v * 3];
        uv = new float[v * 2];
        nrm = new float[v * 3];
        b0 = new byte[v];
        b1 = new byte[v];
        w0 = new float[v];
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i < v; i++) {
            pos[i * 3] = b.getFloat();
            pos[i * 3 + 1] = b.getFloat();
            pos[i * 3 + 2] = b.getFloat();
            uv[i * 2] = b.getFloat();
            uv[i * 2 + 1] = b.getFloat();
            nrm[i * 3] = b.getFloat();
            nrm[i * 3 + 1] = b.getFloat();
            nrm[i * 3 + 2] = b.getFloat();
            b0[i] = b.get();
            b1[i] = b.get();
            w0[i] = b.getFloat();
            lo = Math.min(lo, pos[i * 3 + 1]);
            hi = Math.max(hi, pos[i * 3 + 1]);
        }
        minY = lo;
        maxY = hi;
        int t = b.getInt();
        tris = new int[t * 3];
        for (int i = 0; i < t * 3; i++) tris[i] = b.getInt();
    }

    public int bone(String name) {
        for (int i = 0; i < names.length; i++) if (names[i].equals(name)) return i;
        return -1;
    }

    public int vertexCount() { return w0.length; }

    /** null if the resource is missing or broken (logged once) */
    public static OmxModel get(ResourceLocation rl) {
        if (CACHE.containsKey(rl)) return CACHE.get(rl);
        OmxModel m = null;
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(rl).open()) {
            m = new OmxModel(ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN));
            LOG.info("[omnitrix] loaded model {} ({} verts, {} bones)", rl, m.vertexCount(), m.names.length);
        } catch (Exception e) {
            LOG.error("[omnitrix] failed to load model {}", rl, e);
        }
        CACHE.put(rl, m);
        return m;
    }
}
