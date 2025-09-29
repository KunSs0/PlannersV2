# Bukkit相关Kether动作

## 概述

本文档介绍Planners插件中与Bukkit实体和世界交互相关的Kether脚本动作。

## 动作列表

### freeze <tick> [at objective:TargetContainer(sender)]
**功能：** 冻结实体
**返回类型：** Void
**语法：** `freeze <tick> [at <objective>]`

**参数说明：**
- `tick` - 冻结时间（单位：游戏刻）
- `objective` - 目标实体（可选，默认：发送者）

**示例：**
```kotlin
freeze 100
freeze 200 at @target
```

### fire <tick> [at objective:TargetContainer(sender)]
**功能：** 设置实体着火时间
**返回类型：** Void
**语法：** `fire <tick> [at <objective>]`

**参数说明：**
- `tick` - 着火时间（单位：游戏刻）
- `objective` - 目标实体（可选，默认：发送者）

**示例：**
```kotlin
fire 60
fire 120 at @target
```

### explosion <power> <fire:bool(false)> <break:bool(false)> [at objective:TargetContainer(sender)]
**功能：** 创建爆炸
**返回类型：** Void
**语法：** `explosion <power> <fire> <break> [at <objective>]`

**参数说明：**
- `power` - 爆炸威力（浮点数）
- `fire` - 是否着火（可选，默认：false）
- `break` - 是否破坏方块（可选，默认：false）
- `objective` - 目标位置（可选，默认：发送者位置）

**示例：**
```kotlin
explosion 4.0 true false
explosion 2.0 false true at @location
```