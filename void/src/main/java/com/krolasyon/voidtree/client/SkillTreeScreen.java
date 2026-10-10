package com.krolasyon.voidtree.client;

import com.krolasyon.voidtree.ModRegistry;
import com.krolasyon.voidtree.Skill;
import com.krolasyon.voidtree.SkillLogic;
import com.krolasyon.voidtree.VoidData;
import com.krolasyon.voidtree.VoidTree;
import com.krolasyon.voidtree.net.Net;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** The void skill tree, drawn 1:1 from the reference artwork (736x920 coordinate space). */
public class SkillTreeScreen extends Screen {
    private static final ResourceLocation BG = new ResourceLocation(VoidTree.MODID, "textures/gui/tree_bg.png");
    private static final int W = 736, H = 920, HW = 56, HH = 54, IW = 113, IH = 108;

    private record Seg(int x0, int y0, int x1, int y1, Predicate<Set<Skill>> lit) {}

    private record Burst(Skill skill, long start) {}

    private record Spark(float x, float y, float vx, float vy, long born, int life) {}

    private static final List<Seg> SEGS = new ArrayList<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final RandomSource rnd = RandomSource.create();

    static {
        Predicate<Set<Skill>> anyRoot = u -> u.contains(Skill.SHADOW_DASH) || u.contains(Skill.PHANTOM_FORM) || u.contains(Skill.VOID_MANTLE);
        SEGS.add(new Seg(147, 188, 147, 242, u -> u.contains(Skill.SHADOW_DASH)));
        SEGS.add(new Seg(368, 188, 368, 242, u -> u.contains(Skill.PHANTOM_FORM)));
        SEGS.add(new Seg(590, 188, 590, 242, u -> u.contains(Skill.VOID_MANTLE)));
        SEGS.add(new Seg(147, 242, 590, 242, anyRoot));
        for (int x : new int[]{147, 295, 443, 590}) SEGS.add(new Seg(x, 242, x, 292, anyRoot));
        SEGS.add(new Seg(295, 406, 295, 460, u -> u.contains(Skill.SPIRIT_TWISTER)));
        SEGS.add(new Seg(443, 406, 443, 460, u -> u.contains(Skill.SHADOW_CLAWS)));
        SEGS.add(new Seg(590, 406, 590, 460, u -> u.contains(Skill.SOUL_PRISON)));
        Predicate<Set<Skill>> mid = u -> Skill.SHADOW_RAIN.requirementsMet(u);
        SEGS.add(new Seg(221, 460, 590, 460, mid));
        for (int x : new int[]{221, 368, 516}) SEGS.add(new Seg(x, 460, x, 510, mid));
        SEGS.add(new Seg(221, 624, 221, 678, u -> u.contains(Skill.SHADOW_RAIN)));
        SEGS.add(new Seg(368, 624, 368, 678, u -> u.contains(Skill.BLACK_HOLE)));
        Predicate<Set<Skill>> low = u -> Skill.ASCENSION.requirementsMet(u);
        SEGS.add(new Seg(147, 678, 443, 678, low));
        for (int x : new int[]{147, 295, 443}) SEGS.add(new Seg(x, 678, x, 727, low));
        Predicate<Set<Skill>> rift = u -> u.contains(Skill.RIFT_WALK);
        SEGS.add(new Seg(516, 624, 516, 678, rift));
        SEGS.add(new Seg(516, 678, 590, 678, rift));
        SEGS.add(new Seg(590, 678, 590, 727, rift));
    }

    private float s;
    private int ox, oy;
    private Skill selected;
    private long opened;

    public SkillTreeScreen() { super(Component.translatable("screen.voidtree.title")); }

