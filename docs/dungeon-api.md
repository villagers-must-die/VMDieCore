# 地牢接口参考手册

面向两类读者：写数据包的人（不碰 Java）和写 Java 的人（要在地牢前后插逻辑）。内部实现备忘在最后一节，看代码之前建议先读第一节。想直接动手的先看 `docs/dungeon-api-tutorial.md`。

接口版本 `1.0.0`，对应 `org.dishanhai.vmdiecore.api.VMDungeonApi.VERSION`。

---

## 1. 概念模型

四个名词，互相不耦合：

| 名词 | 是什么 | 谁定义 |
| --- | --- | --- |
| 种类（theme） | 一组按等级排列的关卡，比如「墓穴」 | `dungeon_theme` JSON |
| 等级（tier） | 种类里的某一级，指定结构池、有没有撤离点、用哪个维度档 | `dungeon_theme` JSON 的 `tiers` 数组 |
| 维度档（profile） | 一个地牢实例所在维度长什么样：地形生成器、固定时间、锚点范围、拼图深度、落脚平台 | `dungeon_profile` JSON |
| 规则（rule） | 进入前的准入判断，目前只有「必须存在撤离点」 | `dungeon_rule` JSON |

实例（dungeon）是「种类 + 等级」跑起来之后的东西：一个独立维度、一个 id、一次结构拼装。同一份种类等级可以同时存在多个实例，id 不同、维度不同。

关系：

```
种类 example:dungeon
└── 等级 1 前厅   pools=[trial_chambers/chamber/end]  has_exit=true   profile=example:empty  rule=example:open
└── 等级 2 内室   pools=[trial_chambers/chamber/slanted] ...
└── 等级 3 核心   pools=[...]  profile=example:noise
```

等级里写的是「结构池」（structure template pool），不是单个结构。池里有很多模板，实例创建时随机挑一个。

---

## 2. 数据包

### 2.1 目录与加载顺序

```
data/<命名空间>/dungeon_theme/<名字>.json      → id = <命名空间>:<名字>
data/<命名空间>/dungeon_profile/<名字>.json
data/<命名空间>/dungeon_rule/<名字>.json
```

加载顺序固定两层，后者覆盖前者，覆盖用的是完整 id：

1. 数据包里的 JSON
2. 其他模组在注册事件里塞进来的定义

模组本身不带任何内置定义，没注册过就是空的，`create` 会直接报「未定义」。

`/reload` 和三份 reload listener 都会重跑整个流程，改完 JSON 不需要重启。手头没有数据包工具时用 `/vmdiecore dungeon reload` 强制重扫一遍，它会打印三类定义的数量。

单个文件解析失败只打 warn 并跳过该文件，不影响其他定义。

### 2.2 `dungeon_theme`

```json
{
  "name": "平层大厅",
  "icon": "minecraft:oak_planks",
  "tiers": [
    {
      "level": 1,
      "name": "浅层平房",
      "pool": "minecraft:trial_chambers/chamber/slanted",
      "has_exit": true,
      "profile": "example:room",
      "rule": "example:open"
    }
  ]
}
```

顶层字段：

| 字段 | 类型 | 必填 | 缺省 | 说明 |
| --- | --- | --- | --- | --- |
| `name` | 字符串 | 否 | 文件路径 | 界面上显示的名字 |
| `icon` | 资源位置 | 否 | `minecraft:stone` | 界面图标物品 |
| `tiers` | 数组 | 是 | — | 至少一项，不能为空 |

`tiers` 每一项：

| 字段 | 类型 | 必填 | 缺省 | 说明 |
| --- | --- | --- | --- | --- |
| `level` | 整数 | 是 | — | 必须大于 0，同一文件内重复的等级只保留第一个 |
| `name` | 字符串 | 否 | `第 N 级` | 等级显示名 |
| `pool` 或 `pools` | 字符串或字符串数组 | 是 | — | 二选一，不能都空。数组按顺序尝试，第一个能拼出来的生效 |
| `has_exit` | 布尔 | 否 | `true` | 这个等级有没有撤离点 |
| `profile` | 资源位置 | 是 | — | 用哪个维度档 |
| `rule` | 资源位置 | 是 | — | 进入前用哪条规则 |

`has_exit` 是「结构里有没有出口」的声明，规则才决定这个声明要不要强制执行。默认规则要求有出口，所以 `has_exit: false` 的等级默认进不去；要放行就把 `rule` 换成 `require_exit_point: false` 的规则。

### 2.3 `dungeon_profile`

