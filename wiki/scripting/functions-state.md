# 状态函数

用于在脚本中附加、移除、检查实体状态。

---

## stateAttach — 附加状态

```javascript
stateAttach(id, duration)                       // 给 sender 附加状态
stateAttach(id, duration, refresh)              // 附加并控制是否刷新持续时间
stateAttach(id, duration, refresh, targets)     // 给目标附加状态
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | String | 状态 ID（对应 `state/*.yml` 中定义的 key） |
| `duration` | Long | 持续时间（tick，20 tick = 1 秒） |
| `refresh` | Boolean | 可选，默认 `false`。是否刷新已有状态的持续时间 |
| `targets` | ProxyTargetContainer | 可选。目标 |

```javascript
// 给自己附加眩晕 3 秒
stateAttach("stun", 60)

// 给目标附加灼烧 5 秒，并刷新持续时间
stateAttach("burn", 100, true, targets)

// 不刷新持续时间（只叠加层数）
stateAttach("poison", 200, false, targets)
```

**关于层数叠加**：
- 每次调用 `stateAttach` 会增加 1 层
- 如果已达到 `max-layer` 上限且 `refresh` 为 `false`，不会有任何效果
- 如果 `refresh` 为 `true`，即使达到上限也会刷新持续时间

## stateDetach — 移除状态层数

```javascript
stateDetach(id)                                 // 移除 sender 的 1 层
stateDetach(id, layer)                          // 移除指定层数
stateDetach(id, layer, targets)                 // 移除目标的指定层数
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `id` | String | 状态 ID |
| `layer` | Int | 可选，默认 `1`。要移除的层数。传 `999` 表示移除所有层 |
| `targets` | ProxyTargetContainer | 可选。目标 |

```javascript
// 移除自己 1 层眩晕
stateDetach("stun")

// 移除目标 2 层灼烧
stateDetach("burn", 2, targets)

// 移除目标所有层
stateDetach("stun", 999, targets)
```

## stateRemove — 完全移除状态

```javascript
stateRemove(id)                                 // 完全移除 sender 的状态
stateRemove(id, targets)                        // 完全移除目标的状态
```

与 `stateDetach(id, 999)` 效果相同，但语义更清晰。

```javascript
stateRemove("stun")
stateRemove("burn", targets)
```

## stateHas — 检查是否拥有状态

```javascript
stateHas(id)                                    // 检查 sender
stateHas(id, targets)                           // 检查目标
```

返回值：`true` = 拥有该状态，`false` = 没有。

```javascript
if (stateHas("stun")) {
  tell("你正在眩晕中，无法释放技能！")
  return
}
```

---

## 完整示例

### 冰冻技能

```javascript
function main() {
  var targets = finder().range(10).excludeType("PLAYER").sort("DISTANCE").limit(3).build()

  // 造成伤害
  damage(30 + level * 15, targets)

  // 附加冰冻状态 3 秒
  stateAttach("frozen", 60, false, targets)

  // 播放效果
  sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0)
  freeze(60, targets)

  setCooldown(skill, 120)
  tell("&b冰冻！")
}
```

### 解除 debuff 技能

```javascript
function main() {
  // 移除自己身上的所有负面状态
  stateRemove("stun")
  stateRemove("burn")
  stateRemove("frozen")
  stateRemove("poison")

  // 移除药水负面效果
  potionRemove("SLOW")
  potionRemove("BLINDNESS")
  potionRemove("POISON")

  tell("&a所有负面效果已清除！")
  setCooldown(skill, 200)
}
```
