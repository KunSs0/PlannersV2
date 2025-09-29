# SelectorRectangleBody

矩形体选择器，用于选择指定矩形区域内的实体。支持3D空间中的矩形体检测。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`, `Selector.Filterable`
- **命名空间**: `rectangle`, `ra`

## 语法

```kether
rectangle <width> <height> <depth> [offset "<x> <y> <z>"] [debug: <showDebug>]
```

选择指定矩形区域内的实体。

**参数说明**:
- `<width>`: **必填** - 矩形宽度（X轴方向）
- `<height>`: **必填** - 矩形高度（Y轴方向）
- `<depth>`: **必填** - 矩形深度（Z轴方向）
- `offset "<x> <y> <z>"`: **选填** - 矩形偏移向量
- `debug <showDebug>`: **选填** - 是否显示调试框，默认为false

**返回值**: 包含矩形区域内实体的目标容器

## 矩形体顶点示意图

```
       y
       |
       |____ x
      /
     z

顶点编号：

    4--------5
   /|       /|
  0--------1 |
  | |      | |
  | 7------|-6
  |/       |/
  3--------2

顶点坐标：

前上左 0: ( width/2,  height/2,  depth/2)
前上右 1: (-width/2,  height/2,  depth/2)
前下右 2: ( width/2, -height/2,  depth/2)
前下左 3: (-width/2, -height/2,  depth/2)
后上右 4: ( width/2,  height/2, -depth/2)
后上左 5: (-width/2,  height/2, -depth/2)
后下右 6: ( width/2, -height/2, -depth/2)
后下左 7: (-width/2, -height/2, -depth/2)
```

## 简单语句示例

### 示例1：选择3x3x3立方体区域
```kether
select rectangle 3 3 3
```
**详细讲解**:
- `rectangle 3 3 3` 选择3x3x3立方体区域内的实体
- 基于执行者当前位置和朝向
- 返回立方体内的所有实体

### 示例2：带偏移的矩形选择
```kether
select rectangle 5 2 4 offset "0 1 0"
```
**详细讲解**:
- `rectangle 5 2 4` 选择5x2x4矩形区域
- `offset "0 1 0"` 在Y轴方向偏移1格
- 返回偏移后矩形区域内的实体

## 实际功能示例

### 示例1：矩形区域陷阱系统
```kether
# 矩形区域陷阱系统
def rectangleTrapSystem = {
    # 定义陷阱区域参数
    set trapWidth to 5
    set trapHeight to 3
    set trapDepth to 5
    set trapOffset to vector create 0 0 2  # 向前偏移2格
    
    # 选择陷阱区域内的目标
    set trappedTargets to select rectangle &trapWidth &trapHeight &trapDepth offset &trapOffset
    
    # 过滤出敌对目标
    set hostileTargets to filter &trappedTargets where entity_type in [ "zombie" "skeleton" "spider" ]
    
    if check count &hostileTargets > 0 then {
        # 激活陷阱效果
        tell inline "🚨 陷阱激活! 捕获 {{ size &hostileTargets }} 个敌对目标"
        
        # 对每个目标应用陷阱效果
        for target in &hostileTargets then {
            # 造成持续伤害
            health take 5 at &target
            
            # 减速效果
            velocity multiply vector create 0.5 1 0.5 at &target
            
            # 显示陷阱效果
            particle create "witch" at target location duration 500
            tell "陷阱对 " target name " 造成伤害并减速"
        }
        
        # 显示陷阱区域边界
        particle create "redstone" along rectangle &trapWidth &trapHeight &trapDepth offset &trapOffset duration 2000
        
        # 陷阱冷却
        metadata "trap.active" to true
        metadata "trap.cooldown" to 10000
        
    } else {
        # 陷阱未触发
        if check metadata "trap.active" def false is true then {
            tell "✅ 陷阱区域安全"
            metadata "trap.active" to false
        }
    }
}

