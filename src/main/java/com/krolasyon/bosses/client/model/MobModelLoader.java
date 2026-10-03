package com.krolasyon.bosses.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds model layers and keyframe animations from the generated JSON files in
 * {@code assets/krolasyonbosses/mobmodels/<id>.json} (written by gen/build_mobs.py).
 */
public final class MobModelLoader {
    private MobModelLoader() {}

    private static final Map<String, JsonObject> CACHE = new HashMap<>();
    private static final Map<String, Map<String, AnimationDefinition>> ANIMS = new HashMap<>();

    private static synchronized JsonObject json(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            String path = "/assets/krolasyonbosses/mobmodels/" + k + ".json";
            try (InputStream in = MobModelLoader.class.getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("missing model json " + path);
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static LayerDefinition layer(String id) {
        JsonObject root = json(id);
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = new HashMap<>();
        parts.put("", mesh.getRoot());
        for (JsonElement be : root.getAsJsonArray("bones")) {
            JsonArray b = be.getAsJsonArray();
            String name = b.get(0).getAsString();
            String parent = b.get(1).isJsonNull() ? "" : b.get(1).getAsString();
            JsonArray o = b.get(2).getAsJsonArray();
            JsonArray r = b.get(3).getAsJsonArray();
            CubeListBuilder cl = CubeListBuilder.create();
            for (JsonElement ce : b.get(4).getAsJsonArray()) {
                JsonArray c = ce.getAsJsonArray();
                cl.texOffs(c.get(0).getAsInt(), c.get(1).getAsInt()).addBox(c.get(2).getAsFloat(), c.get(3).getAsFloat(), c.get(4).getAsFloat(),
                        c.get(5).getAsFloat(), c.get(6).getAsFloat(), c.get(7).getAsFloat(), new CubeDeformation(c.get(8).getAsFloat()));
            }
            PartDefinition pd = parts.get(parent).addOrReplaceChild(name, cl,
                    PartPose.offsetAndRotation(o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat(),
                            r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat()));
            parts.put(name, pd);
        }
        return LayerDefinition.create(mesh, root.get("tw").getAsInt(), root.get("th").getAsInt());
    }

    public static synchronized Map<String, AnimationDefinition> anims(String id) {
        return ANIMS.computeIfAbsent(id, k -> {
            Map<String, AnimationDefinition> out = new HashMap<>();
            JsonObject all = json(k).getAsJsonObject("anims");
            for (Map.Entry<String, JsonElement> en : all.entrySet()) {
                JsonObject a = en.getValue().getAsJsonObject();
                AnimationDefinition.Builder b = AnimationDefinition.Builder.withLength(a.get("len").getAsFloat());
                if (a.get("loop").getAsBoolean()) b.looping();
                for (JsonElement che : a.getAsJsonArray("ch")) {
                    JsonArray ch = che.getAsJsonArray();
                    String bone = ch.get(0).getAsString();
                    String kind = ch.get(1).getAsString();
                    JsonArray keys = ch.get(2).getAsJsonArray();
                    Keyframe[] kfs = new Keyframe[keys.size()];
                    for (int i = 0; i < kfs.length; i++) {
                        JsonArray kf = keys.get(i).getAsJsonArray();
                        float t = kf.get(0).getAsFloat(), x = kf.get(1).getAsFloat(), y = kf.get(2).getAsFloat(), z = kf.get(3).getAsFloat();
                        boolean cat = kf.get(4).getAsString().equals("c");
                        org.joml.Vector3f v = switch (kind) {
                            case "r" -> KeyframeAnimations.degreeVec(x, y, z);
                            case "p" -> KeyframeAnimations.posVec(x, y, z);
                            default -> KeyframeAnimations.scaleVec(x, y, z);
                        };
                        kfs[i] = new Keyframe(t, v, cat ? AnimationChannel.Interpolations.CATMULLROM : AnimationChannel.Interpolations.LINEAR);
                    }
                    AnimationChannel.Target target = switch (kind) {
                        case "r" -> AnimationChannel.Targets.ROTATION;
                        case "p" -> AnimationChannel.Targets.POSITION;
                        default -> AnimationChannel.Targets.SCALE;
                    };
                    b.addAnimation(bone, new AnimationChannel(target, kfs));
                }
                out.put(en.getKey(), b.build());
            }
            return out;
        });
    }
}
