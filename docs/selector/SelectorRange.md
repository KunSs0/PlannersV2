# SelectorRange

范围选择器，用于根据距离选择目标实体。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `@range`

## 语法

```kether
@range <radius>
```

选择指定半径范围内的所有实体。

**参数说明**:
- `<radius>`: **必填** - 选择半径（格数）

**返回值**: 包含范围内所有实体的目标容器

## 简单语句示例

### 示例1：选择5格范围内的实体
```kether
select @range 5
```
**详细讲解**:
- `@range 5` 选择5格范围内的所有实体
- `select` 命令执行选择操作
- 返回包含范围内实体的目标容器

### 示例2：选择半径范围内的目标
```kether
set targets to select @range 10
```
**详细讲解**:
- `@range 10` 选择10格范围内的所有实体
- `select` 执行选择操作
- `set targets to` 将选择结果存储在变量中

## 实际功能示例

### 示例1：范围伤害技能
```kether
# 范围伤害技能
def areaDamageSkill = {
    # 选择8格范围内的所有敌人
    set enemies to select range 8 entity-type monster
    
    # 对每个敌人造成伤害
    for enemy in &enemies then {
        # 计算伤害（基于距离衰减）
        set distance to vector length vector sub enemy location player location
        set damage to math 20 * (1.0 - &distance / 8.0)
        
        # 造成伤害
        health take &damage at &enemy
        
        # 显示伤害效果
        particle create "flame" at enemy location duration 200
        tell inline "对 {{ enemy name }} 造成 {{ &damage }} 点伤害"
    }
    
    # 显示技能效果
    particle create "explosion" at player location duration 1000
    tell "范围伤害技能释放!"
}

# 使用范围伤害技能
if check profile mp >= 25 then {
    profile mp take 25
    call areaDamageSkill
}
```
**详细讲解**:
- 使用 `range 8` 选择8格范围内的怪物
- 根据距离计算伤害衰减
- 对每个目标应用伤害效果
- 结合粒子效果和提示信息
- 完整的范围伤害技能系统

### 示例2：警戒区域检测
```kether
# 警戒区域检测
def alertZoneDetection = {
    # 定义警戒区域
    set alertRadius to 15
    
    # 检测范围内的敌对实体
    set hostiles to select @range &alertRadius @entity-type [ "skeleton" "zombie" "spider" ]
    
    # 检查是否有敌对实体
    if check count &hostiles > 0 then {
        # 发现有敌人
        set nearestEnemy to sort &hostiles by distance from player location limit 1
        set distance to vector length vector sub &nearestEnemy[0] location player location
        
        tell inline "警戒! 发现 {{ size &hostiles }} 个敌对目标"
        tell inline "最近目标: {{ &nearestEnemy[0] name }} 距离: {{ math &distance }} 格"
        
        # 触发警报效果
        particle create "redstone" at player location duration 2000
        play sound "block.note_block.bell" at player location
        
        # 进入战斗状态
        metadata "combat.state" to "alert"
        metadata "combat.timer" to 10000
    } else {
        # 区域安全
        if check metadata "combat.state" def "" is "alert" then {
            tell "区域安全，警戒解除"
            metadata "combat.state" remove
        }
    }
}

# 持续监测警戒区域
def call continuousMonitoring = {
    # 每5秒检测一次
    while true {
        call alertZoneDetection
        sleep 5s
    }
}

# 启动警戒监测
continuousMonitoring
```
**详细讲解**:
- 使用 `range 15` 定义15格警戒范围
- 检测特定类型的敌对实体
- 实现距离计算和最近目标查找
- 提供视觉和听觉警报效果
- 完整的警戒区域监测系统

## 组合使用示例

### 示例：智能治疗范围技能
```kether
# 智能治疗范围技能
def smartHealingAura = {
    # 选择范围内的友方玩家
    set allies to select @range 12 @entity-type player @their
    
    # 过滤掉满血的玩家
    set injuredAllies to filter &allies where health < max_health
    
    # 按受伤程度排序（生命值百分比最低的优先）
    set sortedAllies to sort &injuredAllies by math health / max_health limit 3
    
    # 对受伤最严重的前3个玩家进行治疗
    for ally in &sortedAllies then {
        # 计算治疗量（基于受伤程度）
        set healthRatio to math ally health / ally max_health
        set healAmount to math (1.0 - &healthRatio) * 30
        
        # 进行治疗
        health add &healAmount at &ally
        
        # 显示治疗效果
        particle create "heart" at ally location duration 500
        tell inline "对 {{ ally name }} 治疗 {{ &healAmount }} 点生命值"
    }
    
    # 显示治疗统计
    tell inline "治疗完成! 共治疗了 {{ size &sortedAllies }} 名玩家"
}

# 使用治疗技能
if check cooldown skill "healing_aura" <= 0 then {
    cooldown set 30000 skill "healing_aura"
    profile mp take 40
    call smartHealingAura
}
```
**详细讲解**:
- 组合使用 `range`, `entity-type`, `their` 选择友方玩家
- 使用 `filter` 和 `sort` 进行智能筛选
- 根据受伤程度计算治疗量
- 实现智能的治疗优先级系统

## 使用场景

- 范围技能和效果
- 区域监测和警戒系统
- 群体治疗和增益效果
- 环境互动和区域控制

## 注意事项

- 范围选择基于执行者的当前位置
- 半径单位为格数（Minecraft方块）
- 选择结果可能包含各种类型的实体
- 建议结合其他选择器进行精确筛选