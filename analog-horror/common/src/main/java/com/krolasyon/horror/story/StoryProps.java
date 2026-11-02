package com.krolasyon.horror.story;

import com.krolasyon.horror.block.PropBlock;
import com.krolasyon.horror.house.HauntedHouse;
import com.krolasyon.horror.registry.ModBlocks;
import com.krolasyon.horror.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Notes and pickups only exist once the story has reached the step they belong to, so nothing from a later objective
 * can be found (or skipped to) early. Props of later steps are removed from houses built by older versions.
 */
public final class StoryProps {
    private StoryProps() {}

    public static final List<String> ORDER = List.of("intro", "ch1_find", "ch1_play", "ch1_knock", "ch1_storage", "ch1_stairs",
            "ch2_start", "ch2_fuses", "ch2_fusebox", "ch2_power", "ch2_watch", "ch2_climb",
            "ch3_start", "ch3_candles", "ch3_tape", "ch3_key", "ch3_escape",
            "ch4_start", "ch4_dials", "ch4_chase", "ch5_start", "ch5_master", "ch5_final", "end_host", "end_evict", "end_return");

    record Prop(String id, String step, int x, int y, int z, boolean note, int index, boolean raised) {}

    private static Prop n(int note, String step, int x, int y, int z, boolean raised) { return new Prop("n" + note, step, x, y, z, true, note, raised); }

    private static Prop i(String id, int item, String step, int x, int y, int z, boolean raised) { return new Prop(id, step, x, y, z, false, item, raised); }

    static final List<Prop> PROPS = List.of(
            // chapter 1 - ground floor
            n(0, "ch1_find", 11, 1, 20, false), n(1, "ch1_find", 4, 2, 1, false), n(18, "ch1_find", 6, 1, 10, false), n(3, "ch1_find", 4, 2, 17, true),
            i("tape1", ModItems.TAPE_BIRTHDAY, "ch1_find", 4, 2, 6, true),
            n(2, "ch1_storage", 13, 2, 1, false), n(4, "ch1_storage", 17, 1, 9, false),
            // chapter 2 - upstairs
            n(5, "ch2_fuses", 16, 7, 15, false), n(6, "ch2_fuses", 2, 8, 10, false), n(7, "ch2_fuses", 9, 7, 13, false), n(8, "ch2_fuses", 4, 7, 16, false),
            // chapter 3 - Mira's room, then the dark bedroom
            n(9, "ch3_candles", 14, 7, 9, false), n(10, "ch3_candles", 18, 7, 9, false), n(11, "ch3_candles", 13, 7, 3, false), n(12, "ch3_candles", 19, 7, 2, false),
            i("lighter", ModItems.LIGHTER, "ch3_candles", 16, 7, 8, false),
            n(13, "ch3_key", 2, 8, 2, false), i("attic_key", ModItems.ATTIC_KEY, "ch3_key", 3, 7, 2, false),
            // chapter 4 - the attic
            n(14, "ch4_dials", 5, 13, 6, false), n(15, "ch4_dials", 15, 13, 6, false), n(16, "ch4_dials", 13, 13, 8, false), n(17, "ch4_dials", 7, 13, 15, false),
            n(19, "ch4_dials", 12, 13, 18, false), i("eviction", ModItems.EVICTION_NOTICE, "ch4_dials", 13, 13, 7, false));

    public static int stage(String step) {
        int i = ORDER.indexOf(step);
        return i < 0 ? 0 : i;
    }

    /** places props whose step has been reached (once each) and removes props that belong to later steps */
    public static void sync(ServerLevel l, StoryState s) {
        int now = stage(s.step);
        for (Prop p : PROPS) {
            BlockPos pos = s.at(p.x(), p.y(), p.z());
            BlockState cur = l.getBlockState(pos);
            Block b = p.note() ? ModBlocks.NOTE_PAPER.get() : ModBlocks.STORY_ITEM.get();
            if (stage(p.step()) <= now) {
                if (s.flags.add("prop_" + p.id())) {
                    BlockState st = p.note()
                            ? b.defaultBlockState().setValue(PropBlock.NOTE, p.index()).setValue(PropBlock.RAISED, p.raised())
                            : HauntedHouse.item(p.index(), p.raised());
                    l.setBlock(pos, st, 3);
                }
            } else if (cur.getBlock() == b) {
                l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
