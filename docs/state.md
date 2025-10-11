# Planners V2 状态系统说明

本说明涵盖状态（State）在 Planners V2 中的配置、脚本指令、事件回调以及层数管理等最新行为。

## 系统概览

- 状态配置位于 `src/main/resources/state/`。每个 `.yml` 文件可定义多个状态，节点名即状态 ID。
- 执行 `planners reload` 时会重新载入配置，并通过 `Registries.STATE` 暴露于运行时。
- 非静态状态会在实体元数据 `pl.state.<id>` 下维护 `TargetStateHolder`，记录层数与剩余时间；静态状态 (`static: true`) 视为常驻，不会被系统移除。
- 状态支持叠层，可通过 `max-layer` 限制上限；超过上限时仅根据 `refreshDuration` 决定是否刷新时间。

## Kether 指令

所有 `state` 指令均注册在 `planners-common` 命名空间，脚本中可直接调用。

### `state attach`

```kether
state attach <stateId> [duration <-1>] [refresh <true>] [at <目标容器>]
```

- `stateId`：必须存在于 `Registries.STATE`。
- `duration`：持续时间（tick）；-1 代表由调用方或配置提供默认值。
- `refresh`：当状态已存在时是否刷新剩余时间，默认 `true`。
- `at`：可选目标容器，未指定时默认使用脚本 `sender`。
- 实际调用 `CapableState.attachState`，首次挂载会触发 `EntityStateEvent.Mount` 与 `Attach`，后续叠层只触发 `Attach`。

### `state detach`

```kether
state detach <stateId|~> [layer <1>] [at <目标容器>]
```

- `stateId` 或 `~`：`~` 会优先读取脚本变量 `@State`，否则解析文本。
- `layer`：要移除的层数，默认 `1`；传入 `999` 表示一次性清空。
- 执行 `CapableState.detachState`，触发 `EntityStateEvent.Detach`，当层数归零时额外触发 `EntityStateEvent.Close`。

### `state close`

```kether
state close <stateId|~> [at <目标容器>]
```

- 强制移除指定状态，直接触发 `CapableState.removeState`。
- 会依次广播 `EntityStateEvent.Detach` 与 `EntityStateEvent.Close`。

### `state has` / `state contains`

```kether
state has <stateId|~> [at <目标容器>]
```

- 判断目标是否拥有指定状态（忽略层数与剩余时间）。
- `state contains` 为同义指令。

## 配置示例

```yaml
state0:
  priority: 0
  max-layer: 3
  name: "眩晕"
  static: false
  trigger:
    "state-attach":
      listen: state attach
      action: |-
        tell "眩晕层数增加"
    "state-detach":
      listen: state detach
      action: |-
        tell "眩晕层数减少"
    "state-mount":
      listen: state mount
      action: |-
        tell "首次获得眩晕"
    "state-close":
      listen: state close
      action: |-
        tell "完全解除眩晕"
    "state-end":
      listen: state end
      action: |-
        tell "眩晕自然结束"
```

| 字段        | 类型   | 默认值   | 说明                                   |
|-------------|--------|----------|----------------------------------------|
| `priority`  | double | `0.0`    | 状态优先级，数值越大越先执行。         |
| `max-layer` | int    | `无上限` | 可叠加的最大层数，至少为 `1`。         |
| `static`    | bool   | `false`  | 静态状态不会被系统自动移除。           |
| `name`      | string | `id`     | 展示名称，用于界面或日志。             |
| `trigger`   | section| —        | 触发器集合，键为自定义触发器 ID。     |

## 事件与脚本绑定

| 事件类                         | 触发时机                                 | 备注                       |
|--------------------------------|------------------------------------------|----------------------------|
| `EntityStateEvent.Attach`      | 每次成功执行 `attachState` 后            | 兼容旧行为。               |
| `EntityStateEvent.Detach`      | 调用 `detachState` 或 `removeState` 后   | 永远会在层数减少时触发。   |
| `EntityStateEvent.Mount`       | 状态层数由 0 → 1 时                      | 监听首次挂载。             |
| `EntityStateEvent.Close`       | 状态层数从正数降为 0 时                  | 监听完全移除。             |
| `EntityStateEvent.End`         | 计时器到期且未被取消时                   | 结束流程仍会走 `removeState`。 |

脚本事件注册位于 `com.gitee.planners.core.skill.script.state.ScriptEntityState`，提供 `state attach/detach/mount/close/end` 等监听名称。

## 运行时行为要点

- `attachState`：首次挂载会创建 `TargetStateHolder`，注册到期任务；到期后由回调调用 `removeState`。
- 达到 `max-layer` 后再次调用 `attachState` 时，仅在 `refreshDuration=true` 时刷新剩余时间。
- `detachState`：按层数递减；当层数降为 0 时触发 `Detach` 与 `Close`，并清理元数据。
- `removeState`：无视层数直接移除，用于过期或强制清理。
- `States.tick` 会在实体过期时调用 `removeState`，确保层数归零并触发事件。

## MythicMobs 兼容

| Mechanic 名称                       | 功能说明           | 主要参数                                   |
|------------------------------------|--------------------|--------------------------------------------|
| `plstateattach` / `pl-state-attach` | 等价 `state attach` | `state`/`id`、`duration`/`time`、`refresh` |
| `plstatedetach` / `pl-state-detach` | 层数卸载/全清      | `state`/`id`、`layer`（默认 999，即全清）  |
| `plstatecustomtrigger`             | 触发自定义脚本事件 | `name`/`trigger`                            |

## 调试建议

1. 使用 `planners test <stateId> <duration>` 快速挂载状态，配合 `state detach/close` 验证层数变化。
2. 在脚本中结合 `state has` 做前置判断，避免达到上限后重复调用。
3. 监听 `state mount` 与 `state close` 事件，记录首次挂载与完全移除的关键节点。*** End Patch
