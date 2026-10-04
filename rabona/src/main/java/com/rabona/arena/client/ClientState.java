package com.rabona.arena.client;

import com.rabona.arena.game.Move;
import com.rabona.arena.game.Pitch;
import com.rabona.arena.game.Team;
import com.rabona.arena.net.S2C;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/** Istemci tarafi durum: mac bilgisi, istatistikler, banner, akis ve kullanici ayarlari. */
public final class ClientState {
    private ClientState() {}

    public static S2C.MatchState match;
    public static Pitch pitch;
    public static S2C.Stats stats;
    public static long statsTime;

    // banner
    public static S2C.Banner banner;
    public static int bannerTicks;

    public record FeedLine(Component text, int born) {}

    public static final List<FeedLine> feed = new ArrayList<>();
    public static int clientTicks;

    // kullanici ayarlari
    public static final Move[] slots = {Move.RAINBOW, Move.ELASTICO, Move.ROULETTE, Move.STEPOVER};
    public static Move shotStyle = Move.SHOT_POWER;
    public static Move ability = Move.AB_FIRE;
    public static Move celebration = Move.CELEB_SIU;
    public static final Move[] SHOT_STYLES = {Move.SHOT_POWER, Move.SHOT_FINESSE, Move.SHOT_CHIP, Move.SHOT_TRIVELA,
            Move.SHOT_KNUCKLE, Move.SHOT_RABONA, Move.SHOT_PANENKA, Move.SHOT_TOEPOKE};

    public static void setMatch(S2C.MatchState m) {
        match = m;
        pitch = m.hasPitch() ? new Pitch(BlockPos.of(m.origin()), m.alongX()) : null;
    }

    public static Team teamOf(Entity e) {
        if (e instanceof com.rabona.arena.entity.FootballerEntity f) return f.getSquad();
        if (match == null) return Team.NONE;
        return match.teamOf(e.getUUID());
    }

    public static int numberOf(UUID u) { return match == null ? 0 : match.numberOf(u); }

    public static void addFeed(Component c) {
        feed.add(new FeedLine(c, clientTicks));
        while (feed.size() > 6) feed.remove(0);
    }

    public static int cooldown(Move m) {
        if (stats == null || stats.cooldowns().length <= m.ordinal()) return 0;
        long elapsed = (System.currentTimeMillis() - statsTime) / 50;
        return (int) Math.max(0, stats.cooldowns()[m.ordinal()] - elapsed);
    }

    // ---------------------------------------------------------------- ayar dosyasi
    private static Path file() { return FMLPaths.CONFIGDIR.get().resolve("rabonaarena-client.properties"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(f)) { p.load(r); }
            for (int i = 0; i < slots.length; i++) slots[i] = parse(p.getProperty("slot" + i), slots[i]);
            shotStyle = parse(p.getProperty("shot"), shotStyle);
            ability = parse(p.getProperty("ability"), ability);
            celebration = parse(p.getProperty("celebration"), celebration);
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            for (int i = 0; i < slots.length; i++) p.setProperty("slot" + i, slots[i].name());
            p.setProperty("shot", shotStyle.name());
            p.setProperty("ability", ability.name());
            p.setProperty("celebration", celebration.name());
            try (Writer w = Files.newBufferedWriter(file())) { p.store(w, "Rabona Arena istemci ayarlari"); }
        } catch (Exception ignored) {
        }
    }

    private static Move parse(String s, Move def) {
        if (s == null) return def;
        try {
            return Move.valueOf(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean inMatchTeam() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && teamOf(mc.player).playing();
    }
}
