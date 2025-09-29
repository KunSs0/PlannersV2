# SelectorAmount

数量限制选择器，用于限制选择结果的数量。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `@amount`, `@count`, `@limit`

## 语法

```kether
@amount <maxCount>
@count <maxCount>
@limit <maxCount>
```

限制选择结果的数量。

**参数说明**:
- `<maxCount>`: **必填** - 最大选择数量

**返回值**: 数量限制后的目标容器

## 简单语句示例

### 示例1：限制选择5个目标
```kether
select @entity-type zombie @amount 5
```
**详细讲解**:
- `@entity-type zombie` 选择所有僵尸
- `@amount 5` 限制最多选择5个
- 返回前5个找到的僵尸

### 示例2：结合其他选择器
```kether
select @range 10 @amount 3
```
**详细讲解**:
- `@range 10` 选择10格范围内的实体
- `@amount 3` 限制最多选择3个
- 返回范围内前3个实体

## 实际功能示例

### 示例1：随机目标选择
```kether
# 随机目标选择系统
def randomTargetSelection = {
    # 选择20格范围内的所有敌对目标
    set allTargets to select @range 20 @entity-type [ "zombie" "skeleton" "spider" ]
    
    # 随机打乱顺序
    shuffle &allTargets
    
    # 随机选择3个目标
    set selectedTargets to select @amount 3 from &allTargets
    
    # 显示选择结果
    tell inline "随机选择了 {{ size &selectedTargets }} 个目标:"
    
    for target in &selectedTargets then {
        tell inline "- {{ target name }} (距离: {{ math vector length vector sub target location player location }} 格)"
        
        # 高亮显示选中目标
        particle create "glow" at target location duration 3000
    }
    
    return &selectedTargets
}

# 使用随机选择
set targets to randomTargetSelection
```
**详细讲解**:
- 组合使用 `@range`, `@entity-type`, `@shuffle` 和 `@amount`
- 实现随机目标选择机制
- 提供详细的选中目标信息
- 完整的随机选择系统

### 示例2：技能目标数量限制
```kether
# 多目标技能系统
def multiTargetSkillSystem = {
    # 获取技能等级
    set skillLevel to skill level "multi_attack"
    
    # 根据技能等级计算最大目标数
    set maxTargets to math &skillLevel + 1
    
    # 选择范围内的目标
    set potentialTargets to select @range 8 @entity-type [ "zombie" "skeleton" "spider" ]
    
    # 按距离排序并限制数量
    set finalTargets to select @sort distance @amount &maxTargets from &potentialTargets
    
    # 对每个目标执行攻击
    for target in &finalTargets then {
        # 计算伤害（基于技能等级）
        set damage to math 10 + &skillLevel * 2
        
        # 造成伤害
        health take &damage at &target
        
        # 显示攻击效果
        particle create "damage" at target location duration 500
        tell inline "对 {{ target name }} 造成 {{ &damage }} 点伤害"
    }
    
    # 显示技能效果
    tell inline "多重攻击命中 {{ size &finalTargets }} 个目标!"
    particle create "sweep_attack" at player location duration 1000
}

# 使用多目标技能
if check cooldown skill "multi_attack" <= 0 then {
    cooldown set 20000 skill "multi_attack"
    profile mp take 25
    call multiTargetSkillSystem
}
```
**详细讲解**:
- 根据技能等级动态计算目标数量
- 组合使用 `@sort` 和 `@amount` 实现智能目标选择
- 提供伤害计算和视觉效果
- 完整的多目标技能系统

## 组合使用示例

### 示例：智能治疗优先级系统
```kether
# 智能治疗优先级系统
def smartHealingPriority = {
    # 选择范围内的友方玩家
    set allAllies to select @range 15 @entity-type player @their
    
    # 过滤受伤的玩家
    set injuredAllies to filter &allAllies where health < max_health
    
    # 按受伤程度排序（生命值百分比最低的优先）
    set sortedAllies to sort &injuredAllies by health / max_health
    
    # 根据治疗者等级限制治疗数量
    set healerLevel to skill level "group_heal"
    set maxHeals to math &healerLevel / 2 + 1
    
    # 选择最需要治疗的前N个目标
    set targetsToHeal to select @amount &maxHeals from &sortedAllies
    
    # 执行治疗
    for ally in &targetsToHeal then {
        # 计算治疗量（基于受伤程度）
        set healthRatio to math ally health / ally max_health
        set healAmount to math (1.0 - &healthRatio) * 30 + 5
        
        # 进行治疗
        health add &healAmount at &ally
        
        # 显示治疗效果
        particle create "heart" at ally location duration 800
        tell inline "治疗 {{ ally name }} +{{ &healAmount }} HP"
    }
    
    # 显示治疗统计
    tell inline "治疗完成! 共治疗了 {{ size &targetsToHeal }} 名玩家"
    
    return count &targetsToHeal
}

# 使用群体治疗
if check profile mp >= 40 then {
    profile mp take 40
    call smartHealingPriority
}
```
**详细讲解**:
- 组合使用 `@filter`, `@sort` 和 `@amount` 实现智能治疗优先级
- 根据治疗者等级动态调整治疗数量
- 提供详细的治疗反馈
- 完整的智能治疗系统

## 使用场景

- 技能目标数量限制
- 随机选择机制
- 优先级系统
- 资源分配控制

## 注意事项

- 数量限制基于当前选择结果的顺序
- 建议先排序再限制数量
- 数量为0时将清空选择结果
- 可以与其他选择器任意组合