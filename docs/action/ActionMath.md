# ActionMath

数学运算相关的动作类，提供基础的数学函数和三角函数。

## 类信息

- **包名**: `com.gitee.planners.module.kether.math`
- **类型**: 独立函数

## 动作列表

### `cos <value>`

```kether
cos <value>
```

计算余弦值。

**参数说明**:
- `<value>`: **必填** - 角度值（弧度）

**返回值**: 余弦值（-1到1之间的浮点数）

### `sin <value>`

```kether
sin <value>
```

计算正弦值。

**参数说明**:
- `<value>`: **必填** - 角度值（弧度）

**返回值**: 正弦值（-1到1之间的浮点数）

### `radians <value>`

```kether
radians <value>
```

将角度转换为弧度。

**参数说明**:
- `<value>`: **必填** - 角度值

**返回值**: 弧度值

### `pow <value> <exponent>`

```kether
pow <value> <exponent>
```

计算幂运算。

**参数说明**:
- `<value>`: **必填** - 底数
- `<exponent>`: **必填** - 指数

**返回值**: 计算结果

### `tan <value>`

```kether
tan <value>
```

计算正切值。

**参数说明**:
- `<value>`: **必填** - 角度值（弧度）

**返回值**: 正切值

### `atan <value>`

```kether
atan <value>
```

计算反正切值。

**参数说明**:
- `<value>`: **必填** - 数值

**返回值**: 角度值（弧度）

### `abs <value>`

```kether
abs <value>
```

计算绝对值。

**参数说明**:
- `<value>`: **必填** - 数值

**返回值**: 绝对值

## 简单语句示例

### 示例1：计算角度
```kether
set angle to math 45
set radian to radians &angle
set cosine to cos &radian
```
**详细讲解**:
- `math 45` 创建数值45
- `radians &angle` 将45度转换为弧度
- `cos &radian` 计算余弦值
- 结果存储在变量中供后续使用

### 示例2：计算平方
```kether
set result to pow 5 2
```
**详细讲解**:
- `pow 5 2` 计算5的平方
- 结果为25
- 存储在变量result中

## 实际功能示例

### 示例1：圆形运动轨迹计算
```kether
# 圆形运动轨迹计算
def calculateCircularMotion = {
    # 定义圆的基本参数
    set centerX to 100
    set centerY to 64
    set radius to 10
    set angle to metadata "motion.angle" def 0
    
    # 增加角度
    set angle to math &angle + 0.1
    
    # 计算新位置
    set radian to radians &angle
    set newX to math &centerX + &radius * cos &radian
    set newY to math &centerY + &radius * sin &radian
    
    # 更新位置
    metadata "motion.angle" to &angle
    metadata "entity.position.x" to &newX
    metadata "entity.position.y" to &newY
    
    # 返回新位置
    return array [ &newX &newY ]
}

# 动画实体圆形运动
def call animateCircularMotion = {
    # 获取当前位置
    set currentPos to calculateCircularMotion
    
    # 移动实体到新位置
    teleport entity to location &currentPos[0] &currentPos[1] &currentPos[2]
    
    # 显示轨迹点
    particle create "flame" at location &currentPos[0] &currentPos[1] &currentPos[2]
}

# 每tick执行动画
animateCircularMotion
```
**详细讲解**:
- 使用三角函数计算圆形轨迹
- `cos` 和 `sin` 函数计算x和y坐标
- `radians` 将角度转换为弧度
- 实现平滑的圆形动画效果
- 结合粒子效果显示运动轨迹

### 示例2：弹道抛物线计算
```kether
# 弹道抛物线计算
def calculateProjectilePath = {
    # 定义发射参数
    set launchAngle to 45  # 发射角度
    set launchSpeed to 20  # 发射速度
    set gravity to 9.8     # 重力加速度
    set time to metadata "projectile.time" def 0
    
    # 转换为弧度
    set angleRad to radians &launchAngle
    
    # 计算速度分量
    set vx to math &launchSpeed * cos &angleRad
    set vy to math &launchSpeed * sin &angleRad
    
    # 计算当前位置
    set x to math &vx * &time
    set y to math &vy * &time - 0.5 * &gravity * pow &time 2
    
    # 更新时间
    set time to math &time + 0.1
    
    # 检查是否落地
    if check &y < 0 then {
        # 抛物线结束
        metadata "projectile.active" to false
        tell "弹道计算完成"
        return null
    }
    
    # 更新状态
    metadata "projectile.time" to &time
    
    # 返回当前位置
    return array [ &x &y 0 ]
}

# 抛物线弹道模拟
def call simulateProjectile = {
    # 检查是否激活
    set active to metadata "projectile.active" def false
    
    if check &active is true then {
        # 计算下一位置
        set nextPos to calculateProjectilePath
        
        if check &nextPos is not null then {
            # 显示弹道轨迹
            particle create "flame" at location &nextPos[0] &nextPos[1] &nextPos[2]
            
            # 检查碰撞
            if check block at &nextPos is not "air" then {
                # 发生碰撞
                explosion 2.0 false true at location &nextPos[0] &nextPos[1] &nextPos[2]
                metadata "projectile.active" to false
            }
        }
    }
}

# 发射弹道
def launchProjectile = {
    # 重置状态
    metadata "projectile.time" to 0
    metadata "projectile.active" to true
    
    tell "弹道发射!"
}

# 每tick更新弹道
simulateProjectile
```
**详细讲解**:
- 使用三角函数分解发射速度
- 抛物线运动公式计算轨迹
- `pow` 函数计算时间的平方
- 实现真实的物理弹道模拟
- 结合碰撞检测和爆炸效果

## 使用场景

- 物理运动轨迹计算
- 动画效果和路径规划
- 游戏机制中的数学运算
- 特效和粒子系统的控制

## 注意事项

- 三角函数使用弧度制而非角度制
- 使用 `radians` 函数进行角度转换
- 数学运算可能产生浮点数结果
- 建议对重要计算结果进行范围检查