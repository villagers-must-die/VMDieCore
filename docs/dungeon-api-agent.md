# 地牢 API（Agent 版）

调用方是 agent 时看这份。每条都是规格：签名、取值、返回值、失败条件、副作用、可观察结果。人读版在 `docs/dungeon-api.md`，那份有解释和学习路径，这份没有。

依赖范围：只依赖 `org.dishanhai.vmdiecore.api` 与 `org.dishanhai.vmdiecore.api.event`。其余包是内部实现，签名会变。

---

## A. Java 调用

### A.1 门面：`VMDungeons`（final，全静态）

| 方法 | 参数约束 | 返回 | 为假/空的条件 |
| --- | --- | --- | --- |
| `create(MinecraftServer, ResourceLocation theme, int level)` | `level >= 1`；`theme` 已定义 | `OptionalInt`，地牢 id | 种类不存在、等级不存在、维度档不存在、被事件取消、达上限且无可淘汰、结构全拼失败 |
| `destroy(MinecraftServer, int id)` | `id` 为现有实例 | `boolean` | id 不存在、被事件取消 |
| `enter(ServerPlayer, int id)` | 无额外约束；重复调用会再传送一次并再计一次 | `boolean` | 见 A.6 失败条件 |
| `leave(ServerPlayer)` | — | `boolean` | 不在任何地牢、送回失败 |
| `handle(int id)` | — | `Optional<DungeonHandle>` | 实例不存在或维度不在内存 |
| `handles()` | — | `List<DungeonHandle>` | 空列表表示当前没有实例 |
| `themes()` | — | `Collection<DungeonTheme>` | — |
| `theme(ResourceLocation)` | — | `Optional<DungeonTheme>` | id 未定义 |
| `profile(ResourceLocation)` | — | `Optional<DungeonProfile>` | id 未定义 |
| `rule(ResourceLocation)` | — | `Optional<DungeonRule>` | id 未定义 |

`create` 与 `destroy` 的 `server` 参数当前都不参与逻辑，保留用于将来按 server 隔离；实际 server 由 `DungeonPool` 内部取。

`create` 成功只代表维度和结构就绪，玩家仍在原地；进门要再调 `enter`。

### A.2 数据记录

```java
record DungeonTheme(ResourceLocation id, String name, ResourceLocation icon,
                    Map<Integer, TierDefinition> tiers) {
    // Optional<TierDefinition> tier(int level)   无此等级返回空
    // List<TierDefinition> tierList()            按 level 升序
    // int lowestLevel()                          空种类返回 0
}

record TierDefinition(int level, String name, List<ResourceLocation> pools,
                      boolean hasExit, ResourceLocation profileId, ResourceLocation ruleId) {
    // ResourceLocation firstPool()               等价 pools.get(0)
}

record DungeonProfile(ResourceLocation id, ResourceLocation dimensionType, GeneratorSettings generator,
                      long fixedTime, Anchor anchor, Jigsaw jigsaw, int clearRadius, Landing landing) {
    record Anchor(int radius, int minY, int maxY) {}
    record Jigsaw(int depth, int landingOffset, boolean keepJigsaws) {}
    record Landing(int size, ResourceLocation floor) {}
}

record DungeonRule(ResourceLocation id, boolean requireExitPoint) {}

record DungeonHandle(int id, ResourceLocation theme, int tier,
                     ResourceKey<Level> dimension, BlockPos anchor, long seed) {}
```

约束：

- `tiers` 不可变副本，`level` 字段必须 > 0。
- `pools` 不可变副本，至少一项，顺序即回退顺序。
- `fixedTime` 负数表示不改时间。
- `clearRadius` 无调用点，写多少都不生效。
- `Jigsaw.depth` 小于 14 大概率拼不出结构。
- `Jigsaw.keepJigsaws` 建实例时应为 false。

`GeneratorSettings` 是 sealed，四种实现：

```java
new GeneratorSettings.Empty()
new GeneratorSettings.Flat(List<Layer> layers, ResourceLocation biome)
new GeneratorSettings.Layer(int height, ResourceLocation block)
new GeneratorSettings.Noise(ResourceLocation settings, JsonElement biomeSource)
new GeneratorSettings.Noise(ResourceLocation settings, ResourceLocation biomeSource)
```

`Empty` 才会拼结构；`Flat` / `Noise` 只出地形，`pools` 被忽略。

### A.3 返回值的补充信息

`DungeonInfo`（`id, theme, themeName, level, levelName, hasExit, visits`）只能从内部 `DungeonApi.list()` / `listFor(ServerPlayer)` 拿到，`VMDungeons` 没有暴露。要中文名或进入次数，用命令 `/vmdiecore dungeon list`，或自己拿 `handles()` 的 id 再拼。

### A.4 事件

