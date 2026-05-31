---
name: planners-skill
description: 编写 Planners 插件技能配置和脚本的完整参考指南。涵盖技能/职业/路由/技能树 YAML 配置、JS 脚本 API、变量系统、背包系统、状态效果、属性系统、伤害类型、延迟释放、外部按键桥接、config.yml 全部配置项、配置目录结构、30+ 命令参考等。
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

  # 属性钩子 (Map 结构，值为 JS 表达式)
  hook:
    attributes:
      STR: "10 + level * 2"

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
                    │ Event.Check     │──取消→ CANCEL_WITH_EVENT
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
                    │ PlayerSkillCast │
                    │ Event.Pre       │──取消→ CANCEL_WITH_EVENT
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ 设置冷却         │
                    │ 扣除法力         │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Hook 拦截?      │──是→ INTERCEPTED (等待 resume)
                    │ SkillInputExec  │
                    └────────┬────────┘
                             │ 否
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

### ExecutableResult 枚举

| 值 | 含义 |
|----|------|
| `COOLING` | 技能冷却中 |
| `MAGICPOINT_INSUFFICIENT` | 法力不足 |
| `CANCEL_WITH_EVENT` | 被 Check/Pre 事件取消 |
| `INTERCEPTED` | 被 SkillInputExecHook 接管，等待 resume() |
| `SUCCESS` | 释放成功 |

### SkillInputExec — 延迟释放机制

当外部插件（如动画系统）需要在技能执行前播放招式动画时，可通过 `SkillInputExecHook` 拦截释放流程：

```kotlin
// 外部插件实现拦截器
class AnimHook : SkillInputExecHook {
    override fun intercept(ctx: SkillInputExec.Context) {
        // 播放动画...
        animation.play(ctx.player) {
            ctx.resume()  // 动画结束后继续执行技能
        }
    }
}

// 注册
PlannersAPI.registerSkillInputExecHook(AnimHook())
```

**流程**：`cast()` 在锁定资源后、`execute()` 前调用 hook → hook 可延迟调用 `resume()` → `resume()` 执行脚本 + 触发 Post 事件。

**注意**：全局只能有一个 hook（`registerSkillInputExecHook` 替换旧值）。`cast()` 返回 `INTERCEPTED` 时不代表失败，而是等待 hook 异步恢复。

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
    attributes:                   # 职业级属性 (Map 结构)
      STR: "10 + level * 2"
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

### KeyBindingBridge — 外部按键接入

供外部插件（如 FightCore）将自定义按键事件送入 CombinedAnalyzer 组合键匹配：

```kotlin
KeyBindingBridge.processKeyAction(player: Player, keyCode: String)
```

`keyCode` 需与 config.yml 中 `keymapping.*.mapping` 的值一致（如 `"R"`, `"mouse.left"`, `"keyboard.r"`）。

**两种按键入口**：
| 入口 | 来源 | 说明 |
|------|------|------|
| `CombinedHandler` | Bukkit 原生事件 | 左/右键、潜行、疾跑、跳跃 |
| `KeyBindingBridge` | 外部插件调用 | 自定义热键面板、技能轮盘等 |

两者最终都汇入 `CombinedAnalyzer.processAction()`，走相同的组合键匹配引擎。

---

## 八、事件系统

### 技能相关

| 事件 | 触发时机 | 可取消 |
|------|---------|--------|
| `PlayerSkillCastEvent.Check` | 技能释放最早阶段（CD/MP检查前） | 是 |
| `PlayerSkillCastEvent.Pre` | CD/MP 检查后，资源锁定前 | 是 |
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

## 九、属性系统 (AttributeProxy)

### 架构

```
AttributeSource (多来源)
    ├── HookAttributeSource (技能/职业 hook.attributes，priority=5)
    ├── 外部 AttributeSource 实现 (priority 自定义)
    └── ...
        ↓ 所有来源汇总
AttributeProxy (编排器，按 priority 排序)
    ↓ 分流
    ├── 逻辑属性 (在 registry 中) → AttributeConversion.convert → 物理属性
    └── 物理属性 (不在 registry 中) → 直接推送
        ↓
AttributeDriver.set(entity, "planners-proxy", attributes)
```

