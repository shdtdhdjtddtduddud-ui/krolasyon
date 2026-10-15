package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.Ben10;
import com.krolasyon.ben10.ModRegistry;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.client.model.AlienModel;
import com.krolasyon.ben10.client.render.Quads;
import com.krolasyon.ben10.net.Net;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Ben 10 style transformation from the wrist: the left arm comes up, the dial core pops out, scrolling turns the dial and
 * shows a green hologram of the alien, left click slams the core down. Rendered in first person over the hands.
 */
public final class WatchMode {
    private static final ResourceLocation WATCH = new ResourceLocation(Ben10.MODID, "textures/alien/watch.png");
    private static final ResourceLocation WATCH_GLOW = new ResourceLocation(Ben10.MODID, "textures/alien/watch_glow.png");
    private static final ResourceLocation GLOW = new ResourceLocation(Ben10.MODID, "textures/fx/glow.png");
    private static final ResourceLocation BEAM = new ResourceLocation(Ben10.MODID, "textures/fx/beam.png");
    private static final int N = Alien.values().length;

    public static boolean active;
    private static int selected;
    private static int ticks;
    private static int slam = -1;
    private static float turn, turnO, change;
    private static AlienModel watch;

    private WatchMode() {}

    private static void ui(net.minecraft.sounds.SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.9F));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || active) return;
        OmniData d = OmniData.get(mc.player);
        if (!d.has(OmniData.F_WATCH)) {
            mc.player.displayClientMessage(Component.translatable("msg.krolben10.wear_first").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (d.alien != null) {
            // slap the symbol on the chest to turn back
            Net.CHANNEL.sendToServer(new Net.Select(-1));
            return;
        }
        if (d.recharge > 0) {
            mc.player.displayClientMessage(Component.translatable("msg.krolben10.recharging", (d.recharge + 19) / 20).withStyle(ChatFormatting.RED), true);
            ui(ModRegistry.S_BEEP.get(), 0.6F);
            return;
        }
        active = true;
        ticks = 0;
        slam = -1;
        turn = turnO = selected * 360F / N;
        ui(ModRegistry.S_DIAL_OPEN.get(), 1F);
    }

    public static void close() {
        active = false;
        slam = -1;
    }

    public static void scroll(int dir) {
        if (!active || slam >= 0) return;
        selected = Math.floorMod(selected + dir, N);
        change = 1F;
        ui(ModRegistry.S_DIAL.get(), 0.9F + selected * 0.04F);
    }

    public static void confirm() {
        if (!active || slam >= 0 || ticks < 4) return;
        slam = 0;
        ui(ModRegistry.S_DIAL.get(), 0.5F);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!active) return;
        if (mc.player == null || !mc.player.isAlive() || mc.screen != null) {
            close();
            return;
        }
        ticks++;
        turnO = turn;
        float target = selected * 360F / N;
        turn += Mth.wrapDegrees(target - turn) * 0.45F;
        change *= 0.8F;
        if (slam >= 0) {
            slam++;
            if (slam == 3) Net.CHANNEL.sendToServer(new Net.Select(selected));
            if (slam >= 8) close();
        }
    }

    private static float open(float pt) {
        float o = Mth.clamp((ticks + pt) / 6F, 0, 1);
        return 1 - (1 - o) * (1 - o) * (1 - o);
    }

    private static float slam(float pt) {
        return slam < 0 ? 0 : Mth.clamp((slam + pt) / 3F, 0, 1);
    }

    /** the arm transform in camera space (shared by the renderer and the hologram placement) */
    private static Matrix4f armMatrix(float pt) {
        float o = open(pt), s = slam(pt);
        float bob = Mth.sin((ticks + pt) * 0.12F) * 0.006F;
        Matrix4f m = new Matrix4f();
        m.translate(-0.6F, -0.62F + 0.3F * o - 0.07F * s + bob, -0.74F);
        m.rotate(new Quaternionf().rotateX(Mth.DEG_TO_RAD * (32F + 10F * s)));
        m.rotate(new Quaternionf().rotateZ(Mth.DEG_TO_RAD * (8F - 8F * o)));
        // model +y (shoulder -> hand) points to the right, the outer side of the left arm (+x, the dial) points up
        m.rotate(new Quaternionf().rotateAxis(Mth.PI, 0.70710677F, 0.70710677F, 0));
        return m;
    }

    public static void render(PoseStack ps, MultiBufferSource buf, int light, float pt) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null) return;
        if (watch == null) watch = new AlienModel(mc.getEntityModels().bakeLayer(ClientSetup.layer("watch")));
        float o = open(pt), s = slam(pt);
        Matrix4f arm = armMatrix(pt);

        ps.pushPose();
        ps.last().pose().mul(arm);
        ps.last().normal().rotate(arm.getNormalizedRotation(new Quaternionf()));
        // the player's own left arm (skin + sleeve)
        if (mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer pr) {
            PlayerModel<AbstractClientPlayer> model = pr.getModel();
            for (ModelPart part : new ModelPart[]{model.leftArm, model.leftSleeve}) {
                float x = part.x, y = part.y, z = part.z, xr = part.xRot, yr = part.yRot, zr = part.zRot;
                part.setPos(0, 0, 0);
                part.setRotation(0, 0, 0);
                part.visible = true;
                RenderType rt = part == model.leftArm ? RenderType.entitySolid(p.getSkinTextureLocation()) : RenderType.entityTranslucent(p.getSkinTextureLocation());
                part.render(ps, buf.getBuffer(rt), light, OverlayTexture.NO_OVERLAY);
                part.setPos(x, y, z);
                part.setRotation(xr, yr, zr);
            }
        }
        // the watch with the dial core popped out and turning
        ps.translate(1.0F / 16F, 7.5F / 16F, 0);
        ps.scale(0.5F, 0.5F, 0.5F);
        watch.reset();
        ModelPart core = watch.bone("core");
        if (core != null) {
            core.x += 1.8F * o - 1.8F * s;
            core.xRot = Mth.DEG_TO_RAD * Mth.rotLerp(pt, turnO, turn);
        }
        watch.root().render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(WATCH)), light, OverlayTexture.NO_OVERLAY);
        float pulse = 0.75F + 0.25F * Mth.sin((ticks + pt) * 0.4F) + s;
        watch.root().render(ps, buf.getBuffer(RenderType.eyes(WATCH_GLOW)), Quads.FULL, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1F);
        ps.popPose();

        // green hologram rising from the dial
        Vector3f face = new Matrix4f(arm).translate(1.0F / 16F, 7.5F / 16F, 0).scale(0.5F).transformPosition(new Vector3f(7.2F / 16F, 0, 0));
        float h = 0.17F * o * (1 - s);
        if (h < 0.01F) return;
        ps.pushPose();
        ps.translate(face.x, face.y, face.z);
        Quads.vertical(ps, buf.getBuffer(RenderType.eyes(BEAM)), 0.07F * o, 0, h, -(ticks + pt) * 0.08F, 1F, 0x55FF33, 0.55F, Quads.FULL);
        ps.translate(0, h + 0.04F, 0);
        float flick = 0.85F + 0.15F * Mth.sin((ticks + pt) * 1.7F);
        float sz = 0.075F * o * (1 + 0.15F * change);
        Quads.billboard(ps, buf.getBuffer(RenderType.eyes(GLOW)), sz * 1.9F, 0x40FF30, 0.35F * flick, Quads.FULL);
        Alien a = Alien.values()[selected];
        Quads.quadXY(ps, buf.getBuffer(RenderType.eyes(OmniHud.portrait(a))), sz, rgb(0.55F * flick, 1F * flick, 0.5F * flick), 1F, Quads.FULL, 0, 0, 1, 1);
        // neighbours on the dial, smaller and dimmer
        for (int k = -1; k <= 1; k += 2) {
            Alien n = Alien.values()[Math.floorMod(selected + k, N)];
            ps.pushPose();
            ps.translate(k * sz * 2.4F, -sz * 0.35F, -0.02F);
            Quads.quadXY(ps, buf.getBuffer(RenderType.eyes(OmniHud.portrait(n))), sz * 0.55F, rgb(0.2F, 0.45F, 0.2F), 1F, Quads.FULL, 0, 0, 1, 1);
            ps.popPose();
        }
        ps.popPose();
    }

    private static int rgb(float r, float g, float b) {
        return ((int) (Mth.clamp(r, 0, 1) * 255) << 16) | ((int) (Mth.clamp(g, 0, 1) * 255) << 8) | (int) (Mth.clamp(b, 0, 1) * 255);
    }

    /** name + controls hint on the HUD while the dial is up */
    public static void hud(GuiGraphics g, int sw, int sh) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        Alien a = Alien.values()[selected];
        int y = sh - 88;
        g.drawCenteredString(mc.font, a.displayName().copy().withStyle(ChatFormatting.BOLD), sw / 2, y, 0xFF55FF33);
        g.drawCenteredString(mc.font, Component.translatable("watch.krolben10.hint"), sw / 2, y + 12, 0xFFB0FFB0);
    }
}
