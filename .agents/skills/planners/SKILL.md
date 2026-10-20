---
name: planners
description: 编写 Planners Bukkit RPG 插件技能脚本和配置的完整指南。当需要创建/修改技能 YAML 配置、编写技能 JS 脚本、理解技能释放流程与 API、配置职业/路由/技能树、设置属性系统、使用命令或事件时触发。涵盖所有 YAML 结构、JS API、按键背包系统、延迟释放、外部桥接、冷却/法力/等级系统。
---

# Planners 技能编写指南

## 概述

Planners 是一个 Bukkit RPG 技能/职业插件。技能通过 **YAML** 定义元数据，通过 **JavaScript** 实现逻辑。

### 核心概念链

```
config.yml (keymapping + backpack + attribute 等)
    ├── skill/*.yml → ImmutableSkill (技能定义 + JS 脚本)
    ├── job/*.yml → ImmutableJob (职业 = 技能集合 + 属性)
    ├── router/*.yml → ImmutableRouter (职业树/转职路线)
    └── skilltree/*.yml → 技能节点升级条件 + 依赖图

玩家选择 Router → 获得 Job → 学习 Job 下所有 Skill → 装入 Backpack 槽位 → 按键触发释放
```

---

## 一、技能 YAML 最小示例

```yaml
__option__:
  name: "裂地斩"
  async: true                    # 异步执行（sleep 需要）
  max-level: 10
  started-level: 0

  # 图标（支持 {{variable}} 模板）
  display:
    icon:
      material: DIAMOND_SWORD
      name: "裂地斩"
      lore:
        - "等级 {{level}}"
        - "伤害 {{damage}}"

  # 变量（cooldown 和 mp 自动生效，无需在脚本中处理）
  variables:
    cooldown: "100"             # 冷却(ticks)
    mp: "50"                    # 法力消耗
    damage: "32 * level + 100"  # 自定义变量

  # 属性钩子（Map 结构，JS 表达式）
  hook:
    attributes:
      STR: "10 + level * 2"

action: |
  function main() {
    tell("释放裂地斩!")
    damage(variables.damage)
  }
```

### 关键字段说明

| 字段 | 说明 |
|------|------|
| `async` | true=异步（sleep 可用），默认 true |
| `variables.cooldown` | cast() 时自动设置冷却 |
| `variables.mp` | cast() 时自动扣除法力 |
| `display.icon` | 技能图标，支持 `{{variable}}` 模板 |
| `hook.attributes` | Map 结构，值支持 JS 表达式，自动汇总到属性系统 |
| `action` | JS 脚本，必须有 `function main()` |

### When 条件变量（列表格式）

```yaml
variables:
  effectType:
    - id: "fire"
      condition: "level <= 5"
      action: "\"燃烧\""
    - id: "ice"
      condition: "level > 5"
      action: "\"冰冻\""
```

按顺序匹配第一个满足 condition 的项，`action` 值赋给变量。

> 完整 YAML 结构（skill/job/router/skilltree/state/action/module）参见 `references/yaml-reference.md`

---

## 二、JS 脚本环境

### 执行流程

```
ImmutableSkill.execute(sender, level)
  → 注入变量：sender, origin, level, skill, ctx, profile
  → eval(action) 加载函数声明
  → invoke("main") 调用入口
```

### 全局变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `sender` | ProxyTarget | 施法者 |
| `origin` | Location | 施法原点 |
| `level` | int | 技能等级 |
| `skill` | ImmutableSkill | 技能对象（`skill.id`、`skill.name`、`skill.getVariable("key")`） |
| `profile` | PlayerTemplate | 玩家数据（sender 为 Player 时） |
| `ctx` | SkillContext | 执行上下文 |
| `target` | ProxyTarget | 目标实体（脚本动态设置） |

### 最常用 API

