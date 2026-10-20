# 场景：配置/查看/修改技能树

## 场景概述

技能树（SkillTree）控制玩家**如何学习和升级技能**。它定义每个技能的逐级条件（等级要求、消耗、前置依赖），与技能 YAML 本身的"技能能做什么"是独立的两个维度。

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建技能树 | `skilltree/<树id>.yml` | 创建树 + 节点 + 拓扑 |
| 修改条件 | `skilltree/<树id>.yml` → `nodes.<skill>.lv-N` | 改等级/消耗要求 |
| 修改前置 | `skilltree/<树id>.yml` → `graph` | 改技能解锁依赖 |
| 绑定到职业 | `router/<路由>.yml` → `<route>.skill.tree` | 将树关联到路由节点 |
| 添加条件类型 | `config.yml` → `settings.condition` | 新增全局条件模板 |

---

## 技能树 YAML 完整结构

```yaml
<树ID>:                           # 树 ID（router 中 skill.tree 引用此 ID）
  name: "流派显示名"              # 必填
  class: none                     # 所属职业标签，默认 "none"（关联展示用）
  type: base                      # base=初始可选 / branch=转职后解锁

  # ── 技能节点 ──
  nodes:
    <技能ID>:                     # 对应 skill/*.yml 的文件名
      maxLevel: 8                 # 该技能在此树中的最大可升等级
      levels:                     # 逐级条件
        1:                        # Lv1 条件（学习条件）
          <条件key>: { <参数> }
        2:                        # Lv2 条件（升级条件）
          <条件key>: { <参数> }
        # ... 直到 maxLevel

  # ── 前置依赖图 ──
  graph:
    <技能ID>: [前置技能ID, ...]   # 空列表 = 无前置，可直接学
```

### 字段速查表

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `<树ID>` | String | 必填 | 技能树 ID，router 中 `skill.tree` 引用 |
| `name` | String | 树 ID | 流派显示名 |
| `class` | String | `"none"` | 关联职业标签（仅展示用途） |
| `type` | `base` / `branch` | `base` | 基础流派（初始可选）/ 分支流派（转职解锁） |
| `nodes.<skill>.maxLevel` | Int | - | 该技能在此树中最高可升级到多少级 |
| `nodes.<skill>.levels.<N>` | Map | - | 第 N 级的条件集合（key=条件 ID，value=参数） |
| `graph.<skill>` | List\<String\> | 必填 | 前置技能 ID 列表（空列表 = 无前置依赖） |

### 重要约束

- `nodes` 中的 skill ID 必须在 `skill/*.yml` 中存在（否则警告）
- `graph` 中的每个节点必须在 `nodes` 中存在（否则报错阻塞加载）
- `graph` 中的前置节点也必须在 `nodes` 中存在（否则报错阻塞加载）
- 条件 key（如 `player_lv`、`consume_sp`）必须在 `config.yml → settings.condition` 中定义

---

## 内置条件类型

条件定义在 `config.yml → settings.condition`，技能树中引用其 key：

### player_lv — 等级要求

```yaml
1: { player_lv: { min: 5 } }
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `min` | Int | 最低职业等级 |

校验：`profile.getLevel() >= props.min`
提示：`需要等级{props.min}`

### consume_sp — 技能点消耗

```yaml
1: { consume_sp: { amount: 2 } }
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `amount` | Int | 消耗的技能点数 |

校验：`route.getSkillPointsCurrent() >= props.amount`
消耗：`route.takeSkillPoints(props.amount)`（自动执行）
提示：`技能点不足`

### need_skill — 前置技能要求

```yaml
1: { need_skill: { skillId: "slash", lv: 3 } }
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `skillId` | String | 前置技能 ID |
| `lv` | Int | 前置技能最低等级 |

校验：已学该技能且等级 >= props.lv
提示：`需要{props.skillId} Lv{props.lv}`

### is_warrior / is_mage — 职业限制

```yaml
1: { is_warrior: {} }
```

校验当前 route 的职业 ID 是否匹配。无参数。

### need_coin / need_item — 预留（未实现）

`exper` 值为 `"false"`，始终不通过。需自行实现。

---

## 同一技能可组合多个条件

条件按 **AND** 逻辑组合，全部通过才满足：

```yaml
1:
  player_lv: { min: 10 }
  consume_sp: { amount: 3 }
  need_skill: { skillId: "slash", lv: 5 }
```

---

## graph 前置依赖

`graph` 定义技能解锁的拓扑依赖——玩家必须先学完所有前置技能（达到 Lv1+），才能看到该技能节点：

```yaml
graph:
  slash: []                       # 无前置，初始可用
  charge: [slash]                 # 需先学 slash
  heavy_slash: [slash, charge]    # 需先学 slash 和 charge
  berserk: [heavy_slash, war_cry] # 需先学 heavy_slash 和 war_cry
```

**注意**：graph 只检查前置技能是否已学（level > 0），不检查前置技能的等级高低。

---

## 最小可工作示例

```yaml
example_tree:
  name: "示例树"
  class: none
  type: base
  nodes:
    example0:
      maxLevel: 1
      levels:
        1:
          player_lv: { min: 1 }
          consume_sp: { amount: 1 }
  graph:
    example0: []