### AttributeSource 接口

```kotlin
interface AttributeSource {
    val id: String            // 来源唯一标识
    val priority: Int         // 越小越先算，同名 key 累加
    fun getAttributes(entity: LivingEntity): Map<String, Double>
}
```

### 预设优先级常量

| 常量 | 值 | 用途 |
|------|-----|------|
| `PRIORITY_BASE` | 0 | 基础值 |
| `PRIORITY_SKILL` | 5 | 技能/职业 Hook 属性 |
| `PRIORITY_GROWTH` | 10 | 成长属性 |
| `PRIORITY_INVESTED` | 20 | 加点属性 |
| `PRIORITY_EQUIP` | 30 | 装备属性 |
| `PRIORITY_BUFF` | 40 | Buff 属性 |
| `PRIORITY_OVERRIDE` | 100 | 覆盖/终极属性 |

### 核心 API

```kotlin
AttributeProxy.register(source)              // 注册来源
AttributeProxy.unregister(source)            // 注销来源
AttributeProxy.get(entity, "STR")            // 获取单个属性汇总值
AttributeProxy.sync(entity)                  // 完整重算并推送到 AttributeDriver
```

### 配置属性注册表 (config.yml)

```yaml
settings:
  attribute:
    registry:
      STR:
        name: "力量"
        mappings:
          ATK: 1.0       # 1 力量 = 1 攻击力
          DEF: 0.5       # 1 力量 = 0.5 防御力
      INT:
        name: "智力"
        mappings:
          MAGIC_ATK: 1.0
          MANA_REGEN: 0.3
      AGI:
        name: "敏捷"
        mappings:
          SPEED: 0.8
          CRIT: 0.3
```

逻辑属性（STR/INT/AGI 等）通过 mappings 系数转换为物理属性推送到外部属性插件。

### 技能/职业中配置 Hook 属性

```yaml
# skill/*.yml 或 job/*.yml
__option__:
  hook:
    attributes:
      STR: "10 + level * 2"    # JS 表达式
```

`HookAttributeSource` 自动收集当前职业和所有已学技能的 hook 属性，同名 key 累加。

---

## 十、DamageCause 伤害类型

```kotlin
sealed interface DamageCause {
    class Bukkit(cause: DamageCause) : DamageCause   // 原生伤害类型
    class Custom(name: String) : DamageCause          // 自定义伤害类型
}

DamageCause.of("SKILL")          // 按名称查找
DamageCause.ofOrNull("MYTHIC")   // 安全查找，不存在返回 null
```

查找优先级：先匹配 Bukkit 枚举，再查 config.yml 的 `settings.damage-causes` 列表。

### JS 中使用

```javascript
damageEx(100, "SKILL")           // 自定义 cause 伤害
damageExBy(100, "ATTRIBUTE", target)  // 完整指定
```

---

## 十一、config.yml 完整配置

### database（数据库）

| 配置项 | 含义 | 默认值 |
|--------|------|--------|
| `database.use` | LOCAL(SQLite) / SQL(MySQL) | SQL |
| `database.sql.host` | MySQL 主机 | 127.0.0.1 |
| `database.sql.port` | MySQL 端口 | 3306 |
| `database.sql.user` | MySQL 用户名 | root |
| `database.sql.password` | MySQL 密码 | 123456 |
| `database.sql.database` | 数据库名 | bukkit_plugin |
| `database.sql.table` | 数据表前缀 | planners_v2 |

### settings.level（等级系统）

