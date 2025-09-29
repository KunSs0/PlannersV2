# ActionMetadata

元数据管理相关的动作类，用于读取、设置、修改目标的元数据。

## 类信息

- **包名**: `com.gitee.planners.module.kether.common`
- **类型**: `object` (单例对象)
- **继承**: `OperationKetherParser`
- **命名空间**: `metadata`

## 动作列表

### `metadata <id>`

```kether
metadata <id> [def <defaultValue>] [at <objective>:TargetContainer(sender)]
```

获取指定目标的元数据值。

**参数说明**:
- `<id>`: **必填** - 元数据键名
- `def <defaultValue>`: **选填** - 默认值，如果元数据不存在则返回此值
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 元数据的值，如果不存在且未指定默认值则返回null

### `metadata <id> to <value>`

```kether
metadata <id> to <value> [timeout: <duration>] [at <objective>:TargetContainer(sender)]
```

设置指定目标的元数据值。

**参数说明**:
- `<id>`: **必填** - 元数据键名
- `to <value>`: **必填** - 要设置的元数据值
- `timeout <duration>`: **选填** - 超时时间（毫秒），-1表示永久，默认为-1
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `metadata <id> add <value>`

```kether
metadata <id> add <value> [at <objective>:TargetContainer(sender)]
```

为数值类型的元数据增加值。

**参数说明**:
- `<id>`: **必填** - 元数据键名
- `add <value>`: **必填** - 要增加的值
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

## 简单语句示例

### 示例1：读取玩家状态
```kether
set playerState to metadata "combat.state" def "idle"
```
**详细讲解**:
- `metadata "combat.state"` 读取战斗状态元数据
- `def "idle"` 指定默认值为"idle"
- `set playerState to` 将读取到的值存储在变量中
- 如果元数据不存在，会使用默认值"idle"

### 示例2：设置临时标记
```kether
metadata "marked.target" to "boss" timeout 30000
```
**详细讲解**:
- `metadata "marked.target" to "boss"` 设置标记目标为boss
- `timeout 30000` 设置30秒后自动清除标记
- 适用于临时状态标记，避免手动清理

## 实际功能示例

### 示例1：战斗状态管理系统
```kether
# 战斗状态管理
def call handleCombatState = {
    # 检查玩家是否在战斗中
    set combatState to metadata "combat.state" def "idle"
    set combatTimer to metadata "combat.timer" def 0
    
    # 判断战斗状态
    if check &combatState is "fighting" then {
        # 减少战斗计时器
        set newTimer to math &combatTimer - 50
        
        if check &newTimer <= 0 then {
            # 战斗结束
            metadata "combat.state" to "idle"
            tell "战斗状态结束"
            
            # 清除战斗相关标记
            metadata "combat.target" remove
            metadata "combat.timer" remove
        } else {
            # 更新计时器
            metadata "combat.timer" to &newTimer
            
            # 显示剩余战斗时间
            set remainingSeconds to math &newTimer / 1000
            if check &remainingSeconds % 5 is 0 then {
                tell "战斗中... 剩余时间: " &remainingSeconds " 秒"
            }
        }
    }
}

# 进入战斗状态
def enterCombat = {
    # 设置战斗状态
    metadata "combat.state" to "fighting"
    
    # 设置战斗计时器（30秒）
    metadata "combat.timer" to 30000
    
    # 记录战斗目标
    metadata "combat.target" to &target
    
    tell "进入战斗状态!"
}

# 受到攻击时触发
if check damage source is not null then {
    call enterCombat
}

# 每tick更新战斗状态
handleCombatState
```
**详细讲解**:
- 使用元数据存储战斗状态和计时器
- 实现战斗状态的自动管理和超时
- 提供战斗状态的实时显示
- 自动清理过期的战斗标记
- 实现完整的战斗状态管理系统

### 示例2：技能冷却标记系统
```kether
# 技能冷却标记管理
def call manageSkillCooldownMarkers = {
    # 获取所有技能冷却标记
    set skillCooldowns to array [ "fireball.cd" "heal.cd" "teleport.cd" ]
    
    # 遍历检查每个技能的冷却状态
    foreach &skillCooldowns as cdMarker {
        # 获取冷却剩余时间
        set remainingTime to metadata &cdMarker def 0
        
        if check &remainingTime > 0 then {
            # 减少冷却时间
            set newTime to math &remainingTime - 50
            
            if check &newTime <= 0 then {
                # 冷却完成，清除标记
                metadata &cdMarker remove
                
                # 通知玩家
                set skillName to substring &cdMarker 0 math length &cdMarker - 3
                tell "" &skillName " 技能冷却完成!"
            } else {
                # 更新冷却时间
                metadata &cdMarker to &newTime
                
                # 每5秒显示一次冷却状态
                set secondsLeft to math &newTime / 1000
                if check &secondsLeft % 5 is 0 then {
                    set skillName to substring &cdMarker 0 math length &cdMarker - 3
                    tell "" &skillName " 冷却剩余: " &secondsLeft " 秒"
                }
            }
        }
    }
}

# 设置技能冷却标记
def setSkillCooldown = {
    # 定义技能冷却时间
    set skillCooldowns to map [
        "fireball" -> 10000
        "heal" -> 5000
        "teleport" -> 15000
    ]
    
    # 获取要释放的技能
    set skillId to "fireball"
    
    # 检查是否在冷却中
    set currentCd to metadata "" &skillId ".cd" def 0
    
    if check &currentCd > 0 then {
        tell "技能 " &skillId " 冷却中，剩余 " math &currentCd / 1000 " 秒"
        return false
    }
    
    # 设置冷却标记
    set cooldownTime to &skillCooldowns[&skillId]
    metadata "" &skillId ".cd" to &cooldownTime
    
    tell "技能 " &skillId " 释放成功，冷却时间 " math &cooldownTime / 1000 " 秒"
    return true
}

# 每tick更新冷却标记
manageSkillCooldownMarkers
```
**详细讲解**:
- 使用元数据为每个技能创建独立的冷却标记
- 实现冷却时间的自动递减和清理
- 提供详细的冷却状态提示
- 支持多个技能的并行冷却管理
- 实现灵活的技能冷却标记系统

## 使用场景

- 状态标记和临时数据存储
- 计时器和超时管理
- 战斗状态和效果跟踪
- 技能冷却和效果持续时间

## 注意事项

- 元数据只在当前会话中有效，重启后会被清除
- 超时时间单位为毫秒
- 数值类型的元数据可以使用 `add` 操作
- 建议为重要的元数据设置合理的超时时间