# Planners V2 状态管理系统指南

## 系统概述
- 状态（State）用于在 `TargetEntity`（玩家、生物等）上附加持续效果、限制或数据标记。
- 配置文件位于 `src/main/resources/state/`。每个 `.yml` 文件可以定义一个或多个状态条目，键名即状态 ID。
- 插件启动或执行 `planners reload` 时，`Registries.STATE` 会读取所有文件并生成 `ImmutableState` 实例。`States` 组件随后把每个状态的触发器注册到脚本事件系统。
- 运行时通过实体元数据 `pl.state.<id>` 记录状态持有情况，并存储附加时间戳用于过期检测。静态状态（`static: true`）被视为永久存在，不参与元数据存储。

## 状态相关脚本命令

### `state attach`
```
state attach <stateId> [duration <-1>] [cover <true>] [at <目标容器>]
```
- `stateId` 需存在于 `Registries.STATE`，否则命令会中止并输出警告。
- `duration` 可选项，单位毫秒；为 -1 时表示永久状态，与旧版本保持兼容。
- `cover` 可选项，默认为 `true`, 表示状态会覆盖已有状态。
- `目标容器` 默认解析为脚本 `sender`，也可通过 `at &target` 指定 TabooLib 的目标集合。
- 成功解析的目标若为 `TargetEntity` 将触发 `EntityStateEvent.Attach.Pre/Post` 事件并调用 `addState`。

### `state detach`
```
state detach <stateId|~> [at <目标容器>]
```
- `~` 可从上下文读取当前状态 ID（`@State`），等价于显式写出该 ID。
- 静态状态无法移除，命令会直接返回。
- 仅对解析到的 `TargetEntity` 生效，逐个调用 `removeState`，并在命中时触发 `EntityStateEvent.Detach`。

### `state has`
```
state has <stateId|~> [at <目标容器>]
```
- `stateId` 同上，支持使用 `~` 占位符。
- 仅检查解析到的首个 `TargetEntity`；若目标为空或缺少该状态返回 `false`。
- 命令返回布尔值，可用于判定语句或逻辑组合。

### `state contains`
```
state contains <stateId|~> [at <目标容器>]
```
- 行为与 `state has` 等价，提供更贴近日常表述的别名。
- 可与 `if`、`and` 等脚本语句组合，实现更语义化的状态判断。

## 状态配置结构

### 文件示例
`src/main/resources/state/example.yml`（插件默认附带）

```yaml
state0:
  priority: 0
  name: "眩晕"
  static: true
  trigger:
    chat:
      action: |-
        tell "chat"
        state detach ~
    "state-attach":
      action: |-
        tell "我被眩晕了"
    "state-detach":
      action: |-
        tell "我已解除眩晕!"
```

默认示例通过 `chat`、`state-attach`、`state-detach` 三个触发器演示状态生命周期；若需要与上表提供的事件名对齐，可为触发器显式声明 `on` 字段（如 `on: "player chat"`、`on: "state attach"`）。

### 字段说明

| 字段        | 类型   | 默认值 | 说明 |
|-------------|--------|--------|------|
| `priority`  | double | `0.0`  | 状态优先级，值越大越先执行。|
| `static`    | bool   | `false`| 静态状态始终视为已附着，不会因计时而移除。|
| `name`      | string | `id`   | 展示名称，常用于 UI 或日志。|
| `trigger`   | section| —      | 触发器配置集合，键名即触发器 ID。|

> 状态持续时长不再由配置文件提供，需在脚本调用 `state attach` 时通过 `duration` 参数控制。

## 可用脚本事件

下表列出了 `com.gitee.planners.core.skill.script` 提供的事件名称及上下文。只有当 `sender` 可以转换为 `TargetEntity` 且确实持有该状态时，`States.ScriptCallbackImpl` 才会执行对应触发器。

