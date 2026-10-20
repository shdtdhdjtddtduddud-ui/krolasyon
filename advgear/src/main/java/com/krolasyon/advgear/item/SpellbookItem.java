package com.krolasyon.advgear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Büyü kitabı: sağ tık basılı tutulunca kitap açılır, sayfalar döner ve büyü hazırlanır; bırakınca büyü yapılır.
 * Bedel deneyim puanıdır.
 */
public class SpellbookItem extends Item {
    public enum Spell {
        FIREBOLT(10, 6, 12), BLINK(14, 12, 50), STORM(20, 30, 140);

        public final int charge, cost, cooldown;

        Spell(int charge, int cost, int cooldown) {
            this.charge = charge;
            this.cost = cost;
            this.cooldown = cooldown;
        }
    }

    public final Spell spell;
    public final int variant;

    public SpellbookItem(Spell spell, int variant, Properties props) {
        super(props.stacksTo(1));
        this.spell = spell;
        this.variant = variant;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && player.totalExperience < spell.cost) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("msg.advgear.no_xp", spell.cost).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack stack, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (level.isClientSide) {
            // kitabın çevresinde dönen rünler
            for (int i = 0; i < 2; i++) {
                double a = (used * 0.35 + i * Math.PI) % (Math.PI * 2);
                Vec3 c = e.getEyePosition().add(e.getLookAngle().scale(0.8));
                level.addParticle(spell == Spell.STORM ? ParticleTypes.ELECTRIC_SPARK : spell == Spell.BLINK ? ParticleTypes.PORTAL : ParticleTypes.ENCHANT,
                        c.x + Math.cos(a) * 0.6, c.y - 0.3 + Math.sin(used * 0.2) * 0.2, c.z + Math.sin(a) * 0.6, 0, 0.02, 0);
            }
            if (used == spell.charge) level.addParticle(ParticleTypes.FLASH, e.getX(), e.getEyeY(), e.getZ(), 0, 0, 0);
        } else if (used % 6 == 0) {
            level.playSound(null, e.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1F + used * 0.02F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity e, int remaining) {
        int used = getUseDuration(stack) - remaining;
        if (used < spell.charge || !(e instanceof Player p)) return;
        if (!level.isClientSide && cast((ServerLevel) level, p)) {
            if (!p.getAbilities().instabuild) p.giveExperiencePoints(-spell.cost);
            p.getCooldowns().addCooldown(this, spell.cooldown);
        }
    }

    private boolean cast(ServerLevel level, Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        switch (spell) {
            case FIREBOLT -> {
                for (int i = -1; i <= 1; i++) {
                    Vec3 d = look.yRot(i * 0.12F);
                    SmallFireball f = new SmallFireball(level, p, d.x, d.y, d.z);
                    f.setPos(eye.x + d.x, eye.y + d.y - 0.1, eye.z + d.z);
                    level.addFreshEntity(f);
                }
                level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1F, 1.2F);
                return true;
            }
            case BLINK -> {
                BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(12)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                Vec3 to = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(12)) : hit.getLocation().subtract(look.scale(0.8));
                to = new Vec3(to.x, to.y - p.getEyeHeight() + 0.2, to.z);
                for (int k = 0; k < 4; k++) {
                    Vec3 t = to.add(0, k, 0);
                    if (level.noCollision(p, p.getBoundingBox().move(t.subtract(p.position())))) {
                        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.6, 0.3, 0.1);
                        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1F, 1.3F);
                        p.teleportTo(t.x, t.y, t.z);
                        p.resetFallDistance();
                        level.sendParticles(ParticleTypes.PORTAL, t.x, t.y + 1, t.z, 60, 0.3, 0.6, 0.3, 0.5);
                        return true;
                    }
                }
                return false;
            }
            case STORM -> {
                BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(40)), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, p));
                if (hit.getType() == HitResult.Type.MISS) return false;
                BlockPos pos = hit.getBlockPos();
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt == null) return false;
                bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
                if (p instanceof net.minecraft.server.level.ServerPlayer sp) bolt.setCause(sp);
                level.addFreshEntity(bolt);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 1, 0.3, 1, 0.05);
                level.playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1F, 0.6F);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.advgear." + net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath())
                .withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.advgear.spell_cost", spell.cost).withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable("tooltip.advgear.spell_use").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(com.krolasyon.advgear.client.ClientHooks.ITEM_EXTENSIONS);
    }
}
