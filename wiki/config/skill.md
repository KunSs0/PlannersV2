# 技能配置

技能文件位于 `plugins/Planners/skill/` 目录，每个 `.yml` 文件定义一个技能。支持子目录组织。

技能是 Planners 最核心的功能，这里会详细解释每一个字段。

---

## 完整示例

以下是项目自带的 `skill/example0.yml`：

```yaml
__option__:
  name: "裂地斩"

  icon-formatter:
    material: DIAMOND_SWORD
    name: "裂地斩"
    lore:
      - "技能等级 {{level}}"
      - "技能伤害 {{32 * level + 100}}"
  variables:
    cooldown: 100
    damage: 32 * level + 100
  upgrade:
    condition:
      0-100:
        money: 100.0 * level + 32.5
  hook:
    attributes:
      - "攻击力 +${10 * level}"

action: |
  function main() {
    tell("技能释放成功")
    sleep(1500)
    setCooldown(skill, 200)
  }

  function handleHit() {
    tell("on hit " + target)
  }
```

---

## __option__ 字段详解

### name — 技能名称

```yaml
name: "裂地斩"
```

| 类型 | 必填 | 默认值 |
|------|------|--------|
| String | 否 | 文件名 |

技能的显示名称，在 UI 和消息中使用。如果不填，默认使用文件名。

### icon-formatter — 技能图标

```yaml
icon-formatter:
  material: DIAMOND_SWORD
  name: "裂地斩"
  lore:
    - "技能等级 {{level}}"
    - "技能伤害 {{32 * level + 100}}"
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `material` | String | 是 | 物品材质，使用 Bukkit Material 枚举名（如 `DIAMOND_SWORD`、`BLAZE_POWDER`） |
| `name` | String | 否 | 物品显示名称，支持颜色代码（`&6金色`） |
| `lore` | List\<String\> | 否 | 物品描述列表，支持颜色代码 |

**`{{表达式}}` 语法**：在 `name` 和 `lore` 中可以使用双花括号包裹 JS 表达式，会被自动计算。

可用变量：
- `level` — 技能当前等级
- 技能 `variables` 中定义的所有变量名

**示例**：
```yaml
lore:
  - "等级: {{level}}/10"
  - "伤害: {{32 * level + 100}}"
  - "冷却: {{Math.max(200 - level * 10, 50)}} tick"
```

### variables — 技能变量

```yaml
variables:
  cooldown: 100
  damage: 32 * level + 100
  mp: 50
  rectX: [4, 1, 3, 4]
  rectZ: [2, 4, 3, 3]
```

| 类型 | 必填 | 默认值 |
|------|------|--------|
| Map\<String, Any\> | 否 | 空 |

变量的值可以是：
- **固定数值**：`cooldown: 100`
- **JS 表达式**：`damage: 32 * level + 100`（`level` 是技能等级）
- **数组**：`rectX: [4, 1, 3, 4]`

变量定义后，可以在以下地方使用：
1. **图标 lore** 中通过 `{{变量名}}` 引用
2. **脚本** 中直接作为全局变量使用（但数组类型需要在脚本中重新定义）
3. **升级条件** 和 **属性挂钩** 的表达式中

#### 条件变量

变量还支持条件分支，根据不同条件返回不同值：

```yaml
variables:
  damage:
    - condition: "level <= 5"
      action: "level * 10 + 50"
    - condition: "level <= 10"
      action: "level * 20 + 100"
    - condition: "true"
      action: "level * 30 + 200"
```

系统会从上到下检查条件，返回第一个满足条件的值。

### upgrade — 升级条件

```yaml
upgrade:
  condition:
    0-100:
      money: 100.0 * level + 32.5
```

| 字段 | 说明 |
|------|------|
| `condition` | 升级条件组 |

**等级范围格式**：`最小等级-最大等级`

- `0-100` 表示等级 0 到 100 都使用这个条件
- `0-5` 和 `6-10` 可以分段设置不同的升级消耗
- 单个等级用 `5-5` 或简写为 `5`

**货币 ID**：条件中的 key（如 `money`）对应 `module/currency/` 中定义的货币 ID。

**表达式**：值是 JS 表达式，计算升级需要消耗的货币数量。`level` 是当前技能等级。

**分段示例**：
```yaml
upgrade:
  condition:
    # 1~5 级：每级消耗 100 * 等级 金币
    1-5:
      money: 100 * level
    # 6~10 级：每级消耗 500 * 等级 金币
    6-10:
      money: 500 * level