```

---

## 完整示例：先锋战士（14 个节点）

```yaml
warrior_vanguard:
  name: "先锋战士"
  class: none
  type: base

  nodes:
    slash:
      maxLevel: 8
      levels:
        1:  { player_lv: { min: 1 },  consume_sp: { amount: 1 } }
        2:  { player_lv: { min: 2 },  consume_sp: { amount: 1 } }
        3:  { player_lv: { min: 3 },  consume_sp: { amount: 2 } }
        4:  { player_lv: { min: 5 },  consume_sp: { amount: 2 } }
        5:  { player_lv: { min: 7 },  consume_sp: { amount: 3 } }
        6:  { player_lv: { min: 10 }, consume_sp: { amount: 3 } }
        7:  { player_lv: { min: 14 }, consume_sp: { amount: 4 } }
        8:  { player_lv: { min: 18 }, consume_sp: { amount: 5 } }

    charge:
      maxLevel: 6
      levels:
        1:  { player_lv: { min: 2 },  consume_sp: { amount: 1 } }
        2:  { player_lv: { min: 4 },  consume_sp: { amount: 2 } }
        3:  { player_lv: { min: 6 },  consume_sp: { amount: 2 } }
        4:  { player_lv: { min: 10 }, consume_sp: { amount: 3 } }
        5:  { player_lv: { min: 15 }, consume_sp: { amount: 4 } }
        6:  { player_lv: { min: 20 }, consume_sp: { amount: 5 } }

    last_stand:
      maxLevel: 1
      levels:
        1: { player_lv: { min: 20 }, consume_sp: { amount: 10 } }

    passive_toughness:
      maxLevel: 6
      levels:
        1: { player_lv: { min: 1 },  consume_sp: { amount: 1 } }
        # ... 省略中间等级
        6: { player_lv: { min: 16 }, consume_sp: { amount: 4 } }

  graph:
    slash: []
    charge: [slash]
    last_stand: [blade_storm, blood_lust, earth_splitter]
    passive_toughness: []
    passive_rage: [passive_toughness]
```

---

## 学习/升级流程（运行时）

玩家通过 `/pl skill tree <player>` 打开技能树 UI 后：

```
点击技能节点
  → SkillTree.learn(player, skillId)
    → 检查是否已学（level > 0 → 拒绝）
    → 检查 nodes 中是否有该技能
    → ConditionEvaluator.verify(Lv1 条件)
        → 遍历所有条件 key → 从 config.yml condition 池加载
        → eval(JS exper) 逐个校验 → 任一失败 → 返回 hints
    → ConditionEvaluator.consume(Lv1 条件)
        → 执行 consume 语句（如扣技能点）
    → 创建 PlayerSkill → 存入数据库 → 设置等级为 1
```

```
点击已学技能升级
  → SkillTree.upgrade(player, skillId)
    → 检查是否已学（level == 0 → 拒绝）
    → 检查是否满级（level >= maxLevel → 拒绝）
    → ConditionEvaluator.verify(下一级条件)
    → ConditionEvaluator.consume(下一级条件)
    → setSkillLevel(template, skill, newLevel)
```

### ConditionEvaluator 校验细节

```kotlin
// 对每个条件 key:
val cfg = Planners.conditions[key]           // 从 config.yml 取 ConditionConfig
val props = merge(defaultProps, overrideProps) // 合并参数（String 值会 eval 求值）
val options = ScriptOptions(player, profile, route, props)
val passed = ScriptManager.eval(cfg.exper, options)  // 执行 JS 校验表达式
if (!passed) { hints += interpolate(cfg.hint, props) }
```

---

## 自定义条件

在 `config.yml → settings.condition` 中添加新条件：

```yaml
settings:
  condition:
    need_gold:                    # 条件 key
      exper: "getBalance() >= props.amount"
      props:
        amount: 1000
      hint: "需要{props.amount}金币"
      consume: "takeMoney(props.amount)"
```

然后在技能树中引用：

```yaml
1: { need_gold: { amount: 500 } }
```

| ConditionConfig 字段 | 说明 |
|----------------------|------|
| `exper` | JS 校验表达式，返回 boolean。可用的注入变量：`player`、`profile`、`route`、`props` |
| `props` | 默认参数（可在技能树中覆盖） |
| `hint` | 失败提示，支持 `{props.xxx}` 插值 |
| `consume` | 校验通过后执行的 JS 消耗语句（可选） |

---

## 修改检查清单

配置技能树时按顺序检查：

1. 树 ID 是否已在 `router/*.yml` 的 `skill.tree` 中被引用
2. `nodes` 中的每个 skill ID 是否在 `skill/*.yml` 中存在
3. `graph` 中的每个节点是否都在 `nodes` 中存在
4. `graph` 中的前置节点是否都在 `nodes` 中存在
5. 是否遗漏了 `graph` 声明（每个 node 都必须在 graph 中有一项，即使为空列表）
6. 条件 key 是否在 `config.yml → settings.condition` 中已定义
7. `maxLevel` 是否 ≤ 对应 ImmutableSkill 的 `max-level`

---

## 关联配置

| 配置 | 关系 |
|------|------|
| `skill/*.yml` | 技能脚本定义（nodes 中的 ID 必须存在） |
| `router/*.yml` → `skill.tree` | 路由节点绑定技能树 |
| `job/*.yml` → `__option__.skill[]` | 职业拥有的技能列表 |
| `config.yml` → `settings.condition` | 条件模板定义 |
| `config.yml` → `settings.skill-points` | 技能点获取公式 |
