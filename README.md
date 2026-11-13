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
