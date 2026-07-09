# 状态配置

状态用于定义 buff、debuff、控制效果和临时标记。当前版本统一使用 SE（ScriptEngine）执行标准 JavaScript，状态配置中不再使用 Kether 或 `trigger` 节点。

## 文件位置

状态文件放在：

```text
plugins/Planners/state/
```

一个 `.yml` 文件可以定义多个状态。每个顶层节点都是一个状态 ID。

```yaml
stun:
  priority: 0
  max-layer: 3
  name: "眩晕"
  action: |
    function onStateMount() {
      tell("&c你被眩晕了")
    }
```

## 配置项

| 配置路径 | 类型 | 默认值 | 说明 | 生效方式 |
| --- | --- | --- | --- | --- |
| `<状态ID>.priority` | number | `0.0` | 状态优先级字段。当前内置状态流程不会按该字段排序。 | `/planners reload` |
| `<状态ID>.max-layer` | int | 无上限 | 最大叠加层数。小于等于 `0` 表示无上限。 | `/planners reload` |
| `<状态ID>.name` | string | 状态 ID | 状态显示名称。 | `/planners reload` |
| `<状态ID>.attribute` | string list | 空 | 外部属性插件属性源。每层状态都会重复追加一次该列表，不参与 Planners 逻辑属性转换。 | `/planners reload` |
| `<状态ID>.action` | string | 空 | SE JavaScript 脚本，定义生命周期函数。 | `/planners reload` |

## 状态属性

状态属性直接写给 AttributePlus 等外部属性插件，不走 Planners 的逻辑属性/二级属性转换，也不执行公式。

```yaml
berserk:
  priority: 0
  max-layer: 3
  name: "狂暴"
  attribute:
    - "物理攻击: 10"
    - "暴击几率: 5"
```

层数叠加规则：

```text
最终属性源 = attribute 列表重复 当前层数 次
```

例如 `berserk` 当前 3 层时，会写入 3 份 `物理攻击: 10` 和 3 份 `暴击几率: 5`。每个状态使用独立属性源 ID：`__pl.state.<状态ID>`，完整移除状态时会自动清理。

## 生命周期函数

在 `action` 中按需定义以下函数：

| 函数 | 触发时机 |
| --- | --- |
| `main()` | 状态配置加载或重载时执行一次。 |
| `onStateAttach()` | 每次成功附加状态后执行，包含首次附加和后续叠层。 |
| `onStateMount()` | 状态从无到有时执行。 |
| `onStateDetach()` | 状态开始减少层数时执行。 |
| `onStateClose()` | 状态即将完全移除时执行。 |
| `onStateEnd()` | 状态计时到期事件触发时执行。 |

首次附加时，当前顺序为：

```text
onStateAttach() -> onStateMount()
```

手动完整移除时，当前顺序为：

```text
onStateDetach() -> onStateClose()
```

## 完整示例

```yaml
stun:
  priority: 0
  max-layer: 3
  name: "眩晕"
  action: |
    function onStateMount() {
      tell("&c你被眩晕了")
      potion("SLOW", 2, 100)
      potion("BLINDNESS", 1, 100)
    }

    function onStateAttach() {
      tell("&c眩晕层数增加")
    }

    function onStateDetach() {
      tell("&e眩晕层数减少")
    }

    function onStateClose() {
      tell("&a眩晕解除")
      potionRemove("SLOW")
      potionRemove("BLINDNESS")
    }
```

## 在技能中使用状态

技能 `action` 同样使用 SE JavaScript，可以直接调用状态函数。

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
      stateAPI.attach("frozen", 60, true, targets)
      freeze(60, targets)
      sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0, targets)
    }
```

## 已移除的旧写法

以下写法属于旧 Kether/触发器文档，当前状态系统不再使用：

```yaml
stun:
  trigger:
    state-attach:
      listen: state attach
      action: |-
        tell "眩晕层数增加"
```

请改为：

```yaml
stun:
  action: |
    function onStateAttach() {
      tell("眩晕层数增加")
    }
```

## 排障

| 现象 | 检查项 |
| --- | --- |
| 状态没有挂上 | `duration` 必须大于 `0`；状态 ID 必须存在；目标必须是实体。 |
| 生命周期函数没执行 | 函数名必须完全匹配，例如 `onStateAttach`；脚本必须写在 `action` 下。 |
| 旧配置没有反应 | 删除 `trigger` / `listen` 写法，改为 SE JavaScript 函数。 |
| 层数不再增加 | 检查 `max-layer` 是否已经达到上限。 |
