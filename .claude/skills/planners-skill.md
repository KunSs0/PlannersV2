---
name: planners-skill
description: 编写 Planners 插件技能配置和脚本的参考指南。涵盖技能/职业/路由 YAML 配置、JS 脚本 API、变量系统、背包系统、状态效果、升级条件等。
---

# Planners 技能编写指南

## 概述

Planners 是一个 Bukkit RPG 技能/职业插件。技能通过 **YAML 配置文件**定义元数据，通过 **JavaScript 脚本**实现逻辑。

### 核心概念链

```
config.yml (keymapping + backpack)
    ├── skill/*.yml → ImmutableSkill (技能定义 + JS脚本)
    ├── job/*.yml → ImmutableJob (职业=技能集合 + 属性)
    └── router/*.yml → ImmutableRouter (职业树/转职路线)

玩家选择 Router → 获得 Job → 学习 Job 下所有 Skill → 将技能装入 Backpack 槽位 → 按键触发释放
```

---

## 一、技能 YAML 配置 (skill/*.yml)

### 完整结构

```yaml
__option__:
  name: "技能名"                    # 必填
  async: true                      # 异步执行，默认true（sleep需要异步）
  started-level: 0                 # 起始等级，默认0
  max-level: 10                    # 最大等级，默认10
  category: "*"                    # 分类

  # 图标 (支持 {{expression}} 模板)
  icon-formatter:
    material: DIAMOND_SWORD
    name: "技能名"
    lore:
      - "等级 {{level}}"
      - "伤害 {{damage}}"

  # 变量 (cooldown 和 mp 有特殊含义)
  variables:
    cooldown: "100"                # 冷却(ticks)，cast()时自动设置
    mp: "50"                       # 法力消耗，cast()时自动扣除
    damage: "32 * level + 100"     # 自定义变量，无特殊行为
    configList: [1, 2, 3]          # 列表变量 → When条件分支(见下文)

  # 升级条件
  upgrade:
    condition:
      0-100:                       # 等级范围 begin-end
        money: "100.0 * level + 32.5"
        experience: "50 * level"
        mark: true                 # 标记为里程碑

  # 属性钩子 (${} 表达式)
  hook:
    attributes:
      - "攻击力 +${10 * level}"

# JS 脚本 (必须有 main 函数)
action: |
  function main() {
    tell("释放技能!")
    damage(variables.damage)
    setCooldown(skill, 200)
  }
```

### When 条件变量

列表格式触发条件分支机制：

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

### upgrade.condition 详解

每个等级区间的 cost 是 JS 表达式，可用变量 `level`：

| key | 含义 |
|-----|------|
| `money` | Vault 金币消耗 |
| `experience` | 经验消耗 |
| `mark` | 是否标记为里程碑 |
| 自定义 key | 对应 `Registries.CURRENCY` 中的货币 ID |

---

## 二、JS 脚本执行环境

### 执行流程（伪代码）

```
ImmutableSkill.execute(sender, level, extraVars)
  → ScriptOptions 创建，注入变量: sender, origin, level, skill, ctx, profile, 自定义变量
  → JsSession 打开
  → eval(action) —— 加载函数声明
  → invoke("main") —— 调用入口
  → session.close()
```

### 全局可用变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `sender` | ProxyTarget | 施法者（通常为 Player） |
| `origin` | Location | 施法原点位置 |
| `level` | int | 技能等级 |
| `skill` | ImmutableSkill | 技能定义对象 |
| `profile` | PlayerTemplate | 玩家完整数据（仅当 sender 为 Player） |
| `ctx` | SkillContext | 完整执行上下文 |
| `target` | ProxyTarget | 目标实体（由脚本流程动态设置） |
| `event` | Event | 状态回调时的事件对象 |

### skill 对象的可用属性

```
skill.id          — 技能 ID (文件名不含扩展名)
skill.name        — 技能名称
skill.getVariable("key")  — 获取变量值
```

---

## 三、完整 JS API 函数参考

### 通用

| 函数 | 说明 |
|------|------|
| `random(min, max)` | 随机整数 [min, max] |
| `sleep(ms)` | 暂停毫秒（仅 async=true 时可用） |
| `tell(msg)` / `tell(msg, targets)` | 发送消息 |

### 伤害与治疗

| 函数 | 说明 |
|------|------|
| `damage(amount)` / `damage(amount, targets)` | 技能伤害 (cause=SKILL) |
| `damageBy(amount, source)` / `damageBy(amount, source, targets)` | 指定来源伤害 |
| `damageEx(amount, cause)` / `damageEx(amount, cause, targets)` | 自定义 cause 伤害 |
| `damageExBy(amount, cause, source)` | 完整自定义伤害 |
| `heal(amount)` / `heal(amount, targets)` | 治疗 |

