# 场景：配置/查看/修改技能

## 场景概述

当插件管理员需要**新增一个技能**、**查看已有技能配置**、或**修改技能参数**时，涉及以下文件：

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建技能 | `skill/<技能id>.yml` | 创建 YAML + JS 脚本 |
| 修改参数 | `skill/<技能id>.yml` | 修改变量/图标/冷却等 |
| 修改逻辑 | `skill/<技能id>.yml` → `action` | 修改 JS 脚本 |
| 加入职业 | `job/<职业>.yml` → `__option__.skill[]` | 将技能 ID 加入职业技能列表 |
| 加入技能树 | `skilltree/<树>.yml` → `nodes` + `graph` | 定义学习条件和前置依赖 |

---

## 技能 YAML 完整结构

```yaml
__option__:
  # ── 基础信息 ──
  name: "技能显示名"              # 必填，默认取文件名
  async: true                     # 异步执行，默认 true（sleep 需要）
  max-level: 10                   # 最大等级，默认 10
  started-level: 0                # 起始等级，默认 0
  category: "*"                   # 分类标签，默认 "*"

  # ── 图标显示 ──
  display:
    icon:
      material: DIAMOND_SWORD     # Bukkit Material 枚举
      name: "&b技能名"            # 支持 & 颜色码 + {{变量}} 模板
      lore:                       # 支持 & 颜色码 + {{变量}} 模板
        - "&7等级 &fLv{{level}}"
        - "&7伤害 &b{{damage}}"
      # 可选字段（XSeries 支持）:
      amount: 1                   # 物品数量
      custom-model-data: 10001    # 自定义模型数据

  # ── 变量（JS 表达式，变量: level = 技能等级）──
  variables:
    cooldown: "100"               # 特殊：cast() 自动设置冷却(ticks)
    mp: "50"                      # 特殊：cast() 自动扣除法力
    damage: "32 * level + 100"    # 自定义，脚本中通过 variables.damage 访问
    range: "5"                    # 静态值
    # 条件分支（列表→When）:
    effectType:
      - id: "fire"
        condition: "level <= 5"
        action: "\"燃烧\""
      - id: "ice"
        condition: "level > 5"
        action: "\"冰冻\""

  # ── 属性钩子（key=属性名, value=JS 表达式）──
  hook:
    attributes:
      STR: "10 + level * 2"       # 逻辑属性（需在 config.yml registry 中注册）
      ATK: "5 * level"            # 物理属性直通（无需 registry）

# ── JS 脚本（必须有 function main()）──
action: |
  function main() {
    tell("释放技能!")
    damage(variables.damage)
    setCooldown(skill, variables.cooldown)
  }
```

### 字段速查表

| 字段路径 | 类型 | 默认值 | 说明 |
|----------|------|--------|------|
| `__option__.name` | String | 文件名 | 技能显示名 |
| `__option__.async` | Boolean | true | 异步执行（sleep 需要） |
| `__option__.max-level` | Int | 10 | 最大等级 |
| `__option__.started-level` | Int | 0 | 起始等级 |
| `__option__.category` | String | `"*"` | 分类标签 |
| `__option__.display.icon.material` | Material | - | 物品材质 |
| `__option__.display.icon.name` | String | - | 物品名（`&` + `{{}}`） |
| `__option__.display.icon.lore` | List\<String\> | - | 物品描述 |
| `__option__.variables.<key>` | String/List | - | JS 表达式或 When 列表 |
| `__option__.hook.attributes.<key>` | String | - | JS 表达式 |
| `action` 或 `run` | String | `""` | JS 脚本（优先读 `action`，回退 `run`） |

---

## 最小可工作示例

```yaml
__option__:
  name: "测试技能"

action: |
  function main() {
    tell("Hello from 测试技能!")
  }
```

无需任何变量、图标、属性——仅有 `name` + `action` 即可。

---

## 完整示例：剑刃风暴

```yaml
__option__:
  name: "剑刃风暴"
  max-level: 18
  display:
    icon:
      material: DIAMOND_SWORD
      name: "&b剑刃风暴"
      lore:
        - "&7等级 &fLv{{level}}"
        - "&7段数 &b{{2 * level + 5}}"
        - "&7每段伤害 &b{{15 * level + 60}}"
  variables:
    cooldown: "300"
    hits: "2 * level + 5"
    damage_per_hit: "15 * level + 60"

action: |
  function main() {
    tell("&b剑刃风暴 Lv" + level)
    setCooldown(skill, variables.cooldown)
    // 实际伤害/效果逻辑在此
  }
```

