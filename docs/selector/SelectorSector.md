# SelectorSector

扇形区域选择器，用于选择指定扇形区域内的实体。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `@sector`

## 语法

```kether
@sector <radius> <angle> [yaw: <direction>] [at <objective>:TargetContainer(sender)]
```

选择指定扇形区域内的实体。

**参数说明**:
- `<radius>`: **必填** - 扇形半径（格数）
- `<angle>`: **必填** - 扇形角度（度数）
- `yaw <direction>`: **选填** - 扇形方向（角度），默认为执行者朝向
- `at <objective>`: **选填** - 扇形中心位置，默认为执行者位置

**返回值**: 包含扇形区域内实体的目标容器

## 简单语句示例

### 示例1：选择前方90度扇形区域
```kether
select @sector 10 90
```
**详细讲解**:
- `@sector 10 90` 选择前方10格半径90度扇形区域
- 使用执行者当前位置和朝向
- 返回扇形区域内的所有实体

### 示例2：指定方向的扇形选择
```kether
select sector 8 45 yaw 180 at player location
```
**详细讲解**:
- `sector 8 45` 选择8格半径45度扇形
- `yaw 180` 指定朝向为南方（180度）
- `at player location` 以玩家位置为中心
- 返回指定扇形区域内的实体

## 实际功能示例

### 示例1：扇形范围攻击技能
```kether
# 扇形范围攻击技能
def sectorAttack = {
    # 定义扇形攻击参数
    set attackRadius to 12
    set attackAngle to 120
    
    # 选择扇形区域内的敌人
    set targets to select @sector &attackRadius &attackAngle @entity-type [ "zombie" "skeleton" "spider" ]
    
    # 统计目标数量
    set targetCount to count &targets
    
    if check &targetCount > 0 then {
        # 对每个目标造成伤害
        for target in &targets then {
            # 计算伤害（基于距离和角度）
            set distance to vector length vector sub target location player location
            set damage to math 25 * (1.0 - &distance / &attackRadius)
            
            # 造成伤害
            health take &damage at &target
            
            # 击退效果
            set knockback to vector scale vector norm vector sub target location player location 2
            velocity add &knockback at &target
            
            # 显示伤害效果
            particle create "crit" at target location duration 200
            tell "对 " target name " 造成 " &damage " 点伤害"
        }
        
        # 显示扇形攻击效果
        particle create "flame" along sector &attackRadius &attackAngle duration 1000
        tell inline "扇形攻击命中 {{ &targetCount }} 个目标!"
        
    } else {
        tell "扇形范围内没有发现目标"
    }
}

# 使用扇形攻击技能
if check cooldown skill "sector_attack" <= 0 then {
    cooldown set 15000 skill "sector_attack"
    profile mp take 35
    call sectorAttack
}
```
**详细讲解**:
- 使用 `sector` 选择指定角度和半径的区域
- 根据距离计算伤害衰减
- 实现击退物理效果
- 提供扇形区域的视觉显示
- 完整的扇形攻击技能系统

### 示例2：扇形警戒区域
```kether
# 扇形警戒区域监测
def sectorAlertSystem = {
    # 定义警戒扇形参数
    set alertRadius to 20
    set alertAngle to 90
    set alertDirection to player yaw  # 使用玩家朝向
    
    # 监测扇形区域内的敌对目标
    set threats to select @sector &alertRadius &alertAngle yaw &alertDirection @entity-type [ "zombie" "skeleton" "creeper" ]
    
    # 威胁等级评估
    set threatLevel to count &threats
    
    if check &threatLevel > 0 then {
        # 计算威胁方向
        set nearestThreat to sort &threats by distance from player location limit 1
        set threatDirection to vector norm vector sub &nearestThreat[0] location player location
        
        # 显示威胁警报
        tell "⚠️ 扇形警戒区域发现 " &threatLevel " 个威胁"
        tell "最近威胁方向: " math degrees vector angle threatDirection vector looking-at
        
        # 视觉警报效果
        particle create "redstone" along sector &alertRadius &alertAngle yaw &alertDirection duration 2000
        
        # 声音警报
        if check &threatLevel >= 3 then {
            play sound "entity.elder_guardian.curse" at player location
            tell "🔴 高威胁警报! 建议立即应对"
        } else if check &threatLevel >= 1 then {
            play sound "block.note_block.hat" at player location
            tell "🟡 中威胁警报! 保持警惕"
        }
        
        # 进入警戒状态
        metadata "sector_alert.active" to true
        metadata "sector_alert.timer" to 10000
        
    } else {
        # 安全状态
        if check metadata "sector_alert.active" def false is true then {
            tell "✅ 扇形警戒区域安全"
            metadata "sector_alert.active" to false
            
            # 安全指示效果
            particle create "greenstone" along sector &alertRadius &alertAngle yaw &alertDirection duration 1000
        }
    }
}

# 持续扇形警戒监测
def call continuousSectorMonitoring = {
    # 每3秒监测一次
    while true {
        call sectorAlertSystem
        sleep 3s
    }
}

# 启动扇形警戒系统
continuousSectorMonitoring
```
**详细讲解**:
- 使用 `sector` 定义特定方向的警戒区域
- 实现威胁等级评估和方向计算
- 提供视觉和听觉警报效果
- 完整的扇形区域监测系统

## 组合使用示例

### 示例：智能扇形治疗技能
```kether
# 智能扇形治疗技能
def smartSectorHealing = {
    # 定义治疗扇形参数
    set healRadius to 15
    set healAngle to 180  # 前方180度扇形
    
    # 选择扇形区域内的友方玩家
    set allies to select sector &healRadius &healAngle entity-type player their
    
    # 过滤受伤的玩家
    set injuredAllies to filter &allies where health < max_health
    
    if check count &injuredAllies > 0 then {
        # 按受伤程度排序
        set sortedAllies to sort &injuredAllies by math health / max_health
        
        # 计算总治疗量
        set totalHealAmount to 0
        
        # 对受伤玩家进行治疗
        for ally in &sortedAllies then {
            # 根据受伤程度计算治疗量
            set healthRatio to math ally health / ally max_health
            set healAmount to math (1.0 - &healthRatio) * 40
            
            # 进行治疗
            health add &healAmount at &ally
            totalHealAmount to math &totalHealAmount + &healAmount
            
            # 显示个体治疗效果
            particle create "heart" at ally location duration 600
            tell "治疗 " ally name " +" &healAmount " HP"
        }
        
        # 显示扇形治疗效果
        particle create "enchanted_hit" along sector &healRadius &healAngle duration 1500
        tell "扇形治疗完成! 总计治疗 " &totalHealAmount " 点生命值"
        
    } else {
        tell "扇形范围内没有需要治疗的友方玩家"
    }
}

# 使用扇形治疗技能
if check profile mp >= 50 then {
    profile mp take 50
    call smartSectorHealing
}
```
**详细讲解**:
- 组合使用 `sector`, `entity-type`, `their` 选择友方玩家
- 实现智能的治疗优先级和量计算
- 提供扇形区域的视觉治疗效果
- 完整的智能扇形治疗系统

## 使用场景

- 扇形范围攻击技能
- 定向警戒和监测系统
- 扇形治疗和增益效果
- 方向性互动和区域控制

## 注意事项

- 角度参数使用度数（0-360）
- 方向参数0度为北方，90度为东方
- 扇形区域基于执行者朝向或指定方向
- 建议合理设置半径和角度避免性能问题