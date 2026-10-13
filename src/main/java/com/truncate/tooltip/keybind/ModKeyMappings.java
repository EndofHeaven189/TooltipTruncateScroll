package com.truncate.tooltip.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static KeyMapping PAGE_UP;
    public static KeyMapping PAGE_DOWN;
    private static final String CATEGORY = "key.categories.tooltiptruncatescroll";

    private ModKeyMappings() {}

    /** 由主类注册到 MOD 总线，在客户端按键注册生命周期中创建和注册。 */
    public static void register(RegisterKeyMappingsEvent event) {
        PAGE_UP = new KeyMapping("key.tooltiptruncatescroll.page_up", KeyConflictContext.GUI,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP, CATEGORY);
        PAGE_DOWN = new KeyMapping("key.tooltiptruncatescroll.page_down", KeyConflictContext.GUI,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN, CATEGORY);
        event.register(PAGE_UP);
        event.register(PAGE_DOWN);
    }
}
