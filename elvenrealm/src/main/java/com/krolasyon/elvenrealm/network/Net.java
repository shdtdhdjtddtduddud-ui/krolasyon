package com.krolasyon.elvenrealm.network;

import com.krolasyon.elvenrealm.dialogue.DialogueManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Diyalog paketleri: sunucu ekranı açar/günceller, istemci seçimi gönderir. */
public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("elvenrealm", "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        CHANNEL.registerMessage(0, OpenDialogue.class, OpenDialogue::write, OpenDialogue::read, OpenDialogue::handle);
        CHANNEL.registerMessage(1, Choice.class, Choice::write, Choice::read, Choice::handle);
    }

    public static void send(ServerPlayer p, OpenDialogue msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    /** Sunucudan: konuşan elf, metin, seçenekler, dostluk. entityId -1 ekranı kapatır. */
    public record OpenDialogue(int entityId, String name, String title, String text, List<String> options, int friendship, int questState,
                               float voice) {
        public void write(FriendlyByteBuf b) {
            b.writeInt(entityId);
            b.writeUtf(name);
            b.writeUtf(title);
            b.writeUtf(text, 8192);
            b.writeVarInt(options.size());
            for (String o : options) b.writeUtf(o);
            b.writeVarInt(friendship);
            b.writeVarInt(questState);
            b.writeFloat(voice);
        }

        public static OpenDialogue read(FriendlyByteBuf b) {
            int id = b.readInt();
            String n = b.readUtf(), t = b.readUtf(), tx = b.readUtf(8192);
            int c = b.readVarInt();
            List<String> o = new ArrayList<>();
            for (int i = 0; i < c; i++) o.add(b.readUtf());
            return new OpenDialogue(id, n, t, tx, o, b.readVarInt(), b.readVarInt(), b.readFloat());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.elvenrealm.client.ClientHooks.dialogue(this)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** İstemciden: seçilen seçeneğin sırası (-1 = kapattı). */
    public record Choice(int entityId, int index) {
        public void write(FriendlyByteBuf b) {
            b.writeInt(entityId);
            b.writeVarInt(index + 1);
        }

        public static Choice read(FriendlyByteBuf b) {
            return new Choice(b.readInt(), b.readVarInt() - 1);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p != null) DialogueManager.choose(p, entityId, index);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
