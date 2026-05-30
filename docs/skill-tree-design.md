# 职业技能树系统 · 方案设计

> 版本：V1.0  
> 日期：2026-05-30  
> 基于：《技能创建规则 V3.0》《职业大纲》《先锋流派》《插件模块实现清单》

---

## 一、系统定位

职业技能树是 P0 核心战斗基础设施之一，为玩家提供**职业主动/被动/终极技的学习与升级**功能。

**核心特征：**
- 同级平铺：满足等级+前置条件后自由分配技能点，非严格层叠结构
- 线性共享：转职路径线性，技能点跨阶段共享

---

## 二、架构分层

```
┌─────────────────────────────────────────────────┐
│ SkillTreeModule (核心逻辑层)                       │
│                                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │Condition │ │SkillData │ │SkillPoints│       │
│  │ 条件校验  │ │ 技能数据  │ │ 技能点    │       │
│  └──────────┘ └──────────┘ └──────────┘        │
├─────────────────────────────────────────────────┤
│ Planners 现有基础设施                              │
│ PlayerTemplate / PlayerRoute / PlayerSkill / DB  │
└─────────────────────────────────────────────────┘
```

---

## 三、模块职责

### 3.1 Condition 条件模块

**目标**：通用化前置条件系统，JS 脚本驱动，不限于技能树，后续 EC 解锁、公会准入、MR 槽位等均可复用。

#### 3.1.1 字段定义

每个条件单元由四个字段组成，全部走 JS 表达式：

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| **exper** | String | ✅ | JS 校验表达式，返回 `Boolean`，上下文注入 `player`、`props` |
| **props** | Section | 否 | 静态参数默认值，注入上下文 `props`；值可以是常量或 JS 公式字符串 |
| **hint** | String | ✅ | 校验失败时提示文本，支持 `{props.xxx}` 变量插值 |
| **consume** | String | 否 | JS 执行语句，所有条件通过后执行，如 `player.takePoints(props.amount)` |

**props 值支持两种形式：**
- 常量值：数字、字符串等，直接传递到 JS 上下文
- JS 公式字符串：以 `"` 包裹的字符串，执行时先 eval 求值，再注入 JS 上下文。公式可引用上下文变量（如 `skillLevel`、`player`）

```yaml
# 调用方示例
levels:
  1:
    consume_sp: { amount: 1 }                           # 常量
  2:
    consume_sp: { amount: "1 + 1" }                     # 公式
  3:
    consume_sp: { amount: "1 + skillLevel * 0.5" }      # 公式引用变量
```

#### 3.1.2 配置格式

Condition 集中定义在 `config.yml` → `settings.condition`，采用 **section 键值对**：

```yaml
# config.yml → settings.condition

settings:
  condition:
    player_lv:
      exper: "player.level >= props.min"
      props:
        min: 1
      hint: "需要等级{props.min}"

    need_skill:
      exper: "player.getSkillLevel(props.skillId) >= props.lv"
      props:
        skillId: ""
        lv: 1
      hint: "需要{props.skillId} Lv{props.lv}"

    consume_sp:
      exper: "player.availablePoints() >= props.amount"
      props:
        amount: 0
      hint: "技能点不足"
      consume: "player.takePoints(props.amount)"

    is_warrior:
      exper: "player.getJob() == 'warrior'"
      props: {}
      hint: "需要战士职业"

    need_coin:
      exper: "player.currency() >= props.amount"
      props:
        amount: 0
      hint: "金币不足{props.amount}"
      consume: "player.takeCurrency(props.amount)"

    need_item:
      exper: "player.hasItem(props.itemId, props.count)"
      props:
        itemId: ""
        count: 1
      hint: "需要{props.itemId} x{props.count}"
      consume: "player.takeItem(props.itemId, props.count)"
```

#### 3.1.3 逻辑运算符

YAML 层默认 **AND**：所有条件全部通过才算通过，任一失败立即中止。需要 **OR** 的场景，在 `exper` 中用 JS `||` 运算符组合：

```yaml
    warrior_or_mage:
      exper: "player.getJob() == 'warrior' || player.getJob() == 'mage'"
      props: {}
      hint: "需要战士或法师职业"
```

一个 condition = 一个语义 = 一个 hint。调用方纯 AND 平铺，OR 封装在 condition 定义的 exper 中。

#### 3.1.4 执行流程

```
遍历 conditions (按 key 的顺序)
  ↓
读取默认 props + 引用处传参合并覆盖
  ↓
逐值处理 props：若值为 String → ScriptManager.eval(表达式) → 数值，否则直接用
  ↓
执行 exper 表达式 (注入 player + 求值后的 props)
  ↓                              ↓
返回 true                      返回 false → 收集 hint → 终止遍历
  ↓
所有条件通过 → 逐项执行各 condition 的 consume (JS语句)
```

