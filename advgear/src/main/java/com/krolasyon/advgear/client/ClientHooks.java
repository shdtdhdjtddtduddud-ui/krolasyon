package com.krolasyon.advgear.client;

import com.krolasyon.advgear.AdvGear;
import com.krolasyon.advgear.item.BackpackItem;
import com.krolasyon.advgear.item.GogglesItem;
import com.krolasyon.advgear.item.SpellbookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Tüm eşyalar 3D çizilir: envanterde, elde, yerde ve çerçevede. Sıvılar ve sallanan parçalar canlıdır. */
public final class ClientHooks {
    private ClientHooks() {}

    private static BlockEntityWithoutLevelRenderer renderer;

    public static final IClientItemExtensions ITEM_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) renderer = new Renderer(Minecraft.getInstance());
            return renderer;
        }
    };

    private static Map<Item, BiConsumer<G, Models3D.Anim>> models;

    private static void ensure() {
        if (models != null) return;
        models = new HashMap<>();
        models.put(AdvGear.HEART_TONIC.get(), Models3D::heartTonic);
        models.put(AdvGear.OCEAN_DRAUGHT.get(), Models3D::oceanDraught);
        models.put(AdvGear.BLACK_DEATH.get(), Models3D::blackDeath);
        models.put(AdvGear.STARLIGHT_ELIXIR.get(), Models3D::starlightElixir);
        models.put(AdvGear.SUN_ESSENCE.get(), Models3D::sunEssence);
        models.put(AdvGear.GOLD_BAND.get(), Models3D::goldBand);
        models.put(AdvGear.SAPPHIRE_RING.get(), Models3D::sapphireRing);
        models.put(AdvGear.SKULL_RING.get(), Models3D::skullRing);
        models.put(AdvGear.BONE_HAND_RING.get(), Models3D::boneHandRing);
        models.put(AdvGear.EMERALD_EYE_RING.get(), Models3D::emeraldEyeRing);
        for (var e : Map.<Item, BiConsumer<G, Models3D.Anim>>of(AdvGear.IRON_KEY.get(), Models3D::ironKey, AdvGear.DUNGEON_KEY.get(), Models3D::dungeonKey,
                AdvGear.HEART_KEY.get(), Models3D::heartKey, AdvGear.MECHANIC_KEY.get(), Models3D::mechanicKey,
                AdvGear.THORN_LOCKPICK.get(), Models3D::thornLockpick, AdvGear.CRYSTAL_KEY.get(), Models3D::crystalKey).entrySet()) {
            BiConsumer<G, Models3D.Anim> f = e.getValue();
            models.put(e.getKey(), (g, a) -> {
                g.ps.pushPose();
                g.ps.mulPose(Axis.ZP.rotationDegrees(45));
                g.ps.scale(0.95F, 0.95F, 0.95F);
                f.accept(g, a);
                g.ps.popPose();
            });
        }
        models.put(AdvGear.KEY_RING.get(), (g, a) -> {
            g.ps.pushPose();
            g.ps.translate(0, 1.5F, 0);
            g.ps.scale(0.85F, 0.85F, 0.85F);
            Models3D.keyRing(g, a);
            g.ps.popPose();
        });
        for (int v = 0; v < 3; v++) {
            final int vv = v;
            Item belt = v == 0 ? AdvGear.TRAVELER_BELT.get() : v == 1 ? AdvGear.TOOL_BELT.get() : AdvGear.ALCHEMIST_BELT.get();
            models.put(belt, (g, a) -> {
                g.ps.pushPose();
                g.ps.scale(1.35F, -1.35F, -1.35F);
                g.ps.translate(0, -11.5F, 0);
                Models3D.belt(g, vv, a);
                g.ps.popPose();
            });
        }
    }

    public static Models3D.Anim idle(float time, int seed) {
        float ph = seed * 0.37F;
        return new Models3D.Anim(time, 0.07F * Mth.sin(time * 0.05F + ph), 0.05F * Mth.cos(time * 0.043F + ph), 0.05F, 0, 0);
    }

    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private float open, openPrev;
        private long lastTick;

        Renderer(Minecraft mc) {
            super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ensure();
            Minecraft mc = Minecraft.getInstance();
            float pt = mc.getFrameTime();
            float time = (mc.level != null ? mc.level.getGameTime() : 0) + pt;
            boolean hand = ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            Item item = stack.getItem();
            Models3D.Anim anim = hand ? new Models3D.Anim(time, Slosh.tiltX(pt), Slosh.tiltZ(pt), 0.05F + Slosh.wave(pt), 0, 0)
                    : idle(time, Item.getId(item));
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(1 / 16F, 1 / 16F, 1 / 16F);
            G g = new G(ps, buf, light);
            if (item instanceof SpellbookItem sb) renderBook(g, stack, sb, hand, time, pt);
            else if (item instanceof BackpackItem b) {
                ps.scale(1.25F, -1.25F, -1.25F);
                ps.mulPose(Axis.YP.rotationDegrees(180));
                ps.translate(0, -5.4F, -4.6F);
                Models3D.backpack(g, b.variant, hand ? Slosh.tiltZ(pt) * 0.4F : 0);
            } else if (item instanceof GogglesItem gi) {
                ps.scale(1.25F, -1.25F, -1.25F);
                ps.translate(0, 4.6F, 5.4F);
                Models3D.goggles(g, gi.variant, false);
            } else {
                BiConsumer<G, Models3D.Anim> f = models.get(item);
                if (f != null) f.accept(g, anim);
            }
            ps.popPose();
        }

        private void renderBook(G g, ItemStack stack, SpellbookItem sb, boolean hand, float time, float pt) {
            LocalPlayer p = Minecraft.getInstance().player;
            float target = 0F;
            int using = 0;
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
            float flip = using > 0 ? (time * 0.09F) % 1F : 0F;
            g.ps.pushPose();
            g.ps.translate(-3.5F * (1 - o), 0, 0);
            g.ps.scale(1.25F, 1.25F, 1.25F);
            Models3D.book(g, sb.variant, o, flip);
            g.ps.popPose();
        }
    }
}
