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
| **consume** | String | 否 | JS 执行语句，所有条件通过后执行，如 `route.takeSkillPoints(props.amount)` |

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
      exper: "profile.getLevel() >= props.min"
      props:
        min: 1
      hint: "需要等级{props.min}"

    need_skill:
      exper: "profile.getRegisteredSkillOrNull(props.skillId) != null && profile.getRegisteredSkillOrNull(props.skillId).getLevel() >= props.lv"
      props:
        skillId: ""
        lv: 1
      hint: "需要{props.skillId} Lv{props.lv}"

    consume_sp:
      exper: "route != null && route.getSkillPointsCurrent() >= props.amount"
      props:
        amount: 0
      hint: "技能点不足"
      consume: "route.takeSkillPoints(props.amount)"

    is_warrior:
      exper: "route != null && route.getJob().getId() == 'warrior'"
      props: {}
      hint: "需要战士职业"

    need_coin:
      exper: "false"
      props:
        amount: 0
      hint: "金币条件未实现"

    need_item:
      exper: "false"
      props:
        itemId: ""
        count: 1
      hint: "物品条件未实现"
```

#### 3.1.3 逻辑运算符

YAML 层默认 **AND**：所有条件全部通过才算通过，任一失败立即中止。需要 **OR** 的场景，在 `exper` 中用 JS `||` 运算符组合：

```yaml
    warrior_or_mage:
      exper: "route != null && (route.getJob().getId() == 'warrior' || route.getJob().getId() == 'mage')"
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
执行 exper 表达式 (注入 player/profile/route + 求值后的 props)
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

条件脚本直接注入真实对象：`player` 为 Bukkit Player，`profile` 为 Planners 的 PlayerTemplate，`route` 为当前 PlayerRoute（可能为 null）。`props` 注入为已求值的 `Map`。

exper / consume 表达式执行时可访问以下预注入变量：

| 变量 | 类型 | 来源 |
|------|------|------|
| `player` | Bukkit Player | Bukkit 玩家对象 |
| `profile` | PlayerTemplate | Planners 玩家档案 |
| `route` | PlayerRoute? | 当前路线，未选择路线时为 null |
| `props` | Map | 定义中的 props + 引用处传参合并（已求值） |
| `profile.getLevel()` | Int | Planners 玩家等级 |
| `profile.getRegisteredSkillOrNull(skillId)` | PlayerSkill? | 已注册/学习技能 |
| `route.getSkillPointsCurrent()` | Int | 当前可用技能点 |
| `route.getJob().getId()` | String | 当前职业 ID |
| `route.takeSkillPoints(amount)` | Boolean | 扣除技能点 |

> `need_coin`、`need_item` 当前默认配置为未实现条件，后续对接经济/背包系统时再改为对应真实 API。

#### 3.1.6 代码接口

实现文件：

| 文件 | 路径 |
|------|------|
| `ConditionConfig` | `core/condition/ConditionConfig.kt` |
| `ConditionRegistry` | `core/condition/ConditionRegistry.kt` |
| `ConditionEvaluator` | `core/condition/ConditionEvaluator.kt` |

```kotlin
// ConditionConfig — 数据类
data class ConditionConfig(
    val key: String,                // 条件名
    val exper: String,              // JS 校验表达式，返回 Boolean
    val props: Map<String, Any>,    // 默认参数（值可为常量或 String 公式）
    val hint: String,               // 校验失败提示，支持 {props.xxx}
    val consume: String?            // JS 消耗语句，null 表示无消耗
)

// ConditionRegistry — 单例注册表
object ConditionRegistry {
    fun init()                           // Planners.onEnable() 调用
    fun reload()                         // PluginReloadEvents.Post 时重载
    fun get(key: String): ConditionConfig
    fun getOrNull(key: String): ConditionConfig?
    fun contains(key: String): Boolean
}

// ConditionEvaluator — 执行器
class ConditionEvaluator {
    data class VerifyResult(val passed: Boolean, val hints: List<String>)

    fun verify(conditions: Map<String, Map<String, Any>>, 
               player: Player, contextVars: Map<String, Any> = emptyMap()): VerifyResult
    fun consume(conditions: Map<String, Map<String, Any>>, 
                player: Player, contextVars: Map<String, Any> = emptyMap())
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

  graph:
    shoulder_bash: []
    vanguard_heart:   ["shoulder_bash"]
    vanguard_rupture: ["vanguard_heart"]
```

#### 3.2.3 数据字段说明

**SkillData 层（ImmutableSkillTree）：**

| 字段 | 含义 |
|------|------|
| `id` | 技能树 ID（配置 key，如 `warrior_vanguard`） |
| `name` | 流派显示名 |
| `class` | 所属职业 |
| `type` | base（基础流派）/ branch（分支流派） |
| `nodes` | 技能节点集合，key = ImmutableSkill.id |
| `graph` | 纯拓扑关系，`nodeId → [前置 nodeId]`；UI 读 graph 画连线 |

**Node 层（SkillNode）：**

