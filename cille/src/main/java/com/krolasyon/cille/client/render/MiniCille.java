package com.krolasyon.cille.client.render;

import com.krolasyon.cille.client.model.BehmudModel;
import com.krolasyon.cille.client.model.CilleModel;
import com.krolasyon.cille.client.model.KabkabaModel;
import com.krolasyon.cille.client.model.SarkanModel;
import com.krolasyon.cille.client.model.NirumendModel;
import com.krolasyon.cille.registry.ModItems;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** the tiny sleeping Cille shown inside each kind of marble */
public final class MiniCille {
    public final CilleModel<?> model;
    public final ResourceLocation texture;
    /** model-units-to-marble-radius scale, centring offsets (blocks of model space) and tint */
    public final float scale, offY, offZ, r, g, b;
    /** glass tint */
    public final float gr, gg, gb;

    private MiniCille(CilleModel<?> model, String tex, float scale, float offY, float offZ, float r, float g, float b, float gr, float gg, float gb) {
        this.model = model;
        this.texture = CilleRenderer.tex(tex);
        this.scale = scale;
        this.offY = offY;
        this.offZ = offZ;
        this.r = r; this.g = g; this.b = b;
        this.gr = gr; this.gg = gg; this.gb = gb;
    }

    private static final Map<Item, MiniCille> CACHE = new HashMap<>();
    private static EntityModelSet bakedFrom;

    public static MiniCille forItem(Item item, EntityModelSet models) {
        if (bakedFrom != models) { CACHE.clear(); bakedFrom = models; }
        return CACHE.computeIfAbsent(item, it -> {
            if (it == ModItems.NIRUMEND_MISKETI.get())
                return new MiniCille(new NirumendModel(models.bakeLayer(NirumendModel.LAYER)), "nirumend", 0.11F, 0.45F, 0.9F, 0.95F, 0.88F, 0.75F, 1.0F, 0.92F, 0.75F);
            if (it == ModItems.SARKAN_MISKETI.get())
                return new MiniCille(new SarkanModel(models.bakeLayer(SarkanModel.LAYER)), "sarkan", 0.2F, 1.15F, 0F, 0.9F, 0.85F, 0.75F, 1.0F, 0.88F, 0.62F);
            if (it == ModItems.BEHMUD_MISKETI.get())
                return new MiniCille(new BehmudModel(models.bakeLayer(BehmudModel.LAYER)), "behmud", 0.12F, 0.62F, -1.8F, 0.75F, 0.85F, 1.0F, 0.72F, 1.0F, 1.0F);
            return new MiniCille(new KabkabaModel(models.bakeLayer(KabkabaModel.LAYER)), "kabkaba", 0.11F, 0.62F, 0F, 0.42F, 0.48F, 0.9F, 1F, 1F, 1F);
        });
    }
}