**设计要点：**
- conditions 默认 AND 逻辑：全部通过才算通过，任一失败立即中止
- 若需要 OR 逻辑，在 exper 脚本中用 JS 运算符组合
- consume 在所有条件通过后才统一执行
- hint 中的 `{props.xxx}` 插值在提示时动态替换

#### 3.1.5 JS 上下文绑定

`player` 注入为 `PlayerBridge` Java 对象（实现：`module/script/bridge/PlayerBridge.java`），通过 `ScriptOptions.set("player", bridge)` 注入。`props` 注入为已求值的 `Map`。

exper / consume 表达式执行时可访问以下预注入变量：

| 变量 | 类型 | 来源 |
|------|------|------|
| `player` | PlayerBridge | Bukkit Player 桥接对象 |
| `props` | Map | 定义中的 props + 引用处传参合并（已求值） |
| `player.level` | Int | PlayerTemplate.getLevel() |
| `player.availablePoints()` | Int | 当前可用技能点 |
| `player.getSkillLevel(skillId)` | Int | 已学习技能等级（未学习返回0） |
| `player.hasItem(itemId, count)` | Boolean | 背包持有物品（占位） |
| `player.currency()` | Long | 金币余额（占位） |
| `player.getJob()` | String | 当前职业ID |
| `player.takePoints(amount)` | — | 扣除技能点 |
| `player.takeCurrency(amount)` | — | 扣除金币（占位） |
| `player.takeItem(itemId, count)` | — | 扣除物品（占位） |

> `currency`、`hasItem`、`takeCurrency`、`takeItem` 在 Phase A 为占位实现（抛 UnsupportedOperationException），后续对接经济/背包系统。

#### 3.1.6 代码接口

```kotlin
// core/condition/ConditionConfig.kt
data class ConditionConfig(
    val key: String,                // 条件名
    val exper: String,              // JS 校验表达式，返回 Boolean
    val props: Map<String, Any>,    // 默认参数（值可为常量或 String 公式）
    val hint: String,               // 校验失败提示，支持 {props.xxx}
    val consume: String?            // JS 消耗语句，null 表示无消耗
)

// core/condition/ConditionRegistry.kt
object ConditionRegistry {
    fun init()                          // Planners.onEnable() 调用
    fun reload()                        // PluginReloadEvents.Post 时重载
    fun get(key: String): ConditionConfig
    fun getOrNull(key: String): ConditionConfig?
    fun contains(key: String): Boolean
}

// core/condition/ConditionEvaluator.kt
class ConditionEvaluator {
    data class VerifyResult(val passed: Boolean, val hints: List<String>)

    /** 校验条件组，全部通过才返回 true */
    fun verify(conditions: Map<String, Map<String, Any>>, player: Player): VerifyResult

    /** 执行消耗（校验通过后调用） */
    fun consume(conditions: Map<String, Map<String, Any>>, player: Player)
}
```

---

### 3.2 SkillData 技能数据

**目标**：定义职业流派的技能节点集合。核心信息全部收敛到 Condition，节点层只做引用。

#### 3.2.1 设计原则

- 节点 key 即 `ImmutableSkill.id`，不额外做映射
- 所有校验逻辑（等级要求、前置技能、点数消耗）收敛到 Condition
- 配置只引用 condition key + 传参覆盖 props
- 节点层极简：`maxLevel` + 每级引用哪些 condition
- 不管理点数，点数归属 SkillPoints 模块

#### 3.2.2 配置格式

```yaml
# skilltree/warrior.yml

warrior_vanguard:
  name: "先锋"
  class: warrior
  type: base

  nodes:
    shoulder_bash:
      maxLevel: 3
      levels:
        1:
          player_lv: { min: 1 }
          is_warrior: {}
          consume_sp: { amount: 1 }
        2:
          player_lv: { min: 5 }
          consume_sp: { amount: 1 }
        3:
          player_lv: { min: 9 }
          consume_sp: { amount: 2 }

    vanguard_heart:
      maxLevel: 3
      levels:
        1:
          player_lv: { min: 8 }
          need_skill: { skillId: "shoulder_bash", lv: 1 }
          consume_sp: { amount: 1 }
        2:
          player_lv: { min: 18 }
          consume_sp: { amount: 1 }
        3:
          player_lv: { min: 28 }
          consume_sp: { amount: 1 }

    vanguard_rupture:
      maxLevel: 1
      levels:
        1:
          player_lv: { min: 30 }
          need_skill: { skillId: "vanguard_heart", lv: 3 }
          consume_sp: { amount: 5 }
```

