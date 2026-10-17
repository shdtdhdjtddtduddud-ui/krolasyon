package com.krolasyon.mirror.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** The reflection wears its owner's skin. */
public final class SkinHelper {
    @Nullable
    private static PlayerInfo info(@Nullable UUID id) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return id != null && connection != null ? connection.getPlayerInfo(id) : null;
    }

    public static ResourceLocation skin(@Nullable UUID id) {
        PlayerInfo info = info(id);
        if (info != null) return info.getSkinLocation();
        return id != null ? DefaultPlayerSkin.getDefaultSkin(id) : DefaultPlayerSkin.getDefaultSkin();
    }

    public static boolean slim(@Nullable UUID id) {
        PlayerInfo info = info(id);
        if (info != null) return "slim".equals(info.getModelName());
        return id != null && "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
    }

    private SkinHelper() {}
}
