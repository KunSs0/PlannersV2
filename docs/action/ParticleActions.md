# 粒子效果Kether动作

## 概述

本文档介绍Planners插件中用于创建和控制粒子效果的Kether脚本动作。

## 粒子效果动作

### particle create <type> <shape> [frame <bool>] [duration <number>] [at objective]
**功能：** 创建粒子效果
**返回类型：** BukkitParticle
**语法：** `particle create <type particle:string> <shape:string> [frame:bool(false)] [duration:number(100)] [at objective:TargetContainer(origin)]`

### particle show <animated>
**功能：** 显示粒子效果
**返回类型：** Void
**语法：** `particle show <animated:BukkitParticle>`

### particle stop <animated>
**功能：** 停止粒子效果
**返回类型：** Void
**语法：** `particle stop <animated:particle animated>`

### particle reset <animated>
**功能：** 重置粒子效果
**返回类型：** Void
**语法：** `particle reset <animated:particle animated>`

## 使用示例

### 简单语句示例

**示例1：基本粒子效果**
```kotlin
set flame = particle create flame point
particle show flame
```
**讲解：**
- 第一行：创建点状火焰粒子效果
- 第二行：显示创建的粒子效果

**示例2：带参数的粒子效果**
```kotlin
set circle_particle = particle create heart circle frame true duration 200 at @location
particle show circle_particle
```
**讲解：**
- 创建圆形心形粒子效果，显示框线，持续200刻
- 在指定位置显示粒子效果

### 实际功能例子

**例子1：魔法技能特效**
```kotlin
def magicSkill = {
    # 创建魔法特效
    set magicEffect = particle create enchant circle frame true duration 300 at @sender
    # 显示粒子效果
    particle show magicEffect
    # 播放魔法音效
    sound entity.evoker.cast_spell
    # 显示施法消息
    tell "你施放了魔法！"
    # 等待3秒
    wait 3s
    # 停止粒子效果
    particle stop magicEffect
}
```
**讲解：**
- 创建带框线的圆形附魔粒子效果，持续15秒
- 配合魔法音效和消息提示，模拟完整的魔法施放过程
- 3秒后自动停止粒子效果

**例子2：治疗光环特效**
```kotlin
def healingAura = {
    # 创建治疗光环
    set healEffect = particle create minecraft:happy_villager circle frame false duration 600 at @sender
    # 显示治疗光环
    particle show healEffect
    # 播放治疗音效
    sound entity.player.levelup
    # 显示治疗消息
    tell "你获得了治疗光环！"
    # 每2秒为玩家恢复生命
    repeat 5 {
        wait 2s
        health add 2
        tell "生命值恢复+2"
    }
    # 停止治疗光环
    particle stop healEffect
    tell "治疗光环效果结束"
}
```
**讲解：**
- 创建圆形村民快乐粒子效果作为治疗光环
- 持续30秒，每2秒恢复2点生命值
- 配合音效和消息提示，模拟治疗光环效果
- 5次循环后停止粒子效果

## 支持的粒子形状

### point
点状粒子效果，在指定位置显示单个粒子

### line
线状粒子效果，显示为直线粒子流

### circle
圆形粒子效果，显示为圆形粒子环

## 常用粒子类型

- `flame` - 火焰
- `heart` - 心形
- `enchant` - 附魔
- `cloud` - 云
- `smoke` - 烟雾
- `lava` - 岩浆
- `water` - 水
- `portal` - 传送门

**注意事项：**
- 粒子类型需要与Minecraft支持的粒子名称匹配
- 粒子效果需要指定目标位置才能显示
- 持续时间单位为游戏刻（20刻=1秒）
- 框线模式可能不适用于所有粒子类型