# 验证记录

验证日期：2026-10-13。

## 完整构建

- Java：Microsoft OpenJDK 21.0.7。
- Gradle：8.12。
- ModDevGradle：2.0.80。
- NeoForge：21.1.172。
- Minecraft：1.21.1。
- 执行 Gradle `build` 成功，包含真实 Minecraft 与 NeoForge 依赖下的 `compileJava`、资源处理、普通 JAR 和源码 JAR 生成。
- 普通 JAR：`tooltiptruncatescroll-1.0.0.jar`。
- SHA-256：`e76c6745d4bb2a7f39f7d8c1d58caa3d67cd3cb15cf7ba853546003d9f332d77`。

## 独立逻辑检查

在 Java 21 下运行了 15 项状态管理断言，覆盖不足一整页的末页、小数滚轮刻度、边界钳制、跨帧位置保留、槽位切换、内容变化、滚动与翻页共存、关闭限制、无提示框时重置，以及屏幕切换。

使用完整构建生成的真实 Minecraft 编译类路径，另外运行了 14 项字符与格式断言，覆盖英文单词边界、过长单词、中文单宽/双宽、补充平面 Unicode、恰好达到上限、行省略号额度、显式换行与末尾空行、颜色和粗体继承、原始组件不变、旧式格式代码、页数计算。

两种语言文件均能解析为 JSON，全部 9 个配置 translation key 均存在。

以上是独立检查程序的结果；项目没有配置 JUnit 测试，因此 Gradle `test` 本身显示 `NO-SOURCE`。

## 尚未验证

没有启动 Minecraft 客户端进行人工视觉测试，也没有验证第三方 Tooltip 模组组合。
组合组件会保留图像与样式，但改变 `RenderTooltipEvent.Pre` 中组件列表的结构，依赖原生文本组件类型的第三方模组可能需要适配。
