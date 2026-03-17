# API 参考

Planners 提供 Kotlin/Java API，位于 `com.gitee.planners.api` 包下。

---

## 引入依赖

```kotlin
// build.gradle.kts
compileOnly(files("libs/Planners.jar"))
```

---

## PlannersAPI

技能释放与变量获取。

```kotlin
import com.gitee.planners.api.PlannersAPI

// 释放技能（指定等级）
val future: CompletableFuture<Any?> = PlannersAPI.cast(player, skill, level)

// 释放玩家已学习的技能（使用玩家当前技能等级）
val result: ExecutableResult = PlannersAPI.cast(player, playerSkill)

// 获取技能变量值
val value: CompletableFuture<Any?> = PlannersAPI.getVariableValue(player, skill, "damage")
val value2: CompletableFuture<Any?> = PlannersAPI.getVariableValue(player, skill, variable)

// 创建脚本执行选项
val options: ScriptOptions = PlannersAPI.newOptions(player, skill)
val options2: ScriptOptions = PlannersAPI.newOptions(player, skill, level)
```

---

## PlayerTemplateAPI

玩家数据管理。

### 获取玩家档案

```kotlin
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate

// 获取玩家的 Planners 档案
val template: PlayerTemplate = player.plannersTemplate
```

### 职业

```kotlin
// 设置玩家职业路线
val future = PlayerTemplateAPI.setPlayerRoute(player, route)

// 获取当前职业
val route = template.route

// 清空职业
template.route = null
```

### 等级与经验

```kotlin
PlayerTemplateAPI.addLevel(player, 5)
PlayerTemplateAPI.setLevel(player, 10)

PlayerTemplateAPI.addExperience(player, 1000)
PlayerTemplateAPI.setExperience(player, 5000)
PlayerTemplateAPI.takeExperience(player, 200)
```

### 法力值

```kotlin
PlayerTemplateAPI.addMagicPoint(player, 50)
PlayerTemplateAPI.takeMagicPoint(player, 30)
PlayerTemplateAPI.setMagicPoint(player, 100)
PlayerTemplateAPI.resetMagicPoint(player)
```

### 技能

```kotlin
// 设置技能等级
PlayerTemplateAPI.setSkillLevel(template, playerSkill, 5)

// 绑定技能到按键
PlayerTemplateAPI.setSkillBinding(template, playerSkill, keyBinding)

// 获取玩家已注册的技能
val skills: Map<String, PlayerSkill> = template.getRegisteredSkill()
```

---

## KeyBindingAPI

按键绑定与图标格式化。

```kotlin
import com.gitee.planners.api.KeyBindingAPI

// 创建技能图标（用于 UI 显示）
val icon: IconFormatter = KeyBindingAPI.createIconFormatter(player, playerSkill)
val icon2: IconFormatter = KeyBindingAPI.createIconFormatter(player, playerSkill, level)
```

---

## Registries

资源注册表，获取已加载的配置对象。

```kotlin
import com.gitee.planners.api.Registries

// 获取所有职业
Registries.JOB.keys()                    // 所有职业 ID
Registries.JOB.values()                  // 所有职业对象
Registries.JOB.get("warrior")            // 按 ID 获取
Registries.JOB.getOrNull("warrior")      // 按 ID 获取（可能为 null）

// 获取所有技能
Registries.SKILL.keys()
Registries.SKILL.get("ground_slash")

// 获取所有路由
Registries.ROUTER.keys()

// 获取所有状态
Registries.STATE.keys()

// 获取所有按键绑定
Registries.KEYBINDING.keys()

// 获取等级算法
Registries.LEVEL.keys()

// 获取货币定义
Registries.CURRENCY.keys()
```

---

## 核心接口

### Job

```kotlin
interface Job : Unique, VariableProvider {
    val name: String
    fun getSkillOrNull(id: String): Skill?
    fun hasSkill(id: String): Boolean
}
```

### Skill

```kotlin
interface Skill : Unique, VariableProvider {
    val name: String
}
```

### Router

```kotlin
interface Router : Unique {
    val name: String
    val algorithmLevel: Algorithm?
    fun getRouteOrNull(id: String): Route?
    fun getRouteByJob(job: Job): Route?
}
```

### Route

```kotlin
interface Route : Unique {
    fun getBranches(): List<Route>
    fun getJob(): Job
    fun getIcon(): ItemStack?
    fun isInfer(player: Player, options: ScriptOptions): Condition.VerifyInfo
}
```

### Variable

```kotlin
interface Variable {
    val id: String
    fun run(options: ScriptOptions): CompletableFuture<Any?>
}
```

### KeyBinding

```kotlin
interface KeyBinding : Unique, Sortable {
    val name: String
}
```
