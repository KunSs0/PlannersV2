# Selector模块概述

选择器(Selector)模块用于在Kether脚本中筛选和定位目标实体。选择器可以组合使用，构建复杂的目标选择逻辑。

## 选择器分类

### 基础选择器
- **sender/self** - 选择脚本执行者
- **their** - 选择除执行者外的其他目标
- **console** - 选择控制台

### 范围选择器
- **range** - 根据距离选择目标
- **sector** - 扇形区域选择
- **rectangle** - 矩形区域选择

### 属性选择器
- **entity-type** - 根据实体类型选择
- **name** - 根据名称选择
- **in-world** - 根据世界选择

### 操作选择器
- **amount** - 限制选择数量
- **shuffle** - 随机打乱选择顺序
- **sort** - 排序选择结果

## 选择器组合语法

选择器可以链式组合使用：

```kether
# 选择10格范围内除自己外的所有怪物
select range 10 their entity-type monster

# 选择前方扇形区域内的敌人
select sector 5 90 entity-type enemy
```

## 使用示例

### 基础用法
```kether
# 选择自己
select sender

# 选择10格范围内的所有实体
select range 10

# 选择所有僵尸
select entity-type zombie
```

### 组合用法
```kether
# 选择10格范围内除自己外的所有怪物
select range 10 their entity-type monster amount 3

# 选择前方扇形区域并随机选择2个目标
select sector 8 60 shuffle amount 2
```