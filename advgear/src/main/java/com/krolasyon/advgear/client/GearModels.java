package com.krolasyon.advgear.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Giyilen eşyaların modelleri. Gözlük baş uzayında, çanta ve kemer gövde uzayında (piksel birimleri) tanımlıdır;
 * böylece oyuncu modelinin baş/gövde dönüşüyle birebir hareket ederler. UV yerleşimi tools/make_textures.py ile aynıdır.
 */
public final class GearModels {
    public static final ModelLayerLocation GOGGLES = new ModelLayerLocation(new ResourceLocation("advgear", "goggles"), "main");
    public static final ModelLayerLocation BACKPACK = new ModelLayerLocation(new ResourceLocation("advgear", "backpack"), "main");
    public static final ModelLayerLocation BELT = new ModelLayerLocation(new ResourceLocation("advgear", "belt"), "main");

    private GearModels() {}

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d, CubeDeformation def) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d, def);
    }

    private static CubeDeformation g(float x, float y, float z) {
        return new CubeDeformation(x, y, z);
    }

    private static final CubeDeformation N = CubeDeformation.NONE;

    public static LayerDefinition goggles() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("strap_back", box(0, 0, -4.5F, -5.6F, 3.9F, 9, 2, 1, g(-0.0F, -0.25F, -0.3F)), PartPose.ZERO);
        r.addOrReplaceChild("strap_l", box(0, 4, 3.9F, -5.6F, -4.5F, 1, 2, 9, g(-0.3F, -0.25F, -0.0F)), PartPose.ZERO);
        r.addOrReplaceChild("strap_r", box(20, 4, -4.9F, -5.6F, -4.5F, 1, 2, 9, g(-0.3F, -0.25F, -0.0F)), PartPose.ZERO);
        r.addOrReplaceChild("bridge", box(20, 0, -1F, -5.6F, -4.9F, 2, 1, 1, g(0F, -0.1F, -0.2F)), PartPose.ZERO);
        r.addOrReplaceChild("frame_l", box(0, 16, 0.3F, -6.6F, -5.9F, 4, 4, 2, N), PartPose.ZERO);
        r.addOrReplaceChild("frame_r", box(12, 16, -4.3F, -6.6F, -5.9F, 4, 4, 2, N), PartPose.ZERO);
        r.addOrReplaceChild("lens_l", box(24, 16, 0.8F, -6.1F, -6.0F, 3, 3, 1, g(0F, 0F, -0.45F)), PartPose.ZERO);
        r.addOrReplaceChild("lens_r", box(32, 16, -3.8F, -6.1F, -6.0F, 3, 3, 1, g(0F, 0F, -0.45F)), PartPose.ZERO);
        r.addOrReplaceChild("cone_l", box(0, 22, 0.3F, -6.6F, -7.0F, 4, 4, 1, g(0.15F, 0.15F, 0F)), PartPose.ZERO);
        r.addOrReplaceChild("cone_r", box(10, 22, -4.3F, -6.6F, -7.0F, 4, 4, 1, g(0.15F, 0.15F, 0F)), PartPose.ZERO);
        r.addOrReplaceChild("lens_hl", box(24, 20, 0.8F, -6.1F, -7.2F, 3, 3, 1, g(0F, 0F, -0.45F)), PartPose.ZERO);
        r.addOrReplaceChild("lens_hr", box(32, 20, -3.8F, -6.1F, -7.2F, 3, 3, 1, g(0F, 0F, -0.45F)), PartPose.ZERO);
        PartDefinition loupe = r.addOrReplaceChild("loupe", box(40, 0, -3.3F, -5.6F, -8.0F, 2, 2, 2, N), PartPose.ZERO);
        loupe.addOrReplaceChild("hinge", box(40, 4, -4.6F, -6.1F, -6.6F, 1, 1, 3, g(-0.2F, -0.2F, 0F)), PartPose.ZERO);
        r.addOrReplaceChild("loupe_lens", box(48, 0, -3.3F, -5.6F, -8.1F, 2, 2, 1, g(-0.3F, -0.3F, -0.45F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition belt() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("band", box(0, 0, -4.5F, 10.0F, -2.5F, 9, 2, 5, g(0F, -0.2F, 0F)), PartPose.ZERO);
        r.addOrReplaceChild("buckle", box(28, 0, -1.5F, 9.8F, -3.0F, 3, 2, 1, g(-0.2F, 0F, -0.25F)), PartPose.ZERO);
        r.addOrReplaceChild("pouch_l", box(36, 0, 3.7F, 10.4F, -2.0F, 2, 3, 3, new CubeDeformation(-0.15F)), PartPose.ZERO);
        r.addOrReplaceChild("pouch_r", box(46, 0, -5.7F, 10.4F, -2.0F, 2, 3, 3, new CubeDeformation(-0.15F)), PartPose.ZERO);
        r.addOrReplaceChild("pouch_back", box(0, 8, -2F, 10.4F, 2.2F, 4, 3, 2, new CubeDeformation(-0.1F)), PartPose.ZERO);
        r.addOrReplaceChild("vial1", box(12, 8, -3.8F, 9.2F, -3.0F, 1, 3, 1, new CubeDeformation(-0.1F)), PartPose.ZERO);
        r.addOrReplaceChild("vial2", box(16, 8, -2.6F, 9.4F, -3.0F, 1, 3, 1, new CubeDeformation(-0.1F)), PartPose.ZERO);
        r.addOrReplaceChild("vial3", box(20, 8, 2.6F, 9.2F, -3.0F, 1, 3, 1, new CubeDeformation(-0.1F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition backpack() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        // omuz kayışları (ortak)
        r.addOrReplaceChild("strap_lf", box(0, 0, 1.6F, 0F, -2.6F, 1, 9, 1, g(0F, 0F, -0.3F)), PartPose.ZERO);
        r.addOrReplaceChild("strap_lt", box(4, 0, 1.6F, -0.6F, -2.6F, 1, 1, 5, g(0F, -0.2F, 0F)), PartPose.ZERO);
        r.addOrReplaceChild("strap_rf", box(16, 0, -2.6F, 0F, -2.6F, 1, 9, 1, g(0F, 0F, -0.3F)), PartPose.ZERO);
        r.addOrReplaceChild("strap_rt", box(20, 0, -2.6F, -0.6F, -2.6F, 1, 1, 5, g(0F, -0.2F, 0F)), PartPose.ZERO);
        // 0: yeşil heybe
        r.addOrReplaceChild("s_body", box(0, 10, -3.5F, 3F, 2.2F, 7, 7, 3, N), PartPose.ZERO);
        r.addOrReplaceChild("s_flap", box(20, 10, -3.5F, 2.7F, 5.0F, 7, 6, 1, g(0.2F, 0.1F, -0.3F)), PartPose.ZERO);
        r.addOrReplaceChild("s_strap", box(36, 10, -0.5F, -8F, -2.6F, 1, 16, 1, g(0F, 0F, -0.3F)), PartPose.offsetAndRotation(0F, 6F, 0F, 0F, 0F, 0.62F));
        r.addOrReplaceChild("s_buckle", box(40, 10, -1F, 6.2F, 5.4F, 2, 2, 1, g(0F, 0F, -0.2F)), PartPose.ZERO);
        // 1: gri gezgin çantası
        r.addOrReplaceChild("t_body", box(0, 27, -4F, 0.5F, 2.2F, 8, 10, 4, N), PartPose.ZERO);
        r.addOrReplaceChild("t_flap", box(24, 27, -4F, 0.3F, 2.0F, 8, 3, 5, g(0.25F, 0.1F, 0.15F)), PartPose.ZERO);
        r.addOrReplaceChild("t_flap2", box(24, 35, -4F, 0.3F, 6.3F, 8, 5, 1, g(0.25F, 0.1F, -0.1F)), PartPose.ZERO);
        r.addOrReplaceChild("t_pocket", box(50, 27, -2.5F, 5.6F, 6.1F, 5, 4, 1, N), PartPose.ZERO);
        r.addOrReplaceChild("t_clasp", box(50, 32, -0.5F, 4.6F, 6.9F, 1, 2, 1, g(0F, 0F, -0.2F)), PartPose.ZERO);
        // 2: turuncu sefer çantası
        r.addOrReplaceChild("e_body", box(0, 41, -4.5F, 0F, 2.2F, 9, 11, 5, N), PartPose.ZERO);
        r.addOrReplaceChild("e_roll", box(28, 41, -5F, -2.7F, 2.7F, 10, 3, 3, N), PartPose.ZERO);
        r.addOrReplaceChild("e_pocket_l", box(28, 47, 4.4F, 4F, 3.0F, 2, 5, 3, N), PartPose.ZERO);
        r.addOrReplaceChild("e_pocket_r", box(38, 47, -6.4F, 4F, 3.0F, 2, 5, 3, N), PartPose.ZERO);
        r.addOrReplaceChild("e_front", box(48, 47, -3F, 5.6F, 7.1F, 6, 4, 1, N), PartPose.ZERO);
        r.addOrReplaceChild("e_flap", box(0, 57, -4.5F, -0.2F, 7.0F, 9, 5, 1, g(0.15F, 0.05F, -0.2F)), PartPose.ZERO);
        r.addOrReplaceChild("e_lock", box(20, 57, -0.5F, 4.4F, 7.7F, 1, 2, 1, g(0F, 0F, -0.1F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Model + varyant görünürlükleri. */
    public static final class Goggles {
        public final ModelPart root;
        private final ModelPart[] lenses;
        private final ModelPart[] frame;

        public Goggles(ModelPart root) {
            this.root = root;
            lenses = new ModelPart[]{root.getChild("lens_l"), root.getChild("lens_r"), root.getChild("lens_hl"), root.getChild("lens_hr"),
                    root.getChild("loupe_lens")};
            frame = new ModelPart[]{root.getChild("cone_l"), root.getChild("cone_r"), root.getChild("loupe")};
        }

        /** Çerçeve geçişi için: camları gizle; cam geçişi için: yalnızca camları göster. */
        public void prepare(int variant, boolean glass) {
            for (ModelPart p : root.getAllParts().toList()) p.visible = !glass;
            root.visible = true;
            frame[0].visible = frame[1].visible = !glass && variant == 2;
            frame[2].visible = !glass && variant == 1;
            frame[2].getChild("hinge").visible = !glass && variant == 1;
            lenses[0].visible = lenses[1].visible = glass && variant != 2;
            lenses[2].visible = lenses[3].visible = glass && variant == 2;
            lenses[4].visible = glass && variant == 1;
        }
    }

    public static final class Belt {
        public final ModelPart root;

        public Belt(ModelPart root) {
            this.root = root;
        }

        public void prepare(int variant) {
            root.getChild("pouch_l").visible = variant >= 1;
            root.getChild("pouch_back").visible = variant == 1;
            root.getChild("pouch_r").visible = variant == 2;
            root.getChild("vial1").visible = root.getChild("vial2").visible = root.getChild("vial3").visible = variant == 2;
        }
    }

    public static final class Backpack {
        public final ModelPart root;

        public Backpack(ModelPart root) {
            this.root = root;
        }

        public void prepare(int variant, float sway) {
            for (String n : new String[]{"s_body", "s_flap", "s_strap", "s_buckle"}) root.getChild(n).visible = variant == 0;
            for (String n : new String[]{"t_body", "t_flap", "t_flap2", "t_pocket", "t_clasp"}) root.getChild(n).visible = variant == 1;
            for (String n : new String[]{"e_body", "e_roll", "e_pocket_l", "e_pocket_r", "e_front", "e_flap", "e_lock"})
                root.getChild(n).visible = variant == 2;
            for (String n : new String[]{"strap_lf", "strap_lt", "strap_rf", "strap_rt"}) root.getChild(n).visible = variant != 0;
            // kapaklar yürürken hafifçe sallanır
            root.getChild("s_flap").xRot = sway * 0.6F;
            root.getChild("t_flap2").xRot = sway;
            root.getChild("e_flap").xRot = sway;
            root.getChild("e_roll").y = Math.abs(sway) * -2F;
        }
    }
}
