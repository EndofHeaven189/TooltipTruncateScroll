package com.truncate.tooltip.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_LINE_LIMIT;
    public static final ModConfigSpec.IntValue MAX_LINES;
    public static final ModConfigSpec.BooleanValue ENABLE_CHAR_LIMIT;
    public static final ModConfigSpec.IntValue MAX_CHARS_PER_LINE;
    public static final ModConfigSpec.BooleanValue CHINESE_DOUBLE_WIDTH;
    public static final ModConfigSpec.BooleanValue ENABLE_SCROLL;
    public static final ModConfigSpec.IntValue SCROLL_SPEED;
    public static final ModConfigSpec.BooleanValue ENABLE_PAGE_FLIP;
    public static final ModConfigSpec.BooleanValue SHOW_PAGE_INDICATOR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        // 不使用 push，确保 TOML 中的配置键与需求中的名称完全一致。
        ENABLE_LINE_LIMIT = builder.comment("是否限制可见文本行数；关闭后显示全部行，滚动和翻页暂停。")
                .translation("tooltiptruncatescroll.config.enableLineLimit")
                .define("enableLineLimit", true);
        MAX_LINES = builder.comment("每页的正文行数；页码不占正文行数。")
                .translation("tooltiptruncatescroll.config.maxLines")
                .defineInRange("maxLines", 8, 1, 20);
        ENABLE_CHAR_LIMIT = builder.comment("是否启用单行字符宽度限制；省略号也计入上限。")
                .translation("tooltiptruncatescroll.config.enableCharLimit")
                .define("enableCharLimit", true);
        MAX_CHARS_PER_LINE = builder.comment("单行最大字符宽度。")
                .translation("tooltiptruncatescroll.config.maxCharsPerLine")
                .defineInRange("maxCharsPerLine", 60, 20, 120);
        CHINESE_DOUBLE_WIDTH = builder.comment("汉字、中文标点和全角字符按 2 个宽度计算。")
                .translation("tooltiptruncatescroll.config.chineseDoubleWidth")
                .define("chineseDoubleWidth", false);
        ENABLE_SCROLL = builder.comment("悬停时使用鼠标滚轮查看隐藏行。")
                .translation("tooltiptruncatescroll.config.enableScroll")
                .define("enableScroll", false);
        SCROLL_SPEED = builder.comment("每个滚轮刻度移动的行数。")
                .translation("tooltiptruncatescroll.config.scrollSpeed")
                .defineInRange("scrollSpeed", 1, 1, 20);
        ENABLE_PAGE_FLIP = builder.comment("允许通过可配置的按键翻页。")
                .translation("tooltiptruncatescroll.config.enablePageFlip")
                .define("enablePageFlip", true);
        SHOW_PAGE_INDICATOR = builder.comment("启用翻页且内容超过一页时显示页码。")
                .translation("tooltiptruncatescroll.config.showPageIndicator")
                .define("showPageIndicator", true);
        SPEC = builder.build();
    }

    private ModConfig() {}
}
