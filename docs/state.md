# Planners V2 状态系统说明

状态系统用于给实体附加可叠层、可计时的 buff/debuff。当前版本统一使用 SE（ScriptEngine）执行标准 JavaScript 脚本，不再使用 Kether 指令。

## 系统概览

- 状态配置位于 `plugins/Planners/state/`，内置示例来自 `state/example.yml`。
- 每个 `.yml` 文件可以定义多个状态，节点名就是状态 ID。
- 执行 `/planners reload` 后会重新加载状态配置。
- 状态挂载在实体元数据 `__pl.state.<状态ID>` 下，记录层数、持续时间和到期时间。
- 状态没有 `static` 模式。所有状态都必须通过 `stateAPI.attach` 挂载，并可通过 `stateAPI.detach` 或 `stateAPI.remove` 移除。

## 配置格式

```yaml
state0:
  priority: 0
  max-layer: 3
  name: "眩晕"
  action: |
    function main() {
      // 状态配置加载或重载时执行一次
    }

    function onStateAttach() {
      tell("眩晕层数增加")
    }

    function onStateDetach() {
      tell("眩晕层数减少")
    }

    function onStateMount() {
      tell("首次获得眩晕")
    }

    function onStateClose() {
      tell("完全解除眩晕")
    }

    function onStateEnd() {
      tell("眩晕自然结束")
    }
```

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `priority` | number | `0.0` | 状态优先级字段。当前内置挂载、移除流程不会按该字段排序。 |
| `max-layer` | int | 无上限 | 最大叠加层数。小于等于 `0` 表示无上限。 |
| `name` | string | 状态 ID | 状态显示名。 |
| `attribute` | string list | 空 | 外部属性插件属性源。每层状态都会重复追加一次该列表，不参与 Planners 逻辑属性转换。 |
| `action` | string | 空 | SE JavaScript 脚本，可定义状态生命周期函数。 |

## 状态属性

状态可以直接提供 AttributePlus 等外部属性插件的属性源：

```yaml
berserk:
  priority: 0
  max-layer: 3
  name: "狂暴"
  attribute:
    - "物理攻击: 10"
    - "暴击几率: 5"
```

`attribute` 不走 Planners 的逻辑属性/二级属性转换，也不执行公式。状态当前有几层，就把这组属性源重复追加几次。比如 `berserk` 为 3 层时，会向外部属性插件写入 3 份同样的属性行。

每个状态使用独立属性源 ID：`__pl.state.<状态ID>`。状态层数变化时会覆盖该状态自己的属性源；状态完全移除时会清理该属性源。

## SE 脚本入口

状态 `action` 使用标准 JavaScript。脚本中可以直接调用 Planners 注册的全局函数和 API 对象，例如 `tell`、`potion`、`fire`、`freeze`、`finder`、`stateAPI.attach`、`stateAPI.detach` 等。

生命周期函数为可选函数。未定义某个函数时，该阶段不会执行额外脚本。

| 函数 | 调用时机 | 可用变量 |
| --- | --- | --- |
| `main()` | 状态配置加载或重载时执行一次 | 全局函数 |
| `onStateAttach()` | 状态成功附加后执行；首次附加和后续叠层都会触发 | `sender`、`event`、`state` |
| `onStateMount()` | 状态从无到有时执行 | `sender`、`event`、`state` |
| `onStateDetach()` | 状态开始减少层数时执行 | `sender`、`event`、`state` |
| `onStateClose()` | 状态即将完全移除时执行 | `sender`、`event`、`state` |
| `onStateEnd()` | 状态计时到期事件触发时执行 | `sender`、`event`、`state` |

常见顺序：

| 场景 | 顺序 |
| --- | --- |
| 首次附加 | `onStateAttach()` -> `onStateMount()` |
| 后续叠层 | `onStateAttach()` |
| 手动移除部分层数 | `onStateDetach()` |
| 手动移除到 0 层 | `onStateDetach()` -> `onStateClose()` |
| 计时到期 | `onStateEnd()` -> `onStateDetach()` -> `onStateClose()` |

## 状态函数

### `stateAPI.attach`