```json
{
  "dimension_type": "vmdiecore:dungeon",
  "generator": {
    "type": "flat",
    "biome": "minecraft:plains",
    "layers": [
      { "height": 1, "block": "minecraft:bedrock" },
      { "height": 120, "block": "minecraft:stone" },
      { "height": 1, "block": "minecraft:grass_block" }
    ]
  },
  "fixed_time": 6000,
  "jigsaw": { "depth": 4, "landing_offset": 4, "keep_jigsaws": false },
  "clear_radius": 32,
  "landing": { "size": 5, "floor": "minecraft:oak_planks" }
}
```

| 字段 | 类型 | 必填 | 缺省 | 说明 |
| --- | --- | --- | --- | --- |
| `dimension_type` | 资源位置 | 是 | — | 必须指向已注册的维度类型，通常 `vmdiecore:dungeon` |
| `generator` | 对象 | 是 | — | 地形生成器，见下 |
| `fixed_time` | 长整数 | 否 | `13000` | 区域内没有其他玩家时把维度时间设成这个值；负数表示不改时间 |
| `anchor` | 对象 | 否 | `{radius:32,min_y:-44,max_y:-26}` | 结构锚点在水平半径和竖直区间内随机取 |
| `jigsaw` | 对象 | 否 | `{depth:20,landing_offset:8,keep_jigsaws:false}` | 拼图参数，见下 |
| `clear_radius` | 整数 | 否 | `48` | 保留字段，当前版本不清方块，填多少都不影响 |
| `landing` | 对象 | 否 | `{size:3,floor:minecraft:stone}` | 玩家落脚平台，见下 |

`generator.type` 三选一：

| type | 需要的字段 | 效果 |
| --- | --- | --- |
| `empty` | 无 | 全空世界，只有结构本身 |
| `flat` | `layers`（必填，非空）、`biome`（缺省 `minecraft:plains`） | 超平坦，`layers` 从 y 最低往上叠 |
| `noise` | `settings`、`biome_source`（都必填，无缺省） | 原版噪音地形。`settings` 指向 `worldgen/noise_settings`；`biome_source` 写字符串时先按 `worldgen/multi_noise_biome_source_parameter_list` 查（`minecraft:overworld` 这类预设），查不到再按 `worldgen/biome_source` 的 codec 名构造；写对象时直接按 `worldgen/biome_source` 的 codec 解析，带参数的写法靠它 |

**只有 `empty` 会拼结构。** `flat` 和 `noise` 走的是「先生成地形，再在 (0, 地表高度 + landing_offset, 0) 放锚点」这条路，等级里写的结构池完全不参与拼装。要地形又要建筑，目前得改代码或者在结构模板里自带地形。

`flat.layers` 每项是 `{height, block}`：`height` 是层数，`block` 是方块 id。想复刻出生维度就把 bedrock / stone / bedrock 三层写进去。

`noise.biome_source` 两种写法：

```json
"biome_source": "minecraft:overworld"
```

```json
"biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" }
```

字符串先查预设表，所以 `minecraft:overworld` 直接能用；预设表里没有的，才按 `worldgen/biome_source` 的 codec 名构造，这条路只对不需要参数的 codec 成立（如 `minecraft:the_end`）。要带参数就用对象形式，字段照抄原版 `worldgen/biome_source`。

`jigsaw.depth` 是拼图递归深度。**太小会静默失败**：`generateJigsaw` 什么都不返回，日志里也没有报错，看起来像结构池不存在。实战里 4 只能拼很小的房间，`chamber/slanted` 这类要 14 以上。

`jigsaw.landing_offset` 是锚点到落脚平台的距离，平台在锚点下方这么多格。

`jigsaw.keep_jigsaws` 为 true 时拼图方块原样留在世界里。只有调试拼图布局才开；平时开着玩家会看到一堆带箭头的 `minecraft:jigsaw`。堡垒遗迹、试炼密室的模板本身就是拼图方块拼的，关掉才会被替换成实际方块。

`landing.size` 是平台边长（奇数），`landing.floor` 是平台地板方块。平台铺在锚点下方，玩家进入时落在平台中心。

### 2.4 `dungeon_rule`

```json
{
  "require_exit_point": false
}
```

| 字段 | 类型 | 必填 | 缺省 | 说明 |
| --- | --- | --- | --- | --- |
| `require_exit_point` | 布尔 | 否 | `true` | 为 true 时，`has_exit: false` 的等级不允许进入 |

### 2.5 没有任何内置定义

