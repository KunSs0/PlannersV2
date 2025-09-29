# Planners V2 状态管理系统使用文档

## 概述

状态管理系统允许您为游戏实体（如玩家、生物）添加和管理各种状态效果，如增益效果、减益效果、特殊能力等。状态通过事件触发器实现自动化的效果管理。

## 核心功能

### 状态管理命令
```kotlin
// 附加状态到实体
state attach "poison"                    // 附加中毒状态
state attach "fire" at &target            // 为指定目标附加燃烧状态

// 从实体移除状态  
state detach "poison"                    // 移除中毒状态
state detach "fire" at &target            // 从指定目标移除燃烧状态

// 特殊符号：移除当前上下文状态
state detach "~"                         // 移除当前状态
```

### 状态检查方式

**注意：目前没有独立的 `has_state` 命令。状态检查主要通过以下方式实现：**

1. **触发器内部自动检查**：在状态触发器的 `action` 中，系统会自动检查实体是否具有该状态
2. **事件上下文**：在 `state attach` 和 `state detach` 事件中，可以通过 `&state.id` 获取当前状态

```kotlin
// 在状态触发器中，系统自动检查状态存在性
// 触发器内的 action 只在实体具有该状态时执行

// 例如在 poison 状态的触发器中：
action: |
    // 这里实体已经具有 poison 状态
    damage 2
    print "毒性发作!"
```

## 配置方式

### 状态配置文件结构

状态配置文件放在 `state/` 目录下，每个文件定义一个或多个状态：

```yaml
# state/poison.yml
poison:
  priority: 5.0
  name: "中毒"
  static: false
  trigger:
    on_damage:
      on: "entity damage"
      action: |
        // 系统自动检查实体是否具有 poison 状态
        // 只有具有该状态时才会执行这里的代码
        damage 2
        print "毒性发作!"
    
    on_tick:
      on: "entity tick"
      action: |
        if has_state "poison" then {
          if random(0, 100) < 10:
            print "毒性扩散!"
            damage 1
        }
```

### 配置字段说明

#### 基本字段
- **priority**: 优先级（数值越大优先级越高，决定执行顺序）
- **name**: 状态显示名称
- **static**: 是否为静态状态（静态状态不可被移除）

#### 触发器配置
每个触发器包含：
- **on**: 触发的事件类型
- **action**: 触发时执行的脚本动作

### 支持的事件类型
- `entity damage` - 实体受到伤害时
- `entity tick` - 实体每tick更新时  
- `entity death` - 实体死亡时
- `entity move` - 实体移动时
- `entity interact` - 实体交互时
- `state attach` - 状态附加时
- `state detach` - 状态移除时

## 实际使用示例

### 眩晕状态示例
```yaml
# state/stun.yml
stun:
  priority: 10.0
  name: "眩晕"
  static: false
  trigger:
    on_attach:
      on: "state attach"
      action: |
        print "目标被眩晕!"
        set_velocity 0 0 0  # 停止移动
        
    on_detach:
      on: "state detach" 
      action: |
        print "眩晕解除!"
        
    on_tick:
      on: "entity tick"
      action: |
        if has_state "stun" then {
          print "眩晕中，无法行动!"
          cancel_event  # 取消其他动作
        }
```

### 燃烧状态示例
```yaml
# state/fire.yml
fire:
  priority: 8.0
  name: "燃烧"
  static: false
  trigger:
    on_attach:
      on: "state attach"
      action: |
        print "目标开始燃烧!"
        play_sound "block.fire.ambient"
        
    on_tick:
      on: "entity tick"
      action: |
        if has_state "fire" then {
          damage 1
          if tick_count % 20 == 0:  # 每秒显示一次
            print "火焰持续燃烧!"
        }
        
    on_detach:
      on: "state detach"
      action: |
        print "火焰熄灭!"
        play_sound "block.fire.extinguish"
```

### 静态状态示例（永久效果）
```yaml
# state/bless.yml
bless:
  priority: 20.0
  name: "神圣祝福"
  static: true  # 静态状态，无法被移除
  trigger:
    on_tick:
      on: "entity tick"
      action: |
        if has_state "bless" then {
          if health < max_health * 0.5:
            heal 1
            print "神圣祝福正在治疗你!"
        }
```

## 脚本中使用状态

### 技能状态管理
```kotlin
// 眩晕攻击技能
def stunAttack = {
    state attach "stun" at &target
    damage 3
    print "使用眩晕攻击!"
}

// 状态检查组合技能
def comboAttack = {
    if has_state "stun" at &target then {
        print "目标被眩晕，连击生效!"
        damage 8
    } else {
        state attach "stun" at &target
        damage 3
        print "使用眩晕攻击!"
    }
}

// 状态持续时间管理
def timedPoison = {
    state attach "poison" at &target
    wait 5000  # 等待5秒
    state detach "poison" at &target
    print "毒性效果结束!"
}
```

