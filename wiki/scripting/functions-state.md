# 状态 API

状态 API 用于在 SE JavaScript 脚本中附加、移除和检查实体状态。状态 ID 对应 `plugins/Planners/state/*.yml` 中的顶层节点。

状态能力统一挂在 `stateAPI` 对象下，不再注册 `stateAttach`、`stateDetach`、`stateRemove`、`stateHas` 这类全局函数。

## stateAPI.attach

附加状态。

```javascript
stateAPI.attach(id, duration)
stateAPI.attach(id, duration, refresh)
stateAPI.attach(id, duration, refresh, targets)
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 必填 | 状态 ID。 |
| `duration` | long | 必填 | 持续时间，单位为 tick，必须大于 `0`。 |
| `refresh` | boolean | `true` | 已有状态时是否刷新剩余时间。 |
| `targets` | target container | `sender` | 目标集合。 |

```javascript
stateAPI.attach("stun", 60)
stateAPI.attach("burn", 100, true, targets)
stateAPI.attach("poison", 200, false, targets)
```

层数规则：

- 每次成功调用会尝试增加 1 层。
- 达到 `max-layer` 后不会继续增加层数。
- 达到层数上限后，`refresh` 为 `true` 时仍会刷新持续时间。

## stateAPI.detach

按层数移除状态。

```javascript
stateAPI.detach(id)
stateAPI.detach(id, layer)
stateAPI.detach(id, layer, targets)
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 必填 | 状态 ID。 |
| `layer` | int | `1` | 移除层数。传入 `999` 表示清空所有层。 |
| `targets` | target container | `sender` | 目标集合。 |

```javascript
stateAPI.detach("stun")
stateAPI.detach("burn", 2, targets)
stateAPI.detach("stun", 999, targets)
```

## stateAPI.remove

完整移除状态。

```javascript
stateAPI.remove(id)
stateAPI.remove(id, targets)
```

```javascript
stateAPI.remove("stun")
stateAPI.remove("burn", targets)
```

`stateAPI.remove(id, targets)` 用于清空所有层数。只想减少部分层数时使用 `stateAPI.detach`。

## stateAPI.has

检查目标是否拥有状态。

```javascript
stateAPI.has(id)
stateAPI.has(id, targets)
```

返回值：

| 返回值 | 说明 |
| --- | --- |
| `true` | 目标拥有该状态。 |
| `false` | 目标没有该状态，或状态 ID 不存在，或目标不是实体。 |

```javascript
if (stateAPI.has("stun")) {
  tell("你正在眩晕中，无法释放技能")
  return
}
```

## 完整示例

```javascript
function main() {
  var targets = finder().range(10).limit(3).sort("DISTANCE").build()

  healthTake(30 + level * 15, targets)
  stateAPI.attach("frozen", 60, true, targets)
  freeze(60, targets)
  sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0, targets)

  setCooldown(skill, 120)
  tell("&b冰冻！")
}
```