```javascript
stateAPI.attach(target, id, duration)
stateAPI.attach(target, id, duration, refresh)
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `target` | ProxyTarget.Entity | 必填 | 要附加状态的单个实体目标。 |
| `id` | string | 必填 | 状态 ID。 |
| `duration` | long | 必填 | 持续时间，单位为 tick，必须大于 `0`。 |
| `refresh` | boolean | `true` | 目标已有该状态时，是否刷新剩余时间。 |

行为：

- 目标没有该状态时，创建 1 层状态并启动计时。
- 目标已有该状态时，尝试增加 1 层。
- 达到 `max-layer` 后不会继续加层。
- 达到层数上限时，如果 `refresh` 为 `true`，仍会刷新剩余时间。
- 返回 `true` 表示状态已挂载、叠层或刷新；其他情况返回 `false`。

### `stateAPI.detach`

```javascript
stateAPI.detach(target, id)
stateAPI.detach(target, id, layer)
```

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `target` | ProxyTarget.Entity | 必填 | 要卸载状态的单个实体目标。 |
| `id` | string | 必填 | 状态 ID。 |
| `layer` | int | `1` | 移除层数。传入 `999` 表示清空全部层数。 |

返回 `true` 表示至少移除了一个状态层；状态不存在、目标不支持或状态被事件取消时返回 `false`。

### `stateAPI.remove`

```javascript
stateAPI.remove(target, id)
```

完整移除目标身上的指定状态，效果等同于清空全部层数。

### `stateAPI.has`

```javascript
stateAPI.has(target, id)
```

返回 `true` 或 `false`。

### `stateAPI.getLayer`

```javascript
stateAPI.getLayer(target, id)
```

返回目标指定状态的当前有效层数；状态不存在、已失效或目标不支持时返回 `0`。

`stateAPI` 不解析目标集合，也不会隐式回退到 `sender`。多个目标需要由脚本逐个遍历并调用 API。

## 技能中附加状态

```yaml
frost_bolt:
  __option__:
    name: "寒冰箭"
    variables:
      damage: 30 * level + 50

  action: |
    function main() {
      var targets = finder().range(10).limit(1).sort("DISTANCE").build()
      healthTake(30 * level + 50, targets)
      for (const target of targets) {
        stateAPI.attach(target, "frozen", 60, true)
      }
      freeze(60, targets)
      sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0, targets)
    }
```

## 状态示例

```yaml
frozen:
  priority: 0
  max-layer: 3
  name: "冰冻"
  action: |
    function onStateMount() {
      tell("&b你被冻结了")
      potion("SLOW", 2, 60)
      freeze(60)
    }

    function onStateAttach() {
      tell("&b冰冻层数增加")
    }

    function onStateClose() {
      tell("&a冰冻解除")
      potionRemove("SLOW")
      freeze(0)
    }
```

## 命令

| 命令 | 权限 | 执行者 | 说明 |
| --- | --- | --- | --- |
| `/planners test <状态ID> <持续tick>` | `planners.command` | 玩家 | 给自己附加指定状态，主要用于测试。 |
| `/planners state trigger <玩家> <名称>` | `planners.command` | 管理员、控制台 | 广播自定义脚本触发事件，供扩展监听使用。该命令不会自动调用状态 `action` 中的生命周期函数。 |

## MythicMobs 兼容

| Mechanic | 参数 | 说明 |
| --- | --- | --- |
| `plstateattach` / `pl-state-attach` | `state`/`id`、`duration`/`time`/`t`、`refresh`/`cover` | 给目标附加状态。 |
| `plstatedetach` / `pl-state-detach` | `state`/`id`、`layer` | 移除目标状态层数；`layer >= 999` 时完整移除。 |
| `plstatecustomtrigger` / `pl-state-customtrigger` | `name`/`trigger`/`id` | 广播自定义脚本触发事件，供扩展监听使用。 |

## 注意事项

- `duration` 必须大于 `0`。传入 `0` 或负数时不会挂载状态。
- `action` 是 SE JavaScript，不支持 Kether 的 `state attach`、`tell "文本"`、`listen: state attach` 等写法。
- `trigger:` 配置节点已经废弃。请把逻辑写进 `action` 的生命周期函数。
- `stateAPI.remove` 会直接清空所有层数；只想减少层数时使用 `stateAPI.detach`。
- 状态函数只会处理实体目标。控制台、纯位置目标不能携带状态。
