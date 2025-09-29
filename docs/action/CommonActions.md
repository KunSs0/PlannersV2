# 通用Kether动作

## 概述

本文档介绍Planners插件中通用的Kether脚本动作，这些动作可以在任何Kether脚本中使用。

## 动作列表

### lazy <id>
**功能：** 延迟获取变量值
**返回类型：** Any
**语法：** `lazy <变量名>`

**示例：**
```kotlin
lazy myVariable
```

### tell <message> <at <objective...>>
**功能：** 向目标发送消息
**返回类型：** Void
**语法：** `tell <消息内容> <at <目标对象>>`

**示例：**
```kotlin
tell "Hello World!"
tell "You have been healed!" at @target
```

### chance <value:double>
**功能：** 概率判断
**返回类型：** Boolean
**语法：** `chance <概率值>`

**说明：** 根据给定的概率值（0.0-1.0）返回true或false

**示例：**
```kotlin
if chance 0.3 then {
    tell "幸运触发！"
}
```

### inline <text>
**功能：** 内联函数执行
**返回类型：** String
**语法：** `inline <脚本内容>` 或 `function <脚本内容>`

**说明：** 执行内联的Kether脚本并返回结果

**示例：**
```kotlin
set result = inline "1 + 2"
tell "计算结果：%result%"
```