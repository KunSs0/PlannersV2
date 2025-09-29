# 药水效果Kether动作

## 概述

本文档介绍Planners插件中用于管理药水效果的Kether脚本动作。

## 药水效果动作

### potion add <id> [level <value>] [duration <ticks>] [amplifier <bool>] [ambient <bool>] [particles <bool>] [at objective]
**功能：** 为实体添加药水效果
**返回类型：** Void
**语法：** `potion add <id> [level <value>] [duration <ticks>] [amplifier <bool>] [ambient <bool>] [particles <bool>] [at objective:TargetContainer(sender)]`

**参数说明：**
- `id` - 药水效果ID（字符串，如：`SPEED`, `REGENERATION`）
- `level` - 效果等级（可选，默认：1）
- `duration` - 持续时间（游戏刻，可选，默认：20）
- `amplifier` - 是否增强效果（可选，默认：false）
- `ambient` - 是否环境效果（可选，默认：false）
- `particles` - 是否显示粒子效果（可选，默认：true）
- `objective` - 目标实体（可选，默认：发送者）

## 使用示例

### 简单语句示例

**示例1：基本药水效果**
```kotlin
potion add SPEED
potion add REGENERATION at @target
```
**讲解：**
- 第一行：为发送者添加速度效果，使用默认参数（等级1，持续时间20刻）
- 第二行：为@target目标添加生命恢复效果

**示例2：指定参数**
```kotlin
potion add STRENGTH level 2 duration 100
potion add JUMP_BOOST level 3 duration 200 particles false
```
**讲解：**
- 第一行：添加2级力量效果，持续100游戏刻
- 第二行：添加3级跳跃提升效果，持续200游戏刻，不显示粒子效果

### 实际功能例子

**例子1：治疗技能效果**
```kotlin
def healSkill = {
    # 为玩家添加生命恢复效果
    potion add REGENERATION level 2 duration 60
    # 添加伤害吸收效果
    potion add ABSORPTION level 1 duration 100
    # 播放治疗音效
    sound entity.player.levelup
    # 显示治疗消息
    tell "你获得了治疗效果！"
}
```
**讲解：**
- 使用2级生命恢复效果持续3秒（60刻）
- 添加1级伤害吸收效果持续5秒（100刻）
- 配合音效和消息提示，模拟完整的治疗技能

**例子2：控制技能效果**
```kotlin
def controlSkill = {
    # 对目标添加缓慢效果
    potion add SLOWNESS level 1 duration 40 at @target
    # 添加失明效果
    potion add BLINDNESS level 1 duration 30 at @target
    # 添加虚弱效果
    potion add WEAKNESS level 1 duration 50 at @target
    # 播放控制音效
    sound entity.enderdragon.growl
    # 显示控制消息
    tell "目标被控制！" at @target
}
```
**讲解：**
- 对目标施加缓慢（2秒）、失明（1.5秒）、虚弱（2.5秒）三重控制效果
- 使用龙吼音效增强控制感
- 向目标发送被控制的消息提示

## 支持的药水效果ID

常用药水效果ID：
- `SPEED` - 速度
- `SLOWNESS` - 缓慢
- `STRENGTH` - 力量
- `WEAKNESS` - 虚弱
- `REGENERATION` - 生命恢复
- `POISON` - 中毒
- `HEALING` - 瞬间治疗
- `HARMING` - 瞬间伤害
- `JUMP_BOOST` - 跳跃提升
- `INVISIBILITY` - 隐身
- `NIGHT_VISION` - 夜视
- `FIRE_RESISTANCE` - 防火
- `WATER_BREATHING` - 水下呼吸

**注意事项：**
- 持续时间单位为游戏刻（20刻=1秒）
- 部分药水效果需要特定Minecraft版本支持
- 无效的药水效果ID会被忽略并发出警告
- 药水效果会异步应用到目标实体