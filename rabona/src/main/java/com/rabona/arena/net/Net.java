package com.rabona.arena.net;

import com.rabona.arena.RabonaArena;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(RabonaArena.id("main"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static int id;

    private Net() {}

    public static void init() {
        reg(C2S.Act.class, C2S.Act::encode, C2S.Act::new, C2S.Act::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(C2S.Menu.class, C2S.Menu::encode, C2S.Menu::new, C2S.Menu::handle, NetworkDirection.PLAY_TO_SERVER);
        reg(S2C.Anim.class, S2C.Anim::encode, S2C.Anim::new, S2C.Anim::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(S2C.Shake.class, S2C.Shake::encode, S2C.Shake::new, S2C.Shake::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(S2C.MatchState.class, S2C.MatchState::encode, S2C.MatchState::new, S2C.MatchState::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(S2C.Stats.class, S2C.Stats::encode, S2C.Stats::new, S2C.Stats::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(S2C.Banner.class, S2C.Banner::encode, S2C.Banner::new, S2C.Banner::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(S2C.Feed.class, S2C.Feed::encode, S2C.Feed::new, S2C.Feed::handle, NetworkDirection.PLAY_TO_CLIENT);
        reg(S2C.OpenMenu.class, S2C.OpenMenu::encode, S2C.OpenMenu::new, S2C.OpenMenu::handle, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <T> void reg(Class<T> c, BiConsumer<T, FriendlyByteBuf> enc, Function<FriendlyByteBuf, T> dec,
                                BiConsumer<T, Supplier<NetworkEvent.Context>> handler, NetworkDirection dir) {
        CH.registerMessage(id++, c, enc, dec, (msg, ctx) -> {
            handler.accept(msg, ctx);
            ctx.get().setPacketHandled(true);
        }, Optional.of(dir));
    }

    public static void toServer(Object msg) { CH.sendToServer(msg); }

    public static void toPlayer(ServerPlayer p, Object msg) { CH.send(PacketDistributor.PLAYER.with(() -> p), msg); }

    public static void toAll(Object msg) { CH.send(PacketDistributor.ALL.noArg(), msg); }

    public static void toTrackingAndSelf(Entity e, Object msg) {
        CH.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), msg);
    }
}
