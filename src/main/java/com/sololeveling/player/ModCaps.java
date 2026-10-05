package com.sololeveling.player;

import com.sololeveling.SoloLeveling;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class ModCaps {
    private ModCaps() {}

    public static final Capability<SLPlayer> PLAYER = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation ID = new ResourceLocation(SoloLeveling.MODID, "player");

    public static SLPlayer get(Player p) {
        return p.getCapability(PLAYER).orElseGet(SLPlayer::new);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) e.addCapability(ID, new Provider());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        SLPlayer o = get(e.getOriginal());
        SLPlayer n = get(e.getEntity());
        n.copyFrom(o);
        if (e.isWasDeath()) n.mana = n.maxMana() * 0.5F;
        e.getOriginal().invalidateCaps();
    }

    private static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final SLPlayer data = new SLPlayer();
        private final LazyOptional<SLPlayer> opt = LazyOptional.of(() -> data);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER ? opt.cast() : LazyOptional.empty();
        }

        @Override public CompoundTag serializeNBT() { return data.save(); }
        @Override public void deserializeNBT(CompoundTag nbt) { data.load(nbt); }
    }
}
