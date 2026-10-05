package com.sololeveling.net;

import com.sololeveling.client.ClientHooks;
import com.sololeveling.system.UiActions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class Packets {
    private Packets() {}

    private static void client(Supplier<NetworkEvent.Context> ctx, Runnable r) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> r));
        ctx.get().setPacketHandled(true);
    }

    private static void server(Supplier<NetworkEvent.Context> ctx, java.util.function.Consumer<ServerPlayer> r) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) r.accept(sp);
        });
        ctx.get().setPacketHandled(true);
    }

    // ------------------------------------------------------------------ S2C
    public record Sync(CompoundTag tag) {
        public static void encode(Sync p, FriendlyByteBuf b) { b.writeNbt(p.tag); }
        public static Sync decode(FriendlyByteBuf b) { return new Sync(b.readNbt()); }
        public static void handle(Sync p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.sync(p.tag)); }
    }

    /** System notification window. kind: 0 info, 1 level-up, 2 quest, 3 warning, 4 reward, 5 arise */
    public record Notify(int kind, Component title, Component body) {
        public static void encode(Notify p, FriendlyByteBuf b) { b.writeVarInt(p.kind); b.writeComponent(p.title); b.writeComponent(p.body); }
        public static Notify decode(FriendlyByteBuf b) { return new Notify(b.readVarInt(), b.readComponent(), b.readComponent()); }
        public static void handle(Notify p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.notify(p.kind, p.title, p.body)); }
    }

    public record News(CompoundTag tag) {
        public static void encode(News p, FriendlyByteBuf b) { b.writeNbt(p.tag); }
        public static News decode(FriendlyByteBuf b) { return new News(b.readNbt()); }
        public static void handle(News p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.news(p.tag)); }
    }

    public record OpenNpc(int entityId, String role, String name, String guild) {
        public static void encode(OpenNpc p, FriendlyByteBuf b) { b.writeVarInt(p.entityId); b.writeUtf(p.role); b.writeUtf(p.name); b.writeUtf(p.guild); }
        public static OpenNpc decode(FriendlyByteBuf b) { return new OpenNpc(b.readVarInt(), b.readUtf(), b.readUtf(), b.readUtf()); }
        public static void handle(OpenNpc p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.openNpc(p.entityId, p.role, p.name, p.guild)); }
    }

    public record OpenSystem(int tab) {
        public static void encode(OpenSystem p, FriendlyByteBuf b) { b.writeVarInt(p.tab); }
        public static OpenSystem decode(FriendlyByteBuf b) { return new OpenSystem(b.readVarInt()); }
        public static void handle(OpenSystem p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.openSystem(p.tab)); }
    }

    /** screen effects: "arise", "levelup", "gate", "flash" */
    public record Fx(String kind, int arg) {
        public static void encode(Fx p, FriendlyByteBuf b) { b.writeUtf(p.kind); b.writeVarInt(p.arg); }
        public static Fx decode(FriendlyByteBuf b) { return new Fx(b.readUtf(), b.readVarInt()); }
        public static void handle(Fx p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.fx(p.kind, p.arg)); }
    }

    public record Cooldown(String skill, int ticks) {
        public static void encode(Cooldown p, FriendlyByteBuf b) { b.writeUtf(p.skill); b.writeVarInt(p.ticks); }
        public static Cooldown decode(FriendlyByteBuf b) { return new Cooldown(b.readUtf(), b.readVarInt()); }
        public static void handle(Cooldown p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.cooldown(p.skill, p.ticks)); }
    }

    /** active gates: list of compounds {rank, theme, x, y, z, dim, red} */
    public record Gates(CompoundTag tag) {
        public static void encode(Gates p, FriendlyByteBuf b) { b.writeNbt(p.tag); }
        public static Gates decode(FriendlyByteBuf b) { return new Gates(b.readNbt()); }
        public static void handle(Gates p, Supplier<NetworkEvent.Context> c) { client(c, () -> ClientHooks.gates(p.tag)); }
    }

    // ------------------------------------------------------------------ C2S
    public record Action(String action, String arg, int n) {
        public static void encode(Action p, FriendlyByteBuf b) { b.writeUtf(p.action); b.writeUtf(p.arg); b.writeVarInt(p.n); }
        public static Action decode(FriendlyByteBuf b) { return new Action(b.readUtf(100), b.readUtf(200), b.readVarInt()); }
        public static void handle(Action p, Supplier<NetworkEvent.Context> c) { server(c, sp -> UiActions.handle(sp, p.action, p.arg, p.n)); }
    }

    public record Cast(String skill) {
        public static void encode(Cast p, FriendlyByteBuf b) { b.writeUtf(p.skill); }
        public static Cast decode(FriendlyByteBuf b) { return new Cast(b.readUtf(100)); }
        public static void handle(Cast p, Supplier<NetworkEvent.Context> c) { server(c, sp -> com.sololeveling.skill.SkillExec.cast(sp, p.skill)); }
    }
}
