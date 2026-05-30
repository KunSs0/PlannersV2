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
| **props** | Section | 否 | 静态参数默认值，注入上下文 `props` |
| **hint** | String | ✅ | 校验失败时提示文本，支持 `{props.xxx}` 变量插值 |
| **consume** | String | 否 | JS 执行语句，所有条件通过后执行，如 `player.takePoints(props.amount)` |

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

#### 3.1.3 执行流程

```
遍历 conditions (按 key 的顺序)
  ↓
读取 props，合并引用处传参覆盖默认值
  ↓
执行 exper 表达式 → 返回 false → 收集 hint 为失败原因 → 终止遍历
  ↓                              ↓
返回 true                      返回 (false, hint)
  ↓
所有条件通过 → 逐项执行各 condition 的 consume (JS语句)
```

**设计要点：**
- conditions 默认 AND 逻辑：全部通过才算通过，任一失败立即中止
- 若需要 OR 逻辑，在 exper 脚本中用 JS 运算符组合
- consume 在所有条件通过后才统一执行
- hint 中的 `{props.xxx}` 插值在提示时动态替换

#### 3.1.4 JS 上下文绑定

exper / consume 表达式执行时可访问以下预注入变量：

| 变量 | 类型 | 来源 |
|------|------|------|
| `player` | Player | Bukkit Player 对象 |
| `props` | Map | 定义中的 props + 引用处传参合并 |
| `player.level` | Int | PlayerTemplate.getLevel() |
| `player.availablePoints()` | Int | 当前可用技能点 |
| `player.getSkillLevel(skillId)` | Int | 已学习技能等级（未学习返回0） |
| `player.hasItem(itemId, count)` | Boolean | 背包持有物品 |
| `player.currency()` | Long | 金币余额 |
| `player.getJob()` | String | 当前职业ID |
| `player.takePoints(amount)` | — | 扣除技能点 |
| `player.takeCurrency(amount)` | — | 扣除金币 |
| `player.takeItem(itemId, count)` | — | 扣除物品 |

#### 3.1.5 代码接口

```kotlin
interface ConditionEvaluator {
    data class VerifyResult(val passed: Boolean, val hints: List<String>)

    /** 校验条件组，全部通过才返回 true */
    fun verify(conditions: Map<String, Map<String, Any>>, context: ConditionContext): VerifyResult

    /** 执行消耗（校验通过后调用） */
    fun consume(conditions: Map<String, Map<String, Any>>, context: ConditionContext)
}

data class ConditionConfig(
    val exper: String,
    val props: Map<String, Any>,
    val hint: String,
    val consume: String?       // JS 语句，null 表示无消耗
)
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

点数存储在 `PlayerRoute` 上，持久化到 `planners_route` 表：

```kotlin
class PlayerRoute {
    var skillPointsCurrent: Int   // 当前可用
    var skillPointsUsed: Int      // 累计消耗
}
```

| 字段 | 数据库列 | 含义 |
|------|---------|------|
| `current` | `sp_current` | 当前可用余额 |
| `used` | `sp_used` | 累计已消耗 |

`可用 = current`，`总获得 = current + used`。

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

    /** 可用点数 */
    fun getAvailable(route: PlayerRoute): Int = route.skillPointsCurrent

    /** 增加点数（升级时调用） */
    fun addPoints(route: PlayerRoute, amount: Int)

    /** 消耗点数（学技能时调用） */
    fun takePoints(route: PlayerRoute, amount: Int): Boolean

    /** 计算指定等级的累计获得 */
    fun calcAccumulated(level: Int): Int
}
```

#### 3.4.5 JS 上下文

| 变量 | 来源 |
|------|------|
| `player.availablePoints()` | `SkillPointsManager.getAvailable(route)` |
| `player.takePoints(n)` | `SkillPointsManager.takePoints(route, n)` |

通过 `GlobalFunctions` 注册。

---

## 四、数据流

```
玩家升级 (PlayerLevelChangeEvent)
  → SkillPointsManager 计算增量 = 累计(newLevel) - 累计(oldLevel)
  → route.skillPointsCurrent += delta → 持久化 database.updateRoute()

请求升级技能
  → 读取节点目标等级的 conditions
  → evaluator.verify(conditions, context)
  → 失败: 返回 hints 列表
  → 通过: evaluator.consume(conditions, context)
    → consume_sp: player.takePoints(n)
      → route.skillPointsCurrent -= n, route.skillPointsUsed += n
      → 持久化 database.updateRoute()
  → SkillTree.upgrade(skillId) → PlayerSkill.level += 1
```

---

## 五、与现有系统对接

| 现有组件 | 改动 |
|---------|------|
| `ImmutableSkill` | 不变，node key 即其 id |
| `PlayerSkill` | 不变，已有 `level` 字段即技能树等级 |
| `PlayerRoute` | 新增 `skillPointsCurrent` / `skillPointsUsed` 字段 + 内部类 `SkillTree` |
| `PlayerTemplate` | 不变 |
| `GlobalFunctions` | 注册 `player.availablePoints` / `player.takePoints` 到 JS 上下文 |
| `config.yml` | 新增 `settings.condition` + `settings.skill-points` 节点 |
| `planners_route` 表 | 新增 `sp_current` / `sp_used` 列 |

---

## 六、开发顺序

```
Phase A → Condition 条件模块（settings.condition 加载 + Evaluator）
Phase B → SkillData 数据加载（skilltree/*.yml 加载 + 校验）
Phase C → PlayerRoute.SkillTree（learn/upgrade/getLevel）
Phase D → SkillPoints 技能点模块
          ├── config.yml 加载 per-level / bonuses JS
          ├── PlayerRoute 新增 sp_current / sp_used 字段
          ├── Database 新增 planners_route 对应列
          ├── SkillPointsManager（监听 PlayerLevelChangeEvent、addPoints / takePoints / calcAccumulated）
          └── GlobalFunctions 注册 player.availablePoints / player.takePoints
```

---

## 七、待细化项

1. ConditionEvaluator 与现有 ScriptManager 的集成方式
