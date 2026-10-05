package com.krolasyon.sololeveling.client.render;

import com.krolasyon.sololeveling.entity.ShadowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Shadow soldiers are drawn with the renderer of the monster they came from, fed through a buffer that turns every
 * colour into deep shadow-black and every glowing part into the Shadow Monarch's purple.
 */
public class ShadowRenderer extends EntityRenderer<ShadowEntity> {
    private final Map<Integer, LivingEntity> dummies = new HashMap<>();
    private static final DustParticleOptions EYE = new DustParticleOptions(new Vector3f(0.65F, 0.45F, 1F), 0.7F);

    public ShadowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.5F;
        shadowStrength = 1.2F;
    }

    private LivingEntity dummy(ShadowEntity e) {
        LivingEntity d = dummies.get(e.getId());
        EntityType<?> t = e.sourceType();
        if (d != null && d.getType() == t) return d;
        if (t == null || e.level() == null) return null;
        Entity made = t.create(e.level());
        if (!(made instanceof LivingEntity le)) return null;
        if (le instanceof Mob m) m.setNoAi(true);
        dummies.put(e.getId(), le);
        if (dummies.size() > 64) dummies.clear();
        return le;
    }

    @Override
    public void render(ShadowEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        LivingEntity d = dummy(e);
        if (d == null) return;
        d.setPos(e.getX(), e.getY(), e.getZ());
        d.xo = e.xo;
        d.yo = e.yo;
        d.zo = e.zo;
        d.xOld = e.xOld;
        d.yOld = e.yOld;
        d.zOld = e.zOld;
        d.setYRot(e.getYRot());
        d.yRotO = e.yRotO;
        d.setXRot(e.getXRot());
        d.xRotO = e.xRotO;
        d.yBodyRot = e.yBodyRot;
        d.yBodyRotO = e.yBodyRotO;
        d.yHeadRot = e.yHeadRot;
        d.yHeadRotO = e.yHeadRotO;
        d.tickCount = e.tickCount;
        d.attackAnim = e.attackAnim;
        d.oAttackAnim = e.oAttackAnim;
        d.swinging = e.swinging;
        d.setOnGround(e.onGround());
        d.walkAnimation.setSpeed(e.walkAnimation.speed());
        syncWalk(d, e);
        d.setCustomName(null);
        pose.pushPose();
        int rise = e.riseTicks();
        if (rise > 0) pose.translate(0, -e.getBbHeight() * (rise / 30F), 0);
        try {
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(d).render(d, yaw, partial, pose, new Tinted(buf), light);
        } catch (RuntimeException ex) {
            // some modded or special renderers can't handle a detached copy; skip the frame
        }
        pose.popPose();
        if (e.level().random.nextInt(4) == 0) {
            double h = e.getBbHeight() * 0.88;
            e.level().addParticle(EYE, e.getX() + (e.level().random.nextDouble() - 0.5) * 0.2, e.getY() + h, e.getZ() + (e.level().random.nextDouble() - 0.5) * 0.2, 0, 0.01, 0);
        }
        super.render(e, yaw, partial, pose, buf, light);
    }

    private final Map<Integer, Float> walkPos = new HashMap<>();

    private void syncWalk(LivingEntity d, ShadowEntity e) {
        // WalkAnimationState has no setter for its position: advance the copy by the same amount as the shadow
        float target = e.walkAnimation.position();
        float have = walkPos.getOrDefault(e.getId(), d.walkAnimation.position());
        float delta = target - have;
        if (Math.abs(delta) > 0.0001F && Math.abs(delta) < 10) {
            d.walkAnimation.update(delta, 1F);
            d.walkAnimation.setSpeed(e.walkAnimation.speed());
        }
        walkPos.put(e.getId(), d.walkAnimation.position());
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowEntity e) { return TextureAtlas.LOCATION_BLOCKS; }

    @Override
    protected boolean shouldShowName(ShadowEntity e) { return e.isNamed() && super.shouldShowName(e); }

    /** Buffer wrapper that recolours everything. */
    static final class Tinted implements MultiBufferSource {
        private final MultiBufferSource inner;

        Tinted(MultiBufferSource inner) { this.inner = inner; }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            String n = type.toString();
            boolean glow = n.contains("eyes") || n.contains("emissive") || n.contains("energy");
            return new Tint(inner.getBuffer(type), glow);
        }
    }

    static final class Tint implements VertexConsumer {
        private final VertexConsumer v;
        private final boolean glow;

        Tint(VertexConsumer v, boolean glow) {
            this.v = v;
            this.glow = glow;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            v.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            if (glow) v.color(Math.min(255, r * 3 / 4 + 40), Math.min(255, g / 2 + 20), 255, a);
            else v.color(r * 30 / 255 + 6, g * 22 / 255 + 4, b * 48 / 255 + 14, a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float w) {
            v.uv(u, w);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int w) {
            v.overlayCoords(u, w);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int w) {
            v.uv2(u, w);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            v.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() { v.endVertex(); }

        @Override
        public void defaultColor(int r, int g, int b, int a) { v.defaultColor(r, g, b, a); }

        @Override
        public void unsetDefaultColor() { v.unsetDefaultColor(); }
    }
}
