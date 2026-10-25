package com.krolasyon.labirent.story;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.item.CodeWordItem;
import com.krolasyon.labirent.maze.*;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.net.ServerHooks;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.registry.ModEntities;
import com.krolasyon.labirent.registry.ModItems;
import com.krolasyon.labirent.registry.ModSounds;
import com.krolasyon.labirent.team.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Drives the book's plot: chapters, objectives, scripted events and the escape. */
public final class StoryDirector {
    public static final int CHAPTERS = 8;          // 0..7 playable, 8 = epilogue
    public static final String[] WORDS = {"SÜZÜL", "YAKALA", "KANA", "ÖLÜM", "KASKATI", "İT", "DİREN", "KAÇ"};

    private final MazeManager m;
    private boolean doorsWereClosed;
    private int teresaTimer = -1;
    private long lastDayPhase = -1;
    private final Set<UUID> escaped = new HashSet<>();
    private int endingTimer = -1;

    public StoryDirector(MazeManager m) { this.m = m; }

    private MazeState st() { return m.state; }

    // ------------------------------------------------------------------ objectives
    public void progress(Objective o, int amount) {
        int k = o.ordinal();
        if (st().progress[k] >= o.target) return;
        st().progress[k] = Math.min(o.target, st().progress[k] + amount);
        st().setDirty();
        if (st().progress[k] >= o.target) announceObjective(o);
    }

    public void set(Objective o, int value) {
        int k = o.ordinal();
        if (value <= st().progress[k] || st().progress[k] >= o.target) return;
        st().progress[k] = Math.min(o.target, value);
        st().setDirty();
        if (st().progress[k] >= o.target) announceObjective(o);
    }

