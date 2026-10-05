package com.krolasyon.sololeveling.client.screen;

import com.krolasyon.sololeveling.client.ClientHooks;
import com.krolasyon.sololeveling.client.Holo;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.system.Sys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Shop window (System shop, market stalls, guild stores). */
public class ShopScreen extends Screen {
    private final int type;
    private final long gold;
    private final List<ItemStack> items = new ArrayList<>();
    private final List<Integer> prices = new ArrayList<>();
    private final List<Boolean> locked = new ArrayList<>();
    private final List<String> ranks = new ArrayList<>();
    private final List<String> ids = new ArrayList<>();
    private int scroll;

    public ShopScreen(CompoundTag t) {
        super(Component.empty());
        type = t.getInt("type");
        gold = t.getLong("gold");
        for (Tag x : t.getList("items", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) x;
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.getString("item")));
            if (item == null) continue;
            items.add(new ItemStack(item));
            ids.add(e.getString("item"));
            prices.add(e.getInt("price"));
            locked.add(e.getBoolean("locked"));
            ranks.add(e.getString("rank"));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private int rows() { return 8; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int w = 300, h = 40 + rows() * 22 + 16, x = (width - w) / 2, y = (height - h) / 2;
        int accent = type == 0 ? Holo.CYAN : Holo.GOLD;
        Holo.panel(g, x, y, w, h, 1F, accent);
        Holo.title(g, font, Sys.t("shop.title." + type), x, y, w, 1F, accent);
        Component gc = Sys.t("status.gold", gold);
        g.drawString(font, gc, x + w - 10 - font.width(gc), y + 7, Holo.GOLD, true);
        ItemStack hover = ItemStack.EMPTY;
        for (int i = 0; i < rows() && i + scroll < items.size(); i++) {
            int k = i + scroll;
            int ry = y + 24 + i * 22;
            boolean lock = locked.get(k);
            g.fill(x + 8, ry, x + w - 8, ry + 20, Holo.a(0x0F2747, 0.7F));
            g.renderItem(items.get(k), x + 11, ry + 2);
            g.drawString(font, items.get(k).getHoverName(), x + 32, ry + 6, lock ? 0x6B7C8F : 0xFFFFFF, false);
            String price = prices.get(k) + " G";
            g.drawString(font, price, x + w - 100 - font.width(price), ry + 6, gold >= prices.get(k) ? Holo.GOLD : Holo.RED, false);
            if (lock) g.drawString(font, Sys.t("shop.need_rank", ranks.get(k)), x + w - 82, ry + 6, Holo.RED, false);
            else Holo.button(g, font, Sys.t("shop.buy"), x + w - 80, ry + 3, 64, 14, mx, my, gold >= prices.get(k), accent);
            if (Holo.in(mx, my, x + 8, ry, 200, 20)) hover = items.get(k);
        }
        g.drawString(font, Sys.t("shop.hint"), x + 10, y + h - 12, 0x5F7FA0, false);
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int w = 300, h = 40 + rows() * 22 + 16, x = (width - w) / 2, y = (height - h) / 2;
        for (int i = 0; i < rows() && i + scroll < items.size(); i++) {
            int k = i + scroll;
            int ry = y + 24 + i * 22;
            if (!locked.get(k) && Holo.in(mx, my, x + w - 80, ry + 3, 64, 14)) {
                ClientHooks.playClick();
                Net.toServer(new Net.Action("shop", hasShiftDown() ? 5 : 1, type, ids.get(k)));
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(Math.max(0, items.size() - rows()), scroll - (int) Math.signum(d)));
        return true;
    }
}
