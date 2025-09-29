# SelectorName

名称选择器，用于根据实体名称筛选目标。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `name`

## 语法

```kether
name <namePattern> [rule: <matchingRule>]
```

选择符合名称模式的目标实体。

**参数说明**:
- `<namePattern>`: **必填** - 名称匹配模式
- `rule <matchingRule>`: **选填** - 匹配规则，可选：`fuzzy`(模糊), `strict`(严格)，默认为`fuzzy`

**返回值**: 包含符合名称条件的实体的目标容器

## 匹配规则说明

### FUZZY（模糊匹配）
- 检查实体名称是否包含指定的模式
- 不区分大小写
- 支持部分匹配

### STRICT（严格匹配）
- 检查实体名称是否完全等于指定模式
- 区分大小写
- 必须完全匹配

## 简单语句示例

### 示例1：模糊匹配名称
```kether
select name "boss"
```
**详细讲解**:
- `name "boss"` 选择名称包含"boss"的实体
- 使用默认的模糊匹配规则
- 返回所有名称包含"boss"的实体

### 示例2：严格匹配名称
```kether
select name "FinalBoss" rule strict
```
**详细讲解**:
- `name "FinalBoss"` 选择名称完全等于"FinalBoss"的实体
- `rule strict` 使用严格匹配规则
- 只返回名称完全匹配的实体

## 实际功能示例

### 示例1：BOSS战管理系统
```kether
# BOSS战管理系统
def bossBattleManager = {
    # 搜索附近的BOSS级实体
    set bosses to select name ["boss", "Boss", "BOSS", "首领", "王"]
    
    # 检查是否有BOSS存在
    if check count &bosses > 0 then {
        # 获取最近的BOSS
        set nearestBoss to sort &bosses by distance from player location limit 1
        set boss to &nearestBoss[0]
        set bossName to boss name
        set distance to math vector length vector sub boss location player location
        
        # BOSS战开始提示
        tell "=== BOSS战开始 ==="
        tell "BOSS名称: " &bossName
        tell "距离: " &distance " 格"
        tell "生命值: " boss health "/" boss max_health
        
        # BOSS战特殊效果
        particle create "large_smoke" at boss location duration 3000
        play sound "entity.ender_dragon.growl" at boss location
        
        # 设置BOSS战状态
        metadata "boss_battle.active" to true
        metadata "boss_battle.target" to &boss
        metadata "boss_battle.start_time" to world time
        
        # BOSS特殊技能触发
        if check &bossName contains "火焰" then {
            # 火焰BOSS技能
            particle create "flame" around boss location radius 5 duration 5000
            tell "🔥 火焰BOSS激活了火焰领域!"
        } else if check &bossName contains "冰霜" then {
            # 冰霜BOSS技能
            particle create "snowflake" around boss location radius 5 duration 5000
            tell "❄️ 冰霜BOSS激活了冰冻领域!"
        }
        
    } else {
        # 没有BOSS存在
        if check metadata "boss_battle.active" def false is true then {
            tell "✅ BOSS战结束"
            metadata "boss_battle.active" to false
            metadata "boss_battle.target" remove
        }
    }
}

# BOSS战状态监测
def bossBattleMonitor = {
    set active to metadata "boss_battle.active" def false
    
    if check &active is true then {
        set boss to metadata "boss_battle.target"
        
        # 检查BOSS是否还存在
        if check &boss is null or boss health <= 0 then {
            # BOSS被击败
            tell "🎉 BOSS被击败! 战斗胜利!"
            
            # 战斗奖励
            set rewardExp to math random 100 500
            profile experience add &rewardExp
            tell "获得 " &rewardExp " 点经验值"
            
            # 清除BOSS战状态
            metadata "boss_battle.active" to false
            metadata "boss_battle.target" remove
        } else {
            # BOSS战进行中
            set battleTime to math world time - metadata "boss_battle.start_time"
            set minutes to math &battleTime / 1200  # 转换为分钟
            
            # 定期显示BOSS状态
            if check &battleTime % 600 is 0 then  # 每30秒显示一次
                tell "BOSS战进行中 - 时间: " math &minutes " 分钟"
                tell "BOSS剩余生命值: " math boss health "/" boss max_health
            }
        }
    }
}

# 持续监测BOSS战
while true {
    call bossBattleManager
    call bossBattleMonitor
    sleep 5s
}
```
**详细讲解**:
- 使用 `name` 选择器搜索BOSS级实体
- 实现BOSS战开始、进行中和结束的全流程管理
- 根据BOSS名称触发特殊技能效果
- 提供详细的战斗状态和奖励系统
- 完整的BOSS战管理系统

