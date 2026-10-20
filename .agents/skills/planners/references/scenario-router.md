# 场景：配置/查看/修改职业树（Router）

## 场景概述

Router（职业树/路由）定义了一条**职业路线图**：玩家从哪个职业起步、能转职到哪些分支、每个职业绑定什么技能树。

| 操作 | 文件 | 说明 |
|------|------|------|
| 新建职业系 | `router/<系id>.yml` | 创建 Router + Route 节点 |
| 修改起始职业 | `router/<系>.yml` → `__option__.originate` | 改初始职业 |
| 新增转职分支 | `router/<系>.yml` → 新增 route 节点 | 扩展职业路线 |
| 绑定技能树 | `router/<系>.yml` → `<route>.skill.tree` | 关联 skilltree |
| 修改经验算法 | `router/<系>.yml` → `__option__.algorithm.level` | 切换或调整算法 |

> **注意**：转职条件校验当前**未实现**。`ImmutableRoute` 没有 condition 字段，`PlayerRouteTransferUI` 直接执行转职不做校验。详见文末"转职条件现状"。

---

## Router 与 Job 的关系

```
Router (职业系)
  │
  ├── __option__.originate → 起始 Route
  │     └── ID = swordsman → job/swordsman.yml
  │           ├── __option__.skill[] → 技能列表
  │           ├── __option__.hook.attributes → 职业属性
  │           └── __option__.variables → 职业变量
  │
  ├── blade-master (转职分支)
  │     ├── ID = blade-master → job/blade-master.yml
  │     └── branch: [grand-master] → 下一级分支
  │
  └── grand-master (终级分支)
        └── ID = grand-master → job/grand-master.yml
```

**核心约定**：Route 节点的 key = Job 文件名 = ImmutableJob ID。三个 ID 必须一致。

---

## Router YAML 完整结构

```yaml
# ── 路由器元信息 ──
__option__:
  name: "战士系"                  # 必填，职业系显示名
  originate: swordsman            # 起始职业 ID（= job 文件名）
  algorithm:
    level: def0                   # 经验算法 ID（引用 module/level/*.yml）
  icon:                           # 可选，Router 选择界面的图标
    material: DIAMOND_SWORD
    name: "职业: 战士"
    lore:
      - '&f- &a基础数值'
      - "&f攻击： &b|||||"

# ── 路由节点（每个节点 = 一个职业）──
<route-id>:                       # key = 职业 ID（= job 文件名）
  skill:
    tree: <skilltree-id>          # 绑定的技能树 ID（→ skilltree/*.yml）
  branch:                         # 可转职的子职业 ID 列表
    - blade-master
    - grand-master
  icon:                           # 可选，转职界面的图标
    material: DIAMOND_SWORD
    name: "职业: 剑魂"
    lore:
      - '&f- &a基础数值'
```

### 字段速查表

| 字段路径 | 类型 | 必填 | 说明 |
|----------|------|------|------|
| `__option__.name` | String | 是 | 职业系显示名 |
| `__option__.originate` | String | 是 | 起始职业 ID。如果是字符串则按 ID 查找 route；否则取 routes 的第一个 |
| `__option__.algorithm.level` | String | 否 | 经验算法 ID，引用 `module/level/*.yml` |
| `__option__.icon` | ItemStack | 否 | Router 选择界面图标 |
| `<route>.skill.tree` | String | 否 | 绑定的 SkillTree ID |
| `<route>.branch` | String / List\<String\> | 否 | 可转职的职业 ID（单个字符串或列表） |
| `<route>.icon` | ItemStack | 否 | 转职界面图标 |

**代码实现**（`ImmutableRoute.kt`）：
```kotlin
class ImmutableRoute(private val parent: ImmutableRouter, private val config: ConfigurationSection) {
    val routerId = parent.id
    val id = config.name
    val icon = config.getItemStack("icon")
    val skillTree: String? = config.getString("skill.tree")
    private val branches = if (config.isString("branch")) {
        listOf(config.getString("branch")!!)
    } else {
        config.getStringList("branch")
    }
    // 没有 condition 字段！转职无条件执行
}
```