    private void announceObjective(Objective o) {
        if (o.chapter != st().chapter) return;
        ServerLevel level = m.level();
        if (level == null) return;
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(Component.literal("✔ ").withStyle(ChatFormatting.GREEN).append(o.text().withStyle(ChatFormatting.GRAY)));
            p.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    private boolean chapterDone(int ch) {
        for (Objective o : Objective.values()) if (o.chapter == ch && st().progress(o) < o.target) return false;
        return true;
    }

    private void advance(ServerLevel level) {
        st().chapter++;
        st().setDirty();
        int ch = st().chapter;
        for (ServerPlayer p : level.players()) {
            Net.title(p, Component.translatable("chapter.labirent.num", ch + 1).withStyle(ChatFormatting.GOLD),
                    Component.translatable("chapter.labirent." + ch), 10, 80, 20);
            p.sendSystemMessage(Component.translatable("msg.labirent.journal_updated").withStyle(ChatFormatting.YELLOW));
            p.playNotifySound(ModSounds.STORY_STING.get(), SoundSource.MASTER, 1F, 1F);
        }
        onEnterChapter(level, ch);
    }

    private void onEnterChapter(ServerLevel level, int ch) {
        switch (ch) {
            case 2 -> sayFrom(level, "minho", "talk.labirent.event.runners");
            case 3 -> {
                GladerEntity ben = find(level, "alby");
                if (ben != null) {
                    ben.addEffect(new MobEffectInstance(ModEffects.VENOM.get(), 20 * 60 * 8, 0));
                    ben.teleportTo(Glade.GATHER.getX() + 0.5, Glade.Y, Glade.GATHER.getZ() + 2.5);
                    ben.setOrder(GladerEntity.Order.STAY, ben.blockPosition());
                    broadcast(level, Component.translatable("msg.labirent.event.ben").withStyle(ChatFormatting.DARK_GREEN));
                } else progress(Objective.BEN_FATE, 1);
                st().set(MazeState.EV_BEN);
            }
            case 4 -> broadcast(level, Component.translatable("msg.labirent.event.walls").withStyle(ChatFormatting.GRAY));
            case 5 -> {
                m.box.queueTeresa();
                st().set(MazeState.EV_TERESA);
            }
            case 6 -> broadcast(level, Component.translatable("msg.labirent.event.code").withStyle(ChatFormatting.AQUA));
            case 7 -> broadcast(level, Component.translatable("msg.labirent.event.hole").withStyle(ChatFormatting.LIGHT_PURPLE));
            default -> {}
        }
    }

    // ------------------------------------------------------------------ tick
    private boolean cleaned;

    public void tick(ServerLevel level, int day, int phase, boolean night) {
        if (!st().has(MazeState.EV_NPCS) && m.gladeReady()) spawnFounders(level);
        if (!cleaned && level.getGameTime() % 100 == 0) {
            // only the five Gladers (and Teresa) live in the Glade now; older worlds lose the rest
            cleaned = true;
            for (GladerEntity g : ServerHooks.allGladers(level)) if (!g.hasCustomSkin()) g.discard();
        }
        long t = level.getGameTime();
        if (t % 20 == 0) {
            int pct = ExploreTracker.percent(st());
            set(Objective.MAP_10, pct);
            set(Objective.MAP_30, pct);
            if (!night) for (ServerPlayer p : level.players())
                if (!p.isSpectator() && !Glade.inside(p.getX(), p.getZ()) && MazeLayout.unitOf(p.getBlockX()) >= 0) progress(Objective.MAZE_TIME, 1);
            if (st().chapter < CHAPTERS && chapterDone(st().chapter)) advance(level);
        }
        boolean closed = m.doors.closed();
        if (closed && !doorsWereClosed) {
            for (ServerPlayer p : level.players()) if (Glade.inside(p.getX(), p.getZ())) { progress(Objective.WATCH_DOORS, 1); break; }
            if (!st().has(MazeState.EV_FIRST_DOORS)) {
                st().set(MazeState.EV_FIRST_DOORS);
                sayFrom(level, "newt", "talk.labirent.event.doors");
            }
            for (ServerPlayer p : level.players())
                if (!Glade.inside(p.getX(), p.getZ()) && MazeLayout.unitOf(p.getBlockX()) >= 0)
                    Net.title(p, Component.translatable("title.labirent.trapped").withStyle(ChatFormatting.DARK_RED),
                            Component.translatable("title.labirent.trapped.sub"), 10, 70, 20);
        }
        doorsWereClosed = closed;

        // the sky goes grey after Teresa arrives
        if (teresaTimer > 0 && --teresaTimer == 0 && !st().has(MazeState.EV_SUN_GONE)) {
            st().set(MazeState.EV_SUN_GONE);
            st().sunGoneNight = (int) ((level.getDayTime() + 10000L) / 24000L);
            st().setDirty();
            for (ServerPlayer p : level.players()) {
                Net.title(p, Component.translatable("title.labirent.sun_gone").withStyle(ChatFormatting.DARK_GRAY),
                        Component.translatable("title.labirent.sun_gone.sub"), 20, 100, 30);
                Net.cinematic(p, 2);
            }
        }
        // surviving the nights without walls
        if (st().has(MazeState.EV_SUN_GONE) && lastDayPhase >= 0 && lastDayPhase > 22000 && phase < 2000) {
            int nightNo = (int) ((level.getDayTime() + 10000L) / 24000L);
            if (nightNo > st().lastSurvivedNight && !level.players().isEmpty()) {
                st().lastSurvivedNight = nightNo;
                progress(Objective.SURVIVE_NIGHTS, 1);
                broadcast(level, Component.translatable("msg.labirent.survived", st().progress(Objective.SURVIVE_NIGHTS)).withStyle(ChatFormatting.GOLD));
            }
        }
        lastDayPhase = phase;

        // the escape room
        if (t % 10 == 0) for (ServerPlayer p : level.players())
            if (p.getX() > Landmarks.ESCAPE_X + 1 && p.getX() < Landmarks.ESCAPE_X + 15 && Math.abs(p.getZ() - Landmarks.CZ) < 9 && !escaped.contains(p.getUUID()))
                escape(level, p);
        if (endingTimer > 0) endingTick(level);
    }

    private void spawnFounders(ServerLevel level) {
        st().set(MazeState.EV_NPCS);
        Object[][] founders = {
                {"Newt", 0, Role.BAHCIVAN, "newt"}, {"Minho", 0, Role.KOSUCU, "minho"}, {"Alby", 0, Role.SIFACI, "alby"},
                {"Chuck", 0, Role.ASCI, "chuck"}, {"Gally", 0, Role.BEKCI, "gally"}};
        int k = 0;
        for (Object[] f : founders) {
            GladerEntity g = ModEntities.GLADER.get().create(level);
            if (g == null) continue;
            g.setup((String) f[0], (Integer) f[1], (Role) f[2]);
            g.setStoryId((String) f[3]);
            BlockPos p = Glade.GATHER.offset(k * 2 - 4, 0, 0);
            g.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180F, 0F);
            level.addFreshEntity(g);
            k++;
        }
        // the Blood House herd
        EntityType<?>[] herd = {EntityType.COW, EntityType.COW, EntityType.COW, EntityType.PIG, EntityType.PIG, EntityType.PIG,
                EntityType.SHEEP, EntityType.SHEEP, EntityType.CHICKEN, EntityType.CHICKEN, EntityType.CHICKEN, EntityType.CHICKEN};
        for (int i = 0; i < herd.length; i++) {
            Entity a = herd[i].create(level);
            if (a instanceof Animal an) {
                an.moveTo(-25.5 + (i % 6) * 1.6, Glade.Y, 20.5 + (i / 6) * 3, level.random.nextFloat() * 360F, 0F);
                an.setPersistenceRequired();
                level.addFreshEntity(an);
            }
        }
    }

