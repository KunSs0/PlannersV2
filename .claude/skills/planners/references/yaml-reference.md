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

```yaml
__option__:
  name: 战士
  skill:                          # 该职业包含的技能 ID 列表
    - sword-slash
    - blade-storm
  variables:                      # 职业级变量（同技能变量系统）
    atkBonus: "level * 2"
  hook:
    attributes:                   # 职业级属性（Map 结构）
      STR: "10 + level * 2"
```

玩家选择职业后，其下所有 skill 自动创建 PlayerSkill 实例。

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
