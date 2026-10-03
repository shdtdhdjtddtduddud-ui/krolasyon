package com.krolasyon.bosses.realm.portal;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.RealmConfig;
import com.krolasyon.bosses.realm.registry.RealmBlocks;
import com.krolasyon.bosses.realm.registry.RealmItems;
import com.krolasyon.bosses.realm.world.Builders;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID)
public final class PortalEvents {
    private PortalEvents() {}

    /** flint and steel, fire charges or the Ember Key light a Cursed Obsidian frame */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        ItemStack stack = e.getItemStack();
        boolean igniter = stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(RealmItems.EMBER_KEY.get());
        if (!igniter || e.getFace() == null) return;
        Level level = e.getLevel();
        if (!level.getBlockState(e.getPos()).is(RealmBlocks.CURSED_OBSIDIAN.get())) return;
        if (level.dimension() != Level.OVERWORLD && level.dimension() != Realm.REALM) return;
        BlockPos start = e.getPos().relative(e.getFace());
        if (level.isClientSide()) {
            if (PortalShape.find(level, start, net.minecraft.core.Direction.Axis.X) != null
                    || PortalShape.find(level, start, net.minecraft.core.Direction.Axis.Z) != null) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        PortalShape shape = PortalShape.tryLight(level, start);
        if (shape == null) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        PortalLinks.get((ServerLevel) level).add(shape.bottomLeft);
        level.playSound(null, start, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 1.4F);
        level.playSound(null, start, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        if (!e.getEntity().getAbilities().instabuild) {
            if (stack.is(Items.FIRE_CHARGE)) stack.shrink(1);
            else stack.hurtAndBreak(1, e.getEntity(), p -> p.broadcastBreakEvent(e.getHand()));
        }
    }

    /** crimson rifts: realm gates that tear themselves open near players during the night */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 200 != 37) return;
        if (!RealmConfig.RIFTS.get() || p.level().dimension() != Level.OVERWORLD || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        if (!level.isNight() || level.getDayTime() < 24000L) return;
        long last = p.getPersistentData().getLong("krolasyon_rift");
        if (last != 0 && level.getGameTime() - last < RealmConfig.RIFT_MIN_DAYS.get() * 24000L) return;
        if (p.getRandom().nextDouble() >= RealmConfig.RIFT_CHANCE.get()) return;
        if (PortalLinks.get(level).nearest(level, p.blockPosition(), 160, false) != null) return;
        openRift(level, p);
    }

    public static boolean openRift(ServerLevel level, ServerPlayer p) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            double d = 22 + p.getRandom().nextDouble() * 16;
            int x = Mth.floor(p.getX() + Math.cos(a) * d), z = Mth.floor(p.getZ() + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (y <= level.getSeaLevel() || !level.getFluidState(at.below()).isEmpty()) continue;
            BlockPos portal = Builders.portalRuin(level, at, p.getRandom(), true);
            PortalLinks.get(level).add(portal);
            p.getPersistentData().putLong("krolasyon_rift", level.getGameTime());
            for (int i = 0; i < 3; i++) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(x + p.getRandom().nextInt(7) - 3, y, z + p.getRandom().nextInt(7) - 3);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
            level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 1.2F, 0.6F);
            p.sendSystemMessage(Component.translatable("message.krolasyonbosses.rift").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return true;
        }
        return false;
    }
}
