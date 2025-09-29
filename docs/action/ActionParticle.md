# ActionParticle

粒子效果相关的动作类，用于创建、显示和控制粒子动画效果。

## 类信息

- **包名**: `com.gitee.planners.module.kether.common`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `particle`

## 动作列表

### `particle create <type> <shape>`

```kether
particle create <type> <shape> [frame: <showFrame>] [duration: <duration>] [at <objective>:TargetContainer(origin)]
```

创建粒子效果对象。

**参数说明**:
- `<type>`: **必填** - 粒子类型，格式为`命名空间:粒子ID`或直接使用粒子ID
- `<shape>`: **必填** - 粒子形状，可选：`point`, `line`, `circle`
- `frame <showFrame>`: **选填** - 是否显示边框，默认为false
- `duration <duration>`: **选填** - 持续时间（毫秒），默认为100
- `at <objective>`: **选填** - 粒子源位置容器，默认为原点

**返回值**: 粒子动画对象（BukkitParticle）

### `particle show <particle>`

```kether
particle show <particle>
```

显示粒子效果。

**参数说明**:
- `<particle>`: **必填** - 粒子动画对象

**返回值**: 无

### `particle stop <particle>`

```kether
particle stop <particle>
```

暂停粒子效果。

**参数说明**:
- `<particle>`: **必填** - 粒子动画对象

**返回值**: 无

### `particle reset <particle>`

```kether
particle reset <particle>
```

重置粒子效果。

**参数说明**:
- `<particle>`: **必填** - 粒子动画对象

**返回值**: 无

## 简单语句示例

### 示例1：创建火焰粒子
```kether
set flameParticle to particle create "flame" "point" at player location
```
**详细讲解**:
- `particle create "flame" "point"` 创建点状火焰粒子
- `at player location` 设置在玩家位置显示
- `set flameParticle to` 将粒子对象存储在变量中

### 示例2：显示圆形粒子效果
```kether
particle show particle create "heart" "circle" duration 2000
```
**详细讲解**:
- `particle create "heart" "circle"` 创建心形圆形粒子
- `duration 2000` 设置持续2秒
- `particle show` 立即显示粒子效果

## 实际功能示例

### 示例1：技能特效粒子系统
```kether
# 火球术粒子特效
def call createFireballEffect = {
    # 创建火球核心粒子
    set coreParticle to particle create "flame" "point" frame true duration 5000 at player location
    
    # 创建火球外围粒子
    set outerParticle to particle create "lava" "circle" frame false duration 5000 at player location
    
    # 设置粒子大小和颜色
    metadata "effect.core.particle" to &coreParticle
    metadata "effect.outer.particle" to &outerParticle
    
    # 显示粒子效果
    particle show &coreParticle
    particle show &outerParticle
    
    tell "火球术准备完成!"
}

# 火球术发射
def call launchFireball = {
    # 获取粒子对象
    set coreParticle to metadata "effect.core.particle"
    set outerParticle to metadata "effect.outer.particle"
    
    if check &coreParticle is null or &outerParticle is null then {
        tell "火球术未准备就绪"
        return
    }
    
    # 计算发射方向
    set direction to vector looking-at of player scale 2.0
    
    # 移动粒子效果
    set fireballPos to vector add player location &direction
    
    # 更新粒子位置
    set &coreParticle[location] to &fireballPos
    set &outerParticle[location] to &fireballPos
    
    # 显示移动轨迹
    particle create "flame" "line" from player location to &fireballPos duration 100
    
    # 碰撞检测
    if check block at &fireballPos is not "air" then {
        # 碰撞爆炸效果
        particle show particle create "explosion" "circle" at &fireballPos duration 1000
        
        # 停止原粒子效果
        particle stop &coreParticle
        particle stop &outerParticle
        
        tell "火球术命中目标!"
    }
}

# 准备火球术
createFireballEffect

# 延迟发射
sleep 2s
launchFireball
```
**详细讲解**:
- 创建多层粒子效果模拟火球
- 使用元数据存储粒子对象
- 计算弹道轨迹并移动粒子
- 实现碰撞检测和爆炸效果
- 完整的火球术粒子特效系统