### 示例2：NPC对话系统
```kether
# NPC对话系统
def npcDialogueSystem = {
    # 搜索附近的NPC
    set npcs to select name ["商人", "村长", "导师", "守卫"]
    
    # 检查是否有可交互的NPC
    if check count &npcs > 0 then {
        # 找到最近的NPC
        set nearestNpc to sort &npcs by distance from player location limit 1
        set npc to &nearestNpc[0]
        set npcName to npc name
        set distance to math vector length vector sub npc location player location
        
        # 检查是否在对话范围内
        if check &distance <= 3 then {
            # 在对话范围内，显示对话选项
            tell "=== 与 " &npcName " 对话 ==="
            
            # 根据NPC名称提供不同的对话选项
            if check &npcName contains "商人" then {
                tell "1. 查看商品"
                tell "2. 出售物品"
                tell "3. 打听消息"
                
            } else if check &npcName contains "村长" then {
                tell "1. 接受任务"
                tell "2. 汇报进度"
                tell "3. 村庄信息"
                
            } else if check &npcName contains "导师" then {
                tell "1. 学习技能"
                tell "2. 职业咨询"
                tell "3. 训练指导"
                
            } else if check &npcName contains "守卫" then {
                tell "1. 区域信息"
                tell "2. 敌人报告"
                tell "3. 通行许可"
            }
            
            tell "输入数字选择对话选项 (输入0取消)"
            
            # 等待玩家选择
            wait for input 10s
            
            if check &input is not null then {
                # 处理玩家选择
                handleNpcDialogue &npcName &input
            }
            
        } else {
            tell "距离 " &npcName " 太远，请靠近至3格内对话"
        }
        
    } else {
        tell "附近没有可交互的NPC"
    }
}

# NPC对话处理
def handleNpcDialogue = {
    set npcName to &arg1
    set choice to &arg2
    
    # 根据NPC名称和选择处理对话
    if check &npcName contains "商人" then {
        if check &choice is "1" then {
            tell "商人: 欢迎光临! 这是我的商品清单..."
            # 打开商店界面
            
        } else if check &choice is "2" then {
            tell "商人: 你想出售什么物品?"
            # 打开出售界面
            
        } else if check &choice is "3" then {
            tell "商人: 最近听说森林里有宝箱..."
            # 提供游戏提示
        }
        
    } else if check &npcName contains "村长" then {
        if check &choice is "1" then {
            tell "村长: 村庄需要帮助，你愿意接受任务吗?"
            # 提供任务
            
        } else if check &choice is "2" then {
            tell "村长: 任务完成得怎么样了?"
            # 检查任务进度
            
        } else if check &choice is "3" then {
            tell "村长: 我们村庄有悠久的历史..."
            # 讲述背景故事
        }
    }
    
    # 显示对话效果
    particle create "villager_happy" at player location duration 1000
}

# NPC交互检测
def npcInteractionCheck = {
    # 检查玩家是否在点击NPC
    if check player is interacting then {
        set target to player interaction_target
        
        if check target is not null and target name in ["商人", "村长", "导师", "守卫"] then {
            call npcDialogueSystem
        }
    }
}

# 持续监测NPC交互
while true {
    call npcInteractionCheck
    sleep 1s
}
```
**详细讲解**:
- 使用 `name` 选择器识别特定NPC
- 实现距离检测和对话范围控制
- 根据NPC名称提供不同的对话选项
- 完整的NPC交互系统
- 支持多种NPC类型和对话内容

## 组合使用示例

### 示例：智能任务目标追踪
```kether
# 智能任务目标追踪系统
def smartQuestTargetTracking = {
    # 获取当前任务信息
    set currentQuest to metadata "current_quest" def ""
    
    if check &currentQuest is not "" then {
        # 根据任务类型定义目标名称模式
        set targetPatterns to map [
            "hunt_wolves" -> ["狼", "wolf", "野狼"]
            "collect_herbs" -> ["草药", "herb", "药草"]
            "rescue_villagers" -> ["村民", "villager", "居民"]
            "defeat_boss" -> ["boss", "首领", "王"]
        ]
        
        # 获取当前任务的目标模式
        set patterns to &targetPatterns[&currentQuest]
        
        if check &patterns is not null then {
            # 搜索任务目标
            set questTargets to select name &patterns
            
            # 显示目标追踪信息
            set targetCount to count &questTargets
            
            if check &targetCount > 0 then {
                # 找到目标，显示追踪信息
                set nearestTarget to sort &questTargets by distance from player location limit 1
                set distance to math vector length vector sub &nearestTarget[0] location player location
                
                tell "📌 任务目标追踪: " &nearestTarget[0] name
                tell "距离: " &distance " 格"
                tell inline "数量: {{ &targetCount }} 个"
                
                # 目标高亮效果
                particle create "witch" at &nearestTarget[0] location duration 2000
                
            } else {
                tell "⚠️ 未找到任务目标，请到其他区域搜索"
            }
        }
    }
}

# 任务目标自动追踪
def call autoQuestTracking = {
    # 每10秒更新一次追踪信息
    while true {
        call smartQuestTargetTracking
        sleep 10s
    }
}

# 启动自动追踪
autoQuestTracking
```
**详细讲解**:
- 使用 `name` 选择器根据任务类型搜索目标
- 实现智能的目标追踪和距离计算
- 提供视觉高亮效果和状态提示
- 完整的任务目标追踪系统

## 使用场景

- BOSS战和特殊实体管理
- NPC对话和交互系统
- 任务目标追踪和识别
- 特定名称实体的筛选和操作

## 注意事项

- 模糊匹配不区分大小写
- 严格匹配要求完全一致
- 名称模式支持数组形式的多模式匹配
- 建议结合其他选择器提高筛选精度