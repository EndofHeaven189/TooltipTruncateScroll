# TooltipTruncateScroll

Minecraft 1.21.1 / NeoForge 21.1.172 / Java 21，仅客户端。

## 构建与运行

将 JAVA_HOME 指向 JDK 21，在项目目录执行：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Linux/macOS 使用 `./gradlew build`。构建产物位于 `build/libs/tooltiptruncatescroll-1.0.0.jar`。
将普通 JAR 放入客户端的 mods 目录，不要安装 `-sources.jar`。

## 使用

- 默认显示最多 8 行正文，每行最多 60 个字符宽度；正文之外显示页码。
- 在游戏控制设置的“提示框截断与滚动”分类修改 PageUp / PageDown。
- 客户端配置文件为 `config/tooltiptruncatescroll-client.toml`。
- 将 `enableScroll` 改为 `true` 后，鼠标滚轮向上查看前面的行、向下查看后面的行。
- `scrollSpeed` 为每个滚轮刻度移动的行数，范围 1–20。触控板的小数刻度会累积。
- 鼠标离开、切换槽位、切换屏幕或原始提示内容变化时重置位置。

## 边界规则

1. `enableLineLimit=false` 时显示全部行，滚动和翻页暂停；字符限制仍独立生效。
2. `maxLines` 只计算正文行，页码另外显示。非文本组件跟随前面的正文行；单独的图像占一个逻辑行。
3. 字符限制按显式换行后的逻辑文本执行，然后按屏幕像素宽度换行，再计算可见行和页码。
4. `…` 和 `...` 都计入字符宽度上限。英文字母、数字及常见单词连接符不会从中间截断。
5. 一个完整单词比可用字符额度更长时，整体隐藏该单词并显示省略号。
6. 双倍宽度选项针对汉字、中文标点、全角字符；颜色代码、组合音标不额外占宽度。
7. 字符上限隐藏的字符不会通过翻页恢复；滚动和翻页查看的是被行数上限隐藏的正文行。
8. 滚轮可以停在页内任意行；页码按 `offset / maxLines + 1` 计算。翻页跳到相邻页起始行。
9. 最后一页允许少于一整页。最大偏移为最后一页的起始行，避免出现永远到不了末页的问题。
10. 图像组件的原有像素高度保持不变，行数限制不是整个提示框的像素高度限制。

## NeoForge 1.21.1 API 更正

- 此版本的滚轮事件名为 `ScreenEvent.MouseScrolled.Pre`，不是 `MouseScrollEvent.Pre`。
- 按键注册事件 `RegisterKeyMappingsEvent` 属于 MOD 总线。主类向 MOD 总线直接添加监听器，运行时处理类统一使用 CLIENT / GAME 的 `EventBusSubscriber`。
- 库存里的键盘输入使用可取消的 `ScreenEvent.KeyPressed.Pre`；同时保留 `InputEvent.Key` 入口，并防止重复处理。
- `RenderTooltipEvent.Pre#getComponents()` 返回不可修改列表。内容替换放在 `GatherComponents`，并通过 `RegisterClientTooltipComponentFactoriesEvent` 注册组合组件。
- 本实现不使用访问转换器、Mixin、反射或专用服务器入口。

## 兼容范围

适用于进入 NeoForge `RenderTooltipEvent.GatherComponents` 管线的物品和组件提示框。
其他模组自行绘制、绕过这一管线的提示框不会被本模组拦截。
其他模组若直接识别 `Pre` 事件列表中的原生文本组件类型，可能需要适配本模组的组合组件。
使用不同字体、不同宽度或同时绘制多个独立提示框的第三方界面，建议另行做游戏内兼容测试。

本项目的源码和构建文件均为 UTF-8。
