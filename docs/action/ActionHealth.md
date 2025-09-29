# ActionHealth

生命值管理相关的动作类，用于增加、设置、减少实体的生命值。

## 类信息

- **包名**: `com.gitee.planners.module.kether.bukkit`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `health`

## 动作列表

### `health add <amount>`

```kether
health add <amount> [at <objective>:TargetContainer(sender)]
```

增加指定目标的生命值。

**参数说明**:
- `<amount>`: **必填** - 要增加的生命值数量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `health set <amount>`

```kether
health set <amount> [at <objective>:TargetContainer(sender)]
```

设置指定目标的生命值。

**参数说明**:
- `<amount>`: **必填** - 要设置的生命值数量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `health take <amount>`

```kether
health take <amount> [at <objective>:TargetContainer(sender)]
```

减少指定目标的生命值。

**参数说明**:
- `<amount>`: **必填** - 要减少的生命值数量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

## 简单语句示例

### 示例1：治疗玩家
```kether
health add 10 at player
```
**详细讲解**:
- `health add 10` 增加10点生命值
- `at player` 指定目标为玩家
- 执行后玩家的生命值会增加10点

### 示例2：设置生命值
```kether
health set 20 at &target
```
**详细讲解**:
- `health set 20` 设置生命值为20点
- `at &target` 指定目标为变量中的目标
- 执行后目标的生命值会被设置为20点

## 实际功能示例

### 示例1：智能治疗系统
```kether
# 智能治疗系统
def smartHealing = {
    # 获取目标当前生命值
    set currentHealth to player health
    set maxHealth to player max_health
    
    # 计算生命值百分比
    set healthPercentage to math &currentHealth / &maxHealth * 100
    
    # 根据生命值百分比决定治疗量
    if check &healthPercentage < 30 then {
        # 危急状态：大量治疗
        health add 50
        tell "紧急治疗! 恢复50点生命值"
        
    } else if check &healthPercentage < 70 then {
        # 中等伤害：中等治疗
        health add 25
        tell "治疗生效! 恢复25点生命值"
        
    } else {
        # 轻微伤害：少量治疗
        health add 10
        tell "轻微治疗! 恢复10点生命值"
    }
    
    # 显示治疗后的状态
    set newHealth to player health
    tell "当前生命值: " &newHealth "/" &maxHealth
}

# 使用治疗技能
if check profile mp >= 20 then {
    # 消耗魔法值
    profile mp take 20
    
    # 执行智能治疗
    call smartHealing
    
    # 显示治疗效果
    particle create "heart" at player location duration 1000
} else {
    tell "魔法值不足，无法使用治疗技能"
}
```
**详细讲解**:
- 根据玩家当前生命值百分比决定治疗量
- 危急状态（<30%）提供大量治疗
- 中等伤害（30-70%）提供中等治疗
- 轻微伤害（>70%）提供少量治疗
- 结合魔法值消耗和粒子效果
- 实现智能的治疗系统

### 示例2：生命值惩罚系统
```kether
# 生命值惩罚系统
def applyHealthPenalty = {
    # 获取玩家状态
    set playerLevel to profile level
    set currentHealth to player health
    
    # 根据玩家等级计算惩罚
    set penaltyRate to 1.0
    
    if check &playerLevel < 5 then {
        # 新手保护：减少惩罚
        set penaltyRate to 0.5
        tell "新手保护生效，惩罚减半"
    }
    
    # 计算惩罚量
    set penaltyAmount to math 10 * &penaltyRate
    
    # 应用惩罚
    health take &penaltyAmount
    
    # 显示惩罚信息
    set newHealth to player health
    tell "受到惩罚! 失去" &penaltyAmount "点生命值"
    tell "剩余生命值: " &newHealth
    
    # 检查是否死亡
    if check &newHealth <= 0 then {
        tell "你已死亡!"
        
        # 复活处理
        health set &playerLevel * 5  # 根据等级设置复活生命值
        tell "复活成功! 生命值恢复至" player health
    }
}

# 违规行为惩罚
if check player is in "no_pvp_zone" and player is attacking then {
    tell "禁止在安全区域进行PVP!"
    call applyHealthPenalty
}

# 任务失败惩罚
if check quest "protect_village" is failed then {
    tell "村庄保护任务失败!"
    call applyHealthPenalty
}
```
**详细讲解**:
- 根据玩家等级调整惩罚力度
- 新手玩家享受惩罚减免
- 动态计算惩罚量
- 实现死亡检测和复活机制
- 适用于违规行为和任务失败的惩罚

## 使用场景

- 治疗技能和恢复效果
- 伤害计算和生命值减少
- 状态惩罚和奖励系统
- 游戏机制中的生命值管理

## 注意事项

- 生命值会被限制在0到最大生命值之间
- 只能对LivingEntity类型的实体使用
- 建议在操作前检查目标是否为有效实体
- 死亡检测应该在生命值操作后进行