### 事件监听中的状态管理
```kotlin
// 监听状态附加事件
on state attach:
    if &state.id is "poison" then {
        print "中毒状态已附加!"
        play_sound "entity.spider.ambient"
    }
    
    if &state.id is "stun" then {
        print "眩晕状态已附加!"
    }

// 监听状态移除事件  
on state detach:
    if &state.id is "poison" then {
        print "中毒状态已移除!"
    }
    
    if &state.id is "stun" then {
        print "眩晕状态已移除!"
        print "目标可以正常行动了!"
    }
```

### 战斗系统中的状态应用
```kotlin
// 玩家攻击时触发状态
def onPlayerAttack = {
    if has_item "poison_sword" then {
        state attach "poison" at &target
        print "毒刃生效，目标中毒!"
    }
    
    if has_skill "fire_attack" then {
        state attach "fire" at &target
        print "火焰攻击，目标燃烧!"
    }
    
    // 状态效果叠加
    if has_state "poison" at &target and has_state "fire" at &target then {
        print "毒火交织，额外伤害!"
        damage 5
    }
}

// 区域状态效果
def onPlayerMove = {
    if in_region "holy_ground" then {
        if not has_state "blessed" then {
            state attach "blessed"
            print "进入圣地，获得祝福!"
        }
    }
    
    if in_region "cursed_land" then {
        if not has_state "cursed" then {
            state attach "cursed"
            print "进入诅咒之地，受到诅咒!"
            damage 1
        }
    }
}
```

## 高级功能

### 优先级系统
状态优先级决定执行顺序，高优先级状态先执行：

```yaml
stun:
  priority: 10.0  # 高优先级，先执行
  # ...

poison:  
  priority: 5.0   # 低优先级，后执行
  # ...

fire:
  priority: 8.0   # 中优先级，在poison之前执行
  # ...
```

### 静态状态的特殊性
静态状态具有以下特性：
- 无法被 `state detach` 命令移除
- 适合用于永久性效果
- 重启服务器后需要重新附加

### 状态生命周期管理
```kotlin
// 自动状态清理
def autoCleanStates = {
    // 检查并清理过期的状态
    if has_state "temporary_buff" then {
        set duration to metadata "buff_duration" def 0
        if duration <= 0 then {
            state detach "temporary_buff"
            print "临时增益效果结束!"
        }
    }
}
```

## 语法要点总结

### ✅ 正确的状态检查语法
```kotlin
// 直接布尔判断，不需要check
if has_state "state_id" then

// 带目标的状态检查  
if has_state "state_id" at &target then

// 状态组合检查
if has_state "state1" and has_state "state2" then

// 状态取反检查
if not has_state "immune" then
```

### ❌ 错误的用法
```kotlin
// 错误：不需要check
if check has_state "poison" then

// 错误：语法不正确（当前不支持）
if state has "poison" then
```

### `check`的正确使用场景
```kotlin
// check用于验证操作，不是条件判断
check teleport &location  # 验证传送操作
check give_item "diamond"  # 验证物品给予

// 但状态检查是布尔表达式，不需要check
```

## 最佳实践

### 状态设计原则
1. **单一职责**：每个状态应有明确的单一效果
2. **合理优先级**：重要状态设置较高优先级
3. **适当持续时间**：避免过长或过短的状态持续时间
4. **清晰的触发器**：触发器逻辑应简洁明了

### 性能优化建议
```kotlin
// 使用静态状态减少重复创建
static_state = true   # 适合长期存在的状态

// 优化触发器频率
on entity tick:      # 每tick触发，谨慎使用
  if tick_count % 20 == 0:  # 每20tick执行一次
    // 低频逻辑

// 使用状态组合而非复杂单个状态
// 不推荐：一个状态包含10种效果
// 推荐：多个简单状态组合
```

### 调试技巧
```kotlin
// 状态调试
print "当前状态ID: ${@State.id}"
print "当前触发器ID: ${@Trigger.id}"
print "实体名称: ${sender.name}"

// 状态信息输出
if has_state "debug" then {
    print "调试模式激活!"
    print "实体状态列表: ${get_states()}"
}
```

## 故障排除

### 常见问题

1. **状态不触发**
   - 检查事件类型是否正确
   - 验证实体是否具有该状态
   - 检查脚本语法错误

2. **状态无法移除**
   - 确认状态是否为静态状态
   - 检查是否有其他脚本阻止移除

3. **性能问题**
   - 减少高频触发器使用
   - 优化复杂脚本逻辑

### 调试命令
```bash
# 控制台调试
states reload          # 重载所有状态
states list <player>   # 列出玩家状态
states remove <player> <state> # 移除玩家状态
```

这份文档基于实际代码分析，准确地描述了状态管理系统的功能和用法。状态管理系统通过事件驱动机制实现自动化的状态效果管理，为游戏实体提供丰富的状态行为控制能力。