---

## 图标模板变量

`display.icon.name` 和 `display.icon.lore` 中 `{{变量名}}` 在运行时自动替换为变量值。变量来源：
1. `__option__.variables` 中定义的变量（求值后）
2. 内置变量 `level`（技能等级）

```yaml
display:
  icon:
    name: "&6裂地斩 Lv{{level}}"
    lore:
      - "&7伤害 &c{{damage}}"
      - "&7冷却 &e{{cooldown / 20}}秒"
```

---

## When 条件变量

列表格式按顺序匹配第一个满足 `condition` 的项，执行其 `action` 作为变量值：

```yaml
variables:
  damageType:
    - id: "physical"
      condition: "level <= 5"
      action: "\"物理\""
    - id: "magical"
      condition: "level > 5 && level <= 15"
      action: "\"魔法\""
    - id: "true_damage"
      condition: "level > 15"
      action: "\"真实\""
```

`condition` 中可用变量：`level`（技能等级）、`sender`、`profile`。

---

## 脚本中的内置变量

脚本执行时自动注入以下全局变量：

| 变量 | 类型 | 说明 |
|------|------|------|
| `sender` | ProxyTarget | 施法者实体 |
| `origin` | Location | 施法原点（sender 的 location） |
| `level` | int | 当前技能等级 |
| `skill` | ImmutableSkill | 技能对象（`skill.id`、`skill.name`、`skill.getVariable("key")`） |
| `profile` | PlayerTemplate | 玩家数据（sender 为 Player 时可用） |
| `ctx` | SkillContext | 执行上下文 |
| `target` | ProxyTarget | 目标实体（脚本中动态设置） |
| `variables` | Map\<String, Any?\> | `__option__.variables` 求值后的 Map |

---

## 常见配置模式

### 被动技能（无主动释放）

```yaml
__option__:
  name: "战斗精通"
  hook:
    attributes:
      ATK: "5 * level"
      CRIT: "2 * level"

action: |
  function main() {
    // 被动技能：属性由 hook.attributes 自动生效
    // main 可以为空，仅用于占位
  }
```

### 光环/持续效果

```yaml
__option__:
  name: "战吼"
  async: true
  variables:
    cooldown: "600"
    duration: "60 + level * 20"

action: |
  function main() {
    tell("释放战吼!")
    potion("STRENGTH", level, variables.duration)
    var nearby = finder().range(5).type("player").includeSelf().build()
    nearby.forEach(function(t) {
      potion("STRENGTH", level, variables.duration, t)
    })
    setCooldown(skill, variables.cooldown)
  }
```

### 射线/投射技能

```yaml
__option__:
  name: "火球术"
  async: true
  variables:
    cooldown: "60"
    damage: "20 * level + 50"

action: |
  function main() {
    var targets = finder()
        .range(10)
        .sector(2, 15, 0)
        .excludeType("player")
        .sort("DISTANCE")
        .limit(1)
        .build()
    if (targets.length > 0) {
      damageExBy(variables.damage, "SKILL", targets.get(0))
      fire(60, targets.get(0))
    }
    setCooldown(skill, variables.cooldown)
  }
```

---

## 修改检查清单

配置或修改技能时按以下顺序检查：

1. `action` 中是否有 `function main()` —— 没有则脚本不执行
2. `async: true` 是否与脚本中 `sleep()` 使用匹配 —— sleep 需要异步
3. `variables.cooldown` 和 `variables.mp` 是否正确 —— 这两个有自动行为
4. `hook.attributes` 中的 key 是否在 config.yml registry 中注册 —— 否则作为物理直通
5. 技能 ID（文件名）是否已加入对应 job 的 `skill` 列表
6. 技能 ID 是否已加入对应 skilltree 的 `nodes` + `graph`

---

## 关联配置

| 配置 | 关系 |
|------|------|
| `job/*.yml` → `__option__.skill[]` | 将技能 ID 列入职业 |
| `skilltree/*.yml` → `nodes` + `graph` | 定义学习条件 |
| `config.yml` → `settings.attribute.registry` | 属性映射注册 |
| `config.yml` → `settings.condition` | 技能树条件定义 |
| `config.yml` → `settings.damage-causes` | 自定义伤害类型声明 |

> JS API 完整参考（30+ 函数）参见 `references/js-api.md`
> 技能释放流程参见 `SKILL.md#三技能释放流程`