| 类 | 父类 | 可取消 | 访问器 | 触发点 | 每次动作触发次数 |
| --- | --- | --- | --- | --- | --- |
| `DungeonCreateEvent` | `Event` | 是 | `server()` `theme()` `tier()` | 建维度之前 | 1 |
| `DungeonDestroyEvent` | `Event` | 是 | `server()` `dungeonId()` `dimension()` | 送玩家、卸区块之前 | 1 |
| `DungeonEnterEvent` | `Event` | 是 | `player()` `dungeonId()` | 传送之前 | 1 |
| `DungeonLeaveEvent` | `Event` | 否 | `player()` `dungeonId()` | 已送回起源大厅之后 | 1 |

注册定义（数据包加载完成后触发，同 id 覆盖数据包）：

```java
@SubscribeEvent
public static void onThemes(RegisterDungeonThemesEvent e) { e.register(new DungeonTheme(...)); }

@SubscribeEvent
public static void onProfiles(RegisterDungeonProfilesEvent e) { e.register(new DungeonProfile(...)); }

@SubscribeEvent
public static void onRules(RegisterDungeonRulesEvent e) { e.register(new DungeonRule(...)); }
```

`register` 可多次调用，`definitions()` 返回只读副本。

挂监听用 `@EventBusSubscriber(modid = "<你的modid>")`，这些事件走 `NeoForge.EVENT_BUS`。

`DungeonCreateEvent` 里拿不到维度，`server.getLevel(...)` 会是 null；要动结构等 `DungeonEnterEvent`。

### A.5 可直接抄的组合

```java
// 创建并进入
OptionalInt id = VMDungeons.create(server, ResourceLocation.fromNamespaceAndPath("example", "dungeon"), 3);
if (id.isPresent() && !VMDungeons.enter(player, id.getAsInt())) {
    LOGGER.warn("进入失败：{}", id.getAsInt());
}

// 遍历实例改方块
for (DungeonHandle handle : VMDungeons.handles()) {
    ServerLevel level = server.getLevel(handle.dimension());
    if (level == null) continue;
    level.setBlockAndUpdate(handle.anchor().above(), Blocks.TORCH.defaultBlockState());
}

// 拦掉高层级
@SubscribeEvent
public static void onCreate(DungeonCreateEvent event) {
    boolean allowed = false;   // 换成你的条件
    if (event.tier().level() >= 3 && !allowed) event.setCanceled(true);
}
```

### A.6 `enter` 的失败条件

按检查顺序，任一命中即返回 false 并打一条 warn：

1. 实例不存在 —— `进入失败：地牢 #N 不存在`
2. 种类或等级定义已删 —— `进入失败：地牢 #N 的定义已不存在`
3. 规则要求出口而等级声明无出口 —— `进入失败：地牢 #N 没有撤离点`（`rule` 定义缺失时跳过这一项）
4. 维度不在 `server.levels` —— `进入失败：地牢 #N 维度不在维度表`
5. 事件被取消 —— `进入地牢 #N 被取消`

成功路径的副作用：维度内除自己外没有其他玩家时把时间设成 `profile.fixedTime()`（负数则不动）；加载落脚点区块；传送到 `(x+0.5, y, z+0.5)`；该玩家对这张地牢的进入次数 +1。

---

## B. 数据包

三个目录，id = 命名空间 + 文件名：

```
data/<ns>/dungeon_theme/<name>.json
data/<ns>/dungeon_profile/<name>.json
data/<ns>/dungeon_rule/<name>.json
```

合并顺序：数据包 → 代码事件。模组不带任何内置定义，没注册过就是空表。

单个文件解析失败只打 warn 并跳过，其余照常加载。

### B.1 `dungeon_theme`

```json
{
  "name": "显示名",
  "icon": "minecraft:stone",
  "tiers": [
    {
      "level": 1,
      "name": "第 1 级",
      "pool": "minecraft:trial_chambers/chamber/end",
      "has_exit": true,
      "profile": "example:empty",
      "rule": "example:open"
    }
  ]
}
```

| 字段 | 类型 | 必填 | 缺省 | 取值 |
| --- | --- | --- | --- | --- |
| `name` | string | 否 | 文件路径 | 任意 |
| `icon` | id | 否 | `minecraft:stone` | 物品 id |
| `tiers` | array | 是 | — | 非空，重复 `level` 只留第一个 |
| `tiers[].level` | int | 是 | — | > 0 |
| `tiers[].name` | string | 否 | `第 N 级` | 任意 |
| `tiers[].pool` / `pools` | id 或 id[] | 是 | — | 二选一，非空，数组按序回退 |
| `tiers[].has_exit` | bool | 否 | `true` | — |
| `tiers[].profile` | id | 是 | — | 已定义的维度档 |
| `tiers[].rule` | id | 是 | — | 已定义的规则 |

### B.2 `dungeon_profile`

