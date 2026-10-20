package com.krolasyon.advgear.network;

import com.krolasyon.advgear.gear.GearData;
import com.krolasyon.advgear.gear.GearType;
import com.krolasyon.advgear.item.BackpackItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Kuşanım eşitleme (sunucu → istemci) ve ekran açma istekleri (istemci → sunucu). */
public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("advgear", "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        CHANNEL.registerMessage(0, SyncGear.class, SyncGear::write, SyncGear::read, SyncGear::handle);
        CHANNEL.registerMessage(1, Open.class, Open::write, Open::read, Open::handle);
    }

    public static SyncGear packet(Player p) {
        ItemStack[] s = new ItemStack[GearType.SLOTS];
        GearData.Gear g = GearData.get(p);
        for (int i = 0; i < s.length; i++) s[i] = g.getItem(i).copy();
        // çanta içeriği istemciye gönderilmez (yalnızca görünüm için)
        if (!s[4].isEmpty()) s[4].removeTagKey("Items");
        return new SyncGear(p.getId(), s);
    }

    public static void syncGear(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), packet(p));
    }

    public static void sendTo(ServerPlayer to, Player about) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> to), packet(about));
    }

    public record SyncGear(int entityId, ItemStack[] stacks) {
        public void write(FriendlyByteBuf b) {
            b.writeInt(entityId);
            for (ItemStack s : stacks) b.writeItem(s);
        }

        public static SyncGear read(FriendlyByteBuf b) {
            int id = b.readInt();
            ItemStack[] s = new ItemStack[GearType.SLOTS];
            for (int i = 0; i < s.length; i++) s[i] = b.readItem();
            return new SyncGear(id, s);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.advgear.client.ClientGear.set(entityId, stacks)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** 0 = kuşanım ekranı, 1 = sırttaki çanta. */
    public record Open(int which) {
        public void write(FriendlyByteBuf b) {
            b.writeByte(which);
        }

        public static Open read(FriendlyByteBuf b) {
            return new Open(b.readByte());
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p == null) return;
                if (which == 0) com.krolasyon.advgear.AdvGear.openGear(p);
                else {
                    ItemStack bag = GearData.back(p);
                    if (bag.getItem() instanceof BackpackItem bi) bi.open(p, bag, -1);
                    else p.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.advgear.no_backpack"), true);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
