package com.krolasyon.bosses.rpg.client;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.rpg.npc.RpgNpc;
import com.krolasyon.bosses.rpg.world.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Humanoid people of every race. Male/female bodies, race proportions and race features (ears, horns, tails, tusks). */
public class NpcRenderer extends EntityRenderer<RpgNpc> {
    public static final ModelLayerLocation FEATURES = new ModelLayerLocation(new ResourceLocation(KrolasyonBosses.MODID, "npc_features"), "main");

    private final Body male, female;

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.male = new Body(ctx, false);
        this.female = new Body(ctx, true);
        this.shadowRadius = 0.5F;
    }

    public static ResourceLocation texture(RpgNpc n) {
        String g = n.female() ? "f" : "m";
        String outfit = n.isChildNpc() ? "common" : n.role().outfit.name().toLowerCase();
        return new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/npc/" + n.race().name().toLowerCase() + "_" + g + "_" + outfit + "_" + (n.variant() & 1) + ".png");
    }

    @Override
    public void render(RpgNpc e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        (e.female() ? female : male).render(e, yaw, partial, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RpgNpc e) { return texture(e); }

    public static LayerDefinition featuresLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // pointed elf ears
        PartDefinition elf = root.addOrReplaceChild("elf", CubeListBuilder.create(), PartPose.ZERO);
        elf.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(0, 0).addBox(0, -1.5F, -0.5F, 4, 2, 1), PartPose.offsetAndRotation(4, -4.5F, 0.5F, 0, -0.35F, -0.45F));
        elf.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4, -1.5F, -0.5F, 4, 2, 1), PartPose.offsetAndRotation(-4, -4.5F, 0.5F, 0, 0.35F, 0.45F));
        // curved demon horns
        PartDefinition horns = root.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition hl = horns.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(0, 8).addBox(-1, -4, -1, 2, 4, 2), PartPose.offsetAndRotation(2.5F, -8, -1, -0.3F, 0, 0.35F));
        hl.addOrReplaceChild("horn_l2", CubeListBuilder.create().texOffs(8, 8).addBox(-0.5F, -3, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0, -3.5F, 0, -0.6F, 0, 0));
        PartDefinition hr = horns.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(0, 8).addBox(-1, -4, -1, 2, 4, 2), PartPose.offsetAndRotation(-2.5F, -8, -1, -0.3F, 0, -0.35F));
        hr.addOrReplaceChild("horn_r2", CubeListBuilder.create().texOffs(8, 8).addBox(-0.5F, -3, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0, -3.5F, 0, -0.6F, 0, 0));
        // beast ears on top of the head
        PartDefinition beast = root.addOrReplaceChild("beast", CubeListBuilder.create(), PartPose.ZERO);
        beast.addOrReplaceChild("bear_l", CubeListBuilder.create().texOffs(16, 0).addBox(-1.5F, -3, -0.5F, 3, 3, 1), PartPose.offsetAndRotation(2.5F, -8, -1, 0, 0, 0.2F));
        beast.addOrReplaceChild("bear_r", CubeListBuilder.create().texOffs(16, 0).addBox(-1.5F, -3, -0.5F, 3, 3, 1), PartPose.offsetAndRotation(-2.5F, -8, -1, 0, 0, -0.2F));
        // orc tusks
        PartDefinition tusks = root.addOrReplaceChild("tusks", CubeListBuilder.create(), PartPose.ZERO);
        tusks.addOrReplaceChild("tusk_l", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -2, -0.5F, 1, 2, 1), PartPose.offsetAndRotation(1.8F, -1, -4.3F, -0.2F, 0, 0.15F));
        tusks.addOrReplaceChild("tusk_r", CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -2, -0.5F, 1, 2, 1), PartPose.offsetAndRotation(-1.8F, -1, -4.3F, -0.2F, 0, -0.15F));
        // tails (attached to the body)
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 16).addBox(-1, 0, 0, 2, 2, 8), PartPose.offsetAndRotation(0, 10, 2, -0.9F, 0, 0));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(20, 16).addBox(-1.5F, -0.5F, 0, 3, 3, 3), PartPose.offset(0, 0, 7.5F));
        // dwarf beard
        root.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, 0, -0.5F, 7, 5, 2), PartPose.offset(0, -2, -4.3F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** one body type (wide / slim arms) */
    static class Body extends HumanoidMobRenderer<RpgNpc, PlayerModel<RpgNpc>> {
        Body(EntityRendererProvider.Context ctx, boolean slim) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
            this.addLayer(new Features(this, ctx.bakeLayer(FEATURES)));
        }

        @Override
        public ResourceLocation getTextureLocation(RpgNpc e) { return texture(e); }

        @Override
        protected void scale(RpgNpc e, PoseStack ps, float partial) {
            float y = e.renderScaleY() * 0.9375F, xz = e.renderScaleXZ() * 0.9375F;
            ps.scale(xz, y, xz);
        }

        @Override
        public void render(RpgNpc e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            PlayerModel<RpgNpc> m = this.getModel();
            m.rightArmPose = e.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            m.leftArmPose = e.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : e.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD) && e.getTarget() != null ? HumanoidModel.ArmPose.BLOCK : HumanoidModel.ArmPose.ITEM;
            m.setAllVisible(true);
            m.hat.visible = true;
            m.jacket.visible = true;
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        protected void setupRotations(RpgNpc e, PoseStack ps, float age, float yaw, float partial) {
            super.setupRotations(e, ps, age, yaw, partial);
            if (e.flag(RpgNpc.F_DISTRESSED)) ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mth.sin(age * 0.8F) * 3));
        }
    }

    static class Features extends RenderLayer<RpgNpc, PlayerModel<RpgNpc>> {
        private final ModelPart root, elf, horns, beast, tusks, tail, beard;

        Features(LivingEntityRenderer<RpgNpc, PlayerModel<RpgNpc>> parent, ModelPart root) {
            super(parent);
            this.root = root;
            this.elf = root.getChild("elf");
            this.horns = root.getChild("horns");
            this.beast = root.getChild("beast");
            this.tusks = root.getChild("tusks");
            this.tail = root.getChild("tail");
            this.beard = root.getChild("beard");
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, RpgNpc e, float limb, float limbAmt, float partial, float age, float yaw, float pitch) {
            if (e.isInvisible()) return;
            Race r = e.race();
            ResourceLocation tex = new ResourceLocation(KrolasyonBosses.MODID, "textures/entity/npc/features_" + r.name().toLowerCase() + ".png");
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(tex));
            int ov = LivingEntityRenderer.getOverlayCoords(e, 0);
            ps.pushPose();
            this.getParentModel().head.translateAndRotate(ps);
            if (r == Race.ELF || r == Race.DARK_ELF) elf.render(ps, vc, light, ov);
            if (r == Race.DEMON) horns.render(ps, vc, light, ov);
            if (r == Race.BEASTKIN) beast.render(ps, vc, light, ov);
            if (r == Race.ORC) tusks.render(ps, vc, light, ov);
            if (r == Race.DWARF && !e.female() && !e.isChildNpc()) beard.render(ps, vc, light, ov);
            if (r == Race.GIANT && !e.female() && !e.isChildNpc()) beard.render(ps, vc, light, ov);
            ps.popPose();
            if (r == Race.BEASTKIN || r == Race.DEMON) {
                ps.pushPose();
                this.getParentModel().body.translateAndRotate(ps);
                tail.yRot = Mth.sin(age * 0.12F) * 0.35F + Mth.sin(limb * 0.6F) * 0.2F * limbAmt;
                tail.render(ps, vc, light, ov);
                ps.popPose();
            }
        }
    }

    @SuppressWarnings("unused")
    private static void unused(OverlayTexture o) {}
}