| 字段 | 含义 |
|------|------|
| `maxLevel` | 该技能最大可升级等级 |
| `levels[N]` | 第 N 级引用的 condition key + 传参（覆盖默认 props） |

**graph 说明：**
- 与 nodes 平级，纯拓扑声明，不携带业务语义
- UI 层：读取 graph → 画连线图
- 校验层：graph 不参与条件校验，前置等级要求走 `levels` 里的 `need_skill` condition
- 数组形式支持多前驱，无前驱用空数组 `[]`

#### 3.2.4 加载

`Registries.SKILL_TREE` = `createDeepMultiBuiltin("skilltree")`，每个 section key → `ImmutableSkillTree`。

加载时校验：
- node key 必须在 `Registries.SKILL` 中存在（警告但不阻塞）
- condition key 必须在 `ConditionRegistry` 中存在（抛异常）
- graph 中声明的节点都必须在 `nodes` 中

---

### 3.3 玩家状态（PlayerRoute.SkillTree）

**目标**：封装技能树相关的玩家状态，所有技能相关操作收敛到 `PlayerRoute.SkillTree` 内部类。

#### 3.3.1 结构

```kotlin
class PlayerRoute {
    var skillTree: SkillTree? = null        // 选职业后通过 initSkillTree() 初始化

    fun initSkillTree(treeId: String)       // 首次选择职业后调用

    inner class SkillTree(
        val immutable: ImmutableSkillTree
    ) {
        val treeId: String                  // immutable.id
        private val evaluator = ConditionEvaluator()

        fun getLevel(skillId: String): Int
        fun getLearnedSkills(): Map<String, PlayerSkill>
        fun learn(player: Player, skillId: String): CompletableFuture<Void>
        fun upgrade(player: Player, skillId: String): CompletableFuture<Void>
        fun canLearn(player: Player, skillId: String): VerifyResult
        fun canUpgrade(player: Player, skillId: String): VerifyResult
    }
}
```

#### 3.3.2 操作流程

**learn(player, skillId):**
1. 确认未学习（`!skills.containsKey`）
2. 加载 SkillData 中该技能 Lv1 的 conditions
3. `evaluator.verify(conditions, player)` → 失败抛异常（含 hints）
4. `evaluator.consume(conditions, player)`
5. `Database.createPlayerSkill()` → `registerSkill()` → `setSkillLevel(template, ps, 1)`

**upgrade(player, skillId):**
1. 从 skills 取当前 PlayerSkill，确认未满级
2. 加载目标等级 (level+1) 的 conditions
3. `evaluator.verify()` → 失败抛异常
4. `evaluator.consume()`
5. `setSkillLevel(template, ps, targetLevel)`

**canLearn / canUpgrade：**
仅校验不消耗，返回 `VerifyResult`，供 UI 预览。

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
| `PlayerRoute` | 已新增 `skillPointsCurrent` / `skillPointsUsed` + `addSkillPoints()` / `takeSkillPoints()` + 内部类 `SkillTree`（learn/upgrade/getLevel/canLearn/canUpgrade） |
| `PlayerTemplate` | 不变 |
| `Database` | 已新增 `updateSkillPoints()` 接口方法 |
| `DatabaseLocal` / `DatabaseSQL` | `planners_route` 表已新增 `sp_current` / `sp_used` 列 + 迁移 + `updateSkillPoints()` |
| `SkillPointsManager` | ✅ 已完成 — 配置加载、等级事件监听、累计计算（带缓存） |
| `SkillPointsFunctions` | ✅ 已完成 — JS 全局函数 `availablePoints()` / `takePoints()` |
| `ConditionConfig` | ✅ 已完成 — 条件配置数据类（exper/props/hint/consume） |
| `ConditionRegistry` | ✅ 已完成 — 加载 `settings.condition` + PluginReloadEvents 重载 |
| `ConditionEvaluator` | ✅ 已完成 — 校验 + 消耗：遍历 conditions → 合并 props → eval exper → 收集 hint；props 支持常量/String公式 |
| `Planners` | `onEnable()` 中调用 `ConditionRegistry.init()` |
| `config.yml` | 新增 `settings.condition` 节点 |
| `Condition` (旧) | ✅ 已删除 — 原有 `Condition` 接口 / `Messaged` / `Combined` / `VerifyInfo` 全部移除 |
| `Route.isInfer()` | ✅ 已删除 — 死代码，无人调用 |
| `ImmutableSkill.conditionAsUpgrade` | ✅ 已删除 — 升级条件移到技能树 |
| `ImmutableSkillTree` | ✅ 已完成 — 技能树定义（nodes + graph） |
| `SkillNode` | ✅ 已完成 — 技能节点（maxLevel + levels） |
| `Registries.SKILL_TREE` | ✅ 已完成 — 加载 `skilltree/*.yml` |
| `PlayerRoute.SkillTree` | ✅ 已完成 — learn/upgrade/getLevel/canLearn/canUpgrade |
| `PlayerRoute.initSkillTree()` | ✅ 已完成 — 选职业后初始化技能树 |