| 配置项 | 含义 |
|--------|------|
| `level.isolation` | 隔离模式：`all` 全职业统一 / `router` 职业路线独立 / `job` 职业独立 |
| `level.synchronize` | 是否同步原版 Minecraft 等级 |
| `level.original-hook` | 是否兼容原版经验获取机制 |
| `level.algorithm` | isolation=all 时的经验算法 ID（引用 module/level/*.yml） |

### settings.skill-points（技能点系统）

| 配置项 | 含义 |
|--------|------|
| `per-level` | 每升一级获得的技能点数（JS 表达式，`level` 为变量） |
| `bonuses` | 关键等级额外奖励：Map `<等级, 奖励点数>` |

### settings.magic-point（法力值系统）

| 配置项 | 含义 |
|--------|------|
| `resume.expression` | 每更新周期恢复量（JS 表达式） |
| `resume.update-tick` | 恢复更新间隔（tick） |
| `upper-limit.expression` | 法力上限公式（JS，`profile.level` 可用） |
| `upper-limit.update-tick` | 上限更新间隔（tick） |

### settings.cooler（冷却系统）

| 配置项 | 含义 |
|--------|------|
| `cooler.use` | `memory`（内存/重启清空）或 `persistence`（持久化） |

### settings.damage-causes（伤害类型）

```yaml
damage-causes: [SKILL, ATTRIBUTE, MYTHIC]
```

在 `config.yml` 中声明后，`DamageCause.of("SKILL")` 才能识别。自定义伤害类型需在此注册。

### settings.attribute.registry（属性注册表）

逻辑属性到物理属性的转换映射，详见 [九、属性系统](#九属性系统-attributeproxy)。

### settings.placeholder（占位符）

| 配置项 | 含义 |
|--------|------|
| `placeholder.use` | `script`（JS 计算）/ `literal`（直接取值） |

### settings.attack.protect（攻击保护）

| 配置项 | 含义 |
|--------|------|
| `protect.enable` | 是否启用 |
| `protect.scene` | 生效场景列表（world / dungeonplus / team / worldguard 匹配） |

### settings.condition（条件定义）

供技能树、转职等引用的通用条件池，每个条件包含：

| 字段 | 含义 |
|------|------|
| `exper` | JS 表达式，返回 true 时满足 |
| `props` | 条件参数（如 `min`、`skillId`、`amount`） |
| `hint` | 不满足时的提示文本 |
| `consume` | 满足后执行的消耗动作（可选） |

预定义条件：`player_lv`（等级检查）、`need_skill`（前置技能）、`consume_sp`（消耗技能点）、`need_coin`（消耗金币）、`need_item`（消耗物品）、`is_warrior`/`is_mage`（职业检查）。

### settings.keybinding（按键绑定）

**keymapping** — 按键注册表，每个 key 包含：

| 字段 | 含义 |
|------|------|
| `name` | 显示名称 |
| `matching-type` | `strict`（顺序）/ `fuzzy`（无序） |
| `request-tick` | 组合键输入窗口（tick） |
| `mapping` | 对应 Minecraft 按键代码 |

**backpack** — 技能背包页配置：

| 字段 | 含义 |
|------|------|
| `default-page` | 默认页 ID |
| `pages.<id>.name` | 页名称 |
| `pages.<id>.slots.<slotN>.key` | 槽位→按键引用（keymapping 中的 key ID） |

### settings.minecraft.interaction-action（交互动作）

| 配置项 | 含义 |
|--------|------|
| `enable` | 是否启用空手交互触发热键栏技能 |
| `empty-skill.material` | 空槽位材质 |
| `empty-skill.name` | 空槽位名称 |

### settings.bukkit-launch.unimpeded-types（穿透方块）

技能射线检测时穿透的方块类型列表，默认：`AIR, SHORT_GRASS, GLASS, WATER, DANDELION`。

---

## 十二、配置目录结构

```
resources/
├── config.yml              # 核心配置
├── lang/zh_CN.yml          # 语言文件（30条）
├── skill/*.yml             # 技能配置（~20个）
├── job/<路线>/*.yml         # 职业配置
├── router/*.yml            # 职业路由
├── skilltree/*.yml         # 技能树（节点+依赖图）
├── module/
│   ├── level/*.yml         # 等级经验算法
│   └── currency/*.yml      # 自定义货币
├── state/*.yml             # 状态效果
├── action/*.yml            # 自定义动作
└── ui/*.yml                # GUI 布局（7个）
```

### skilltree/（技能树配置）

定义技能节点、升级条件和依赖关系图：

```yaml
nodes:
  slash:                          # 节点 ID（对应技能 ID）
    lv-1:                         # 第 1 级条件
      condition:
        player_lv: { min: 1 }
        consume_sp: { amount: 1 }
    lv-2:                         # 第 2 级条件
      condition:
        player_lv: { min: 5 }
        consume_sp: { amount: 2 }
    # ... lv-N
  charge:
    lv-1:
      condition: { ... }
    graph:                        # 前置依赖
      - slash                     # 必须先解锁 slash
```

条件 ID 引用 `config.yml → settings.condition` 池。

### module/level/（经验算法）

```yaml
def0:                              # 算法 ID
  min: 1                           # 最低等级
  max: 100                         # 最高等级
  experience: |                    # 升级经验（JS 三目表达式）
    level <= 10 ? level * 200 :
    level <= 20 ? level * 600 :
    level * 3000
```

### module/currency/（自定义货币）

```yaml
money:
  name: '金币'
  action:
    hook: getBalance()             # 查询
    withdraw: takeBalance(arg)     # 扣除
    deposit: giveBalance(arg)      # 存入
    set: setBalance(arg)           # 设置
```

### action/（自定义动作）

```yaml
__option__:
  id: "example"
  format: [type, uuid]            # 参数列表
  props:                          # 可选参数及默认值
    - { id: title, default: "abc" }
action: |-                        # JS 脚本
  tell(type + ":" + uuid)
```

### ui/（GUI 配置）

7 个 YAML 文件定义 GUI 布局：槽位位置、材质、名称、Lore、点击行为。UI 文件与对应的 `*UI.kt` 类配合加载。

### lang/zh_CN.yml（语言文件）

覆盖职业、转职、经验、等级、法力、技能、UI、指令等模块共 30 条本地化消息，支持 `&` 颜色码。

---

## 十三、完整命令参考

**根命令**：`/planners`（别名 `/pl`、`/ps`），权限 `planners.command`（所有子命令继承）。

### 技能操作

| 命令 | 说明 | 补全 |
|------|------|------|
| `/pl skill open <player>` | 打开技能操作 UI | 在线玩家 |
| `/pl skill tree <player>` | 打开技能树 UI | 在线玩家 |
| `/pl skill upgrade <player> <id>` | 打开技能升级 UI | 技能 ID |
| `/pl skill cast <player> <id>` | 释放技能（完整 CD/MP 流程） | 玩家已学技能 |
| `/pl skill run <player> <id> [level]` | 执行脚本（绕过检查，默认 1 级） | 所有注册技能 |

### 背包

| 命令 | 说明 | 补全 |
|------|------|------|
| `/pl backpack open <player>` | 打开背包 UI | 在线玩家 |
| `/pl backpack page <player> <page>` | 切换背包页并刷新物品栏 | 背包页 ID |

### 职业与转职

| 命令 | 说明 | 补全 |
|------|------|------|
| `/pl route open <player>` | 打开职业选择 UI | 在线玩家 |
| `/pl route select <player> <router>` | 直接设置职业路线 | 路由 ID |
| `/pl route transfer <player>` | 打开转职 UI | 在线玩家 |
| `/pl route clear <player>` | 清除职业路线 | 在线玩家 |

### 玩家属性管理（profile）

| 命令 | 说明 |
|------|------|
| `/pl profile level add <player> <value>` | 增加等级 |
| `/pl profile level take <player> <value>` | 减少等级 |
| `/pl profile level set <player> <value>` | 设置等级 |
| `/pl profile experience add <player> <value>` | 增加经验 |
| `/pl profile experience take <player> <value>` | 减少经验 |
| `/pl profile experience set <player> <value>` | 设置经验 |
| `/pl profile magicpoint add <player> <value>` | 增加法力 |
| `/pl profile magicpoint take <player> <value>` | 减少法力 |
| `/pl profile magicpoint set <player> <value>` | 设置法力 |
| `/pl profile magicpoint reset <player>` | 重置法力到上限 |

别名：`/pl profile mp ...` = `/pl profile magicpoint ...`

### 状态与测试

| 命令 | 说明 |
|------|------|
| `/pl state trigger <player> <name>` | 触发指定状态 |
| `/pl test <state> <duration>` | 为自己附加状态（Player only） |

### 其他

| 命令 | 说明 |
|------|------|
| `/pl main` | 显示命令帮助 |
| `/pl reload` | 重载全部配置 |
| `/pl console cast <id> [level]` | 以控制台身份执行技能 |
