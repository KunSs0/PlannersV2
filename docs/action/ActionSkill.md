# ActionSkill

技能释放和管理相关的动作类，用于执行技能释放、获取技能等级等功能。

## 类信息

- **包名**: `com.gitee.planners.module.kether.common`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `skill`

## 动作列表

### `skill cast0 <id>`

```kether
skill cast0 <id> [type: <playupType>] [level: <skillLevel>] [at <objective>:TargetContainer(sender)]
```

释放指定技能，支持多种释放类型。

**参数说明**:
- `<id>`: **必填** - 技能ID
- `type <playupType>`: **选填** - 释放类型，可选值：`relative`(相对), `force`(强制), `invoke`(调用)，默认为`relative`
- `level <skillLevel>`: **选填** - 技能等级，默认为1
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `skill cast force <id>`

```kether
skill cast force <id> [level: <skillLevel>] [at <objective>:TargetContainer(sender)]
```

强制释放技能，不计入冷却时间。

**参数说明**:
- `<id>`: **必填** - 技能ID
- `level <skillLevel>`: **选填** - 技能等级，默认为1
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `skill cast invoke <id>`

```kether
skill cast invoke <id> [level: <skillLevel>] [cooled: <boolean>] [at <objective>:TargetContainer(sender)]
```

调用释放技能，技能必须存在于释放者身上，会计入冷却时间。

**参数说明**:
- `<id>`: **必填** - 技能ID
- `level <skillLevel>`: **选填** - 技能等级，-1表示使用自身技能等级，默认为-1
- `cooled <boolean>`: **选填** - 是否计入冷却，默认为true
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `skill level <id>`

```kether
skill level <id> [at <objective>:TargetContainer(sender)]
```

获取指定目标的技能等级。

**参数说明**:
- `<id>`: **必填** - 技能ID
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 技能等级数值，如果目标没有该技能则返回0

## 简单语句示例

### 示例1：释放基础技能
```kether
skill cast0 "fireball"
```
**详细讲解**:
- `skill cast0` 是技能释放的基本命令
- `"fireball"` 指定要释放的技能ID
- 使用默认参数：相对释放类型，等级1，执行者为释放目标

### 示例2：获取技能等级
```kether
set fireballLevel to skill level "fireball"
```
**详细讲解**:
- `skill level "fireball"` 获取当前玩家的火球术等级
- `set fireballLevel to` 将技能等级存储在变量中
- 便于后续进行等级相关的条件判断

## 实际功能示例

### 示例1：连击技能系统
```kether
# 连击技能释放逻辑
def comboSkillSystem = {
    # 检查连击计数
    set comboCount to metadata "combo.count" def 0
    
    # 根据连击数释放不同技能
    if check &comboCount is 0 then {
        # 第一击：基础攻击
        skill cast0 "basic_attack"
        metadata "combo.count" to 1
        tell "连击开始!"
        
    } else if check &comboCount is 1 then {
        # 第二击：快速攻击
        skill cast0 "quick_attack"
        metadata "combo.count" to 2
        tell "二连击!"
        
    } else if check &comboCount is 2 then {
        # 第三击：终结技
        skill cast0 "finisher"
        metadata "combo.count" to 0
        tell "终结连击!"
        
    } else {
        # 重置连击
        metadata "combo.count" to 0
        tell "连击中断"
    }
    
    # 设置连击超时
    metadata "combo.timer" to 3000
}

# 连击超时检查
def checkComboTimeout = {
    set timer to metadata "combo.timer" def 0
    if check &timer > 0 then {
        metadata "combo.timer" to math &timer - 50
        if check &timer <= 0 then {
            metadata "combo.count" to 0
            tell "连击超时"
        }
    }
}

# 每次攻击时调用
comboSkillSystem
```
**详细讲解**:
- 使用元数据存储连击计数和计时器
- 根据连击数释放不同的技能
- 实现连击技能的递进效果
- 设置连击超时机制防止滥用
- 提供完整的连击系统体验

### 示例2：技能等级限制系统
```kether
# 技能权限检查系统
def checkSkillPermission = {
    # 获取要释放的技能ID
    set skillId to "advanced_fireball"
    
    # 获取玩家该技能等级
    set skillLevel to skill level &skillId
    
    # 获取玩家总等级
    set playerLevel to profile level
    
    # 检查技能使用条件
    if check &skillLevel is 0 then {
        tell "你尚未学习 " &skillId " 技能"
        return false
    }
    
    if check &skillLevel < 3 then {
        tell &skillId " 技能需要达到3级才能使用，当前等级: " &skillLevel
        return false
    }
    
    if check &playerLevel < 10 then {
        tell "使用 " &skillId " 需要角色等级达到10级"
        return false
    }
    
    # 检查魔法值消耗
    set manaCost to math 20 * &skillLevel
    if check profile mp < &manaCost then {
        tell "魔法值不足! 需要 " &manaCost " 点魔法值"
        return false
    }
    
    # 所有条件满足，允许释放技能
    return true
}

# 释放高级技能
if check checkSkillPermission then {
    # 消耗魔法值
    profile mp take math 20 * skill level "advanced_fireball"
    
    # 释放技能
    skill cast invoke "advanced_fireball"
    
    # 显示技能效果
    tell "高级火球术释放!"
    particle create "large_flame" at player location
}
```
**详细讲解**:
- 使用 `skill level` 检查技能等级限制
- 结合 `profile level` 检查角色等级要求
- 动态计算魔法值消耗（基于技能等级）
- 提供详细的权限检查反馈
- 实现完整的技能权限管理系统

## 释放类型说明

### FORCE（强制释放）
- 不计入冷却时间
- 可指定任意等级
- 不需要玩家拥有该技能
- 适用于管理员或特殊事件

### RELATIVE（相对释放）
- 基于玩家当前状态释放
- 适用于标准技能释放场景

### INVOKE（调用释放）
- 技能必须存在于释放者身上
- 会计入冷却时间
- 可指定等级或使用自身等级
- 适用于玩家主动技能释放

## 使用场景

- 玩家技能释放系统
- 连击和组合技能
- 技能权限和等级限制
- 特殊事件技能触发

## 注意事项

- 强制释放可能绕过游戏平衡机制
- 调用释放需要确保玩家拥有对应技能
- 技能等级检查应在释放前进行
- 建议结合冷却时间检查使用