#### 3.2.3 数据字段说明

**SkillData 层：**

| 字段 | 含义 |
|------|------|
| `name` | 流派显示名 |
| `class` | 所属职业 |
| `type` | base(基础流派) / branch(分支流派) |
| `nodes` | 技能节点集合，key = ImmutableSkill.id |

**Node 层：**

| 字段 | 含义 |
|------|------|
| `maxLevel` | 该技能最大可升级等级 |
| `levels[N]` | 第 N 级引用的 condition key + 传参（覆盖默认 props） |

---

### 3.3 玩家状态（PlayerRoute.SkillTree）

**目标**：封装技能树相关的玩家状态，所有技能相关操作收敛到 `PlayerRoute.SkillTree` 内部类。

#### 3.3.1 结构

```kotlin
class PlayerRoute {
    lateinit var skillTree: SkillTree           // Lv1 选职业后初始化

    inner class SkillTree {
        val treeId: String                       // 关联 SkillData ID
        private val nodes: Map<String, PlayerSkill>  // skillId → PlayerSkill

        /** 获取某技能当前等级（未学习返回0） */
        fun getLevel(skillId: String): Int

        /** 首次学习 */
        fun learn(skillId: String, evaluator: ConditionEvaluator): CompletableFuture<Void>

        /** 升级 */
        fun upgrade(skillId: String, evaluator: ConditionEvaluator): CompletableFuture<Void>
    }
}
```

#### 3.3.2 操作流程

**learn(skillId):**
1. 确认 `!nodes.containsKey(skillId)`
2. 加载 SkillData 配置中该技能 Lv1 的 conditions
3. `evaluator.verify()` → 失败则抛异常
4. `evaluator.consume()`
5. 创建 `PlayerSkill(level=1)` → 写入 nodes → 持久化

**upgrade(skillId):**
1. 从 nodes 取当前 level
2. 加载目标等级 (level+1) 的 conditions
3. `evaluator.verify()` → 失败则抛异常
4. `evaluator.consume()`
5. `PlayerSkill.level += 1` → 持久化

---

### 3.4 SkillPoints 技能点模块

**定位**：技能点获取与余额管理，与 SkillData / Condition 解耦。

**核心特征**：
- **职业线路隔离**：点数归属于 `PlayerRoute`，不同线路独立
- **两级存储**：`current`（当前可用）+ `used`（累计消耗）
- **JS 驱动**：per-level 和 bonuses 均为 JS 表达式
- **无 Provider 层**：点数来源仅等级，直接由 `SkillPointsManager` 在升级事件中计算

#### 3.4.1 存储

点数存储在 `PlayerRoute` 上（`private set`，外部通过方法修改），持久化到 `planners_route` 表：

```kotlin
class PlayerRoute {
    var skillPointsCurrent: Int = 0   // 当前可用 (private set)
        private set
    var skillPointsUsed: Int = 0      // 累计消耗 (private set)
        private set

    fun addSkillPoints(amount: Int)        // 升级时增加，内部持久化
    fun takeSkillPoints(amount: Int): Boolean  // 消耗时扣减，内部持久化
}
```

| 字段 | 数据库列 | 含义 |
|------|---------|------|
| `skillPointsCurrent` | `sp_current` | 当前可用余额 |
| `skillPointsUsed` | `sp_used` | 累计已消耗 |

`可用 = skillPointsCurrent`，`总获得 = current + used`。
每次变更通过 `Database.updateSkillPoints(route)` 异步持久化。

#### 3.4.2 配置

`config.yml` → `settings.skill-points`：

```yaml
settings:
  skill-points:
    # 每级获得点数，JS 表达式，变量: level
    per-level: "level <= 30 ? 3 : 2"
    # 关键等级额外奖励，JS 表达式，变量: level
    bonuses:
      10: "5"
      20: "5"
      30: "10"
      50: "15"
```

#### 3.4.3 计算逻辑

```
监听 PlayerLevelChangeEvent
  → 获取玩家当前 PlayerRoute
  → 计算累计点数(newLevel) - 累计(oldLevel) = 增量
  → route.skillPointsCurrent += 增量
  → 持久化到数据库

累计点数(level) = Σ per-level(lv) for lv in 1..level
                + Σ bonuses[bonusLv] for bonusLv ≤ level
```

降级时增量可能为负，current 最低为 0。

#### 3.4.4 SkillPointsManager

```kotlin
object SkillPointsManager {
    init()                                 // Planners.onEnable() 调用，加载配置
    reloadConfig()                         // PluginReloadEvents 时重新加载，清除缓存

    fun getAvailable(route: PlayerRoute): Int       // → route.skillPointsCurrent
    fun takePoints(route: PlayerRoute, n: Int): Boolean  // → route.takeSkillPoints(n)
    fun calcAccumulated(level: Int): Int             // 累计点数计算（带缓存）
}
```

