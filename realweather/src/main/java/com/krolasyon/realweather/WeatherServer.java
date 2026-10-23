package com.krolasyon.realweather;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sunucu tarafı: hava cephelerinin gelip gidişi, vanilya yağmur/gök gürültüsüyle eşleme, istemcilere bildirim ve ortamın
 * oyuna etkisi (tipide donma, kum fırtınasında yavaşlama, karın birikip erimesi, yağmurda ekinlerin hızlı büyümesi).
 */
public final class WeatherServer {
    private WeatherServer() {}

    private static final String ID = "realweather";
    private static final Map<UUID, Integer> WARM = new HashMap<>();

    public static WeatherData data(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(WeatherData::load, WeatherData::new, ID);
    }

    private static int duration(WeatherState s, RandomSource r) {
        return 20 * 60 * (s.minMinutes + r.nextInt(s.maxMinutes - s.minMinutes + 1));
    }

    /** Durumu değiştirir; seconds <= 0 ise süre rastgele, now ise yoğunluk ve rüzgâr hemen hedefe atlar. */
    public static void set(ServerLevel level, WeatherState s, int seconds, boolean now) {
        WeatherData d = data(level);
        RandomSource r = level.random;
        d.state = s;
        d.ticksLeft = seconds > 0 ? seconds * 20 : duration(s, r);
        d.next = s.next(r);
        d.targetIntensity = s.intensity * (0.85F + 0.15F * r.nextFloat());
        d.windTarget = Mth.clamp(s.wind * (0.8F + 0.4F * r.nextFloat()), 0, 1);
        if (now) {
            d.intensity = d.targetIntensity;
            d.wind = d.windTarget;
        }
        apply(level, d);
        d.setDirty();
        sync(null, d);
    }

    private static void apply(ServerLevel level, WeatherData d) {
        boolean rain = d.state.precipitation, thunder = d.state.thunder;
        int t = d.ticksLeft + 6000;
        level.setWeatherParameters(rain ? 0 : t, rain ? t : 0, rain, thunder);
        d.appliedRain = rain;
        d.appliedThunder = thunder;
    }

    public static Net.Sync packet(WeatherData d) {
        return new Net.Sync(d.state.ordinal(), d.next.ordinal(), d.ticksLeft, d.intensity, d.windAngle, d.wind);
    }

