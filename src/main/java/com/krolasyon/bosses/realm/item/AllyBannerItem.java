package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.entity.RealmEntities;
import com.krolasyon.bosses.realm.entity.RealmMob;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Müttefik Sancağı: every kingdom bound to you sends warriors for three minutes. */
public class AllyBannerItem extends LoreItem {
    public AllyBannerItem(Properties props) {
        super(props, true);
    }

    public static String soldier(Faction f) {
        return switch (f) {
            case ASH -> "ash_knight";
            case BLOOD -> "blood_guard";
            case SHADOW -> "shadow_assassin";
            case LEGION -> "legion_footsoldier";
            case SOUL -> "phantom_knight";
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            RealmData d = RealmData.get(sp);
            ServerLevel sl = sp.serverLevel();
            int n = 0;
            for (Faction f : Faction.ALL) {
                if (!d.bound(f)) continue;
                EntityType<? extends Mob> t = RealmEntities.type(soldier(f));
                if (t == null) continue;
                for (int i = 0; i < (d.ruler ? 2 : 1); i++) {
                    Mob m = t.create(sl);
                    if (!(m instanceof RealmMob rm)) continue;
                    double a = sp.getRandom().nextDouble() * Math.PI * 2;
                    rm.moveTo(sp.getX() + Math.cos(a) * 2.5, sp.getY(), sp.getZ() + Math.sin(a) * 2.5, sp.getYRot(), 0);
                    rm.finalizeSpawn(sl, sl.getCurrentDifficultyAt(rm.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    rm.makeAlly(sp, 3600);
                    sl.addFreshEntity(rm);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, rm.getX(), rm.getY() + 1, rm.getZ(), 20, 0.3, 0.6, 0.3, 0.2);
                    n++;
                }
            }
            if (n == 0) {
                sp.displayClientMessage(Component.translatable("message.krolasyonbosses.no_allies").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 3.0F, 1.1F);
            sp.displayClientMessage(Component.translatable("message.krolasyonbosses.allies_called").withStyle(ChatFormatting.GOLD), true);
            player.getCooldowns().addCooldown(this, 2400);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
