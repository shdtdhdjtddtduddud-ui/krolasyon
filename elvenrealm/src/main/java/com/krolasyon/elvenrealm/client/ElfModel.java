package com.krolasyon.elvenrealm.client;

import com.krolasyon.elvenrealm.entity.Activity;
import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.krolasyon.elvenrealm.entity.ElfKind;
import com.krolasyon.elvenrealm.entity.ElfKind.Parts;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Oyuncu modelinin üstüne elf parçaları: sivri kulaklar, uzun saç ve perçemler, etek/tunik, pelerin, kapüşon,
 * omuzluklar, kın, hançer, kese, sadak ve Tavi'nin omzundaki kuş. Doku 128x128 ızgarada (görüntü 256x256).
 */
public class ElfModel extends PlayerModel<ElfEntity> {
    private final ElfKind kind;
    private final ModelPart earL, earR, hair, lockL, lockR, skirtF, skirtB, skirtL, skirtR, sheath, pauldronL, pauldronR, tails, pouch,
            cloak, collar, dagger, cloakShort, quiver, bird, birdHead, wingL, wingR, birdTail, hood, tabardF, tabardB, mantle;

    public ElfModel(ModelPart root, ElfKind kind) {
        super(root, kind.slim);
        this.kind = kind;
        ModelPart head = root.getChild("head"), body = root.getChild("body");
        earL = head.getChild("elf_ear_l");
        earR = head.getChild("elf_ear_r");
        lockL = head.getChild("elf_lock_l");
        lockR = head.getChild("elf_lock_r");
        hood = head.getChild("elf_hood");
        hair = body.getChild("elf_hair");
        skirtF = body.getChild("elf_skirt_f");
        skirtB = body.getChild("elf_skirt_b");
        skirtL = body.getChild("elf_skirt_l");
        skirtR = body.getChild("elf_skirt_r");
        sheath = body.getChild("elf_sheath");
        tails = body.getChild("elf_tails");
        pouch = body.getChild("elf_pouch");
        cloak = body.getChild("elf_cloak");
        collar = body.getChild("elf_collar");
        dagger = body.getChild("elf_dagger");
        cloakShort = body.getChild("elf_cloak_short");
        quiver = body.getChild("elf_quiver");
        tabardF = body.getChild("elf_tabard_f");
        tabardB = body.getChild("elf_tabard_b");
        mantle = body.getChild("elf_mantle");
        pauldronL = root.getChild("left_arm").getChild("elf_pauldron_l");
        pauldronR = root.getChild("right_arm").getChild("elf_pauldron_r");
        bird = root.getChild("left_arm").getChild("elf_bird");
        birdHead = bird.getChild("head");
        wingL = bird.getChild("wing_l");
        wingR = bird.getChild("wing_r");
        birdTail = bird.getChild("tail");

        earL.visible = earR.visible = kind.has(Parts.EARS);
        hair.visible = kind.has(Parts.HAIR_LONG);
        lockL.visible = lockR.visible = kind.has(Parts.LOCKS);
        boolean skirt = kind.has(Parts.SKIRT) || kind.has(Parts.TUNIC);
        skirtF.visible = skirtB.visible = skirtL.visible = skirtR.visible = skirt;
        sheath.visible = kind.has(Parts.SHEATH);
        pauldronL.visible = pauldronR.visible = kind.has(Parts.PAULDRONS);
        tails.visible = kind.has(Parts.COAT_TAILS);
        pouch.visible = kind.has(Parts.POUCH);
        cloak.visible = kind.has(Parts.CLOAK);
        collar.visible = kind == ElfKind.CAELAN;
        dagger.visible = kind.has(Parts.DAGGER);
        cloakShort.visible = kind.has(Parts.CLOAK_SHORT);
        quiver.visible = kind.has(Parts.QUIVER);
        bird.visible = kind.has(Parts.BIRD);
        hood.visible = kind.has(Parts.HOOD);
        tabardF.visible = tabardB.visible = kind.has(Parts.TABARD);
        mantle.visible = kind.has(Parts.FEATHERS);
        hair.yScale = switch (kind) {
            case NIMRAE -> 1.3F;
            case LIRAEL -> 0.8F;
            case VAELOR -> 0.85F;
            default -> 0.45F;
        };
        if (kind == ElfKind.CAELAN) {
            // tunik: düz, diz boyu
            skirtF.yScale = skirtB.yScale = skirtL.yScale = skirtR.yScale = 0.85F;
        }
        if (kind == ElfKind.NIMRAE) skirtF.yScale = skirtB.yScale = skirtL.yScale = skirtR.yScale = 1.0F;
        if (kind == ElfKind.LIRAEL) skirtF.yScale = skirtB.yScale = skirtL.yScale = skirtR.yScale = 0.9F;
        if (kind == ElfKind.THALION) quiver.z = 4.2F;
        if (kind == ElfKind.TAVI) quiver.z = 3.4F;
    }

