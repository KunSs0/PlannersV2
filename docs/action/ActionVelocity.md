# ActionVelocity

速度管理相关的动作类，用于设置、修改实体的速度向量。

## 类信息

- **包名**: `com.gitee.planners.module.kether.bukkit`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `velocity`

## 动作列表

### `velocity set <vector>`

```kether
velocity set <vector> [at <objective>:TargetContainer(sender)]
```

设置实体的速度向量。

**参数说明**:
- `<vector>`: **必填** - 速度向量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `velocity add <vector>`

```kether
velocity add <vector> [at <objective>:TargetContainer(sender)]
```

为实体增加速度向量。

**参数说明**:
- `<vector>`: **必填** - 要增加的速度向量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `velocity subtract <vector>`

```kether
velocity subtract <vector> [at <objective>:TargetContainer(sender)]
```

从实体减去速度向量。

**参数说明**:
- `<vector>`: **必填** - 要减去的速度向量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `velocity multiply <vector>`

```kether
velocity multiply <vector> [at <objective>:TargetContainer(sender)]
```

将实体的速度向量与指定向量相乘。

**参数说明**:
- `<vector>`: **必填** - 要相乘的向量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `velocity divide <vector>`

```kether
velocity divide <vector> [at <objective>:TargetContainer(sender)]
```

将实体的速度向量与指定向量相除。

**参数说明**:
- `<vector>`: **必填** - 要相除的向量
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `velocity zero`

```kether
velocity zero [at <objective>:TargetContainer(sender)]
```

将实体的速度向量归零。

**参数说明**:
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

### `velocity move <vector>`

```kether
velocity move <vector> [at <objective>:TargetContainer(sender)]
```

基于实体朝向移动实体。

**参数说明**:
- `<vector>`: **必填** - 移动向量（基于实体朝向）
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 无

## 简单语句示例

### 示例1：设置向上速度
```kether
velocity set vector create 0 2 0 at player
```
**详细讲解**:
- `velocity set` 设置速度向量
- `vector create 0 2 0` 创建向上的速度向量
- `at player` 指定目标为玩家
- 执行后玩家会向上飞起

### 示例2：增加向前速度
```kether
velocity add vector looking-at scale 2 at &target
```
**详细讲解**:
- `velocity add` 增加速度向量
- `vector looking-at scale 2` 获取看向方向并放大2倍
- `at &target` 指定目标为变量中的目标
- 执行后目标会加速向前移动

## 实际功能示例

### 示例1：弹跳技能效果
```kether
# 弹跳技能效果
def bounceEffect = {
    # 获取玩家当前速度
    set currentVelocity to player velocity
    
    # 计算弹跳方向（主要向上，略带随机）
    set bounceHeight to 2.0
    set randomX to math random -0.5 0.5
    set randomZ to math random -0.5 0.5
    
    # 创建弹跳向量
    set bounceVector to vector create &randomX &bounceHeight &randomZ
    
    # 设置弹跳速度
    velocity set &bounceVector at player
    
    # 显示弹跳效果
    particle create "cloud" at player location duration 500
    
    tell "弹跳!"
}

# 地面弹跳检测
def checkGroundBounce = {
    # 检查玩家是否在地面附近
    set groundCheck to block at player location offset 0 -1 0
    
    if check &groundCheck is "air" then {
        # 在空中，应用重力减速
        set gravityVector to vector create 0 -0.1 0
        velocity add &gravityVector at player
        
        # 限制下降速度
        set currentVelocity to player velocity
        if check &currentVelocity[1] < -2 then {
            set newVelocity to vector create &currentVelocity[0] -2 &currentVelocity[2]
            velocity set &newVelocity at player
        }
    } else {
        # 在地面，可以弹跳
        metadata "can_bounce" to true
    }
}

# 使用弹跳技能
if check metadata "can_bounce" def false is true then {
    # 消耗魔法值
    if check profile mp >= 15 then {
        profile mp take 15
        
        # 执行弹跳
        bounceEffect
        
        # 重置弹跳状态
        metadata "can_bounce" to false
    } else {
        tell "魔法值不足，无法弹跳"
    }
}

# 每tick检查地面状态
checkGroundBounce
```
**详细讲解**:
- 实现基于地面检测的弹跳机制
- 在空中时应用重力减速
- 限制最大下降速度防止摔伤
- 结合魔法值消耗和粒子效果
- 完整的弹跳技能系统

### 示例2：冲击波技能效果
```kether
# 冲击波技能效果
def shockwaveEffect = {
    # 获取玩家朝向
    set direction to vector looking-at of player
    
    # 计算冲击波强度（基于技能等级）
    set skillLevel to skill level "shockwave"
    set wavePower to math &skillLevel * 1.5
    
    # 创建冲击波向量
    set shockwaveVector to vector scale &direction &wavePower
    
    # 找到前方范围内的敌人
    set enemies to entities in radius 10 from player location where entity type is "monster"
    
    # 对每个敌人应用冲击波
    foreach &enemies as enemy {
        # 计算从玩家到敌人的方向
        set toEnemy to vector sub enemy location player location
        set distance to vector length &toEnemy
        
        # 根据距离衰减冲击力
        set distanceFactor to math 1.0 - (&distance / 10.0)
        set actualForce to vector scale &shockwaveVector &distanceFactor
        
        # 应用冲击力
        velocity add &actualForce at &enemy
        
        # 显示冲击效果
        particle create "explosion" at enemy location duration 200
        
        # 造成伤害（基于冲击力）
        set damage to math &wavePower * &distanceFactor * 2
        health take &damage at &enemy
        
        tell "冲击波命中 " enemy name "!"
    }
    
    # 玩家反冲效果
    set recoilVector to vector scale &direction -0.5  # 轻微反冲
    velocity add &recoilVector at player
    
    tell "冲击波释放!"
}

# 冲击波冷却管理
def shockwaveCooldown = {
    set cooldownTime to cooldown skill "shockwave"
    
    if check &cooldownTime > 0 then {
        set remainingSeconds to math &cooldownTime / 1000
        tell "冲击波冷却中，剩余 " &remainingSeconds " 秒"
        return false
    }
    
    return true
}

# 使用冲击波技能
if check shockwaveCooldown and profile mp >= 30 then {
    # 设置冷却时间
    cooldown set 10000 skill "shockwave"
    
    # 消耗魔法值
    profile mp take 30
    
    # 执行冲击波
    shockwaveEffect
} else {
    tell "无法使用冲击波技能"
}
```
**详细讲解**:
- 基于玩家朝向计算冲击波方向
- 根据技能等级和距离衰减冲击力
- 对范围内敌人应用冲击效果
- 实现玩家反冲物理效果
- 完整的冲击波技能系统

## 使用场景

- 物理效果和运动控制
- 技能冲击和弹射效果
- 环境互动和物理模拟
- 游戏机制中的运动控制

## 注意事项

- 速度向量会立即影响实体运动
- 建议合理控制速度大小防止过度移动
- 只能对LivingEntity类型的实体使用
- 使用前应检查目标是否为有效实体