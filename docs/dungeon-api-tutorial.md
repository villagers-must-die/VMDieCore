# 地牢接入教程

从零做一个能跑的地牢，再补几条常见改法。字段全集与运行时细节在 `docs/dungeon-api.md`，这里只讲怎么动手。

---

## 1. 只写 JSON：做一个两级试验场

全程不碰 Java，也不需要重启游戏。

### 1.1 建目录

数据包根目录（或直接改模组 jar 里的 `data/`）建三层：

```
data/mypack/dungeon_theme/test_field.json
data/mypack/dungeon_profile/test_room.json
data/mypack/dungeon_rule/test_open.json
```

`mypack` 换成你自己的命名空间，三个文件的名字随便起，id 由「命名空间 + 文件名」拼出来。

### 1.2 维度档：一块空试验场

`dungeon_profile/test_room.json`：

```json
{
  "dimension_type": "vmdiecore:dungeon",
  "generator": { "type": "empty" },
  "fixed_time": 6000,
  "landing": { "size": 5, "floor": "minecraft:stone_bricks" }
}
```

`dimension_type` 用 `vmdiecore:dungeon` 这个已注册的类型，不要自己编 id 现造。`generator` 必须是 `empty`——只有空生成器才会去拼结构；换成 `flat` 或 `noise` 就只出地形，建筑一个都不会放。`fixed_time` 是白天，`landing` 是玩家落脚的石砖平台。

想要地形当底子，就先按 flat 建一个维度档，把结构和地形分开测试，别指望一次同时拿到两者。

### 1.3 种类：两级，一级有出口一级没有

`dungeon_theme/test_field.json`：

```json
{
  "name": "试验场",
  "icon": "minecraft:crafting_table",
  "tiers": [
    {
      "level": 1,
      "name": "浅仓",
      "pool": "minecraft:trial_chambers/chamber/end",
      "profile": "mypack:test_room"
    },
    {
      "level": 2,
      "name": "深仓",
      "pools": [
        "minecraft:bastion/starts",
        "minecraft:ancient_city/city_center"
      ],
      "has_exit": false,
      "rule": "mypack:test_open",
      "profile": "mypack:test_room"
    }
  ]
}
```

1 级用默认规则，`has_exit` 缺省就是 true，能进。2 级关掉出口，同时换成开放规则，否则默认规则会拦住它。`pools` 数组是按顺序尝试，第一个拼成功的生效。

### 1.4 规则：允许没有出口

`dungeon_rule/test_open.json`：

```json
{
  "require_exit_point": false
}
```

### 1.5 跑起来

进游戏（创造模式）敲命令，需要权限等级 2：

```
/reload
/vmdiecore dungeon reload
```

第二条会打印「重载完成：种类 N 维度档 N 规则 N」。数量比之前多 1 就说明三个文件都被读到了。没变就去日志里搜 `解析失败`，单文件出错只打 warn，不会炸。

创建：

```
/vmdiecore dungeon create mypack:test_field 1
```

成功会回一行 `已创建地牢 #1 锚点 x, y, z`。接着：

```
/vmdiecore dungeon list
/vmdiecore dungeon enter 1
```

`list` 里 1 级应该显示「有撤离点」，2 级显示「无撤离点」。`enter` 之后你会站在石砖平台上，头顶就是拼出来的结构。

### 1.6 收尾

```
/vmdiecore dungeon debug 1
/vmdiecore dungeon destroy 1
```

`debug` 会打印维度是否在表、玩家数、实体数、tick 总和。`维度在表 true` 且 `tick 总和` 随时间增长，才说明这个手造维度真的在跑。

---

## 2. 用 Java：在模组里注册一份定义

数据包能表达的东西代码都能表达，适合定义要跟着配置或条件变的情况。

### 2.1 监听三个注册事件

```java
package com.example.mypack;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.dishanhai.vmdiecore.api.*;
import org.dishanhai.vmdiecore.api.event.*;

@EventBusSubscriber(modid = "mypack")
public final class DungeonSetup {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("mypack", path);
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    @SubscribeEvent
    public static void themes(RegisterDungeonThemesEvent event) {
        event.register(new DungeonTheme(id("reactor"), "反应堆", mc("beacon"),
                java.util.Map.of(
                        1, new TierDefinition(1, "外环",
                                java.util.List.of(mc("ancient_city/city_center")),
                                true, id("reactor_room"), id("open_rule")),
                        2, new TierDefinition(2, "堆芯",
                                java.util.List.of(mc("bastion/starts")),
                                false, id("reactor_room"), id("open_rule")))));
    }

    private static final ResourceLocation DUNGEON_TYPE =
            ResourceLocation.fromNamespaceAndPath("vmdiecore", "dungeon");

    @SubscribeEvent
    public static void profiles(RegisterDungeonProfilesEvent event) {
        event.register(new DungeonProfile(id("reactor_room"), DUNGEON_TYPE,
                new GeneratorSettings.Empty(),
                18000L,
                new DungeonProfile.Anchor(24, -40, -20),
                new DungeonProfile.Jigsaw(16, 6, false),
                40,
                new DungeonProfile.Landing(5, mc("polished_deepslate"))));
    }

    @SubscribeEvent
    public static void rules(RegisterDungeonRulesEvent event) {
        event.register(new DungeonRule(id("open_rule"), false));
    }
}
```

