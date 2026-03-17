# 状态系统

状态文件位于 `plugins/Planners/state/` 目录，用于定义 buff/debuff 等可叠加的实体状态。

---

## 什么是状态？

状态就是附加在实体身上的效果，比如「眩晕」「灼烧」「加速」等。状态有以下特点：

- **可叠加**：同一个状态可以叠加多层（由 `max-layer` 控制上限）
- **有持续时间**：到期后自动移除
- **有生命周期回调**：在附加、移除、到期等时机执行脚本

---

## 完整示例

以下是项目自带的 `state/example.yml`：

```yaml
state0:
  # 优先级（数值越小优先级越高）
  priority: 0
  # 最大叠加层数
  max-layer: 3
  # 状态显示名称
  name: "眩晕"
  # 状态脚本
  action: |
    function main() {
      // 状态加载时执行一次，可自行注册 listener
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

一个文件中可以定义多个状态，每个状态以其 ID 作为 key（如 `state0`）。

---

## 字段说明

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `priority` | Double | 否 | `0` | 优先级，数值越小优先级越高 |
| `max-layer` | Int | 否 | 无限 | 最大叠加层数。不填或填 0 表示无限叠加 |
| `name` | String | 否 | 状态 ID | 状态显示名称 |
| `static` | Boolean | 否 | `false` | 是否为静态状态。静态状态永远存在，无法被移除 |
| `action` | String | 否 | 空 | 状态脚本（标准 JavaScript） |

---

## 生命周期回调函数

在 `action` 脚本中，你可以定义以下函数，它们会在对应时机被自动调用：

| 函数名 | 调用时机 | 说明 |
|--------|---------|------|
| `main()` | 状态首次加载时 | 只执行一次，用于初始化 |
| `onStateMount()` | 状态首次附加时 | 从 0 层变为 1 层时触发 |
| `onStateAttach()` | 每次层数增加时 | 包括首次附加和后续叠加 |
| `onStateDetach()` | 每次层数减少时 | 包括部分移除和完全移除 |
| `onStateClose()` | 状态完全移除时 | 层数从 >0 变为 0 时触发 |
| `onStateEnd()` | 状态持续时间到期时 | 自然到期，不是手动移除 |

### 调用顺序

**首次附加状态**：
```
onStateMount() → onStateAttach()
```

**叠加层数**：
```
onStateAttach()
```

**手动移除一层**：
```
onStateDetach()
```

**完全移除（层数归零）**：
```
onStateDetach() → onStateClose()
```

**持续时间到期**：
```
onStateEnd() → onStateDetach() → onStateClose()
```

---

## 在脚本中操作状态

### 附加状态

```javascript
// stateAttach(状态ID, 持续时间tick)
stateAttach("stun", 100)                    // 给 sender 附加眩晕 100 tick

// stateAttach(状态ID, 持续时间tick, 是否刷新)
stateAttach("stun", 100, true)              // 附加并刷新持续时间

// stateAttach(状态ID, 持续时间tick, 是否刷新, 目标)
stateAttach("stun", 100, false, targets)    // 给指定目标附加
```

### 移除状态

```javascript
// stateDetach(状态ID) — 移除 sender 的 1 层
stateDetach("stun")

// stateDetach(状态ID, 层数) — 移除指定层数
stateDetach("stun", 2)

// stateDetach(状态ID, 层数, 目标)
stateDetach("stun", 1, targets)

// stateRemove(状态ID) — 完全移除所有层
stateRemove("stun")
stateRemove("stun", targets)
```

### 检查状态

```javascript
// stateHas(状态ID) — 检查 sender 是否拥有状态
if (stateHas("stun")) {
  tell("你正在眩晕中！")
}

// stateHas(状态ID, 目标)
stateHas("stun", targets)
```

---

## 实战示例

### 眩晕状态

```yaml
stun:
  priority: 0
  max-layer: 3
  name: "眩晕"
  action: |
    function onStateMount() {
      tell("&c你被眩晕了！")
      // 给予缓慢和失明效果
      potion("SLOW", 2, 100)
      potion("BLINDNESS", 0, 100)
    }
    function onStateAttach() {
      tell("&c眩晕叠加！")
    }
    function onStateClose() {
      tell("&a眩晕解除")
      // 移除药水效果
      potionRemove("SLOW")
      potionRemove("BLINDNESS")
    }
    function onStateEnd() {
      tell("&a眩晕自然消退")
    }
```

### 灼烧状态

```yaml
burn:
  priority: 1
  max-layer: 5
  name: "灼烧"
  action: |
    function onStateMount() {
      tell("&c你着火了！")
      fire(100)
    }
    function onStateAttach() {
      // 每次叠加都造成 5 点伤害
      damage(5)
    }
    function onStateClose() {
      tell("&a火焰熄灭")
    }
```

### 在技能中使用状态

```yaml
# skill/frost_bolt.yml
__option__:
  name: "寒冰箭"
  variables:
    damage: 30 * level + 50

action: |
  function main() {
    var targets = finder().range(10).type("ZOMBIE,SKELETON").limit(1).sort("DISTANCE").build()
    damage(30 * level + 50, targets)
    // 给目标附加冰冻状态 3 秒
    stateAttach("frozen", 60, false, targets)
    sound("ENTITY_PLAYER_HURT_FREEZE", 1.0, 1.0)
    setCooldown(skill, 100)
  }
```

---

## 相关命令

| 命令 | 说明 |
|------|------|
| `/planners test <状态ID> <持续时间tick>` | 给自己附加状态（测试用） |
| `/planners state trigger <玩家> <触发器名>` | 触发自定义状态触发器 |
