# 玩家属性Kether动作

## 概述

本文档介绍Planners插件中用于管理玩家档案和属性的Kether脚本动作。

## 魔法值管理

### profile magicpoint get [at objective:TargetContainer(sender)]
**功能：** 获取玩家魔法值
**返回类型：** Int
**语法：** `profile magicpoint get [at <objective>]`

**示例：**
```kotlin
set mp = profile magicpoint get
set target_mp = profile magicpoint get at @target
```

### profile magicpoint [at objective:TargetContainer(sender)]
**功能：** 获取玩家魔法值（主操作）
**返回类型：** Int
**语法：** `profile magicpoint [at <objective>]`

**示例：**
```kotlin
set mp = profile magicpoint
```

### profile magicpoint set <value> [at objective:TargetContainer(sender)]
**功能：** 设置玩家魔法值
**返回类型：** Void
**语法：** `profile magicpoint set <value> [at <objective>]`

**示例：**
```kotlin
profile magicpoint set 100
profile magicpoint set 50 at @target
```

### profile magicpoint add <value> [at objective:TargetContainer(sender)]
**功能：** 增加玩家魔法值
**返回类型：** Void
**语法：** `profile magicpoint add <value> [at <objective>]`

**示例：**
```kotlin
profile magicpoint add 20
profile magicpoint add 10 at @target
```

### profile magicpoint take <value> [at objective:TargetContainer(sender)]
**功能：** 减少玩家魔法值
**返回类型：** Void
**语法：** `profile magicpoint take <value> [at <objective>]`

**示例：**
```kotlin
profile magicpoint take 15
profile magicpoint take 5 at @target
```

### profile magicpoint.max [at objective:TargetContainer(sender)]
**功能：** 获取玩家魔法值上限
**返回类型：** Int
**语法：** `profile magicpoint.max [at <objective>]`
**别名：** `mp.max`, `magicpoint.max`, `mp.upperlimit`

**示例：**
```kotlin
set max_mp = profile magicpoint.max
```

## 玩家基本信息

### profile job
**功能：** 获取玩家职业ID
**返回类型：** String
**语法：** `profile job`

**示例：**
```kotlin
set job_id = profile job
if job_id == "warrior" then {
    tell "你是一名战士"
}
```

### profile level
**功能：** 获取玩家等级
**返回类型：** Int
**语法：** `profile level`

**示例：**
```kotlin
set player_level = profile level
if player_level >= 10 then {
    tell "你已达到10级"
}
```

### profile experience
**功能：** 获取玩家当前经验值
**返回类型：** Int
**语法：** `profile experience`

**示例：**
```kotlin
set exp = profile experience
```

### profile experience.max
**功能：** 获取玩家经验值上限
**返回类型：** Int
**语法：** `profile experience.max`
**别名：** `experience.max`, `max.experience`, `experience-max`, `max-exp`, `max.exp`

**示例：**
```kotlin
set max_exp = profile experience.max
```