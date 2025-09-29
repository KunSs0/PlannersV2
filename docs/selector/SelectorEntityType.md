# SelectorEntityType

实体类型选择器，用于根据实体类型筛选目标。

## 类信息

- **包名**: `com.gitee.planners.module.kether.selector`
- **类型**: `object` (单例对象)
- **继承**: `AbstractSelector`
- **命名空间**: `@type`, `@entity-type`

## 语法

```kether
@type <entityTypes...>
@entity-type <entityTypes...>
```

选择指定类型的实体。

**参数说明**:
- `<entityTypes...>`: **必填** - 一个或多个实体类型名称

**返回值**: 包含指定类型实体的目标容器

## 支持的实体类型

### 生物类型
- `player` - 玩家
- `zombie` - 僵尸
- `skeleton` - 骷髅
- `spider` - 蜘蛛
- `creeper` - 苦力怕
- `enderman` - 末影人

### 动物类型
- `cow` - 牛
- `pig` - 猪
- `sheep` - 羊
- `chicken` - 鸡

### 其他类型
- `item` - 掉落物
- `arrow` - 箭
- `experience_orb` - 经验球

## 简单语句示例

### 示例1：选择所有玩家
```kether
select @type player
```
**详细讲解**:
- `@type player` 选择所有玩家类型的实体
- `select` 命令执行选择操作
- 返回包含所有玩家的目标容器

### 示例2：选择多种怪物类型
```kether
select @entity-type [ "zombie" "skeleton" "spider" ]
```
**详细讲解**:
- `@entity-type` 使用完整名称
- `[ "zombie" "skeleton" "spider" ]` 选择三种怪物类型
- 返回包含指定类型怪物的目标容器

## 实际功能示例

### 示例1：怪物清理系统
```kether
# 怪物清理系统
def monsterCleanup = {
    # 选择20格范围内的所有敌对怪物
    set monsters to select @range 20 @type [ "zombie" "skeleton" "spider" "creeper" ]
    
    # 统计怪物数量
    set monsterCount to count &monsters
    
    if check &monsterCount > 0 then {
        # 清理怪物
        for monster in &monsters then {
            # 造成致命伤害
            health take 1000 at &monster
            
            # 显示清理效果
            particle create "large_smoke" at monster location duration 300
        }
        
        tell inline "清理了 {{ &monsterCount }} 个怪物"
        
        # 奖励经验
        set expReward to math &monsterCount * 10
        profile experience add &expReward
        tell inline "获得 {{ &expReward }} 点经验值"
    } else {
        tell "附近没有需要清理的怪物"
    }
}

# 管理员清理命令
if check player has permission "planners.cleanup" then {
    call monsterCleanup
}
```
**详细讲解**:
- 使用 `type` 选择特定怪物类型
- 结合 `range` 限制清理范围
- 实现怪物统计和清理效果
- 提供经验奖励机制
- 完整的怪物清理系统

### 示例2：动物繁殖管理
```kether
# 动物繁殖管理系统
def animalBreedingManager = {
    # 定义可繁殖的动物类型
    set breedableAnimals to [ "cow" "pig" "sheep" "chicken" ]
    
    # 选择15格范围内的动物
    set animals to select range 15 type &breedableAnimals
    
    # 按类型分组统计
    set animalStats to map []
    
    for animal in &animals then {
        set animalType to animal entity_type
        set currentCount to &animalStats[&animalType] def 0
        set &animalStats[&animalType] to math &currentCount + 1
    }
    
    # 显示动物统计
    tell "=== 动物种群统计 ==="
    for type in keys &animalStats then {
        set count to &animalStats[&type]
        tell inline "{{ &type }}: {{ size &animalsOfType }} 只"
    }
    
    # 自动繁殖逻辑
    for type in keys &animalStats then {
        set count to &animalStats[&type]
        
        if check &count < 5 then {
            # 种群数量不足，进行繁殖
            set animalsOfType to filter &animals where entity_type is &type
            
            if check count &animalsOfType >= 2 then {
                # 找到一对动物进行繁殖
                set parent1 to &animalsOfType[0]
                set parent2 to &animalsOfType[1]
                
                # 执行繁殖
                entity breed &parent1 with &parent2
                tell "" &type " 繁殖成功!"
                
                # 显示繁殖效果
                particle create "heart" at &parent1 location duration 1000
                particle create "heart" at &parent2 location duration 1000
            }
        }
    }
}

# 定期执行动物管理
metadata "animal_manager.timer" to 60000  # 1分钟

while check metadata "animal_manager.timer" > 0 {
    call animalBreedingManager
    sleep 30s
    metadata "animal_manager.timer" to math metadata "animal_manager.timer" - 30000
}
```
**详细讲解**:
- 使用 `type` 选择特定动物类型
- 实现动物种群统计和分组
- 自动繁殖逻辑基于种群数量
- 提供视觉效果和状态提示
- 完整的动物管理系统

## 组合使用示例

### 示例：智能敌我识别系统
```kether
# 智能敌我识别系统
def smartTargetIdentification = {
    # 选择范围内的所有实体
    set allEntities to select range 12
    
    # 分类识别
    set players to filter &allEntities where entity_type is "player"
    set monsters to filter &allEntities where entity_type in [ "zombie" "skeleton" "spider" ]
    set animals to filter &allEntities where entity_type in [ "cow" "pig" "sheep" ]
    
    # 敌我识别逻辑
    set friendlyCount to count &players + count &animals
    set hostileCount to count &monsters
    
    # 显示识别结果
    tell "=== 区域扫描结果 ==="
    tell inline "友方单位: {{ &friendlyCount }} 个"
    tell inline "敌方单位: {{ &hostileCount }} 个"
    
    # 威胁评估
    if check &hostileCount > &friendlyCount then {
        tell "⚠️ 威胁等级: 高 - 建议撤退或请求支援"
        particle create "redstone" at player location duration 2000
    } else if check &hostileCount > 0 then {
        tell "⚠️ 威胁等级: 中 - 可以应对"
        particle create "yellowstone" at player location duration 2000
    } else {
        tell "✅ 威胁等级: 低 - 区域安全"
        particle create "greenstone" at player location duration 2000
    }
    
    # 标记敌对目标
    if check &hostileCount > 0 then {
        set nearestHostile to sort &monsters by distance from player location limit 1
        tell "最近敌对目标: " &nearestHostile[0] name " (距离: " math vector length vector sub &nearestHostile[0] location player location " 格)"
        
        # 高亮显示敌对目标
        particle create "flame" at &nearestHostile[0] location duration 3000
    }
}

# 战斗前扫描
if check metadata "combat.state" def "" is "preparing" then {
    call smartTargetIdentification
}
```
**详细讲解**:
- 使用 `type` 进行实体分类
- 实现敌我识别和威胁评估
- 提供视觉标记和状态提示
- 完整的智能目标识别系统

## 使用场景

- 敌我识别和分类
- 特定类型实体管理
- 生物种群控制
- 游戏机制中的实体筛选

## 注意事项

- 实体类型名称使用Minecraft标准名称
- 类型名称区分大小写
- 支持单个类型或多个类型的数组
- 建议结合其他选择器进行精确筛选