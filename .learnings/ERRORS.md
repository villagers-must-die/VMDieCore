# 构建脚本入口

- 状态：resolved（2026-11-13）
- 问题：仅给 `build.bat` 添加暂停，直接双击 `gradlew.bat` 仍会退出。
- 处理：无参数运行 `gradlew.bat` 时转入交互式构建，完成后等待按键。

## AnvilCraft 发布包的 Minecraft 版本范围

- 状态：pending（2026-11-13）
- Modrinth 标注 NeoForge 1.21.1 的 AnvilCraft 1.5.0、1.5.5 和 1.4.10 包，其内部 `neoforge.mods.toml` 的 Minecraft 范围为 `[1.21,1.21.1)`；不能仅凭平台标签断言能在 1.21.1 加载。

## 1.21.1 大堆叠的数量限制

- 状态：resolved（2026-11-15）
- 超过 99 件的堆叠需要同时处理 `ItemStack` 的存档数量校验、容器和槽位的通用上限，以及掉落物合并的 64 件上限。KubeJS 脚本用 `SimpleContainer` 验证时，可直接传入一个 `ItemStack`；只传 JS 数字可能被 Rhino 匹配到 `ItemStack...` 构造器。

## Mixin 配置文件名

- 状态：resolved（2026-11-15）
- VMDieCore 的 NeoForge Mixin 配置使用 `vmdiecore.neoforge.mixin.json`，并在 `neoforge.mods.toml` 中引用同名文件。
