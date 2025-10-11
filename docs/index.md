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

Selector 模块是 Minecraft 插件的核心目标选择系统，它提供了一套强大的查询语言来精确地筛选游戏中的各种实体目标。就像数据库的 SELECT 查询语句，你可以使用各种条件来过滤和选择想要操作的游戏对象。

### 选择器基本语法

选择器使用统一的语法结构，遵循 `@选择器名称 参数` 的格式：

```kether
# 基本语法格式
select @选择器名称 参数值

# 组合使用示例
set targets to select @range 10 @type player
```

**语法规则:**
- 选择器以 `@` 符号开头
- 参数可以是数字、字符串、数组或表达式
- 多个选择器可以组合使用，形成复合条件
- 选择器返回目标容器，可以配合 `select` 命令使用

### 选择器结构层次

选择器按照功能分为多个层次，从基础筛选到高级组合：

#### 1. 基础选择器（Basic Selectors）
```kether
# 范围选择 - 距离筛选
select @range 10

# 类型选择 - 实体类型筛选  
select @type player

# 名称选择 - 名称匹配
select @name "Steve"
```

#### 2. 数量控制选择器（Quantity Control）
```kether
# 数量限制
select @range 20 @amount 5

# 随机选择
select @type player @shuffle @amount 3

# 排序选择
select @range 15 @amount 1
```

#### 3. 区域选择器（Area Selectors）
```kether
# 矩形区域
select @rectangle-body 10 5 10

# 扇形区域  
select @sector 45 8
```

#### 4. 复合选择器（Compound Selectors）
```kether
# 组合多个条件
select @range 12 @type [ "zombie" "skeleton" ] @amount 3

# 组合选择器筛选
set targets to select @range 20 @type player @amount 5
```

### 选择器工作流程

选择器的执行遵循清晰的流程：

1. **初始选择** → 获取基础目标集合
2. **条件筛选** → 应用各种选择器条件
3. **结果处理** → 排序、限制、随机化等
4. **目标容器** → 返回最终选择结果

### 核心概念解析

#### 目标容器（Target Container）
选择器返回的不是原始实体，而是包含目标实体的容器对象，支持：
- 迭代操作：`for target in &container`
- 数量统计：`count &container`
- 索引访问：`&container[0]`
- 结果过滤：使用组合选择器进行条件筛选

#### 选择器链（Selector Chain）
多个选择器可以形成链式筛选：
```kether
# 链式筛选：范围 → 类型 → 排序 → 数量
set enemies to select @range 15 @type monster @amount 3
```

#### 动态参数（Dynamic Parameters）
参数支持变量和表达式：
```kether
set searchRadius to 10
set targetType to "player"
select @range &searchRadius @type &targetType
```

### 实用示例教程

#### 示例1：链接动作语法示例
```kether
# 链接动作格式：动作 参数1 参数2 at/on/from/through 选择器

# 血量操作示例
health add 10 at @self  # 给自己加10点生命值
health take 5 at @range 3 @type zombie  # 对3格内的僵尸造成5点伤害
health set 20 at @nearest player  # 设置最近的玩家生命值为20

# 速度操作示例
velocity set 0 1 0 at @range 5 @type creeper  # 将5格内的苦力怕向上击飞
velocity add 0.5 0 0 at @type player @amount 1  # 给一个玩家添加X轴速度

# 音效操作示例
sound entity.player.levelup at @self  # 给自己播放升级音效
sound entity.player.hurt at @range 10 @type player  # 给10格内的玩家播放受伤音效
sound resource:custom.magic_sound with 1.0 0.8 at @target  # 给目标播放自定义音效，音量1.0，音高0.8

# 药水效果示例
potion effect "speed" 60 1 at @range 10 @type player  # 给10格内的玩家速度效果60秒1级
potion remove "slowness" from @self  # 移除自己的缓慢效果
```

#### 示例2：智能怪物清理系统
```kether
# 智能怪物清理
set dangerousMonsters to array [ "creeper" "skeleton" "zombie" ]
set targets to select @range 20 @type &dangerousMonsters

for monster in &targets then {
    if check vector length vector sub &monster location player location < 5 then {
        # 近战击杀
        entity remove &monster
        particle create "explosion" at &monster location
    } else {
        # 远程标记
        particle create "flame" at &monster location duration 3000
    }
}
```

#### 示例2：玩家分组系统
```kether
# 按等级分组玩家
set lowLevelPlayers to select @type player @amount 10
set midLevelPlayers to select @type player @amount 10
set highLevelPlayers to select @type player @amount 10

tell "新手玩家: {{ size &lowLevelPlayers }} 人"
tell "中级玩家: {{ size &midLevelPlayers }} 人"  
tell "高级玩家: {{ size &highLevelPlayers }} 人"
```

#### 示例3：区域保护系统
```kether
# 区域入侵检测
def areaProtection = {
    set intruders to select @rectangle-body 20 10 20 @type player @sort distance @amount 3
    
    if check count &intruders > 0 then {
        for intruder in &intruders then {
            tell &intruder "警告: 您已进入受限区域，请立即离开!"
            velocity set 0 -1 0 at &intruder  # 向上推离
        }
    }
}
```

### 选择器索引

| # | 选择器 | 简单说明 | 功能分类 |
|---|--------|----------|----------|
| Amount Selector | [数量选择器](./selector/SelectorAmount.md) | 按数量选择目标 | 数量控制 |
| Entity Type Selector | [实体类型选择器](./selector/SelectorEntityType.md) | 按实体类型选择 | 基础筛选 |
| Index Selector | [索引选择器](./selector/SelectorIndex.md) | 按索引位置选择 | 结果处理 |
| Name Selector | [名称选择器](./selector/SelectorName.md) | 按名称选择目标 | 基础筛选 |
| Range Selector | [范围选择器](./selector/SelectorRange.md) | 按距离范围选择 | 空间筛选 |
| Rectangle Body Selector | [矩形区域选择器](./selector/SelectorRectangleBody.md) | 按矩形区域选择 | 空间筛选 |
| Sector Selector | [扇形区域选择器](./selector/SelectorSector.md) | 按扇形区域选择 | 空间筛选 |
| Shuffle Selector | [随机选择器](./selector/SelectorShuffle.md) | 随机选择目标 | 结果处理 |
| Sort Selector | [排序选择器](./selector/SelectorSort.md) | 对目标进行排序 | 结果处理 |

### 最佳实践建议

1. **性能优化**: 先使用范围选择器限制搜索区域，再应用其他条件
2. **条件顺序**: 将最严格的条件放在前面，减少后续处理的数据量
3. **错误处理**: 总是检查选择结果是否为空，避免空指针错误
4. **可读性**: 使用变量存储复杂的选择器参数，提高代码可读性
5. **测试验证**: 在安全环境中测试选择器逻辑，确保预期行为