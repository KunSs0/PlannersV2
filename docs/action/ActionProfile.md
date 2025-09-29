# ActionProfile

玩家属性相关的动作类，用于管理玩家的魔法值、等级、经验等属性。

## 类信息

- **包名**: `com.gitee.planners.module.kether`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `profile`

## 动作列表

### 魔法值操作

#### `profile magicpoint` / `profile mp`

```kether
profile magicpoint [at <objective>:TargetContainer(sender)]
```

获取指定目标的当前魔法值。

**参数说明**:
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 当前魔法值数值，如果目标不是玩家则返回-1

#### `profile magicpoint set <value>`

```kether
profile magicpoint set <value> [at <objective>:TargetContainer(sender)]
```

设置指定目标的魔法值。

**参数说明**:
- `<value>`: **必填** - 要设置的魔法值数值
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

#### `profile magicpoint add <value>`

```kether
profile magicpoint add <value> [at <objective>:TargetContainer(sender)]
```

增加指定目标的魔法值。

**参数说明**:
- `<value>`: **必填** - 要增加的魔法值数值
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

#### `profile magicpoint take <value>`

```kether
profile magicpoint take <value> [at <objective>:TargetContainer(sender)]
```

减少指定目标的魔法值。

**参数说明**:
- `<value>`: **必填** - 要减少的魔法值数值
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

#### `profile magicpoint.max`

```kether
profile magicpoint.max [at <objective>:TargetContainer(sender)]
```

获取指定目标的魔法值上限。

**参数说明**:
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 魔法值上限数值，如果目标不是玩家则返回-1

### 职业信息

#### `profile job`

```kether
profile job
```

获取当前玩家的职业ID。

**参数说明**: 无参数

**返回值**: 职业ID字符串，如果没有职业则返回null

### 等级和经验

#### `profile level`

```kether
profile level
```

获取当前玩家的等级。

**参数说明**: 无参数

**返回值**: 等级数值

#### `profile experience`

```kether
profile experience
```

获取当前玩家的经验值。

**参数说明**: 无参数

**返回值**: 经验值数值

#### `profile experience.max`

```kether
profile experience.max
```

获取当前玩家的经验值上限。

**参数说明**: 无参数

**返回值**: 经验值上限数值

## 简单语句示例

### 示例1：检查魔法值
```kether
if check profile mp < 50 then {
    tell "魔法值不足"
}
```
**详细讲解**:
- `profile mp` 获取当前玩家的魔法值
- `check ... < 50` 检查魔法值是否小于50
- `then` 分支在条件满足时执行
- `tell` 语句向玩家发送提示信息

### 示例2：显示玩家等级
```kether
tell "你的等级是: " profile level
```
**详细讲解**:
- `profile level` 获取玩家当前等级
- `tell` 语句将等级信息与文本连接后发送给玩家
- 这是最简单的玩家信息显示方式

## 实际功能示例

### 示例1：技能消耗系统
```kether
# 定义技能消耗检查函数
def checkManaCost = {
    # 获取技能消耗的魔法值
    set skillCost to 30
    
    # 获取当前魔法值
    set currentMP to profile mp
    
    # 获取魔法值上限
    set maxMP to profile mp.max
    
    # 检查魔法值是否足够
    if check &currentMP < &skillCost then {
        tell "魔法值不足! 需要 " &skillCost " 点魔法值，当前只有 " &currentMP " 点"
        return false
    }
    
    # 消耗魔法值
    profile mp take &skillCost
    
    # 显示剩余魔法值
    set remainingMP to profile mp
    tell inline "技能释放成功! 剩余魔法值: {{ &remainingMP }}/{{ &maxMP }}"
    
    return true
}

# 使用技能前检查魔法值
if check checkManaCost then {
    # 执行技能逻辑
    tell "火球术释放!"
    particle create "flame" at player location
}
```
**详细讲解**:
- 定义函数检查魔法值消耗
- 使用 `profile mp` 获取当前魔法值
- 使用 `profile mp.max` 获取魔法值上限
- 通过条件判断检查是否满足消耗要求
- 如果满足则消耗魔法值并执行技能
- 提供详细的提示信息给玩家

### 示例2：等级升级系统
```kether
# 经验获取和升级处理
def handleExperienceGain = {
    # 定义获得的经验值
    set expGain to 100
    
    # 获取当前等级和经验
    set currentLevel to profile level
    set currentExp to profile experience
    set maxExp to profile experience.max
    
    # 增加经验值
    profile experience add &expGain
    
    # 检查是否升级
    if check profile experience >= &maxExp then {
        # 升级处理
        profile level add 1
        profile experience set 0
        
        # 显示升级信息
        tell "恭喜升级! 当前等级: " profile level
        
        # 升级奖励
        profile mp.max add 10
        tell "魔法值上限提升10点!"
        
        return true
    } else {
        # 显示经验获取信息
        set newExp to profile experience
        tell "获得 " &expGain " 点经验，当前经验: " &newExp "/" &maxExp
        return false
    }
}

# 完成任务后获得经验
call handleExperienceGain
```
**详细讲解**:
- 定义经验获取处理函数
- 使用 `profile level` 和 `profile experience` 获取当前状态
- 增加经验值后检查是否满足升级条件
- 升级时重置经验值并增加等级
- 提供升级奖励和详细的提示信息
- 实现完整的经验系统逻辑

## 使用场景

- 技能消耗和魔法值管理
- 等级系统和经验获取
- 职业相关的属性操作
- 玩家状态监控和显示

## 注意事项

- 所有操作都依赖于玩家的职业模板(plannersTemplate)
- 如果目标不是玩家，魔法值操作会返回-1
- 经验值操作会自动处理升级逻辑
- 建议在使用前检查目标是否为玩家