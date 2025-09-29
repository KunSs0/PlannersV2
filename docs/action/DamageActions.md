# 伤害和生命值Kether动作

## 概述

本文档介绍Planners插件中与伤害和生命值管理相关的Kether脚本动作。

## 伤害动作

### damage <value> [at objective] [source objective]
**功能：** 对实体造成伤害
**返回类型：** Void
**语法：** `damage <value:number> [at objective:TargetContainer(empty)] [source objective:TargetContainer(sender)]`

**参数说明：**
- `value` - 伤害值（数字类型）
- `objective` - 目标实体（可选，默认：空）
- `source` - 伤害来源实体（可选，默认：发送者）

**说明：**
- 支持设置伤害来源，会触发相应的死亡事件
- 避免自我伤害（伤害来源和目标相同时不会生效）
- 兼容不同Minecraft版本的击杀者设置

**示例：**
```kotlin
damage 10
damage 5 at @target
damage 8 at @target source @killer
```

## 生命值管理

### health add <amount> [at objective]
**功能：** 增加实体生命值
**返回类型：** Void
**语法：** `health add <amount> [at objective:TargetContainer(sender)]`

**示例：**
```kotlin
health add 10
health add 5 at @target
```

### health set <amount> [at objective]
**功能：** 设置实体生命值
**返回类型：** Void
**语法：** `health set <amount> [at objective:TargetContainer(sender)]`

**说明：** 生命值会被限制在0到最大生命值之间

**示例：**
```kotlin
health set 20
health set 15 at @target
```

### health take <amount> [at objective]
**功能：** 减少实体生命值
**返回类型：** Void
**语法：** `health take <amount> [at objective:TargetContainer(sender)]`

**示例：**
```kotlin
health take 8
health take 3 at @target
```