    public static LayerDefinition createLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head"), body = root.getChild("body");
        PartDefinition la = root.getChild("left_arm"), ra = root.getChild("right_arm");
        CubeDeformation none = CubeDeformation.NONE;

        // Kulaklar: başın yanından arkaya ve yukarı uzanan sivri kulaklar
        PartDefinition el = head.addOrReplaceChild("elf_ear_l", CubeListBuilder.create().texOffs(64, 0).addBox(0F, -1.5F, -0.5F, 4, 2, 1, none),
                PartPose.offsetAndRotation(3.6F, -4.2F, 0.6F, 0F, -0.45F, -0.42F));
        el.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(76, 0).addBox(0F, -0.5F, -0.5F, 2, 1, 1, none),
                PartPose.offsetAndRotation(3.6F, -0.6F, 0F, 0F, 0F, -0.3F));
        PartDefinition er = head.addOrReplaceChild("elf_ear_r", CubeListBuilder.create().texOffs(64, 4).mirror().addBox(-4F, -1.5F, -0.5F, 4, 2, 1, none),
                PartPose.offsetAndRotation(-3.6F, -4.2F, 0.6F, 0F, 0.45F, 0.42F));
        er.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(76, 4).mirror().addBox(-2F, -0.5F, -0.5F, 2, 1, 1, none),
                PartPose.offsetAndRotation(-3.6F, -0.6F, 0F, 0F, 0F, 0.3F));

        // Saç: sırttan aşağı dökülen uzun saç (gövdeye bağlı, boyu karaktere göre ölçeklenir)
        body.addOrReplaceChild("elf_hair", CubeListBuilder.create().texOffs(88, 0).addBox(-4.5F, 0F, 0F, 9, 16, 1, none),
                PartPose.offsetAndRotation(0F, -0.5F, 2.05F, 0.06F, 0F, 0F));
        head.addOrReplaceChild("elf_lock_l", CubeListBuilder.create().texOffs(110, 0).addBox(-1F, 0F, -0.75F, 2, 8, 1, new CubeDeformation(0F, 0F, 0.25F)),
                PartPose.offsetAndRotation(3.4F, -2.5F, -2.6F, 0F, 0F, -0.08F));
        head.addOrReplaceChild("elf_lock_r", CubeListBuilder.create().texOffs(118, 0).addBox(-1F, 0F, -0.75F, 2, 8, 1, new CubeDeformation(0F, 0F, 0.25F)),
                PartPose.offsetAndRotation(-3.4F, -2.5F, -2.6F, 0F, 0F, 0.08F));

        // Etek / tunik: bele bağlı dört panel
        body.addOrReplaceChild("elf_skirt_f", CubeListBuilder.create().texOffs(64, 20).addBox(-4.6F, 0F, -0.5F, 9, 11, 1, new CubeDeformation(0.05F)),
                PartPose.offset(0F, 11F, -2.1F));
        body.addOrReplaceChild("elf_skirt_b", CubeListBuilder.create().texOffs(64, 32).addBox(-4.6F, 0F, -0.5F, 9, 11, 1, new CubeDeformation(0.05F)),
                PartPose.offset(0F, 11F, 2.1F));
        body.addOrReplaceChild("elf_skirt_l", CubeListBuilder.create().texOffs(84, 20).addBox(-0.5F, 0F, -2.6F, 1, 11, 5, new CubeDeformation(0.04F)),
                PartPose.offset(4.15F, 11F, 0F));
        body.addOrReplaceChild("elf_skirt_r", CubeListBuilder.create().texOffs(96, 20).addBox(-0.5F, 0F, -2.6F, 1, 11, 5, new CubeDeformation(0.04F)),
                PartPose.offset(-4.15F, 11F, 0F));

        // Kın ve kabza (sol kalça)
        PartDefinition sh = body.addOrReplaceChild("elf_sheath", CubeListBuilder.create().texOffs(108, 20).addBox(-0.5F, 0F, -1F, 1, 12, 2, none),
                PartPose.offsetAndRotation(4.7F, 10.5F, 0.5F, 0.55F, 0F, -0.12F));
        sh.addOrReplaceChild("hilt", CubeListBuilder.create().texOffs(114, 20).addBox(-0.5F, -4F, -0.5F, 1, 4, 1, none)
                .texOffs(114, 26).addBox(-0.5F, -0.6F, -2F, 1, 1, 4, none), PartPose.ZERO);

        // Omuzluklar
        la.addOrReplaceChild("elf_pauldron_l", CubeListBuilder.create().texOffs(0, 64).addBox(-1.5F, -3.2F, -3F, 6, 4, 6, new CubeDeformation(0.1F)),
                PartPose.rotation(0F, 0F, -0.22F));
        ra.addOrReplaceChild("elf_pauldron_r", CubeListBuilder.create().texOffs(0, 74).mirror().addBox(-4.5F, -3.2F, -3F, 6, 4, 6, new CubeDeformation(0.1F)),
                PartPose.rotation(0F, 0F, 0.22F));

        // Zırh etekliği (arka)
        body.addOrReplaceChild("elf_tails", CubeListBuilder.create().texOffs(24, 64).addBox(-4F, 0F, 0F, 8, 10, 1, none),
                PartPose.offset(0F, 11F, 2.05F));
        body.addOrReplaceChild("elf_pouch", CubeListBuilder.create().texOffs(42, 64).addBox(-1.5F, 0F, -1F, 3, 3, 2, none),
                PartPose.offsetAndRotation(-2.6F, 10.4F, -2.6F, 0F, 0.25F, 0F));

        // Pelerin, kürk yaka, hançer
        body.addOrReplaceChild("elf_cloak", CubeListBuilder.create().texOffs(64, 44).addBox(-5F, 0F, 0F, 10, 22, 1, none),
                PartPose.offsetAndRotation(0F, -0.2F, 2.6F, 0.08F, 0F, 0F));
        body.addOrReplaceChild("elf_collar", CubeListBuilder.create().texOffs(86, 44).addBox(-5F, -1.2F, -3.1F, 10, 3, 6, new CubeDeformation(0.15F)),
                PartPose.ZERO);
        body.addOrReplaceChild("elf_dagger", CubeListBuilder.create().texOffs(90, 54).addBox(-0.5F, 0F, -1F, 1, 7, 2, none),
                PartPose.offsetAndRotation(2.6F, 9.2F, -2.5F, -0.15F, 0F, 0.35F));
        body.addOrReplaceChild("elf_cloak_short", CubeListBuilder.create().texOffs(42, 70).addBox(-4.5F, 0F, 0F, 9, 13, 1, none),
                PartPose.offsetAndRotation(0F, -0.2F, 2.3F, 0.1F, 0F, 0F));

        // Sadak (sırtta çapraz)
        PartDefinition qv = body.addOrReplaceChild("elf_quiver", CubeListBuilder.create().texOffs(0, 84).addBox(-1.5F, -6F, 0F, 3, 12, 3, none),
                PartPose.offsetAndRotation(0F, 6F, 3.0F, 0F, 0F, 0.55F));
        qv.addOrReplaceChild("fletch", CubeListBuilder.create().texOffs(12, 84).addBox(-1.5F, -9F, 0.5F, 3, 3, 2, none), PartPose.ZERO);

        // Kuş (Tavi'nin sol omzunda)
        PartDefinition bd = la.addOrReplaceChild("elf_bird", CubeListBuilder.create().texOffs(22, 84).addBox(-1F, -2F, -1.5F, 2, 2, 3, none),
                PartPose.offset(0.6F, -2.1F, 0.3F));
        PartDefinition bh = bd.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 84).addBox(-1F, -2F, -1.2F, 2, 2, 2, none),
                PartPose.offset(0F, -1.4F, -1.3F));
        bh.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(40, 84).addBox(-0.5F, -1.2F, -2F, 1, 1, 1, none), PartPose.ZERO);
        bd.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(22, 90).addBox(0F, 0F, -1.5F, 1, 2, 3, none),
                PartPose.offset(1F, -2F, 0F));
        bd.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(30, 90).addBox(-1F, 0F, -1.5F, 1, 2, 3, none),
                PartPose.offset(-1F, -2F, 0F));
        bd.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(44, 84).addBox(-1F, 0F, 0F, 2, 1, 3, none),
                PartPose.offsetAndRotation(0F, -1F, 1.4F, -0.4F, 0F, 0F));

        // Kapüşon: önü açık paneller ve sivri arka uç
        PartDefinition hd = head.addOrReplaceChild("elf_hood", CubeListBuilder.create()
                .texOffs(64, 68).addBox(-4.6F, -8.8F, -4.7F, 9, 1, 9, new CubeDeformation(0.2F, 0F, 0.2F))
                .texOffs(100, 68).addBox(4.05F, -8.3F, -4.7F, 1, 9, 9, new CubeDeformation(0F, 0F, 0.2F))
                .texOffs(64, 78).addBox(-5.05F, -8.3F, -4.7F, 1, 9, 9, new CubeDeformation(0F, 0F, 0.2F))
                .texOffs(64, 96).addBox(-4.6F, -8.3F, 4.1F, 9, 9, 1, new CubeDeformation(0.2F, 0F, 0F))
                .texOffs(84, 96).addBox(-4.6F, -8.9F, -5.35F, 9, 2, 1, new CubeDeformation(0.2F, 0F, 0F)), PartPose.ZERO);
        hd.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(84, 100).addBox(-2F, 0F, 0F, 4, 5, 2, none),
                PartPose.offsetAndRotation(0F, -8.2F, 4.2F, -0.9F, 0F, 0F));

        // Tabar (ön/arka kumaş)
        body.addOrReplaceChild("elf_tabard_f", CubeListBuilder.create().texOffs(0, 100).addBox(-2.5F, 0F, -0.5F, 5, 9, 1, none),
                PartPose.offset(0F, 11.5F, -2.2F));
        body.addOrReplaceChild("elf_tabard_b", CubeListBuilder.create().texOffs(12, 100).addBox(-2.5F, 0F, -0.5F, 5, 9, 1, none),
                PartPose.offset(0F, 11.5F, 2.2F));
        // Tüylü omuz örtüsü
        body.addOrReplaceChild("elf_mantle", CubeListBuilder.create().texOffs(24, 100).addBox(-5.5F, -0.6F, -3.4F, 11, 4, 7, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void prepareMobModel(ElfEntity e, float limbSwing, float limbSwingAmount, float partialTick) {
        rightArmPose = HumanoidModel.ArmPose.EMPTY;
        leftArmPose = HumanoidModel.ArmPose.EMPTY;
        ItemStack main = e.getMainHandItem();
        Activity a = e.getActivity();
        if (main.getItem() instanceof BowItem && (e.isAggressive() || (a == Activity.PRACTICE && (e.tickCount / 40) % 2 == 0))) {
            rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        } else if (!main.isEmpty()) {
            rightArmPose = HumanoidModel.ArmPose.ITEM;
        }
        super.prepareMobModel(e, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(ElfEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        Activity a = e.getActivity();
        riding = a.seated() || e.isPassenger();
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        float breath = Mth.sin(age * 0.07F);
        float walk = Math.min(1F, limbSwingAmount);

        switch (a) {
            case TALK -> {
                float g = Mth.sin(age * 0.16F);
                rightArm.xRot = -0.45F + g * 0.25F;
                rightArm.yRot = -0.25F;
                if (e.getMainHandItem().isEmpty()) {
                    leftArm.xRot = -0.3F - Mth.sin(age * 0.11F + 1F) * 0.2F;
                    leftArm.zRot = -0.15F;
                }
                head.xRot += Mth.sin(age * 0.3F) * 0.04F;
            }
            case WAVE -> {
                rightArm.xRot = -0.1F;
                rightArm.yRot = 0F;
                rightArm.zRot = 2.6F + Mth.sin(age * 0.55F) * 0.35F;
            }
            case CROSS_ARMS -> {
                rightArm.xRot = -0.95F;
                rightArm.yRot = -0.8F;
                rightArm.zRot = 0.1F;
                leftArm.xRot = -0.8F;
                leftArm.yRot = 0.8F;
                leftArm.zRot = -0.1F;
                head.xRot = Math.min(head.xRot, 0.05F);
            }
            case READ -> {
                rightArm.xRot = -0.95F;
                rightArm.yRot = -0.32F;
                leftArm.xRot = -0.95F;
                leftArm.yRot = 0.42F;
                head.xRot = 0.5F + Mth.sin(age * 0.05F) * 0.05F;
                head.yRot = Mth.sin(age * 0.02F) * 0.18F;
            }
            case MEDITATE -> {
                rightLeg.xRot = -1.45F;
                rightLeg.yRot = 0.85F;
                rightLeg.zRot = 0.05F;
                leftLeg.xRot = -1.45F;
                leftLeg.yRot = -0.85F;
                leftLeg.zRot = -0.05F;
                rightArm.xRot = -0.55F;
                rightArm.zRot = 0.25F;
                leftArm.xRot = -0.55F;
                leftArm.zRot = -0.25F;
                head.xRot = 0.15F + breath * 0.04F;
                head.yRot = 0F;
            }
            case KNEEL -> {
                leftLeg.xRot = -1.3F;
                leftLeg.yRot = 0F;
                rightLeg.xRot = 0.35F;
                rightLeg.yRot = 0F;
                if (e.kind == ElfKind.TAVI) {
                    // kuşa bakar, elini uzatır
                    leftArm.xRot = -1.1F;
                    leftArm.yRot = 0.2F;
                    head.yRot = 0.55F;
                    head.xRot = 0.2F;
                } else {
                    rightArm.xRot = -0.35F;
                    leftArm.xRot = -0.5F;
                    head.xRot = 0.3F;
                }
            }
            case PRAY -> {
                rightLeg.xRot = 1.45F;
                leftLeg.xRot = 1.45F;
                rightLeg.yRot = leftLeg.yRot = 0F;
                rightArm.xRot = -1.05F;
                rightArm.yRot = -0.48F;
                leftArm.xRot = -1.05F;
                leftArm.yRot = 0.48F;
                head.xRot = 0.35F;
            }
            case SIT -> {
                rightArm.xRot = -0.35F;
                leftArm.xRot = -0.35F;
            }
            case RECOVER -> {
                rightArm.xRot = -0.5F;
                rightArm.yRot = -0.5F;
                leftArm.xRot = -0.2F;
                head.xRot = 0.5F;
            }
            default -> {}
        }

        // büyü ve atılış animasyonları
        if (e.castAnim > 0) {
            float t = e.castAnim / 14F;
            rightArm.xRot = -2.2F * t - 0.3F;
            leftArm.xRot = -2.2F * t - 0.3F;
            rightArm.zRot = 0.35F * t;
            leftArm.zRot = -0.35F * t;
        }
        if (e.lungeAnim > 0) body.xRot = 0.35F * e.lungeAnim / 10F;

        // nefes alma
        if (!a.seated()) {
            body.y += breath * 0.12F;
            rightArm.zRot += breath * 0.03F;
            leftArm.zRot -= breath * 0.03F;
        }

        // kulak seğirmesi
        float twitch = (age + e.getId() * 13) % 90F < 6F ? Mth.sin(age * 1.6F) * 0.18F : 0F;
        earL.zRot = -0.42F + twitch;
        earR.zRot = 0.42F - twitch * 0.6F;

        // saç, perçem, pelerin ve eteğin rüzgarda/yürürken salınımı
        float sway = Mth.sin(age * 0.09F + e.getId()) * 0.04F;
        hair.xRot = 0.06F + walk * 0.35F + sway;
        lockL.xRot = Math.max(-head.xRot, -0.2F) + walk * 0.1F;
        lockR.xRot = lockL.xRot;
        cloak.xRot = 0.08F + walk * 0.55F + sway * 1.5F;
        cloakShort.xRot = 0.1F + walk * 0.45F + sway * 1.2F;
        tails.xRot = Math.max(0F, Math.max(rightLeg.xRot, leftLeg.xRot)) * 0.8F + 0.05F;
        tabardB.xRot = Math.max(0F, Math.max(rightLeg.xRot, leftLeg.xRot)) * 0.8F + 0.03F;
        tabardF.xRot = Math.min(0F, Math.min(rightLeg.xRot, leftLeg.xRot)) * 0.8F - 0.03F;
        float flare = kind == ElfKind.CAELAN ? 0.06F : kind == ElfKind.NIMRAE ? 0.1F : 0.2F;
        if (a.seated()) {
            skirtF.xRot = -1.35F;
            skirtB.xRot = flare;
            skirtL.zRot = -0.25F;
            skirtR.zRot = 0.25F;
        } else {
            skirtF.xRot = -flare + Math.min(0F, Math.min(rightLeg.xRot, leftLeg.xRot)) * 0.9F + sway * 0.5F;
            skirtB.xRot = flare + Math.max(0F, Math.max(rightLeg.xRot, leftLeg.xRot)) * 0.9F - sway * 0.5F;
            skirtL.zRot = -flare * 0.9F - Math.abs(leftLeg.zRot) - walk * 0.05F;
            skirtR.zRot = flare * 0.9F + Math.abs(rightLeg.zRot) + walk * 0.05F;
        }
        if (a == Activity.KNEEL || a == Activity.PRAY) {
            skirtF.xRot = Math.min(skirtF.xRot, -0.6F);
            skirtB.xRot = Math.max(skirtB.xRot, 0.6F);
        }
        if (a.seated()) {
            cloak.xRot = 1.2F;
            hair.xRot = 0.3F;
        }

        // kuş: baş sallama, kanat çırpma
        if (bird.visible) {
            boolean flap = e.isAggressive() || (age + e.getId()) % 160 < 25 || walk > 0.6F;
            float f = flap ? Mth.sin(age * 2.2F) * 0.9F : 0F;
            wingL.zRot = -Math.abs(f) - 0.05F;
            wingR.zRot = Math.abs(f) + 0.05F;
            birdHead.xRot = Mth.sin(age * 0.33F) > 0.85F ? 0.35F : 0F;
            birdHead.yRot = Mth.sin(age * 0.07F) * 0.6F;
            birdTail.xRot = -0.4F + Mth.sin(age * 0.5F) * 0.08F;
            bird.y = -2.1F + (flap ? -0.6F - Math.abs(f) * 0.4F : 0F);
            // kolu ne kadar oynarsa oynasın kuş dik dursun
            bird.xRot = -leftArm.xRot;
            bird.zRot = -leftArm.zRot;
        }

        hat.copyFrom(head);
        jacket.copyFrom(body);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
    }
}
