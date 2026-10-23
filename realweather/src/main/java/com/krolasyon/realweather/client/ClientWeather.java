package com.krolasyon.realweather.client;

import com.krolasyon.realweather.Climate;
import com.krolasyon.realweather.Net;
import com.krolasyon.realweather.RealWeather;
import com.krolasyon.realweather.WeatherState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * İstemci tarafı hava: sunucudan gelen cepheyi kameranın bulunduğu yerin iklimine göre yorumlar (yağmur, kar, tipi,
 * kum fırtınası, sis, sabah pusu), değerleri yumuşakça geçirir; sisi, gök rengini, parçacıkları, ses döngülerini ve
 * şimşekleri yönetir.
 */
public final class ClientWeather {
    private ClientWeather() {}

    // sunucudan
    public static WeatherState state = WeatherState.CLEAR, next = WeatherState.CLEAR;
    public static int ticksLeft;
    private static float serverIntensity, serverWindAngle, serverWind = 0.2F;

    // yumuşatılmış, kameraya göre
    public static float intensity, windAngle, wind = 0.2F, stormness, overcast, fogW, exposure = 1;
    public static float rainW = 1, snowW, sandW, dryW;
    public static float rainAmt, snowAmt, blizzardAmt, sandAmt, fogAmt, mistAmt, gust = 1;
    public static double windX, windZ;
    public static boolean received;

    private static final Map<String, LoopSound> LOOPS = new HashMap<>();
    private record Pending(int at, String sound, float volume, float pitch) {}
    private static final List<Pending> THUNDER = new ArrayList<>();
    private static int clientTicks, flashCd = 200;

    public static void receive(Net.Sync s) {
        state = WeatherState.values()[Math.floorMod(s.state(), WeatherState.values().length)];
        next = WeatherState.values()[Math.floorMod(s.next(), WeatherState.values().length)];
        ticksLeft = s.ticksLeft();
        serverIntensity = s.intensity();
        serverWindAngle = s.windAngle();
        serverWind = s.wind();
        if (!received) {
            intensity = serverIntensity;
            windAngle = serverWindAngle;
            wind = serverWind;
            stormness = state == WeatherState.STORM ? intensity : 0;
            overcast = state == WeatherState.CLEAR ? 0 : 1;
            fogW = state == WeatherState.FOG ? 1 : 0;
        }
        received = true;
    }

    public static void reset() {
        received = false;
        for (LoopSound l : LOOPS.values()) l.kill();
        LOOPS.clear();
        THUNDER.clear();
    }

    private static float approach(float v, float t, float k) {
        return v + (t - v) * k;
    }

