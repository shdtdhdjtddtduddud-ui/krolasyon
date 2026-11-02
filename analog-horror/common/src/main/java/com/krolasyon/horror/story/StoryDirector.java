package com.krolasyon.horror.story;

import com.krolasyon.horror.block.CounterBlock;
import com.krolasyon.horror.block.PropBlock;
import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.house.HauntedHouse;
import com.krolasyon.horror.registry.ModBlocks;
import com.krolasyon.horror.registry.ModEntities;
import com.krolasyon.horror.registry.ModItems;
import com.krolasyon.horror.registry.ModSounds;
import com.krolasyon.horror.story.StoryBridge.Msg;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Runs "The Hatman: Rent Due" - a linear story in five chapters inside the Ardel house. Every step has an entry action
 * (objective, checkpoint, monster, cutscene) and a completion condition; interactions with notes, pickups, doors, the VCR,
 * fuse box, candles, dials, fireplace and hat drive it forward. Being caught by the Hatman never kills: it jumpscares,
 * fades out and returns the player to the last checkpoint.
 */
public final class StoryDirector {
    private StoryDirector() {}

    public static final int NOTES_FOR_SECRET = 16;

    // ------------------------------------------------------------------ start / finish
    public static void start(ServerPlayer p) {
        if (!StoryBridge.available(p)) {
            p.sendSystemMessage(Component.translatable("msg.krolasyonhorror.singleplayer"));
            return;
        }
        ServerLevel l = p.serverLevel();
        BlockPos origin = BlockPos.containing(p.getX() - 10, p.getY(), p.getZ() - 30);
        HauntedHouse.buildStory(l, origin);
        StoryState s = StoryState.get(p);
        s.active = true;
        s.finished = false;
        s.origin = origin;
        s.flags.clear();
        s.notes.clear();
        s.fusesInserted = 0;
        s.candles = "";
        s.dials = new int[4];
        s.chapter = 0;
        s.step = "intro";
        s.media = false;
        s.caughtTimer = 0;
        clearHatmen(l, s);
        GameRules g = l.getGameRules();
        g.getRule(GameRules.RULE_DAYLIGHT).set(false, l.getServer());
        g.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, l.getServer());
        g.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, l.getServer());
        g.getRule(GameRules.RULE_KEEPINVENTORY).set(true, l.getServer());
        if (l.getDifficulty() == Difficulty.PEACEFUL) l.getServer().setDifficulty(Difficulty.NORMAL, true);
        l.setDayTime(18000);
        l.setWeatherParameters(0, 1_000_000, true, false);
        p.getInventory().clearContent();
        p.setGameMode(GameType.ADVENTURE);
        p.setHealth(p.getMaxHealth());
        p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20 * 60 * 60 * 3, 1, false, false));
        StoryBridge.send(p, Msg.of("active", "", 1));
        enter(p, s, "intro");
        s.save(p);
    }

    public static void stop(ServerPlayer p) {
        StoryState s = StoryState.get(p);
        if (!s.active) return;
        ServerLevel l = p.serverLevel();
        clearHatmen(l, s);
        s.active = false;
        s.media = false;
        s.save(p);
        p.setGameMode(GameType.SURVIVAL);
        StoryBridge.send(p, Msg.of("end"));
        Vec3 out = w(s, 10.5, 1, 27.5);
        p.teleportTo(l, out.x, out.y, out.z, 180F, 0F);
    }

    private static void finish(ServerPlayer p, StoryState s) {
        s.finished = true;
        stop(p);
        p.sendSystemMessage(Component.translatable("msg.krolasyonhorror.story_done"));
    }

    // ------------------------------------------------------------------ helpers
    private static Vec3 w(StoryState s, double x, double y, double z) {
        return new Vec3(s.origin.getX() + x, s.origin.getY() + y, s.origin.getZ() + z);
    }

    private static boolean in(ServerPlayer p, StoryState s, double x0, double y0, double z0, double x1, double y1, double z1) {
        double x = p.getX() - s.origin.getX(), y = p.getY() - s.origin.getY(), z = p.getZ() - s.origin.getZ();
        return x >= x0 && x <= x1 + 1 && y >= y0 - 0.5 && y <= y1 + 1 && z >= z0 && z <= z1 + 1;
    }

    private static boolean isAt(StoryState s, BlockPos pos, int x, int y, int z) {
        return pos.equals(s.at(x, y, z));
    }

    private static boolean isDoor(StoryState s, BlockPos pos, int x, int y, int z) {
        return pos.equals(s.at(x, y, z)) || pos.equals(s.at(x, y + 1, z));
    }

    private static void cp(StoryState s, double x, double y, double z, float yaw) {
        s.cpX = x; s.cpY = y; s.cpZ = z; s.cpYaw = yaw;
    }

    private static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("msg.krolasyonhorror." + key, args), true);
    }

    private static void sound(ServerPlayer p, String name, float vol) {
        SoundEvent e = ModSounds.byName(name);
        if (e != null) p.playNotifySound(e, SoundSource.MASTER, vol, 1F);
    }

    private static void soundAt(ServerLevel l, StoryState s, double x, double y, double z, String name, float vol) {
        SoundEvent e = ModSounds.byName(name);
        Vec3 v = w(s, x, y, z);
        if (e != null) l.playSound(null, v.x, v.y, v.z, e, SoundSource.HOSTILE, vol, 1F);
    }

    private static void objective(ServerPlayer p, StoryState s, String key, int arg) {
        s.objective = key;
        s.objectiveArg = arg;
        StoryBridge.send(p, Msg.of("objective", key, arg));
    }

    private static void chapter(ServerPlayer p, StoryState s, int n) {
        if (s.chapter >= n) return;
        s.chapter = n;
        s.chapterTicks = 0;
        StoryBridge.send(p, Msg.of("chapter", "", n));
    }

    private static void media(ServerPlayer p, StoryState s, boolean on) {
        s.media = on;
        s.mediaTicks = 0;
        for (HatmanEntity h : hatmen(p.serverLevel(), s)) {
            if (on) {
                h.setNoAi(true);
                h.getNavigation().stop();
            } else h.setNoAi(h.storyMode == 0 || h.storyMode == 3);
        }
    }

    private static void cutscene(ServerPlayer p, StoryState s, String id) {
        media(p, s, true);
        s.mediaWhat = "cs:" + id;
        StoryBridge.send(p, new Msg("cutscene", id, 0, s.origin.getX(), s.origin.getY(), s.origin.getZ()));
    }

    private static void tape(ServerPlayer p, StoryState s, String id) {
        media(p, s, true);
        s.mediaWhat = "tape:" + id;
        StoryBridge.send(p, Msg.of("tape", id));
    }

    private static ItemStack stack(int idx) { return new ItemStack(ModItems.STORY[idx].get()); }

    private static int count(ServerPlayer p, int idx) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() == ModItems.STORY[idx].get()) n += st.getCount();
        }
        return n;
    }

    private static boolean has(ServerPlayer p, int idx) { return count(p, idx) > 0; }

    private static void take(ServerPlayer p, int idx) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.getItem() == ModItems.STORY[idx].get()) {
                st.shrink(1);
                return;
            }
        }
    }

    private static void give(ServerPlayer p, int idx) {
        ItemStack st = stack(idx);
        Component name = st.getHoverName();
        p.getInventory().add(st);
        say(p, "picked", name);
        sound(p, "story.pickup", 1F);
    }

    private static List<HatmanEntity> hatmen(ServerLevel l, StoryState s) {
        Vec3 a = w(s, -6, -3, -6), b = w(s, 27, 26, 30);
        return l.getEntitiesOfClass(HatmanEntity.class, new AABB(a.x, a.y, a.z, b.x, b.y, b.z), e -> e.storyControlled && e.isAlive());
    }

    private static void clearHatmen(ServerLevel l, StoryState s) {
        for (HatmanEntity h : hatmen(l, s)) h.discard();
    }

    private static HatmanEntity spawn(ServerLevel l, StoryState s, double x, double y, double z, float yaw) {
        HatmanEntity h = ModEntities.HATMAN.get().create(l);
        if (h == null) return null;
        Vec3 v = w(s, x, y, z);
        h.storyControlled = true;
        h.moveTo(v.x, v.y, v.z, yaw, 0F);
        h.setYHeadRot(yaw);
        h.setYBodyRot(yaw);
        l.addFreshEntity(h);
        return h;
    }

    private static void setDoor(ServerLevel l, StoryState s, int x, int y, int z, boolean open) {
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos p = s.at(x, y + dy, z);
            BlockState st = l.getBlockState(p);
            if (st.getBlock() instanceof DoorBlock) l.setBlock(p, st.setValue(DoorBlock.OPEN, open), 10);
        }
    }

    private static void resend(ServerPlayer p, BlockPos pos) {
        ServerLevel l = p.serverLevel();
        p.connection.send(new ClientboundBlockUpdatePacket(l, pos));
        p.connection.send(new ClientboundBlockUpdatePacket(l, pos.above()));
        p.connection.send(new ClientboundBlockUpdatePacket(l, pos.below()));
    }

    private static void lights(ServerLevel l, StoryState s, boolean ground, boolean upper) {
        HauntedHouse.setLights(l, s.origin, 1, 5, ground);
        HauntedHouse.setLights(l, s.origin, 6, 17, upper);
    }

    private static void restoreLights(ServerLevel l, StoryState s) {
        boolean black = s.has("blackout");
        lights(l, s, !black, s.has("power") && !black);
    }

    // ------------------------------------------------------------------ steps
    private static void enter(ServerPlayer p, StoryState s, String step) {
        ServerLevel l = p.serverLevel();
        if (StoryProps.stage(step) < StoryProps.stage(s.step) && s.active && !step.startsWith("end")) step = s.step;
        s.step = step;
        s.stepTicks = 0;
        s.appSeen = 0;
        StoryProps.sync(l, s);
        switch (step) {
            case "intro" -> {
                cp(s, 10.5, 1, 20.5, 180F);
                Vec3 v = w(s, 10.5, 1, 20.5);
                p.teleportTo(l, v.x, v.y, v.z, 180F, 0F);
                cutscene(p, s, "intro");
            }
            case "ch1_find" -> {
                chapter(p, s, 1);
                cp(s, 10.5, 1, 20.5, 180F);
                objective(p, s, "find_tapes", -1);
            }
            case "ch1_play" -> objective(p, s, "watch_tape1", -1);
            case "ch1_knock" -> {
                objective(p, s, "knocking", -1);
                soundAt(l, s, 12.5, 2, 8.5, "fx.knock", 1.2F);
            }
            case "ch1_storage" -> objective(p, s, "search_storage", -1);
            case "ch1_stairs" -> objective(p, s, "go_upstairs", -1);
            case "ch2_start" -> cutscene(p, s, "upstairs");
            case "ch2_fuses" -> {
                chapter(p, s, 2);
                cp(s, 15.5, 1, 15.5, 0F);
                objective(p, s, "fuses", fuseTotal(p, s));
            }
            case "ch2_fusebox" -> objective(p, s, "fusebox", -1);
            case "ch2_power" -> {
                s.flags.add("power");
                restoreLights(l, s);
                cutscene(p, s, "power_on");
            }
            case "ch2_watch" -> objective(p, s, "watch_tv", -1);
            case "ch2_climb" -> {
                cp(s, 15.5, 7, 19.5, 90F);
                objective(p, s, "black_room", -1);
                clearHatmen(l, s);
            }
            case "ch3_start" -> {
                clearHatmen(l, s);
                cutscene(p, s, "mira_room");
            }
            case "ch3_candles" -> {
                chapter(p, s, 3);
                cp(s, 16.5, 7, 9.5, 180F);
                objective(p, s, "candles", -1);
            }
            case "ch3_tape" -> objective(p, s, "watch_tape3", -1);
            case "ch3_key" -> {
                cp(s, 10.5, 7, 19.5, 180F);
                objective(p, s, "attic_key", -1);
            }
            case "ch3_escape" -> {
                cp(s, 3.5, 7, 4.5, 180F);
                objective(p, s, "escape_attic", -1);
                List<HatmanEntity> hs = hatmen(l, s);
                HatmanEntity h = hs.isEmpty() ? spawn(l, s, 15.5, 7, 18.5, 90F) : hs.get(0);
                if (h != null) h.storyHunt(p);
            }
            case "ch4_start" -> {
                clearHatmen(l, s);
                cutscene(p, s, "attic");
            }
            case "ch4_dials" -> {
                chapter(p, s, 4);
                cp(s, 10.5, 13, 12.5, 180F);
                objective(p, s, "dials", -1);
            }
            case "ch4_chase" -> {
                cp(s, 10.5, 13, 12.5, 180F);
                s.flags.add("blackout");
                restoreLights(l, s);
                objective(p, s, "escape_down", -1);
            }
            case "ch5_start" -> {
                clearHatmen(l, s);
                s.flags.remove("blackout");
                s.flags.add("front_open");
                studio(l, s);
                restoreLights(l, s);
                cutscene(p, s, "twist");
            }
            case "ch5_master" -> {
                chapter(p, s, 5);
                cp(s, 4.5, 1, 20.5, 180F);
                objective(p, s, "master_tape", -1);
                ensureHost(l, s);
            }
            case "ch5_final" -> {
                objective(p, s, "final", -1);
                ensureHost(l, s);
            }
            case "end_host", "end_evict", "end_return" -> {
                objective(p, s, "", -1);
                if (!step.equals("end_host")) clearHatmen(l, s);
                if (step.equals("end_evict")) {
                    BlockPos fp = s.at(1, 1, 18);
                    BlockState fs = l.getBlockState(fp);
                    if (fs.getBlock() instanceof CampfireBlock) l.setBlock(fp, fs.setValue(CampfireBlock.LIT, true), 3);
                    BlockPos tv = s.at(4, 1, 13);
                    BlockState ts = l.getBlockState(tv);
                    if (ts.getBlock() == ModBlocks.CRT_TV.get())
                        l.setBlock(tv, ts.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, false), 3);
                }
                cutscene(p, s, step);
            }
            default -> { }
        }
        s.save(p);
    }

    /** the digits actually shown on the four dials (x = 8..11, left to right seen from the front) */
    private static int[] readDials(ServerLevel l, StoryState s) {
        int[] d = new int[4];
        for (int i = 0; i < 4; i++) {
            BlockState b = l.getBlockState(s.at(8 + i, 14, 4));
            d[i] = b.getBlock() == ModBlocks.DIAL.get() ? b.getValue(CounterBlock.DIGIT) : 0;
        }
        return d;
    }

    /** 1102 read from the front, or the same digits read from behind the desk */
    private static boolean dialsSolved(int[] d) {
        return d[0] == 1 && d[1] == 1 && d[2] == 0 && d[3] == 2 || d[0] == 2 && d[1] == 0 && d[2] == 1 && d[3] == 1;
    }

    private static void trySolveDials(ServerPlayer p, StoryState s, ServerLevel l) {
        boolean inAttic = p.getY() - s.origin.getY() >= 12.5;
        boolean ready = s.step.equals("ch4_dials") || inAttic && (s.step.equals("ch4_start") || s.step.equals("ch3_escape"));
        if (!ready || s.media || !dialsSolved(readDials(l, s))) return;
        if (!s.step.equals("ch4_dials")) {
            clearHatmen(l, s);
            s.step = "ch4_dials";
            chapter(p, s, 4);
        }
        sound(p, "story.radio_sweep", 1F);
        tape(p, s, "tape4");
    }

    private static int fuseTotal(ServerPlayer p, StoryState s) { return Math.min(3, s.fusesInserted + count(p, ModItems.FUSE)); }

    private static void ensureHost(ServerLevel l, StoryState s) {
        if (hatmen(l, s).isEmpty()) {
            HatmanEntity h = spawn(l, s, 4.5, 1, 15.2, 0F);
            if (h != null) h.storyHost();
        }
    }

    /** after a cutscene / tape / credits finishes on the client */
    private static void afterMedia(ServerPlayer p, StoryState s, String what) {
        switch (what) {
            case "cs:intro" -> enter(p, s, "ch1_find");
            case "tape:tape1" -> enter(p, s, "ch1_knock");
            case "cs:upstairs" -> enter(p, s, "ch2_fuses");
            case "cs:power_on" -> enter(p, s, "ch2_watch");
            case "tape:tape2" -> enter(p, s, "ch2_climb");
            case "cs:mira_room" -> enter(p, s, "ch3_candles");
            case "tape:tape3" -> enter(p, s, "ch3_key");
            case "cs:attic" -> enter(p, s, "ch4_dials");
            case "tape:tape4" -> enter(p, s, "ch4_chase");
            case "cs:twist" -> enter(p, s, "ch5_master");
            case "cs:end_host" -> StoryBridge.send(p, Msg.of("credits", "host"));
            case "cs:end_evict" -> StoryBridge.send(p, Msg.of("credits", "evict"));
            case "cs:end_return" -> StoryBridge.send(p, Msg.of("credits", "return"));
            case "credits" -> finish(p, s);
            default -> { }
        }
    }

    private static void tickStep(ServerPlayer p, StoryState s, ServerLevel l) {
        switch (s.step) {
            case "ch1_find" -> {
                if (has(p, ModItems.TAPE_BIRTHDAY)) enter(p, s, "ch1_play");
            }
            case "ch1_knock" -> {
                if (!s.has("ch1_app") && in(p, s, 9, 1, 6, 11, 4, 12)) {
                    s.flags.add("ch1_app");
                    HatmanEntity h = spawn(l, s, 10.5, 1, 6.4, 0F);
                    if (h != null) h.storyApparition();
                }
                if (s.has("ch1_app") && tickApparitions(p, s, l)) {
                    s.flags.add("storage_open");
                    setDoor(l, s, 12, 1, 8, true);
                    soundAt(l, s, 12.5, 2, 8.5, "story.door_slam", 0.5F);
                    enter(p, s, "ch1_storage");
                }
            }
            case "ch1_storage" -> {
                if (has(p, ModItems.STAIR_KEY) || s.has("stairs_open")) enter(p, s, "ch1_stairs");
            }
            case "ch1_stairs" -> {
                if (in(p, s, 13, 1, 13, 19, 4, 21)) enter(p, s, "ch2_start");
            }
            case "ch2_fuses" -> {
                int t = fuseTotal(p, s);
                if (t != s.objectiveArg) objective(p, s, "fuses", t);
                if (t >= 3) enter(p, s, "ch2_fusebox");
                if (!s.has("ch2_app") && s.chapterTicks > 20 * 60 * 4 && in(p, s, 13, 7, 13, 19, 10, 21)) {
                    s.flags.add("ch2_app");
                    HatmanEntity h = spawn(l, s, 15.5, 7, 11.6, 0F);
                    if (h != null) h.storyApparition();
                }
                tickApparitions(p, s, l);
            }
            case "ch2_watch" -> {
                if (in(p, s, 1, 1, 13, 7, 4, 17)) tape(p, s, "tape2");
            }
            case "ch2_climb" -> {
                if (hatmen(l, s).isEmpty() && in(p, s, 9, 7, 9, 11, 10, 20) && s.respawnTimer <= 0) {
                    HatmanEntity h = spawn(l, s, 10.5, 7, 8.6, 0F);
                    if (h != null) h.storyStalk(28F);
                }
                if (in(p, s, 13, 7, 1, 19, 10, 11)) enter(p, s, "ch3_start");
            }
            case "ch3_candles", "ch3_tape", "ch3_key" -> {
                if (!s.has("ch3_stalker") && s.chapterTicks > 20 * 60 * 3) {
                    s.flags.add("ch3_stalker");
                    soundAt(l, s, 6.5, 8, 4.5, "hatman.emerge", 1.5F);
                }
                if (s.has("ch3_stalker") && hatmen(l, s).isEmpty() && --s.respawnTimer <= 0) {
                    HatmanEntity h = spawn(l, s, 6.5, 7, 4.5, 180F);
                    if (h != null) h.storyStalk(45F);
                }
                if (s.step.equals("ch3_candles") && has(p, ModItems.TAPE_PARTY)) enter(p, s, "ch3_tape");
                if (s.step.equals("ch3_key") && has(p, ModItems.ATTIC_KEY)) enter(p, s, "ch3_escape");
            }
            case "ch3_escape" -> {
                if (hatmen(l, s).isEmpty() && --s.respawnTimer <= 0) {
                    HatmanEntity h = spawn(l, s, 15.5, 7, 18.5, 90F);
                    if (h != null) h.storyHunt(p);
                }
                if (p.getY() - s.origin.getY() >= 12.5) enter(p, s, "ch4_start");
            }
            case "ch4_dials" -> {
                if (s.stepTicks % 20 == 0) trySolveDials(p, s, l);
            }
            case "ch4_chase" -> {
                if (s.stepTicks == 40 || hatmen(l, s).isEmpty() && s.stepTicks > 40 && --s.respawnTimer <= 0) {
                    HatmanEntity h = spawn(l, s, 10.5, 13, 20.5, 180F);
                    if (h != null) h.storyHunt(p);
                }
                if (in(p, s, 1, 1, 13, 7, 4, 21)) enter(p, s, "ch5_start");
            }
            case "ch5_master", "ch5_final" -> {
                if (s.step.equals("ch5_master") && has(p, ModItems.MASTER_TAPE)) enter(p, s, "ch5_final");
                if (++s.hostTimer > 220) {
                    s.hostTimer = 0;
                    for (HatmanEntity h : hatmen(l, s)) {
                        h.getLookControl().setLookAt(p, 30F, 30F);
                        h.storyAction(l.random.nextBoolean() ? "hat_tip" : "look");
                        soundAt(l, s, 4.5, 2.5, 15, "story.brim_voice", 1F);
                    }
                }
            }
            default -> { }
        }
    }

    /** apparitions vanish once you have looked at them long enough or come too close; returns true when one vanished */
    private static boolean tickApparitions(ServerPlayer p, StoryState s, ServerLevel l) {
        boolean gone = false;
        for (HatmanEntity h : hatmen(l, s)) {
            if (h.storyMode != 0) continue;
            h.getLookControl().setLookAt(p, 30F, 30F);
            if (h.isObservedBy(p)) s.appSeen++;
            if (s.appSeen > 30 || h.distanceTo(p) < 4.5F || h.tickCount > 20 * 45) {
                h.storyVanish();
                s.flickerTimer = 30;
                gone = true;
            }
        }
        return gone;
    }

    // ------------------------------------------------------------------ ambience
    private static void ambience(ServerPlayer p, StoryState s, ServerLevel l) {
        if (s.step.startsWith("end") || s.step.equals("intro")) return;
        if (s.flickerTimer > 0) {
            s.flickerTimer--;
            if (s.flickerTimer % 3 == 0) {
                boolean on = s.flickerTimer == 0 || l.random.nextBoolean();
                double y = p.getY() - s.origin.getY();
                if (s.flickerTimer == 0) restoreLights(l, s);
                else if (y < 5.5) HauntedHouse.setLights(l, s.origin, 1, 5, on && !s.has("blackout"));
                else HauntedHouse.setLights(l, s.origin, 6, 17, on && s.has("power") && !s.has("blackout"));
            }
        }
        if (--s.trickTimer > 0) return;
        s.trickTimer = 1400 + l.random.nextInt(1400);
        Vec3 behind = p.position().subtract(p.getViewVector(1F).multiply(1, 0, 1).normalize().scale(4));
        switch (l.random.nextInt(5)) {
            case 0 -> l.playSound(null, behind.x, behind.y + 1, behind.z, ModSounds.KNOCK.get(), SoundSource.HOSTILE, 0.9F, 1F);
            case 1 -> l.playSound(null, behind.x, behind.y + 1, behind.z, ModSounds.FAKE_STEP.get(), SoundSource.HOSTILE, 1F, 1F);
            case 2 -> l.playSound(null, behind.x, behind.y + 1, behind.z, ModSounds.WHISPER.get(), SoundSource.HOSTILE, 0.8F, 1F);
            case 3 -> s.flickerTimer = 40;
            default -> sound(p, "story.house_amb", 0.8F);
        }
    }

    // ------------------------------------------------------------------ world changes
    /** chapter five: the living room becomes the set of Mr. Brim's Neighborly Hour */
    private static void studio(ServerLevel l, StoryState s) {
        for (int x = 1; x <= 7; x++)
            for (int z = 13; z <= 21; z++) {
                BlockPos p = s.at(x, 1, z);
                Block b = l.getBlockState(p).getBlock();
                if (b == ModBlocks.RUG_PERSIAN.get() || b == ModBlocks.RUG_PERSIAN_EDGE.get())
                    l.setBlock(p, ((x + z) % 2 == 0 ? Blocks.BLACK_CARPET : Blocks.WHITE_CARPET).defaultBlockState(), 3);
            }
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x <= 8; x++) if (x != 4 || y > 3) l.setBlock(s.at(x, y, 12), Blocks.RED_WOOL.defaultBlockState(), 3);
            for (int z = 13; z <= 21; z++) l.setBlock(s.at(0, y, z), Blocks.RED_WOOL.defaultBlockState(), 3);
        }
        l.setBlock(s.at(4, 4, 12), Blocks.OCHRE_FROGLIGHT.defaultBlockState(), 3);
        l.setBlock(s.at(2, 4, 16), Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12), 3);
        l.setBlock(s.at(6, 4, 16), Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 12), 3);
        l.setBlock(s.at(4, 2, 17), ModBlocks.STRAW_HAT.get().defaultBlockState(), 3);
        BlockPos fp = s.at(1, 1, 18);
        BlockState fs = l.getBlockState(fp);
        if (fs.getBlock() instanceof CampfireBlock) l.setBlock(fp, fs.setValue(CampfireBlock.LIT, true), 3);
    }

    // ------------------------------------------------------------------ scripted playthrough support (CI capture only)
    /** uses the block at a house-local position as the player would, optionally holding a story item and sneaking */
    public static void debugUse(ServerPlayer p, int lx, int ly, int lz, int heldItem, boolean sneak) {
        StoryState s = StoryState.get(p);
        if (!s.active) return;
        ServerLevel l = p.serverLevel();
        if (heldItem >= 0) {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i).getItem() == ModItems.STORY[heldItem].get()) {
                    ItemStack cur = inv.getItem(inv.selected);
                    inv.setItem(inv.selected, inv.getItem(i));
                    if (i != inv.selected) inv.setItem(i, cur);
                    break;
                }
            }
        }
        BlockPos pos = s.at(lx, ly, lz);
        BlockState st = l.getBlockState(pos);
        p.setShiftKeyDown(sneak);
        boolean used = !s.media && s.caughtTimer == 0 && interact(p, s, l, pos, st, st.getBlock(), p.getMainHandItem());
        p.setShiftKeyDown(false);
        if (!used && st.getBlock() instanceof DoorBlock) {
            BlockPos low = st.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER ? pos.below() : pos;
            boolean open = !l.getBlockState(low).getValue(DoorBlock.OPEN);
            setDoor(l, s, low.getX() - s.origin.getX(), low.getY() - s.origin.getY(), low.getZ() - s.origin.getZ(), open);
            l.playSound(null, low, open ? net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN : net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,
                    SoundSource.BLOCKS, 1F, 1F);
        }
    }

    public static void debugEnter(ServerPlayer p, String step) {
        StoryState s = StoryState.get(p);
        if (!s.active) return;
        s.media = false;
        s.caughtTimer = 0;
        enter(p, s, step);
    }

    public static String debugStep(ServerPlayer p) { return StoryState.get(p).step; }

    /** /hatman durum - where the story is, for players who feel stuck */
    public static void status(ServerPlayer p) {
        StoryState s = StoryState.get(p);
        if (!s.active) {
            p.sendSystemMessage(Component.translatable("msg.krolasyonhorror.start_hint"));
            return;
        }
        int[] d = readDials(p.serverLevel(), s);
        p.sendSystemMessage(Component.literal("[Hatman] step=" + s.step + " chapter=" + s.chapter + " notes=" + s.notes.size() + "/20 fuses="
                + s.fusesInserted + " dials=" + d[0] + d[1] + d[2] + d[3] + (s.media ? " (media)" : "")));
        if (!s.objective.isEmpty()) StoryBridge.send(p, Msg.of("objective", s.objective, s.objectiveArg));
    }

    // ------------------------------------------------------------------ events
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        StoryState s = StoryState.get(p);
        if (!s.active) return;
        ServerLevel l = p.serverLevel();
        Msg m;
        while ((m = StoryBridge.TO_SERVER.poll()) != null) {
            switch (m.type()) {
                case "cs_done" -> {
                    media(p, s, false);
                    afterMedia(p, s, "cs:" + m.s());
                }
                case "tape_done" -> {
                    media(p, s, false);
                    afterMedia(p, s, "tape:" + m.s());
                }
                case "credits_done" -> afterMedia(p, s, "credits");
                case "note" -> {
                    if (s.notes.add(m.i())) say(p, "notes_found", s.notes.size());
                }
                default -> { }
            }
        }
        if (!s.active) return;
        if (s.caughtTimer > 0) {
            if (--s.caughtTimer == 0) respawn(p, s);
            return;
        }
        s.stepTicks++;
        s.chapterTicks++;
        if (s.knockTimer > 0 && --s.knockTimer == 0) {
            if (s.knocks == 99) {
                soundAt(l, s, 8.5, 8, 19.5, "fx.knock", 1.2F);
                say(p, "knock_answer");
                s.flags.add("bedroom_open");
                setDoor(l, s, 8, 7, 19, true);
            }
            s.knocks = 0;
        }
        // watchdog: a cutscene or tape that never reported back (closed some unusual way) must not soft-lock the story
        if (s.media && ++s.mediaTicks > 20 * 150 && s.mediaWhat != null) {
            String what = s.mediaWhat;
            s.mediaWhat = null;
            media(p, s, false);
            afterMedia(p, s, what);
        }
        if (!s.media) {
            tickStep(p, s, l);
            ambience(p, s, l);
        }
        if (p.tickCount % 40 == 0) s.save(p);
    }

    private static void caught(ServerPlayer p, StoryState s) {
        s.caughtTimer = 46;
        StoryBridge.send(p, Msg.of("caught"));
        for (HatmanEntity h : hatmen(p.serverLevel(), s)) {
            h.setNoAi(true);
            h.getNavigation().stop();
        }
    }

    private static void respawn(ServerPlayer p, StoryState s) {
        ServerLevel l = p.serverLevel();
        clearHatmen(l, s);
        s.respawnTimer = 300;
        Vec3 v = w(s, s.cpX, s.cpY, s.cpZ);
        p.teleportTo(l, v.x, v.y, v.z, s.cpYaw, 0F);
        p.setHealth(p.getMaxHealth());
        restoreLights(l, s);
        StoryBridge.send(p, Msg.of("fade_in"));
        String step = s.step;
        enter(p, s, step);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        StoryState s = StoryState.get(p);
        if (!s.active) return;
        e.setCanceled(true);
        if (e.getSource().getEntity() instanceof HatmanEntity && s.caughtTimer == 0 && !s.media) caught(p, s);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !StoryState.isActive(p)) return;
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        StoryState s = StoryState.get(p);
        if (s.caughtTimer == 0) caught(p, s);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        StoryState.forget(p);
        StoryState s = StoryState.get(p);
        StoryBridge.TO_SERVER.clear();
        if (s.active) {
            StoryBridge.send(p, Msg.of("active", "", 1));
            if (s.chapter > 0) StoryBridge.send(p, Msg.of("chapter_quiet", "", s.chapter));
            clearHatmen(p.serverLevel(), s);
            s.media = false;
            enter(p, s, s.step);
        } else if (!s.finished && StoryBridge.available(p)) {
            p.sendSystemMessage(Component.translatable("msg.krolasyonhorror.start_hint"));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            StoryState.get(p).save(p);
            StoryState.forget(p);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide()) {
            BlockState cs = e.getLevel().getBlockState(e.getPos());
            if (cs.getBlock() == ModBlocks.NOTE_PAPER.get() && e.getHand() == InteractionHand.MAIN_HAND) {
                int n = cs.getValue(PropBlock.NOTE);
                StoryBridge.OPEN_NOTE.accept(n);
                StoryBridge.TO_SERVER.add(Msg.of("note", "", n));
                e.setCanceled(true);
            }
            return;
        }
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        StoryState s = StoryState.get(p);
        if (!s.active) return;
        BlockPos pos = e.getPos();
        BlockState st = e.getLevel().getBlockState(pos);
        Block b = st.getBlock();
        if (e.getHand() != InteractionHand.MAIN_HAND) {
            if (b instanceof DoorBlock || b instanceof TrapDoorBlock || b == Blocks.BARREL) e.setCanceled(true);
            return;
        }
        if (s.media || s.caughtTimer > 0) {
            e.setCanceled(true);
            return;
        }
        if (interact(p, s, p.serverLevel(), pos, st, b, p.getMainHandItem())) {
            e.setCanceled(true);
            resend(p, pos);
        }
    }

    /** @return true when the interaction was consumed by the story */
    private static boolean interact(ServerPlayer p, StoryState s, ServerLevel l, BlockPos pos, BlockState st, Block b, ItemStack held) {
        String step = s.step;
        int ly = pos.getY() - s.origin.getY();
        if (b == ModBlocks.NOTE_PAPER.get()) {
            sound(p, "story.page", 1F);
            return true;
        }
        if (b == ModBlocks.STORY_ITEM.get()) {
            int idx = st.getValue(PropBlock.ITEM);
            l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            give(p, idx);
            return true;
        }
        if (b == ModBlocks.CRT_TV.get() || b == ModBlocks.VCR.get()) {
            if (ly >= 5) {
                say(p, "only_static");
                return true;
            }
            if (step.equals("ch1_play") && has(p, ModItems.TAPE_BIRTHDAY)) tape(p, s, "tape1");
            else if (step.equals("ch3_tape") && has(p, ModItems.TAPE_PARTY)) tape(p, s, "tape3");
            else if (step.equals("ch5_master") && !has(p, ModItems.MASTER_TAPE)) {
                give(p, ModItems.MASTER_TAPE);
                say(p, "behind_tv");
            } else if (step.equals("ch2_watch")) tape(p, s, "tape2");
            else say(p, b == ModBlocks.VCR.get() ? "need_tape" : "only_static");
            return true;
        }
        if (b == ModBlocks.FUSE_BOX.get()) {
            if (has(p, ModItems.FUSE) && s.fusesInserted < 3) {
                take(p, ModItems.FUSE);
                s.fusesInserted++;
                l.setBlock(pos, st.setValue(CounterBlock.FUSES, s.fusesInserted), 3);
                sound(p, "story.fuse_spark", 1F);
                say(p, "fuse_in", s.fusesInserted);
                if (s.fusesInserted >= 3 && (step.equals("ch2_fusebox") || step.equals("ch2_fuses"))) enter(p, s, "ch2_power");
            } else if (s.fusesInserted < 3) say(p, "fusebox_empty");
            return true;
        }
        if (b == ModBlocks.DIAL.get()) {
            int idx = pos.getX() - s.origin.getX() - 8;
            if (idx < 0 || idx > 3) return true;
            int d = (st.getValue(CounterBlock.DIGIT) + 1) % 10;
            l.setBlock(pos, st.setValue(CounterBlock.DIGIT, d), 3);
            s.dials = readDials(l, s);
            sound(p, "story.dial_click", 1F);
            say(p, "dial", s.dials[0], s.dials[1], s.dials[2], s.dials[3]);
            trySolveDials(p, s, l);
            return true;
        }
        if (b instanceof CandleBlock && ly >= 6 && ly <= 10) {
            if (!step.equals("ch3_candles")) return true;
            if (!held.is(ModItems.STORY[ModItems.LIGHTER].get())) {
                say(p, "need_lighter");
                return true;
            }
            if (st.getValue(CandleBlock.LIT)) return true;
            char c = b == Blocks.BLACK_CANDLE ? 'k' : b == Blocks.RED_CANDLE ? 'r' : b == Blocks.BLUE_CANDLE ? 'b' : 'w';
            s.candles += c;
            if ("krbw".startsWith(s.candles)) {
                l.setBlock(pos, st.setValue(CandleBlock.LIT, true), 3);
                sound(p, "story.candle_light", 1F);
                say(p, "candle_lit");
                if (s.candles.length() == 4) {
                    sound(p, "story.music_box", 1F);
                    l.setBlock(s.at(15, 7, 5), HauntedHouse.item(ModItems.TAPE_PARTY, false), 3);
                }
            } else {
                s.candles = "";
                for (int[] cxz : new int[][]{{14, 4}, {18, 4}, {14, 6}, {18, 6}}) {
                    BlockPos cp = s.at(cxz[0], 7, cxz[1]);
                    BlockState cs = l.getBlockState(cp);
                    if (cs.getBlock() instanceof CandleBlock) l.setBlock(cp, cs.setValue(CandleBlock.LIT, false), 3);
                }
                say(p, "candle_wrong");
                l.playSound(null, pos, ModSounds.WHISPER.get(), SoundSource.HOSTILE, 1F, 1.3F);
            }
            return true;
        }
        if (b instanceof CampfireBlock && isAt(s, pos, 1, 1, 18)) {
            if (step.equals("ch5_final")) {
                if (has(p, ModItems.MASTER_TAPE) && has(p, ModItems.EVICTION_NOTICE)) {
                    take(p, ModItems.MASTER_TAPE);
                    take(p, ModItems.EVICTION_NOTICE);
                    enter(p, s, "end_evict");
                } else if (has(p, ModItems.MASTER_TAPE)) say(p, "need_notice");
                else say(p, "need_master");
            } else say(p, "need_master");
            return true;
        }
        if (b == ModBlocks.STRAW_HAT.get()) {
            if (step.equals("ch5_final")) enter(p, s, "end_host");
            else say(p, "hat_wait");
            return true;
        }
        if (b == ModBlocks.BALLOON_GOLD.get() || b == ModBlocks.BALLOON_WHITE.get()) {
            l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            sound(p, "story.balloon_pop", 1F);
            if (isAt(s, pos, 14, 8, 14) && !s.has("fuse1")) {
                s.flags.add("fuse1");
                l.setBlock(s.at(14, 7, 14), HauntedHouse.item(ModItems.FUSE, false), 3);
                say(p, "balloon_pop");
            }
            return true;
        }
        if (b == ModBlocks.PICTURE_PORTRAIT.get() && isAt(s, pos, 9, 8, 16)) {
            if (!s.has("fuse3") && (step.equals("ch2_fuses") || step.equals("ch2_fusebox"))) {
                s.flags.add("fuse3");
                give(p, ModItems.FUSE);
                say(p, "portrait");
            }
            return true;
        }
        if (b == Blocks.BARREL) {
            if (isAt(s, pos, 13, 1, 1) && s.has("storage_open") && !s.has("key_given")) {
                s.flags.add("key_given");
                give(p, ModItems.STAIR_KEY);
            } else if (isAt(s, pos, 6, 7, 20) && !s.has("fuse2") && (step.equals("ch2_fuses") || step.equals("ch2_fusebox"))) {
                s.flags.add("fuse2");
                give(p, ModItems.FUSE);
            } else if (isAt(s, pos, 12, 1, 24)) {
                if (step.equals("ch5_final") && has(p, ModItems.MASTER_TAPE) && s.notes.size() >= NOTES_FOR_SECRET) {
                    take(p, ModItems.MASTER_TAPE);
                    enter(p, s, "end_return");
                } else say(p, "mailbox_closed");
            } else say(p, "search_nothing");
            return true;
        }
        if (b instanceof DoorBlock) {
            if (isDoor(s, pos, 10, 1, 23)) {
                if (s.has("front_open")) return false;
                say(p, "cant_leave");
                l.playSound(null, pos, ModSounds.KNOCK.get(), SoundSource.HOSTILE, 0.6F, 0.7F);
                return true;
            }
            if (isDoor(s, pos, 12, 1, 8) && !s.has("storage_open")) {
                say(p, "locked");
                return true;
            }
            if (isDoor(s, pos, 12, 1, 16) && !s.has("stairs_open")) {
                if (has(p, ModItems.STAIR_KEY)) {
                    take(p, ModItems.STAIR_KEY);
                    s.flags.add("stairs_open");
                    setDoor(l, s, 12, 1, 16, true);
                    say(p, "unlocked");
                } else say(p, "locked_key");
                return true;
            }
            if (isDoor(s, pos, 8, 7, 19) && !s.has("bedroom_open")) {
                if (p.isShiftKeyDown() && s.knocks < 3) {
                    s.knocks++;
                    s.knockTimer = 50;
                    l.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 0.6F);
                    say(p, "knock");
                    if (s.knocks >= 3) {
                        s.knocks = 99;
                        s.knockTimer = 34;
                    }
                } else say(p, "knock_hint");
                return true;
            }
            return false;
        }
        if (b instanceof TrapDoorBlock && isAt(s, pos, 9, 12, 10)) {
            if (s.has("attic_open")) return false;
            if (has(p, ModItems.ATTIC_KEY)) {
                take(p, ModItems.ATTIC_KEY);
                s.flags.add("attic_open");
                l.setBlock(pos, st.setValue(TrapDoorBlock.OPEN, true), 3);
                say(p, "unlocked");
            } else say(p, "locked_attic");
            return true;
        }
        return false;
    }
}
