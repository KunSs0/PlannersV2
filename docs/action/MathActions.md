# 数学运算Kether动作

## 概述

本文档介绍Planners插件中用于数学运算的Kether脚本动作。

## 数学运算动作

### cos <value:Number>
**功能：** 计算余弦值
**返回类型：** Double
**语法：** `cos <value:Number>`

### sin <value:Number>
**功能：** 计算正弦值
**返回类型：** Double
**语法：** `sin <value:Number>`

### tan <value:Number>
**功能：** 计算正切值
**返回类型：** Double
**语法：** `tan <value:Number>`

### atan <value:Number>
**功能：** 计算反正切值
**返回类型：** Double
**语法：** `atan <value:Number>`

### radians <value:Number>
**功能：** 角度转弧度
**返回类型：** Double
**语法：** `radians <value:Number>`

### pow <value:Number>
**功能：** 计算幂次方
**返回类型：** Double
**语法：** `pow <value:Number>`

### abs <value:Number>
**功能：** 计算绝对值
**返回类型：** Double
**语法：** `abs <value:Number>`

## 使用示例

### 简单语句示例

**示例1：基础三角函数**
```kotlin
set result1 = cos 0.5
set result2 = sin 1.0
```
**讲解：**
- 第一行：计算0.5弧度的余弦值
- 第二行：计算1.0弧度的正弦值

**示例2：数学运算**
```kotlin
set angle = radians 90
set power = pow 2 3
```
**讲解：**
- 第一行：将90度转换为弧度值
- 第二行：计算2的3次方（2³=8）

### 实际功能例子

**例子1：圆形运动轨迹计算**
```kotlin
def circularMotion = {
    # 设置角度和时间参数
    set radius = 5.0
    set speed = 0.1
    set time = 0
    
    # 计算圆形轨迹
    repeat 20 {
        # 计算当前位置
        set x = radius * cos time
        set y = radius * sin time
        
        # 生成粒子效果显示轨迹
        particle create flame point at [~&x, ~&y, ~0]
        
        # 增加时间
        set time = time + speed
        wait 0.1s
    }
}
```
**讲解：**
- 使用正弦和余弦函数计算圆形轨迹坐标
- 半径5格，速度0.1弧度/秒
- 每0.1秒更新位置并显示火焰粒子
- 模拟20个点的圆形运动轨迹

**例子2：技能伤害公式计算**
```kotlin
def calculateSkillDamage = {
    # 获取玩家属性
    set player_level = profile level
    set base_damage = 50
    set skill_multiplier = 1.5
    
    # 计算等级加成（使用幂函数）
    set level_bonus = pow player_level 1.2
    
    # 计算随机波动（使用三角函数）
    set random_factor = abs sin (time * 2)
    set damage_variance = 0.1 * random_factor
    
    # 最终伤害计算
    set final_damage = base_damage + level_bonus * skill_multiplier
    set final_damage = final_damage * (1 + damage_variance)
    
    # 应用伤害
    damage &final_damage at @target
    
    tell "造成伤害：%final_damage%"
}
```
**讲解：**
- 使用幂函数计算等级加成（等级^1.2）
- 使用正弦函数生成随机波动因子
- 使用绝对值确保波动值始终为正
- 综合计算最终伤害并应用

**例子3：角度和方向计算**
```kotlin
def calculateDirection = {
    # 获取目标位置
    set target_pos = select @nearest @their
    set my_pos = @sender
    
    # 计算方向向量
    set dx = target_pos.x - my_pos.x
    set dy = target_pos.y - my_pos.y
    set dz = target_pos.z - my_pos.z
    
    # 计算水平角度（弧度）
    set horizontal_angle = atan2 dz dx
    
    # 计算垂直角度（弧度）
    set distance = sqrt (dx*dx + dz*dz)
    set vertical_angle = atan2 dy distance
    
    # 转换为角度显示
    set horizontal_deg = degrees horizontal_angle
    set vertical_deg = degrees vertical_angle
    
    tell "目标方向：水平 %horizontal_deg%度，垂直 %vertical_deg%度"
}
```
**讲解：**
- 计算从发送者到目标的向量差
- 使用atan2函数计算水平和垂直角度
- 将弧度转换为角度显示
- 提供精确的方向信息