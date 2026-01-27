# Kether → Fluxon 脚本引擎迁移设计

> 版本: 3.0
> 日期: 2026-01-27
> 策略: 完全替换，不兼容旧脚本

## 概述

将脚本引擎从 Kether (树形命令框架) 迁移到 Fluxon (完整脚本语言)。

## 迁移决策

| 决策项 | 选择 |
|-------|------|
| 迁移范围 | 完全替换，移除所有 Kether 依赖 |
| 旧脚本兼容 | 不兼容，用户需重写 |
| 迁移顺序 | 自底向上 (基础层 → 上层) |
| 异步模型 | 按 async 配置项决定运行环境 |
| 上下文变量 | Environment 注入，用 `&var` 引用 |
| Action 迁移 | 全部转为 Fluxon Command |
| 代码组织 | 按业务领域分包 |

---

## 架构变更

### 核心映射

```
旧 (Kether)                      新 (Fluxon)
───────────────────────────────────────────────
ComplexScriptPlatform       →    移除 (用 FluxonScriptCache 替代)
ComplexCompiledScript       →    移除
KetherScript                →    移除
ScriptContext               →    Environment (Fluxon 原生)
KetherProperty              →    ExtensionFunction
ScriptEventHolder           →    FluxonTrigger + FluxonEventRegistry
CombinationKetherParser     →    CommandRegistry
```

### 上下文变量

```
旧: context.sender / context["sender"]
新: &sender (Environment 注入)

注入变量:
  &sender   - 执行者
  &origin   - 执行位置
  &event    - 触发事件
  &level    - 技能等级 (技能脚本)
  &skill    - 技能对象 (技能脚本)
```

---

## 新目录结构

```
com.gitee.planners.module.fluxon/
  ├─ FluxonScriptCache.kt       # 脚本缓存
  ├─ FluxonTrigger.kt           # 触发器
  ├─ FluxonEventRegistry.kt     # 事件注册表
  │
  ├─ entity/                    # 实体领域
  │    ├─ EntityCommands.kt
  │    └─ EntityExtensions.kt
  │
  ├─ world/                     # 世界领域
  │    ├─ WorldCommands.kt
  │    └─ LocationExtensions.kt
  │
  ├─ skill/                     # 技能领域
  │    └─ SkillCommands.kt
  │
  └─ common/                    # 通用
       └─ CommonCommands.kt
```

---

## 核心组件设计

### 1. FluxonScriptCache

```kotlin
object FluxonScriptCache {
    private val cache = ConcurrentHashMap<String, ParsedScript>()
    val runtime = FluxonRuntime()

    fun getOrParse(source: String): ParsedScript {
        return cache.computeIfAbsent(source) { Fluxon.parse(it) }
    }

    fun clear() = cache.clear()
}
```

### 2. FluxonTrigger

```kotlin
class FluxonTrigger(
    val id: String,
    val listen: String,
    val script: ParsedScript,
    val async: Boolean = false
) {
    fun execute(sender: Any, event: Event, variables: Map<String, Any?> = emptyMap()) {
        val env = Environment(FluxonScriptCache.runtime).apply {
            setVariable("sender", sender)
            setVariable("event", event)
            variables.forEach { (k, v) -> setVariable(k, v) }
        }

        if (async) {
            CompletableFuture.runAsync { script.eval(env) }
        } else {
            script.eval(env)
        }
    }
}
```

### 3. FluxonEventRegistry

```kotlin
object FluxonEventRegistry {
    private val holders = ConcurrentHashMap<String, FluxonEventHolder<*>>()

    fun <T : Event> register(name: String, holder: FluxonEventHolder<T>) {
        holders[name] = holder
    }

    fun get(name: String): FluxonEventHolder<*>? = holders[name]

    fun init() {
        register("player-join", PlayerJoinHolder)
        register("player-attack", PlayerAttackHolder)
        register("entity-damage", EntityDamageHolder)
    }
}
```

---

## 迁移示例

### 属性系统 (KetherProperty → ExtensionFunction)

**旧:**
```kotlin
@KetherProperty(TargetBukkitEntity::class)
fun property() = object : ScriptProperty<TargetBukkitEntity>("planners.entity") {
    override fun read(instance: TargetBukkitEntity, key: String) = when (key) {
        "health" -> OpenResult.successful(instance.get().health)
        "name" -> OpenResult.successful(instance.get().name)
        else -> OpenResult.failed()
    }
}
```

