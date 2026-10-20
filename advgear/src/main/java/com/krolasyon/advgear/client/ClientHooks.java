package com.krolasyon.advgear.client;

import com.krolasyon.advgear.item.BackpackItem;
import com.krolasyon.advgear.item.GogglesItem;
import com.krolasyon.advgear.item.SpellbookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Elde 3D çizilen eşyalar: açılıp sayfaları dönen büyü kitapları, sırt çantaları ve gözlükler. */
public final class ClientHooks {
    private ClientHooks() {}

    private static BlockEntityWithoutLevelRenderer renderer;

    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                Minecraft mc = Minecraft.getInstance();
                renderer = new Renderer(mc);
            }
            return renderer;
        }
    };

    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private BookModel book;
        /** Kitap başına yumuşatılmış açıklık (yalnızca yerel oyuncu). */
        private float open, openPrev;
        private long lastTick;

        Renderer(Minecraft mc) {
            super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }

        @Override
        public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager rm) {
            book = new BookModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BOOK));
            GearRender.bake(Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (stack.getItem() instanceof SpellbookItem sb) renderBook(stack, sb, ctx, ps, buf, light);
            else if (stack.getItem() instanceof BackpackItem b) {
                ps.pushPose();
                ps.translate(0.5, 0.5, 0.5);
                ps.scale(-1.15F, -1.15F, 1.15F);
                ps.translate(0, -0.35, -0.27);
                GearRender.backpack(ps, buf, light, b.variant, 0F);
                ps.popPose();
            } else if (stack.getItem() instanceof GogglesItem g) {
                ps.pushPose();
                ps.translate(0.5, 0.5, 0.5);
                ps.scale(-1.6F, -1.6F, 1.6F);
                ps.translate(0, 0.3, 0.05);
                GearRender.goggles(ps, buf, light, g.variant);
                ps.popPose();
            }
        }

        private void renderBook(ItemStack stack, SpellbookItem sb, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light) {
            if (book == null) onResourceManagerReload(null);
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            float pt = mc.getFrameTime();
            float time = p == null ? 0 : p.tickCount + pt;
            float target = 0F;
            int using = 0;
            boolean hand = ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            if (p != null && hand && p.isUsingItem() && p.getUseItem().getItem() == stack.getItem()) {
                using = p.getTicksUsingItem();
                target = Math.min(1F, using / (float) Math.max(4, sb.spell.charge - 2));
            }
            if (p != null && p.tickCount != lastTick && hand) {
                lastTick = p.tickCount;
                openPrev = open;
                open += (target - open) * 0.35F;
            }
            float o = hand ? Mth.lerp(pt, openPrev, open) : 0F;
            // sayfalar hızla döner (büyü hazırlanırken)
            float flip = using > 0 ? (time * 0.18F) % 1F : 0.25F;
            float flip2 = using > 0 ? (time * 0.18F + 0.5F) % 1F : 0.75F;
            book.setupAnim(time, Mth.clamp(flip, 0F, 1F), Mth.clamp(flip2, 0F, 1F), o);
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(90F));
            ps.mulPose(Axis.ZP.rotationDegrees(180F));
            float s = 1.45F;
            ps.scale(s, s, s);
            ps.translate(0, 0, 0.0F);
            ResourceLocation tex = new ResourceLocation("advgear", "textures/models/book_" + sb.variant + ".png");
            book.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            if (sb.variant == 2 && o > 0.2F) {
                // Kara Grimuar açılınca rünleri parlar
                book.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(new ResourceLocation("advgear", "textures/models/book_2_glow.png"))),
                        light, OverlayTexture.NO_OVERLAY, o, o, o, 1F);
            }
            ps.popPose();
        }
    }
}
