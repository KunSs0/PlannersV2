# ActionJob

职业相关的动作类，用于获取玩家职业信息。

## 类信息

- **包名**: `com.gitee.planners.module.kether`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `job`

## 动作列表

### `job id`

```kether
job id
```

获取当前玩家的职业ID。

**参数说明**: 无参数

**返回值**: 职业的ID字符串，如果没有职业则返回null

### `job name`

```kether
job name
```

获取当前玩家的职业名称。

**参数说明**: 无参数

**返回值**: 职业的名称字符串，如果没有职业则返回null

## 简单语句示例

### 示例1：显示职业信息
```kether
tell "你的职业ID是: " job id
```
**详细讲解**:
- `job id` 获取当前玩家的职业ID
- `tell` 语句将职业ID和文本字符串连接后发送给玩家
- 这是最简单的职业信息显示方式

### 示例2：职业名称判断
```kether
set jobName to job name
```
**详细讲解**:
- `job name` 获取当前玩家的职业名称
- `set jobName to` 将职业名称存储在变量 `jobName` 中
- 便于后续对职业名称进行条件判断

## 实际功能示例

### 示例1：职业特定技能权限检查
```kether
# 检查玩家职业是否符合技能要求
def canUseSkill = {
    # 获取当前职业ID
    set currentJob to job id
    
    # 检查职业是否为战士
    if check &currentJob is "warrior" then {
        return true
    }
    
    # 检查职业是否为法师
    if check &currentJob is "mage" then {
        return true
    }
    
    # 其他职业不能使用该技能
    tell "你的职业无法使用此技能"
    return false
}

# 使用技能前检查权限
if check canUseSkill then {
    # 执行技能逻辑
    tell "技能释放成功!"
    skill cast0 "warrior_skill"
}
```
**详细讲解**:
- 定义函数 `canUseSkill` 检查职业权限
- 使用 `job id` 获取当前职业ID
- 通过条件判断检查职业是否符合技能要求
- 只有战士和法师可以释放该技能
- 根据检查结果决定是否执行技能逻辑

### 示例2：职业进阶系统
```kether
# 职业进阶检查逻辑
def checkJobAdvancement = {
    # 获取当前职业和等级
    set currentJob to job id
    set currentLevel to profile level
    
    # 判断是否满足进阶条件
    if check &currentJob is "novice" and &currentLevel >= 10 then {
        # 显示进阶选项
        tell "请选择进阶职业:"
        tell "1. 战士 - 高物理攻击"
        tell "2. 法师 - 强大魔法"
        
        # 等待玩家选择
        wait for input 30s
        
        # 处理玩家选择
        if check &input is "1" then {
            # 进阶为战士
            profile job set "warrior"
            tell "恭喜你成为战士!"
            return true
        } else if check &input is "2" then {
            # 进阶为法师
            profile job set "mage"
            tell "恭喜你成为法师!"
            return true
        }
    }
    
    return false
}

# 每次升级时检查进阶
profile level add 1
if check checkJobAdvancement then {
    tell "职业进阶成功!"
}
```
**详细讲解**:
- 使用 `job id` 获取当前职业判断是否是新手
- 结合 `profile level` 检查等级是否满足进阶条件
- 提供职业选择界面给玩家
- 根据选择设置新的职业ID
- 实现完整的职业进阶流程

## 使用场景

- 根据玩家职业显示不同的技能效果
- 职业特定的条件判断和权限检查
- 职业进阶和转职系统的实现

## 注意事项

- 只有在玩家有职业时才会返回有效值
- 动作依赖于玩家的职业模板(plannersTemplate)
- 如果玩家没有职业，返回值可能为null，需要进行空值检查

## 相关技能示例

在技能配置中，可以通过变量引用职业信息：

```yaml
variables:
  job_name: lazy job name
  job_id: lazy job id
```