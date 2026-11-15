# VillagerMustDieCore (VMDieCore)

VillagerMustDieCore 是 Villager Must Die 整合包的核心模组。

- 模组 ID：`vmdiecore`
- Minecraft：1.21.1
- 模组加载器：NeoForge（开发版本 21.1.265）
- 开发环境：JDK 21、Gradle Wrapper
- 必需前置：[铁砧工艺（AnvilCraft）](https://modrinth.com/mod/anvilcraft)，NeoForge 1.21.1 版本，最低 1.5.0；需要独立安装。注意：Modrinth 的 1.5.0 发布包内部 Minecraft 版本范围声明与 1.21.1 不符，尚需确认可用发布包后才能进行运行验证。

- 必需前置：[LDLib2](https://github.com/Low-Drag-MC/LDLib2)，最低 2.2.39.a；[Multiblocked2](https://github.com/Low-Drag-MC/Multiblocked2)，最低 21.1.1。两者均需独立安装；KilaGraph 由该版本 MBD2 自带。

## 构建

Windows 下可双击 `gradlew.bat` 或 `build.bat` 构建，完成后按任意键关闭窗口。也可以在项目根目录运行 `./gradlew build`（Windows 为 `gradlew.bat build`）；生成的 JAR 位于 `build/libs/`。

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
