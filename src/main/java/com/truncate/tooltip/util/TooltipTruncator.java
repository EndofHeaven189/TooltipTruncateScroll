package com.truncate.tooltip.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

public final class TooltipTruncator {
    private record Glyph(int codePoint, Style style) {}

    private TooltipTruncator() {}

    /** 同时解析继承样式与旧式 § 颜色码，格式字符不占字符宽度。 */
    private static List<Glyph> glyphs(FormattedText text) {
        List<Glyph> result = new ArrayList<>();
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            result.add(new Glyph(codePoint, style));
            return true;
        });
        return result;
    }

    private static MutableComponent component(List<Glyph> glyphs) {
        MutableComponent result = Component.empty();
        StringBuilder run = new StringBuilder();
        Style previous = Style.EMPTY;
        for (Glyph glyph : glyphs) {
            if (!glyph.style().equals(previous) && !run.isEmpty()) {
                result.append(Component.literal(run.toString()).withStyle(previous));
                run.setLength(0);
            }
            previous = glyph.style();
            run.appendCodePoint(glyph.codePoint());
        }
        if (!run.isEmpty()) result.append(Component.literal(run.toString()).withStyle(previous));
        return result;
    }

    public static MutableComponent copy(FormattedText text) {
        return component(glyphs(text));
    }

    /** 按真实换行符分行，颜色等样式不会因换行丢失。 */
    public static List<Component> splitExplicitLines(FormattedText text) {
        List<Component> result = new ArrayList<>();
        List<Glyph> line = new ArrayList<>();
        for (Glyph glyph : glyphs(text)) {
            if (glyph.codePoint() == '\n') {
                result.add(component(line));
                line.clear();
            } else if (glyph.codePoint() != '\r') {
                line.add(glyph);
            }
        }
        result.add(component(line));
        return result;
    }

    public static Component truncateCharacters(FormattedText text, int limit, boolean chineseDoubleWidth) {
        return truncate(text, limit, chineseDoubleWidth, "…", false);
    }

    /** 行数截断也预留标记宽度，避免追加三个点后突破字符上限。 */
    public static Component appendLineMarker(FormattedText text, boolean charLimited,
                                           int limit, boolean chineseDoubleWidth) {
        return truncate(text, charLimited ? limit : Integer.MAX_VALUE,
                chineseDoubleWidth, "...", true);
    }

    private static Component truncate(FormattedText text, int limit, boolean doubleWidth,
                                      String marker, boolean forceMarker) {
        List<Glyph> source = glyphs(text);
        long width = 0;
        for (Glyph glyph : source) width += characterWidth(glyph.codePoint(), doubleWidth);
        if (!forceMarker && width <= limit) return component(source);

        int available = Math.max(0, limit - marker.codePointCount(0, marker.length()));
        int end = 0;
        int used = 0;
        while (end < source.size()) {
            int next = characterWidth(source.get(end).codePoint(), doubleWidth);
            if (used + next > available) break;
            used += next;
            end++;
        }

        // 先看截断点两侧，只有落在英文单词内部时才回退到单词开头。
        if (end > 0 && end < source.size()
                && wordCharacter(source.get(end - 1).codePoint())
                && wordCharacter(source.get(end).codePoint())) {
            while (end > 0 && wordCharacter(source.get(end - 1).codePoint())) end--;
        }
        while (end > 0 && Character.isWhitespace(source.get(end - 1).codePoint())) end--;

        MutableComponent result = component(source.subList(0, end));
        Style markerStyle = end > 0 ? source.get(end - 1).style()
                : source.isEmpty() ? Style.EMPTY : source.getFirst().style();
        return result.append(Component.literal(marker).withStyle(markerStyle));
    }

    private static boolean wordCharacter(int cp) {
        return Character.UnicodeScript.of(cp) == Character.UnicodeScript.LATIN
                || Character.isDigit(cp) || cp == '_' || cp == '\'' || cp == '’' || cp == '-'
                || Character.getType(cp) == Character.NON_SPACING_MARK;
    }

    public static int characterWidth(int cp, boolean doubleWidth) {
        int type = Character.getType(cp);
        // 组合音标与变体选择符不额外占宽度；按码点处理，不拆 UTF-16 代理对。
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || cp == 0x200D) return 0;
        if (doubleWidth && (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN
                || cp >= 0x3000 && cp <= 0x303F
                || cp >= 0xFF01 && cp <= 0xFF60
                || cp >= 0xFFE0 && cp <= 0xFFE6)) return 2;
        return 1;
    }

    public static int pageCount(int totalLines, int pageSize) {
        if (pageSize < 1) throw new IllegalArgumentException("pageSize 必须大于 0");
        return totalLines <= 0 ? 1 : (totalLines - 1) / pageSize + 1;
    }
}
