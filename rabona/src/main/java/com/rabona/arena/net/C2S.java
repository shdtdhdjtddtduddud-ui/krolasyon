package com.rabona.arena.net;

import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.game.*;
import com.rabona.arena.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Istemciden sunucuya paketler. */
public final class C2S {
    private C2S() {}

    /** Hareket istegi. kind: 0 dogrudan, 1 sut (baglama gore), 2 pas, 3 uzun pas, 4 mudahale. */
    public record Act(int kind, int move, float power, int side, boolean modifier) {
        public Act(FriendlyByteBuf b) { this(b.readByte(), b.readVarInt(), b.readFloat(), b.readByte(), b.readBoolean()); }

        public void encode(FriendlyByteBuf b) {
            b.writeByte(kind);
            b.writeVarInt(move);
            b.writeFloat(power);
            b.writeByte(side);
            b.writeBoolean(modifier);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p == null || p.isSpectator()) return;
                float pw = Mth.clamp(power, 0, 1.4f);
                int sd = Mth.clamp(side, -1, 1);
                if (kind == 5) {
                    Match.get(p.server).requestPass(p);
                    return;
                }
                Move m = resolve(p, this);
                if (m != null) MoveLogic.tryPerform(p, m, pw, sd, null);
            });
        }
    }

    static Move resolve(ServerPlayer p, Act a) {
        Move chosen = Move.byId(a.move());
        if (a.kind() == 0) {
            if (!chosen.selectable()) return null;
            // hava/elde top varyasyonlari
            if (chosen.req == Move.Req.BALL && MoveLogic.heldBall(p) != null) return chosen.cat == Move.Cat.PASS ? Move.GK_THROW : Move.GK_PUNT;
            return chosen;
        }
        if (MoveLogic.heldBall(p) != null) {
            return a.kind() == 2 ? Move.GK_THROW : Move.GK_PUNT;
        }
        switch (a.kind()) {
            case 1 -> {
                BallEntity foot = MoveLogic.footBall(p);
                if (foot != null && foot.getY() - p.getY() < 0.45) return chosen.cat == Move.Cat.SHOT ? chosen : Move.SHOT_POWER;
                BallEntity air = MoveLogic.airBall(p);
                if (air == null) return chosen.cat == Move.Cat.SHOT ? chosen : Move.SHOT_POWER;
                double rel = air.center().y - p.getY();
                Vec3 to = air.center().subtract(p.position());
                boolean behind = MoveLogic.face(p).dot(new Vec3(to.x, 0, to.z).normalize()) < -0.15;
                if (rel > 1.3) return p.isSprinting() && rel < 1.9 ? Move.DIVING_HEADER : Move.HEADER;
                if (behind) return Move.SCORPION;
                if (p.getXRot() < -22) return Move.BICYCLE;
                return Move.VOLLEY;
            }
            case 2 -> {
                BallEntity air = MoveLogic.airBall(p);
                if (MoveLogic.footBall(p) == null && air != null) return air.center().y - p.getY() > 1.3 ? Move.HEADER : Move.CHEST_CONTROL;
                return a.modifier() ? Move.PASS_THROUGH : Move.PASS_SHORT;
            }
            case 3 -> {
                return a.modifier() ? Move.PASS_CROSS : Move.PASS_LOB;
            }
            case 4 -> {
                return p.isSprinting() || a.modifier() ? Move.SLIDE : Move.TACKLE;
            }
            default -> {
                return null;
            }
        }
    }

    /** Menu islemleri. */
    public record Menu(int action, int value) {
        public static final int JOIN = 0, FILL_BOTS = 1, CLEAR_BOTS = 2, START = 3, STOP = 4, DURATION = 5, TEAM_SIZE = 6,
                DIFFICULTY = 7, BUILD = 8, BALL = 9, TP = 10, POS = 11, CARD_OPEN = 12, CARD_SQUAD = 13, CARD_SELL = 14, CARD_SYNC = 15;

        public Menu(FriendlyByteBuf b) { this(b.readByte(), b.readVarInt()); }

        public void encode(FriendlyByteBuf b) {
            b.writeByte(action);
            b.writeVarInt(value);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (p == null) return;
                Match m = Match.get(p.server);
                if (action == JOIN) {
                    m.join(p, Team.byId(value));
                    return;
                }
                switch (action) {
                    case POS -> { m.setPlayerPos(p, Pos.byId(value)); return; }
                    case CARD_OPEN -> { Cards.openPack(p, Cards.Pack.byId(value)); return; }
                    case CARD_SQUAD -> { Cards.toggleSquad(p, value); return; }
                    case CARD_SELL -> { Cards.sell(p, value); return; }
                    case CARD_SYNC -> { Cards.sync(p, java.util.List.of()); return; }
                    default -> {}
                }
                if (action == TP) {
                    if (m.pitch != null) {
                        Vec3 c = m.pitch.world(0, -Pitch.HALF_WID - 2, m.pitch.surfaceY());
                        p.teleportTo(m.level(), c.x, c.y, c.z, Match.yawToward(c, m.pitch.center()), 0);
                    }
                    return;
                }
                boolean admin = p.hasPermissions(2) || p.server.isSingleplayerOwner(p.getGameProfile());
                if (!admin) {
                    p.displayClientMessage(Component.translatable("msg.rabonaarena.no_permission").withStyle(ChatFormatting.RED), true);
                    return;
                }
                switch (action) {
                    case FILL_BOTS -> m.fillBots();
                    case CLEAR_BOTS -> m.clearBots();
                    case START -> m.start();
                    case STOP -> m.stop();
                    case DURATION -> { m.durationMin = Mth.clamp(value, 1, 45); m.saveData(); m.sync(); }
                    case TEAM_SIZE -> { m.teamSize = Mth.clamp(value, 1, 11); m.saveData(); m.sync(); }
                    case DIFFICULTY -> { m.difficulty = Mth.clamp(value, 0, 2); m.saveData(); m.sync(); }
                    case BUILD -> StadiumBuilder.build(p.serverLevel(), p.blockPosition().below(), value == 1, p);
                    case BALL -> {
                        BallEntity b = new BallEntity(ModEntities.BALL.get(), p.level());
                        Vec3 at = p.position().add(p.getLookAngle().multiply(2, 0, 2));
                        b.moveTo(at.x, p.getY() + 0.5, at.z, 0, 0);
                        p.level().addFreshEntity(b);
                    }
                    default -> {}
                }
            });
        }
    }
}