    // ------------------------------------------------------------------ hooks
    public void onPlayerArrived(ServerPlayer p) {
        ServerLevel level = m.level();
        if (level == null) return;
        GladerEntity newt = find(level, "newt");
        if (newt != null) newt.say(p, Component.translatable(m.isCaptain(p) ? "talk.labirent.event.welcome_captain" : "talk.labirent.event.welcome"));
        p.sendSystemMessage(Component.translatable("msg.labirent.howto").withStyle(ChatFormatting.GRAY));
        Net.title(p, Component.translatable("chapter.labirent.num", st().chapter + 1).withStyle(ChatFormatting.GOLD),
                Component.translatable("chapter.labirent." + st().chapter), 20, 60, 20);
    }

    public void onRoleAssigned(Role r) {
        if (r == Role.KOSUCU) progress(Objective.ASSIGN_RUNNER, 1);
        if (r == Role.ASCI) progress(Objective.ASSIGN_COOK, 1);
    }

    public void onTalk(ServerPlayer p, GladerEntity g) {
        String id = g.storyId();
        if ("newt".equals(id)) progress(Objective.TALK_NEWT, 1);
        if ("teresa".equals(id) && !g.isNewcomer() && st().chapter >= 5) progress(Objective.TALK_TERESA, 1);
    }