```

#### 高级升级条件

升级条件还支持标记模式（`mark`），用于只检查条件但不扣除货币：

```yaml
upgrade:
  condition:
    0-100:
      money:
        experience: "100.0 * level + 32.5"
        mark: false
```

| 字段 | 说明 |
|------|------|
| `experience` | 消耗数量的 JS 表达式 |
| `mark` | `true` = 只检查是否满足条件，不扣除；`false` = 检查并扣除 |

### hook — 挂钩

```yaml
hook:
  attributes:
    - "攻击力 +${10 * level}"
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `attributes` | List\<String\> | 属性列表，传递给 AttributePlus 等属性插件 |

`${表达式}` 中的 `level` 是技能等级。技能学习后，属性自动生效。

### started-level — 初始等级

```yaml
started-level: 1
```

| 类型 | 必填 | 默认值 |
|------|------|--------|
| Int | 否 | `0` |

玩家学会技能时的初始等级。设为 `0` 表示需要先升级才能使用。

### max-level — 最大等级

```yaml
max-level: 10
```

| 类型 | 必填 | 默认值 |
|------|------|--------|
| Int | 否 | `10` |

技能可以升到的最高等级。

### async — 异步执行

```yaml
async: true
```

| 类型 | 必填 | 默认值 |
|------|------|--------|
| Boolean | 否 | `true` |

是否在异步线程中执行技能脚本。设为 `true` 可以避免技能中的 `sleep()` 阻塞主线程。

> **注意**：异步执行时，某些 Bukkit API 调用可能需要回到主线程。插件内置的全局函数已经处理了线程安全问题。

### category — 技能分类

```yaml
category: "active"
```

| 类型 | 必填 | 默认值 |
|------|------|--------|
| String 或 List | 否 | `*` |

技能分类标签，可用于 UI 中的分类筛选。`*` 表示属于所有分类。

---

## action — 技能脚本

```yaml
action: |
  function main() {
    // 技能主逻辑
  }
```

技能脚本使用标准 JavaScript 语法。详细说明请参考 [脚本引擎概述](../scripting/overview.md)。

### 入口函数

| 函数名 | 调用时机 |
|--------|---------|
| `main()` | 技能释放时自动调用 |
| `handleHit()` | 技能命中目标时调用 |

### 脚本中可用的变量

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `sender` | Object | 技能释放者（通常是 Player） |
| `level` | Int | 技能当前等级 |
| `skill` | ImmutableSkill | 当前技能对象（用于 `setCooldown(skill, ticks)` 等） |
| `ctx` | SkillContext | 技能上下文 |
| `profile` | PlayerTemplate | 玩家档案（仅玩家释放时可用） |
| `origin` | Location | 技能释放位置 |

---

## 更多示例

### 带目标查找的技能

```yaml
__option__:
  name: "批量操作测试"

action: |
  function main() {
    // 对自己造成 10 点伤害（不传 targets，默认作用于 sender）
    damage(10)
    // 治疗自己 5 点
    heal(5)

    // 查找 8 格内的非玩家实体，最多 5 个
    var targets = finder().range(8).excludeType("player").limit(5).build()
    // 对目标造成 20 点伤害
    damage(20, targets)

    // 给自己加速度效果
    potion("SPEED", 1, 200)

    // 把目标弹飞
    velocityAdd(0, 1, 0, targets)

    // 冻结和点燃目标
    freeze(100, targets)
    fire(60, targets)

    // 播放声音
    sound("ENTITY_PLAYER_LEVELUP", 1.0, 1.0)

    tell("技能执行完毕")
  }
```

### 带状态切换的技能

```yaml
__option__:
  name: "x1"
  icon-formatter:
    material: DIAMOND_SWORD
  variables:
    cooldown: 100
    damage: 32 * level + 100
    rectX: [4, 1, 3, 4]
    rectZ: [2, 4, 3, 3]

action: |
  function main() {
    // 获取当前状态索引，默认为 0
    var idx = hasMeta("x1.status", sender) ? getMeta("x1.status", sender) : 0

    var rectX = [4, 1, 3, 4]
    var rectZ = [2, 4, 3, 3]

    var x = rectX[idx]
    var z = rectZ[idx]

    tell("idx=" + idx + ",x=" + x + ", z=" + z)

    // 循环切换状态
    if (idx == 3) {
      idx = -1
    }

    // 更新状态索引
    setMeta("x1.status", idx + 1, sender)
  }
```
