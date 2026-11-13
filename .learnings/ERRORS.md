# 构建脚本入口

- 状态：resolved（2026-11-13）
- 问题：仅给 `build.bat` 添加暂停，直接双击 `gradlew.bat` 仍会退出。
- 处理：无参数运行 `gradlew.bat` 时转入交互式构建，完成后等待按键。

## AnvilCraft 发布包的 Minecraft 版本范围

- 状态：pending（2026-11-13）
- Modrinth 标注 NeoForge 1.21.1 的 AnvilCraft 1.5.0、1.5.5 和 1.4.10 包，其内部 `neoforge.mods.toml` 的 Minecraft 范围为 `[1.21,1.21.1)`；不能仅凭平台标签断言能在 1.21.1 加载。
