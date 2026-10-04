package com.rabona.arena.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rabona.arena.RabonaArena;
import com.rabona.arena.game.Pitch;
import com.rabona.arena.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Tribunlerdeki taraftarlar (yalnizca istemci gorseli). Koltuklarda durur, sallanir, tezahurat yapar,
 * meksika dalgasi yapar; gol olunca takiminin taraftari ziplayarak kollarini kaldirir.
 */
public final class CrowdRenderer {
    private record Fan(BlockPos pos, float yaw, int skin, int team, float seed) {}

    private static final List<Fan> FANS = new ArrayList<>();
    private static Pitch builtFor;
    private static int scanTick;
    private static PlayerModel<Player> model;
    /** [takim-1][ten]: ten ve forma tek dokuda. */
    private static final ResourceLocation[][] FAN = new ResourceLocation[2][8];

    static {
        for (int i = 0; i < 8; i++) {
            FAN[0][i] = RabonaArena.id("textures/entity/fan/red_" + i + ".png");
            FAN[1][i] = RabonaArena.id("textures/entity/fan/blue_" + i + ".png");
        }
    }

    private CrowdRenderer() {}

    private static void scan(Minecraft mc, Pitch p) {
        FANS.clear();
        builtFor = p;
        int L = Pitch.HALF_LEN, W = Pitch.HALF_WID;
        for (int row = 0; row < 6; row++) {
            for (int a = -(L + 4); a <= L + 4; a++) for (int sb : new int[]{-1, 1}) add(mc, p, a, sb * (W + 5 + row), 1 + row, a);
            for (int b = -(W + 4); b <= W + 4; b++) for (int sa : new int[]{-1, 1}) add(mc, p, sa * (L + 7 + row), b, 1 + row, sa * 50);
        }
    }

    private static void add(Minecraft mc, Pitch p, int a, int b, int dy, int sideA) {
        int h = (a * 73856093) ^ (b * 19349663) ^ (dy * 83492791);
        if (Math.floorMod(h, 100) > 52) return; // doluluk
        BlockPos pos = p.block(a, b, dy);
        BlockState s = mc.level.getBlockState(pos);
        if (!s.is(ModBlocks.SEAT_RED.get()) && !s.is(ModBlocks.SEAT_BLUE.get()) && !s.is(ModBlocks.SEAT_WHITE.get()) && !s.is(ModBlocks.SEAT_GOLD.get()))
            return;
        float yaw = s.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        int team = sideA < 0 ? 1 : sideA > 0 ? 2 : (Math.floorMod(h, 2) + 1);
        if (s.is(ModBlocks.SEAT_RED.get())) team = 1;
        if (s.is(ModBlocks.SEAT_BLUE.get())) team = 2;
        FANS.add(new Fan(pos, yaw, Math.floorMod(h >> 3, 8), team, Math.floorMod(h >> 7, 1000) / 1000f));
    }

    public static void render(PoseStack ps, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Pitch p = ClientState.pitch;
        if (p == null || mc.level == null || mc.player == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        if (cam.distanceTo(p.center()) > 170) return;
        if (builtFor != p || ++scanTick % 200 == 0) scan(mc, p);
        if (FANS.isEmpty()) return;
        if (model == null) {
            model = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        float time = ClientState.clientTicks + partial;
        int goalTeam = 0;
        if (ClientState.bannerTicks > 0 && ClientState.banner != null && ClientState.banner.type() == 1) {
            String[] ex = ClientState.banner.extra().split("\\|", -1);
            goalTeam = ex.length > 2 ? parse(ex[2]) : 0;
        }
        boolean matchOn = ClientState.match != null && ClientState.match.phase() != 0;
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float wave = (time * 0.9f) % 260f - 60f; // meksika dalgasi konumu
        int drawn = 0;
        for (Fan f : FANS) {
            double dx = f.pos.getX() + 0.5 - cam.x, dy = f.pos.getY() + 0.5 - cam.y, dz = f.pos.getZ() + 0.5 - cam.z;
            if (dx * dx + dy * dy + dz * dz > 120 * 120) continue;
            if (++drawn > 700) break;
            boolean cheer = goalTeam == f.team;
            float ph = time * 0.25f + f.seed * 40;
            float jump = 0;
            resetPose(model);
            // varsayilan: hafif sallanma, arada alkis
            model.head.xRot = 0.15f + Mth.sin(ph * 0.3f) * 0.08f;
            model.body.xRot = 0.05f;
            float clap = matchOn ? Mth.sin(ph * 1.6f) : 0;
            model.rightArm.xRot = -0.9f + clap * 0.1f;
            model.leftArm.xRot = -0.9f + clap * 0.1f;
            model.rightArm.yRot = -0.45f - clap * 0.15f;
            model.leftArm.yRot = 0.45f + clap * 0.15f;
            // dalga
            double along = f.pos.getX() * 0.7 + f.pos.getZ() * 0.7;
            double wd = Math.abs(((along - wave) % 260 + 260) % 260 - 130);
            if (matchOn && wd > 126) {
                jump = 0.25f;
                raise(model, 0);
            }
            if (cheer) {
                jump = Math.abs(Mth.sin(ph * 1.2f)) * 0.45f;
                raise(model, ph);
            }
            ClientAnims.sync(model);
            ps.pushPose();
            ps.translate(dx, dy - 0.0 + jump, dz);
            ps.mulPose(Axis.YP.rotationDegrees(180 - f.yaw));
            ps.scale(-0.9375f, -0.9375f, 0.9375f);
            ps.translate(0, -1.501, 0);
            int light = LevelRenderer.getLightColor(mc.level, f.pos.above());
            model.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(FAN[f.team == 1 ? 0 : 1][f.skin])), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            ps.popPose();
        }
        buf.endBatch();
    }

    private static void raise(PlayerModel<Player> m, float ph) {
        m.rightArm.xRot = -2.9f + Mth.sin(ph * 2) * 0.25f;
        m.leftArm.xRot = -2.9f + Mth.cos(ph * 2) * 0.25f;
        m.rightArm.yRot = m.leftArm.yRot = 0;
        m.rightArm.zRot = 0.35f;
        m.leftArm.zRot = -0.35f;
        m.head.xRot = -0.3f;
    }

    private static void resetPose(PlayerModel<Player> m) {
        for (var part : new net.minecraft.client.model.geom.ModelPart[]{m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) {
            part.xRot = part.yRot = part.zRot = 0;
        }
        m.crouching = false;
        m.young = false;
    }

    private static void copy(PlayerModel<Player> from, PlayerModel<Player> to) {
        to.head.copyFrom(from.head);
        to.body.copyFrom(from.body);
        to.rightArm.copyFrom(from.rightArm);
        to.leftArm.copyFrom(from.leftArm);
        to.rightLeg.copyFrom(from.rightLeg);
        to.leftLeg.copyFrom(from.leftLeg);
        ClientAnims.sync(to);
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
