package com.krolasyon.bosses.net;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.nbt.CompoundTag;
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

public final class Net {
    private Net() {}

    private static final String PROTOCOL = "2";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KrolasyonBosses.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CH.registerMessage(id++, SyncMsg.class, SyncMsg::encode, SyncMsg::decode, SyncMsg::handle);
        CH.registerMessage(id++, DialogueMsg.class, DialogueMsg::encode, DialogueMsg::decode, DialogueMsg::handle);
        CH.registerMessage(id++, ChoiceMsg.class, ChoiceMsg::encode, ChoiceMsg::decode, ChoiceMsg::handle);
        CH.registerMessage(id++, OpenJournalMsg.class, OpenJournalMsg::encode, OpenJournalMsg::decode, OpenJournalMsg::handle);
    }

    public static void syncTo(ServerPlayer sp) {
        CH.send(PacketDistributor.PLAYER.with(() -> sp), new SyncMsg(com.krolasyon.bosses.faction.PlayerData.copyForSync(sp)));
    }

    // ------------------------------------------------------------------ player data sync
    public record SyncMsg(CompoundTag data) {
        public static void encode(SyncMsg m, FriendlyByteBuf b) { b.writeNbt(m.data); }

        public static SyncMsg decode(FriendlyByteBuf b) {
            CompoundTag t = b.readNbt();
            return new SyncMsg(t == null ? new CompoundTag() : t);
        }

        public static void handle(SyncMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.ClientHooks.sync(m.data)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------------ dialogue
    /** server -> client: show a dialogue page; every string is a translation key, {@code args} fill the %s of the text */
    public record DialogueMsg(int npcId, String speakerKey, int color, String textKey, List<String> args, List<String> options) {
        public static void encode(DialogueMsg m, FriendlyByteBuf b) {
            b.writeVarInt(m.npcId);
            b.writeUtf(m.speakerKey);
            b.writeInt(m.color);
            b.writeUtf(m.textKey, 400);
            b.writeVarInt(m.args.size());
            for (String a : m.args) b.writeUtf(a, 200);
            b.writeVarInt(m.options.size());
            for (String o : m.options) b.writeUtf(o, 400);
        }

        public static DialogueMsg decode(FriendlyByteBuf b) {
            int id = b.readVarInt();
            String sp = b.readUtf();
            int col = b.readInt();
            String tx = b.readUtf(400);
            int na = b.readVarInt();
            List<String> args = new ArrayList<>();
            for (int i = 0; i < na; i++) args.add(b.readUtf(200));
            int n = b.readVarInt();
            List<String> o = new ArrayList<>();
            for (int i = 0; i < n; i++) o.add(b.readUtf(400));
            return new DialogueMsg(id, sp, col, tx, args, o);
        }

        public static void handle(DialogueMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.bosses.client.ClientHooks.openDialogue(m)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** client -> server: the player picked option {@code index} (-1 = closed the screen) */
    public record ChoiceMsg(int npcId, int index) {
        public static void encode(ChoiceMsg m, FriendlyByteBuf b) {
            b.writeVarInt(m.npcId);
            b.writeVarInt(m.index + 1);
        }

        public static ChoiceMsg decode(FriendlyByteBuf b) { return new ChoiceMsg(b.readVarInt(), b.readVarInt() - 1); }

        public static void handle(ChoiceMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp != null) com.krolasyon.bosses.faction.Dialogue.onChoice(sp, m.npcId, m.index);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** server -> client: open the journal book */
    public record OpenJournalMsg() {
        public static void encode(OpenJournalMsg m, FriendlyByteBuf b) {}

        public static OpenJournalMsg decode(FriendlyByteBuf b) { return new OpenJournalMsg(); }

        public static void handle(OpenJournalMsg m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.krolasyon.bosses.client.ClientHooks::openJournal));
            ctx.get().setPacketHandled(true);
        }
    }
}
