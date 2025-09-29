# ActionCooldown

技能冷却时间管理相关的动作类，用于获取、设置、重置技能的冷却时间。

## 类信息

- **包名**: `com.gitee.planners.module.kether.common`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `cooldown`, `cd`

## 动作列表

### `cooldown`

```kether
cooldown [skill <skillId>] [at <objective>:TargetContainer(sender)]
```

获取指定技能的剩余冷却时间。

**参数说明**:
- `skill <skillId>`: **选填** - 技能ID，如果不指定则使用当前上下文中的技能
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 剩余冷却时间（毫秒），如果技能不在冷却中则返回-1

### `cooldown reset`

```kether
cooldown reset [skill <skillId>] [at <objective>:TargetContainer(sender)]
```

重置指定技能的冷却时间。

**参数说明**:
- `skill <skillId>`: **选填** - 技能ID，如果不指定则使用当前上下文中的技能
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `cooldown add <value>`

```kether
cooldown add <value> [skill <skillId>] [at <objective>:TargetContainer(sender)]
```

为指定技能增加冷却时间。

**参数说明**:
- `<value>`: **必填** - 要增加的冷却时间（毫秒）
- `skill <skillId>`: **选填** - 技能ID，如果不指定则使用当前上下文中的技能
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `cooldown set <value>`

```kether
cooldown set <value> [skill <skillId>] [at <objective>:TargetContainer(sender)]
```

设置指定技能的冷却时间。

**参数说明**:
- `<value>`: **必填** - 要设置的冷却时间（毫秒）
- `skill <skillId>`: **选填** - 技能ID，如果不指定则使用当前上下文中的技能
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

## 简单语句示例

### 示例1：检查技能冷却
```kether
set remainingCD to cooldown skill "fireball"
```
**详细讲解**:
- `cooldown skill "fireball"` 获取火球术的剩余冷却时间
- `set remainingCD to` 将剩余冷却时间存储在变量中
- 便于后续进行冷却时间相关的条件判断

### 示例2：重置冷却时间
```kether
cooldown reset skill "heal"
```
**详细讲解**:
- `cooldown reset` 是重置冷却时间的命令
- `skill "heal"` 指定要重置的技能ID
- 执行后该技能的冷却时间将被重置为0

## 实际功能示例

### 示例1：智能冷却时间检查系统
```kether
# 智能技能释放检查
def smartSkillCast = {
    # 定义要释放的技能
    set skillId to "powerful_spell"
    
    # 获取技能剩余冷却时间
    set remainingCD to cooldown skill &skillId
    
    # 检查冷却状态
    if check &remainingCD > 0 then {
        # 计算剩余时间（秒）
        set remainingSeconds to math &remainingCD / 1000
        
        # 显示冷却提示
        tell "技能 " &skillId " 冷却中，剩余 " &remainingSeconds " 秒"
        
        # 如果冷却时间较短，提供快速冷却选项
        if check &remainingCD < 5000 then {
            tell "输入 'quick' 可以消耗魔法值快速冷却"
            
            # 等待玩家输入
            wait for input 3s
            
            if check &input is "quick" then {
                # 检查魔法值是否足够
                if check profile mp >= 20 then {
                    # 消耗魔法值重置冷却
                    profile mp take 20
                    cooldown reset skill &skillId
                    tell "快速冷却成功!"
                    return true
                } else {
                    tell "魔法值不足，无法快速冷却"
                }
            }
        }
        
        return false
    }
    
    # 冷却完成，可以释放技能
    return true
}

# 释放技能前检查
if check smartSkillCast then {
    # 正常释放技能
    skill cast0 &skillId
    
    # 设置新的冷却时间（30秒）
    cooldown set 30000 skill &skillId
    tell "技能释放成功!"
}
```
**详细讲解**:
- 使用 `cooldown skill` 获取技能剩余冷却时间
- 根据剩余冷却时间提供不同的用户体验
- 实现快速冷却功能（消耗魔法值重置冷却）
- 提供详细的冷却状态提示
- 实现智能的技能释放检查系统

### 示例2：冷却时间惩罚系统
```kether
# 冷却时间惩罚机制
def applyCooldownPenalty = {
    # 获取玩家当前状态
    set playerLevel to profile level
    set currentHP to player health
    set maxHP to player max_health
    
    # 计算冷却时间惩罚系数
    set penaltyFactor to 1.0
    
    # 根据血量状态增加惩罚
    set hpRatio to math &currentHP / &maxHP
    if check &hpRatio < 0.3 then {
        # 低血量时增加冷却时间
        set penaltyFactor to math &penaltyFactor * 1.5
        tell "生命值过低，技能冷却时间增加50%"
    }
    
    # 根据等级差异调整惩罚
    if check &playerLevel < 10 then {
        # 低等级玩家减少惩罚
        set penaltyFactor to math &penaltyFactor * 0.8
        tell "新手保护，冷却时间减少20%"
    }
    
    # 应用冷却时间惩罚
    return &penaltyFactor
}

# 技能释放后的冷却处理
def handleSkillCooldown = {
    # 定义基础冷却时间（秒）
    set baseCooldown to 60
    
    # 获取冷却时间惩罚系数
    set penaltyFactor to applyCooldownPenalty
    
    # 计算实际冷却时间
    set actualCooldown to math &baseCooldown * &penaltyFactor * 1000
    
    # 设置冷却时间
    cooldown set &actualCooldown
    
    # 显示冷却信息
    set actualSeconds to math &actualCooldown / 1000
    tell "技能进入冷却，冷却时间: " &actualSeconds " 秒"
    
    # 开始冷却计时器
    metadata "cooldown.timer" to &actualCooldown
}

# 冷却计时器更新
def updateCooldownTimer = {
    set timer to metadata "cooldown.timer" def 0
    if check &timer > 0 then {
        # 减少计时器
        metadata "cooldown.timer" to math &timer - 50
        
        # 检查是否冷却完成
        if check &timer <= 0 then {
            tell "技能冷却完成!"
            metadata "cooldown.timer" remove
        }
    }
}

# 技能释放后调用
handleSkillCooldown
```
**详细讲解**:
- 根据玩家状态动态计算冷却时间惩罚
- 低血量时增加冷却时间作为惩罚
- 低等级玩家享受冷却时间减免
- 使用 `cooldown set` 设置实际冷却时间
- 实现冷却时间的实时更新和显示
- 提供完整的冷却时间管理系统

## 使用场景

- 技能冷却时间管理
- 冷却时间惩罚和奖励系统
- 快速冷却功能
- 冷却状态显示和提示

## 注意事项

- 冷却时间单位是毫秒（1秒=1000毫秒）
- 如果不指定技能ID，会使用当前上下文中的技能
- 重置冷却时间会立即清除冷却状态
- 建议在技能释放后立即设置冷却时间