```javascript
// 伤害/治疗
damage(amount)              // cause=SKILL
damageEx(amount, "SKILL")   // 自定义 cause
damageBy(amount, source)    // 指定来源
heal(amount)

// 冷却（skill 参数可用 ID 字符串或 skill 对象）
setCooldown(skill, ticks)
getCooldown(skill)

// 效果
potion("SLOW", 2, 100)      // 缓慢 II，5秒
freeze(60)                  // 冻结 3 秒
fire(100)                   // 着火 5 秒
explosion(2, false, false)  // 威力 2，无火无破坏

// 实体
entitySpawn("ZOMBIE", 200)  // 10 秒后自动移除
entityTeleport(x, y, z)
entitySetAI(false)          // 关闭 AI

// 指令/经济/属性
command("spawn")            // 以 sender 身份执行
commandConsole("say hello") // 控制台执行
getBalance()                // Vault 金币
getAttr("ATK")              // 读取属性值

// 工具
sleep(500)                  // 暂停 500ms（仅 async=true）
random(1, 10)               // 随机整数
tell("消息文本")
```

> 完整 JS API（30+ 函数）参见 `references/js-api.md`

---

## 三、技能释放流程

```
按键 → CombinedAnalyzer → BackpackAPI.getSkillByKey()
  → PlayerSkillCastEvent.Check   → 取消：CANCEL_WITH_EVENT
  → Cooler 检查                   → 冷却：COOLING
  → MP 变量计算                   → 不足：MAGICPOINT_INSUFFICIENT
  → PlayerSkillCastEvent.Pre      → 取消：CANCEL_WITH_EVENT
  → 锁定资源（设置冷却、扣除法力）
  → Hook 拦截？（SkillInputExecHook）
      ├─ 是 → INTERCEPTED（等待 resume()）
      └─ 否 → skill.execute() → Post 事件 → SUCCESS
```

### ExecutableResult 返回值

| 值 | 含义 |
|----|------|
| `COOLING` | 冷却中 |
| `MAGICPOINT_INSUFFICIENT` | 法力不足 |
| `CANCEL_WITH_EVENT` | 被 Check/Pre 事件取消 |
| `INTERCEPTED` | 被 Hook 接管，等待异步 resume() |
| `SUCCESS` | 成功 |

### SkillInputExec — 延迟释放

当外部插件（如动画系统）需要在执行前播放招式动画时：

```kotlin
class AnimHook : SkillInputExecHook {
    override fun intercept(ctx: SkillInputExec.Context) {
        animation.play(ctx.player) {
            ctx.resume()  // 动画完成后继续执行
        }
    }
}
PlannersAPI.registerSkillInputExecHook(AnimHook())
```

可注册多个 hook（追加到尾部），但 cast() 只取第一个执行（`firstOrNull`）。返回 `INTERCEPTED` 不代表失败，而是等待 hook 异步恢复。

---

## 四、按键与背包

### 按键注册（config.yml → keymapping）

```yaml
key0:
  name: R
  matching-type: strict     # strict（顺序）/ fuzzy（无序）/ none（单键直通）
  request-tick: 30
  mapping: R
```

### 背包页配置

```yaml
backpack:
  default-page: "0"
  pages:
    "0":
      name: "战斗页"
      slots:
        slot0: { key: key0 }
        slot1: { key: key1 }
```

每页独立，slot 通过 key ID 引用 keymapping。

### KeyBindingBridge — 外部插件按键接入

供外部插件将自定义按键送入 CombinedAnalyzer：

```kotlin
KeyBindingBridge.processKeyAction(player, "keyboard.r")
```

`keyCode` 需与 keymapping 中的 `mapping` 值一致。与 `CombinedHandler`（处理 Bukkit 原生事件）汇入同一匹配引擎。

### BackpackAPI

```
getCurrentPage(template) → String
setCurrentPage(template, page)
equipSkill(template, skill, page, slot)
unequipSkill(template, skill)
getSkillByKey(template, keyId) → PlayerSkill?
```

---

## 五、职业与路由

### 概念关系

```
Router (职业系)
  ├── originate → 起始 Job（玩家首次选择）
  └── Route 节点（= Job ID）
        ├── skill.tree → SkillTree（技能学习/升级条件）
        └── branch → 可转职的子 Job

Job (职业)
  └── skill[] → 该职业拥有的技能 ID 列表

SkillTree (技能树)
  └── nodes.<skill-id>.lv-N → 逐级学习条件 + graph 前置依赖
```

