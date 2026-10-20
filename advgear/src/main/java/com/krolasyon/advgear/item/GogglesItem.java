package com.krolasyon.advgear.item;

import com.krolasyon.advgear.gear.GearItem;
import com.krolasyon.advgear.gear.GearType;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Gözlük: 0 Kâşif (gece görüşü), 1 Mühendis (cevher görüşü), 2 Avcı (canavar izi). Elde 3D çizilir. */
public class GogglesItem extends GearItem {
    public final int variant;

    public GogglesItem(int variant, Properties props) {
        super(GearType.GOGGLES, props);
        this.variant = variant;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
