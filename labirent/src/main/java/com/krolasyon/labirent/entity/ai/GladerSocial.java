package com.krolasyon.labirent.entity.ai;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.item.SerumItem;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.ServerHooks;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.registry.ModItems;
import com.krolasyon.labirent.team.Role;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The social side of a Glader: walking over to have a word, reacting to what the player is going through,
 * helping in a fight, and chatting with the other Gladers. Lines live in the language file; DialogueTable
 * (generated with it) says how many there are.
 */
public final class GladerSocial {
    /** what the Glade remembers about each player */
    public static final class Memory {
        public int mazeTicks;
        public long returnedAt = -1, killAt = -1, lastApproached = -100000, lastWarned = -100000;
        public int returnedMapPercent;
    }

    public static final Map<UUID, Memory> MEMORY = new HashMap<>();

    public static Memory mem(UUID id) { return MEMORY.computeIfAbsent(id, k -> new Memory()); }

    private final GladerEntity g;
    @Nullable private UUID partner;
    @Nullable private GladerEntity banterWith;
    private final List<String> lines = new ArrayList<>();
    private final List<String> speakers = new ArrayList<>();
    private int lineIndex;
    private long nextLine;
    private long cooldownUntil;
    private String action = "";

    public GladerSocial(GladerEntity g) { this.g = g; }

    public boolean talking() { return lineIndex < lines.size(); }

    private String pid() { return g.personality().id(); }

    // ------------------------------------------------------------------ conversation driver
    /** returns true while the Glader is busy talking (the brain should not do anything else) */
    public boolean tick(ServerLevel level, long now) {
        if (!talking()) return false;
        ServerPlayer p = partner == null ? null : (ServerPlayer) level.getPlayerByUUID(partner);
        if (banterWith != null) {
            if (!banterWith.isAlive() || banterWith.distanceToSqr(g) > 12 * 12) { stop(); return false; }
            g.getLookControl().setLookAt(banterWith);
            g.getNavigation().stop();
        } else {
            if (p == null || !p.isAlive() || p.distanceToSqr(g) > 24 * 24) { stop(); return false; }
            double d = g.distanceToSqr(p);
            if (d > 3.2 * 3.2) {
                if (g.getNavigation().isDone() || now % 10 == 0) g.getNavigation().moveTo(p, 1.1);
                return true;
            }
            g.getNavigation().stop();
            g.getLookControl().setLookAt(p);
        }
        if (now < nextLine) return true;
        String key = lines.get(lineIndex);
        String who = speakers.get(lineIndex);
        GladerEntity speaker = who.equals(pid()) || banterWith == null ? g : banterWith;
        Component c = Component.translatable(key);
        if (banterWith != null) speaker.shout(c, 16);
        else if (p != null) {
            speaker.say(p, c);
            if (lineIndex == 0 && !action.isEmpty()) perform(level, p);
        }
        lineIndex++;
        nextLine = now + 45 + key.length() % 25;
        if (!talking()) {
            cooldownUntil = now + (long) (2400 / Math.max(0.25F, g.personality().social));
            banterWith = null;
        }
        return true;
    }

    private void stop() {
        lines.clear();
        speakers.clear();
        lineIndex = 0;
        banterWith = null;
        action = "";
    }

    private void start(@Nullable ServerPlayer p, String topic, int script, String act, long now) {
        stop();
        int n = DialogueTable.lines(pid(), topic, script);
        for (int k = 0; k < n; k++) {
            lines.add("chat.labirent." + pid() + "." + topic + "." + script + "." + k);
            speakers.add(pid());
        }
        partner = p == null ? null : p.getUUID();
        action = act;
        nextLine = now + 10;
        if (p != null) mem(p.getUUID()).lastApproached = now;
    }