**关键区别**：Job 声明"拥有哪些技能"，SkillTree 控制"如何学习/升级这些技能"。

### 职业 (job/*.yml)

```yaml
__option__:
  name: 战士
  skill: [slash, charge]          # 技能 ID 列表（必填）
  variables:                      # 职业级变量（可选，level=职业等级）
    atkBonus: "level * 2"
  hook:
    attributes:                   # 职业级属性（可选，JS 表达式）
      STR: "10 + level * 2"
```

### 路由 (router/*.yml)

```yaml
__option__:
  name: "战士系"
  originate: swordsman            # 起始职业 ID（= job 文件名）
  algorithm:
    level: def0                   # 经验算法 ID（→ module/level/*.yml）

swordsman:                        # 路由节点（key = 职业 ID）
  skill:
    tree: warrior_vanguard        # 技能树 ID（→ skilltree/*.yml）
  branch: [blade-master, grand-master]

blade-master:
  condition:                      # 转职条件
    cost:
      if: "getBalance() >= 500"
      message: "\"需要500金币\""
      post: "takeMoney(500)"
```

### 玩家选择职业流程

```
/pl route open → Router 选择 UI → 点击
  → PlayerSetRouteEvent.Pre（可取消）
  → Database 创建 PlayerRoute（parentId=-1 根职业）
  → template.route = newRoute
    → 创建 PlayerRouter（等级/经验）
    → 绑定 SkillTree（后续 learn/upgrade 依赖此树）
  → PlayerSetRouteEvent.Post
```

选择职业后，`PlayerRoute` 提供 `skillTree.learn()` / `skillTree.upgrade()` / `getBranches()` 等运行时能力。转职时 `parentId` 链接旧路线形成职业路径链，等级经验继承。

### 等级算法 (module/level/*.yml)

```yaml
def0:
  min: 1
  max: 100
  experience: |
    level <= 10 ? level * 200 :
    level <= 20 ? level * 600 :
    level * 3000
```

> 完整职业配置指南（字段表、绑定关系、运行时流程、转职、属性收集）参见 `references/yaml-reference.md#职业配置-jobyml`

---

## 六、属性系统

逻辑属性（STR/INT/AGI 等）通过 registry 映射为物理属性（ATK/DEF/MAX_HEALTH 等），由 AttributeProxy 编排多来源汇总后推送到外部属性插件。

### 来源优先级

| 优先级 | Source | 说明 |
|--------|--------|------|
| 0 | 基础 | PRIORITY_BASE |
| 5 | HookAttributeSource | 技能/职业 `hook.attributes` 自动收集 |
| 10 | 成长 | PRIORITY_GROWTH |
| 20 | 加点 | PRIORITY_INVESTED |
| 30 | 装备 | PRIORITY_EQUIP |
| 40 | Buff | PRIORITY_BUFF |
| 100 | 覆盖 | PRIORITY_OVERRIDE |

### 配置属性注册表 (config.yml)

```yaml
settings:
  attribute:
    registry:
      STR:
        name: "力量"
        mappings:
          ATK: 1.0
          DEF: 0.5
```

自动对实体调用 `AttributeProxy.sync(entity)` 时：收集来源 → 逻辑属性走 conversion → 合并物理直通 → 推送到 AttributeDriver。

---

## 七、DamageCause 伤害类型

```kotlin
DamageCause.of("SKILL")          // 查找（自定义→Bukkit 回退）
DamageCause.ofOrNull("MYTHIC")   // 安全查找
```

需在 `config.yml → settings.damage-causes` 中声明自定义类型。

JS 使用：
```javascript
damageEx(100, "SKILL")
damageExBy(100, "ATTRIBUTE", target)
```

---

## 八、目标查找器