---

## 等级经验算法

`__option__.algorithm.level` 引用 `module/level/*.yml` 中的算法 ID。如果不指定，取全局默认。

```yaml
# module/level/def0.yml
def0:
  min: 1
  max: 100
  experience: |
    level <= 10 ? level * 200 :
    level <= 20 ? level * 600 :
    level * 3000
```

`Algorithm` 接口还支持 `LevelCallback`（升级回调），可配置升级时执行的命令或 JS 脚本。

---

## 最小可工作示例

```yaml
# router/test.yml
__option__:
  name: "测试系"
  originate: test_warrior

test_warrior:
  skill:
    tree: example_tree
  branch: test_elite

test_elite: {}
```

配套文件：
- `job/test_warrior.yml` — `__option__: { name: 测试战士, skill: [example0] }`
- `job/test_elite.yml` — `__option__: { name: 测试精英, skill: [example0, example1] }`
- `skilltree/example_tree.yml` — 最小技能树

---

## 完整示例：战士系（当前实际配置）

```yaml
# router/soldier.yml
__option__:
  name: "战士"
  originate: swordsman
  algorithm:
    level: def0
  icon:
    material: DIAMOND_SWORD
    name: "职业: 战士"
    lore:
      - '&f- &a基础数值'
      - "&f攻击： &b|||||"
      - "&f防御： &b|||||"
      - '&f- &a转职路线'
      - "    &7> &f剑魂"
      - "    &7> &f剑圣"
      - '&e难度： ||'

swordsman:
  skill:
    tree: warrior_vanguard
  branch:
    - blade-master
    - grand-master
  icon:
    material: DIAMOND_SWORD
    name: "职业: 剑士"

blade-master:
  branch: grand-master
  icon:
    material: DIAMOND_SWORD
    name: "职业: 剑魂"
    lore:
      - '&f- &a基础数值'
      - '&e难度： ||'

grand-master:
  icon:
    material: DIAMOND_SWORD
    name: "职业: 剑圣"
    lore:
      - '&f- &a基础数值'
      - '&e难度： ||'
```

**要点**：
- `swordsman` 有两个分支：`blade-master`（中转）和 `grand-master`（直达终极）
- `blade-master` 的 branch 是 `grand-master`（链式转职：剑士→剑魂→剑圣）
- `grand-master` 没有 branch（终点职业）
- 当前**所有转职无条件执行**——玩家点击即可转

---

## 玩家选择/转职流程

### 初次选择

```
/pl route open <player>
  → PlayerRouterSelectUI（列出所有 Router）
  → 玩家点击"战士系"
  → originate → 获取 "swordsman" ImmutableRoute
  → PlayerSetRouteEvent.Pre（可取消）
  → ProfileOperator.createPlayerRoute(template, element)
    → Database.createPlayerJob(template, parentId=-1, route)
      → INSERT route(row: user, router=soldier, parent=-1, route=swordsman)
  → template.route = PlayerRoute(bindingId, "soldier", Node(-1, "swordsman"))
    → 创建/加载 PlayerRouter（等级=1, 经验=0）
    → 关联 SkillTree("warrior_vanguard")
  → PlayerSetRouteEvent.Post
```

### 转职（当前实现：无条件）

```
/pl route transfer <player>
  → PlayerRouteTransferUI
  → 显示 route.getBranches() = [blade-master, grand-master]
  → 玩家点击目标职业
  → （无校验！直接调用 createPlayerRoute）
  → Database.createPlayerJob(template, parentId=旧route.bindingId, 新route)
    → INSERT route(row: parent=旧ID, route=blade-master)
    → 形成职业路径链：swordsman → blade-master
  → template.route = 新 PlayerRoute
    → SkillTree 自动切换为 blade-master 绑定的树
  → PlayerSetRouteEvent.Post
```