# 陷阱冷却管理
def trapCooldownManager = {
    set cooldown to metadata "trap.cooldown" def 0
    
    if check &cooldown > 0 then {
        set newCooldown to math &cooldown - 50
        metadata "trap.cooldown" to &newCooldown
        
        if check &newCooldown <= 0 then {
            tell "🔄 陷阱冷却完成，可以重新激活"
        }
    }
}

# 持续陷阱监测
def call continuousTrapMonitoring = {
    while true {
        call rectangleTrapSystem
        call trapCooldownManager
        sleep 1s
    }
}

# 启动陷阱系统
continuousTrapMonitoring
```
**详细讲解**:
- 使用 `rectangle` 定义精确的陷阱区域
- 实现敌对目标检测和过滤
- 提供陷阱效果（伤害、减速等）
- 显示视觉边界和冷却管理
- 完整的矩形区域陷阱系统

### 示例2：建筑区域保护系统
```kether
# 建筑区域保护系统
def buildingProtectionSystem = {
    # 定义保护区域参数
    set protectedWidth to 10
    set protectedHeight to 6
    set protectedDepth to 8
    set protectionCenter to vector create 100 64 200  # 建筑中心坐标
    
    # 选择保护区域内的实体
    set areaEntities to select rectangle &protectedWidth &protectedHeight &protectedDepth at location &protectionCenter
    
    # 检查是否有违规行为
    set violators to filter &areaEntities where 
        entity_type in [ "zombie" "skeleton" "creeper" ] or
        (entity_type is "player" and not has permission "build.protect.bypass")
    
    if check count &violators > 0 then {
        # 处理违规行为
        for violator in &violators then {
            if check violator entity_type in [ "zombie" "skeleton" "creeper" ] then {
                # 对怪物进行驱逐
                set ejectDirection to vector norm vector sub violator location &protectionCenter
                set ejectForce to vector scale &ejectDirection 3
                velocity add &ejectForce at &violator
                
                tell "🚫 建筑保护: 驱逐 " violator name
                
            } else if check violator entity_type is "player" then {
                # 对玩家进行警告
                tell violator name " 🚫 此区域受保护，请勿在此建造或破坏"
                
                # 轻微击退效果
                set warningForce to vector scale vector norm vector sub violator location &protectionCenter 0.5
                velocity add &warningForce at &violator
                
                # 记录违规次数
                set violationCount to metadata "violation." violator name def 0
                set &violationCount to math &violationCount + 1
                metadata "violation." violator name to &violationCount
                
                if check &violationCount >= 3 then {
                    tell "⚠️ 警告: " violator name " 多次违规，将被暂时禁止进入"
                    # 实施临时禁令
                    metadata "ban." violator name to world time + 300000  # 5分钟禁令
                }
            }
        }
        
        # 显示保护区域边界
        particle create "barrier" along rectangle &protectedWidth &protectedHeight &protectedDepth at location &protectionCenter duration 3000
        
    } else {
        # 区域安全
        if check metadata "protection.alert" def "" is "active" then {
            tell "✅ 建筑保护区域安全"
            metadata "protection.alert" to "inactive"
        }
    }
}

# 禁令检查
def banCheckSystem = {
    # 检查所有玩家是否在禁令期内
    set allPlayers to select entity-type player
    
    for player in &allPlayers then {
        set banEndTime to metadata "ban." player name def 0
        
        if check &banEndTime > 0 and world time < &banEndTime then {
            # 玩家在禁令期内
            set remainingTime to math (&banEndTime - world time) / 1000  # 转换为秒
            
            # 检查玩家是否在保护区域内
            set inProtectedArea to check player location is in rectangle &protectedWidth &protectedHeight &protectedDepth at location &protectionCenter
            
            if check &inProtectedArea is true then {
                # 强制传送出保护区域
                set safeLocation to vector create 120 64 180  # 安全区域坐标
                teleport player to location &safeLocation
                
                tell player name " ⚠️ 你在禁令期内，禁止进入保护区域"
                tell "剩余禁令时间: " math &remainingTime " 秒"
            }
        } else if check &banEndTime > 0 and world time >= &banEndTime then {
            # 禁令到期
            metadata "ban." player name remove
            tell player name " ✅ 禁令已解除，可以正常进入保护区域"
        }
    }
}

