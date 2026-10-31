package com.krolasyon.cille.client;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.CilleEntity;
import com.krolasyon.cille.network.CilleNetwork;
import com.krolasyon.cille.network.CommandPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** reads the command keys every tick and sends the Cahan's orders to the server */
@Mod.EventBusSubscriber(modid = CilleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CilleClient {
    private CilleClient() {}

    public static final double RANGE = 48.0;
    /** the Cille that receives the orders (picked with the select key or by looking at it) */
    private static int selectedId = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        while (CilleKeys.SELECT.consumeClick()) cycleSelection(mc);
        while (CilleKeys.MENU.consumeClick()) {
            Entity looked = lookedAtOwn(mc);
            if (looked != null) selectedId = looked.getId();
            mc.setScreen(new CilleCommandScreen(pickTarget(mc), pickPoint(mc)));
            return;
        }
        while (CilleKeys.ATTACK.consumeClick()) send(CilleEntity.CMD_ATTACK, pickTarget(mc), null);
        while (CilleKeys.STOP.consumeClick()) send(CilleEntity.CMD_STOP, null, null);
        while (CilleKeys.DEFEND.consumeClick()) send(CilleEntity.CMD_DEFEND, null, null);
        while (CilleKeys.RETREAT.consumeClick()) send(CilleEntity.CMD_RETREAT, null, null);
        while (CilleKeys.MOVE.consumeClick()) send(CilleEntity.CMD_MOVE, null, pickPoint(mc));
        for (int i = 0; i < CilleKeys.ABILITIES.length; i++) {
            while (CilleKeys.ABILITIES[i].consumeClick()) {
                CilleEntity c = myCille(mc);
                if (c == null) continue;
                int[] list = c.commandableAbilities();
                if (i < list.length) send(CilleEntity.CMD_ABILITY + list[i], pickTarget(mc), null);
            }
        }
    }

    public static void send(int cmd, @Nullable Entity target, @Nullable Vec3 point) {
        Minecraft mc = Minecraft.getInstance();
        CilleEntity c = myCille(mc);
        CilleNetwork.CHANNEL.sendToServer(new CommandPacket(cmd, c != null ? c.getId() : -1, target != null ? target.getId() : -1, point));
    }

    private static void cycleSelection(Minecraft mc) {
        List<CilleEntity> mine = allMine(mc);
        if (mine.isEmpty()) return;
        int idx = -1;
        for (int i = 0; i < mine.size(); i++) if (mine.get(i).getId() == selectedId) idx = i;
        CilleEntity next = mine.get((idx + 1) % mine.size());
        selectedId = next.getId();
        mc.player.displayClientMessage(Component.translatable("msg.cille.selected", next.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
    }

    /** the living thing under the crosshair (up to 48 blocks), never your own Cilles */
    @Nullable
    public static Entity pickTarget(Minecraft mc) {
        return pick(mc, false);
    }

    @Nullable
    private static Entity lookedAtOwn(Minecraft mc) {
        return pick(mc, true);
    }

    @Nullable
    private static Entity pick(Minecraft mc, boolean own) {
        Entity cam = mc.getCameraEntity();
        Player me = mc.player;
        if (cam == null || me == null) return null;
        Vec3 eye = cam.getEyePosition(1F);
        Vec3 look = cam.getViewVector(1F);
        Vec3 end = eye.add(look.scale(RANGE));
        HitResult block = cam.pick(RANGE, 1F, false);
        double max = block.getType() != HitResult.Type.MISS ? block.getLocation().distanceToSqr(eye) : RANGE * RANGE;
        AABB box = cam.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(cam, eye, end, box,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && e != me && isMine(e, me) == own, max);
        return hit != null ? hit.getEntity() : null;
    }

    @Nullable
    public static Vec3 pickPoint(Minecraft mc) {
        Entity cam = mc.getCameraEntity();
        if (cam == null) return null;
        HitResult r = cam.pick(64.0, 1F, false);
        return r.getType() == HitResult.Type.BLOCK ? r.getLocation() : null;
    }

    public static boolean isMine(Entity e, Player me) {
        return e instanceof CilleEntity k && me.getUUID().equals(k.getOwnerUUID());
    }

    public static List<CilleEntity> allMine(Minecraft mc) {
        List<CilleEntity> out = new ArrayList<>();
        if (mc.level == null || mc.player == null) return out;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof CilleEntity k && k.isAlive() && isMine(k, mc.player) && k.distanceToSqr(mc.player) < 96 * 96) out.add(k);
        }
        return out;
    }

    /** the Cille that receives orders: the selected one if still around, else the closest of yours */
    @Nullable
    public static CilleEntity myCille(Minecraft mc) {
        CilleEntity best = null;
        double bd = Double.MAX_VALUE;
        for (CilleEntity k : allMine(mc)) {
            if (k.getId() == selectedId) return k;
            double d = k.distanceToSqr(mc.player);
            if (d < bd) { bd = d; best = k; }
        }
        return best;
    }
}
