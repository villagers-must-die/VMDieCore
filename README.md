# VillagerMustDieCore (VMDieCore)

VillagerMustDieCore 是 Villager Must Die 整合包的核心模组。

- 模组 ID：`vmdiecore`
- Minecraft：1.21.1
- 模组加载器：NeoForge（开发版本 21.1.266）
- 开发环境：JDK 21、Gradle Wrapper
- 必需前置：[铁砧工艺（AnvilCraft）](https://modrinth.com/mod/anvilcraft)，最低 1.5.0；本次构建与运行使用整合包中的 1.6.0+rc.1，需要独立安装。

- 必需前置：[LDLib2](https://github.com/Low-Drag-MC/LDLib2)，最低 2.2.39.a；[Multiblocked2](https://github.com/Low-Drag-MC/Multiblocked2)，最低 21.1.1。两者均需独立安装；KilaGraph 由该版本 MBD2 自带。

- 0.2.0 已整合怪物视角 1.0.2 的五族选择、怪物模型、搜打撤风格身份 UI 和种族科技基础实现。**种族科技尚未完成：配方种族限制仅覆盖原版合成流程，其他模组可绕过。** Curios 9.5.1+、Modular Golems 3.1.44+ 及其 L2Library 前置用于相应科技联动；未安装时对应联动不启用。

## 构建

Windows 下可双击 `gradlew.bat` 或 `build.bat` 构建，完成后按任意键关闭窗口。也可以在项目根目录运行 `./gradlew build`（Windows 为 `gradlew.bat build`）；生成的 JAR 位于 `build/libs/`。

本仓库克隆在整合包版本目录内时，可以直接采用上级 `mods/` 的依赖版本：

```powershell
.\gradlew.bat build '-PpackModsDir=../mods'
.\gradlew.bat runGameTestServer '-PpackModsDir=../mods'
```

默认构建会下载固定版本的依赖，无需依赖本机绝对路径。开发测试使用隔离的 `run-test/`、`run-client/` 目录，正式 JAR 不包含测试代码或第三方模组 JAR。

## 怪物视角与种族科技

按 **O** 或输入 `/monster` 打开作战身份界面。可选僵尸、骷髅、末影人、节肢生物、血族；血族保留玩家自身模型和皮肤。沿用大字号、军绿/沙色/警戒橙的搜打撤风格 UI。

种族科技包含肉傀儡替身、灵魂瓶和骨傀儡复活、三类传送物品、八种附魔蛛丝布与毒液。血族共享四系科技。已保留最新的燃烧兼容修复、创造飞行豁免，以及“开始喝牛奶时满血才产骨”的条件。

**种族科技属于未完成部分。** 当前只在原版背包合成与工作台流程检查配方种族权限，其他模组的工作台、自动合成或机器流程可绕过该检查，跨模组的科技解锁限制尚未完成。物品、配方及联动基础已移入核心，后续仍需补齐统一权限与跨模组回归。

新增服务端 `VMDieTechnology.getRace(player)`、`VMDieTechnology.hasTech(player, 'enderman')` 绑定，供 KubeJS 查询种族与科技资格；这些接口只提供查询，不会自动拦截其他模组的配方。

详见 [使用说明](docs/怪物视角使用说明.md)、[移植与科技评估](docs/怪物视角移植与科技评估.md)、[验证记录](docs/移植验证记录.md)。

升级时以 `vmdiecore-0.2.0.jar` 替换旧核心和独立的 `monster-perspective-*.jar`，两者不可并装。原 `monsterperspective:*` 注册 ID、玩家种族数据及 `monsterperspective-server.toml` 均保留。

## 初始维度

`vmdiecore:origin` 使用基岩上下边界，其间由石头填充。关闭地形装饰和普通结构，并限制自然生物生成。玩家首次登录时会进入该维度的初始出生室；出生室采用基岩边界、石头地面、空气空间和火把，只在世界初始化时生成一次。此后玩家可以自行设置重生点。

将模组加入已有存档时，尚未由本模组初始化的玩家首次登录也会被传送到出生室。开发模板来自 [NeoForge 官方 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)，模板许可见 `TEMPLATE_LICENSE.txt`。

## 按物品设置堆叠上限

安装 KubeJS 后，在 `kubejs/startup_scripts/` 中通过 `VMDieStacking.setLimit(物品 ID, 上限)` 标记物品。桥接已在 KubeJS NeoForge `2101.7.2-build.381` 验证。上限可设为 `1–9999`，可用于脚本注册的物品、原版物品和其他模组物品。每个 ID 单独设置。

```js
StartupEvents.registry('item', event => {
    const token = event.create('villager_token')
    VMDieStacking.setLimit(token.id, 1024)
})

VMDieStacking.setLimit('minecraft:ender_pearl', 512)
```

修改后重启游戏。多人游戏需要在服务端和客户端使用相同的启动脚本。带耐久组件的物品、带独立堆叠上限组件的物品栈以及低于原版通用上限的特殊槽位沿用各自的限制。降低或移除上限前，先拆分已有的大堆叠。容器类模组可能另有容量限制。

## 金属脚手架碰撞

在 Minecraft 1.21.1 / NeoForge 21.1.x 整合包中，VMDieCore 只为已由 KubeJS 注册的
`kubejs:metal_scaffolding` 补充实体碰撞：从上方落下可站在顶面，从侧面或下方可穿入。
保留整合包的 `minecraft:climbable` 标签、完整选中轮廓、`bottom` 外观状态和原放置脚本。
请在客户端和服务端安装同一版 VMDieCore，不另装独立碰撞补丁。未安装 KubeJS 或未注册该方块时无效果。

本功能不接管放置，因此不包含取消放置后的残影同步修复。
要运行脚手架集成 GameTest，将 KubeJS NeoForge `2101.7.2-build.381` 和兼容的 Rhino
JAR 放在同一目录，执行 `./gradlew runGameTestServer -PintegrationModsDir=/path/to/mods`。
该测试使用隔离的 `run-test/` 脚本，不覆盖实际整合包。
