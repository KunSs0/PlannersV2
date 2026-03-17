# 投射物函数

用于发射各种投射物（箭、火球、雪球等）。

---

## projectile — 朝面向方向发射

```javascript
projectile(type)                            // sender 朝面向方向发射
projectile(type, speed)                     // 指定速度
projectile(type, speed, targets)            // 从目标位置发射
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | String | - | 投射物类型（Bukkit EntityType 名称） |
| `speed` | Double | `1.0` | 发射速度倍率 |
| `targets` | ProxyTargetContainer | sender | 发射者 |

常用投射物类型：

| 类型 | 说明 |
|------|------|
| `ARROW` | 箭 |
| `FIREBALL` | 火球 |
| `SMALL_FIREBALL` | 小火球 |
| `SNOWBALL` | 雪球 |
| `EGG` | 鸡蛋 |
| `ENDER_PEARL` | 末影珍珠 |
| `WITHER_SKULL` | 凋零头颅 |
| `TRIDENT` | 三叉戟 |

```javascript
// 发射一支箭
projectile("ARROW")

// 发射高速火球
projectile("FIREBALL", 3.0)
```

## projectileAt — 朝指定方向发射

```javascript
projectileAt(type, x, y, z, speed)
projectileAt(type, x, y, z, speed, targets)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | String | 投射物类型 |
| `x, y, z` | Double | 方向向量 |
| `speed` | Double | 速度倍率 |

```javascript
// 向正上方发射箭
projectileAt("ARROW", 0, 1, 0, 2.0)

// 向前方偏上发射火球
projectileAt("FIREBALL", 0, 0.5, 1, 1.5)
```

## projectileToward — 朝目标方向发射

```javascript
projectileToward(type, speed)                       // sender 朝目标发射
projectileToward(type, speed, sources)              // 从 sources 朝目标发射
projectileToward(type, speed, sources, dests)       // 从 sources 朝 dests 发射
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | String | 投射物类型 |
| `speed` | Double | 速度倍率 |
| `sources` | ProxyTargetContainer | 发射者 |
| `dests` | ProxyTargetContainer | 目标位置 |

```javascript
// 向最近的怪物发射箭
var target = finder().range(20).excludeType("PLAYER").sort("DISTANCE").limit(1).build()
projectileToward("ARROW", 2.0, null, target)
```

---

## 完整示例

```javascript
function main() {
  // 向前方发射 3 支箭（扇形）
  projectileAt("ARROW", -0.3, 0.1, 1, 2.0)
  projectileAt("ARROW", 0, 0.1, 1, 2.0)
  projectileAt("ARROW", 0.3, 0.1, 1, 2.0)

  sound("ENTITY_ARROW_SHOOT", 1.0, 1.0)
  setCooldown(skill, 60)
}
```