    // ------------------------------------------------------------------ tik

    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            if (!LOOPS.isEmpty()) reset();
            return;
        }
        if (mc.isPaused()) return;
        clientTicks++;
        boolean overworld = level.dimension() == Level.OVERWORLD;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 cp = cam.getPosition();
        BlockPos camPos = BlockPos.containing(cp);
        RandomSource r = level.random;

        // genel değerler (sunucunun yoğunluğuna yumuşakça yaklaş)
        intensity = approach(intensity, overworld ? serverIntensity : 0, 0.02F);
        wind = approach(wind, overworld ? serverWind : 0.1F, 0.02F);
        windAngle += Mth.wrapDegrees(serverWindAngle - windAngle) * 0.02F;
        stormness = approach(stormness, overworld && state == WeatherState.STORM ? 1 : 0, 0.01F);
        overcast = approach(overcast, overworld && state != WeatherState.CLEAR ? (state == WeatherState.FOG ? 0.6F : 1) : 0, 0.008F);
        fogW = approach(fogW, overworld && state == WeatherState.FOG ? 1 : 0, 0.006F);
        // esintiler
        double t = clientTicks;
        gust = (float) (0.72 + 0.18 * Math.sin(t * 0.031) * Math.sin(t * 0.0127 + 1.3) + 0.12 * Math.sin(t * 0.093 + Math.sin(t * 0.011) * 3));
        float ws = wind * gust * (1 + Mth.clamp((float) (cp.y - 90) / 120F, 0, 0.6F));
        double a = Math.toRadians(windAngle);
        windX = -Math.sin(a) * ws;
        windZ = Math.cos(a) * ws;

        // kameranın bulunduğu yerin iklimi (yumuşak geçiş)
        Climate.Kind kind = overworld ? Climate.kind(level, camPos) : Climate.Kind.DRY;
        float kk = 0.03F;
        rainW = approach(rainW, kind == Climate.Kind.RAIN ? 1 : 0, kk);
        snowW = approach(snowW, kind == Climate.Kind.SNOW ? 1 : 0, kk);
        sandW = approach(sandW, kind == Climate.Kind.SAND ? 1 : 0, kk);
        dryW = approach(dryW, kind == Climate.Kind.DRY ? 1 : 0, kk);
        // açıklık: başın çevresindeki dokuz noktanın kaçı gökyüzünü görüyor
        int open = 0;
        for (int dx = -2; dx <= 2; dx += 2) for (int dz = -2; dz <= 2; dz += 2) {
            if (Climate.exposed(level, camPos.offset(dx, 0, dz))) open++;
        }
        exposure = approach(exposure, open / 9F, 0.08F);

        float precip = state.precipitation || intensity > 0.02F && state != WeatherState.CLEAR ? intensity : 0;
        if (!overworld) precip = 0;
        rainAmt = precip * rainW;
        snowAmt = precip * snowW;
        blizzardAmt = snowAmt * stormness;
        sandAmt = sandW * Math.max(stormness * intensity, Mth.clamp((wind - 0.45F) * 1.2F, 0, 0.35F));
        fogAmt = fogW;
        long day = level.getDayTime() % 24000L;
        boolean dawn = day > 22500 || day < 1800;
        mistAmt = approach(mistAmt, overworld && dawn && state != WeatherState.STORM && Climate.humid(level, camPos) ? 0.75F : 0, 0.01F);

        // gök: bulutluluk ve yağışa göre kararır
        if (overworld) {
            float sky = Math.max(Math.max(rainAmt, snowAmt * 0.9F), Math.max(overcast * 0.45F, sandAmt * 0.7F));
            level.setRainLevel(Mth.clamp(sky, 0, 1));
            level.setThunderLevel(Mth.clamp(stormness * intensity * (1 - sandW), 0, 1));
        }

        if (overworld) {
            particles(mc, level, cp, r);
            lightning(mc, level, r);
            wind(mc.player);
        }
        sounds(mc, level, cp, overworld);
        Iterator<Pending> it = THUNDER.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (clientTicks >= p.at) {
                play(mc, p.sound, p.volume, p.pitch);
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ parçacıklar

    private static void particles(Minecraft mc, ClientLevel level, Vec3 cp, RandomSource r) {
        // yakındaki kar taneleri (tipide yoğun ve yatay)
        int flakes = (int) (snowAmt * exposure * (6 + 22 * stormness));
        for (int i = 0; i < flakes; i++) {
            double x = cp.x + r.nextGaussian() * 6, z = cp.z + r.nextGaussian() * 6, y = cp.y + r.nextDouble() * 8 - 2;
            if (!Climate.exposed(level, BlockPos.containing(x, y, z))) continue;
            level.addParticle(RealWeather.SNOWFLAKE.get(), x - windX * 6, y, z - windZ * 6, windX, -0.05, windZ);
        }
        // kum taneleri: yere yakın, yatay akan
        int grains = (int) (sandAmt * (8 + 30 * stormness));
        for (int i = 0; i < grains; i++) {
            double x = cp.x + r.nextGaussian() * 7 - windX * 8, z = cp.z + r.nextGaussian() * 7 - windZ * 8;
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            double y = gy + r.nextDouble() * r.nextDouble() * 4;
            if (Math.abs(y - cp.y) > 10) continue;
            level.addParticle(RealWeather.SAND.get(), x, y, z, windX * 1.4, 0, windZ * 1.4);
        }
        // rüzgârda düşen yapraklar
        float leafRate = Mth.clamp(0.08F + (float) Math.hypot(windX, windZ) * 0.6F, 0, 0.6F);
        for (int i = 0; i < 8; i++) {
            BlockPos p = BlockPos.containing(cp.x + r.nextInt(29) - 14, cp.y + r.nextInt(16) - 4, cp.z + r.nextInt(29) - 14);
            BlockState s = level.getBlockState(p);
            if (!s.is(BlockTags.LEAVES) || !level.getBlockState(p.below()).isAir() || r.nextFloat() > leafRate) continue;
            level.addParticle(RealWeather.LEAF.get(), p.getX() + r.nextDouble(), p.getY() - 0.05, p.getZ() + r.nextDouble(), windX, 0, windZ);
            // yağmurdan sonra yapraklardan damlar
            if ((rainAmt > 0.1F) && r.nextInt(3) == 0)
                level.addParticle(ParticleTypes.DRIPPING_WATER, p.getX() + r.nextDouble(), p.getY() - 0.05, p.getZ() + r.nextDouble(), 0, 0, 0);
        }
        // ateş böcekleri: ılık, açık havada gece
        long day = level.getDayTime() % 24000L;
        boolean night = day > 13000 && day < 23000;
        BlockPos cb = BlockPos.containing(cp);
        float temp = level.getBiome(cb).value().getBaseTemperature();
        if (night && rainAmt < 0.05F && snowW < 0.2F && sandW < 0.2F && temp >= 0.5F && temp <= 1.1F && r.nextFloat() < 0.45F) {
            double x = cp.x + r.nextInt(41) - 20, z = cp.z + r.nextInt(41) - 20;
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            if (Math.abs(gy - cp.y) < 14) level.addParticle(RealWeather.FIREFLY.get(), x, gy + 0.4 + r.nextDouble() * 2.2, z, 0, 0, 0);
        }
        // sis ve pus: yerde sürünen büyük, saydam bulutçuklar
        float mist = Math.max(mistAmt, fogAmt * 0.8F);
        if (mist > 0.1F) {
            for (int i = 0; i < 2; i++) {
                if (r.nextFloat() > mist) continue;
                double x = cp.x + r.nextInt(49) - 24, z = cp.z + r.nextInt(49) - 24;
                int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
                if (Math.abs(gy - cp.y) > 16) continue;
                level.addParticle(RealWeather.MIST.get(), x, gy + 0.3 + r.nextDouble() * 1.8, z, windX * 0.15, 0, windZ * 0.15);
            }
        }
        // soğukta nefes buharı
        if (clientTicks % 5 == 0) {
            for (Player p : level.players()) {
                if (p.distanceToSqr(cp) > 400 || p.isUnderWater() || p.isInvisible()) continue;
                BlockPos pp = p.blockPosition();
                boolean cold = level.getBiome(pp).value().coldEnoughToSnow(pp) || level.getBiome(pp).value().getBaseTemperature() < 0.2F;
                if (!cold || (p.tickCount + p.getId() * 7) % 60 > 4) continue;
                Vec3 look = p.getLookAngle();
                Vec3 m = p.getEyePosition().add(look.scale(0.35)).add(0, -0.12, 0);
                if (p == mc.player && mc.options.getCameraType().isFirstPerson()) m = m.add(look.scale(0.2));
                level.addParticle(RealWeather.BREATH.get(), m.x, m.y, m.z, look.x * 0.03 + windX * 0.05, 0.01, look.z * 0.03 + windZ * 0.05);
            }
        }
    }

    // ------------------------------------------------------------------ şimşek ve gök gürültüsü

    private static void lightning(Minecraft mc, ClientLevel level, RandomSource r) {
        if (stormness < 0.3F || rainW + snowW < 0.3F) return;
        if (--flashCd > 0) return;
        flashCd = 120 + r.nextInt(360);
        // uzak şimşek: gök aydınlanır, gecikmeyle gürültü gelir (ses hızı)
        float dist = 200 + r.nextFloat() * 1400;
        level.setSkyFlashTime(2 + r.nextInt(2));
        int delay = (int) (dist / 343F * 20F);
        THUNDER.add(new Pending(clientTicks + delay, "thunder_far", Mth.clamp(1.3F - dist / 1600F, 0.25F, 1F), 0.85F + r.nextFloat() * 0.25F));
    }

    /** Yakına düşen yıldırımlar için gecikmeli, mesafeye göre yakın/uzak gürültü. */
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() || !(e.getEntity() instanceof LightningBolt b)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double d = Math.sqrt(b.distanceToSqr(mc.player));
        int delay = (int) (d / 343.0 * 20);
        THUNDER.add(new Pending(clientTicks + delay, d < 120 ? "thunder_near" : "thunder_far", (float) Mth.clamp(1.4 - d / 300, 0.35, 1.0),
                0.9F + mc.level.random.nextFloat() * 0.2F));
    }

    private static void play(Minecraft mc, String sound, float volume, float pitch) {
        ResourceLocation id = new ResourceLocation(RealWeather.MODID, sound);
        mc.getSoundManager().play(new SimpleSoundInstance(id, SoundSource.WEATHER, volume, pitch, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }

    // ------------------------------------------------------------------ rüzgâr itişi

    private static void wind(LocalPlayer p) {
        double s = Math.hypot(windX, windZ);
        if (s < 0.6 || exposure < 0.6F || p.isShiftKeyDown() || p.getAbilities().flying || p.isPassenger() || p.isInWater()) return;
        double k = (s - 0.6) * 0.012 * exposure;
        p.push(windX / s * k, 0, windZ / s * k);
    }

    // ------------------------------------------------------------------ sesler

    private static void sounds(Minecraft mc, ClientLevel level, Vec3 cp, boolean overworld) {
        float e = exposure, in = 1 - exposure;
        float heavy = Mth.clamp((rainAmt - 0.35F) / 0.5F, 0, 1);
        long day = level.getDayTime() % 24000L;
        boolean night = day > 13000 && day < 23000;
        float temp = level.getBiome(BlockPos.containing(cp)).value().getBaseTemperature();
        // yakında yaprak var mı (yaprak hışırtısı ve yaprağa düşen yağmur için)
        float leafy = 0;
        if (clientTicks % 10 == 0 || leafiness < 0) leafiness = leafCount(level, cp);
        leafy = leafiness;
        float windS = (float) Math.hypot(windX, windZ);
        loop(mc, "rain_heavy", overworld ? rainAmt * heavy * e * 0.9F : 0, SoundSource.WEATHER);
        loop(mc, "rain_light", overworld ? rainAmt * (1 - heavy * 0.6F) * e * 0.8F : 0, SoundSource.WEATHER);
        loop(mc, "rain_indoor", overworld && in > 0.3F ? rainAmt * in * 0.9F : 0, SoundSource.WEATHER);
        loop(mc, "rain_leaves", overworld ? rainAmt * leafy * 0.7F : 0, SoundSource.WEATHER);
        loop(mc, "wind_soft", overworld ? Mth.clamp(windS * 0.9F, 0, 0.7F) * (0.35F + 0.65F * e) * (1 - blizzardAmt) : 0, SoundSource.WEATHER);
        loop(mc, "wind_strong", overworld ? Mth.clamp((windS - 0.55F) * 1.8F, 0, 1) * (0.3F + 0.7F * e) * (1 - blizzardAmt) * (1 - sandAmt) : 0,
                SoundSource.WEATHER);
        loop(mc, "blizzard", overworld ? blizzardAmt * (0.3F + 0.7F * e) : 0, SoundSource.WEATHER);
        loop(mc, "sandstorm", overworld ? Mth.clamp(sandAmt * 1.4F, 0, 1) * (0.3F + 0.7F * e) : 0, SoundSource.WEATHER);
        loop(mc, "leaves", overworld ? leafy * Mth.clamp(windS * 1.2F, 0.1F, 1) * 0.6F * (0.4F + 0.6F * e) * (1 - rainAmt * 0.5F) : 0, SoundSource.AMBIENT);
        loop(mc, "crickets", overworld && night && rainAmt < 0.05F && snowW < 0.2F && temp >= 0.5F && temp <= 1.6F ? 0.45F * (0.5F + 0.5F * e) : 0,
                SoundSource.AMBIENT);
    }

    private static float leafiness = -1;

    private static float leafCount(ClientLevel level, Vec3 cp) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -8; dx <= 8; dx += 4) for (int dz = -8; dz <= 8; dz += 4) for (int dy = -2; dy <= 10; dy += 3) {
            p.set(cp.x + dx, cp.y + dy, cp.z + dz);
            if (level.getBlockState(p).is(BlockTags.LEAVES)) n++;
        }
        return Mth.clamp(n / 12F, 0, 1);
    }

    private static void loop(Minecraft mc, String id, float target, SoundSource src) {
        LoopSound l = LOOPS.get(id);
        if (l != null && l.isStopped()) {
            LOOPS.remove(id);
            l = null;
        }
        if (l == null) {
            if (target < 0.02F) return;
            l = new LoopSound(RealWeather.sound(id), src);
            LOOPS.put(id, l);
            mc.getSoundManager().play(l);
        }
        l.target = target;
    }

    /** Test ve hata ayıklama: çalan döngülerin anlık sesleri. */
    public static String loopReport() {
        StringBuilder b = new StringBuilder();
        for (var en : LOOPS.entrySet()) b.append(en.getKey()).append('=').append(String.format(java.util.Locale.ROOT, "%.2f", en.getValue().getVolume())).append(' ');
        return b.toString();
    }

    // ------------------------------------------------------------------ sis

    /** Yoğunluk 0..1 → görüş mesafesi. */
    public static void fog(ViewportEvent.RenderFog e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD || e.getCamera().getFluidInCamera() != FogType.NONE) return;
        if (mc.player != null && (mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.DARKNESS))) return;
        float base = e.getFarPlaneDistance();
        float inside = 0.45F + 0.55F * exposure;
        float far = base;
        far = Math.min(far, Mth.lerp(blizzardAmt * inside, base, 16));
        far = Math.min(far, Mth.lerp(snowAmt * 0.65F * inside, base, 56));
        far = Math.min(far, Mth.lerp(Mth.clamp(sandAmt * 1.3F, 0, 1) * inside, base, 13));
        far = Math.min(far, Mth.lerp(rainAmt * (0.45F + 0.4F * stormness) * inside, base, 48));
        far = Math.min(far, Mth.lerp(fogAmt * inside, base, 22));
        far = Math.min(far, Mth.lerp(mistAmt * inside, base, 44));
        far = Math.min(far, Mth.lerp(overcast * 0.15F, base, base * 0.7F));
        if (far >= base - 0.5F) return;
        e.setFarPlaneDistance(far);
        e.setNearPlaneDistance(e.getMode() == FogRenderer.FogMode.FOG_SKY ? 0 : Math.min(e.getNearPlaneDistance(), far * 0.08F));
        e.setCanceled(true);
    }

    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD || e.getCamera().getFluidInCamera() != FogType.NONE) return;
        float b = Mth.clamp(mc.level.getSkyDarken((float) e.getPartialTick()) * 1.1F, 0.08F, 1);
        float r = e.getRed(), g = e.getGreen(), bl = e.getBlue();
        float[][] tints = {
                {blizzardAmt, 0.80F, 0.85F, 0.92F},
                {snowAmt * 0.5F, 0.78F, 0.81F, 0.86F},
                {Mth.clamp(sandAmt * 1.2F, 0, 1), 0.80F, 0.60F, 0.37F},
                {rainAmt * 0.55F, 0.42F, 0.46F, 0.51F},
                {fogAmt * 0.85F, 0.70F, 0.72F, 0.74F},
                {mistAmt * 0.6F, 0.76F, 0.78F, 0.80F}};
        for (float[] t : tints) {
            float k = Mth.clamp(t[0], 0, 1);
            if (k <= 0) continue;
            r = Mth.lerp(k, r, t[1] * b);
            g = Mth.lerp(k, g, t[2] * b);
            bl = Mth.lerp(k, bl, t[3] * b);
        }
        e.setRed(r);
        e.setGreen(g);
        e.setBlue(bl);
    }
}
