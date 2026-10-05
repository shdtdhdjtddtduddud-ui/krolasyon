package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.SoloLeveling;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class HunterCapability {
    public static final Capability<HunterData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation KEY = SoloLeveling.id("hunter");

    private HunterCapability() {}

    public static void register(RegisterCapabilitiesEvent e) { e.register(HunterData.class); }

    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (!(e.getObject() instanceof Player)) return;
        Provider p = new Provider();
        e.addCapability(KEY, p);
    }

    public static void clone(PlayerEvent.Clone e) {
        Player old = e.getOriginal();
        old.reviveCaps();
        old.getCapability(CAP).ifPresent(o -> e.getEntity().getCapability(CAP).ifPresent(n -> n.copyFrom(o)));
        old.invalidateCaps();
    }

    /** Never null: a throwaway instance is returned if the capability is missing (e.g. fake players). */
    public static HunterData get(Player p) {
        return p.getCapability(CAP).orElseGet(HunterData::new);
    }

    static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final HunterData data = new HunterData();
        private final LazyOptional<HunterData> opt = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return CAP.orEmpty(cap, opt);
        }

        @Override
        public CompoundTag serializeNBT() { return data.save(); }

        @Override
        public void deserializeNBT(CompoundTag nbt) { data.load(nbt); }
    }
}
