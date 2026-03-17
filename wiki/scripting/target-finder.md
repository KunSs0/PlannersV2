# 目标查找器

`finder()` 是一个链式 API，用于在脚本中查找和筛选目标实体。

---

## 基本用法

```javascript
// 查找周围 10 格内的僵尸，最多 3 个
var targets = finder().range(10).type("ZOMBIE").limit(3).build()

// 对目标造成伤害
damage(50, targets)
```

**调用流程**：
1. `finder()` — 创建查找器，以 sender 位置为原点
2. 链式调用过滤方法（`range`、`type`、`limit` 等）
3. `.build()` — 构建结果，返回目标容器

---

## 创建查找器

```javascript
// 以 sender 位置为原点
var f = finder()

// 以指定位置为原点
var f = finder(location)
```

如果不传参数，默认以技能释放者（sender）的位置为原点。

---

## 选择方法

### range(radius) — 范围选择

```javascript
// 选取原点周围 10 格内的所有活体实体
finder().range(10).build()

// 选取 5 格内的实体
finder().range(5).build()
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `radius` | Double | 搜索半径（格） |

- 默认排除 sender 自己
- 只选取 LivingEntity（活体实体）
- 使用球形范围（不是立方体）

### origin(location) — 改变原点

```javascript
// 先在 A 点搜索，再切换到 B 点搜索
finder().range(10).origin(locationB).range(5).build()
```

改变后续 `range()` 的搜索原点。可以实现多区域选择。

### includeSelf() — 包含自己

```javascript
// 默认不包含 sender，调用后会包含
finder().range(10).includeSelf().build()
```

---

## 过滤方法

### type(type) — 按实体类型过滤

```javascript
// 只保留僵尸
finder().range(10).type("ZOMBIE").build()

// 多个类型（OR 逻辑，用逗号分隔）
finder().range(10).type("ZOMBIE,SKELETON,SPIDER").build()
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | String | 实体类型名称，多个用逗号分隔。不区分大小写 |

常用实体类型：`ZOMBIE`、`SKELETON`、`SPIDER`、`CREEPER`、`PLAYER`、`COW`、`PIG`、`VILLAGER` 等。

### excludeType(type) — 排除实体类型

```javascript
// 排除玩家
finder().range(10).excludeType("PLAYER").build()

// 排除多个类型
finder().range(10).excludeType("PLAYER,VILLAGER").build()
```

### name(pattern) — 按名称过滤

```javascript
// 名称包含 "Boss" 的实体
finder().range(20).name("Boss").build()

// 支持正则表达式，多个用逗号分隔
finder().range(20).name("Boss.*,Elite.*").build()
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `pattern` | String | 名称匹配模式（正则表达式），多个用逗号分隔，不区分大小写 |

### inWorld(world) — 按世界过滤

```javascript
// 只保留主世界的实体
finder().range(50).inWorld("world").build()

// 多个世界
finder().range(50).inWorld("world,world_nether").build()
```

---

## 数量与排序

### limit(n) — 限制数量

```javascript
// 最多选取 5 个
finder().range(10).limit(5).build()
```

### sort(type) — 排序

```javascript
// 按距离排序（近到远）
finder().range(10).sort("DISTANCE").build()

// 按名称排序
finder().range(10).sort("NAME").build()

// 随机排序
finder().range(10).sort("RANDOM").build()
```

| 排序类型 | 说明 |
|---------|------|
| `DISTANCE` | 按与原点的距离排序（近到远） |
| `NAME` | 按实体名称字母排序 |
| `RANDOM` | 随机排序 |

### sortReverse() — 反转排序

```javascript
// 按距离排序，最远的在前
finder().range(10).sort("DISTANCE").sortReverse().build()
```

### shuffle() — 随机打乱

```javascript
// 随机打乱顺序
finder().range(10).shuffle().build()
```

---

## 组合示例

### 选取最近的 1 个怪物

```javascript
var nearest = finder()
  .range(15)
  .excludeType("PLAYER")
  .sort("DISTANCE")
  .limit(1)
  .build()
```

### 选取随机 3 个僵尸

```javascript
var targets = finder()
  .range(10)
  .type("ZOMBIE")
  .shuffle()
  .limit(3)
  .build()
```

### 多区域选择

```javascript
// 先在自己周围 10 格搜索，再在另一个位置 5 格搜索
// 两次搜索的结果会合并
var targets = finder()
  .range(10)
  .origin(someLocation)
  .range(5)
  .excludeType("PLAYER")
  .build()
```

### 完整技能示例

```javascript
function main() {
  // 查找周围 8 格内的非玩家实体，按距离排序，最多 5 个
  var targets = finder()
    .range(8)
    .excludeType("PLAYER")
    .sort("DISTANCE")
    .limit(5)
    .build()

  // 造成伤害
  damage(50 + level * 20, targets)

  // 给目标施加缓慢
  potion("SLOW", 1, 60, targets)

  // 把目标弹飞
  velocityAdd(0, 0.5, 0, targets)

  // 播放声音
  sound("ENTITY_PLAYER_ATTACK_SWEEP", 1.0, 1.0)

  // 设置冷却
  setCooldown(skill, 100)

  tell("命中目标！")
}
```
