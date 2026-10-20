package com.krolasyon.advgear.client;

import com.krolasyon.advgear.AdvGear;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** Mühendis Gözlüğü: eğilirken (shift) 12 blok içindeki cevherleri duvarların ardından renkli çerçevelerle gösterir. */
public final class OreSight {
    private static final List<long[]> ORES = new ArrayList<>();
    private static long lastScan;
    /** Testte eğilmeden de görebilmek için. */
    public static boolean force;

    private OreSight() {}

    private static final class Types extends RenderType {
        private Types() {
            super("", DefaultVertexFormat.POSITION, VertexFormat.Mode.LINES, 0, false, false, () -> {}, () -> {});
        }

        static final RenderType LINES = RenderType.create("advgear_ore_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 4096,
                false, false, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(3.0))).setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setOutputState(ITEM_ENTITY_TARGET).setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false));
    }

    private static int color(BlockState s) {
        if (s.is(BlockTags.DIAMOND_ORES)) return 0x4FE3E0;
        if (s.is(BlockTags.EMERALD_ORES)) return 0x30D060;
        if (s.is(BlockTags.GOLD_ORES)) return 0xF5C842;
        if (s.is(BlockTags.IRON_ORES)) return 0xD8A880;
        if (s.is(BlockTags.REDSTONE_ORES)) return 0xE0302A;
        if (s.is(BlockTags.LAPIS_ORES)) return 0x3050E0;
        if (s.is(BlockTags.COPPER_ORES)) return 0xE07A3A;
        if (s.is(BlockTags.COAL_ORES)) return 0x505050;
        if (s.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS)) return 0x8A4A3A;
        if (s.is(Tags.Blocks.ORES)) return 0xC0C0C0;
        return -1;
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!ClientGear.get(mc.player, 2).is(AdvGear.ENGINEER_GOGGLES.get()) || !(mc.player.isShiftKeyDown() || force)) return;
        long now = mc.level.getGameTime();
        if (now - lastScan > 10 || now < lastScan) {
            lastScan = now;
            ORES.clear();
            BlockPos c = mc.player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -12, -12), c.offset(12, 12, 12))) {
                int col = color(mc.level.getBlockState(p));
                if (col >= 0) ORES.add(new long[]{p.asLong(), col});
            }
        }
        PoseStack ps = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buf.getBuffer(Types.LINES);
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        float pulse = 0.65F + 0.35F * (float) Math.sin((now + event.getPartialTick()) * 0.2);
        for (long[] o : ORES) {
            BlockPos p = BlockPos.of(o[0]);
            int col = (int) o[1];
            LevelRenderer.renderLineBox(ps, vc, p.getX() + 0.02, p.getY() + 0.02, p.getZ() + 0.02, p.getX() + 0.98, p.getY() + 0.98, p.getZ() + 0.98,
                    (col >> 16 & 255) / 255F, (col >> 8 & 255) / 255F, (col & 255) / 255F, pulse);
        }
        ps.popPose();
        buf.endBatch(Types.LINES);
    }
}
