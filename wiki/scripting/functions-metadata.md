# 元数据函数

元数据是附加在实体上的自定义键值对数据，可用于存储技能状态、计数器等临时信息。

---

## hasMeta — 检查元数据

```javascript
hasMeta(key, entity)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | String | 元数据键名 |
| `entity` | Entity | 目标实体（通常传 `sender`） |

返回值：`true` = 存在，`false` = 不存在。

```javascript
if (hasMeta("combo.count", sender)) {
  tell("连击计数存在")
}
```

## getMeta — 获取元数据

```javascript
getMeta(key, entity)
```

返回值：元数据的值。如果不存在返回 `null`。

```javascript
var count = getMeta("combo.count", sender)
tell("当前连击: " + count)
```

## setMeta — 设置元数据

```javascript
setMeta(key, value, entity)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | String | 元数据键名 |
| `value` | Object | 要存储的值（数字、字符串等） |
| `entity` | Entity | 目标实体 |

```javascript
setMeta("combo.count", 0, sender)
setMeta("last.skill", "fireball", sender)
```

## setMetaTimeout — 设置带过期时间的元数据

```javascript
setMetaTimeout(key, value, ticks, entity)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | String | 元数据键名 |
| `value` | Object | 要存储的值 |
| `ticks` | Int | 过期时间（tick）。到期后自动删除 |
| `entity` | Entity | 目标实体 |

```javascript
// 设置一个 5 秒后过期的标记
setMetaTimeout("skill.buff", true, 100, sender)
```

## removeMeta — 移除元数据

```javascript
removeMeta(key, entity)
```

```javascript
removeMeta("combo.count", sender)
```

---

## 实战示例

### 连击计数器

```javascript
function main() {
  // 获取当前连击数，默认 0
  var combo = hasMeta("combo", sender) ? getMeta("combo", sender) : 0
  combo = combo + 1

  // 连击伤害递增
  var baseDamage = 20 + level * 10
  var totalDamage = baseDamage * (1 + combo * 0.2)

  var targets = finder().range(5).excludeType("PLAYER").sort("DISTANCE").limit(1).build()
  damage(totalDamage, targets)

  // 更新连击数（3 秒内不释放则重置）
  setMetaTimeout("combo", combo, 60, sender)

  tell("&e连击 x" + combo + " &f伤害: " + Math.floor(totalDamage))
  setCooldown(skill, 20)
}
```

### 技能形态切换

```javascript
function main() {
  var mode = hasMeta("skill.mode", sender) ? getMeta("skill.mode", sender) : 0

  if (mode == 0) {
    // 火焰模式
    tell("&c切换到火焰模式")
    var targets = finder().range(8).excludeType("PLAYER").build()
    fire(60, targets)
    damage(30 + level * 10, targets)
  } else {
    // 冰霜模式
    tell("&b切换到冰霜模式")
    var targets = finder().range(8).excludeType("PLAYER").build()
    freeze(60, targets)
    potion("SLOW", 2, 60, targets)
  }

  // 切换模式
  setMeta("skill.mode", mode == 0 ? 1 : 0, sender)
  setCooldown(skill, 80)
}
```
