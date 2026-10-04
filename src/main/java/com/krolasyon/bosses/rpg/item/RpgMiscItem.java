package com.krolasyon.bosses.rpg.item;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.FX;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Simple named items with a right-click behaviour picked by kind. */
public class RpgMiscItem extends Item {
    public enum Kind { MANA_POTION, GREATER_MANA_POTION, HEALING_POTION, RING, SEAL, ESSENCE, HERB, ELF_PENDANT, ROYAL_SEAL, DEED, TROPHY, LETTER }

    public final Kind kind;
    private final String title, desc;

    public RpgMiscItem(Kind kind, String title, String desc, Properties p) {
        super(p);
        this.kind = kind;
        this.title = title;
        this.desc = desc;
    }

    public static ItemStack seal(MonsterDef boss) {
        ItemStack st = new ItemStack(RpgItems.SEAL.get());
        st.getOrCreateTag().putString("Boss", boss.id());
        return st;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (kind == Kind.SEAL && stack.hasTag()) {
            MonsterDef d = RpgDefs.BY_ID.get(stack.getTag().getString("Boss"));
            if (d != null) return Component.literal("Kadim Mühür: " + d.name());
        }
        return Component.literal(title);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return kind == Kind.SEAL || kind == Kind.RING || kind == Kind.ROYAL_SEAL; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7" + desc));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(st);
        PlayerRpg d = RpgWorldData.player(sp);
        switch (kind) {
            case MANA_POTION, GREATER_MANA_POTION -> {
                d.mana = Math.min(d.maxMana(), d.mana + (kind == Kind.MANA_POTION ? 40 : 120));
                FX.sound(sp, SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
                FX.spiral(level, FX.dust(0x40A0FF, 1.0F), sp.position(), 2, 0.6, 2, 30);
                PlayerMagic.sync(sp);
            }
            case HEALING_POTION -> {
                sp.heal(10);
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                FX.sound(sp, SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
            }
            case SEAL -> {
                MonsterDef boss = st.hasTag() ? RpgDefs.BY_ID.get(st.getTag().getString("Boss")) : null;
                EntityType<?> type = boss == null ? null : RpgEntities.typeOf(boss.id());
                if (type == null) return InteractionResultHolder.fail(st);
                Vec3 at = sp.position().add(sp.getLookAngle().multiply(1, 0, 1).normalize().scale(8));
                FX.column(level, ParticleTypes.SOUL_FIRE_FLAME, at, 6, 2, 120);
                FX.sound(sp, SoundEvents.WITHER_SPAWN, 1.0F, 1.0F);
                if (type.spawn((ServerLevel) level, net.minecraft.core.BlockPos.containing(at), MobSpawnType.EVENT) != null && !sp.getAbilities().instabuild) st.shrink(1);
            }
            case ELF_PENDANT, ROYAL_SEAL, LETTER, HERB, TROPHY, RING, ESSENCE -> {
                sp.displayClientMessage(Component.literal("§7" + desc), true);
                return InteractionResultHolder.pass(st);
            }
            case DEED -> {
                d.home = sp.blockPosition();
                sp.displayClientMessage(Component.literal("§aBurası artık senin evin. Eşin ve çocukların burada yaşayacak."), false);
                FX.sound(sp, SoundEvents.VILLAGER_CELEBRATE, 1.0F, 1.0F);
                if (!sp.getAbilities().instabuild) st.shrink(1);
            }
        }
        if ((kind == Kind.MANA_POTION || kind == Kind.GREATER_MANA_POTION || kind == Kind.HEALING_POTION) && !sp.getAbilities().instabuild) st.shrink(1);
        return InteractionResultHolder.consume(st);
    }
}
