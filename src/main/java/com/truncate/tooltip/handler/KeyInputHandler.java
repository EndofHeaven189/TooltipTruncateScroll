package com.truncate.tooltip.handler;

import com.mojang.blaze3d.platform.InputConstants;
import com.truncate.tooltip.TooltipTruncateScroll;
import com.truncate.tooltip.config.ModConfig;
import com.truncate.tooltip.event.TooltipRenderHandler;
import com.truncate.tooltip.keybind.ModKeyMappings;
import com.truncate.tooltip.state.TooltipStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME,
        modid = TooltipTruncateScroll.MODID, value = Dist.CLIENT)
public final class KeyInputHandler {
    private KeyInputHandler() {}

    /** 库存界面的按键必须从 ScreenEvent 处理，才能阻止界面自身同时翻页。 */
    @SubscribeEvent
    public static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (handle(event.getScreen(), event.getKeyCode(), event.getScanCode())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return;
        // 有 Screen 时上面的监听器负责处理，避免一次按键翻两页。
        if (Minecraft.getInstance().screen != null) return;
        handle(null, event.getKey(), event.getScanCode());
    }

    private static boolean handle(Screen screen, int keyCode, int scanCode) {
        if (!ModConfig.ENABLE_PAGE_FLIP.get() || ModKeyMappings.PAGE_UP == null) return false;
        if (!TooltipRenderHandler.hasNavigableTooltip(screen)) return false;
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        TooltipStateManager state = TooltipStateManager.getInstance();
        if (ModKeyMappings.PAGE_UP.isActiveAndMatches(key)) {
            state.flipPage(-1);
            return true;
        }
        if (ModKeyMappings.PAGE_DOWN.isActiveAndMatches(key)) {
            state.flipPage(1);
            return true;
        }
        return false;
    }
}