`DungeonProfile` 第一个参数是自己的 id，第二个是维度类型。维度类型必须是已注册的那个，也就是 `vmdiecore:dungeon`，不能用自己的命名空间现编一个。

上面三种定义注册完之后，`/vmdiecore dungeon create mypack:reactor 2` 就能用。

### 2.2 在地牢前后插逻辑

```java
@SubscribeEvent
public static void beforeCreate(DungeonCreateEvent event) {
    if (event.tier().level() >= 3 && !someCondition()) {
        event.setCanceled(true);
    }
}

@SubscribeEvent
public static void afterLeave(DungeonLeaveEvent event) {
    event.player().sendSystemMessage(net.minecraft.network.chat.Component.literal("欢迎回来"));
}
```

`DungeonCreateEvent` 触发时维度还没建，别在里面拿 `server.getLevel(...)`。要往里放东西，等 `DungeonEnterEvent`，那时维度已经存在并且玩家即将被传送。

### 2.3 从代码里开地牢

```java
OptionalInt created = VMDungeons.create(server, ResourceLocation.fromNamespaceAndPath("mypack", "reactor"), 1);
created.ifPresent(value -> VMDungeons.enter(player, value));
```

`create` 只造维度、拼结构，不会把人放进去；要进门得再调 `enter`。

---

## 3. 常见改法

| 想要 | 怎么做 |
| --- | --- |
| 每一级用不同的结构池 | `tiers` 里每项写自己的 `pool`/`pools` |
| 一个等级里随机从几个结构里挑 | 一个池里放多个模板，或者 `pools` 写多个池按顺序回退 |
| 某级不允许进入 | 保留默认规则，写 `has_exit: false` |
| 某级没有出口但还要能进 | 配一个 `require_exit_point: false` 的规则，在等级里引用 |
| 每个地牢时间都固定 | 维度档写 `fixed_time: 6000`（白天）或 `18000`（午夜） |
| 不要改时间 | `fixed_time: -1` |
| 空世界只留结构 | `generator: { "type": "empty" }` |
| 想要原版地形 | `generator: { "type": "noise", "settings": "minecraft:overworld", "biome_source": "minecraft:overworld" }` |
| 想复刻「上 3 层基岩 + 石头 + 下 3 层基岩」 | `flat` 的 `layers` 按顺序写 bedrock 3 / stone 若干 / bedrock 3，但这条路上不会摆结构 |
| 换个维度类型（换天空、换雾） | 先注册一份 `dimension_type`，再在维度档的 `dimension_type` 里引用 |
| 调试结构长什么样 | 临时把 `jigsaw.keep_jigsaws` 设 true，看完改回来 |
| 覆盖同 id 的定义 | 数据包或注册事件用同一个 id 写，后者整体覆盖前者 |

---

## 4. 排错清单

**进不去，命令回「进入失败」**

按顺序查：地牢 id 对不对 → 日志里有没有「没有撤离点」→ 等级是不是 `has_exit: false` 还挂着默认规则。日志的 warn 会说明是哪一步拦下的。

**创建成功但建筑没出现**

先确认维度档的 `generator.type` 是 `empty`。`flat` 和 `noise` 不拼结构，只会给你一片地形加落脚平台。确认是 `empty` 之后再看日志是「结构池 X 不存在」还是「结构池 X 拼装失败」。前者是池 id 写错或那个池没有被数据包加载；后者多半是 `jigsaw.depth` 太小，`generateJigsaw` 会静默返回 false。试着把 `depth` 提到 14 以上再看。也可以先用 `minecraft:trial_chambers/chamber/end` 这种确定能拼的池验证整条链路。

**结构里一堆带箭头的方块**

`keep_jigsaws` 是 true。改成 false，然后 `destroy` 重建——已经生成的结构不会自己变。

**每个地牢结构一模一样**

检查维度档是不是同一个、种子有没有被谁写死。正常情况下每个实例的种子不同，结构也不同。

**维度里实体不动、方块敲不掉**

维度没被 tick。这是运行时造维度的老问题，`server.markWorldsDirty()` 没生效时会这样，属于模组自身故障，用 `/vmdiecore dungeon debug <id>` 看「维度在表」和「tick 总和」确认，报给开发者。

**`/vmdiecore dungeon reload` 数量没变**

JSON 没读到。确认路径是 `data/<命名空间>/dungeon_theme/<名字>.json`，不是 `assets/`；确认文件后缀是 `.json`；再看日志里「读取定义 X 失败」。

**改完 JSON 没反应**

`/reload` 是必须的。数据定义在服务端加载，改客户端文件不算。

---

需要字段级细节时回 `docs/dungeon-api.md` 查表。踩坑记录在 `agents/learn.md`。