    private static void sync(ServerPlayer only, WeatherData d) {
        if (only != null) Net.send(only, packet(d));
        else Net.sendAll(packet(d));
    }

    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        WeatherData d = data(level);
        RandomSource r = level.random;
        // vanilya komutu (/weather) ya da uyuyup sabahlamak hava durumunu değiştirdiyse ona uy
        boolean vr = level.getLevelData().isRaining(), vt = level.getLevelData().isThundering();
        if (vr != d.appliedRain || vt != d.appliedThunder) {
            WeatherState s = vt ? WeatherState.STORM : vr ? (d.state.precipitation ? d.state : WeatherState.RAIN) : WeatherState.CLEAR;
            set(level, s, 0, false);
        }
        if (level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && --d.ticksLeft <= 0) set(level, d.next, 0, false);
        // cephe yavaşça gelir; yoğunluk ara sıra dalgalanır (sağanak, durulma)
        if (r.nextInt(600) == 0) d.targetIntensity = d.state.intensity * (0.7F + 0.3F * r.nextFloat());
        if (r.nextInt(500) == 0) d.windTarget = Mth.clamp(d.state.wind * (0.7F + 0.6F * r.nextFloat()), 0, 1);
        d.intensity += (d.targetIntensity - d.intensity) * 0.0025F;
        d.wind += (d.windTarget - d.wind) * 0.002F;
        d.windAngle = Mth.wrapDegrees(d.windAngle + (float) Math.sin(level.getGameTime() * 0.00031) * 0.02F + (r.nextFloat() - 0.5F) * 0.05F);
        if (level.getGameTime() % 20 == 0) sync(null, d);
        if (level.getGameTime() % 200 == 0) d.setDirty();
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p, data(p.serverLevel()));
    }

    // ------------------------------------------------------------------ ortamın etkisi

    private static boolean heatAround(ServerLevel l, BlockPos c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -4; dz <= 4; dz++) {
            p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            BlockState s = l.getBlockState(p);
            if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)) return true;
            if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) return true;
            if (s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(AbstractFurnaceBlock.LIT) && Math.abs(dx) <= 2 && Math.abs(dz) <= 2) return true;
        }
        return false;
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel l)
                || l.dimension() != Level.OVERWORLD || p.isSpectator()) return;
        WeatherData d = data(l);
        BlockPos head = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        boolean exposed = Climate.exposed(l, head);
        Climate.Kind kind = Climate.kind(l, p.blockPosition());
        boolean storm = d.state == WeatherState.STORM && d.intensity > 0.45F;
        if (p.tickCount % 20 == 0) WARM.put(p.getUUID(), heatAround(l, p.blockPosition()) ? 1 : 0);
        boolean warm = WARM.getOrDefault(p.getUUID(), 0) == 1;
        // tipide donma: dışarıda, ateşten uzakta, deri giysisiz; ateş yanında vanilya donmayı çözer
        if (storm && kind == Climate.Kind.SNOW && exposed && !warm && !p.isCreative() && p.canFreeze() && p.tickCount % 4 != 0) {
            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 30, p.getTicksFrozen() + 3));
        }
        // kum fırtınası: rüzgâra karşı yürümek zor
        if (storm && kind == Climate.Kind.SAND && exposed && p.tickCount % 40 == 0 && !p.isCreative()) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
        }
        RandomSource r = l.random;
        // yağmurda ekinler daha hızlı büyür
        if (d.state.precipitation && d.intensity > 0.2F) {
            for (int k = 0; k < 2; k++) {
                int x = p.getBlockX() + r.nextInt(49) - 24, z = p.getBlockZ() + r.nextInt(49) - 24;
                BlockPos pos = new BlockPos(x, l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                BlockState s = l.getBlockState(pos);
                if ((s.getBlock() instanceof CropBlock || s.getBlock() instanceof StemBlock || s.getBlock() instanceof SaplingBlock
                        || s.getBlock() instanceof SweetBerryBushBlock || s.getBlock() instanceof BambooStalkBlock) && l.isRainingAt(pos.above())) {
                    s.randomTick(l, pos, r);
                }
            }
        }
        // kar birikir (fırtınada kalınlaşır), güneşte ılık yerlerde erir
        int maxLayers = d.state == WeatherState.STORM ? 5 : d.state == WeatherState.RAIN ? 3 : 2;
        for (int k = 0; k < 2; k++) {
            int x = p.getBlockX() + r.nextInt(81) - 40, z = p.getBlockZ() + r.nextInt(81) - 40;
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos top = new BlockPos(x, y, z), below = top.below();
            BlockPos snowAt = l.getBlockState(below).is(Blocks.SNOW) ? below : l.getBlockState(top).is(Blocks.SNOW) ? top : null;
            boolean cold = l.getBiome(top).value().coldEnoughToSnow(top);
            boolean snowing = d.state.precipitation && cold && d.intensity > 0.2F && l.getBiome(top).value().hasPrecipitation();
            if (snowing) {
                float chance = 0.15F + 0.6F * d.intensity;
                if (r.nextFloat() > chance) continue;
                if (snowAt != null) {
                    int n = l.getBlockState(snowAt).getValue(SnowLayerBlock.LAYERS);
                    if (n < maxLayers) l.setBlockAndUpdate(snowAt, l.getBlockState(snowAt).setValue(SnowLayerBlock.LAYERS, n + 1));
                } else if (l.getBlockState(top).isAir() && Blocks.SNOW.defaultBlockState().canSurvive(l, top)) {
                    l.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                }
            } else if (snowAt != null && l.isDay() && !d.state.precipitation && r.nextInt(cold ? 6 : 2) == 0) {
                int n = l.getBlockState(snowAt).getValue(SnowLayerBlock.LAYERS);
                if (!cold) {
                    if (n > 1) l.setBlockAndUpdate(snowAt, l.getBlockState(snowAt).setValue(SnowLayerBlock.LAYERS, n - 1));
                    else l.removeBlock(snowAt, false);
                } else if (n > 1 && d.state == WeatherState.CLEAR) {
                    // soğuk biyomda güneşte kar oturur, ince bir örtü kalır
                    l.setBlockAndUpdate(snowAt, l.getBlockState(snowAt).setValue(SnowLayerBlock.LAYERS, n - 1));
                }
            }
        }
    }

    // ------------------------------------------------------------------ komut

    public static void commands(RegisterCommandsEvent e) {
        var root = Commands.literal("hava").requires(s -> s.hasPermission(2));
        for (WeatherState s : WeatherState.values()) {
            root.then(Commands.literal(s.id)
                    .executes(c -> setCmd(c.getSource(), s, 0, false))
                    .then(Commands.argument("saniye", IntegerArgumentType.integer(10, 1000000))
                            .executes(c -> setCmd(c.getSource(), s, IntegerArgumentType.getInteger(c, "saniye"), false))
                            .then(Commands.literal("hemen").executes(c -> setCmd(c.getSource(), s, IntegerArgumentType.getInteger(c, "saniye"), true))))
                    .then(Commands.literal("hemen").executes(c -> setCmd(c.getSource(), s, 0, true))));
        }
        root.then(Commands.literal("bilgi").executes(c -> {
            WeatherData d = data(c.getSource().getLevel());
            c.getSource().sendSuccess(() -> report(d), false);
            return 1;
        }));
        e.getDispatcher().register(root);
        e.getDispatcher().register(Commands.literal("realweather").requires(s -> s.hasPermission(2))
                .then(Commands.argument("durum", StringArgumentType.word()).executes(c -> {
                    WeatherState s = WeatherState.byId(StringArgumentType.getString(c, "durum"));
                    return s == null ? 0 : setCmd(c.getSource(), s, 0, true);
                })));
    }

    private static int setCmd(CommandSourceStack src, WeatherState s, int seconds, boolean now) {
        set(src.getServer().overworld(), s, seconds, now);
        src.sendSuccess(() -> Component.literal("Hava: " + s.title + (now ? " (hemen)" : " (cephe yaklaşıyor)")).withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    public static Component report(WeatherData d) {
        int min = d.ticksLeft / 1200;
        String[] dirs = {"güneyden", "güneybatıdan", "batıdan", "kuzeybatıdan", "kuzeyden", "kuzeydoğudan", "doğudan", "güneydoğudan"};
        // rüzgârın estiği yön açısı -> geldiği yön
        String from = dirs[Math.floorMod(Math.round((d.windAngle + 180) / 45F), 8)];
        int kmh = Math.round(d.wind * 80);
        return Component.literal("Şu an: " + d.state.title + " · rüzgâr " + from + " " + kmh + " km/s · " + min + " dk sonra: " + d.next.title)
                .withStyle(ChatFormatting.AQUA);
    }
}
