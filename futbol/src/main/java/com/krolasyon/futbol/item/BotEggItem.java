package com.krolasyon.futbol.item;

import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class BotEggItem extends Item {
    private final Team team;

    public BotEggItem(Team team, Properties props) {
        super(props);
        this.team = team;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level instanceof ServerLevel sl) {
            FootballerEntity b = ModEntities.FOOTBALLER.get().create(sl);
            if (b != null) {
                Vec3 at = ctx.getClickLocation();
                b.setTeam(team);
                b.setNumber(2 + sl.random.nextInt(22));
                b.slot = 1 + sl.random.nextInt(4);
                float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() + 180F : 0F;
                b.moveTo(at.x, at.y, at.z, yaw, 0F);
                sl.addFreshEntity(b);
                if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(team.title + " forması giyen yapay zekalı oyuncu").withStyle(team.chat));
        tip.add(Component.literal("Maç dışında seninle top oynar, maçta mevkisine göre oynar").withStyle(ChatFormatting.DARK_GRAY));
    }
}