```javascript
var targets = finder()
    .range(5)
    .type("ZOMBIE,SKELETON")    // 实体类型
    .excludeType("player")      // 排除类型
    .includeSelf()              // 包含自身
    .sector(5, 90, 0)           // 扇形（半径/角度/朝向）
    .limit(10)
    .sort("DISTANCE")           // NAME / DISTANCE / RANDOM
    .shuffle()
    .build()                    // → ProxyTargetContainer
```

---

## 九、config.yml 关键配置速查

### 核心配置块

| 块 | 用途 |
|----|------|
| `database` | LOCAL(SQLite) / SQL(MySQL) |
| `level` | 等级隔离模式、算法、原版同步 |
| `skill-points` | 每级获得点数公式 + 关键等级奖励 |
| `magic-point` | 法力恢复/上限公式 |
| `cooler` | 冷却存储（memory/persistence） |
| `damage-causes` | 声明自定义伤害类型 |
| `attribute.registry` | 逻辑→物理属性映射 |
| `keybinding` | 按键注册表 + 背包页配置 |
| `condition` | 通用条件池（技能树/转职引用） |
| `attack.protect` | 攻击保护场景 |
| `placeholder` | 占位符模式（script/literal） |

### 配置目录

```
resources/
├── config.yml
├── lang/zh_CN.yml
├── skill/*.yml        # 技能脚本
├── job/*.yml          # 职业
├── router/*.yml       # 职业路由
├── skilltree/*.yml    # 技能树
├── module/level/      # 经验算法
├── module/currency/   # 自定义货币
├── state/*.yml        # 状态效果
├── action/*.yml       # 自定义动作
└── ui/*.yml           # GUI（7 个）
```

> 完整 config.yml 参考（12 个配置块详解 + 目录结构）参见 `references/config-reference.md`

---

## 十、命令速查

| 分类 | 命令 | 说明 |
|------|------|------|
| 技能 | `/pl skill open <player>` | 技能操作 UI |
| 技能 | `/pl skill tree <player>` | 技能树 UI |
| 技能 | `/pl skill upgrade <player> <id>` | 技能升级 UI |
| 技能 | `/pl skill cast <player> <id>` | 释放（完整流程） |
| 技能 | `/pl skill run <player> <id> [lv]` | 执行脚本（绕过检查） |
| 背包 | `/pl backpack open <player>` | 背包 UI |
| 背包 | `/pl backpack page <player> <page>` | 切换页 |
| 职业 | `/pl route open/select/transfer/clear` | 职业操作 |
| 属性 | `/pl profile level/experience/magicpoint add/take/set <player> <value>` | 玩家属性 |
| 状态 | `/pl state trigger <player> <name>` | 触发状态 |
| 状态 | `/pl test <state> <duration>` | 自测状态 |
| 其他 | `/pl reload` | 重载配置 |
| 其他 | `/pl console cast <id> [lv]` | 控制台执行 |

根命令别名：`/pl`、`/ps`。权限：`planners.command`。

> 完整命令参考（28 条含补全说明）参见 `references/commands.md`

---

## 参考文档索引

| 文档 | 内容 |
|------|------|
| `references/yaml-reference.md` | 完整 YAML 结构（skill/job/router/skilltree/state/action/module）+ 职业配置完整指南 |
| `references/js-api.md` | 完整 JS API（30+ 函数、Finder、target 规则） |
| `references/events.md` | 完整事件列表（技能/玩家/背包/状态，含可取消说明） |
| `references/commands.md` | 完整命令树（28 条含补全） |
| `references/config-reference.md` | 完整 config.yml 参考（12 配置块 + 目录结构 + UI 列表） |

### 场景配置指南

| 文档 | 适用场景 |
|------|----------|
| `references/scenario-skill.md` | 配置/查看/修改技能 — YAML 完整字段、图标模板、When 条件变量、被动/光环/投射模式 |
| `references/scenario-skilltree.md` | 配置/查看/修改技能树 — 节点条件、graph 前置依赖、内置条件类型、自定义条件、学习/升级流程 |
| `references/scenario-router.md` | 配置/查看/修改职业树 — Router/Route 结构、转职条件、职业路径链、经验算法绑定 |