# 持续保护监测
def call continuousProtectionMonitoring = {
    while true {
        call buildingProtectionSystem
        call banCheckSystem
        sleep 2s
    }
}

# 启动保护系统
continuousProtectionMonitoring
```
**详细讲解**:
- 使用 `rectangle` 定义精确的建筑保护区域
- 实现违规行为检测和分级处理
- 提供禁令系统和时间管理
- 完整的建筑区域保护机制

## 组合使用示例

### 示例：精确技能命中区域
```kether
# 精确技能命中区域计算
def preciseSkillHitArea = {
    # 根据技能类型定义不同的矩形区域
    set skillType to metadata "current_skill" def "default"
    
    set areaParams to map [
        "fire_wall" -> [8, 4, 1]      # 火墙：长8高4厚1
        "ice_spike" -> [2, 6, 2]      # 冰刺：长2高6厚2
        "earth_pillar" -> [3, 8, 3]   # 地柱：长3高8厚3
        "lightning_chain" -> [1, 1, 10] # 闪电链：长1高1深10
    ]
    
    # 获取当前技能的区域参数
    set params to &areaParams[&skillType] def [5, 3, 5]
    set width to &params[0]
    set height to &params[1]
    set depth to &params[2]
    
    # 根据玩家朝向计算区域偏移
    set playerDirection to vector looking-at of player
    set areaOffset to vector scale &playerDirection 3  # 向前偏移3格
    
    # 选择技能命中区域内的目标
    set hitTargets to select rectangle &width &height &depth offset &areaOffset
    
    # 根据技能类型应用不同效果
    if check &skillType is "fire_wall" then {
        # 火墙技能：持续伤害
        for target in &hitTargets then {
            health take 3 at &target
            particle create "flame" at target location duration 1000
        }
        
        # 显示火墙区域
        particle create "lava" along rectangle &width &height &depth offset &areaOffset duration 2000
        
    } else if check &skillType is "ice_spike" then {
        # 冰刺技能：冻结效果
        for target in &hitTargets then {
            velocity set vector create 0 0 0 at &target  # 完全停止
            particle create "snowflake" at target location duration 1500
        }
        
        # 显示冰刺区域
        particle create "ice" along rectangle &width &height &depth offset &areaOffset duration 2000
        
    } else if check &skillType is "earth_pillar" then {
        # 地柱技能：击飞效果
        for target in &hitTargets then {
            set knockupForce to vector create 0 2 0
            velocity add &knockupForce at &target
            particle create "block break stone" at target location duration 1000
        }
        
        # 显示地柱区域
        particle create "block crack stone" along rectangle &width &height &depth offset &areaOffset duration 2000
    }
    
    tell inline "技能 {{ &skillType }} 命中 {{ size &hitTargets }} 个目标"
}

# 使用精确技能
if check profile mp >= 30 then {
    profile mp take 30
    call preciseSkillHitArea
}
```
**详细讲解**:
- 使用 `rectangle` 定义不同技能的精确命中区域
- 根据技能类型应用不同的效果
- 实现基于玩家朝向的区域偏移
- 提供视觉区域显示和命中统计
- 完整的精确技能系统

## 使用场景

- 精确的区域效果和技能命中
- 建筑保护和区域管理
- 陷阱和防御系统
- 3D空间中的精确实体选择

## 注意事项

- 矩形区域基于执行者朝向
- 支持3D空间的精确检测
- 调试模式可以显示区域边界
- 建议合理设置区域大小避免性能问题