`addPoints` 由 `PlayerLevelChangeEvent` 监听器内部调用 `route.addSkillPoints()`。

#### 3.4.5 JS 上下文

当前以全局函数形式注册（`SkillPointsFunctions.java` → `ScriptFunctionRegistry`），待 Phase A Condition 系统完成后迁移为 `player` 对象的方法：

| 函数 | 签名 | 来源 |
|------|------|------|
| `availablePoints()` | `() → Int` | `SkillPointsManager.getAvailable(route)` |
| `takePoints(n)` | `(amount) → Boolean` | `SkillPointsManager.takePoints(route, n)` |

均支持可选 player 参数（默认取 `ScriptContext.sender`）。

---

## 四、数据流

```
玩家升级 (PlayerLevelChangeEvent)
  → SkillPointsManager 计算增量 = calcAccumulated(to) - calcAccumulated(form)
  → route.addSkillPoints(delta)
    → skillPointsCurrent += delta
    → submitAsync { Database.updateSkillPoints(this) }

请求升级技能
  → 读取节点目标等级的 conditions
  → evaluator.verify(conditions, context)
  → 失败: 返回 hints 列表
  → 通过: evaluator.consume(conditions, context)
    → consume_sp: takePoints(n)
      → route.takeSkillPoints(n)
        → skillPointsCurrent -= n, skillPointsUsed += n
        → submitAsync { Database.updateSkillPoints(this) }
  → SkillTree.upgrade(skillId) → PlayerSkill.level += 1
```

---

## 五、与现有系统对接

| 现有组件 | 改动 |
|---------|------|
| `ImmutableSkill` | 不存在，node key 即其 id |
| `PlayerSkill` | 不变，已有 `level` 字段即技能树等级 |
| `PlayerRoute` | 已新增 `skillPointsCurrent` / `skillPointsUsed` 字段 + `addSkillPoints()` / `takeSkillPoints()`；内部类 `SkillTree` 待实现 |
| `PlayerTemplate` | 不变 |
| `Database` | 已新增 `updateSkillPoints()` 接口方法 |
| `DatabaseLocal` / `DatabaseSQL` | `planners_route` 表已新增 `sp_current` / `sp_used` 列 + 迁移 + `updateSkillPoints()` |
| `SkillPointsManager` | ✅ 已完成 — 配置加载、等级事件监听、累计计算（带缓存） |
| `SkillPointsFunctions` | ✅ 已完成 — JS 全局函数 `availablePoints()` / `takePoints()` |
| `ConditionConfig` | 新文件 — 条件配置数据类 |
| `ConditionRegistry` | 新文件 — 加载 `settings.condition` + PluginReloadEvents 重载 |
| `ConditionEvaluator` | 新文件 — 校验 + 消耗：遍历 conditions → 合并 props → eval exper → 收集 hint |
| `PlayerBridge` | 新文件（Java）— 注入 JS 的 `player` 桥接对象 |
| `Planners` | `onEnable()` 中调用 `ConditionRegistry.init()` |
| `config.yml` | 新增 `settings.condition` 节点 |
| `Condition` (旧) | ✅ 已删除 — 原有 `Condition` 接口 / `Messaged` / `Combined` / `VerifyInfo` 全部移除 |
| `Route.isInfer()` | ✅ 已删除 — 死代码，无人调用 |
| `ImmutableSkill.conditionAsUpgrade` | ✅ 已删除 — 升级条件移到技能树 |

## 六、开发顺序

```
Phase A → Condition 条件模块（settings.condition 加载 + Registry + Evaluator + PlayerBridge）
Phase B → SkillData 数据加载（skilltree/*.yml 加载 + 校验）
Phase C → PlayerRoute.SkillTree（learn/upgrade/getLevel）
Phase D → SkillPoints 技能点模块 ✅ 已完成
          ├── config.yml 加载 per-level / bonuses JS
          ├── PlayerRoute 新增 skillPointsCurrent / skillPointsUsed + addSkillPoints / takeSkillPoints
          ├── Database 新增 planners_route.sp_current / sp_used 列 + 迁移
          ├── SkillPointsManager（监听 PlayerLevelChangeEvent、calcAccumulated 带缓存）
          └── SkillPointsFunctions 注册 availablePoints / takePoints 到 JS 全局函数
```

---

## 七、待细化项

1. SkillData YAML 的加载与校验（Phase B）
2. PlayerRoute.SkillTree 内部类的具体实现（Phase C）
3. PlayerBridge 中 currency/hasItem/takeCurrency/takeItem 对接经济/背包系统
