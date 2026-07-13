# 状态 API

状态 API 用于在 SE JavaScript 脚本中附加、移除和检查实体状态。状态 ID 对应 `plugins/Planners/state/*.yml` 中的顶层节点。每次调用只处理一个已确认的实体目标。

状态能力统一挂在 `stateAPI` 对象下，不再注册 `stateAttach`、`stateDetach`、`stateRemove`、`stateHas` 这类全局函数。

## stateAPI.attach

附加状态。

```javascript
stateAPI.attach(target, id, duration)
stateAPI.attach(target, id, duration, refresh)
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `target` | ProxyTarget.Entity | 必填 | 要附加状态的单个实体目标。 |
| `id` | string | 必填 | 状态 ID。 |
| `duration` | long | 必填 | 持续时间，单位为 tick，必须大于 `0`。 |
| `refresh` | boolean | `true` | 已有状态时是否刷新剩余时间。 |

```javascript
stateAPI.attach(target, "stun", 60)
stateAPI.attach(target, "burn", 100, true)
stateAPI.attach(target, "poison", 200, false)
```

返回 `true` 表示状态已挂载、叠层或刷新；参数无效、状态不存在、目标不支持、状态被事件取消或层数已满且未刷新时返回 `false`。

层数规则：

- 每次成功调用会尝试增加 1 层。
- 达到 `max-layer` 后不会继续增加层数。
- 达到层数上限后，`refresh` 为 `true` 时仍会刷新持续时间。

## stateAPI.detach

按层数移除状态。

```javascript
stateAPI.detach(target, id)
stateAPI.detach(target, id, layer)
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `target` | ProxyTarget.Entity | 必填 | 要卸载状态的单个实体目标。 |
| `id` | string | 必填 | 状态 ID。 |
| `layer` | int | `1` | 移除层数。传入 `999` 表示清空所有层。 |

```javascript
stateAPI.detach(target, "stun")
stateAPI.detach(target, "burn", 2)
stateAPI.detach(target, "stun", 999)
```

返回 `true` 表示至少移除了一个状态层；状态不存在、目标不支持或状态被事件取消时返回 `false`。

## stateAPI.remove

完整移除状态。

```javascript
stateAPI.remove(target, id)
```

```javascript
stateAPI.remove(target, "stun")
```

`stateAPI.remove(target, id)` 用于清空所有层数。只想减少部分层数时使用 `stateAPI.detach`。

## stateAPI.has

检查目标是否拥有状态。

```javascript
stateAPI.has(target, id)
```

返回值：

| 返回值 | 说明 |
| --- | --- |
| `true` | 目标拥有该状态。 |
| `false` | 目标没有该状态，或状态 ID 不存在，或目标不是实体。 |

```javascript
if (stateAPI.has(target, "stun")) {
  tell("你正在眩晕中，无法释放技能")
  return
}
```

## stateAPI.getLayer

获取目标当前有效状态的层数。

```javascript
stateAPI.getLayer(target, id)
```

状态不存在、已失效或目标不支持时返回 `0`。

`stateAPI` 不解析目标集合，也不会隐式回退到 `sender`。需要处理多个目标时，由脚本先遍历目标集合，再逐个调用 API。

## 完整示例

```javascript
function main() {
  var targets = finder().range(10).limit(3).sort("DISTANCE").build()

  healthTake(30 + level * 15, targets)
  for (const target of targets) {
    stateAPI.attach(target, "frozen", 60, true)
  }
  freeze(60, targets)
  sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0, targets)

  setCooldown(skill, 120)
  tell("&b冰冻！")
}
```