## 六、开发顺序

```
Phase A → Condition 条件模块 ✅ 已完成
          ├── ConditionConfig 数据类
          ├── ConditionRegistry（加载 settings.condition + PluginReloadEvents 重载）
          ├── ConditionEvaluator（verify 遍历AND→合并props→eval exper→收集hint / consume）
          ├── JS 上下文直接注入 player/profile/route
          ├── Planners.onEnable 调用 ConditionRegistry.init()
          └── config.yml 新增 settings.condition 节点（7条预置条件）
Phase B → SkillData 数据加载 ✅ 已完成
          ├── SkillNode（maxLevel + levels → conditions）
          ├── ImmutableSkillTree（nodes + graph + 校验）
          ├── Registries.SKILL_TREE（skilltree/*.yml → DEEP_MULTI）
          └── skilltree/warrior.yml（先锋流派样例）
Phase C → PlayerRoute.SkillTree ✅ 已完成
          ├── initSkillTree(treeId)
          ├── learn(player, skillId)：校验→消耗→创建PlayerSkill→设为Lv1
          ├── upgrade(player, skillId)：校验→消耗→升级
          ├── canLearn / canUpgrade：预览校验（仅校验不消耗）
          └── getLevel / getLearnedSkills
Phase D → SkillPoints 技能点模块 ✅ 已完成
          ├── config.yml 加载 per-level / bonuses JS
          ├── PlayerRoute 新增 skillPointsCurrent / skillPointsUsed + addSkillPoints / takeSkillPoints
          ├── Database 新增 planners_route.sp_current / sp_used 列 + 迁移
          ├── SkillPointsManager（监听 PlayerLevelChangeEvent、calcAccumulated 带缓存）
          └── SkillPointsFunctions 注册 availablePoints / takePoints 到 JS 全局函数
```

---

## 七、技能树 UI 布局

### 7.1 网格布局（9×6 箱子界面）

```
┌────────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬────────┐
│  ▲     │ Skill│  Lv1 │  Lv2 │  Lv3 │  Lv4 │  Lv5 │  Lv6 │  ▲     │
├────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼────────┤
│ Skill1 │  ←   │ [★] │ [★] │ [📖] │      │      │      │   →    │
│ Skill2 │      │ [★] │ [📖] │      │      │      │      │        │
│ Skill3 │      │ [📖] │      │      │      │      │      │        │
│ Skill4 │      │      │      │      │      │      │      │        │
├────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┼────────┤
│  ▼     │      │ SP:5 │ 选中技能详情                     │  ▼     │
└────────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴────────┘
```

### 7.2 区域定义

| 区域 | 坐标 | 功能 |
|------|------|------|
| **技能列表** | Col0, Row1-4 | 4 行技能图标，每行一个技能。按 `graph` 拓扑顺序排列 |
| **上翻技能** | Col0, Row0 | 技能列表上翻页（>4 个技能时） |
| **下翻技能** | Col0, Row5 | 技能列表下翻页 |
| **技能名称** | Col1, Row0 | 当前选中技能的图标和名称 |
| **等级槽** | Col3-8, Row0 | Lv1~Lv6 等级编号 |
| **等级槽** | Col3-8, Row1-4 | 当前选中技能的等级状态（一行一个槽位可操作，其余行只读） |
| **上翻等级** | Col1, Row1 | 等级水平翻页（<Lv1 时用，通常不需要） |
| **下翻等级** | Col9, Row1 | 等级水平翻页（>Lv6 时用） |
| **技能点** | Col3, Row5 | 当前可用技能点余额 |
| **详情** | Col4-6, Row5 | 当前选中技能的名称和等级 |
| **上翻技能** | Col0, Row5 | 技能列表上翻页 |
| **下翻技能** | Col9, Row5 | 技能列表下翻页 |

### 7.3 等级槽状态

| 状态 | 材质 | 含义 | 点击行为 |
|------|------|------|---------|
| `[★]` | 附魔台/金锭 | 已学到该等级 | 查看详情 |
| `[📖]` | 知识之书 | 未学（maxLevel 以内） | 学习或升级 |
| 空 | 灰色染色玻璃 | 超过 maxLevel | — |

- 已学等级 < 当前技能 level → `[★]`
- 当前技能 level + 1 → `[📖]`，可点击升级
- 未学技能 level 1 → `[📖]`，可点击学习
- 超过 maxLevel → 不显示

### 7.4 交互

```
单击 Col0 技能行 → 选中技能，Col3-8 刷新为该技能的等级条
单击等级槽 [📖]
  ├── 技能未学 → 校验+消耗 Lv1 条件 → learn()
  └── 已学 → 校验+消耗 (level+1) 条件 → upgrade()
```

---

## 八、待细化项

1. 技能树 UI 实现（箱子界面编码） ← 当前
2. 转职与技能树切换（分支流派解锁）
3. need_coin / need_item 对接经济/背包系统
