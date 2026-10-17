package com.krolasyon.mirror.block;

import com.krolasyon.mirror.Config;
import com.krolasyon.mirror.MirrorWorld;
import com.krolasyon.mirror.world.HouseBlueprint;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/**
 * Lower half of the mirror. Drives the reflection's life: it copies its owner, wakes up, then builds a house in the
 * mirror dimension, pausing to ask for materials. Everything it changes over there is remembered for the merge.
 */
public class MirrorBlockEntity extends BlockEntity {
    public static final int MIMIC = 0, AWAKEN = 1, BUILD = 2, REQUEST = 3, DONE = 4;
    public static final int EVENT_SHAKE = 1, EVENT_THANKS = 2, EVENT_NOD = 3;
    private static final TicketType<ChunkPos> BUILD_TICKET = TicketType.create("mirrorworld_build", Comparator.comparingLong(ChunkPos::toLong), 200);
    private static final int PREFIX_PER_TICK = 8;

    @Nullable private UUID owner;
    private String ownerName = "";
    private int phase = MIMIC;
    private int phaseTime;
    /** Entries placed so far: first the site preparation prefix, then the house. */
    private int placed;
    private int stage;
    private int need;
    private long[] prefixPos = new long[0];
    private byte[] prefixFill = new byte[0];
    private final LongLinkedOpenHashSet changed = new LongLinkedOpenHashSet();

    // client only
    private int houseShown;
    public long shakeUntil, thanksUntil, nodUntil;

