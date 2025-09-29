# Planners V2

## Kether 模块

Kether 是一个专门为 Minecraft 插件设计的脚本引擎，让玩家和开发者可以用简单的脚本语言来创建复杂的游戏功能。它就像是给 Minecraft 插件加了一个编程语言，让你不用写复杂的 Java 代码就能实现各种功能。

比如你可以用 Kether 来写脚本控制游戏中的各种事件，比如玩家登录时给奖励，完成任务时发放物品，或者创建复杂的游戏机制。它支持变量、函数、条件判断这些基本的编程概念，让写脚本变得简单方便。

Kether 还和很多常用的 Minecraft 插件配合得很好，比如聊天插件、菜单插件、经济系统插件等等，这样你写的脚本就能直接调用这些插件的功能。

## Action 模块

Action 模块是用来执行各种游戏动作的工具，它把复杂的游戏操作封装成简单的指令，让你可以轻松控制游戏中的各种行为。

比如你可以用 Action 模块来执行一些常见的游戏操作，比如给玩家物品、改变玩家状态、触发游戏事件、播放音效和粒子效果等等。它就像是游戏操作的遥控器，你按下一个按钮就能执行对应的动作。

Action 模块支持的动作类型很多，从简单的物品给予到复杂的条件判断都能处理。你还可以把多个动作组合在一起，形成动作链，这样就可以实现更加复杂的游戏逻辑。

### 动作索引

| # | 动作 | 简单说明 | 前置 |
|---|------|----------|------|
| Animated Action | [动画动作](./action/ActionAnimated.md) | 播放动画效果 |  |
| Cooldown Action | [冷却动作](./action/ActionCooldown.md) | 设置技能冷却时间 |  |
| Health Action | [生命值动作](./action/ActionHealth.md) | 控制生命值相关操作 |  |
| Job Action | [职业动作](./action/ActionJob.md) | 职业系统相关操作 |  |
| Math Action | [数学动作](./action/ActionMath.md) | 数学运算操作 |  |
| Metadata Action | [元数据动作](./action/ActionMetadata.md) | 元数据管理操作 |  |
| Particle Action | [粒子效果动作](./action/ActionParticle.md) | 播放粒子效果 |  |
| Profile Action | [档案动作](./action/ActionProfile.md) | 玩家档案管理 |  |
| Skill Action | [技能动作](./action/ActionSkill.md) | 技能系统相关操作 |  |
| Vector Action | [向量动作](./action/ActionVector.md) | 向量计算操作 |  |
| Velocity Action | [速度动作](./action/ActionVelocity.md) | 速度控制操作 |  |
| Bukkit Actions | [Bukkit动作](./action/BukkitActions.md) | Bukkit API相关动作 |  |
| Common Actions | [常用动作](./action/CommonActions.md) | 常用基础动作 |  |
| Damage Actions | [伤害动作](./action/DamageActions.md) | 伤害相关操作 |  |
| Math Actions | [数学动作集](./action/MathActions.md) | 数学运算动作集 |  |
| Particle Actions | [粒子动作集](./action/ParticleActions.md) | 粒子效果动作集 |  |
| Potion Actions | [药水动作](./action/PotionActions.md) | 药水效果相关 |  |
| Profile Actions | [档案动作集](./action/ProfileActions.md) | 玩家档案动作集 |  |
| Sound Actions | [音效动作](./action/SoundActions.md) | 音效播放相关 |  |
| Velocity Actions | [速度动作集](./action/VelocityActions.md) | 速度控制动作集 |  |

## Selector 模块

Selector 模块是用来选择游戏中的各种目标的工具，它可以帮助你精确地找到想要操作的游戏对象。

比如你想给某个区域内的所有玩家发送消息，或者选择特定条件的生物进行某种操作，Selector 模块就能帮你做到。它支持多种选择方式，比如按位置选择（圆形区域、方形区域）、按类型选择（玩家、生物、物品）、按条件选择（等级、权限、状态）等等。

Selector 模块就像是一个智能搜索工具，你可以用各种条件来筛选出想要的目标，然后配合 Action 模块来对这些目标执行相应的操作。它还支持复合选择器，可以把多个选择条件组合起来使用，让选择更加精确和灵活。

### 选择器索引

| # | 选择器 | 简单说明 | 前置 |
|---|--------|----------|------|
| Amount Selector | [数量选择器](./selector/SelectorAmount.md) | 按数量选择目标 |  |
| Entity Type Selector | [实体类型选择器](./selector/SelectorEntityType.md) | 按实体类型选择 |  |
| Index Selector | [索引选择器](./selector/SelectorIndex.md) | 按索引位置选择 |  |
| Name Selector | [名称选择器](./selector/SelectorName.md) | 按名称选择目标 |  |
| Range Selector | [范围选择器](./selector/SelectorRange.md) | 按距离范围选择 |  |
| Rectangle Body Selector | [矩形区域选择器](./selector/SelectorRectangleBody.md) | 按矩形区域选择 |  |
| Sector Selector | [扇形区域选择器](./selector/SelectorSector.md) | 按扇形区域选择 |  |
| Shuffle Selector | [随机选择器](./selector/SelectorShuffle.md) | 随机选择目标 |  |
| Sort Selector | [排序选择器](./selector/SelectorSort.md) | 对目标进行排序 |  |