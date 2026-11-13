# VillagerMustDieCore (VMDieCore)

VillagerMustDieCore 是 Villager Must Die 整合包的核心模组。

- 模组 ID：`vmdiecore`
- Minecraft：1.21.1
- 模组加载器：NeoForge（开发版本 21.1.265）
- 开发环境：JDK 21、Gradle Wrapper

## 构建

在项目根目录运行 `./gradlew build`（Windows 也可运行 `gradlew.bat build`），生成的 JAR 位于 `build/libs/`。

当前项目仅包含基础模组入口，尚未实现整合包专属玩法。开发模板来自 [NeoForge 官方 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)，模板许可见 `TEMPLATE_LICENSE.txt`。
