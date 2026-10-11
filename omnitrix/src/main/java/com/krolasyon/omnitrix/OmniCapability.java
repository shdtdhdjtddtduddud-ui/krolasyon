package com.krolasyon.omnitrix;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OmniCapability {
    public static final Capability<OmniData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation KEY = new ResourceLocation(Omnitrix.MODID, "omnitrix_data");

    private OmniCapability() {}

    /** server/owner-client data; returns a throwaway instance if missing so callers never null-check */
    public static OmniData get(Player p) {
        return p.getCapability(CAP).orElseGet(OmniData::new);
    }

    public static void register(RegisterCapabilitiesEvent e) { e.register(OmniData.class); }

    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) e.addCapability(KEY, new Provider());
    }

    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        OmniData old = get(e.getOriginal());
        OmniData neu = get(e.getEntity());
        neu.copyFrom(old);
        if (e.isWasDeath()) {
            // dying while transformed reverts you and drains the watch
            neu.alien = -1;
            neu.timer = 0;
            neu.recharge = OmniData.RECHARGE_TIME;
            neu.clearStates();
        }
        e.getOriginal().invalidateCaps();
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final OmniData data = new OmniData();
        private final LazyOptional<OmniData> opt = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAP.orEmpty(cap, opt);
        }

        @Override
        public CompoundTag serializeNBT() { return data.save(); }

        @Override
        public void deserializeNBT(CompoundTag nbt) { data.load(nbt); }
    }
}