| `on` 值                 | Bukkit 事件类型                                    | `sender` 来源                                      | 额外上下文键 |
|-------------------------|----------------------------------------------------|----------------------------------------------------|--------------|
| `damage`                | `EntityDamageByEntityEvent`                        | 攻击者（玩家）                                     | `event`（`DamageEventModifier`），`damager`，`entity` |
| `damaged`               | `EntityDamageByEntityEvent`                        | 受击的玩家                                         | 同 `damage` |
| `death`                 | `PlayerDeathEvent`                                 | 死亡的玩家                                         | `attacker`（`LivingEntity`），`message` |
| `projectile.hit`        | `ProjectileHitEvent`                               | 发射该投射物的玩家                                 | `entity`（投射物），`target`（命中目标 `Target`），`block`（命中方块位置） |
| `player join`           | `PlayerJoinEvent`                                  | 加入服务器的玩家                                   | — |
| `player joined`         | `PlayerProfileLoadedEvent`（Planners 自定义）      | 档案加载完成的玩家                                 | — |
| `player quit`           | `PlayerEvent`（当前实现绑定 `PlayerJoinEvent`）    | 玩家（待替换为离线事件）                           | — |
| `player chat`           | `AsyncPlayerChatEvent`                             | 发送消息的玩家                                     | — |
| `player attack`         | `EntityDamageByEntityEvent`                        | 攻击者（玩家）                                     | — |
| `player damaged`        | `EntityDamageByEntityEvent`                        | 受击的玩家                                         | — |
| `player toggle sprint`  | `PlayerToggleSprintEvent`                          | 切换疾跑的玩家                                     | — |
| `player toggle sneak`   | `PlayerToggleSprintEvent`（待更换为潜行事件）      | 切换状态的玩家                                     | — |
| `player cast skill`     | `PlayerSkillCastEvent.Post`                        | 释放技能的玩家                                     | — |
| `keyup`                 | `ProxyClientKeyEvents.Up`                          | 客户端按键释放的玩家                               | — |
| `keydown`               | `ProxyClientKeyEvents.Down`                        | 客户端按键按下的玩家                               | — |
| `state attach`          | `EntityStateEvent.Attach.Post`                     | 获得状态的实体                                     | — |
| `state detach`          | `EntityStateEvent.Detach.Post`                     | 失去状态的实体                                     | — |
| `state end`             | `EntityStateEvent.End`                            | 状态自然到期的实体                                   | — |
> 状态自然到期后会触发 state end 事件，可在脚本中处理到期后的收尾逻辑。`r`n
> `DamageEventModifier` 继承 `AbstractCancellableEvent`，通过 Animated 元字段暴露 `is-cancelled`、`source`、`damage`、`final-damage`、`cause` 等属性，可在 Kether 中读取或修改（参见 TabooLib Animated 文档）。

## 触发器上下文

- `@State`：当前触发的 `State` 实例，可读取元数据或配合 `state detach "~"` 使用。
- `@Trigger`：当前触发器的 `State.Trigger` 数据，包含触发器 ID 与 `on` 值。
- `event`：可选的 `AbstractEventModifier`，具体类型取决于事件持有者。
- 其他键由事件持有者注入，例如 `damager`、`entity`、`attacker`、`message`、`target`、`block` 等。
- `sender`：脚本命令上下文中的 `TargetEntity`，通常与事件主体一致，可继续调用 Target API。

## 状态生命周期与过期机制

- `TargetBukkitEntity.addState` 会在实体元数据中标记 `pl.state.<id>` 并记录当前时间；成功时依次广播 `EntityStateEvent.Attach.Pre/Post`。
- `TargetBukkitEntity.removeState` 在状态非静态且仍被持有时清除元数据，并广播 `EntityStateEvent.Detach.Pre/Post`。
- `TargetBukkitEntity.isExpired` 会读取实体元数据中的结束时间，完全由 `state attach` 传入的持续时长控制；`States.tick` 会清理超时状态，并在必要时调用 `States.record(target)`。
- 若需手动控制持续时间，可在触发器脚本中记录剩余时间并调用 `state detach "~"`。

## 调试与测试

- `planners test <stateId>`：把指定状态附加到执行该命令的玩家，便于快速验证触发器。
- `planners reload`：重新加载所有注册表（包括状态定义），会触发 `PluginReloadEvents.Pre/Post`，从而卸载并重建状态监听器。
- 开发阶段可在触发器脚本内使用 `print` 或自定义日志输出，观察上下文变量的实际值。

## 最佳实践

1. 让每个状态保持单一职责，便于组合与复用。
2. 合理设置 `priority`，确保互斥或依赖的状态按预期顺序执行。
3. 使用具有辨识度的触发器 ID，并避免在同一状态内重复。
4. 借助 `@State` 和 `state detach "~"` 复用脚本，降低硬编码 ID 的风险。
5. 对高频事件（如 `damage`、`projectile.hit`）在脚本内增加条件判断，避免不必要的运行成本。
## 第三方插件支持

目前仅适配 MythicMobs 4.x，在插件启动阶段监听 `MythicMechanicLoadEvent` 注册自定义 mechanic，这些机制的语义与 Kether `state` 指令一致：

| Mechanic 名称 | 对应行为 | 主要参数 |
|---------------|----------|----------|
| `plstateattach` / `pl-state-attach` | 等价 `state attach`，为目标实体附加状态 | `state`/`id`（必填），`duration`/`time`/`t`（毫秒，默认 -1），`cover`（默认 `true`） |
| `plstatedetach` / `pl-state-detach` | 等价 `state detach`，移除状态 | `state`/`id`（必填） |

> 适配逻辑位于 `com.gitee.planners.module.compat.mythic`，MythicMobs 其他版本会忽略这些 mechanic。

**示例（附加状态）**

```yaml
Skills:
  AttachState:
    Skills:
      - plstateattach{state=state0;duration=5000;cover=true} @Self
```

**示例（移除状态）**

```yaml
Skills:
  DetachState:
    Skills:
      - plstatedetach{state=state0} @Self
```

