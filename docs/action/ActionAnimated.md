# ActionAnimated

动画实体相关操作的动作类。

## 类信息

- **包名**: `com.gitee.planners.module.kether`
- **类型**: `object` (单例对象)

## 属性操作

### `animated.operator` 属性

```kether
&<animated对象>[<属性名>]
```

该属性允许读取和设置动画实体(Animated)的元数据。

**参数说明**:
- `<animated对象>`: **必填** - Animated类型的对象实例
- `<属性名>`: **必填** - 要读取或设置的元数据键名

**返回值**: 元数据的值，类型取决于存储的数据类型

## 简单语句示例

### 示例1：读取动画实体位置
```kether
set position to &myAnimated[location]
```
**详细讲解**:
- `&myAnimated` 引用名为 `myAnimated` 的动画实体变量
- `[location]` 访问该实体的位置元数据
- `set position to` 将读取到的位置值存储在 `position` 变量中

### 示例2：设置动画实体状态
```kether
set &myAnimated[state] to "active"
```
**详细讲解**:
- `&myAnimated[state]` 访问动画实体的状态属性
- `to "active"` 将状态设置为字符串 "active"
- 如果实体实现了Updated接口，此操作会触发更新处理

## 实际功能示例

### 示例1：控制动画实体移动
```kether
# 创建动画实体
set animated to entity create "custom_entity" at player location

# 设置移动速度
set &animated[speed] to 2.0

# 设置移动方向
set direction to vector looking-at of player
set &animated[direction] to &direction

# 检查是否到达目标位置
if check &animated[at_target] is true then {
    set &animated[state] to "idle"
}
```
**详细讲解**:
- 首先创建自定义动画实体并设置其属性
- 通过 `&animated[speed]` 设置移动速度
- 使用向量计算实体的移动方向
- 通过 `&animated[at_target]` 检查是否到达目标位置
- 根据状态控制实体的行为

### 示例2：动画实体攻击系统
```kether
# 设置动画实体的攻击属性
set &warriorAnimated[attack_power] to 50
set &warriorAnimated[attack_range] to 3.0

# 检查攻击目标
set target to &warriorAnimated[target]
if check &target is not null then {
    # 计算伤害
    set damage to math &warriorAnimated[attack_power] * 1.5
    
    # 执行攻击
    damage &target take &damage
    
    # 播放攻击动画
    set &warriorAnimated[animation] to "attack"
}
```
**详细讲解**:
- 设置动画实体的攻击相关属性
- 检查实体是否有攻击目标
- 基于攻击力计算实际伤害值
- 对目标执行伤害操作
- 播放相应的攻击动画效果

## 使用场景

- 在技能脚本中管理动画实体的状态
- 控制自定义实体的属性和行为
- 实现动态的实体动画效果

## 注意事项

- 只能对Animated类型的对象使用此属性
- 元数据的读写操作会自动处理类型转换
- 如果动画实体实现了Updated接口，设置元数据时会触发更新处理