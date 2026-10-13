package com.truncate.tooltip;

import com.truncate.tooltip.config.ModConfig;
import com.truncate.tooltip.event.TooltipRenderHandler;
import com.truncate.tooltip.keybind.ModKeyMappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

/** 客户端入口；专用服务器不会实例化这个类。 */
@Mod(value = TooltipTruncateScroll.MODID, dist = Dist.CLIENT)
public final class TooltipTruncateScroll {
    public static final String MODID = "tooltiptruncatescroll";

    public TooltipTruncateScroll(IEventBus modBus, ModContainer container) {
        container.registerConfig(Type.CLIENT, ModConfig.SPEC);

        // 这两个注册事件属于 MOD 总线，不能交给 GAME 总线订阅器。
        modBus.addListener(ModKeyMappings::register);
        modBus.addListener(TooltipRenderHandler::registerTooltipFactories);
    }
}
