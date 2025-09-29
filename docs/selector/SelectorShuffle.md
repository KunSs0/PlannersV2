# SelectorShuffle

随机打乱选择器，用于随机化选择结果的顺序。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `shuffle`

## 语法

```kether
shuffle
```

随机打乱当前选择结果的顺序。

**参数说明**: 无参数

**返回值**: 顺序被打乱后的目标容器

## 简单语句示例

### 示例1：打乱选择结果
```kether
select entity-type monster shuffle
```
**详细讲解**:
- `entity-type monster` 选择所有怪物
- `shuffle` 随机打乱选择顺序
- 返回随机排序的怪物列表

### 示例2：结合数量限制
```kether
select range 10 shuffle amount 3
```
**详细讲解**:
- `range 10` 选择10格范围内的实体
- `shuffle` 打乱顺序
- `amount 3` 限制最多选择3个
- 返回随机选择的3个实体

## 实际功能示例

### 示例1：随机抽奖系统
```kether
# 随机抽奖系统
def call randomLotterySystem = {
    # 选择参与抽奖的玩家
    set participants to select range 15 entity-type player
    
    # 打乱参与者顺序
    shuffle &participants
    
    # 随机选择获奖者
    set winners to select @amount 3 from &participants
    
    # 显示抽奖结果
    tell "=== 抽奖结果 ==="
    tell inline "参与人数: {{ count &participants }}"
    tell "获奖者:"
    
    for winner in &winners then {
        tell "- " winner name
        
        # 给获奖者奖励
        profile money add 100 at &winner
        tell winner name " 获得100金币奖励!"
        
        # 显示获奖效果
        particle create "firework" at winner location duration 1000
    }
}

# 执行抽奖
randomLotterySystem
```
**详细讲解**:
- 使用 `shuffle` 实现公平随机抽奖
- 组合 `amount` 选择多个获奖者
- 提供奖励和视觉效果
- 完整的抽奖系统

### 示例2：随机怪物生成
```kether
# 随机怪物生成系统
def randomMonsterSpawn = {
    # 可生成的怪物类型
    set monsterTypes to ["zombie", "skeleton", "spider", "creeper", "enderman"]
    
    # 随机选择3种怪物
    shuffle &monsterTypes
    set selectedMonsters to select @amount 3 from &monsterTypes
    
    # 在随机位置生成怪物
    for monsterType in &selectedMonsters then {
        # 计算随机位置（玩家周围5-10格）
        set angle to math random 0 360
        set distance to math random 5 10
        set offsetX to math &distance * cos &angle
        set offsetZ to math &distance * sin &angle
        
        # 生成怪物
        set spawnLocation to vector add player location vector create &offsetX 0 &offsetZ
        entity spawn &monsterType at location &spawnLocation
        
        # 显示生成效果
        particle create "smoke" at &spawnLocation duration 500
        tell inline "生成了一只 {{ &monsterType }} 在 {{ math &distance }} 格外"
    }
}

# 使用随机生成
if check profile mp >= 30 then {
    profile mp take 30
    call randomMonsterSpawn
}
```
**详细讲解**:
- 使用 `shuffle` 随机选择怪物类型
- 计算随机生成位置
- 实现多样化的怪物生成
- 完整的随机生成系统

## 组合使用示例

### 示例：随机任务分配
```kether
# 随机任务分配系统
def randomQuestAssignment = {
    # 可分配的任务列表
    set availableQuests to [
        "收集10个木材",
        "击败5个骷髅",
        "找到隐藏的宝箱",
        "护送村民",
        "采集20朵花"
    ]
    
    # 随机打乱任务顺序
    shuffle &availableQuests
    
    # 分配3个随机任务
    set assignedQuests to select @amount 3 from &availableQuests
    
    # 显示分配结果
    tell "=== 今日任务 ==="
    for quest in &assignedQuests index then {
        tell "" math &index + 1 ". " &quest
    }
    
    # 存储分配的任务
    metadata "assigned_quests" to &assignedQuests
}

# 每日任务刷新
def call dailyQuestRefresh = {
    # 检查是否是新的游戏日
    set currentDay to world full_time / 24000
    set lastRefreshDay to metadata "quest_refresh_day" def -1
    
    if check &currentDay > &lastRefreshDay then {
        # 新的一天，刷新任务
        call randomQuestAssignment
        metadata "quest_refresh_day" to &currentDay
        tell "🔔 每日任务已刷新!"
    }
}

# 检查任务刷新
dailyQuestRefresh
```
**详细讲解**:
- 使用 `shuffle` 实现随机任务分配
- 结合游戏时间实现每日刷新
- 提供任务列表和状态存储
- 完整的随机任务系统

## 使用场景

- 随机选择和抽奖系统
- 随机生成和刷新机制
- 公平的任务分配
- 任何需要随机化的场景

## 注意事项

- 会改变原始选择结果的顺序
- 建议在限制数量前使用
- 随机算法基于系统随机数生成器
- 可以与其他选择器任意组合