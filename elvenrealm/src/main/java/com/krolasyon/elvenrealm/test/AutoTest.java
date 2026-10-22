package com.krolasyon.elvenrealm.test;

import com.krolasyon.elvenrealm.ElvenRealm;
import com.krolasyon.elvenrealm.dialogue.DialogueManager;
import com.krolasyon.elvenrealm.dialogue.PlayerData;
import com.krolasyon.elvenrealm.entity.Activity;
import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.krolasyon.elvenrealm.entity.ElfKind;
import com.krolasyon.elvenrealm.item.SilverLeafItem;
import com.krolasyon.elvenrealm.world.CityLayout;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** CI-only (-Delvenrealm.autotest=true): Sylvaren'i bulur, kenti, karakterleri, diyaloğu, kiralamayı ve savaşı görüntüler. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final long SEED = 4242L;

    private record Step(int delay, String shot, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private boolean gui, allowScreen;
    private CityLayout.City city;
    private int y0;
    private final Map<ElfKind, ElfEntity> gallery = new EnumMap<>(ElfKind.class);
    private final List<Husk> husks = new ArrayList<>();
    private int gx, gy, gz;
    private CityLayout.City other;
    private com.krolasyon.elvenrealm.world.CityType otherType;
    private long otherD;
    private int oy;
    private final List<ElfEntity> folk = new ArrayList<>();

    public static void init() {
        LOG.info("[ELFTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        server(0, null, s -> {
            run(s, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 4500",
                    "weather clear", "gamemode spectator @a");
            ServerLevel l = s.overworld();
            city = CityLayout.nearest(l.getSeed(), l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l, 0, 0, 6);
            LOG.info("[ELFTEST] nearest city {}", city);
            if (city == null) return;
            y0 = CityLayout.base(city, l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l);
            LOG.info("[ELFTEST] base y {} type {} capital {}", y0, CityLayout.type(l.getSeed(), city, l.getChunkSource().getGenerator(),
                    l.getChunkSource().randomState(), l), CityLayout.capital(l.getSeed(), l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l));
            // çevredeki diğer kentler ve türleri; ziyaret için özel (ay / çiçek) bir kent seç
            for (int ci = -4; ci <= 4; ci++)
                for (int cj = -4; cj <= 4; cj++) {
                    CityLayout.City c = CityLayout.cell(l.getSeed(), ci, cj);
                    if (c == null || !CityLayout.valid(c, l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l)) continue;
                    com.krolasyon.elvenrealm.world.CityType t = CityLayout.type(l.getSeed(), c, l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l);
                    LOG.info("[ELFTEST] city {} {} at {} {} -> {} {}", ci, cj, c.cx(), c.cz(), t, t.name(c.seed()));
                    if (t == com.krolasyon.elvenrealm.world.CityType.CAPITAL) continue;
                    long d = (long) c.cx() * c.cx() + (long) c.cz() * c.cz();
                    boolean special = t != com.krolasyon.elvenrealm.world.CityType.COMMON;
                    boolean bestSpecial = other != null && otherType != com.krolasyon.elvenrealm.world.CityType.COMMON;
                    if (other == null || (special && !bestSpecial) || (special == bestSpecial && d < otherD)) {
                        other = c;
                        otherType = t;
                        otherD = d;
                    }
                }
            LOG.info("[ELFTEST] visiting {} {}", other, otherType);
            look(s, city.cx(), y0 + 110, city.cz() - 20, city.cx(), y0 + 10, city.cz() - 40);
        });
        // kent ve ağaçlar üretilip çizilsin
        wait(900, "overhead");
        server(5, null, s -> look(s, city.cx() + 40, y0 + 90, city.cz() + 40, city.cx(), y0 + 20, city.cz() - 60));
        wait(400, null);
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            int wood = 0, leaves = 0;
            for (int x = city.cx() - 80; x <= city.cx() + 80; x += 2)
                for (int z = city.cz() - 150; z <= city.cz() - 60; z += 2)
                    for (int y = y0 + 10; y <= y0 + 120; y += 2) {
                        net.minecraft.world.level.block.state.BlockState b = l.getBlockState(new BlockPos(x, y, z));
                        if (b.is(net.minecraft.world.level.block.Blocks.OAK_WOOD)) wood++;
                        else if (b.is(net.minecraft.tags.BlockTags.LEAVES)) leaves++;
                    }
            LOG.info("[ELFTEST] giant tree samples wood {} leaves {}", wood, leaves);
            look(s, city.cx(), y0 + 60, city.cz() + 85, city.cx(), y0 + 20, city.cz() - 45);
        });
        wait(300, "aerial_south");
        server(5, null, s -> look(s, city.cx() - 95, y0 + 65, city.cz() + 50, city.cx(), y0 + 25, city.cz() - 50));
        wait(250, "aerial_sw");
        server(5, null, s -> look(s, city.cx() + 100, y0 + 55, city.cz() - 30, city.cx(), y0 + 35, city.cz() - 75));
        wait(250, "aerial_east");
        server(5, null, s -> look(s, city.cx() + 7, y0 + 2.6, city.cz() + 66, city.cx(), y0 + 12, city.cz() + 20));
        wait(160, "gate_ground");
        server(5, null, s -> look(s, city.cx() - 9, y0 + 3, city.cz() + 26, city.cx(), y0 + 14, city.cz() - 40));
        wait(160, "courtyard");
        server(5, null, s -> look(s, city.cx() + 12, y0 + 11, city.cz() - 27, city.cx(), y0 + 34, city.cz() - 85));
        wait(160, "terrace_castle");
        server(5, null, s -> look(s, city.cx() + 30, y0 + 23, city.cz() - 52, city.cx() - 10, y0 + 40, city.cz() - 88));
        wait(160, "castle_close");
        server(5, null, s -> look(s, city.cx() + 85, y0 + 45, city.cz() - 60, city.cx() - 20, y0 + 60, city.cz() - 100));
        wait(220, "giant_trees");
        server(5, null, s -> look(s, city.cx() - 22, y0 + 3, city.cz() - 8, city.cx() - 20, y0 + 6, city.cz() - 26));
        wait(120, "waterfall");
        server(5, null, s -> {
            run(s, "time set 18000");
            look(s, city.cx(), y0 + 60, city.cz() + 85, city.cx(), y0 + 20, city.cz() - 45);
        });
        wait(120, "night");
        server(5, null, s -> {
            run(s, "time set 4500");
            // şehirde doğan sakinler
            int n = 0;
            for (ElfEntity e : s.overworld().getEntitiesOfClass(ElfEntity.class, new net.minecraft.world.phys.AABB(
                    city.cx() - 80, y0 - 20, city.cz() - 140, city.cx() + 80, y0 + 80, city.cz() + 60))) {
                LOG.info("[ELFTEST] city elf {} at {} home {}", e.kind, e.blockPosition(), e.home());
                n++;
            }
            LOG.info("[ELFTEST] city elves {}", n);
            look(s, city.cx() + 9, y0 + 3.5, city.cz() + 17, city.cx() + 20, y0 + 1.5, city.cz() + 8);
        });
        wait(100, "lirael_home");

        // ---------------------------------------------------- karakter galerisi (gökyüzü platformu)
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            gx = city.cx();
            gy = 240;
            gz = city.cz() + 160;
            run(s, String.format("fill %d %d %d %d %d %d minecraft:grass_block", gx - 16, gy - 1, gz - 12, gx + 16, gy - 1, gz + 12));
            run(s, String.format("fill %d %d %d %d %d %d minecraft:air", gx - 16, gy, gz - 12, gx + 16, gy + 8, gz + 12));
            int i = 0;
            for (ElfKind k : ElfKind.values()) {
                if (k.ordinal() > ElfKind.GUARD.ordinal()) break;
                ElfEntity e = ElvenRealm.TYPES.get(k).get().create(l);
                double x = gx - 9 + i * 3;
                e.moveTo(x + 0.5, gy, gz + 0.5, 0F, 0F);
                e.setHome(new BlockPos((int) x, gy, gz));
                e.finalizeSpawn(l, l.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null, null);
                e.setNoAi(true);
                e.setYRot(0F);
                e.yBodyRot = 0F;
                e.yHeadRot = 0F;
                l.addFreshEntity(e);
                gallery.put(k, e);
                i++;
            }
            look(s, gx + 0.5, gy + 1.6, gz + 9.5, gx + 0.5, gy + 1.0, gz);
        });
        wait(80, "gallery_front");
        server(5, null, s -> look(s, gx + 0.5, gy + 1.6, gz - 8.5, gx + 0.5, gy + 1.0, gz));
        wait(40, "gallery_back");
        server(5, null, s -> look(s, gx - 7.5, gy + 0.1, gz + 3.4, gx - 7.5, gy + 1.6, gz));
        wait(40, "close_lirael_vaelor");
        server(5, null, s -> look(s, gx - 1.5, gy + 0.1, gz + 3.4, gx - 1.5, gy + 1.6, gz));
        wait(40, "close_caelan_nimrae");
        server(5, null, s -> look(s, gx + 4.5, gy + 0.1, gz + 3.4, gx + 4.5, gy + 1.6, gz));
        wait(40, "close_tavi_thalion");
        server(5, null, s -> look(s, gx + 8.0, gy + 1.4, gz + 3.5, gx + 3.5, gy + 1.0, gz));
        wait(30, "side_tavi");
        server(5, null, s -> {
            Activity[] poses = {Activity.PRACTICE, Activity.CROSS_ARMS, Activity.READ, Activity.MEDITATE, Activity.KNEEL, Activity.WAVE, Activity.SIT, Activity.STAND};
            int i = 0;
            for (ElfEntity e : gallery.values()) e.setActivity(poses[i++], 100000);
            gallery.get(ElfKind.CAELAN).setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
            gallery.get(ElfKind.LIRAEL).setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            look(s, gx + 0.5, gy + 1.8, gz + 10.5, gx + 0.5, gy + 0.8, gz);
        });
        wait(60, "gallery_poses");

        // ---------------------------------------------------- yeni kentlerin halkı (ikinci platform)
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            int fz = gz + 40;
            run(s, String.format("fill %d %d %d %d %d %d minecraft:grass_block", gx - 18, gy - 1, fz - 8, gx + 18, gy - 1, fz + 8));
            run(s, String.format("fill %d %d %d %d %d %d minecraft:air", gx - 18, gy, fz - 8, gx + 18, gy + 8, fz + 8));
            Object[][] who = {{ElfKind.TOWNSFOLK, 0}, {ElfKind.TOWNSFOLK, 1}, {ElfKind.TOWNSFOLK, 2}, {ElfKind.TOWNSFOLK, 3}, {ElfKind.TOWNSFOLK, 4},
                    {ElfKind.MOON_SEER, 0}, {ElfKind.GUARD, 1}, {ElfKind.GUARD, 2}, {ElfKind.BLOSSOM_LADY, 0},
                    {ElfKind.TOWNSMAN, 0}, {ElfKind.TOWNSMAN, 1}, {ElfKind.TOWNSMAN, 2}, {ElfKind.TOWNSMAN, 3}, {ElfKind.TOWNSMAN, 4}};
            for (int j = 0; j < who.length; j++) {
                ElfEntity e = ElvenRealm.TYPES.get((ElfKind) who[j][0]).get().create(l);
                double x = gx - 13 + j * 2;
                e.moveTo(x + 0.5, gy, fz + 0.5, 0F, 0F);
                e.setHome(new BlockPos((int) x, gy, fz));
                e.finalizeSpawn(l, l.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null, null);
                e.setVariant((Integer) who[j][1]);
                e.setCityType(j < 6 ? com.krolasyon.elvenrealm.world.CityType.MOON : com.krolasyon.elvenrealm.world.CityType.BLOSSOM, "Lunareth", j * 977L);
                e.setNoAi(true);
                e.setYRot(0F);
                e.yBodyRot = 0F;
                e.yHeadRot = 0F;
                l.addFreshEntity(e);
                folk.add(e);
                LOG.info("[ELFTEST] folk {} variant {} name {}", e.kind, e.variant(), e.elfName());
            }
            look(s, gx - 6.5, gy + 1.5, fz + 6.5, gx - 6.5, gy + 1.0, fz);
        });
        wait(60, "folk_left");
        server(5, null, s -> look(s, gx + 6.5, gy + 1.5, gz + 46.5, gx + 6.5, gy + 1.0, gz + 40));
        wait(30, "folk_right");
        server(5, null, s -> look(s, gx + 0.5, gy + 0.2, gz + 43.2, gx + 0.5, gy + 1.6, gz + 40));
        wait(30, "folk_leaders_close");
        server(5, null, s -> look(s, gx + 0.5, gy + 1.5, gz + 33.5, gx + 0.5, gy + 1.0, gz + 40));
        wait(30, "folk_back");

        // ---------------------------------------------------- diyalog
        server(5, null, s -> {
            run(s, "gamemode creative @a");
            for (ElfEntity e : gallery.values()) e.setActivity(Activity.NONE, 0);
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.teleportTo(s.overworld(), gx - 8.5, gy, gz + 3.5, 160F, 10F);
        });
        client(20, null, mc -> {
            allowScreen = true;
            gui = true;
        });
        server(5, null, s -> DialogueManager.open(s.getPlayerList().getPlayers().get(0), gallery.get(ElfKind.LIRAEL)));
        wait(140, "dialogue_lirael");
        server(5, null, s -> DialogueManager.choose(s.getPlayerList().getPlayers().get(0), gallery.get(ElfKind.LIRAEL).getId(), 3));
        wait(200, "dialogue_quest");
        server(5, null, s -> DialogueManager.choose(s.getPlayerList().getPlayers().get(0), gallery.get(ElfKind.LIRAEL).getId(), 0));
        wait(60, "quest_accepted");
        // kiralama reddi (dostluk yetersiz)
        server(5, null, s -> DialogueManager.open(s.getPlayerList().getPlayers().get(0), gallery.get(ElfKind.VAELOR)));
        wait(20, null);
        server(5, null, s -> DialogueManager.choose(s.getPlayerList().getPlayers().get(0), gallery.get(ElfKind.VAELOR).getId(), 5));
        wait(200, "hire_refused");
        // kiralama (koşullar sağlanmış)
        server(5, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            PlayerData.addFriendship(p, ElfKind.LIRAEL, 7);
            PlayerData.finishQuest(p, ElfKind.LIRAEL);
            p.getInventory().add(new ItemStack(Items.EMERALD, 64));
            DialogueManager.open(p, gallery.get(ElfKind.LIRAEL));
        });
        wait(20, null);
        server(5, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            DialogueManager.choose(p, gallery.get(ElfKind.LIRAEL).getId(), 5);
            LOG.info("[ELFTEST] hired {} owner {} emeralds left {} friendship {}", gallery.get(ElfKind.LIRAEL).isHired(),
                    gallery.get(ElfKind.LIRAEL).ownerId(), p.getInventory().countItem(Items.EMERALD), PlayerData.friendship(p, ElfKind.LIRAEL));
        });
        wait(160, "hired");
        client(5, null, mc -> {
            allowScreen = false;
            mc.setScreen(null);
        });

        // ---------------------------------------------------- savaş: kiralık Lirael, Tavi ve Thalion kumdan zombilere karşı
        server(5, null, s -> {
            ServerLevel l = s.overworld();
            run(s, "difficulty easy");
            for (ElfKind k : new ElfKind[]{ElfKind.LIRAEL, ElfKind.TAVI, ElfKind.THALION, ElfKind.NIMRAE, ElfKind.CAELAN}) gallery.get(k).setNoAi(false);
            for (int i = 0; i < 4; i++) {
                Husk h = EntityType.HUSK.create(l);
                h.moveTo(gx - 6 + i * 4 + 0.5, gy, gz - 9.5, 0F, 0F);
                h.finalizeSpawn(l, l.getCurrentDifficultyAt(h.blockPosition()), MobSpawnType.COMMAND, null, null);
                l.addFreshEntity(h);
                husks.add(h);
            }
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.teleportTo(l, gx + 0.5, gy, gz + 8.5, 180F, 15F);
        });
        client(60, "combat_1", mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        wait(60, "combat_2");
        wait(60, "combat_3");
        wait(200, null);
        server(5, null, s -> {
            int alive = 0;
            for (Husk h : husks) if (h.isAlive()) alive++;
            LOG.info("[ELFTEST] husks alive after combat {}", alive);
            for (ElfEntity e : gallery.values()) LOG.info("[ELFTEST] state {} hp {} activity {} hired {}", e.kind, e.getHealth(), e.getActivity(), e.isHired());
        });
        wait(20, "after_combat");
        // takip: oyuncu uzaklaşınca Lirael gelir
        server(5, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.teleportTo(s.overworld(), gx + 12.5, gy, gz + 10.5, 135F, 20F);
        });
        wait(100, "follow");
        // yaralanma: evine döner, sözleşme biter
        server(5, null, s -> {
            ElfEntity e = gallery.get(ElfKind.LIRAEL);
            e.hurt(s.overworld().damageSources().generic(), 1000F);
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            LOG.info("[ELFTEST] after fall: alive {} recovering {} hired {} friendship {} pos {}", e.isAlive(), e.isRecovering(), e.isHired(),
                    PlayerData.friendship(p, ElfKind.LIRAEL), e.blockPosition());
            ItemStack leaf = new ItemStack(ElvenRealm.SILVER_LEAF.get());
            SilverLeafItem.search(s.overworld(), leaf, p);
            LOG.info("[ELFTEST] silver leaf tag {}", leaf.getTag());
        });
        // giriş başlığı
        server(5, null, s -> {
            run(s, "gamemode spectator @a");
            look(s, city.cx(), y0 + 25, city.cz() + 140, city.cx(), y0 + 15, city.cz());
        });
        client(60, null, mc -> gui = true);
        server(45, null, s -> look(s, city.cx(), y0 + 25, city.cz() + 55, city.cx(), y0 + 15, city.cz()));
        wait(5, "entry_title");
        client(2, null, mc -> gui = false);

        // ---------------------------------------------------- yaşayan kent: nöbet yerleri, taht, lord yürüyüşü
        server(5, null, s -> {
            run(s, "time set 1000", "gamemode spectator @a");
            for (ElfEntity e : s.overworld().getEntitiesOfClass(ElfEntity.class, new net.minecraft.world.phys.AABB(
                    city.cx() - 80, y0 - 20, city.cz() - 140, city.cx() + 80, y0 + 80, city.cz() + 60))) {
                LOG.info("[ELFTEST] resident {} post {} routine {} at {} activity {}", e.kind, e.post(), e.routineKey, e.blockPosition(), e.getActivity());
            }
            look(s, city.cx(), y0 + 19.5, city.cz() - 69, city.cx(), y0 + 17.5, city.cz() - 78);
        });
        wait(300, "lord_throne");
        server(5, null, s -> look(s, city.cx(), y0 + 5, city.cz() + 18, city.cx(), y0 + 2, city.cz() + 28));
        wait(120, "gate_guards");
        // ---------------------------------------------------- başka bir elf kenti: kendi halkı, kendi muhafızları, kendi lideri
        server(5, null, s -> {
            if (other == null) return;
            ServerLevel l = s.overworld();
            oy = CityLayout.base(other, l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l);
            run(s, "time set 4500");
            look(s, other.cx(), oy + 110, other.cz() - 20, other.cx(), oy + 10, other.cz() - 40);
        });
        wait(900, "other_overhead");
        server(5, null, s -> {
            if (other == null) return;
            look(s, other.cx(), oy + 60, other.cz() + 85, other.cx(), oy + 20, other.cz() - 45);
        });
        wait(300, "other_aerial");
        server(5, null, s -> {
            if (other == null) return;
            look(s, other.cx() + 85, oy + 45, other.cz() - 60, other.cx() - 20, oy + 60, other.cz() - 100);
        });
        wait(200, "other_trees");
        server(5, null, s -> {
            if (other == null) return;
            int n = 0;
            for (ElfEntity e : s.overworld().getEntitiesOfClass(ElfEntity.class, new net.minecraft.world.phys.AABB(
                    other.cx() - 80, oy - 20, other.cz() - 140, other.cx() + 80, oy + 80, other.cz() + 60))) {
                LOG.info("[ELFTEST] other resident {} name {} variant {} type {} post {} routine {} at {} activity {}", e.kind, e.elfName(), e.variant(),
                        e.cityType(), e.post(), e.routineKey, e.blockPosition(), e.getActivity());
                n++;
            }
            LOG.info("[ELFTEST] other residents {}", n);
            look(s, other.cx() - 9, oy + 4, other.cz() + 24, other.cx(), oy + 2, other.cz() + 6);
        });
        wait(200, "other_courtyard");
        server(5, null, s -> {
            if (other == null) return;
            look(s, other.cx() + 18, oy + 4, other.cz() + 18, other.cx() + 22, oy + 1, other.cz() + 8);
        });
        wait(100, "other_training");
        server(5, null, s -> {
            if (other == null) return;
            look(s, other.cx(), oy + 19.5, other.cz() - 69, other.cx(), oy + 17.5, other.cz() - 78);
        });
        wait(200, "other_throne");
        server(5, null, s -> {
            if (other == null) return;
            look(s, other.cx(), oy + 5, other.cz() + 18, other.cx(), oy + 2, other.cz() + 28);
        });
        wait(100, "other_gate");
        server(5, null, s -> {
            if (other == null) return;
            look(s, other.cx() - 4, oy + 14, other.cz() - 31, other.cx() - 20, oy + 9, other.cz() - 42);
        });
        wait(120, "other_library");
        // kente giriş başlığı ve bir kent sakiniyle konuşma
        server(5, null, s -> {
            if (other == null) return;
            run(s, "gamemode creative @a");
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.getPersistentData().putBoolean("elvenrealm_inside", false);
            p.teleportTo(s.overworld(), other.cx() + 0.5, oy + 1, other.cz() + 20.5, 180F, 0F);
        });
        client(5, null, mc -> gui = true);
        wait(50, "other_entry_title");
        client(5, null, mc -> allowScreen = true);
        server(5, null, s -> {
            if (other == null) return;
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            ElfEntity best = null;
            for (ElfEntity e : s.overworld().getEntitiesOfClass(ElfEntity.class, p.getBoundingBox().inflate(70), e -> e.kind.citizen()))
                if (best == null || e.distanceToSqr(p) < best.distanceToSqr(p)) best = e;
            if (best == null) return;
            p.teleportTo(s.overworld(), best.getX() + 2, best.getY(), best.getZ() + 2, 135F, 10F);
            DialogueManager.open(p, best);
            LOG.info("[ELFTEST] talking to {} {}", best.kind, best.elfName());
        });
        wait(80, "other_dialogue");
        client(5, null, mc -> {
            allowScreen = false;
            mc.setScreen(null);
            gui = false;
        });
        wait(20, "END");
    }

    private static void run(MinecraftServer s, String... cmds) {
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }

    /** (x,y,z) konumuna ışınlanıp (tx,ty,tz) noktasına bakar. */
    private static void look(MinecraftServer s, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        run(s, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    private void server(int wait, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(wait, shot, a, null)); }
    private void client(int wait, String shot, Consumer<Minecraft> a) { steps.add(new Step(wait, shot, null, a)); }
    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[ELFTEST] creating world");
            LevelSettings settings = new LevelSettings("elftest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("elftest", settings, new WorldOptions(SEED, true, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !allowScreen) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[ELFTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (city == null && stepIndex > 1) {
                LOG.info("[ELFTEST] no city found, finishing");
                mc.stop();
                return;
            }
            if (st.client() != null) st.client().accept(mc);
            MinecraftServer s = mc.getSingleplayerServer();
            if (st.server() != null) s.execute(() -> {
                try {
                    st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[ELFTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[ELFTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "elf_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[ELFTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
