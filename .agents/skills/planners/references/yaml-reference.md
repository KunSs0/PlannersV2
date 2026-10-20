# YAML 配置结构参考

## 技能配置 (skill/*.yml)

```yaml
__option__:
  name: "技能名"                    # 必填，显示名
  async: true                      # 异步执行，默认 true（sleep 需要异步）
  started-level: 0                 # 起始等级，默认 0
  max-level: 10                    # 最大等级，默认 10
  category: "*"                    # 分类

  # 图标（支持 {{expression}} 模板）
  display:
    icon:
      material: DIAMOND_SWORD
      name: "技能名"
      lore:
        - "等级 {{level}}"
        - "伤害 {{damage}}"

  # 变量（cooldown 和 mp 有特殊含义）
  variables:
    cooldown: "100"                # 冷却(ticks)，cast() 自动设置
    mp: "50"                       # 法力消耗，cast() 自动扣除
    damage: "32 * level + 100"     # 自定义变量，无特殊行为
    configList: [1, 2, 3]          # 列表变量 → When 条件分支

  # 属性钩子（Map 结构，值为 JS 表达式）
  hook:
    attributes:
      STR: "10 + level * 2"

# JS 脚本（必须有 main 函数）
action: |
  function main() {
    tell("释放技能!")
    damage(variables.damage)
    setCooldown(skill, 200)
  }
```

### When 条件变量

列表格式触发条件分支，按 condition 匹配第一个满足的项：

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

### icon 模板变量

`display.icon.name` 和 `display.icon.lore` 支持 `{{variable}}` 模板，变量来自 `variables` 段。运行时自动替换。

---

## 职业配置 (job/*.yml)

### 完整字段

