# ActionVector

向量操作相关的动作类，用于3D向量计算和空间变换。

## 类信息

- **包名**: `com.gitee.planners.module.kether.math`
- **类型**: `object` (单例对象)
- **继承**: `MultipleKetherParser`
- **命名空间**: `vector`

## 动作列表

### `vector`

```kether
vector [at <objective>:TargetContainer(sender)]
```

获取执行者的位置向量。

**参数说明**:
- `at <objective>`: **选填** - 目标容器，默认为执行者

**返回值**: 位置向量（Vector对象）

### `vector create <x> <y> <z>`

```kether
vector create <x> <y> <z> [at <objective>:TargetContainer(origin)]
```

创建新的向量，支持相对位置符号。

**参数说明**:
- `<x>`: **必填** - X坐标，支持`~`前缀表示相对位置
- `<y>`: **必填** - Y坐标，支持`~`前缀表示相对位置
- `<z>`: **必填** - Z坐标，支持`~`前缀表示相对位置
- `at <objective>`: **选填** - 基准位置容器，默认为原点

**返回值**: 新的向量对象

### `vector looking-at`

```kether
vector looking-at [of <objective>:TargetContainer(sender)] [scale <scale>]
```

获取目标看向方向的单位向量。

**参数说明**:
- `of <objective>`: **选填** - 目标容器，默认为执行者
- `scale <scale>`: **选填** - 缩放比例，默认为1.0

**返回值**: 看向方向的向量

### `vector copy <vector>`

```kether
vector copy <vector>
```

复制向量。

**参数说明**:
- `<vector>`: **必填** - 要复制的向量

**返回值**: 新的向量副本

### `vector add <vector1> <vector2>`

```kether
vector add <vector1> <vector2>
```

向量加法。

**参数说明**:
- `<vector1>`: **必填** - 第一个向量
- `<vector2>`: **必填** - 第二个向量

**返回值**: 向量相加结果

### `vector norm <vector>`

```kether
vector norm <vector>
```

向量归一化（单位化）。

**参数说明**:
- `<vector>`: **必填** - 要归一化的向量

**返回值**: 单位向量

### `vector length <vector>`

```kether
vector length <vector>
```

计算向量长度。

**参数说明**:
- `<vector>`: **必填** - 要计算长度的向量

**返回值**: 向量长度（标量）

### `vector dot <vector1> <vector2>`

```kether
vector dot <vector1> <vector2>
```

计算向量点积。

**参数说明**:
- `<vector1>`: **必填** - 第一个向量
- `<vector2>`: **必填** - 第二个向量

**返回值**: 点积结果（标量）

### `vector cross <vector1> <vector2>`

```kether
vector cross <vector1> <vector2>
```

计算向量叉积。

**参数说明**:
- `<vector1>`: **必填** - 第一个向量
- `<vector2>`: **必填** - 第二个向量

**返回值**: 叉积向量

### `vector scale <vector> <scale>`

```kether
vector scale <vector> <scale>
```

向量缩放。

**参数说明**:
- `<vector>`: **必填** - 要缩放的向量
- `<scale>`: **必填** - 缩放比例

**返回值**: 缩放后的向量

### `vector angle <vector1> <vector2>`

```kether
vector angle <vector1> <vector2>
```

计算向量夹角。

**参数说明**:
- `<vector1>`: **必填** - 第一个向量
- `<vector2>`: **必填** - 第二个向量

**返回值**: 夹角（弧度）

### `vector rotate <vector> <angle> <axis>`

```kether
vector rotate <vector> <angle> <axis>
```

绕任意轴旋转向量。

**参数说明**:
- `<vector>`: **必填** - 要旋转的向量
- `<angle>`: **必填** - 旋转角度（弧度）
- `<axis>`: **必填** - 旋转轴向量

**返回值**: 旋转后的向量

### `vector rotate-on <vector> <angle> <axis>`

```kether
vector rotate-on <vector> <angle> <axis>
```

绕坐标轴旋转向量。

**参数说明**:
- `<vector>`: **必填** - 要旋转的向量
- `<angle>`: **必填** - 旋转角度（弧度）
- `<axis>`: **必填** - 坐标轴，可选：`x`, `y`, `z`

**返回值**: 旋转后的向量

## 简单语句示例

### 示例1：获取玩家位置向量
```kether
set playerPos to vector
```
**详细讲解**:
- `vector` 获取当前执行者的位置向量
- `set playerPos to` 将位置向量存储在变量中
- 便于后续进行向量运算

