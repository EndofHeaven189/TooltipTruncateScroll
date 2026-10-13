package com.truncate.tooltip.event;

import com.mojang.datafixers.util.Either;
import com.truncate.tooltip.TooltipTruncateScroll;
import com.truncate.tooltip.config.ModConfig;
import com.truncate.tooltip.state.TooltipStateManager;
import com.truncate.tooltip.util.TooltipTruncator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME,
        modid = TooltipTruncateScroll.MODID, value = Dist.CLIENT)
public final class TooltipRenderHandler {
    private static final TooltipStateManager STATE = TooltipStateManager.getInstance();

    /** 非文本组件附着在前一行上，例如收纳袋预览跟随物品名称。 */
    private static final class Row {
        private final Component text;
        private final List<TooltipComponent> images = new ArrayList<>();

        private Row(Component text) { this.text = text; }
    }

    private record PageTooltip(List<ClientTooltipComponent> body,
                               ClientTooltipComponent footer) implements TooltipComponent {}

    private TooltipRenderHandler() {}

    /** 此方法由主类添加到 MOD 总线，因此不标记 SubscribeEvent。 */
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PageTooltip.class, PageClientTooltip::new);
    }

    @SubscribeEvent
    public static void onFramePre(RenderFrameEvent.Pre event) {
        STATE.beginFrame(Minecraft.getInstance().screen);
    }

    @SubscribeEvent
    public static void onFramePost(RenderFrameEvent.Post event) {
        STATE.endFrame();
    }

    public static Object hoverTarget(Screen screen) {
        if (screen == null) return null;
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        if (screen instanceof AbstractContainerScreen<?> container) {
            // 使用槽位身份，使相邻的两个相同物品也会重置页码。
            var slot = container.getSlotUnderMouse();
            // 输入事件可能早于下一次渲染；额外检查实时鼠标坐标，避免使用过期槽位。
            if (slot != null && slot.isActive()
                    && x >= container.getGuiLeft() + slot.x - 1 && x < container.getGuiLeft() + slot.x + 17
                    && y >= container.getGuiTop() + slot.y - 1 && y < container.getGuiTop() + slot.y + 17) return slot;
        }
        return screen.getChildAt(x, y).map(child -> (Object) child).orElse(screen);
    }

    /** 输入事件只允许操纵当前屏幕、当前悬停目标的 tooltip。 */
    public static boolean hasNavigableTooltip(Screen screen) {
        if (!ModConfig.ENABLE_LINE_LIMIT.get()) return false;
        if (!STATE.matches(screen, hoverTarget(screen))) {
            STATE.reset();
            return false;
        }
        return STATE.canNavigate();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        Minecraft mc = Minecraft.getInstance();
        Font font = ClientHooks.getTooltipFont(event.getItemStack(), mc.font);
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty()) return;

        // 将原始内容快照用于身份判断；绝不修改其他模组提供的 Component。
        List<Object> signature = new ArrayList<>();
        ItemStack snapshot = event.getItemStack().copy();
        signature.add(snapshot.getItem());
        signature.add(snapshot.getCount());
        signature.add(snapshot.getComponents());
        for (Either<FormattedText, TooltipComponent> element : elements) {
            element.ifLeft(text -> signature.add(TooltipTruncator.copy(text)));
            // 某些图像组件每帧重新创建，不能直接按对象引用判定切换。
            element.ifRight(image -> signature.add(image.getClass()));
        }

        int mouseX = (int) (mc.mouseHandler.xpos() * event.getScreenWidth() / mc.getWindow().getScreenWidth());
        int wrapWidth = Math.max(20, Math.min(event.getScreenWidth() - 16,
                Math.max(mouseX - 16, event.getScreenWidth() - mouseX - 16)));
        if (event.getMaxWidth() > 0) wrapWidth = Math.min(wrapWidth, event.getMaxWidth());

        boolean charLimited = ModConfig.ENABLE_CHAR_LIMIT.get();
        boolean doubleWidth = ModConfig.CHINESE_DOUBLE_WIDTH.get();
        int maxChars = ModConfig.MAX_CHARS_PER_LINE.get();
        List<Row> rows = new ArrayList<>();
        for (Either<FormattedText, TooltipComponent> element : elements) {
            if (element.left().isPresent()) {
                for (Component explicitLine : TooltipTruncator.splitExplicitLines(element.left().orElseThrow())) {
                    Component line = charLimited
                            ? TooltipTruncator.truncateCharacters(explicitLine, maxChars, doubleWidth) : explicitLine;
                    // 先按像素宽度换行，再分页；保持逻辑文本顺序，避免双重执行双向文本排版。
                    List<FormattedText> wrapped = font.getSplitter().splitLines(line, wrapWidth, Style.EMPTY);
                    if (wrapped.isEmpty()) rows.add(new Row(Component.empty()));
                    else for (FormattedText part : wrapped) rows.add(new Row(TooltipTruncator.copy(part)));
                }
            } else {
                if (rows.isEmpty()) rows.add(new Row(null));
                rows.getLast().images.add(element.right().orElseThrow());
            }
        }

        STATE.update(mc.screen, hoverTarget(mc.screen), List.copyOf(signature),
                rows.size(), ModConfig.MAX_LINES.get(), ModConfig.ENABLE_LINE_LIMIT.get());
        if (!ModConfig.ENABLE_SCROLL.get() && !ModConfig.ENABLE_PAGE_FLIP.get()) STATE.resetPosition();

        int start = STATE.getScrollOffset();
        int end = STATE.getVisibleEnd();
        int lastText = -1;
        for (int i = start; i < end; i++) if (rows.get(i).text != null) lastText = i;

        List<ClientTooltipComponent> visible = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Row row = rows.get(i);
            if (row.text != null) {
                Component text = row.text;
                if (i == lastText && STATE.hasHiddenAfter()) {
                    text = TooltipTruncator.appendLineMarker(text, charLimited, maxChars, doubleWidth);
                    // 追加三个点可能突破像素宽度；必要时继续按单词边界回退。
                    int markerBudget = charLimited ? maxChars : row.text.getString().length() * 2 + 3;
                    while (font.width(text) > wrapWidth && markerBudget > 3) {
                        text = TooltipTruncator.appendLineMarker(row.text, true, --markerBudget, doubleWidth);
                    }
                }
                visible.add(ClientTooltipComponent.create(text.getVisualOrderText()));
            }
            for (TooltipComponent image : row.images) visible.add(ClientTooltipComponent.create(image));
        }

        ClientTooltipComponent footer = null;
        if (STATE.canNavigate() && ModConfig.ENABLE_PAGE_FLIP.get() && ModConfig.SHOW_PAGE_INDICATOR.get()) {
            Component label = Component.translatable("tooltip.tooltiptruncatescroll.page",
                    STATE.getCurrentPage(), STATE.getTotalPages()).withStyle(ChatFormatting.GRAY);
            footer = ClientTooltipComponent.create(label.getVisualOrderText());
        }

        // GatherComponents 的列表允许替换。使用正式组件工厂保留原版背景、定位和渲染事件。
        elements.clear();
        elements.add(Either.right(new PageTooltip(List.copyOf(visible), footer)));
    }

    /** 将一页作为单个组件交还原版，防止原版再次换行后突破行数上限。 */
    private static final class PageClientTooltip implements ClientTooltipComponent {
        private final List<ClientTooltipComponent> body;
        private final ClientTooltipComponent footer;

        private PageClientTooltip(PageTooltip data) {
            body = data.body();
            footer = data.footer();
        }

        private int gapAfter(int index) {
            return index == 0 && body.size() > 1 ? 2 : 0;
        }

        @Override
        public int getHeight() {
            int height = 0;
            for (int i = 0; i < body.size(); i++) height += body.get(i).getHeight() + gapAfter(i);
            // 原版会给单组件 tooltip 的背景高度减 2；复合组件提前补偿。
            return height + (footer == null ? 0 : 4 + footer.getHeight()) + 2;
        }

        @Override
        public int getWidth(Font font) {
            int width = footer == null ? 0 : footer.getWidth(font);
            for (ClientTooltipComponent component : body) width = Math.max(width, component.getWidth(font));
            return width;
        }

        @Override
        public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
            int cursor = y;
            for (int i = 0; i < body.size(); i++) {
                ClientTooltipComponent component = body.get(i);
                component.renderText(font, x, cursor, matrix, buffers);
                cursor += component.getHeight() + gapAfter(i);
            }
            if (footer != null) footer.renderText(font, x, cursor + 4, matrix, buffers);
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            int cursor = y;
            for (int i = 0; i < body.size(); i++) {
                ClientTooltipComponent component = body.get(i);
                component.renderImage(font, x, cursor, graphics);
                cursor += component.getHeight() + gapAfter(i);
            }
            if (footer != null) footer.renderImage(font, x, cursor + 4, graphics);
        }
    }
}
