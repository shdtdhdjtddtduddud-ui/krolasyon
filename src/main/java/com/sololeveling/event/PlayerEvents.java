package com.sololeveling.event;

import com.sololeveling.SoloLeveling;
import com.sololeveling.gen.Content;
import com.sololeveling.item.SLArmorItem;
import com.sololeveling.net.Net;
import com.sololeveling.net.Packets;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModItems;
import com.sololeveling.system.*;
import com.sololeveling.world.GateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class PlayerEvents {
    private PlayerEvents() {}

    private static final Map<UUID, double[]> LAST = new HashMap<>();

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        SLPlayer d = ModCaps.get(sp);
        boolean first = !d.awakened;
        if (first) {
            d.awakened = true;
            d.discovered.add("seoul");
            d.mana = d.maxMana();
            d.skills.add("shadow_extraction");
            d.skills.add("sprint");
            d.gold = 300;
            sp.getInventory().add(new ItemStack(ModItems.get("hunter_license")));
            sp.getInventory().add(new ItemStack(ModItems.get("hunter_dagger")));
            sp.getInventory().add(new ItemStack(ModItems.get("hp_potion_small"), 3));
            sp.getInventory().add(new ItemStack(ModItems.get("mp_potion_small"), 2));
            sp.getInventory().add(new ItemStack(ModItems.get("world_map")));
        }
        for (Content.SkillDef s : Content.SKILLS) if (d.level >= s.level()) d.skills.add(s.id());
        Stats.apply(sp);
        Quests.checkDaily(sp);
        PlayerSync.sync(sp);
        NewsManager.send(sp);
        if (first) {
            Scheduler.later(60, () -> {
                Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.chosen"), Component.translatable("gui.sololeveling.chosen_body"));
                Net.toPlayer(sp, new Packets.Fx("levelup", 1));
            });
        } else {
            Scheduler.later(40, () -> Sys.notify(sp, Sys.INFO, Component.translatable("gui.sololeveling.system"), Component.translatable("gui.sololeveling.welcome_back", d.level)));
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Stats.apply(sp);
            sp.setHealth(sp.getMaxHealth());
            PlayerSync.sync(sp);
        }
    }

    @SubscribeEvent
    public static void dim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Stats.apply(sp);
            PlayerSync.sync(sp);
            NewsManager.send(sp);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        SLPlayer d = ModCaps.get(sp);
        double[] last = LAST.get(sp.getUUID());
        if (last != null && sp.tickCount > 1) {
            double dx = sp.getX() - last[0], dz = sp.getZ() - last[1];
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 3 && sp.onGround()) d.dqDist += (float) dist;
        }
        LAST.put(sp.getUUID(), new double[]{sp.getX(), sp.getZ()});

        // mana regeneration
        float regen = d.manaRegenPerSecond() / 20F;
        regen *= (float) Guilds.manaRegenMultiplier(d);
        if (d.fatigue >= 80) regen *= 0.5F;
        regen *= 1F + setBonus(sp, "hunter") * 0.5F + setBonus(sp, "monarch") * 1.0F;
        if (d.mana < d.maxMana()) d.mana = Math.min(d.maxMana(), d.mana + regen);

        if (sp.tickCount % 20 == 0) {
            if (sp.tickCount % 100 == 0 && d.fatigue > 0) d.fatigue--;
            if (d.fatigue >= 100) {
                sp.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            }
            if (sp.tickCount % 40 == 0) checkGear(sp, d);
            if (sp.tickCount % 200 == 0) Quests.checkDaily(sp);
            if (Quests.dailyDone(d) && !d.dqClaimed && sp.tickCount % 600 == 0)
                Sys.notify(sp, Sys.QUEST, Component.translatable("gui.sololeveling.daily_complete"), Component.translatable("gui.sololeveling.daily_claim_hint"));
            PlayerSync.sync(sp);
        }
    }

    private static int setBonus(ServerPlayer sp, String set) {
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = sp.getItemBySlot(s);
            if (!(st.getItem() instanceof SLArmorItem a) || !a.def.id().equals(set)) return 0;
        }
        return 1;
    }

    private static void checkGear(ServerPlayer sp, SLPlayer d) {
        boolean under = false;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = sp.getItemBySlot(s);
            if (st.getItem() instanceof SLArmorItem a && a.def.level() > d.level) under = true;
        }
        if (under) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
            sp.displayClientMessage(Component.translatable("gui.sololeveling.armor_too_heavy"), true);
        }
        if (setBonus(sp, "knight") == 1) sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, true, false));
        if (setBonus(sp, "ice") == 1) {
            sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
            sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (setBonus(sp, "monarch") == 1) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, true, false));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST.remove(e.getEntity().getUUID());
    }
}
