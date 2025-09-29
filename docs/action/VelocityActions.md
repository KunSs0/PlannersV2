# 速度控制Kether动作

## 概述

本文档介绍Planners插件中用于控制实体速度的Kether脚本动作。

## 速度控制动作

### velocity move <vector> [at objective]
**功能：** 基于实体朝向移动速度
**返回类型：** Void
**语法：** `velocity move <vector> [at objective:TargetContainer(sender)]`

### velocity set <vector> [at objective]
**功能：** 设置实体速度
**返回类型：** Void
**语法：** `velocity set <vector> [at objective:TargetContainer(sender)]`

### velocity add <vector> [at objective]
**功能：** 增加实体速度
**返回类型：** Void
**语法：** `velocity add <vector> [at objective:TargetContainer(sender)]`

### velocity subtract <vector> [at objective]
**功能：** 减少实体速度
**返回类型：** Void
**语法：** `velocity subtract <vector> [at objective:TargetContainer(sender)]`

### velocity multiply <vector> [at objective]
**功能：** 乘以实体速度
**返回类型：** Void
**语法：** `velocity multiply <vector> [at objective:TargetContainer(sender)]`

### velocity divide <vector> [at objective]
**功能：** 除以实体速度
**返回类型：** Void
**语法：** `velocity divide <vector> [at objective:TargetContainer(sender)]`

### velocity zero [at objective]
**功能：** 重置实体速度为零
**返回类型：** Void
**语法：** `velocity zero [at objective:TargetContainer(sender)]`

## 使用示例

### 简单语句示例

**示例1：基础速度操作**
```kotlin
velocity set [1, 0, 0]
velocity add [0, 2, 0] at @target
```
**讲解：**
- 第一行：设置发送者X轴速度为1（向右移动）
- 第二行：为@target目标增加Y轴速度2（向上移动）

**示例2：基于朝向的移动**
```kotlin
velocity move [0, 0, 3]
velocity move [1, 0, 0] at @target
```
**讲解：**
- 第一行：基于发送者朝向向前移动3个单位
- 第二行：基于目标朝向向右移动1个单位

### 实际功能例子

**例子1：冲锋技能效果**
```kotlin
def chargeSkill = {
    # 向前冲锋
    velocity move [0, 0, 5]
    # 播放冲锋音效
    sound entity.player.attack.sweep
    # 显示特效
    particle create flame point at @sender
    # 冲锋后停止
    wait 1s
    velocity zero
    # 播放落地音效
    sound block.stone.place
}
```
**讲解：**
- 使用`move`基于玩家朝向向前冲锋5个单位
- 配合音效和粒子效果增强冲锋感
- 1秒后停止移动，模拟冲锋结束
- 落地音效提供完成反馈

**例子2：击退技能效果**
```kotlin
def knockbackSkill = {
    # 选择目标
    set targets = select @radius 3 @their
    # 对目标施加击退
    velocity move [0, 0.3, 2] at &targets
    # 播放击退音效
    sound entity.generic.explode
    # 显示击退特效
    particle create cloud point at &targets
    # 显示击退消息
    tell "你被击退了！" at &targets
    # 2秒后停止击退效果
    wait 2s
    velocity zero at &targets
}
```
**讲解：**
- 对半径3格内的敌对目标施加击退
- 使用`move`确保击退方向基于施法者朝向
- Y轴0.3提供轻微上升效果，Z轴2提供向前击退
- 爆炸音效和云粒子增强击退视觉效果
- 2秒后重置目标速度

**向量表示格式：**
- `[x, y, z]` - 标准向量格式
- `{x: 1, y: 2, z: 3}` - 命名向量格式

**注意事项：**
- `move`操作基于实体当前的朝向方向
- 速度向量坐标对应世界坐标系
- 支持小数精度
- 对非生物实体也有效