    // ------------------------------------------------------------------ deciding to talk
    /** called every ~2 s while the Glader is free; may start a conversation */
    public void consider(ServerLevel level, long now, boolean night, boolean dusk) {
        if (talking() || now < cooldownUntil) return;
        Personality per = g.personality();
        if (per == Personality.GENERIC) return;
        MazeManager m = MazeManager.get();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(g) > 20 * 20) continue;
            Memory mem = mem(p.getUUID());
            boolean inGlade = Glade.inside(p.getX(), p.getZ());
            // urgent things first
            if (p.hasEffect(ModEffects.VENOM.get())) {
                if (g.getRole() == Role.SIFACI && hasSerum(level)) { start(p, "stung", pick("stung"), "serum", now); return; }
                if (now - mem.lastWarned > 600) { mem.lastWarned = now; g.shout(Component.translatable(key("stung", pick("stung"), 0)), 24); return; }
                continue;
            }
            if (dusk && !inGlade && MazeLayout.unitOf(p.getBlockX()) >= 0 && now - mem.lastWarned > 900) {
                mem.lastWarned = now;
                g.shout(Component.translatable(key("dusk", pick("dusk"), 0)), 40);
                cooldownUntil = now + 300;
                return;
            }
            if (!inGlade || now - mem.lastApproached < 900) continue;
            if (p.getHealth() < p.getMaxHealth() * 0.45F && (g.getRole() == Role.SIFACI || per == Personality.NEWT)) {
                start(p, "lowhp", pick("lowhp"), g.getRole() == Role.SIFACI ? "bandage" : "", now);
                return;
            }
            if (p.getFoodData().getFoodLevel() < 9 && (g.getRole() == Role.ASCI || per == Personality.CHUCK)) {
                start(p, "hungry", pick("hungry"), "food", now);
                return;
            }
            if (mem.returnedAt > 0 && now - mem.returnedAt < 1200 && level.random.nextFloat() < per.social) {
                mem.returnedAt = -1;
                start(p, "back", pick("back"), "", now);
                return;
            }
            if (mem.killAt > 0 && now - mem.killAt < 2400 && level.random.nextFloat() < 0.5F + per.social * 0.5F) {
                mem.killAt = -1;
                start(p, "kill", pick("kill"), "", now);
                return;
            }
            if (night && level.random.nextFloat() < 0.15F) {
                start(p, "night", pick("night"), "", now);
                return;
            }
            boolean captain = m != null && m.isCaptain(p);
            float want = per.social * 0.06F;
            if (captain && per.loyalty < 0) want *= 0.5F;
            if (level.random.nextFloat() < want) {
                String topic = captain && per.loyalty < 0 && level.random.nextBoolean() ? "grumble" : "idle";
                start(p, topic, pick(topic), "", now);
                return;
            }
        }
        // nobody to talk to: maybe a word with another Glader
        if (level.random.nextFloat() < 0.04F * per.social) tryBanter(level, now);
    }

    private void tryBanter(ServerLevel level, long now) {
        if (level.getNearestPlayer(g, 18) == null) return;
        for (GladerEntity o : ServerHooks.allGladers(level)) {
            if (o == g || o.distanceToSqr(g) > 7 * 7 || o.isSleeping() || o.social().talking()) continue;
            int script = DialogueTable.banterWith(pid(), o.personality().id(), level.random.nextInt(1000));
            if (script < 0) continue;
            stop();
            String[] sp = DialogueTable.banterSpeakers(script);
            for (int k = 0; k < sp.length; k++) {
                lines.add("chat.labirent.banter." + script + "." + k);
                speakers.add(sp[k]);
            }
            banterWith = o;
            partner = null;
            nextLine = now + 10;
            o.social().cooldownUntil = now + 1200;
            return;
        }
    }

    private int pick(String topic) { return g.getRandom().nextInt(Math.max(1, DialogueTable.scripts(pid(), topic))); }

    private String key(String topic, int script, int line) { return "chat.labirent." + pid() + "." + topic + "." + script + "." + line; }

    // ------------------------------------------------------------------ helping hands
    private void perform(ServerLevel level, ServerPlayer p) {
        switch (action) {
            case "bandage" -> {
                p.heal(8F);
                level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 5, 0.3, 0.2, 0.3, 0);
            }
            case "food" -> {
                ItemStack meal = new ItemStack(level.random.nextBoolean() ? ModItems.GLADE_STEW.get() : ModItems.GLADE_BREAD.get());
                if (!p.getInventory().add(meal)) p.drop(meal, false);
            }
            case "serum" -> {
                if (takeSerum(level)) SerumItem.cure(p, g);
            }
            default -> {}
        }
        action = "";
    }

    private static boolean hasSerum(ServerLevel level) {
        Container c = Jobs.container(level, Glade.MED_CHEST);
        return c != null && Jobs.count(c, ModItems.GRIEF_SERUM.get()) > 0;
    }

    private boolean takeSerum(ServerLevel level) {
        if (g.inventory.hasAnyMatching(s -> s.is(ModItems.GRIEF_SERUM.get()))) {
            for (int k = 0; k < g.inventory.getContainerSize(); k++)
                if (g.inventory.getItem(k).is(ModItems.GRIEF_SERUM.get())) { g.inventory.getItem(k).shrink(1); return true; }
        }
        Container c = Jobs.container(level, Glade.MED_CHEST);
        if (c != null && Jobs.take(c, ModItems.GRIEF_SERUM.get(), 1)) { c.setChanged(); return true; }
        return false;
    }

    /** a player (or another Glader) is being hurt by a monster nearby */
    public void allyAttacked(LivingEntity victim, LivingEntity attacker, long now) {
        if (g.isSleeping() || !attacker.isAlive()) return;
        Personality per = g.personality();
        if (per.brave() || g.getRole() == Role.BEKCI) {
            if (g.getTarget() != attacker) {
                g.setTarget(attacker);
                if (now - helpShout > 200) {
                    helpShout = now;
                    g.shout(Component.translatable(key("help", pick("help"), 0)), 32);
                }
            }
        } else if (now - helpShout > 200) {
            helpShout = now;
            g.shout(Component.translatable(key("flee", pick("flee"), 0)), 32);
        }
    }

    private long helpShout = -1000;

    /** how the Glader answers the captain */
    public Component ack() {
        String topic = g.personality().loyalty < 0 ? "grumble" : "ack";
        return Component.translatable(key(topic, pick(topic), 0));
    }

    public static boolean nearGriever(GladerEntity g, double r) {
        return !g.level().getEntitiesOfClass(GrieverEntity.class, g.getBoundingBox().inflate(r)).isEmpty();
    }
}
