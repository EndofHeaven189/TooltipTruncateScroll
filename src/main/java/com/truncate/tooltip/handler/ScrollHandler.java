package com.truncate.tooltip.handler;

import com.truncate.tooltip.TooltipTruncateScroll;
import com.truncate.tooltip.config.ModConfig;
import com.truncate.tooltip.event.TooltipRenderHandler;
import com.truncate.tooltip.state.TooltipStateManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME,
        modid = TooltipTruncateScroll.MODID, value = Dist.CLIENT)
public final class ScrollHandler {
    private ScrollHandler() {}

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!ModConfig.ENABLE_SCROLL.get() || event.getScrollDeltaY() == 0) return;
        if (!TooltipRenderHandler.hasNavigableTooltip(event.getScreen())) return;
        TooltipStateManager.getInstance().scroll(event.getScrollDeltaY(), ModConfig.SCROLL_SPEED.get());
        // 即使到达边界也消费滚轮，防止同时滚动背后的物品列表。
        event.setCanceled(true);
    }
}
