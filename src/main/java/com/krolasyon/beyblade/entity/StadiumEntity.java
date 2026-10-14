package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModBlocks;
import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A large beyblade stadium (about 29 blocks across). Beyblades inside ride the curved bowl (see {@link BeybladeEntity})
 * and bounce off a continuous striped wall, so they can never leave it. An invisible barrier ring keeps players and mobs
 * from walking through it. Two launch pads sit at its ends: when a player stands on each one a best-of match starts
 * (first to {@link #WIN_POINTS} points); one player sneaking on a pad starts a match against an AI blader.
 * Sneak + right click or hit it to pick it up.
 */
public class StadiumEntity extends Entity {
    /** bowl radius and floor curve: y = FLOOR_Y0 + FLOOR_RISE * (r / BOWL_R)^2 */
    public static final float BOWL_R = 10.5F, FLOOR_Y0 = 0.35F, FLOOR_RISE = 1.15F;
    public static final float LIP_R = 11.1F, LIP_Y = 1.7F, LEDGE_R = 12.2F;
    public static final float WALL_IN = 11.1F, WALL_OUT = 11.5F, WALL_TOP = 2.75F;
    /** outer base tiers (also walkable by beyblades) */
    public static final float SLOPE_R = 12.8F, TIER2_Y = 1.1F, TIER1_Y = 0.55F, APOTHEM2 = 13.4F, APOTHEM1 = 14.4F;
    /** the wall now runs all the way around: no pockets */
    public static final float WALL_HALF_DEG = 45F;
    /** launch pads: players stand here, one at each end (+z = blue side A, -z = red side B) */
    public static final float PAD_R = 17.2F, PAD_HALF = 1.6F;
    public static final double BARRIER_R0 = 14.6, BARRIER_R1 = 15.6;
    public static final int BARRIER_H = 3;

    public static final int M_IDLE = 0, M_COUNTDOWN = 1, M_BATTLE = 2, M_ROUND_END = 3, M_MATCH_END = 4;
    public static final int WIN_POINTS = 3, ROUND_TICKS = 2400, LAUNCH_GRACE = 300;

    private static final EntityDataAccessor<Integer> DATA_MSTATE = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SCORE_A = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SCORE_B = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TIMER = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROUND = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_NAME_A = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_NAME_B = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.STRING);

    // ---- server state
    private final List<BlockPos> barriers = new ArrayList<>();
    private boolean barriersDone;
    private int barrierRetry;
    @Nullable private UUID playerA, playerB, aiBey;
    private boolean vsAI;
    private int sneakTicks, waitMsg, graceTicks, outTicksA, outTicksB;
    private boolean seenA, seenB;
    private int timer;

    /** client only: cracks big impacts leave in the bowl {x, z (relative), radius, birth tick, rotation}; they fade out */
    public final List<float[]> scars = new ArrayList<>();
    public static final int SCAR_LIFE = 1800;

    public void addScar(double x, double z, float r) {
        scars.add(new float[]{(float) (x - getX()), (float) (z - getZ()), r, this.tickCount, this.random.nextFloat() * 6.283F});
        if (scars.size() > 28) scars.remove(0);
    }

    public StadiumEntity(EntityType<? extends StadiumEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    public static boolean isGap(double angleRad) {
        double deg = Math.toDegrees(angleRad);
        double m = ((deg - 45.0) % 90.0 + 90.0) % 90.0;     // 0 at a wall centre
        double off = Math.min(m, 90.0 - m);
        return off > WALL_HALF_DEG;
    }

    /** height of the playing surface above the stadium origin at horizontal distance r, or -1 beyond the base */
    public static double surface(double r) {
        if (r <= BOWL_R) {
            double k = r / BOWL_R;
            return FLOOR_Y0 + FLOOR_RISE * k * k;
        }
        if (r <= LIP_R) return FLOOR_Y0 + FLOOR_RISE + (LIP_Y - FLOOR_Y0 - FLOOR_RISE) * (r - BOWL_R) / (LIP_R - BOWL_R);
        if (r <= LEDGE_R) return LIP_Y;
        if (r <= SLOPE_R) return LIP_Y + (TIER2_Y - LIP_Y) * (r - LEDGE_R) / (SLOPE_R - LEDGE_R);
        if (r <= APOTHEM2) return TIER2_Y;
        if (r <= APOTHEM1) return TIER1_Y;
        return -1;
    }

    public static double slope(double r) {
        return r <= BOWL_R ? 2.0 * FLOOR_RISE * r / (BOWL_R * BOWL_R) : 0.0;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_MSTATE, M_IDLE);
        this.entityData.define(DATA_SCORE_A, 0);
        this.entityData.define(DATA_SCORE_B, 0);
        this.entityData.define(DATA_TIMER, 0);
        this.entityData.define(DATA_ROUND, 0);
        this.entityData.define(DATA_NAME_A, "");
        this.entityData.define(DATA_NAME_B, "");
    }

    public int matchState() { return this.entityData.get(DATA_MSTATE); }
    public int scoreA() { return this.entityData.get(DATA_SCORE_A); }
    public int scoreB() { return this.entityData.get(DATA_SCORE_B); }
    public int timerTicks() { return this.entityData.get(DATA_TIMER); }
    public int round() { return this.entityData.get(DATA_ROUND); }
    public String nameA() { return this.entityData.get(DATA_NAME_A); }
    public String nameB() { return this.entityData.get(DATA_NAME_B); }

    public Vec3 padPos(int side) { return position().add(0, 0, side == 0 ? PAD_R : -PAD_R); }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (this.level().isClientSide()) return;
        if (!barriersDone && --barrierRetry <= 0) placeBarriers();
        tickMatch();
    }

    // ------------------------------------------------------------------ invisible barrier ring
    private void placeBarriers() {
        barrierRetry = 100;
        Level l = this.level();
        int y0 = Mth.floor(getY());
        int R = (int) Math.ceil(BARRIER_R1) + 1;
        boolean all = true;
        BlockState wall = ModBlocks.STADIUM_BARRIER.get().defaultBlockState();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                BlockPos col = BlockPos.containing(getX() + dx, y0, getZ() + dz);
                double cx = col.getX() + 0.5 - getX(), cz = col.getZ() + 0.5 - getZ();
                double r = Math.sqrt(cx * cx + cz * cz);
                if (r < BARRIER_R0 || r > BARRIER_R1) continue;
                if (!l.isLoaded(col)) { all = false; continue; }
                for (int h = 0; h < BARRIER_H; h++) {
                    BlockPos p = col.above(h);
                    BlockState s = l.getBlockState(p);
                    if (s.is(wall.getBlock())) continue;
                    if (s.isAir() || s.canBeReplaced()) {
                        l.setBlock(p, wall, 2);
                        barriers.add(p.immutable());
                    }
                }
            }
        }
        barriersDone = all;
    }

    private void removeBarriers() {
        Level l = this.level();
        for (BlockPos p : barriers) {
            if (l.isLoaded(p) && l.getBlockState(p).is(ModBlocks.STADIUM_BARRIER.get())) l.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        barriers.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) {
            removeBarriers();
            despawnAI();
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------------ match
    @Nullable
    private ServerPlayer onPad(int side) {
        Vec3 p = padPos(side);
        ServerPlayer best = null;
        double bd = PAD_HALF * PAD_HALF * 2;
        for (Player pl : this.level().players()) {
            if (pl.isSpectator() || !pl.isAlive()) continue;
            double dx = pl.getX() - p.x, dz = pl.getZ() - p.z;
            if (Math.abs(pl.getY() - p.y) > 3) continue;
            double d = dx * dx + dz * dz;
            if (d < bd) { bd = d; best = (ServerPlayer) pl; }
        }
        return best;
    }

    @Nullable
    private ServerPlayer player(@Nullable UUID id) {
        if (id == null) return null;
        Player p = this.level().getPlayerByUUID(id);
        return p instanceof ServerPlayer sp && p.isAlive() && p.distanceToSqr(this) < 56 * 56 ? sp : null;
    }

    private void setState(int s, int t) {
        this.entityData.set(DATA_MSTATE, s);
        timer = t;
        this.entityData.set(DATA_TIMER, t);
    }

    private void tickMatch() {
        int st = matchState();
        if (st == M_IDLE) {
            if (this.tickCount % 5 != 0) return;
            ServerPlayer a = onPad(0), b = onPad(1);
            if (a != null && b != null && a != b) {
                startMatch(a, b);
                return;
            }
            ServerPlayer solo = a != null ? a : b;
            if (solo == null) { sneakTicks = 0; return; }
            if (solo.isShiftKeyDown()) {
                sneakTicks += 5;
                solo.displayClientMessage(Component.translatable("msg.librabeyblade.match.ai_hold", Math.max(0, (40 - sneakTicks + 19) / 20)), true);
                if (sneakTicks >= 40) {
                    sneakTicks = 0;
                    if (a != null) startMatch(a, null); else startMatch(null, b);
                }
            } else {
                sneakTicks = 0;
                if (++waitMsg % 8 == 0) solo.displayClientMessage(Component.translatable("msg.librabeyblade.match.waiting"), true);
            }
            return;
        }
        // cancel when a player left
        ServerPlayer pa = player(playerA), pb = player(playerB);
        if ((playerA != null && pa == null) || (playerB != null && pb == null)) {
            broadcast(Component.translatable("msg.librabeyblade.match.cancel"), Component.empty(), SoundEvents.NOTE_BLOCK_BASS.value());
            endMatch();
            return;
        }
        timer--;
        if (this.tickCount % 4 == 0) this.entityData.set(DATA_TIMER, Math.max(0, timer));
        switch (st) {
            case M_COUNTDOWN -> {
                if (timer == 60 || timer == 40 || timer == 20) {
                    broadcast(Component.literal(String.valueOf(timer / 20)).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD),
                            Component.translatable("msg.librabeyblade.match.ready"), SoundEvents.NOTE_BLOCK_BELL.value());
                }
                if (timer <= 0) {
                    broadcast(Component.translatable("msg.librabeyblade.match.go").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD),
                            Component.translatable("msg.librabeyblade.match.go_sub"), SoundEvents.PLAYER_ATTACK_SWEEP);
                    playAll(SoundEvents.NOTE_BLOCK_PLING.value(), 1.4F);
                    setState(M_BATTLE, 0);
                    graceTicks = LAUNCH_GRACE;
                    seenA = seenB = false;
                    outTicksA = outTicksB = 0;
                    if (vsAI) spawnAI();
                }
            }
            case M_BATTLE -> tickBattle(pa, pb);
            case M_ROUND_END -> {
                if (timer <= 0) {
                    if (scoreA() >= WIN_POINTS || scoreB() >= WIN_POINTS) {
                        boolean aWon = scoreA() >= WIN_POINTS;
                        String w = aWon ? nameA() : nameB();
                        broadcast(Component.translatable("msg.librabeyblade.match.winner", w).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD),
                                Component.translatable("msg.librabeyblade.match.final", scoreA(), scoreB()), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
                        ServerPlayer wp = aWon ? pa : pb;
                        if (wp != null) {
                            BeybladeEntity.grantXp(wp, 150);
                            wp.displayClientMessage(Component.translatable("msg.librabeyblade.match.xp", 150), false);
                        }
                        ServerPlayer lp = aWon ? pb : pa;
                        if (lp != null) BeybladeEntity.grantXp(lp, 40);
                        recallAll(pa, pb);
                        setState(M_MATCH_END, 100);
                    } else {
                        recallAll(pa, pb);
                        startCountdown();
                    }
                }
            }
            case M_MATCH_END -> {
                if (timer <= 0) endMatch();
            }
            default -> {}
        }
    }

    private void startMatch(@Nullable ServerPlayer a, @Nullable ServerPlayer b) {
        vsAI = a == null || b == null;
        playerA = a == null ? null : a.getUUID();
        playerB = b == null ? null : b.getUUID();
        this.entityData.set(DATA_NAME_A, a == null ? Component.translatable("msg.librabeyblade.match.ai").getString() : a.getGameProfile().getName());
        this.entityData.set(DATA_NAME_B, b == null ? Component.translatable("msg.librabeyblade.match.ai").getString() : b.getGameProfile().getName());
        this.entityData.set(DATA_SCORE_A, 0);
        this.entityData.set(DATA_SCORE_B, 0);
        this.entityData.set(DATA_ROUND, 0);
        broadcast(Component.translatable("msg.librabeyblade.match.start").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD),
                Component.literal(nameA() + "  vs  " + nameB()), SoundEvents.UI_TOAST_IN);
        recallAll(a, b);
        startCountdown();
    }

    /** test hook: start a match of this player against the AI */
    public void debugStartVsAI(ServerPlayer p) { startMatch(p, null); }

    private void startCountdown() {
        despawnAI();
        this.entityData.set(DATA_ROUND, round() + 1);
        setState(M_COUNTDOWN, 80);
        broadcast(Component.translatable("msg.librabeyblade.match.round", round()).withStyle(net.minecraft.ChatFormatting.YELLOW),
                Component.translatable("msg.librabeyblade.match.first_to", WIN_POINTS), SoundEvents.NOTE_BLOCK_BELL.value());
    }

    private void endMatch() {
        despawnAI();
        playerA = playerB = null;
        vsAI = false;
        this.entityData.set(DATA_NAME_A, "");
        this.entityData.set(DATA_NAME_B, "");
        setState(M_IDLE, 0);
        sneakTicks = 0;
    }

    @Nullable
    private BeybladeEntity beyOf(@Nullable ServerPlayer p, boolean aiSide) {
        if (aiSide) {
            if (aiBey == null) return null;
            Entity e = ((ServerLevel) this.level()).getEntity(aiBey);
            return e instanceof BeybladeEntity b && b.isAlive() ? b : null;
        }
        if (p == null) return null;
        BeybladeEntity b = BeybladeEntity.findActive(p);
        return b != null && b.distanceToSqr(this) < 30 * 30 ? b : null;
    }

    private boolean inside(BeybladeEntity b) {
        double dx = b.getX() - getX(), dz = b.getZ() - getZ();
        return dx * dx + dz * dz < WALL_IN * WALL_IN && b.getY() > getY() - 1 && b.getY() < getY() + 8;
    }

    private void tickBattle(@Nullable ServerPlayer pa, @Nullable ServerPlayer pb) {
        BeybladeEntity ba = beyOf(pa, playerA == null), bb = beyOf(pb, playerB == null);
        if (ba != null && inside(ba)) seenA = true;
        if (bb != null && inside(bb)) seenB = true;
        if (graceTicks > 0) {
            graceTicks--;
            if (graceTicks % 100 == 0 && graceTicks > 0) {
                if (!seenA && pa != null) pa.displayClientMessage(Component.translatable("msg.librabeyblade.match.launch_now", (graceTicks + 19) / 20), true);
                if (!seenB && pb != null) pb.displayClientMessage(Component.translatable("msg.librabeyblade.match.launch_now", (graceTicks + 19) / 20), true);
            }
            if (graceTicks == 0) {
                if (!seenA && !seenB) { roundOver(-1, 0, "msg.librabeyblade.match.no_launch"); return; }
                if (!seenA) { roundOver(1, 1, "msg.librabeyblade.match.no_launch"); return; }
                if (!seenB) { roundOver(0, 1, "msg.librabeyblade.match.no_launch"); return; }
            }
        }
        boolean downA = seenA && (ba == null || ba.getState() != BeybladeEntity.STATE_BATTLE);
        boolean downB = seenB && (bb == null || bb.getState() != BeybladeEntity.STATE_BATTLE);
        // a launched beyblade that is not in the bowl for 5 s counts as out
        if (seenA && ba != null && !downA && !inside(ba)) { if (++outTicksA > 100) downA = true; } else outTicksA = 0;
        if (seenB && bb != null && !downB && !inside(bb)) { if (++outTicksB > 100) downB = true; } else outTicksB = 0;
        if (downA && downB) { roundOver(-1, 0, "msg.librabeyblade.match.draw"); return; }
        if (downA) { scoreFinish(1, ba); return; }
        if (downB) { scoreFinish(0, bb); return; }
        // no time limit: the round lasts until a beyblade stops
    }

    /** the loser's beyblade stopped: 2 points if an enemy special move finished it, 1 for a plain spin finish */
    private void scoreFinish(int winner, @Nullable BeybladeEntity loser) {
        if (loser == null || loser.getState() == BeybladeEntity.STATE_RECALL || !loser.isAlive()) {
            roundOver(winner, 1, "msg.librabeyblade.match.forfeit");
        } else if (loser.finishedByAbility()) {
            roundOver(winner, 2, "msg.librabeyblade.match.ability_finish");
        } else {
            roundOver(winner, 1, "msg.librabeyblade.match.spin_finish");
        }
    }

    private void roundOver(int winner, int points, String reason) {
        if (winner == 0) this.entityData.set(DATA_SCORE_A, scoreA() + points);
        if (winner == 1) this.entityData.set(DATA_SCORE_B, scoreB() + points);
        Component title = winner < 0 ? Component.translatable("msg.librabeyblade.match.draw_title")
                : Component.translatable("msg.librabeyblade.match.point", winner == 0 ? nameA() : nameB(), points);
        broadcast(title.copy().withStyle(winner == 0 ? net.minecraft.ChatFormatting.BLUE : (winner == 1 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GRAY), net.minecraft.ChatFormatting.BOLD),
                Component.translatable(reason).append("  " + scoreA() + " - " + scoreB()), SoundEvents.FIREWORK_ROCKET_BLAST);
        ServerPlayer wp = winner == 0 ? player(playerA) : (winner == 1 ? player(playerB) : null);
        if (wp != null) BeybladeEntity.grantXp(wp, 25 * points);
        setState(M_ROUND_END, 80);
    }

    private void recallAll(@Nullable ServerPlayer a, @Nullable ServerPlayer b) {
        for (ServerPlayer p : new ServerPlayer[]{a, b}) {
            if (p == null) continue;
            BeybladeEntity bey = BeybladeEntity.findActive(p);
            if (bey != null) bey.recall();
        }
        despawnAI();
    }

    private void spawnAI() {
        int side = playerA == null ? 0 : 1;
        ServerPlayer human = player(side == 0 ? playerB : playerA);
        int lvl = 1;
        if (human != null) {
            BeybladeEntity hb = BeybladeEntity.findActive(human);
            lvl = hb != null ? hb.getLevel() : BeybladeEntity.heldLevel(human);
        }
        List<EntityType<? extends BeybladeEntity>> types = List.of(ModEntities.LIBRA.get(), ModEntities.LDRAGO.get(), ModEntities.LEONE.get(),
                ModEntities.EAGLE.get(), ModEntities.PEGASUS.get(), ModEntities.COMMON.get(), ModEntities.COMMON.get(), ModEntities.COMMON.get());
        EntityType<? extends BeybladeEntity> type = types.get(this.random.nextInt(types.size()));
        double z = side == 0 ? 7.5 : -7.5;
        Vec3 pos = position().add(0, 3.0, z);
        BeybladeEntity b = BeybladeEntity.launch(type, this.level(), null, ItemStack.EMPTY, pos, new Vec3(0, 0.05, side == 0 ? -0.35 : 0.35), 1F);
        if (b instanceof CommonBeybladeEntity c) c.setVariant(this.random.nextInt(CommonBeybladeEntity.VARIANTS.length));
        b.setLevel(Mth.clamp(lvl + this.random.nextInt(3) - 1, 1, BeybladeEntity.MAX_LEVEL));
        b.healSpin(b.getMaxSpin());
        aiBey = b.getUUID();
        playAll(com.krolasyon.beyblade.registry.ModSounds.LAUNCH.get(), 1F);
    }

    private void despawnAI() {
        if (aiBey == null || !(this.level() instanceof ServerLevel sl)) return;
        Entity e = sl.getEntity(aiBey);
        if (e != null) e.discard();
        aiBey = null;
    }

    private List<ServerPlayer> audience() {
        List<ServerPlayer> out = new ArrayList<>();
        for (Player p : this.level().players()) if (p instanceof ServerPlayer sp && p.distanceToSqr(this) < 48 * 48) out.add(sp);
        return out;
    }

    private void broadcast(Component title, Component sub, @Nullable SoundEvent sound) {
        for (ServerPlayer p : audience()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(4, 30, 8));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        }
        if (sound != null) playAll(sound, 1F);
    }

    private void playAll(SoundEvent s, float pitch) {
        for (ServerPlayer p : audience()) this.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 0.9F, pitch);
    }

    // ------------------------------------------------------------------ pick up
    private void pickUp(Player player) {
        if (this.level().isClientSide() || isRemoved()) return;
        if (matchState() != M_IDLE) endMatch();
        ItemStack s = new ItemStack(ModItems.STADIUM.get());
        if (!player.getAbilities().instabuild && !player.getInventory().add(s)) spawnAtLocation(s, 0.5F);
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.7F);
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        pickUp(player);
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (source.getEntity() instanceof Player p && p.isShiftKeyDown()) {
            pickUp(p);
            return true;
        }
        return false;
    }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(19, 3.5, 19); }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 192 * 192; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        barriers.clear();
        for (Tag t : tag.getList("Barriers", Tag.TAG_LONG)) barriers.add(BlockPos.of(((LongTag) t).getAsLong()));
        barriersDone = tag.getBoolean("BarriersDone");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos p : barriers) list.add(LongTag.valueOf(p.asLong()));
        tag.put("Barriers", list);
        tag.putBoolean("BarriersDone", barriersDone);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