模组只提供机制，不附带种类、维度档和规则。装完后 `/vmdiecore dungeon reload` 显示「种类 0 维度档 0 规则 0」是正常的，要么写数据包，要么在 `RegisterDungeonThemesEvent` / `RegisterDungeonProfilesEvent` / `RegisterDungeonRulesEvent` 里注册。

三者靠 id 相互引用：等级的 `profile` 和 `rule` 必须指向已经存在的定义，名字写错要到创建时才会暴露。

---

## 3. Java 接口

入口只有 `org.dishanhai.vmdiecore.api.VMDungeons`，全静态方法，不返回 `ServerLevel`，维度只以 `ResourceKey` 和 `BlockPos` 暴露。

### 3.1 方法

| 方法 | 返回 | 说明 |
| --- | --- | --- |
| `create(MinecraftServer server, ResourceLocation theme, int level)` | `OptionalInt` | 创建实例，成功返回地牢 id。种类或等级不存在时返回空并打 warn |
| `destroy(MinecraftServer server, int id)` | `boolean` | 删除实例：送走玩家、卸载区块、移除维度、删目录。id 不存在返回 false |
| `enter(ServerPlayer player, int id)` | `boolean` | 把玩家传进地牢。任何一个校验不过都返回 false，原因看日志 |
| `leave(ServerPlayer player)` | `boolean` | 把玩家送回起源大厅。玩家不在任何地牢里返回 false |
| `handle(int id)` | `Optional<DungeonHandle>` | 拿实例的维度 key、锚点、种子。对外提供，模组内部目前没有调用方 |
| `handles()` | `List<DungeonHandle>` | 当前所有在内存里的实例 |
| `themes()` | `Collection<DungeonTheme>` | 所有种类，含数据包与代码注册的 |
| `theme(ResourceLocation)` | `Optional<DungeonTheme>` | 按 id 查种类 |
| `profile(ResourceLocation)` | `Optional<DungeonProfile>` | 按 id 查维度档 |
| `rule(ResourceLocation)` | `Optional<DungeonRule>` | 按 id 查规则 |

`server` 参数目前不参与逻辑，留着是为了后续按 server 隔离定义时不改签名。

### 3.2 返回类型

```java
record DungeonHandle(int id, ResourceLocation theme, int tier,
                     ResourceKey<Level> dimension, BlockPos anchor, long seed)

record DungeonTheme(ResourceLocation id, String name, ResourceLocation icon,
                    Map<Integer, TierDefinition> tiers)

record TierDefinition(int level, String name, List<ResourceLocation> pools,
                      boolean hasExit, ResourceLocation profileId, ResourceLocation ruleId)

record DungeonProfile(ResourceLocation id, ResourceLocation dimensionType, GeneratorSettings generator,
                      long fixedTime, Anchor anchor, Jigsaw jigsaw, int clearRadius, Landing landing)

record DungeonRule(ResourceLocation id, boolean requireExitPoint)
```

`DungeonTheme` 上还有三个便捷方法：`tier(int)` 返回 `Optional<TierDefinition>`、`tierList()` 按等级升序返回列表、`lowestLevel()` 返回最小等级（空种类返回 0）。`TierDefinition.firstPool()` 返回池列表第一项。

`GeneratorSettings` 是 sealed 接口，四种实现：

```java
new GeneratorSettings.Empty()
new GeneratorSettings.Flat(List<Layer> layers, ResourceLocation biome)
new GeneratorSettings.Layer(int height, ResourceLocation block)
new GeneratorSettings.Noise(ResourceLocation settings, JsonElement biomeSource)
new GeneratorSettings.Noise(ResourceLocation settings, ResourceLocation biomeSource)
```

第二个是便捷构造，等价于把 id 写成字符串。

`DungeonProfile` 里三个嵌套 record：`Anchor(int radius, int minY, int maxY)`、`Jigsaw(int depth, int landingOffset, boolean keepJigsaws)`、`Landing(int size, ResourceLocation floor)`。

### 3.3 事件

创建、删除、进入三个可以取消（`ICancellableEvent`），取消后动作不发生。

| 事件 | 时机 | 可取消 | 能拿到 |
| --- | --- | --- | --- |
| `DungeonCreateEvent` | 建维度、拼结构之前 | 是 | `server()`、`theme()`、`tier()` |
| `DungeonDestroyEvent` | 送玩家、卸区块之前 | 是 | `server()`、`dungeonId()`、`dimension()` |
| `DungeonEnterEvent` | 传送到落脚点之前 | 是 | `player()`、`dungeonId()` |
| `DungeonLeaveEvent` | 已经送回起源大厅之后 | 否 | `player()`、`dungeonId()` |

