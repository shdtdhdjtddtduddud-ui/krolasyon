package com.krolasyon.cille.client;

import com.krolasyon.cille.CilleMod;
import com.krolasyon.cille.entity.KabkabaEntity;
import com.krolasyon.cille.network.CilleNetwork;
import com.krolasyon.cille.network.CommandPacket;
import net.minecraft.client.Minecraft;
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

/** reads the command keys every tick and sends the Cahan's orders to the server */
@Mod.EventBusSubscriber(modid = CilleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CilleClient {
    private CilleClient() {}

    public static final double RANGE = 48.0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        while (CilleKeys.MENU.consumeClick()) {
            Entity t = pickTarget(mc);
            mc.setScreen(new CilleCommandScreen(t, pickPoint(mc)));
            return;
        }
        while (CilleKeys.ATTACK.consumeClick()) send(KabkabaEntity.CMD_ATTACK, pickTarget(mc), null);
        while (CilleKeys.STOP.consumeClick()) send(KabkabaEntity.CMD_STOP, null, null);
        while (CilleKeys.DEFEND.consumeClick()) send(KabkabaEntity.CMD_DEFEND, null, null);
        while (CilleKeys.RETREAT.consumeClick()) send(KabkabaEntity.CMD_RETREAT, null, null);
        while (CilleKeys.MOVE.consumeClick()) send(KabkabaEntity.CMD_MOVE, null, pickPoint(mc));
        for (int i = 0; i < CilleKeys.ABILITIES.length; i++) {
            while (CilleKeys.ABILITIES[i].consumeClick()) send(KabkabaEntity.CMD_ABILITY + CilleKeys.ABILITY_IDS[i], pickTarget(mc), null);
        }
    }

    public static void send(int cmd, @Nullable Entity target, @Nullable Vec3 point) {
        CilleNetwork.CHANNEL.sendToServer(new CommandPacket(cmd, target != null ? target.getId() : -1, point));
    }

    /** the living thing under the crosshair (up to 48 blocks), never your own Cilles */
    @Nullable
    public static Entity pickTarget(Minecraft mc) {
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
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && e != me && !isMine(e, me), max);
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
        return e instanceof KabkabaEntity k && me.getUUID().equals(k.getOwnerUUID());
    }

    /** the player's own Kabkaba closest to them (client side, for the HUD) */
    @Nullable
    public static KabkabaEntity myKabkaba(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;
        KabkabaEntity best = null;
        double bd = 96 * 96;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof KabkabaEntity k && k.isAlive() && isMine(k, mc.player)) {
                double d = k.distanceToSqr(mc.player);
                if (d < bd) { bd = d; best = k; }
            }
        }
        return best;
    }
}