| 字段 | 类型 | 必填 | 缺省 | 取值 |
| --- | --- | --- | --- | --- |
| `dimension_type` | id | 是 | — | 已注册的维度类型，通常 `vmdiecore:dungeon` |
| `generator` | object | 是 | — | 见下 |
| `generator.type` | string | 是 | — | `empty` / `flat` / `noise` |
| `generator.layers` | array | `flat` 必填 | — | 非空，元素 `{height:int, block:id}` |
| `generator.biome` | id | 否 | `minecraft:plains` | 仅 `flat` |
| `generator.settings` | id | `noise` 必填 | — | `worldgen/noise_settings` |
| `generator.biome_source` | id 或 object | `noise` 必填 | — | 字符串先查 `worldgen/multi_noise_biome_source_parameter_list`（`minecraft:overworld` 等），查不到按 `worldgen/biome_source` 的 codec 名构造（仅无参 codec）；对象按 `worldgen/biome_source` 的 codec 解析，支持带参数形态 |
| `fixed_time` | long | 否 | `13000` | 负数 = 不改 |
| `anchor.radius` | int | 否 | `32` | 水平半径 |
| `anchor.min_y` / `max_y` | int | 否 | `-44` / `-26` | 竖直区间 |
| `jigsaw.depth` | int | 否 | `20` | 小于 14 易静默失败 |
| `jigsaw.landing_offset` | int | 否 | `8` | 锚点到平台的距离，平台在锚点下方 |
| `jigsaw.keep_jigsaws` | bool | 否 | `false` | 生成时保持 false |
| `clear_radius` | int | 否 | `48` | 无调用点 |
| `landing.size` | int | 否 | `3` | 平台边长 |
| `landing.floor` | id | 否 | `minecraft:stone` | 方块 id |

`empty` 走锚点随机 + 拼结构；`flat` / `noise` 走 (0,0) 地表算锚点，**不拼结构**。

### B.3 `dungeon_rule`

| 字段 | 类型 | 必填 | 缺省 |
| --- | --- | --- | --- |
| `require_exit_point` | bool | 否 | `true` |

### B.4 生效判定

```bash
/vmdiecore dungeon reload
```

回 `重载完成：种类 N 维度档 N 规则 N`。三个数字与期望值一致才算加载完整；偏小说明有文件解析失败，去日志搜 `解析失败`。

基线：模组不提供内置定义，只有数据包与注册事件的一律显示 `0 / 0 / 0`。期望值 = 数据包提供数 + 注册事件提供数，两者都算了才对。

---

## C. 可观察结果

调用后按日志判断状态，不必读代码：

| 日志 | 含义 |
| --- | --- |
| `创建地牢 #N <theme> 等级 L 锚点 x, y, z 种子 S` | create 成功 |
| `使用结构池 <pool>` | 池命中并拼装 |
| `结构池 <pool> 不存在` | 池 id 未注册，或命名空间/路径写错 |
| `结构池 <pool> 拼装失败` | 池存在但 `depth` 太小、起点埋太深或池为空 |
| `生成地牢 #N 失败` | 拼装异常，维度已回滚 |
| `创建失败：种类 X 未定义` | theme 未加载 |
| `创建失败：X 没有等级 N` | 该种类无此 level |
| `创建失败：维度档 X 不存在` | tier 引用的 profile 缺失 |
| `创建失败：地牢已达上限 N，没有可淘汰的` | 达 `dungeon.maxDungeons` |
| `销毁地牢 #N 被取消` | 事件监听拦下 |
| `销毁地牢 #N` | destroy 成功 |
| `进入地牢 #N 被取消` | 事件监听拦下 |
| `地牢种类 N 个` / `维度档 N 个` / `进入规则 N 个` | 每轮加载后的数量 |

命令回执同样可用：`已创建地牢 #id 锚点 ...`、`删除失败：地牢 #id 不存在`。

---

## D. 改这份 API 时的硬约束

1. `DimensionType` 必须来自注册表（`vmdiecore:dungeon`），不能用 `Holder.direct`，否则跨维度同步抛 `Can't find id for 'Direct{DimensionType[...]}'` 断连。
2. 改 `server.levels` 后必须 `markWorldsDirty()`，否则维度不进 tick 循环。
3. `DungeonLevel.getSeed()` 必须覆写，基类返回全局种子。
4. 停机路径不调 `level.close()`，会卡在保存世界。
5. 新增公开方法放 `VMDungeons`；`DungeonApi` 是内部实现，别让外部依赖它。
6. 记录加字段后，编译错误会把所有构造点列全（三个 `*Json`、`DungeonPool`、`DungeonStore`、`DungeonInstance`），照改即可。

对应原因见 `agents/learn.md`。

---

## E. 版本与稳定性

`VMDungeonApi.VERSION = "1.0.0"`。破坏性改动升主版本号。`api` 包与 `api.event` 包之外不承诺兼容。
