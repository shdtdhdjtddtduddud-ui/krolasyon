package com.krolasyon.futbol.network;

import com.krolasyon.futbol.FutbolMod;
import com.krolasyon.futbol.game.Move;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private Net() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(new ResourceLocation(FutbolMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CH.messageBuilder(MoveC2S.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(MoveC2S::encode).decoder(MoveC2S::new)
                .consumerMainThread(MoveC2S::handle).add();
        CH.messageBuilder(ControlC2S.class, id++, NetworkDirection.PLAY_TO_SERVER).encoder(ControlC2S::encode).decoder(ControlC2S::new)
                .consumerMainThread(ControlC2S::handle).add();
        CH.messageBuilder(AnimS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(AnimS2C::encode).decoder(AnimS2C::new)
                .consumerMainThread(AnimS2C::handle).add();
        CH.messageBuilder(MatchS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(MatchS2C::encode).decoder(MatchS2C::new)
                .consumerMainThread(MatchS2C::handle).add();
        CH.messageBuilder(GoalS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(GoalS2C::encode).decoder(GoalS2C::new)
                .consumerMainThread(GoalS2C::handle).add();
        CH.messageBuilder(OpenMenuS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(OpenMenuS2C::encode).decoder(OpenMenuS2C::new)
                .consumerMainThread(OpenMenuS2C::handle).add();
        CH.messageBuilder(ShakeS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT).encoder(ShakeS2C::encode).decoder(ShakeS2C::new)
                .consumerMainThread(ShakeS2C::handle).add();
    }

    public static void sendTo(ServerPlayer p, Object msg) { CH.send(PacketDistributor.PLAYER.with(() -> p), msg); }

    public static void sendAnim(LivingEntity e, Move m, int variant, int cooldown) {
        CH.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), new AnimS2C(e.getId(), m.ordinal(), variant, cooldown));
    }

    /** camera shake for everyone near the actor */
    public static void shake(LivingEntity e, int strength) {
        CH.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), new ShakeS2C(strength));
    }

    public static void toServer(Object msg) { CH.sendToServer(msg); }
}