### 冷却

| 函数 | 说明 |
|------|------|
| `getCooldown(skill)` / `getCooldown(skill, player)` | 获取剩余冷却(ticks) |
| `setCooldown(skill, ticks)` / `setCooldown(skill, ticks, player)` | 设置冷却 |
| `resetCooldown(skill)` / `resetCooldown(skill, player)` | 重置冷却为0 |
| `hasCooldown(skill)` / `hasCooldown(skill, player)` | 是否在冷却中 |

`skill` 参数可以是技能 ID 字符串或 skill 对象。

### 生命值

| 函数 | 说明 |
|------|------|
| `healthAdd(amount)` / `healthAdd(amount, targets)` | 增加生命（不超过上限） |
| `healthSet(amount)` / `healthSet(amount, targets)` | 直接设置生命 |
| `healthTake(amount)` / `healthTake(amount, targets)` | 扣除生命（不低于0） |

### 实体操作

| 函数 | 说明 |
|------|------|
| `entitySpawn(type)` / `entitySpawn(type, duration)` / `entitySpawn(type, duration, locations)` | 生成实体，duration 后自动移除 |
| `entityRemove()` / `entityRemove(targets)` | 移除实体 |
| `entityTeleport(x, y, z)` / `entityTeleport(x, y, z, targets)` | 传送 |
| `entityTeleportTo(destinations)` / `entityTeleportTo(targets, destinations)` | 传送到目标 |
| `entitySetAI(bool)` / `entitySetAI(bool, targets)` | AI 开关 |
| `entitySetGravity(bool)` / `entitySetGravity(bool, targets)` | 重力开关 |
| `entitySetInvulnerable(bool)` / `entitySetInvulnerable(bool, targets)` | 无敌 |
| `entitySetGlowing(bool)` / `entitySetGlowing(bool, targets)` | 发光 |
| `entitySetSilent(bool)` / `entitySetSilent(bool, targets)` | 静音 |

### 药水效果

| 函数 | 说明 |
|------|------|
| `potion(type, level, duration)` / `potion(type, level, duration, targets)` | 施加药水效果 |
| `potionRemove(type)` / `potionRemove(type, targets)` | 移除药水效果 |

`type` 为 Bukkit PotionEffectType 枚举名，`level` 为等级(1-indexed)，`duration` 为 ticks。

### 特效

| 函数 | 说明 |
|------|------|
| `freeze(ticks)` / `freeze(ticks, targets)` | 设置冻结 |
| `fire(ticks)` / `fire(ticks, targets)` | 设置着火 |
| `explosion(power)` / `explosion(power, fire, breakBlocks, targets)` | 创建爆炸 |

### 音效

| 函数 | 说明 |
|------|------|
| `sound(name)` / `sound(name, volume, pitch, targets)` | 播放 Bukkit Sound |
| `soundResource(name)` / `soundResource(name, volume, pitch, targets)` | 播放资源包音效 |

### 指令

| 函数 | 说明 |
|------|------|
| `command(cmd)` / `command(cmd, targets)` | 以目标身份执行 |
| `commandOp(cmd)` / `commandOp(cmd, targets)` | 以 OP 身份执行 |
| `commandConsole(cmd)` | 以控制台执行 |

### 弹射物

| 函数 | 说明 |
|------|------|
| `projectile(type)` / `projectile(type, speed, targets)` | 朝目标方向发射 |
| `projectileAt(type, x, y, z, speed)` | 朝固定向量发射 |
| `projectileToward(type, speed, sources, dests)` | 朝目标位置发射 |

支持的 type: ARROW, FIREBALL, LARGE_FIREBALL, SMALL_FIREBALL, DRAGON_FIREBALL, WITHER_SKULL, SNOWBALL, EGG, ENDER_PEARL, TRIDENT, SPECTRAL_ARROW, SHULKER_BULLET, LLAMA_SPIT

### 速度

| 函数 | 说明 |
|------|------|
| `velocitySet(x, y, z)` / `velocitySet(x, y, z, targets)` | 设置速度向量 |
| `velocityAdd(x, y, z)` / `velocityAdd(x, y, z, targets)` | 叠加速度 |
| `velocityMove(x, y, z)` / `velocityMove(x, y, z, targets)` | 相对朝向移动 |
| `velocityZero()` / `velocityZero(targets)` | 速度归零 |
| `getVelocity(entity)` | 获取速度向量 |

### 状态效果

| 函数 | 说明 |
|------|------|
| `stateAttach(id, duration)` / `stateAttach(id, duration, refresh, targets)` | 附加状态 |
| `stateDetach(id)` / `stateDetach(id, layer, targets)` | 移除N层状态 |
| `stateRemove(id)` / `stateRemove(id, targets)` | 完全移除状态 |
| `stateHas(id)` / `stateHas(id, targets)` | 检查是否有某状态 |