关键代码（`PlayerRouteTransferUI.kt`）：
```kotlin
override fun onClick(event: ClickEvent, element: ImmutableRoute) {
    val player = event.clicker
    val template = player.plannersTemplate
    // 无条件！直接创建新 route
    PlayerTemplateAPI.OPERATOR.createPlayerRoute(template, element).thenAccept { newRoute ->
        template.route = newRoute
        player.sendLang("player-transfer-success", element.getJob().name)
    }
}
```

### 清空职业

```
/pl route clear <player>
  → template.route = null
  → route setter 检测 null → 删除所有已学技能的数据库记录
```

---

## 转职条件现状

### 当前状态

**转职条件未实现**。`ImmutableRoute` 只解析 `icon`、`skill.tree`、`branch` 三个字段，没有 condition。`PlayerRouteTransferUI` 不做任何校验，玩家点击即可转职。

### 正确做法：走通用 condition 池

转职条件应和 skilltree 一致，引用 `config.yml → settings.condition` 中的条件 key，而非另搞一套内联 JS 表达式。

**config.yml 中已有可复用的条件**：

```yaml
settings:
  condition:
    player_lv:                              # 等级检查
      exper: "profile.getLevel() >= props.min"
      props: { min: 1 }
      hint: "需要等级{props.min}"

    is_warrior:                             # 职业检查
      exper: "route != null && route.getJob().getId() == 'warrior'"
      props: {}
      hint: "需要战士职业"

    # 如需转职专用条件，在此新增即可：
    # need_gold:
    #   exper: "getBalance() >= props.amount"
    #   props: { amount: 0 }
    #   hint: "需要{props.amount}金币"
    #   consume: "takeMoney(props.amount)"
```

**router YAML 中应按 skilltree 格式引用**（待实现）：

```yaml
blade-master:
  skill:
    tree: warrior_blade
  branch: grand-master
  condition:                    # 引用通用条件池中的 key
    player_lv: { min: 30 }      # 走 config.yml → settings.condition.player_lv
    is_warrior: {}              # 走 config.yml → settings.condition.is_warrior
```

这与 skilltree 的 `levels.N.condition` 完全一致的键引用模式——条件 key 来自全局池，props 可覆盖默认值，`ConditionEvaluator` 统一做 verify + consume。

---

## 修改检查清单

配置 Router 时按顺序检查：

1. `__option__.originate` 对应的 Route 节点是否存在
2. 每个 Route 节点的 key 是否有对应的 `job/<key>.yml` 文件
3. 每个 Route 的 `skill.tree` ID 是否有对应的 `skilltree/<id>.yml`
4. `branch` 中引用的 Route ID 是否在同一个 Router 文件中存在
5. 经验算法 ID 是否在 `module/level/` 中存在
6. 路由文件在 `Registries.ROUTER` 初始化时指定的路径下（`router/`）
7. 转职条件如需添加，先在 `config.yml → settings.condition` 注册条件，再在 router 中按 key 引用（待实现）

---

## 关联配置

| 配置 | 关系 |
|------|------|
| `job/*.yml` | 每个 route key 对应一个 job 文件 |
| `skilltree/*.yml` | `skill.tree` 绑定 |
| `skill/*.yml` | job 的 `__option__.skill[]` 引用 |
| `module/level/*.yml` | `algorithm.level` 引用 |
| `config.yml` → `settings.condition` | 转职条件池（和 skilltree 共用） |
| `config.yml` → `settings.skill-points` | 技能点公式 |
| `config.yml` → `settings.level.isolation` | 等级隔离模式（all/router/job） |

> 职业 YAML 完整字段、运行时属性收集、变量系统参见 `references/yaml-reference.md#职业配置-jobyml`