    public static void onUnlocked(Skill skill) {
        BURSTS.add(new Burst(skill, Util.getMillis()));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.UNLOCK.get(), 1F, 0.8F));
    }

    @Override
    protected void init() {
        int top = 22, bottom = 44;
        s = Math.min((height - top - bottom) / (float) H, (width - 20) / (float) W);
        ox = (int) ((width - W * s) / 2);
        oy = top;
        opened = Util.getMillis();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private float time() { return (Util.getMillis() - opened) / 50F; }

    private Skill skillAt(double mx, double my) {
        double tx = (mx - ox) / s, ty = (my - oy) / s;
        for (Skill k : Skill.VALUES) if (Math.abs(tx - k.gx) <= HW && Math.abs(ty - k.gy) <= HH) return k;
        return null;
    }

    private int slotX(int i) { return width / 2 - 2 * 26 + i * 26 + 1; }

    private int slotY() { return height - 32; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        VoidData d = ClientHooks.DATA;
        float time = time();
        g.fill(0, 0, width, height, 0xFF1B1926);
        drawSparks(g, time);

        Skill hover = skillAt(mx, my);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(s, s, 1);
        RenderSystem.enableBlend();
        g.blit(BG, 0, 0, 0, 0, W, H, W, H);
        drawLines(g, d.unlocked, time);
        for (Skill k : Skill.VALUES) drawNode(g, k, d, k == hover, time);
        drawBursts(g);
        g.pose().popPose();
        // vignette over everything (tree included) so the backdrop has no visible edge
        g.fillGradient(0, 0, width, height / 4, 0x66000000, 0x00000000);
        g.fillGradient(0, height * 3 / 4, width, height, 0x00000000, 0x77000000);

        // header
        Minecraft mc = Minecraft.getInstance();
        int lvl = mc.player != null ? mc.player.experienceLevel : 0;
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), width / 2, 6, 0xFFFFFF);
        g.drawString(font, Component.translatable("screen.voidtree.level", lvl).withStyle(ChatFormatting.GREEN), 6, 6, 0xFFFFFF);
        String en = Component.translatable("screen.voidtree.energy").getString() + " " + (int) d.energy + "/" + (int) d.maxEnergy();
        g.drawString(font, en, width - font.width(en) - 6, 6, 0xD8B8FF);

        drawSlots(g, d, mx, my, time);
        if (selected != null) {
            g.drawCenteredString(font, Component.translatable("screen.voidtree.assign_hint", selected.displayName()), width / 2, slotY() - 11, 0xFFE070);
        } else {
            g.drawCenteredString(font, Component.translatable("screen.voidtree.hint"), width / 2, slotY() - 11, 0x8088A0);
        }
        if (hover != null) g.renderComponentTooltip(font, tooltip(hover, d, lvl), mx, my);
        super.render(g, mx, my, pt);
    }

    private List<Component> tooltip(Skill k, VoidData d, int lvl) {
        List<Component> l = new ArrayList<>();
        l.add(k.displayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        l.add(Component.translatable(k.passive ? "screen.voidtree.passive" : "screen.voidtree.active").append(" • ")
                .append(Component.translatable("screen.voidtree.tier", k.tier)).withStyle(ChatFormatting.DARK_PURPLE));
        for (FormattedText line : font.getSplitter().splitLines(k.description(), 200, net.minecraft.network.chat.Style.EMPTY)) {
            l.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
        }
        if (!k.passive) {
            float cost = Minecraft.getInstance().player != null ? SkillLogic.energyCost(Minecraft.getInstance().player, k) : k.energy;
            l.add(Component.translatable("screen.voidtree.cost", (int) cost, String.format("%.1f", k.cooldown / 20F)).withStyle(ChatFormatting.BLUE));
        }
        if (d.has(k)) {
            l.add(Component.translatable("screen.voidtree.unlocked").withStyle(ChatFormatting.GREEN));
            if (!k.passive) l.add(Component.translatable("screen.voidtree.click_select").withStyle(ChatFormatting.YELLOW));
        } else if (!k.requirementsMet(d.unlocked)) {
            Component any = Component.empty();
            Skill[] ps = k.parents();
            for (int i = 0; i < ps.length; i++) any = any.copy().append(i == 0 ? Component.empty() : Component.literal(" / ")).append(ps[i].displayName());
            l.add(Component.translatable("screen.voidtree.req_any", any).withStyle(ChatFormatting.RED));
        } else {
            boolean creative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getAbilities().instabuild;
            l.add(Component.translatable("screen.voidtree.price", k.levelCost)
                    .withStyle(lvl >= k.levelCost || creative ? ChatFormatting.GREEN : ChatFormatting.RED));
            l.add(Component.translatable("screen.voidtree.click_unlock").withStyle(ChatFormatting.YELLOW));
        }
        return l;
    }

    private void drawLines(GuiGraphics g, Set<Skill> unlocked, float time) {
        for (Seg sg : SEGS) {
            if (!sg.lit.test(unlocked)) continue;
            int x0 = Math.min(sg.x0, sg.x1), x1 = Math.max(sg.x0, sg.x1), y0 = Math.min(sg.y0, sg.y1), y1 = Math.max(sg.y0, sg.y1);
            float pulse = 0.55F + 0.2F * Mth.sin(time * 0.25F + (x0 + y0) * 0.02F);
            g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 4, argb(pulse * 0.25F, 0x7A3BFF));
            g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, argb(pulse, 0xB070FF));
            g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, argb(0.9F, 0xF4E8FF));
            // energy pulses travelling along the segment
            float len = (x1 - x0) + (y1 - y0);
            for (int k = 0; k < Math.max(1, (int) (len / 40)); k++) {
                float f = ((time * 2.2F + k * 40 + sg.x0 * 3 + sg.y0) % len) / len;
                int px = (int) Mth.lerp(f, sg.x0, sg.x1), py = (int) Mth.lerp(f, sg.y0, sg.y1);
                g.fill(px - 5, py - 5, px + 5, py + 5, argb(0.35F, 0xD8A8FF));
                g.fill(px - 3, py - 3, px + 3, py + 3, 0xFFFFFFFF);
            }
        }
    }

    private void drawNode(GuiGraphics g, Skill k, VoidData d, boolean hover, float time) {
        boolean has = d.has(k), avail = !has && k.requirementsMet(d.unlocked);
        int x0 = k.gx - HW, y0 = k.gy - HH;
        g.pose().pushPose();
        if (hover) {
            g.pose().translate(k.gx, k.gy, 0);
            g.pose().scale(1.07F, 1.07F, 1);
            g.pose().translate(-k.gx, -k.gy, 0);
        }
        if (has) {
            float p = 0.5F + 0.5F * Mth.sin(time * 0.18F + k.ordinal());
            for (int i = 6; i >= 1; i--) outline(g, x0 - i, y0 - i, x0 + IW + i, y0 + IH + i, argb((0.07F + 0.06F * p) * (7 - i) / 6F, 0xB070FF));
        } else if (avail) {
            float p = 0.5F + 0.5F * Mth.sin(time * 0.35F);
            for (int i = 4; i >= 1; i--) outline(g, x0 - i, y0 - i, x0 + IW + i, y0 + IH + i, argb((0.1F + 0.2F * p) * (5 - i) / 4F, 0xFFFFFF));
        }
        if (has) RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        else if (avail) RenderSystem.setShaderColor(0.62F, 0.62F, 0.7F, 1F);
        else RenderSystem.setShaderColor(0.24F, 0.24F, 0.3F, 1F);
        if (hover) RenderSystem.setShaderColor(Math.min(1, has ? 1.15F : 0.85F), Math.min(1, has ? 1.15F : 0.85F), 1F, 1F);
        g.blit(k.icon(), x0, y0, 0, 0, IW, IH, IW, IH);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        if (has) {
            // sheen sweeping across unlocked icons
            float f = ((time * 1.6F + k.ordinal() * 37) % 190) - 40;
            if (f > -10 && f < IW) {
                int sx = x0 + (int) f;
                g.fill(Math.max(x0 + 8, sx), y0 + 8, Math.min(x0 + IW - 8, sx + 7), y0 + IH - 8, 0x22FFFFFF);
            }
        } else if (!avail) {
            // small padlock
            int cx = k.gx, cy = k.gy + 36;
            g.fill(cx - 7, cy - 3, cx + 7, cy + 8, 0xDD3A3846);
            outline(g, cx - 5, cy - 9, cx + 5, cy - 2, 0xDD3A3846);
            g.fill(cx - 1, cy + 1, cx + 1, cy + 5, 0xFF8890A8);
        }
        if (k == selected) {
            float p = 0.5F + 0.5F * Mth.sin(time * 0.6F);
            outline(g, x0 - 3, y0 - 3, x0 + IW + 3, y0 + IH + 3, argb(0.6F + 0.4F * p, 0xFFE070));
            outline(g, x0 - 4, y0 - 4, x0 + IW + 4, y0 + IH + 4, argb(0.6F + 0.4F * p, 0xFFE070));
        }
        if (k.passive && has) {
            g.pose().pushPose();
            g.pose().translate(x0 + 7, y0 + IH - 22, 0);
            g.pose().scale(1.6F, 1.6F, 1);
            g.drawString(font, "P", 0, 0, 0xFFE070);
            g.pose().popPose();
        }
        // slot number badge
        for (int i = 0; i < VoidData.SLOTS; i++) {
            if (d.slots[i] == k) {
                g.pose().pushPose();
                g.pose().translate(x0 + IW - 22, y0 + 7, 0);
                g.pose().scale(1.8F, 1.8F, 1);
                g.drawString(font, String.valueOf(i + 1), 0, 0, 0xFFFFFF);
                g.pose().popPose();
            }
        }
        g.pose().popPose();
    }

    private void drawBursts(GuiGraphics g) {
        long now = Util.getMillis();
        BURSTS.removeIf(b -> now - b.start > 900);
        for (Burst b : BURSTS) {
            float t = (now - b.start) / 900F;
            float a = 1 - t;
            int cx = b.skill.gx, cy = b.skill.gy;
            int r = (int) (58 + t * 80);
            outline(g, cx - r, cy - r, cx + r, cy + r, argb(a, 0xE0B8FF));
            outline(g, cx - r + 2, cy - r + 2, cx + r - 2, cy + r - 2, argb(a * 0.5F, 0x8A4CFF));
            g.fill(cx - HW, cy - HH, cx + HW + 1, cy + HH, argb(Math.max(0, 1 - t * 2.5F) * 0.8F, 0xFFFFFF));
            RandomSource rr = RandomSource.create(b.skill.ordinal() * 31L);
            for (int i = 0; i < 16; i++) {
                double ang = rr.nextDouble() * Math.PI * 2, dist = 50 + t * (70 + rr.nextDouble() * 90);
                int px = cx + (int) (Math.cos(ang) * dist), py = cy + (int) (Math.sin(ang) * dist);
                g.fill(px - 3, py - 3, px + 3, py + 3, argb(a, 0xF4E8FF));
            }
        }
    }

    private void drawSparks(GuiGraphics g, float time) {
        long now = Util.getMillis();
        if (sparks.size() < 40 && rnd.nextInt(3) == 0) {
            sparks.add(new Spark(rnd.nextFloat() * width, height + 2, (rnd.nextFloat() - 0.5F) * 0.3F, -0.3F - rnd.nextFloat() * 0.6F, now, 3000 + rnd.nextInt(4000)));
        }
        sparks.removeIf(sp -> now - sp.born > sp.life);
        for (Spark sp : sparks) {
            float age = (now - sp.born) / 50F;
            float a = Mth.sin((float) (now - sp.born) / sp.life * Mth.PI) * 0.5F;
            int x = (int) (sp.x + sp.vx * age + Mth.sin(age * 0.1F + sp.x) * 6), y = (int) (sp.y + sp.vy * age);
            g.fill(x, y, x + 1, y + 1, argb(a, rnd.nextInt(5) == 0 ? 0xFFFFFF : 0xB98AFF));
        }
    }

    private void drawSlots(GuiGraphics g, VoidData d, int mx, int my, float time) {
        int y = slotY();
        for (int i = 0; i < VoidData.SLOTS; i++) {
            int x = slotX(i);
            boolean hov = mx >= x && mx < x + 24 && my >= y && my < y + 24;
            g.fill(x - 1, y - 1, x + 25, y + 25, hov || selected != null ? 0xFF6FA8FF : 0xFF3A3846);
            g.fill(x, y, x + 24, y + 24, 0xFF0F0E18);
            Skill sk = d.slots[i];
            if (sk != null) g.blit(sk.icon(), x + 1, y + 1, 22, 22, 0, 0, IW, IH, IW, IH);
            Component key = ClientSetup.SLOT_KEYS[i].getTranslatedKeyMessage();
            g.pose().pushPose();
            g.pose().translate(x + 1, y + 26, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, (i + 1) + ": " + key.getString(), 0, 0, 0xD8C0FF);
            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        VoidData d = ClientHooks.DATA;
        int y = slotY();
        for (int i = 0; i < VoidData.SLOTS; i++) {
            int x = slotX(i);
            if (mx >= x && mx < x + 24 && my >= y && my < y + 24) {
                if (button == 1) Net.CHANNEL.sendToServer(new Net.Assign(i, -1));
                else if (selected != null) Net.CHANNEL.sendToServer(new Net.Assign(i, selected.ordinal()));
                selected = null;
                click(1.2F);
                return true;
            }
        }
        Skill k = skillAt(mx, my);
        if (k != null) {
            if (d.has(k)) {
                selected = k.passive || selected == k ? null : k;
                click(1F);
            } else if (k.requirementsMet(d.unlocked)) {
                Net.CHANNEL.sendToServer(new Net.Unlock(k.ordinal()));
                click(0.8F);
            } else {
                click(0.5F);
            }
            return true;
        }
        selected = null;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientSetup.OPEN_TREE.matches(key, scan)) { onClose(); return true; }
        if (key >= 49 && key <= 52) { // 1-4: assign hovered/selected skill
            Minecraft mc = Minecraft.getInstance();
            double mx = mc.mouseHandler.xpos() * width / mc.getWindow().getScreenWidth();
            double my = mc.mouseHandler.ypos() * height / mc.getWindow().getScreenHeight();
            Skill k = selected != null ? selected : skillAt(mx, my);
            if (k != null && !k.passive && ClientHooks.DATA.has(k)) {
                Net.CHANNEL.sendToServer(new Net.Assign(key - 49, k.ordinal()));
                selected = null;
                click(1.2F);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int c) {
        g.fill(x0, y0, x1, y0 + 1, c);
        g.fill(x0, y1 - 1, x1, y1, c);
        g.fill(x0, y0 + 1, x0 + 1, y1 - 1, c);
        g.fill(x1 - 1, y0 + 1, x1, y1 - 1, c);
    }

    static int argb(float a, int rgb) { return ((int) (Mth.clamp(a, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF); }
}
