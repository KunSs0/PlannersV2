# TargetFinder

链式目标查找器，采用立即执行模式。每个方法调用立即生效，支持多区域选择和逗号分隔多值匹配。

## 基础用法

```fluxon
// 10格内的3个僵尸
targets = finder()::range(10)::type("zombie")::limit(3)::build()

// 多类型选择 (OR 逻辑)
undead = finder()::range(15)::type("zombie,skeleton")::build()

// 多区域选择：A点10格 + B点5格
multi = finder()::range(10)::origin(&locB)::range(5)::build()

// 按距离排序取最近3个
nearest = finder()::range(20)::sort("distance")::limit(3)::build()
```

## 方法分类

| 类型 | 方法 | 行为 |
|------|------|------|
| 选择器 | `range(r)` | 立即选择，累加到结果集 |
| 状态 | `origin(loc)` | 修改后续 range 的原点 |
| 状态 | `includeSelf()` | 允许选择 sender |
| 过滤器 | `type(t)` | 立即过滤，支持逗号分隔 |
| 过滤器 | `excludeType(t)` | 立即排除，支持逗号分隔 |
| 过滤器 | `name(pattern)` | 立即过滤，支持逗号分隔正则 |
| 过滤器 | `inWorld(w)` | 立即过滤，支持逗号分隔 |
| 限制器 | `limit(n)` | 立即限制数量 |
| 限制器 | `sort(t)` | 立即排序 (name/distance/random) |
| 限制器 | `sortReverse()` | 立即反转顺序 |
| 限制器 | `shuffle()` | 立即随机打乱 |
| 终结 | `build()` | 返回 ProxyTargetContainer |

## 方法详解

### finder([origin])

创建 TargetFinder 实例。

- `origin`: 可选，起始位置。默认使用 sender 的位置

```fluxon
finder()              // 使用 sender 位置
finder(&someLocation) // 指定起始位置
```

### range(r: Double)

从当前 origin 选择半径 r 内的实体，累加到结果集。

```fluxon
// 单区域
finder()::range(10)::build()

// 多区域累加
finder()::range(10)::origin(&locB)::range(5)::build()
```

### origin(location: Location)

修改后续 `range()` 调用的原点位置。

```fluxon
finder()::range(10)::origin(&bossLoc)::range(15)::build()
// 先从 sender 位置选 10 格，再从 boss 位置选 15 格，累加
```

### includeSelf()

允许结果集包含 sender 自身（默认排除）。

```fluxon
finder()::includeSelf()::range(10)::build()
```

### type(type: String)

过滤结果集，只保留指定类型的实体。支持逗号分隔多类型（OR 逻辑）。

```fluxon
// 单类型
finder()::range(10)::type("zombie")::build()

// 多类型：僵尸或骷髅
finder()::range(10)::type("zombie,skeleton")::build()
```

### excludeType(type: String)

从结果集排除指定类型。支持逗号分隔。

```fluxon
// 排除玩家
finder()::range(10)::excludeType("player")::build()

// 排除多种类型
finder()::range(10)::excludeType("player,villager")::build()
```

### name(pattern: String)

按名称正则过滤。支持逗号分隔多模式（OR 逻辑）。

```fluxon
// 名称包含 "Boss"
finder()::range(20)::name("Boss")::build()

// 多模式匹配
finder()::range(20)::name("Boss,Elite")::build()
```

### inWorld(world: String)

按世界名过滤。支持逗号分隔。

```fluxon
finder()::range(50)::inWorld("world,world_nether")::build()
```

### limit(n: Int)

限制结果集数量为前 n 个。

```fluxon
finder()::range(10)::limit(5)::build()
```

### sort(type: String)

对结果集排序。

- `name`: 按名称字母顺序
- `distance`: 按距离当前 origin 远近
- `random`: 随机排序

```fluxon
// 最近的 3 个
finder()::range(20)::sort("distance")::limit(3)::build()

// 最远的 3 个
finder()::range(20)::sort("distance")::sortReverse()::limit(3)::build()
```

### sortReverse()

反转当前结果集顺序。

### shuffle()

随机打乱结果集顺序。

### build()

构建并返回 `ProxyTargetContainer`。

## 逗号分隔语法

逗号分隔的值采用 OR 逻辑匹配：

```fluxon
// 僵尸或骷髅（满足其一即可）
::type("zombie,skeleton")

// 链式多次调用是 AND 逻辑（越来越严格）
::type("zombie")::type("skeleton")  // 结果为空，实体不能同时是两种类型
```

## 执行流程

```fluxon
finder()                    // 创建空结果集，origin=sender.location
  ::range(10)               // 立即选择 10 格内实体 → 结果集
  ::type("zombie")          // 立即过滤，只保留僵尸
  ::origin(&newLoc)         // 切换原点
  ::range(5)                // 从 newLoc 选择 5 格内实体，累加到结果集
  ::limit(3)                // 立即限制为前 3 个
  ::build()                 // 返回 ProxyTargetContainer
```

## 实用示例

### 范围伤害技能

```fluxon
// 对 8 格内最多 5 个敌对生物造成伤害
targets = finder()
  ::range(8)
  ::excludeType("player,villager,iron_golem")
  ::limit(5)
  ::build()

for target in &targets {
  damage(&target, 10)
}
```

### Boss 召唤小怪检测

```fluxon
// 检测 boss 周围是否有足够小怪
minions = finder(&bossLocation)
  ::range(15)
  ::type("zombie,skeleton,spider")
  ::build()

if &minions::size() < 3 {
  // 召唤更多小怪
}
```

### 最近目标锁定

```fluxon
// 锁定最近的敌人
nearest = finder()
  ::range(30)
  ::excludeType("player")
  ::sort("distance")
  ::limit(1)
  ::build()
```