### 示例2：治疗光环粒子效果
```kether
# 治疗光环粒子效果
def call createHealingAura = {
    # 定义治疗区域
    set center to player location
    set radius to 5.0
    
    # 创建治疗粒子效果
    set healingParticles to array []
    
    # 在圆形区域创建多个粒子点
    foreach range 0 12 as i {
        # 计算角度
        set angle to math &i * 30
        set radian to radians &angle
        
        # 计算位置
        set x to math &radius * cos &radian
        set z to math &radius * sin &radian
        set particlePos to vector create &x 0 &z at &center
        
        # 创建治疗粒子
        set healParticle to particle create "heart" "point" duration 3000 at &particlePos
        
        # 添加到粒子数组
        set &healingParticles[&i] to &healParticle
        
        # 显示粒子
        particle show &healParticle
    }
    
    # 存储粒子数组
    metadata "healing.particles" to &healingParticles
    
    # 开始治疗效果
    metadata "healing.active" to true
    metadata "healing.timer" to 10000  # 10秒治疗时间
    
    tell "治疗光环已激活!"
}

# 治疗光环效果更新
def call updateHealingAura = {
    # 检查是否激活
    set active to metadata "healing.active" def false
    
    if check &active is true then {
        # 更新计时器
        set timer to metadata "healing.timer" def 0
        set newTimer to math &timer - 50
        
        if check &newTimer <= 0 then {
            # 治疗结束
            stopHealingAura
            return
        }
        
        # 更新计时器
        metadata "healing.timer" to &newTimer
        
        # 每2秒治疗一次
        if check &newTimer % 2000 is 0 then {
            # 治疗范围内的玩家
            set nearbyPlayers to entities in radius 5 from player location
            
            foreach &nearbyPlayers as target {
                # 恢复生命值
                set target health to math target health + 2
                
                # 显示治疗效果
                particle create "heart" "point" at target location duration 500
            }
            
            tell "治疗效果生效!"
        }
        
        # 更新粒子位置（跟随玩家移动）
        set particles to metadata "healing.particles"
        set center to player location
        
        if check &particles is not null then {
            foreach &particles as index particle {
                if check &particle is not null then {
                    # 重新计算粒子位置
                    set angle to math &index * 30
                    set radian to radians &angle
                    set x to math 5 * cos &radian
                    set z to math 5 * sin &radian
                    set newPos to vector create &x 0 &z at &center
                    
                    # 更新粒子位置
                    set &particle[location] to &newPos
                }
            }
        }
    }
}

# 停止治疗光环
def stopHealingAura = {
    # 停止所有粒子效果
    set particles to metadata "healing.particles"
    
    if check &particles is not null then {
        foreach &particles as particle {
            if check &particle is not null then {
                particle stop &particle
            }
        }
    }
    
    # 清除状态
    metadata "healing.active" remove
    metadata "healing.timer" remove
    metadata "healing.particles" remove
    
    tell "治疗光环效果结束"
}

# 激活治疗光环
createHealingAura

# 每tick更新效果
updateHealingAura
```
**详细讲解**:
- 创建圆形排列的粒子效果
- 实现粒子跟随玩家移动
- 定期治疗范围内的玩家
- 完整的治疗光环系统
- 粒子效果与游戏机制的深度结合

## 支持的粒子形状

### point（点）
- 单个粒子点
- 适用于简单效果

### line（线）
- 线性粒子效果
- 适用于轨迹和路径

### circle（圆）
- 圆形粒子效果
- 适用于光环和范围效果

## 使用场景

- 技能特效和视觉效果
- 状态指示和标记
- 环境效果和氛围
- 游戏机制可视化

## 注意事项

- 粒子效果可能对性能有影响
- 建议合理设置持续时间和粒子数量
- 及时停止不再需要的粒子效果
- 使用帧显示可以调试粒子位置