### 目标查找器

```
var targets = finder()
    .range(5)          // 半径
    .includeSelf()     // 包含自身
    .type("ZOMBIE,SKELETON")  // 实体类型筛选
    .excludeType("player")    // 排除类型
    .name("pattern")   // 名称正则
    .inWorld("world")  // 世界筛选
    .sector(radius, angle, yaw)  // 扇形区域
    .limit(10)         // 数量限制
    .sort("DISTANCE")  // 排序 (NAME/DISTANCE/RANDOM)
    .sortReverse()     // 反向排序
    .shuffle()         // 随机打乱
    .build()           // 返回 ProxyTargetContainer
```

### Metadata

| 函数 | 说明 |
|------|------|
| `hasMeta(key, entity)` | 检查存在 |
| `getMeta(key, entity)` | 获取值 |
| `setMeta(key, value, entity)` | 设置 |
| `setMetaTimeout(key, value, ticks, entity)` | 设置（自动过期） |
| `removeMeta(key, entity)` | 移除 |

### 经济 (Vault)

| 函数 | 说明 |
|------|------|
| `getBalance()` / `getBalance(player)` | 查询余额 |
| `takeMoney(amount)` / `takeMoney(amount, player)` | 扣款 |
| `giveMoney(amount)` / `giveMoney(amount, player)` | 加款 |
| `setMoney(amount)` / `setMoney(amount, player)` | 设置余额 |

### 属性 (AttributeDriver)

| 函数 | 说明 |
|------|------|
| `getAttr(name)` / `getAttr(name, targets)` | 读取属性值 |

### target 参数规则

大多数函数的 `targets` 参数是可选尾参。省略时的默认查找顺序：

1. 如果存在 `target` 变量 → 使用之
2. 如果 sender 是 entity → 使用 sender
3. 否则 → 使用 origin 位置
4. 某些函数会 fallback 到所有在线玩家或控制台

---

## 四、技能释放流程

### PlannersAPI.cast(player, PlayerSkill) 流程

```
                    ┌─────────────────┐
                    │ 玩家按背包槽位键  │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ CombinedAnalyzer│
                    │ 匹配按键序列     │
                    └────────┬────────┘
                             │ CombinedEvent.Close
                    ┌────────▼────────┐
                    │ BackpackAPI     │
                    │ getSkillByKey() │
                    │ 查当前页槽位技能 │
                    └────────┬────────┘
                             │ skill != null
                    ┌────────▼────────┐
                    │ PlayerSkillCast │
                    │ Event.Pre       │──取消→ CANCEL_WITH_EVENT
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Cooler 检查      │──冷却→ COOLING
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ 计算 mp 变量     │──不足→ MAGICPOINT_INSUFFICIENT
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ 设置冷却         │
                    │ 扣除法力         │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ skill.execute() │
                    │ → function main │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ PlayerSkillCast │
                    │ Event.Post      │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ SUCCESS         │
                    └─────────────────┘
```

### PlannersAPI.cast(player, ImmutableSkill, level)

绕过冷却/MP 检查直接执行，用于管理员测试或特殊触发。

---

## 五、职业与路由

### 职业 YAML (job/*.yml)

```yaml
__option__:
  name: 战士
  skill:                          # 该职业包含的技能 ID 列表
    - sword-slash
    - blade-storm
  variables:                      # 职业级变量（同技能变量系统）
    atkBonus: "level * 2"
  hook:
    attributes:                   # 职业级属性
      - "力量 +5"
```

玩家选择职业后，其下的所有 skill 会自动创建 PlayerSkill 实例（学习）。

### 路由 YAML (router/*.yml)

```yaml
__option__:
  name: "战士系"
  originate: swordsman            # 起始职业
  algorithm:
    level: def0                   # 等级算法 ID

swordsman:                        # 路由节点 (ID = 职业 ID)
  branch:                         # 可晋升的分支
    - blade-master
    - grand-master
  icon: { ... }

blade-master:
  condition:                      # 转职条件
    cost:
      if: "getBalance() >= 500"   # JS 条件表达式
      message: "\"需要500金币\""   # 提示消息
      post: "takeMoney(500)"      # 成功后执行
  icon: { ... }
```

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

---

## 六、状态效果系统 (state/*.yml)

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

状态回调用可用变量: `sender`(实体), `event`(事件), `state`(状态对象)

---

## 七、按键与背包系统

### 按键注册 (config.yml → settings.keybinding.keymapping)

```yaml
key0: { name: R, matching-type: strict, request-tick: 30, mapping: R }
```

| 字段 | 说明 |
|------|------|
| `name` | 显示名 |
| `matching-type` | STRICT(顺序) / FUZZY(无序) |
| `request-tick` | 组合键输入窗口(ticks) |
| `mapping` | 按键代码，多键=组合键 |

