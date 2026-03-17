# 主配置 (config.yml)

`config.yml` 是 Planners 的核心配置文件，控制数据库、等级系统、法力值、冷却、攻击保护等全局行为。

---

## 完整默认配置

```yaml
database:
  use: SQL
  sql:
    host: 127.0.0.1
    port: 3306
    user: root
    password: 123456
    database: bukkit_plugin
    table: planners_v2

settings:
  damage-causes:
    - SKILL
    - ATTRIBUTE
    - MYTHIC
  placeholder:
    use: script
  attack:
    protect:
      enable: true
      scene:
        - world xxx
        - dungeonplus *
        - team *
        - worldguard xxx
  magic-point:
    resume:
      expression: "1 + Math::random(1, 2)"
      update-tick: 20
    upper-limit:
      expression: "profile.level * 2 + 100"
      update-tick: 20
  level:
    synchronize: true
    original-hook: true
    isolation: all
    algorithm: def0
  cooler:
    use: memory
  minecraft:
    interaction-action:
      enable: false
      empty-skill:
        material: STONE
        name: '空技能 | 待学习'
  bukkit-launch:
    unimpeded-types:
      - AIR
      - SHORT_GRASS
      - GLASS
      - WATER
      - DANDELION
```

---

## 逐项说明

### database — 数据库

```yaml
database:
  use: SQL
  sql:
    host: 127.0.0.1
    port: 3306
    user: root
    password: 123456
    database: bukkit_plugin
    table: planners_v2
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `use` | String | `SQL` | 存储方式。`SQL` = MySQL 数据库，`LOCAL` = 本地文件 |
| `sql.host` | String | `127.0.0.1` | MySQL 服务器地址 |
| `sql.port` | Int | `3306` | MySQL 端口 |
| `sql.user` | String | `root` | 数据库用户名 |
| `sql.password` | String | - | 数据库密码 |
| `sql.database` | String | - | 数据库名称（需要提前创建） |
| `sql.table` | String | `planners_v2` | 数据表名称（自动创建） |

### damage-causes — 自定义伤害类型

```yaml
settings:
  damage-causes:
    - SKILL
    - ATTRIBUTE
    - MYTHIC
```

定义插件可以使用的自定义伤害类型名称。这些名称可以在脚本中通过 `damageEx()` 函数使用。

**为什么需要这个？** 防止拼写错误。如果你在脚本中写了一个没有在这里注册的伤害类型，插件会报错提醒你。

### placeholder — 占位符

```yaml
settings:
  placeholder:
    use: script
```

| 值 | 说明 |
|----|------|
| `script` | 使用 JavaScript 脚本计算占位符的值（功能更强大） |
| `literal` | 直接返回字面量值（性能更好） |

需要安装 PlaceholderAPI 插件才能使用占位符功能。

### attack.protect — 攻击保护

```yaml
settings:
  attack:
    protect:
      enable: true
      scene:
        - world xxx
        - dungeonplus *
        - team *
        - worldguard xxx
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enable` | Boolean | `true` | 是否启用攻击保护 |
| `scene` | List | - | 保护场景列表 |

**场景格式**：`类型 参数`

| 类型 | 参数 | 说明 |
|------|------|------|
| `world` | 世界名称 | 在指定世界中启用保护。`*` 表示所有世界 |
| `dungeonplus` | `*` | DungeonPlus 副本中启用保护 |
| `team` | `*` | 同队伍成员之间启用保护 |
| `worldguard` | 区域名称 | WorldGuard 保护区域中启用保护。`*` 表示所有区域 |

**什么是攻击保护？** 开启后，在指定场景中，技能伤害不会作用于受保护的目标（比如同队友、保护区域内的玩家等）。

### magic-point — 法力值

```yaml
settings:
  magic-point:
    resume:
      expression: "1 + Math::random(1, 2)"
      update-tick: 20
    upper-limit:
      expression: "profile.level * 2 + 100"
      update-tick: 20
```

#### resume（法力值恢复）

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `expression` | String | - | 每次恢复的法力值。这是一个 JS 表达式 |
| `update-tick` | Int | `20` | 恢复间隔，单位 tick（20 tick = 1 秒） |

**表达式中可用的变量**：
- `Math::random(min, max)` — 生成 min 到 max 之间的随机整数

**示例**：`"1 + Math::random(1, 2)"` 表示每次恢复 2~3 点法力值。

#### upper-limit（法力值上限）

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `expression` | String | - | 法力值上限公式。这是一个 JS 表达式 |
| `update-tick` | Int | `20` | 上限刷新间隔 |

**表达式中可用的变量**：
- `profile.level` — 玩家当前等级
- `profile.magicPoint` — 玩家当前法力值

**示例**：`"profile.level * 2 + 100"` 表示 1 级时上限 102，50 级时上限 200。

### level — 等级系统

```yaml
settings:
  level:
    synchronize: true
    original-hook: true
    isolation: all
    algorithm: def0
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `synchronize` | Boolean | `true` | 是否将 Planners 等级同步到 Minecraft 原版经验条 |
| `original-hook` | Boolean | `true` | 是否拦截原版经验获取事件，转换为 Planners 经验 |
| `isolation` | String | `all` | 等级隔离模式（见下表） |
| `algorithm` | String | `def0` | 等级算法 ID，对应 `module/level/` 中的定义。仅 `isolation: all` 时生效 |

**等级隔离模式**：

| 值 | 说明 | 适用场景 |
|----|------|---------|
| `all` | 所有职业共享同一个等级 | 简单的职业系统，转职不影响等级 |
| `router` | 同一路由下的职业共享等级 | 同系职业共享等级，不同系独立 |
| `job` | 每个职业独立等级 | 每个职业有自己的等级进度 |

### cooler — 冷却系统

```yaml
settings:
  cooler:
    use: memory
```

| 值 | 说明 |
|----|------|
| `memory` | 冷却数据存储在内存中，服务器重启后清零 |
| `persistence` | 冷却数据持久化存储，重启后保留 |

### minecraft.interaction-action — 交互动作

```yaml
settings:
  minecraft:
    interaction-action:
      enable: false
      empty-skill:
        material: STONE
        name: '空技能 | 待学习'
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enable` | Boolean | `false` | 是否启用 Minecraft 原生交互触发技能（左键/右键/蹲下等） |
| `empty-skill.material` | String | `STONE` | 未绑定技能时显示的物品材质 |
| `empty-skill.name` | String | - | 未绑定技能时显示的物品名称 |

### bukkit-launch.unimpeded-types — 投射物穿透方块

```yaml
settings:
  bukkit-launch:
    unimpeded-types:
      - AIR
      - SHORT_GRASS
      - GLASS
      - WATER
      - DANDELION
```

定义投射物可以穿透的方块类型。值为 Bukkit Material 枚举名称。