    public MirrorBlockEntity(BlockPos pos, BlockState state) {
        super(MirrorWorld.MIRROR_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MirrorBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MirrorBlockEntity be) {
        com.krolasyon.mirror.client.MirrorClientEffects.tick(level, pos, be);
    }

    private void tickServer(ServerLevel level) {
        if (level.dimension() == MirrorWorld.MIRROR_DIM) return;
        phaseTime++;
        switch (phase) {
            case MIMIC -> { if (phaseTime >= Config.MIMIC_SECONDS.get() * 20) setPhase(AWAKEN); }
            case AWAKEN -> { if (phaseTime >= Config.AWAKEN_SECONDS.get() * 20) startBuilding(level); }
            case BUILD -> buildTick(level, false);
            default -> { }
        }
        if (phaseTime % 200 == 0) setChanged();
    }

    private void setPhase(int p) {
        phase = p;
        phaseTime = 0;
        sync();
    }

    @Nullable
    private ServerLevel mirrorLevel(ServerLevel level) {
        return level.getServer().getLevel(MirrorWorld.MIRROR_DIM);
    }

    public Direction facing() {
        return getBlockState().hasProperty(MirrorBlock.FACING) ? getBlockState().getValue(MirrorBlock.FACING) : Direction.SOUTH;
    }

    /** Canonical house coordinates to world coordinates (the same in both worlds). */
    public BlockPos toWorld(int lx, int y, int lz) {
        Direction f = facing(), r = f.getCounterClockWise();
        return worldPosition.offset(r.getStepX() * lx + f.getStepX() * lz, y, r.getStepZ() * lx + f.getStepZ() * lz);
    }

    private void keepLoaded(ServerLevel mirror) {
        ChunkPos cp = new ChunkPos(toWorld(0, 0, -6));
        mirror.getChunkSource().addRegionTicket(BUILD_TICKET, cp, 2, cp);
    }

    private void startBuilding(ServerLevel level) {
        ServerLevel mirror = mirrorLevel(level);
        if (mirror == null) {
            setPhase(DONE);
            return;
        }
        keepLoaded(mirror);
        LongLinkedOpenHashSet houseSet = new LongLinkedOpenHashSet();
        for (HouseBlueprint.Entry e : HouseBlueprint.HOUSE) houseSet.add(toWorld(e.x(), e.y(), e.z()).asLong());
        LongLinkedOpenHashSet pos = new LongLinkedOpenHashSet();
        java.util.ArrayList<Byte> fill = new java.util.ArrayList<>();
        // clear the reflection's land, top down
        for (int y = HouseBlueprint.MAX_Y; y >= 0; y--) {
            for (int z = HouseBlueprint.MAX_Z; z >= HouseBlueprint.MIN_Z; z--) {
                for (int x = HouseBlueprint.MIN_X; x <= HouseBlueprint.MAX_X; x++) {
                    BlockPos p = toWorld(x, y, z);
                    if (houseSet.contains(p.asLong())) continue;
                    if (!mirror.getBlockState(p).isAir() && pos.add(p.asLong())) fill.add((byte) 0);
                }
            }
        }
        // prop the foundation up if its world is lower here
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z >= -9; z--) {
                for (int y = -2; y >= -10; y--) {
                    BlockPos p = toWorld(x, y, z);
                    BlockState s = mirror.getBlockState(p);
                    if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) break;
                    if (pos.add(p.asLong())) fill.add((byte) 1);
                }
            }
        }
        prefixPos = pos.toLongArray();
        prefixFill = new byte[fill.size()];
        for (int i = 0; i < prefixFill.length; i++) prefixFill[i] = fill.get(i);
        placed = 0;
        stage = 0;
        setPhase(BUILD);
    }

    private int prefixCount() { return prefixPos.length; }

    private int stageStart(int s) {
        int start = HouseBlueprint.REQUESTS.get(s).start();
        return s == 0 ? 0 : prefixCount() + start;
    }

    private int total() { return prefixCount() + HouseBlueprint.HOUSE.size(); }

    private void buildTick(ServerLevel level, boolean force) {
        ServerLevel mirror = mirrorLevel(level);
        if (mirror == null) return;
        if (placed >= total()) {
            setPhase(DONE);
            return;
        }
        boolean inPrefix = placed >= stageStart(0) && placed < prefixCount() && stage > 0;
        if (!force && !inPrefix && phaseTime % Config.BUILD_INTERVAL_TICKS.get() != 0) return;
        keepLoaded(mirror);
        int budget = inPrefix ? PREFIX_PER_TICK : 1;
        while (budget-- > 0 && placed < total()) {
            if (stage < HouseBlueprint.REQUESTS.size() && placed == stageStart(stage)) {
                need = HouseBlueprint.REQUESTS.get(stage).count();
                setPhase(REQUEST);
                return;
            }
            place(level, mirror, placed);
            placed++;
        }
        if (placed >= total()) setPhase(DONE);
        else sync();
    }

    private void place(ServerLevel level, ServerLevel mirror, int index) {
        BlockPos p;
        BlockState s;
        if (index < prefixCount()) {
            p = BlockPos.of(prefixPos[index]);
            s = prefixFill[index] == 1 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        } else {
            HouseBlueprint.Entry e = HouseBlueprint.HOUSE.get(index - prefixCount());
            p = toWorld(e.x(), e.y(), e.z());
            s = e.state().rotate(HouseBlueprint.rotationFor(facing()));
            // you can faintly hear it working on the other side
            level.playSound(null, worldPosition, s.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.35F, 0.7F + level.random.nextFloat() * 0.2F);
        }
        mirror.setBlock(p, s, 2 | 16);
        changed.add(p.asLong());
        if (s.is(Blocks.CHEST) && mirror.getBlockEntity(p) instanceof ChestBlockEntity chest) fillGifts(chest);
    }

    private void fillGifts(ChestBlockEntity chest) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Yansımadan Mektup");
        tag.putString("author", ownerName.isEmpty() ? "?" : ownerName);
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("mirrorworld.letter.page1"))));
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("mirrorworld.letter.page2"))));
        tag.put("pages", pages);
        tag.putBoolean("resolved", false);
        chest.setItem(0, book);
        chest.setItem(4, new ItemStack(Items.AMETHYST_SHARD, 6));
        chest.setItem(9, new ItemStack(Items.BREAD, 8));
        chest.setItem(13, new ItemStack(Items.EMERALD, 5));
        chest.setItem(17, new ItemStack(Items.POPPY, 3));
        chest.setItem(22, new ItemStack(Items.ENDER_PEARL, 2));
    }

    /** Client side: whether a right click should be swallowed by the mirror. */
    public boolean wantsInteraction(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        if (player.isCreative() && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) return true;
        return phase == REQUEST;
    }

    public boolean interact(ServerPlayer player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return false;
        if (owner == null) setOwner(player.getUUID(), player.getGameProfile().getName());
        ItemStack stack = player.getMainHandItem();
        if (player.isCreative() && player.isShiftKeyDown() && stack.isEmpty()) {
            debugAdvance();
            return true;
        }
        if (phase != REQUEST) return false;
        Item wanted = requestedItem();
        if (!stack.is(wanted)) {
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_SHAKE, 0);
            player.displayClientMessage(Component.translatable("mirrorworld.request.hint", need, new ItemStack(wanted).getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
            return true;
        }
        int take = Math.min(need, stack.getCount());
        if (!player.getAbilities().instabuild) stack.shrink(take);
        need -= take;
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.playSound(null, worldPosition, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.BLOCKS, 0.5F, 1.4F);
        Direction f = facing();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5 - f.getStepX() * 0.3, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5 - f.getStepZ() * 0.3, 25, 0.25, 0.6, 0.25, 0.02);
        if (need <= 0) {
            satisfyRequest();
        } else {
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_NOD, 0);
            sync();
        }
        return true;
    }

    private void satisfyRequest() {
        need = 0;
        stage++;
        if (level != null) level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_THANKS, 0);
        setPhase(BUILD);
    }

    /** Creative shortcut (sneak + empty hand) and the CI self test use this to skip ahead. */
    public void debugAdvance() {
        if (!(level instanceof ServerLevel sl)) return;
        switch (phase) {
            case MIMIC -> setPhase(AWAKEN);
            case AWAKEN -> startBuilding(sl);
            case REQUEST -> satisfyRequest();
            case BUILD -> {
                for (int i = 0; i < 40 && phase == BUILD; i++) buildTick(sl, true);
            }
            default -> { }
        }
    }

    public void debugBuildUntilPause() {
        if (!(level instanceof ServerLevel sl)) return;
        for (int guard = 0; phase == BUILD && guard < 10000; guard++) buildTick(sl, true);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (level != null && level.isClientSide) {
            long t = level.getGameTime();
            switch (id) {
                case EVENT_SHAKE -> shakeUntil = t + 40;
                case EVENT_THANKS -> thanksUntil = t + 80;
                case EVENT_NOD -> nodUntil = t + 30;
                default -> { return false; }
            }
        }
        return true;
    }

    public void setOwner(UUID id, String name) {
        owner = id;
        ownerName = name;
        sync();
    }

    @Nullable public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }
    public int getPhase() { return phase; }
    public int getNeed() { return need; }
    public int getStage() { return stage; }
    public int getPlaced() { return placed; }

    public Item requestedItem() {
        return stage < HouseBlueprint.REQUESTS.size() ? HouseBlueprint.REQUESTS.get(stage).item() : Items.AIR;
    }

    /** Number of house blueprint entries already standing in the mirror world. */
    public int houseShown() {
        return level != null && level.isClientSide ? houseShown : Math.max(0, placed - prefixCount());
    }

    public long[] changedPositions() { return changed.toLongArray(); }

    @Nullable
    public BlockPos homePos() {
        return houseShown() > 0 ? toWorld(0, 0, -6) : null;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeClient(tag);
        tag.putInt("PhaseTime", phaseTime);
        tag.putInt("Placed", placed);
        tag.putLongArray("PrefixPos", prefixPos);
        tag.putByteArray("PrefixFill", prefixFill);
        tag.putLongArray("Changed", changed.toLongArray());
    }

    private void writeClient(CompoundTag tag) {
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.putInt("Phase", phase);
        tag.putInt("Stage", stage);
        tag.putInt("Need", need);
        tag.putInt("HouseShown", Math.max(0, placed - prefixCount()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        phase = tag.getInt("Phase");
        stage = tag.getInt("Stage");
        need = tag.getInt("Need");
        houseShown = tag.getInt("HouseShown");
        if (tag.contains("Placed")) {
            phaseTime = tag.getInt("PhaseTime");
            placed = tag.getInt("Placed");
            prefixPos = tag.getLongArray("PrefixPos");
            prefixFill = tag.getByteArray("PrefixFill");
            if (prefixFill.length != prefixPos.length) prefixFill = new byte[prefixPos.length];
            changed.clear();
            for (long l : tag.getLongArray("Changed")) changed.add(l);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeClient(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 1, 0).inflate(0.5);
    }
}
