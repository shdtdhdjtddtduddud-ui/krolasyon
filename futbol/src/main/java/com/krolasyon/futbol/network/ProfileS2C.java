package com.krolasyon.futbol.network;

import com.krolasyon.futbol.game.CardData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ProfileS2C {
    public final int coins;
    public final List<CardData.Card> cards;
    public final int revealed;

    public ProfileS2C(int coins, List<CardData.Card> cards, int revealed) {
        this.coins = coins;
        this.cards = cards;
        this.revealed = revealed;
    }

    public ProfileS2C(FriendlyByteBuf b) {
        this.coins = b.readVarInt();
        int n = b.readVarInt();
        List<CardData.Card> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(CardData.Card.read(b));
        this.cards = l;
        this.revealed = b.readVarInt() - 1;
    }

    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(coins);
        b.writeVarInt(cards.size());
        for (CardData.Card c : cards) c.write(b);
        b.writeVarInt(revealed + 1);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.krolasyon.futbol.client.ClientPackets.profile(this));
        ctx.get().setPacketHandled(true);
    }
}
