# SelectorSort

排序选择器，用于对选择结果进行排序。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `@sort`

## 语法

```kether
@sort <sortType> [reverse: <isReverse>]
```

对选择结果按指定规则排序。

**参数说明**:
- `<sortType>`: **必填** - 排序类型，可选：`name`, `distance`, `random`
- `reverse <isReverse>`: **选填** - 是否逆序排序，默认为false

**返回值**: 排序后的目标容器

## 排序类型说明

### NAME（名称排序）
- 按实体名称的字母顺序排序
- 适用于Target.Named类型的实体

### DISTANCE（距离排序）
- 按与执行者的距离排序
- 适用于TargetLocation类型的实体

### RANDOM（随机排序）
- 随机排序（与shuffle效果类似）
- 适用于所有类型

## 简单语句示例

### 示例1：按名称排序
```kether
select @entity-type player @sort name
```
**详细讲解**:
- `@entity-type player` 选择所有玩家
- `@sort name` 按玩家名称排序
- 返回按字母顺序排列的玩家列表

### 示例2：按距离排序并限制数量
```kether
select @range 15 @entity-type monster @sort distance @amount 3
```
**详细讲解**:
- `@range 15` 选择15格范围内的怪物
- `@sort distance` 按距离排序（从近到远）
- `@amount 3` 限制最多选择3个
- 返回最近的3个怪物

## 实际功能示例

### 示例1：智能目标优先级系统
```kether
# 智能目标优先级系统
def smartTargetPriority = {
    # 选择范围内的所有敌对目标
    set allTargets to select @range 12 @entity-type [ "zombie" "skeleton" "spider" ]
    
    # 根据威胁程度排序
    set sortedTargets to sort &allTargets by [
        # 优先选择精英怪物
        if target name contains "精英" then 0 else 1,
        # 其次按距离排序
        vector length vector sub target location player location
    ]
    
    # 限制攻击目标数量
    set maxTargets to math skill level "multi_attack" + 1
    set finalTargets to select @amount &maxTargets from &sortedTargets
    
    # 执行攻击
    for target in &finalTargets then {
        # 计算伤害（基于优先级）
        set isElite to check target name contains "精英"
        set damage to if &isElite then 25 else 15
        
        # 造成伤害
        health take &damage at &target
        
        # 显示攻击效果
        particle create "damage" at target location duration 500
        tell "攻击 " target name " 造成 " &damage " 点伤害"
    }
    
    tell inline "智能攻击命中 {{ size &finalTargets }} 个目标"
}

# 使用智能攻击
if check cooldown skill "smart_attack" <= 0 then {
    cooldown set 20000 skill "smart_attack"
    profile mp take 30
    call smartTargetPriority
}
```
**详细讲解**:
- 实现多条件复合排序（精英优先+距离）
- 动态计算目标数量和伤害
- 完整的智能目标选择系统

### 示例2：排行榜系统
```kether
# 排行榜系统
def leaderboardSystem = {
    # 获取所有在线玩家
    set allPlayers to select entity-type player
    
    # 按不同指标排序
    set byLevel to sort &allPlayers by profile level reverse true
    set byMoney to sort &allPlayers by profile money reverse true
    set byKills to sort &allPlayers by metadata "player.kills" def 0 reverse true
    
    # 显示等级排行榜
    tell "=== 等级排行榜 ==="
    for player in &byLevel index limit 5 then {
        tell "" math &index + 1 ". " player name " - 等级: " profile level
    }
    
    # 显示财富排行榜
    tell "=== 财富排行榜 ==="
    for player in &byMoney index limit 5 then {
        tell "" math &index + 1 ". " player name " - 金币: " profile money
    }
    
    # 显示击杀排行榜
    tell "=== 击杀排行榜 ==="
    for player in &byKills index limit 5 then {
        tell "" math &index + 1 ". " player name " - 击杀: " metadata "player.kills" def 0
    }
}

# 每小时更新排行榜
metadata "leaderboard.timer" to 3600000  # 1小时

while check metadata "leaderboard.timer" > 0 {
    call leaderboardSystem
    sleep 300000  # 每5分钟检查一次
    metadata "leaderboard.timer" to math metadata "leaderboard.timer" - 300000
}
```
**详细讲解**:
- 实现多维度排行榜
- 支持逆序排序和数量限制
- 定期自动更新
- 完整的排行榜系统

## 组合使用示例

### 示例：拍卖行物品排序
```kether
# 拍卖行物品排序系统
def auctionSortSystem = {
    # 获取拍卖物品
    set auctionItems to metadata "auction.items" def []
    
    # 按不同条件排序
    set byPriceLow to sort &auctionItems by item price
    set byPriceHigh to sort &auctionItems by item price reverse true
    set byTimeLeft to sort &auctionItems by item time_left
    set byPopularity to sort &auctionItems by item views reverse true
    
    # 显示排序选项
    tell "选择排序方式:"
    tell "1. 价格从低到高"
    tell "2. 价格从高到低"
    tell "3. 即将结束"
    tell "4. 最受欢迎"
    
    # 等待玩家选择
    wait for input 10s
    
    # 处理选择
    if check &input is "1" then {
        tell "=== 价格从低到高 ==="
        for item in &byPriceLow index limit 10 then {
            tell "" math &index + 1 ". " item name " - 价格: " item price " 金币"
        }
    } else if check &input is "2" then {
        tell "=== 价格从高到低 ==="
        for item in &byPriceHigh index limit 10 then {
            tell "" math &index + 1 ". " item name " - 价格: " item price " 金币"
        }
    } else if check &input is "3" then {
        tell "=== 即将结束 ==="
        for item in &byTimeLeft index limit 10 then {
            tell "" math &index + 1 ". " item name " - 剩余: " math item time_left / 1000 " 秒"
        }
    } else if check &input is "4" then {
        tell "=== 最受欢迎 ==="
        for item in &byPopularity index limit 10 then {
            tell "" math &index + 1 ". " item name " - 浏览: " item views " 次"
        }
    }
}

# 访问拍卖行
def accessAuctionHouse = {
    # 检查玩家是否在拍卖行附近
    set inAuctionHouse to check player location is in rectangle 10 5 10 at location 100 64 200
    
    if check &inAuctionHouse is true then {
        tell "欢迎来到拍卖行!"
        call auctionSortSystem
    }
}

# 玩家交互检测
if check player is interacting with "auction_master" then {
    call accessAuctionHouse
}
```
**详细讲解**:
- 实现多条件排序选项
- 支持用户交互选择
- 结合位置检测
- 完整的拍卖行排序系统

## 使用场景

- 目标优先级管理
- 排行榜和统计系统
- 物品和列表排序
- 任何需要有序结果的场景

## 注意事项

- 排序可能影响性能，建议合理使用
- 逆序参数可以反转排序结果
- 可以与其他选择器任意组合
- 复合排序需要明确优先级顺序