package com.krolasyon.furniture.client;

import com.krolasyon.furniture.blockentity.ChalkboardBlockEntity;
import com.krolasyon.furniture.blockentity.PianoBlockEntity;
import com.krolasyon.furniture.client.screen.ChalkboardEditScreen;
import com.krolasyon.furniture.client.screen.PianoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** client-only entry points (kept out of common classes so a dedicated server never loads them) */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openPiano(BlockPos base) {
        Minecraft.getInstance().setScreen(new PianoScreen(base));
    }

    public static void openChalkboard(BlockPos pos) {
        BlockEntity be = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getBlockEntity(pos);
        if (be instanceof ChalkboardBlockEntity board) Minecraft.getInstance().setScreen(new ChalkboardEditScreen(board));
    }

    public static void pianoAnim(BlockPos pos, int note) {
        if (Minecraft.getInstance().level == null) return;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
        if (be instanceof PianoBlockEntity piano) piano.pressClient(note);
    }
}
