# 音效Kether动作

## 概述

本文档介绍Planners插件中用于播放音效的Kether脚本动作。

## 音效动作

### sound <source> [with <volume> <pitch>] [at objective]
**功能：** 播放音效
**返回类型：** Void
**语法：** `sound <source> [with <volume(1)> <pitch(1)>] [at objective:TargetContainer(sender)]`

**参数说明：**
- `source` - 音效源（字符串）
  - 标准Bukkit音效：直接使用音效名称（如：`entity.experience_orb.pickup`）
  - 资源音效：使用`resource:`前缀（如：`resource:custom.sound`）
- `volume` - 音量（可选，默认：1.0）
- `pitch` - 音高（可选，默认：1.0）
- `objective` - 目标实体（可选，默认：发送者）

**音效源类型：**
1. **Bukkit标准音效**：直接使用Bukkit的音效名称
2. **资源音效**：使用`resource:`前缀，指向自定义音效文件

**示例：**
```kotlin
sound entity.player.levelup
sound block.anvil.land
sound resource:custom.magic_sound
sound entity.experience_orb.pickup with 0.5 1.2
sound entity.player.hurt at @target
sound resource:custom.heal_sound with 1.0 0.8 at @target
sound entity.enderdragon.growl with 2.0 0.5 at @all_targets
```