**新:**
```kotlin
object EntityExtensions {
    fun register(runtime: FluxonRuntime) {
        runtime.registerExtensionFunction(Entity::class.java, "health", 0) { ctx ->
            ctx.target.health
        }
        runtime.registerExtensionFunction(Entity::class.java, "name", 0) { ctx ->
            ctx.target.name
        }
        runtime.registerExtensionFunction(Entity::class.java, "setHealth", 1) { ctx ->
            ctx.target.health = ctx.getArgAsDouble(0)
            null
        }
    }
}
```

### 技能系统

**旧:**
```kotlin
class ImmutableSkill : Skill, ComplexCompiledScript {
    override fun source() = action
    override fun namespaces() = listOf(NAMESPACE_COMMON, NAMESPACE_SKILL)
    override fun platform() = ComplexScriptPlatform.SKILL
}
```

**新:**
```kotlin
class ImmutableSkill(config: Configuration) : Skill {
    private val action = config.getString("action", "")!!

    val script: ParsedScript by lazy {
        FluxonScriptCache.getOrParse(action)
    }

    fun execute(sender: Target<*>, level: Int, variables: Map<String, Any?> = emptyMap()): CompletableFuture<Any?> {
        val env = Environment(FluxonScriptCache.runtime).apply {
            setVariable("sender", sender)
            setVariable("origin", sender.getLocation())
            setVariable("level", level)
            setVariable("skill", this@ImmutableSkill)
            variables.forEach { (k, v) -> setVariable(k, v) }
        }

        return if (async) {
            CompletableFuture.supplyAsync { script.eval(env) }
        } else {
            CompletableFuture.completedFuture(script.eval(env))
        }
    }
}
```

### Command 注册

**旧:**
```kotlin
@CombinationKetherParser.Used
fun damage() = simpleKetherParser("damage") {
    val amount = it.nextDouble()
    actionTake { damage(amount) }
}
```

**新:**
```kotlin
CommandRegistry.primary().register("damage",
    { parser ->
        val amount = parser.parseExpression()
        val target = if (parser.tryConsume("to")) parser.parseExpression() else null
        DamageData(amount, target)
    },
    { env, data ->
        val amount = data.amount.eval(env) as Double
        val target = data.target?.eval(env) ?: env.getVariable("sender")
        (target as? LivingEntity)?.damage(amount)
        null
    }
)
```

---

## 脚本语法变化

| 功能 | 旧 (Kether) | 新 (Fluxon) |
|-----|-------------|-------------|
| 变量引用 | `&var` 或 `{{ var }}` | `&var` |
| 属性读取 | `&entity health` | `&entity::health()` |
| 属性写入 | 无直接支持 | `&entity::setHealth(20)` |
| 条件 | `if then { } else { }` | `if cond then a else b` |
| 循环 | `repeat 10 { }` | `for i in 0..9 { }` |
| 字符串插值 | `"Hello \<&name>"` | `"Hello ${&name}"` |

---

## 删除清单

### 删除的文件/包

```
api/common/script/ComplexScriptPlatform.kt
api/common/script/ComplexCompiledScript.kt
api/common/script/KetherScript.kt
api/common/script/SingletonKetherScript.kt
api/common/script/kether/  (整个包)
module/kether/  (整个包)
```

### 删除的接口方法

```kotlin
// 从 Skill 接口移除
fun platform(): ComplexScriptPlatform
fun namespaces(): List<String>
fun source(): String
fun compiledScript(): Quest
```

---

## 依赖变更

```kotlin
// build.gradle.kts
dependencies {
    // 移除
    - implementation("taboolib:module-kether:xxx")

    // 新增
    + implementation("org.tabooproject:fluxon-core:xxx")
}
```

---

## 迁移步骤

1. **基础层** - 新增 `fluxon/` 模块，实现 Cache/Trigger/Registry
2. **属性层** - 迁移 KetherProperty → ExtensionFunction
3. **命令层** - 迁移 Kether Action → Fluxon Command
4. **事件层** - 迁移 ScriptEventHolder → FluxonEventRegistry
5. **技能层** - 重构 ImmutableSkill，移除 Kether 接口
6. **清理** - 删除旧代码，更新依赖