### 背包配置 (config.yml → settings.keybinding.backpack)

```yaml
backpack:
  default-page: "0"
  pages:
    "0":
      name: "战斗页"
      slots:
        slot0: { key: key0 }    # slot → keymapping 引用
        slot1: { key: key1 }
```

**设计理念**：keymapping 是按键注册表，backpack 按页组织槽位映射 slot→key。每页独立。

### 运行时流程

```
玩家加入
  → PlayerProfileLoadedEvent
  → MinecraftInteraction.updateInventory()
     → 清空 keymapping 对应 hotbar 槽位
     → 读取当前页配置
     → 每槽位查找装备技能 → 渲染图标到 hotbar

翻页 (/planners backpack page <id>)
  → BackpackAPI.setCurrentPage()
  → BackpackPageSwitchEvent.Pre → Post
  → MinecraftInteraction.updateInventory()

装备技能 (UI 操作)
  → BackpackAPI.equipSkill(template, skill, page, slot)
  → 如果目标槽已有技能 → 卸下
  → 如果技能已在其他槽 → 卸下
  → 设置 equipped/page/slot → DB 更新
```

### BackpackAPI

```
getCurrentPage(template) → String        // 默认返回 default-page
setCurrentPage(template, page)           // 切换页
equipSkill(template, skill, page, slot)  // 装备
unequipSkill(template, skill)            // 卸下
getSkillByKey(template, keyId) → PlayerSkill?  // 按键→技能
```

---

## 八、事件系统

### 技能相关

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `PlayerSkillCastEvent.Pre` | 技能释放前 | 是 |
| `PlayerSkillCastEvent.Post` | 释放成功后 | 否 |
| `PlayerSkillCooldownEvent.Set` | 冷却设置时 | 是 |
| `PlayerSkillEvent.LevelChange` | 技能等级变化 | 是 |

### 玩家相关

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `PlayerLevelChangeEvent` | 玩家等级变化 | 是 |
| `PlayerExperienceEvent.Increment/Decrement` | 经验增减 | 是 |
| `PlayerMagicPointEvent.Increase/Decrease` | 法力增减 | 是 |
| `PlayerProfileLoadedEvent` | 玩家数据加载完成 | 否 |
| `PlayerSetRouteEvent.Pre/Post` | 职业设置 | Pre可取消 |
| `PlayerDamageEntityEvent` | 玩家伤害实体 | 是 |

### 背包相关

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `BackpackEquipEvent.Equip` | 技能装备前 | 是 |
| `BackpackEquipEvent.Unequip` | 技能卸下前 | 是 |
| `BackpackPageSwitchEvent.Pre` | 翻页前 | 是 |
| `BackpackPageSwitchEvent.Post` | 翻页后 | 否 |

### 状态相关

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `EntityStateEvent.Attach.Pre/Post` | 状态附加 | Pre可取消 |
| `EntityStateEvent.Detach.Pre/Post` | 状态移除 | Pre可取消 |
| `EntityStateEvent.Mount.Pre/Post` | 状态首次挂载 | Pre可取消 |
| `EntityStateEvent.Close.Pre/Post` | 状态完全关闭 | Pre可取消 |
| `EntityStateEvent.End` | 状态到期 | 是 |

---

## 九、config.yml 关键配置

```yaml
database:
  use: LOCAL                    # LOCAL(SQLite) / SQL(MySQL)

settings:
  damage-causes: [SKILL, ATTRIBUTE, MYTHIC]  # 注册自定义伤害类型

  magic-point:
    resume:
      expression: "1 + random(1, 2)"   # 每 tick 恢复值
      update-tick: 20                  # 更新间隔
    upper-limit:
      expression: "profile.level * 2 + 100"  # 上限公式
      update-tick: 20

  level:
    isolation: all              # all / router / job
    synchronize: true           # 同步原版等级

  cooler:
    use: memory                 # memory(内存) / persistence(持久化)

  keybinding:
    keymapping: { ... }         # 按键注册表
    backpack:                   # 技能背包
      default-page: "0"
      pages: { ... }
```

---

## 十、命令参考

| 命令 | 说明 |
|------|------|
| `/pl skill open` | 打开技能操作 UI |
| `/pl skill upgrade <skill>` | 技能升级 UI |
| `/pl skill cast <player> <skill>` | 释放技能（完整流程） |
| `/pl skill run <player> <skill> [level]` | 执行技能脚本（绕过检查） |
| `/pl backpack open` | 打开背包 UI |
| `/pl backpack page <id>` | 切换背包页 |
| `/pl route open` | 职业选择 UI |
| `/pl route transfer` | 转职 UI |
| `/pl reload` | 重载配置 |
