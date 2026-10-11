package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ClientHooks {
    public static float trauma;
    public static float flash;
    public static int flashColor = 0x55FF33;

    private ClientHooks() {}

    public static void onSync(Net.Sync s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(s.entity());
        if (!(e instanceof Player p)) return;
        OmniData d = OmniData.get(p);
        Alien before = d.alien;
        int prevAbility = d.ability;
        d.alien = Alien.byId(s.alien());
        d.ability = s.ability();
        d.abilityTick = s.abilityTick();
        d.blocking = s.blocking();
        d.flags = s.flags();
        d.timeLeft = s.timeLeft();
        d.recharge = s.recharge();
        d.targetId = s.target();
        for (int i = 0; i < 4 && i < s.cd().length; i++) {
            d.cooldown[i] = s.cd()[i];
            d.cooldownMax[i] = s.cdMax()[i];
        }
        if (d.ability >= 0 && (prevAbility != d.ability || s.abilityTick() <= 1)) d.clientAbilityStart = mc.level.getGameTime() - s.abilityTick();
        if (before != d.alien) {
            d.clientPrevAlien = before;
            d.clientTransformTime = mc.level.getGameTime();
            d.blockAmount = d.blockAmountO = 0;
            if (p == mc.player) {
                flash = 1F;
                flashColor = d.alien == null && s.recharge() > 100 ? 0xFF3322 : 0x55FF33;
            }
        }
        p.refreshDimensions();
    }

    public static void openDial() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) mc.setScreen(new OmnitrixScreen());
    }

    public static void shake(float amount) {
        trauma = Math.min(1F, trauma + amount);
    }
}