注册定义用另外三个，都在数据包加载完之后触发：

```java
@SubscribeEvent
public static void onThemes(RegisterDungeonThemesEvent event) {
    event.register(new DungeonTheme(...));
}

@SubscribeEvent
public static void onProfiles(RegisterDungeonProfilesEvent event) { ... }

@SubscribeEvent
public static void onRules(RegisterDungeonRulesEvent event) { ... }
```

`register` 可以调用多次，`definitions()` 返回全部已注册项。注册事件在数据包之后执行，所以代码注册同 id 会覆盖数据包。

### 3.4 典型用法

```java
// 在玩家完成某件事后开一个 3 级地牢
OptionalInt id = VMDungeons.create(server, ResourceLocation.fromNamespaceAndPath("example", "dungeon"), 3);
id.ifPresent(value -> VMDungeons.enter(player, value));

// 找当前所有地牢，给每个加一个提示牌
for (DungeonHandle handle : VMDungeons.handles()) {
    ServerLevel level = server.getLevel(handle.dimension());
    if (level != null) {
        level.setBlockAndUpdate(handle.anchor().above(), Blocks.TORCH.defaultBlockState());
    }
}
```

不要在 `DungeonCreateEvent` 里对维度动手，那时维度还没建。要改结构等 `DungeonEnterEvent` 或者自己监听实例创建完成后的时机。

---

## 4. 运行时行为

### 4.1 创建

1. 查种类、查等级，任一缺失失败。
2. 查等级引用的维度档，缺失失败。
3. 发 `DungeonCreateEvent`，取消则失败。
4. 检查是否超过 `dungeon.maxDungeons`，超了先淘汰最早创建的一个；淘汰不掉就失败。
5. 分配 id，生成加盐的维度 key `vmdiecore:dungeon_<id>_<hex>`，按维度档造 `ServerLevel`。
6. 把维度放进 `server.levels`，调 `markWorldsDirty()`，发 `LevelEvent.Load`。
7. 拼装，分两条路：
   - `empty` 生成器：在锚点区间随机取锚点、强加载区块，按池列表顺序尝试 `JigsawPlacement.generateJigsaw`，第一个返回 true 的生效。全部失败就抛异常。
   - `flat` / `noise` 生成器：加载 (0,0) 区块，取地表高度加上 `landing_offset` 当锚点，强加载。**不拼结构。**
8. 在锚点下方的落脚点铺平台；`fixed_time` 非负时把维度时间设成它。
9. 登记到 `DungeonPool`，写 `DungeonStore`（SavedData `vmdiecore_dungeons`），重启后靠它恢复。

第 7 步失败会回滚：撤掉维度、写 `生成地牢 #N 失败` 的 error，`create` 返回空。日志里「结构池 X 不存在」和「结构池 X 拼装失败」是两种不同原因，后者通常意味着 `depth` 太小或模板池本身是空的。

### 4.2 删除

1. 发 `DungeonDestroyEvent`，取消则失败。
2. 维度里的玩家送回起源大厅。
3. 卸载区块、从 `server.levels` 移除、发卸载事件。
4. 延后一个 tick 关存储并删目录，避免在主线程里等区块任务。

### 4.3 进入

1. 实例、种类、等级任一不存在，失败。
2. 规则的 `requireExitPoint` 为 true 而等级的 `hasExit` 为 false，失败。
3. 维度不在维度表里，失败。
4. 维度内没有其他玩家时，把时间设成维度档的 `fixedTime`（负数则保持不动）。
5. 发 `DungeonEnterEvent`，取消则失败。
6. 加载落脚点所在区块，把玩家传送到平台中心（`x+0.5, y, z+0.5`）。
7. 给这位玩家的进入次数加一。

进入次数存在 `VisitData` 里，只做计数，不做别的。之前有一个「次数到 N 就清场」的机制，已经去掉了。

### 4.4 离开

1. 用玩家当前维度反查地牢 id，不在任何地牢里则失败。
2. 送回起源大厅出生点。
3. 发 `DungeonLeaveEvent`。

玩家在地牢维度里退出游戏，重进会落回主世界；`DungeonStore` 会在重启时把维度恢复回来，但玩家不会自动回到里面。

---

## 5. 命令与配置

命令需要权限等级 2：