```yaml
__option__:
  name: 战士                       # 必填，职业显示名
  skill:                           # 必填，该职业包含的技能 ID 列表
    - slash
    - charge
    - shield_bash
  variables:                       # 可选，职业级变量（JS 表达式，level=职业等级）
    atkBonus: "level * 2"
    defBonus: "10"
  hook:
    attributes:                    # 可选，职业级属性（Map 结构，值为 JS 表达式）
      STR: "10 + level * 2"
      MAX_HEALTH: "level * 20"
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `name` | 是 | 职业显示名 |
| `skill` | 是 | 技能 ID 列表，引用 `skill/*.yml` 文件名 |
| `variables` | 否 | 职业级变量，JS 表达式，`level` = 职业等级 |
| `hook.attributes` | 否 | 职业级属性加成，key=属性名，value=JS 表达式 |

### 职业与路由/技能树的绑定关系

```
router/*.yml (ImmutableRouter)
  ├── __option__.originate → 起始职业 ID（= job 文件名）
  └── <route-id>:           → 路由节点（ID 即职业 ID）
        ├── skill.tree      → 绑定的 SkillTree ID（→ skilltree/*.yml）
        └── branch          → 可转职的子职业 ID 列表

job/*.yml (ImmutableJob)
  └── __option__.skill[]    → 该职业可用的技能 ID 列表

skilltree/*.yml (ImmutableSkillTree)
  └── nodes.<skill-id>      → 每个技能的逐级学习条件 + graph 前置依赖
```

**关键规则**：
- `router` 文件名 = ImmutableRouter ID
- `router` 中每个路由节点 key = route ID = job 文件名（= ImmutableJob ID）
- `router.__option__.originate` = 起始职业 ID（玩家初次选择时获得）
- 每个 route 节点的 `skill.tree` 绑定一个 SkillTree，控制技能学习/升级条件
- job 的 `skill` 列表声明该职业"拥有"哪些技能，但玩家需要通过 SkillTree 学习后才能使用

### 玩家选择职业的运行时流程

```
1. /pl route open → PlayerRouterSelectUI（列出所有 Router）
2. 玩家点击 → getOriginate() → 获取起始 ImmutableRoute
3. PlayerSetRouteEvent.Pre（可取消）
4. Database.createPlayerJob(template, parentId=-1, route)
   → INSERT route 表（user, router, parent=-1, route）
   → 创建 PlayerRoute 实例
5. template.route = newRoute
   → 创建/加载 PlayerRouter（等级+经验）
   → 异步写入数据库
6. PlayerSetRouteEvent.Post
```

### PlayerRoute 内部结构

选择职业后，`PlayerRoute` 提供运行时能力：

| 方法/属性 | 说明 |
|-----------|------|
| `getJob()` | 返回 ImmutableJob，可访问 name/variables/skills |
| `skillTree` | 返回 SkillTree 内部类，提供 learn()/upgrade()/canLearn()/canUpgrade() |
| `getImmutableSkillValues()` | 该职业下所有 ImmutableSkill（从 Registries.SKILL 过滤） |
| `getRegisteredSkill()` | 玩家已学的 PlayerSkill Map |
| `skillPointsCurrent` | 当前可用技能点 |
| `getBranches()` | 可转职的子职业列表 |
| `getVariables()` | 代理到 job.getVariables() |

### 转职流程

```
1. /pl route transfer → PlayerRouteTransferUI
2. 显示当前 route.getBranches()（转职分支列表）
3. 玩家选择目标 → 校验 condition.cost（如金币、等级）
4. Database.createPlayerJob(template, parentId=旧route.bindingId, 新route)
   → parentId 链接到旧路线，形成职业路径链
5. template.route = newRoute
   → PlayerRouter 保留（等级经验继承）
6. PlayerSetRouteEvent.Post
```

**转职条件**在 router YAML 的 route 节点下定义：
```yaml
blade-master:
  condition:
    cost:
      if: "getBalance() >= 500"    # JS 条件
      message: "\"需要500金币\""    # 失败提示
      post: "takeMoney(500)"       # 成功后执行
```

### Job 属性收集机制

`HookAttributeSource`（priority=5）在 `AttributeProxy.sync(entity)` 时自动收集：

```
1. 获取 template.route → PlayerRoute → ImmutableJob
2. 遍历 job.hook.attributes，对每个 key→expr 用 JS eval 求值
3. 遍历已学技能的 skill.hook.attributes，同名 key 累加
4. 返回 Map<String, Double> → 汇入 AttributeProxy 管道
```

JS eval 时可用的上下文变量：`level`（职业等级）、`sender`（实体）、`profile`（PlayerTemplate）。

---

## 路由配置 (router/*.yml)

```yaml
__option__:
  name: "战士系"
  originate: swordsman            # 起始职业 ID
  algorithm:
    level: def0                   # 引用 module/level/*.yml 的算法 ID
  icon:
    material: DIAMOND_SWORD
    name: "职业: 战士"
    lore: ["描述文本"]

swordsman:                        # 路由节点（ID = 职业 ID）
  skill:
    tree: warrior_vanguard        # 引用 skilltree/*.yml 的 ID
  branch:                         # 可晋升的分支职业
    - blade-master
    - grand-master
  icon: { ... }

blade-master:
  condition:                      # 转职条件
    cost:
      if: "getBalance() >= 500"   # JS 条件表达式
      message: "\"需要500金币\""   # 失败提示
      post: "takeMoney(500)"      # 成功后执行
  icon: { ... }
```

---

## 技能树配置 (skilltree/*.yml)

定义技能节点的升级条件和依赖图。每个节点对应一个技能，逐级定义条件：

```yaml
nodes:
  slash:                          # 节点 ID（对应技能 ID）
    lv-1:                         # 第 1 级条件
      condition:
        player_lv: { min: 1 }
        consume_sp: { amount: 1 }
    lv-2:
      condition:
        player_lv: { min: 5 }
        consume_sp: { amount: 2 }
    # ... lv-N
  charge:
    lv-1:
      condition: { ... }
    graph:                        # 前置依赖（必须先解锁）
      - slash
```

条件 ID（如 `player_lv`、`consume_sp`）引用 `config.yml → settings.condition` 池。

---

## 状态效果配置 (state/*.yml)

```yaml
state_id:
  priority: 0          # 优先级
  max-layer: 3         # 最大叠加层数
  name: "眩晕"
  static: false        # true=永久不自动移除
  action: |
    function onStateMount() { ... }     # 首次附加
    function onStateAttach() { ... }    # 每层附加
    function onStateDetach() { ... }    # 每层移除
    function onStateClose() { ... }     # 全部移除
    function onStateEnd() { ... }       # 自然到期
```

状态回调用可用变量: `sender`（实体）、`event`（事件）、`state`（状态对象）。

---

## 等级经验算法 (module/level/*.yml)

```yaml
def0:                              # 算法 ID（router 中引用）
  min: 1                           # 最低等级
  max: 100                         # 最高等级
  experience: |                    # 升级经验（JS 三目表达式，level 为变量）
    level <= 10 ? level * 200 :
    level <= 20 ? level * 600 :
    level * 3000
```

---

## 自定义货币 (module/currency/*.yml)

```yaml
money:
  name: '金币'
  action:
    hook: getBalance()             # 查询
    withdraw: takeBalance(arg)     # 扣除
    deposit: giveBalance(arg)      # 存入
    set: setBalance(arg)           # 设置
```

---

## 自定义动作 (action/*.yml)

```yaml
__option__:
  id: "example"
  format: [type, uuid]            # 参数列表
  props:                          # 可选参数及默认值
    - { id: title, default: "abc" }
    - { id: tick, default: 100 }
action: |-                        # JS 脚本，入参为 format + props
  tell(type + ":" + uuid + " title = " + title)
```
