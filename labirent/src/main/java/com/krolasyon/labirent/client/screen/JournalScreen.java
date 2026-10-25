package com.krolasyon.labirent.client.screen;

import com.krolasyon.labirent.client.ClientState;
import com.krolasyon.labirent.Labirent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Glade journal. The story text lives in assets/labirent/story/*.txt, split into chapters with
 * "## n" headers and "## memory n" sections; only what the group has lived through so far is shown.
 */
public final class JournalScreen {
    private JournalScreen() {}

    public static BookViewScreen create() {
        List<FormattedText> pages = paginate(visibleText());
        return new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() { return pages.size(); }

            @Override
            public FormattedText getPageRaw(int i) { return pages.get(i); }
        });
    }

    private static String load(String lang) {
        ResourceLocation loc = new ResourceLocation(Labirent.MODID, "story/" + lang + ".txt");
        Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (r.isEmpty()) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(r.get().open(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String[]> visibleText() {
        String all = load("tr_tr");
        List<String[]> out = new ArrayList<>();
        if (all == null) {
            out.add(new String[]{"?", "Günlük bulunamadı."});
            return out;
        }
        int chapter = ClientState.chapter();
        int memories = ClientState.state == null ? 0 : ClientState.state.memories();
        String[] parts = all.split("(?m)^## ");
        for (String p : parts) {
            if (p.isBlank()) continue;
            int nl = p.indexOf('\n');
            String header = p.substring(0, nl).trim();
            String body = p.substring(nl + 1).trim();
            String[] hs = header.split(" ", 3);
            boolean show;
            String title;
            if (hs[0].equals("memory")) {
                show = Integer.parseInt(hs[1]) <= memories;
                title = hs.length > 2 ? hs[2] : "";
            } else {
                show = Integer.parseInt(hs[0]) <= chapter;
                title = hs.length > 1 ? header.substring(hs[0].length()).trim() : "";
            }
            if (show) out.add(new String[]{title, body});
        }
        return out;
    }

    private static List<FormattedText> paginate(List<String[]> sections) {
        Font font = Minecraft.getInstance().font;
        List<FormattedText> pages = new ArrayList<>();
        for (String[] s : sections) {
            List<FormattedText> lines = new ArrayList<>();
            lines.add(Component.literal(s[0]).withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.DARK_RED));
            lines.add(FormattedText.EMPTY);
            for (String para : s[1].split("\n")) {
                if (para.isBlank()) {
                    lines.add(FormattedText.EMPTY);
                    continue;
                }
                lines.addAll(font.getSplitter().splitLines(para.trim(), 114, Style.EMPTY));
            }
            for (int k = 0; k < lines.size(); k += 14) {
                List<FormattedText> page = lines.subList(k, Math.min(lines.size(), k + 14));
                List<FormattedText> joined = new ArrayList<>();
                for (int i = 0; i < page.size(); i++) {
                    joined.add(page.get(i));
                    if (i < page.size() - 1) joined.add(FormattedText.of("\n"));
                }
                pages.add(FormattedText.composite(joined));
            }
        }
        if (pages.isEmpty()) pages.add(FormattedText.of("..."));
        return pages;
    }
}