    public void onTeresaOut(GladerEntity teresa) {
        ServerLevel level = m.level();
        if (level == null) return;
        teresa.addEffect(new MobEffectInstance(ModEffects.CHANGING.get(), 20 * 60 * 2, 0));
        for (ServerPlayer p : level.players()) {
            Net.title(p, Component.translatable("title.labirent.last_one").withStyle(ChatFormatting.DARK_RED),
                    Component.translatable("title.labirent.last_one.sub"), 10, 100, 30);
            p.sendSystemMessage(Component.translatable("msg.labirent.teresa_note").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        teresaTimer = 20 * 45;
    }

    public void onGladerDied(GladerEntity g, DamageSource src) {
        ServerLevel level = m.level();
        if (level == null) return;
        if ("alby".equals(g.storyId()) && st().chapter >= 3) progress(Objective.BEN_FATE, 1);
        broadcast(level, Component.translatable("msg.labirent.glader_died", g.getGladerName()).withStyle(ChatFormatting.DARK_RED));
    }

    public void onGrieverKilled(GrieverEntity g, DamageSource src) {
        progress(Objective.KILL_GRIEVER, 1);
        if (src.getEntity() instanceof net.minecraft.world.entity.player.Player p) com.krolasyon.labirent.TripEvents.rememberKill(p);
    }

    public void onCured(LivingEntity target, Entity healer) {
        if (target instanceof GladerEntity g && "alby".equals(g.storyId()) && st().chapter >= 3) {
            progress(Objective.BEN_FATE, 1);
            g.setOrder(GladerEntity.Order.WORK, null);
        }
        if (healer instanceof ServerPlayer p && target != p) p.displayClientMessage(Component.translatable("msg.labirent.cured_other",
                target instanceof GladerEntity g ? g.getGladerName() : target.getName().getString()), true);
    }

    /** the Changing ended: a memory comes back */
    public void onRemember(ServerPlayer p) {
        st().memoriesUnlocked = Math.min(4, st().memoriesUnlocked + 1);
        st().setDirty();
        int n = st().memoriesUnlocked;
        Net.title(p, Component.translatable("title.labirent.remember").withStyle(ChatFormatting.AQUA),
                Component.translatable("memory.labirent." + n + ".short"), 10, 80, 20);
        p.sendSystemMessage(Component.translatable("memory.labirent." + n).withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
        if (st().chapter >= 6) progress(Objective.REMEMBER, 1);
    }

    public void onCodeStone(ServerPlayer p, int sector) {
        boolean first = (st().stonesFound & (1 << sector)) == 0;
        st().stonesFound |= 1 << sector;
        st().setDirty();
        if (first && st().chapter >= 4) progress(Objective.FIND_STONES, 1);
        else if (first) set(Objective.FIND_STONES, Integer.bitCount(st().stonesFound));
        if (st().chapter < 6) {
            p.displayClientMessage(Component.translatable("msg.labirent.stone_strange", sector + 1), false);
            return;
        }
        int pct = ExploreTracker.sectorPercent(st(), sector);
        if (pct < 35) {
            p.displayClientMessage(Component.translatable("msg.labirent.stone_unmapped", sector + 1, pct), false);
            return;
        }
        if ((st().wordsFound & (1 << sector)) == 0) {
            st().wordsFound |= 1 << sector;
            st().setDirty();
            set(Objective.CODE_WORDS, Integer.bitCount(st().wordsFound));
            p.getInventory().add(CodeWordItem.of(sector));
            Net.title(p, Component.literal(WORDS[sector]).withStyle(ChatFormatting.AQUA), Component.translatable("title.labirent.word", sector + 1), 5, 60, 15);
            p.level().playSound(null, p.blockPosition(), ModSounds.TERMINAL_BEEP.get(), SoundSource.PLAYERS, 1F, 0.8F);
        } else p.displayClientMessage(Component.translatable("msg.labirent.stone_known", sector + 1, WORDS[sector]), true);
    }

    public void onMapTable(ServerPlayer p) {
        int pct = ExploreTracker.percent(st());
        p.sendSystemMessage(Component.translatable("msg.labirent.maproom", pct, Integer.bitCount(st().stonesFound), Integer.bitCount(st().wordsFound))
                .withStyle(ChatFormatting.YELLOW));
    }

    public void enterHole(ServerPlayer p) {
        ServerLevel level = m.level();
        if (level == null) return;
        BlockPos a = Landmarks.CHAMBER_ARRIVAL;
        p.teleportTo(level, a.getX() + 0.5, a.getY(), a.getZ() + 0.5, -90F, 0F);
        p.fallDistance = 0;
        progress(Objective.ENTER_HOLE, 1);
        Net.title(p, Component.translatable("title.labirent.hole").withStyle(ChatFormatting.DARK_PURPLE), Component.translatable("title.labirent.hole.sub"), 5, 60, 20);
        Net.shake(p, 0.6F, 20);
        if (!st().has(MazeState.EV_CHAMBER_WAVE)) {
            st().set(MazeState.EV_CHAMBER_WAVE);
            for (int k = 0; k < 3; k++) spawnChamberGriever(level, k);
        }
    }

    private void spawnChamberGriever(ServerLevel level, int k) {
        GrieverEntity g = ModEntities.GRIEVER.get().create(level);
        if (g == null) return;
        g.moveTo(Landmarks.CX + 9 + k * 2, Landmarks.CY + 1, Landmarks.CZ - 5 + k * 5, 90F, 0F);
        g.finalizeSpawn(level, level.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(g);
    }

    public void enterCode(ServerPlayer p, int[] order) {
        ServerLevel level = m.level();
        if (level == null || p.distanceToSqr(Landmarks.TERMINAL.getCenter()) > 64) return;
        if (st().progress(Objective.ENTER_CODE) > 0) {
            Landmarks.openExit(level);
            return;
        }
        boolean ok = Integer.bitCount(st().wordsFound) == 8 && order.length == 8;
        for (int k = 0; ok && k < 8; k++) ok = order[k] == k;
        if (!ok) {
            level.playSound(null, Landmarks.TERMINAL, ModSounds.BOX_ALARM.get(), SoundSource.BLOCKS, 2F, 1.4F);
            p.displayClientMessage(Component.translatable(Integer.bitCount(st().wordsFound) < 8 ? "msg.labirent.code_missing" : "msg.labirent.code_wrong")
                    .withStyle(ChatFormatting.RED), false);
            spawnChamberGriever(level, level.random.nextInt(3));
            return;
        }
        progress(Objective.ENTER_CODE, 1);
        Landmarks.openExit(level);
        level.playSound(null, Landmarks.TERMINAL, ModSounds.TERMINAL_BEEP.get(), SoundSource.BLOCKS, 2F, 1.0F);
        AABB box = new AABB(Landmarks.CX - 5, Landmarks.CY - 5, Landmarks.CZ - 20, Landmarks.ESCAPE_X + 20, Landmarks.CY + 20, Landmarks.CZ + 20);
        for (GrieverEntity g : level.getEntitiesOfClass(GrieverEntity.class, box)) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, g.getX(), g.getY() + 0.8, g.getZ(), 30, 0.8, 0.6, 0.8, 0.02);
            g.kill();
        }
        // every Griever in the maze shuts down
        for (GrieverEntity g : level.getEntitiesOfClass(GrieverEntity.class, new AABB(-200, 0, -200, 200, 256, 200))) g.kill();
        for (ServerPlayer pl : level.players())
            Net.title(pl, Component.translatable("title.labirent.code_ok").withStyle(ChatFormatting.GREEN), Component.translatable("title.labirent.code_ok.sub"), 5, 80, 20);
    }

    // ------------------------------------------------------------------ ending
    private void escape(ServerLevel level, ServerPlayer p) {
        escaped.add(p.getUUID());
        if (!st().has(MazeState.EV_ESCAPED)) {
            st().set(MazeState.EV_ESCAPED);
            if (st().chapter < CHAPTERS) {
                st().chapter = CHAPTERS;
                st().setDirty();
            }
            endingTimer = 1;
        }
        Net.cinematic(p, 1);
        level.playSound(null, p.blockPosition(), ModSounds.ESCAPE.get(), SoundSource.MASTER, 1F, 1F);
        p.getInventory().add(new ItemStack(Items.TOTEM_OF_UNDYING));
    }

    private void endingTick(ServerLevel level) {
        endingTimer++;
        int t = endingTimer;
        if (t % 40 == 0 && t < 400) {
            for (int k = 0; k < 3; k++) {
                ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
                rocket.getOrCreateTagElement("Fireworks").putByte("Flight", (byte) 1);
                FireworkRocketEntity f = new FireworkRocketEntity(level, Landmarks.ESCAPE_X + 12.5, Landmarks.CY + 8, Landmarks.CZ + (k - 1) * 4 + 0.5, rocket);
                level.addFreshEntity(f);
            }
        }
        String[] lines = {"ending.labirent.1", "ending.labirent.2", "ending.labirent.3", "ending.labirent.4", "ending.labirent.5"};
        int idx = t / 120;
        if (t % 120 == 1 && idx < lines.length)
            for (ServerPlayer p : level.players()) p.sendSystemMessage(Component.translatable(lines[idx]).withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE));
        if (t == 700) {
            for (ServerPlayer p : level.players()) {
                Net.title(p, Component.translatable("title.labirent.escaped").withStyle(ChatFormatting.GOLD), Component.translatable("title.labirent.escaped.sub"), 20, 120, 40);
                p.sendSystemMessage(Component.translatable("msg.labirent.after_ending").withStyle(ChatFormatting.GRAY));
            }
            endingTimer = -1;
        }
    }

    // ------------------------------------------------------------------ helpers
    private GladerEntity find(ServerLevel level, String story) {
        for (GladerEntity g : ServerHooks.allGladers(level)) if (story.equals(g.storyId()) && g.isAlive()) return g;
        return null;
    }

    private void sayFrom(ServerLevel level, String story, String key) {
        GladerEntity g = find(level, story);
        for (ServerPlayer p : level.players()) {
            if (g != null) g.say(p, Component.translatable(key));
            else p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    private void broadcast(ServerLevel level, Component c) {
        for (ServerPlayer p : level.players()) p.sendSystemMessage(c);
    }

    /** admin helpers */
    public void forceChapter(int ch) {
        ServerLevel level = m.level();
        if (level == null) return;
        int target = Math.max(0, Math.min(CHAPTERS, ch));
        while (st().chapter < target) {
            for (Objective o : Objective.values()) if (o.chapter == st().chapter) st().progress[o.ordinal()] = o.target;
            advance(level);
        }
        if (target < st().chapter) {
            st().chapter = target;
            st().setDirty();
        }
    }
}