| 命令 | 作用 |
| --- | --- |
| `/vmdiecore dungeon create <theme> <level>` | 创建，成功打印 id 与锚点 |
| `/vmdiecore dungeon destroy <id>` | 删除 |
| `/vmdiecore dungeon enter <id>` | 传进去（仅玩家） |
| `/vmdiecore dungeon leave` | 送回起源大厅（仅玩家） |
| `/vmdiecore dungeon list` | 列出所有地牢，玩家执行时附带自己的进入次数 |
| `/vmdiecore dungeon current` | 当前所在地牢 id（仅玩家） |
| `/vmdiecore dungeon debug <id>` | 打印维度是否在表、玩家数、实体数、tick 总和、出生点是否加载、当前时间 |
| `/vmdiecore dungeon reload` | 重扫三类定义并打印数量 |

配置项：

| 键 | 缺省 | 范围 | 说明 |
| --- | --- | --- | --- |
| `dungeon.maxDungeons` | 8 | 1~64 | 同时存在的实例上限，超了淘汰最早创建的 |
| `console.requireOp` | true | — | 地牢控制台方块是否要求 OP 才能开界面，创造模式玩家豁免 |

`debug` 里的「维度在表」和「tick 总和」两项专门用来查两类常见故障：维度没注册进 `server.levels`、维度没被 tick。

---

## 6. 边界与已知限制

- 对外面只承诺 `VMDungeons` + 数据记录 + 事件。`DungeonApi`、`DungeonPool`、`DungeonLevelFactory` 在 api 包里或别处，但定位是内部实现，签名随时会改。
- `VMDungeons` 没有列实例的方法。要中文名、进入次数这类信息只能走 `/vmdiecore dungeon list`，或者自己用 `handles()` 拿到 id 再拼。要补的话在 `VMDungeons` 上加一个返回 `DungeonInfo` 列表的方法最省事。
- `create` 的 `server` 参数目前没用上。
- 一个实例一个维度，上限受 `maxDungeons` 限制，没有按玩家隔离的实例。
- 结构拼装是一次性的，不会随玩家进度重新生成；同一实例反复进入看到的是同一个结构。
- 维度是运行时造的，不在 `level_stem` 注册表里。存档备份、外部工具读地图时看不到这些维度，重启后才由 `DungeonStore` 恢复。
- 池列表里第一个成功的池生效，不做「全部拼进去」。想拼多个结构就把它们做成一个模板池。
- `clear_radius` 能从 JSON 读、能存进 `DungeonProfile`，但没有任何代码用它清方块。锚点周围原本有什么就还在。
- `flat` 和 `noise` 生成器只出地形，等级里的结构池不参与。要「地形 + 建筑」得改实现。

---

## 7. 内部实现备忘

包结构（39 个类）：

| 包 | 职责 |
| --- | --- |
| `api` | 对外面：`VMDungeons` 门面、数据记录、`DungeonApi` 实现 |
| `api.event` | 四个生命周期事件 + 三个注册事件 |
| `data` | JSON 解析（`JsonSupport`/`ThemeJson`/`ProfileJson`/`RuleJson`）、定义表 `DungeonDefinitions` |
| `dungeon` | `DungeonPool`（实例表与增删）、`DungeonStore`（SavedData）、`DungeonInstance`、`VisitData` |
| `world` | `DungeonLevel`（手造 `ServerLevel`）、`DungeonLevelFactory`（造与恢复）、`DungeonLevelData`、`OriginWorld`、`EmptyProgressListener` |
| `block` / `ui` | 地牢控制台方块与 LDLib2 界面 |
| `command` / `config` / `registry` / `mixin` | 命令、配置、注册、`server.levels` 访问器 |

几条不变量：

- 维度类型必须是注册表里的 id（`vmdiecore:dungeon`），不能用 `Holder.direct` 现造，否则跨维度同步玩家时会抛 `Can't find id for 'Direct{DimensionType[...]}'` 并断连。
- 运行时新增维度之后必须调 `server.markWorldsDirty()`，否则主循环用缓存的维度数组，维度里的实体不动、方块不可破坏。
- `DungeonLevel` 覆写了 `getSeed()`，因为 `ServerLevel` 原实现返回全局种子，会让每个地牢结构一样。
- 停机时不关维度存储，只清内存引用；`ServerStoppedEvent` 里调 `level.close()` 会等 POI 和区块任务卡死。

踩坑细节在 `agents/learn.md`，架构理由在 `agents/docs/dungeon-core-design.md`，逐类结构在 `agents/docs/dungeon-code-structure.md`。