### 示例2：创建相对位置向量
```kether
set offset to vector create ~2 ~0 ~1
```
**详细讲解**:
- `vector create ~2 ~0 ~1` 创建相对向量
- `~2` 表示X轴相对当前位置+2
- `~0` 表示Y轴不变
- `~1` 表示Z轴相对当前位置+1

## 实际功能示例

### 示例1：环绕飞行效果
```kether
# 环绕飞行效果
def createOrbitEffect = {
    # 定义环绕中心
    set center to vector create 100 64 100
    
    # 定义环绕半径和高度
    set radius to 5.0
    set height to 3.0
    
    # 获取当前时间作为角度
    set currentTime to world time
    set angle to math &currentTime * 0.1
    
    # 计算环绕位置
    set orbitX to math &radius * cos &angle
    set orbitZ to math &radius * sin &angle
    set orbitY to &height
    
    # 创建环绕位置向量
    set orbitPos to vector create &orbitX &orbitY &orbitZ
    
    # 相对于中心位置
    set finalPos to vector add &center &orbitPos
    
    # 生成粒子效果
    particle create "flame" at location &finalPos
    
    return &finalPos
}

# 环绕飞行实体
def orbitEntity = {
    # 获取环绕位置
    set orbitLocation to createOrbitEffect
    
    # 移动实体到环绕位置
    teleport entity to location &orbitLocation
    
    # 计算看向中心的方向
    set center to vector create 100 64 100
    set direction to vector norm vector sub &center &orbitLocation
    
    # 设置实体朝向
    set entity direction to &direction
}

# 每tick更新环绕效果
orbitEntity
```
**详细讲解**:
- 使用三角函数计算圆形轨迹
- `vector create` 创建位置向量
- `vector add` 计算相对位置
- `vector norm` 获取单位向量用于方向
- 实现平滑的环绕飞行效果

### 示例2：弹射物轨迹计算
```kether
# 弹射物轨迹计算
def calculateProjectileTrajectory = {
    # 获取发射位置和目标位置
    set launchPos to vector
    set targetPos to vector create 200 70 150
    
    # 计算方向向量
    set direction to vector sub &targetPos &launchPos
    
    # 计算距离和高度差
    set distance to vector length &direction
    set heightDiff to math &targetPos[1] - &launchPos[1]
    
    # 计算发射角度（考虑重力）
    set gravity to 9.8
    set speed to 25.0
    
    # 使用抛物线公式计算角度
    set angleRad to math atan (pow &speed 2 + sqrt pow &speed 4 - &gravity * (&gravity * pow &distance 2 + 2 * &heightDiff * pow &speed 2)) / (&gravity * &distance))
    
    # 分解速度向量
    set vx to math &speed * cos &angleRad
    set vy to math &speed * sin &angleRad
    
    # 创建速度向量
    set velocity to vector create &vx &vy 0
    
    # 旋转到目标方向
    set forward to vector norm &direction
    set up to vector create 0 1 0
    set right to vector cross &forward &up
    
    # 构建旋转矩阵（简化）
    set rotatedVelocity to vector add 
        vector scale &forward &velocity[0]
        vector add
            vector scale &up &velocity[1]
            vector scale &right &velocity[2]
    
    return &rotatedVelocity
}

# 发射弹射物
def launchProjectile = {
    # 计算弹道
    set trajectory to calculateProjectileTrajectory
    
    # 创建弹射物
    set projectile to entity create "arrow" at player location
    
    # 设置弹射物速度
    set projectile velocity to &trajectory
    
    # 显示弹道轨迹
    particle create "critical" along path from player location to add player location &trajectory
    
    tell "弹射物发射!"
}

# 执行发射
launchProjectile
```
**详细讲解**:
- 使用向量运算计算弹道轨迹
- `vector sub` 计算方向向量
- `vector length` 计算距离
- `vector cross` 计算垂直向量
- `vector scale` 缩放向量分量
- 实现真实的物理弹道模拟

## 使用场景

- 3D空间位置计算
- 物理运动轨迹模拟
- 动画效果和路径规划
- 游戏机制中的向量运算

## 注意事项

- 相对位置符号`~`只能在`vector create`中使用
- 向量运算可能产生新的向量对象
- 建议对重要计算结果进行验证
- 使用`vector norm`确保方向向量的正确性