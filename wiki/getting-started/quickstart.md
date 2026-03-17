# 快速入门

本指南带你从零完成一个最小可用的配置：创建「战士」职业，配一个「裂地斩」技能，绑定按键，在游戏中释放。

> 所有配置文件都在 `plugins/Planners/` 目录下。

---

## 第一步：创建职业

在 `job/` 目录下创建文件 `warrior.yml`：

```yaml
__option__:
  # 职业名称，会显示在 UI 中
  name: 战士
  # 这个职业拥有的技能列表
  # 每个值对应 skill/ 目录下的文件名（不含 .yml）
  skill:
    - ground_slash
```

**解释**：
- `__option__` 是固定写法，所有职业配置都必须写在这个节点下
- `name` 是职业的显示名称，玩家在 UI 中看到的就是这个名字
- `skill` 是一个列表，里面填写技能的 ID（就是技能文件的文件名，不带 `.yml` 后缀）

---

## 第二步：创建技能

在 `skill/` 目录下创建文件 `ground_slash.yml`：

```yaml
__option__:
  # 技能名称
  name: "裂地斩"

  # 技能图标，在技能面板 UI 中显示
  icon-formatter:
    material: DIAMOND_SWORD
    name: "裂地斩"
    lore:
      - "技能等级 {{level}}"
      - "技能伤害 {{32 * level + 100}}"

  # 技能变量
  # 这些变量可以在下面的脚本中通过变量名直接使用
  variables:
    cooldown: 100
    damage: 32 * level + 100

  # 升级条件
  # "0-100" 表示等级 0 到 100 都使用这个条件
  # "money" 对应 module/currency/ 中定义的货币 ID
  upgrade:
    condition:
      0-100:
        money: 100.0 * level + 32.5

  # 属性挂钩（需要 AttributePlus 插件）
  hook:
    attributes:
      - "攻击力 +${10 * level}"

  # 初始等级（玩家学会技能时的等级）
  started-level: 1
  # 最大等级
  max-level: 10

# 技能脚本（标准 JavaScript）
action: |
  function main() {
    // 发送消息给释放者
    tell("技能释放成功")

    // 等待 1.5 秒（1500 毫秒）
    sleep(1500)

    // 设置技能冷却为 200 tick（10 秒）
    // "skill" 是内置变量，代表当前技能对象
    setCooldown(skill, 200)
  }

  // 当技能命中目标时调用
  function handleHit() {
    // "target" 是内置变量，代表被命中的目标
    tell("on hit " + target)
  }
```

**解释**：
- `icon-formatter` 中的 `{{表达式}}` 会被自动计算。`level` 是技能等级
- `variables` 中定义的变量，在脚本和图标 lore 中都可以直接用
- `action` 是技能的核心逻辑，使用标准 JavaScript 语法
- `function main()` 是技能释放时自动调用的入口函数
- `function handleHit()` 是命中目标时的回调函数

---

## 第三步：创建路由

路由定义了职业的选择和转职路线。在 `router/` 目录下创建文件 `fighter.yml`：

```yaml
__option__:
  # 路由名称
  name: "战斗系"
  # 初始职业（玩家选择这个路由时，默认进入的职业）
  # 值对应下面定义的节点名
  originate: warrior
  # 使用的等级算法（对应 module/level/ 中的定义）
  algorithm:
    level: def0
  # 路由图标
  icon:
    material: DIAMOND_SWORD
    name: "职业: 战斗系"
    lore:
      - "&7选择战斗系职业"

# 初始职业节点
warrior:
  # 可以转职到哪些职业（留空表示没有后续转职）
  branch: []
```

**解释**：
- `originate` 指定玩家选择这个路由后，默认进入哪个职业
- `branch` 定义转职分支，后面会在 [路由与转职](../config/router.md) 中详细说明

---

## 第四步：配置按键

编辑 `key-binding.yml`：

```yaml
# 按键 ID（唯一标识）
key0:
  # 按键显示名称（在 UI 中显示）
  name: R
  # 匹配规则：strict = 严格匹配，fuzzy = 模糊匹配
  matching-type: strict
  # 组合键输入超时时间（单位 tick，20 tick = 1 秒）
  # 对于单个按键无效，只在组合键时生效
  request-tick: 30
  # 实际映射的按键
  mapping: R
```

玩家可以在游戏内通过技能面板 UI 将技能绑定到这个按键上。

---

## 第五步：测试

### 方法一：使用命令

```
# 重载配置
/planners reload

# 选择职业路由
/planners route select fighter

# 打开技能面板
/planners skill open

# 直接释放技能
/planners skill cast ground_slash

# 让指定玩家释放技能（管理员用）
/planners skill run Steve ground_slash 1
```

### 方法二：通过 UI

1. 输入 `/planners route open` 打开职业选择界面
2. 选择「战斗系」路由
3. 输入 `/planners skill open` 打开技能面板
4. 在面板中将「裂地斩」绑定到 R 键
5. 按 R 键释放技能

---

## 完整的技能示例

下面是一个更实际的技能，展示了目标查找、伤害、声音、药水效果等功能：

```yaml
__option__:
  name: "旋风斩"
  icon-formatter:
    material: IRON_SWORD
    name: "&6旋风斩 Lv.{{level}}"
    lore:
      - "&7对周围 8 格内的怪物造成伤害"
      - ""
      - "&7伤害: &f{{20 * level + 50}}"
      - "&7冷却: &f5 秒"
      - ""
      - "&e当前等级: {{level}}/10"
  variables:
    damage: 20 * level + 50
  started-level: 1
  max-level: 10

action: |
  function main() {
    // 查找周围 8 格内的非玩家实体，最多 5 个
    var targets = finder().range(8).excludeType("player").limit(5).build()

    // 对目标造成伤害
    damage(20 * level + 50, targets)

    // 播放声音
    sound("ENTITY_PLAYER_ATTACK_SWEEP", 1.0, 0.8)

    // 给目标施加缓慢效果（等级 1，持续 60 tick = 3 秒）
    potion("SLOW", 1, 60, targets)

    // 设置冷却 100 tick = 5 秒
    setCooldown(skill, 100)

    // 发送消息
    tell("&6旋风斩命中！")
  }
```

---

## 下一步

- [主配置详解](../config/main-config.md) — 了解 config.yml 的所有选项
- [技能配置](../config/skill.md) — 技能的完整配置参考
- [脚本引擎概述](../scripting/overview.md) — 学习 JavaScript 脚本系统
- [目标查找器](../scripting/target-finder.md) — 学习如何选取目标
