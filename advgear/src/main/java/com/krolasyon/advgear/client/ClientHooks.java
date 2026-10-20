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

/** Tüm eşyalar voksel 3D çizilir: envanterde, elde, yerde ve çerçevede. Sıvılar ve sallanan parçalar canlıdır. */
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

    public static VoxModels.Anim idle(float time, int seed) {
        float ph = seed * 0.37F;
        return new VoxModels.Anim(time, 0.12F * Mth.sin(time * 0.05F + ph), 0.09F * Mth.cos(time * 0.043F + ph), 0.15F, 0, 0);
    }

    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private float open, openPrev;
        private long lastTick;

        Renderer(Minecraft mc) {
            super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }

        @Override
        public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager rm) {
            VoxModels.clear();
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            Minecraft mc = Minecraft.getInstance();
            float pt = mc.getFrameTime();
            float time = (mc.level != null ? mc.level.getGameTime() : 0) + pt;
            boolean hand = ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            Item item = stack.getItem();
            VoxModels.Anim a = hand ? new VoxModels.Anim(time, Slosh.tiltX(pt), Slosh.tiltZ(pt), 0.1F + Slosh.wave(pt), 0, 0) : idle(time, Item.getId(item));
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(1 / 16F, 1 / 16F, 1 / 16F);
            if (item instanceof SpellbookItem sb) renderBook(ps, buf, light, stack, sb, hand, time, pt);
            else if (item instanceof BackpackItem b) {
                ps.scale(1.25F, -1.25F, -1.25F);
                ps.mulPose(Axis.YP.rotationDegrees(180));
                ps.translate(0, -5.4F, -4.6F);
                VoxModels.backpack(ps, buf, light, b.variant, false);
            } else if (item instanceof GogglesItem gi) {
                ps.scale(1.25F, -1.25F, -1.25F);
                ps.translate(0, 4.6F, 5.4F);
                VoxModels.goggles(ps, buf, light, gi.variant);
            } else if (item == AdvGear.HEART_TONIC.get()) VoxModels.heartTonic(ps, buf, light, a);
            else if (item == AdvGear.OCEAN_DRAUGHT.get()) VoxModels.oceanDraught(ps, buf, light, a);
            else if (item == AdvGear.BLACK_DEATH.get()) VoxModels.blackDeath(ps, buf, light, a);
            else if (item == AdvGear.STARLIGHT_ELIXIR.get()) VoxModels.starlightElixir(ps, buf, light, a);
            else if (item == AdvGear.SUN_ESSENCE.get()) VoxModels.sunEssence(ps, buf, light, a);
            else if (ring(item) >= 0) VoxModels.ring(ps, buf, light, ring(item));
            else if (key(item) >= 0) {
                if (key(item) == 6) {
                    ps.translate(0, 1.0F, 0);
                    ps.scale(0.85F, 0.85F, 0.85F);
                }
                VoxModels.key(ps, buf, light, key(item), a);
            } else if (belt(item) >= 0) {
                ps.scale(1.35F, -1.35F, -1.35F);
                ps.translate(0, -11.5F, 0);
                VoxModels.belt(ps, buf, light, belt(item));
            }
            ps.popPose();
        }

        private static int ring(Item i) {
            return i == AdvGear.GOLD_BAND.get() ? 0 : i == AdvGear.SAPPHIRE_RING.get() ? 1 : i == AdvGear.SKULL_RING.get() ? 2
                    : i == AdvGear.BONE_HAND_RING.get() ? 3 : i == AdvGear.EMERALD_EYE_RING.get() ? 4 : -1;
        }

        private static int key(Item i) {
            return i == AdvGear.IRON_KEY.get() ? 0 : i == AdvGear.DUNGEON_KEY.get() ? 1 : i == AdvGear.HEART_KEY.get() ? 2 : i == AdvGear.MECHANIC_KEY.get() ? 3
                    : i == AdvGear.THORN_LOCKPICK.get() ? 4 : i == AdvGear.CRYSTAL_KEY.get() ? 5 : i == AdvGear.KEY_RING.get() ? 6 : -1;
        }

        private static int belt(Item i) {
            return i == AdvGear.TRAVELER_BELT.get() ? 0 : i == AdvGear.TOOL_BELT.get() ? 1 : i == AdvGear.ALCHEMIST_BELT.get() ? 2 : -1;
        }

        private void renderBook(PoseStack ps, MultiBufferSource buf, int light, ItemStack stack, SpellbookItem sb, boolean hand, float time, float pt) {
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
            ps.pushPose();
            ps.translate(-3.5F * (1 - o), 0, 0);
            // açılırken kitap döner: sayfalar (sırtın karşısı, +X) oyuncuya (+Z) bakar
            ps.mulPose(Axis.YP.rotationDegrees(-90F * o));
            ps.scale(1.25F, 1.25F, 1.25F);
            VoxModels.book(ps, buf, light, sb.variant, o, flip);
            ps.popPose();
        }
    }
}
