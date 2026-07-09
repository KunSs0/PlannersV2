# Planners V2 文档入口

Planners V2 是基于 TabooLib 的职业、技能、状态和脚本化战斗系统。当前版本统一使用 SE（ScriptEngine）执行标准 JavaScript 脚本，不再使用 Kether 作为 Planners 的配置脚本语法。

## 当前脚本口径

| 项目 | 当前版本 |
| --- | --- |
| 脚本语言 | 标准 JavaScript |
| 脚本引擎 | SE（ScriptEngine） |
| 运行实现 | GraalJS |
| 脚本入口 | 技能 `action`、状态 `action`、条件表达式、变量公式等 |
| 全局函数与 API | 通过 `tell()`、`healthTake()`、`finder()`、`stateAPI.attach()` 等入口调用插件能力 |

旧文档中出现的 Kether 写法，例如 `state attach`、`tell "文本"`、`select @range 10`、`listen: state attach`，不适用于当前 Planners V2 配置。

## 推荐阅读顺序

1. [安装配置](../wiki/getting-started/installation.md)
2. [快速入门](../wiki/getting-started/quickstart.md)
3. [脚本引擎概述](../wiki/scripting/overview.md)
4. [技能配置](../wiki/config/skill.md)
5. [状态配置](../wiki/config/state.md)
6. [状态系统说明](state.md)

## 核心文档

| 文档 | 内容 |
| --- | --- |
| [脚本引擎概述](../wiki/scripting/overview.md) | SE 引擎、JavaScript 基础、脚本使用位置。 |
| [上下文变量](../wiki/scripting/context-variables.md) | `sender`、`profile`、`level`、`ctx`、`skill` 等变量。 |
| [目标查找器](../wiki/scripting/target-finder.md) | `finder()` 链式目标查找 API。 |
| [状态 API](../wiki/scripting/functions-state.md) | `stateAPI.attach`、`stateAPI.detach`、`stateAPI.remove`、`stateAPI.has`。 |
| [状态配置](../wiki/config/state.md) | 状态 YAML 结构、生命周期函数、排障。 |
| [状态系统说明](state.md) | 状态运行行为、命令、MythicMobs 兼容。 |

## 常用 SE 示例

### 技能中查找目标并附加状态

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

### 状态中处理生命周期

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

    function onStateClose() {
      tell("&a冰冻解除")
      potionRemove("SLOW")
      freeze(0)
    }
```

## 历史资料说明

仓库中仍可能保留部分第三方或旧版本 Kether 资料，用于迁移参考或历史归档。编写当前 Planners V2 配置时，请以 `wiki/` 下的 SE/JavaScript 文档和本页链接的状态系统文档为准。
