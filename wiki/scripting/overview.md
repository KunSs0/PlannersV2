# 脚本引擎概述

Planners 使用标准 JavaScript 作为脚本语言。根据 Java 版本自动选择引擎：

| Java 版本 | 引擎 | 说明 |
|-----------|------|------|
| 8 ~ 14 | Nashorn | JDK 内置，无需额外依赖 |
| 17+ | GraalJS | 首次启动自动下载依赖 |

两个引擎对用户来说没有区别，写法完全一样。

---

## 脚本在哪里使用？

| 位置 | 说明 |
|------|------|
| 技能 `action` | 技能释放时执行的逻辑 |
| 状态 `action` | 状态生命周期回调 |
| 变量 `variables` | 动态计算变量值 |
| 等级算法 `experience` | 计算升级所需经验 |
| 升级条件 | 计算升级消耗 |
| 货币 `action` | 经济操作 |
| 转职条件 `if` | 判断是否满足转职条件 |
| 法力值公式 | 计算恢复量和上限 |

---

## 基本语法

Planners 脚本就是标准 JavaScript，如果你写过网页 JS，语法完全一样。

### 变量

```javascript
var x = 10
var name = "hello"
var arr = [1, 2, 3]
```

### 函数

```javascript
function main() {
  tell("Hello World")
}

function myHelper(a, b) {
  return a + b
}
```

### 条件判断

```javascript
if (level >= 5) {
  tell("等级足够")
} else {
  tell("等级不足")
}
```

### 循环

```javascript
for (var i = 0; i < 5; i++) {
  tell("第 " + i + " 次")
}
```

### 三元表达式

```javascript
// 常用于等级算法
var exp = level <= 10 ? level * 200 : level * 1000
```

---

## 技能脚本结构

```yaml
action: |
  function main() {
    // 技能释放时自动调用
    // 在这里写技能逻辑
  }

  function handleHit() {
    // 命中目标时调用（可选）
  }
```

- `function main()` 是必须的入口函数
- 如果脚本中没有定义 `main()`，整个脚本会被直接执行（不推荐）
- `function handleHit()` 是可选的命中回调

---

## 全局函数

Planners 预注册了大量全局函数，可以在脚本中直接调用，无需 import。

完整列表请参考各函数文档：

| 分类 | 文档 | 包含函数 |
|------|------|---------|
| 通用 | [通用函数](functions-common.md) | `tell` |
| 战斗 | [战斗函数](functions-combat.md) | `damage`, `heal`, `health*`, `velocity*`, `potion*`, `freeze`, `fire`, `explosion` |
| 实体 | [实体函数](functions-entity.md) | `entitySpawn`, `entityRemove`, `entityTeleport`, `entitySetAI` 等 |
| 命令 | [命令函数](functions-command.md) | `command`, `commandOp`, `commandConsole` |
| 冷却 | [冷却函数](functions-cooldown.md) | `getCooldown`, `setCooldown`, `resetCooldown`, `hasCooldown` |
| 状态 | [状态函数](functions-state.md) | `stateAttach`, `stateDetach`, `stateRemove`, `stateHas` |
| 声音 | [声音函数](functions-sound.md) | `sound`, `soundResource` |
| 投射物 | [投射物函数](functions-projectile.md) | `projectile`, `projectileAt`, `projectileToward` |
| 元数据 | [元数据函数](functions-metadata.md) | `getMeta`, `setMeta`, `hasMeta`, `removeMeta`, `setMetaTimeout` |
| 经济 | [经济函数](functions-economy.md) | `getBalance`, `takeMoney`, `giveMoney`, `setMoney` |
| 目标查找 | [目标查找器](target-finder.md) | `finder()` |
| 兼容插件 | [兼容插件函数](functions-compat.md) | DragonCore, GermPlugin, MythicMobs, AttributePlus |

---

## targets 参数约定

大多数全局函数都遵循一个约定：

- **不传 targets 参数** → 作用于 `sender`（技能释放者自己）
- **传入 targets 参数** → 作用于指定的目标集合

```javascript
// 对自己造成 10 点伤害
damage(10)

// 对目标造成 10 点伤害
var targets = finder().range(5).build()
damage(10, targets)
```

这个约定适用于 `damage`、`heal`、`potion`、`velocity*`、`sound`、`freeze`、`fire` 等几乎所有函数。

---

## sleep 函数

在技能脚本中可以使用 `sleep()` 暂停执行：

```javascript
function main() {
  tell("蓄力中...")
  sleep(1000)        // 暂停 1000 毫秒（1 秒）
  tell("释放！")
  sleep(500)         // 暂停 500 毫秒
  tell("完成！")
}
```

> **注意**：`sleep()` 只在 `async: true`（默认值）的技能中有效。如果技能是同步执行的，`sleep()` 会阻塞服务器主线程。
