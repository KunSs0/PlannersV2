# Fluxon → GraalJS 脚本引擎迁移计划

> 版本: 4.0
> 日期: 2026-03-03
> 策略: 完全替换，脚本语法改为标准 JavaScript
> 状态: 📋 计划中

## 概述

将脚本引擎从 Fluxon (自研脚本语言) 迁移到 GraalJS (标准 JS 引擎)。
迁移动机：更高的执行性能（预编译+缓存后微秒级）、标准语法降低用户学习成本。

## 迁移决策

| 决策项 | 选择 |
|-------|------|
| 引擎 | GraalJS (org.graalvm.polyglot) |
| 脚本语法 | 标准 JavaScript (ES2023+) |
| 旧脚本兼容 | 不兼容，用户需重写为 JS |
| API 暴露方式 | 全局函数 (如 `damage(10)`) |
| 异步模型 | CompletableFuture，保持现有模式 |
| 迁移顺序 | 自底向上 (引擎核心 → 扩展函数 → 上层集成) |

---

## 依赖变更

```kotlin
// build.gradle.kts
dependencies {
    // 移除
    - taboo("org.tabooproject.fluxon:core:1.6.1")

    // 新增
    + implementation("org.graalvm.polyglot:polyglot:24.1.1")
    + implementation("org.graalvm.polyglot:js:24.1.1")
}
```

---

## 架构变更

### 核心映射

```
旧 (Fluxon)                        新 (GraalJS)
───────────────────────────────────────────────────
FluxonRuntime                  →   GraalJS Engine (共享 Context Pool)
Fluxon.parse(source)           →   Source.newBuilder("js", ...).build()
ParsedScript                   →   Source (预编译缓存)
Environment                    →   Value bindings (Context.getBindings)
ParsedScript.eval(env)         →   Context.eval(source)
runtime.registerFunction()     →   bindings.putMember("name", ProxyExecutable)
FunctionContext                →   标准 JS 函数参数
FluxonScriptOptions            →   ScriptBindings (自定义绑定容器)
SingletonFluxonScript          →   CachedScript (新封装)
FluxonTrigger                  →   ScriptTrigger (新封装)
```

### 上下文变量

```
旧: env.defineRootVariable("sender", sender)
新: bindings.putMember("sender", sender)

注入变量 (保持一致):
  sender   - 执行者 (Player/Entity)
  event    - 触发事件
  level    - 技能等级
  skill    - 技能对象
  profile  - 玩家档案
  ctx      - SkillContext
```

---

## 核心组件设计

### 1. ScriptEngine (替代 FluxonScriptCache)

```kotlin
object ScriptEngine {
    private val cache = ConcurrentHashMap<String, Source>()
    private val engine = Engine.newBuilder()
        .option("engine.WarnInterpreterOnly", "false")
        .build()

    fun getOrCompile(source: String, name: String = "script"): Source {
        return cache.computeIfAbsent(source) {
            Source.newBuilder("js", it, name).cached(true).build()
        }
    }

    fun eval(source: String, bindings: ScriptBindings): Any? {
        val compiled = getOrCompile(source)
        return createContext(bindings).use { ctx ->
            ctx.eval(compiled).let { unwrap(it) }
        }
    }

    private fun createContext(bindings: ScriptBindings): Context {
        val ctx = Context.newBuilder("js")
            .engine(engine)
            .allowAllAccess(true)
            .build()
        val jsBindings = ctx.getBindings("js")
        // 注入全局函数
        GlobalFunctions.applyTo(jsBindings)
        // 注入上下文变量
        bindings.applyTo(jsBindings)
        return ctx
    }

    fun clear() = cache.clear()
}
```

### 2. ScriptBindings (替代 FluxonScriptOptions)

```kotlin
class ScriptBindings {
    internal val variables = mutableMapOf<String, Any?>()
    internal var async: Boolean = false

    fun set(key: String, value: Any?) = apply { variables[key] = value }
    fun async(async: Boolean) = apply { this.async = async }

    fun applyTo(jsBindings: Value) {
        variables.forEach { (k, v) -> jsBindings.putMember(k, v) }
    }

    companion object {
        fun forSkill(sender: Any, level: Int, skill: ImmutableSkill?, extraVars: Map<String, Any?> = emptyMap()): ScriptBindings {
            return ScriptBindings().apply {
                set("sender", sender)
                set("level", level)
                set("skill", skill)
                set("ctx", SkillContext(sender, level, skill))
                set("profile", PlayerTemplateAPI.getPlayerTemplate(sender))
                extraVars.forEach { (k, v) -> set(k, v) }
            }
        }
    }
}
```

### 3. GlobalFunctions (替代 runtime.registerFunction)

```kotlin
object GlobalFunctions {
    private val functions = mutableMapOf<String, ProxyExecutable>()

    fun register(name: String, fn: (args: Array<Value>) -> Any?) {
        functions[name] = ProxyExecutable { args -> fn(args) }
    }

    fun applyTo(bindings: Value) {
        functions.forEach { (name, fn) -> bindings.putMember(name, fn) }
    }
}
```

### 4. CachedScript (替代 SingletonFluxonScript)

```kotlin
open class CachedScript(source: String? = null) {
    open val action: String = source ?: ""
    val isNotNull: Boolean = action.isNotEmpty()

    fun run(bindings: ScriptBindings): CompletableFuture<Any?> {
        if (action.isEmpty()) return CompletableFuture.completedFuture(null)
        return if (bindings.async) {
            CompletableFuture.supplyAsync { ScriptEngine.eval(action, bindings) }
        } else {
            CompletableFuture.completedFuture(ScriptEngine.eval(action, bindings))
        }
    }

    fun eval(bindings: ScriptBindings = ScriptBindings()): Any? {
        if (action.isEmpty()) return null
        return ScriptEngine.eval(action, bindings)
    }
}
```

---

## 扩展函数迁移 (20 个模块)

### 注册方式变化

```kotlin
// 旧 (Fluxon): 通过 runtime + FunctionSignature
runtime.registerFunction("damage",
    returns(Type.VOID).params(Type.D, Type.OBJECT)
) { ctx ->
    val amount = ctx.getAsDouble(0)
    val targets = ctx.getRef(1)
    // ...
}

// 新 (GraalJS): ProxyExecutable 全局函数
GlobalFunctions.register("damage") { args ->
    val amount = args[0].asDouble()
    val targets = if (args.size > 1) resolveTargets(args[1]) else null
    // ...
}
```

### 模块迁移清单

| # | 模块 | 函数数 | 优先级 | 复杂度 |
|---|------|--------|--------|--------|
| 1 | CommonExtensions | 2 | 高 | 低 |
| 2 | CommandExtensions | 3 | 高 | 低 |
| 3 | MetadataExtensions | 5 | 高 | 低 |
| 4 | CooldownExtensions | 4 | 高 | 低 |
| 5 | ProfileExtensions | 6 | 高 | 中 |
| 6 | ContextExtensions | 4 | 高 | 低 |
| 7 | SkillCommands (damage/heal) | 8 | 高 | 中 |
| 8 | HealthExtensions | 3 | 高 | 低 |
| 9 | EffectExtensions | 3 | 中 | 低 |
| 10 | EntityExtensions | 12 | 中 | 高 |
| 11 | StateExtensions | 5 | 中 | 中 |
| 12 | VelocityExtensions | 4 | 中 | 中 |
| 13 | PotionExtensions | 2 | 中 | 低 |
| 14 | SoundExtensions | 3 | 中 | 低 |
| 15 | ProjectileExtensions | 3 | 中 | 高 |
| 16 | TargetFinder | 8 | 中 | 高 |
| 17 | SkillSystemExtensions | 2 | 中 | 中 |
| 18 | EconomyExtensions | 4 | 低 | 低 |
| 19 | MythicMobsExtensions | 3 | 低 | 中 |
| 20 | DragonCoreExtensions | 8 | 低 | 高 |
| 21 | AttributePlusExtensions | 5 | 低 | 中 |
| 22 | GermPluginExtensions | 10 | 低 | 高 |

---

## 脚本语法变化

### 技能脚本示例

```yaml
# 旧 (Fluxon)
action: |
  fn main() {
    if metadata 状态 contains 前摇 {
      sender :: resetCooldown(skill)
      return
    }
    damage(60 + &level * 15)
    stateAttach("燃烧", 40)
  }

# 新 (JavaScript)
action: |
  if (hasMeta("状态", "前摇")) {
    resetCooldown(skill);
    return;
  }
  damage(60 + level * 15);
  stateAttach("燃烧", 40);
```

### 状态回调示例

```yaml
# 旧 (Fluxon)
action: |
  fn onStateAttach() {
    sender :: tell("进入燃烧状态")
    fire(40)
  }
  fn onStateDetach() {
    sender :: tell("燃烧结束")
  }

# 新 (JavaScript)
action: |
  function onStateAttach() {
    tell("进入燃烧状态");
    fire(40);
  }
  function onStateDetach() {
    tell("燃烧结束");
  }
```

### 语法对照

| 功能 | Fluxon | JavaScript |
|-----|--------|------------|
| 变量声明 | `var x = 1` | `let x = 1` / `const x = 1` |
| 变量引用 | `&sender` / `*var` | `sender` / `varName` |
| 函数定义 | `fn name() { }` | `function name() { }` |
| 方法调用 | `sender :: tell("hi")` | `tell("hi")` |
| 字符串插值 | `"Hello ${&name}"` | `` `Hello ${name}` `` |
| 条件 | `if cond { } else { }` | `if (cond) { } else { }` |
| 链式调用 | `finder()::range(5)::build()` | `finder().range(5).build()` |
| 延迟 | `"label" :: delay("1s")` | `await delay(1000)` 或 `delay("1s")` |

---

## 实施步骤

### Phase 1: 引擎核心 (基础层)

1. 添加 GraalJS 依赖到 build.gradle.kts
2. 实现 `ScriptEngine` — 编译、缓存、执行
3. 实现 `ScriptBindings` — 上下文变量注入
4. 实现 `GlobalFunctions` — 全局函数注册框架
5. 实现 `CachedScript` — 替代 SingletonFluxonScript
6. 单元测试验证基础执行流程

### Phase 2: 扩展函数迁移

7. 迁移高优先级模块 (Common, Command, Metadata, Cooldown, Profile, Context, SkillCommands, Health)
8. 迁移中优先级模块 (Effect, Entity, State, Velocity, Potion, Sound, Projectile, Finder, SkillSystem)
9. 迁移低优先级模块 (Economy, MythicMobs, DragonCore, AttributePlus, GermPlugin)

### Phase 3: 上层集成

10. 重构 `FluxonScript.kt` → 使用 CachedScript
11. 重构 `FluxonLoader.kt` → 初始化 GraalJS Engine
12. 重构 `FluxonTrigger.kt` → 使用 ScriptEngine
13. 重构 `States.kt` → 状态回调改用 JS 函数调用
14. 重构 `ImmutableSkill` / `ImmutableState` 中的脚本字段

### Phase 4: 清理

15. 移除 Fluxon 依赖 (build.gradle.kts)
16. 删除 `FluxonScriptCache.kt`、`FluxonExts.kt` 等旧代码
17. 更新示例配置文件为 JS 语法
18. 重命名包路径 `module/fluxon/` → `module/script/` (可选)

---

## 性能优化要点

1. **共享 Engine 实例** — 全局单例 `Engine`，避免重复初始化
2. **Source 缓存** — `cached(true)` 启用编译缓存，重复执行跳过解析
3. **Context 池化** — 高频场景考虑 Context 对象池，减少 GC 压力
4. **Host Access 配置** — `allowAllAccess(true)` 允许 JS 直接访问 Java 对象，避免序列化开销

---

## 风险与应对

| 风险 | 应对 |
|-----|------|
| GraalJS 首次预热慢 (~200ms) | 插件启动时预热执行一次空脚本 |
| Context 创建开销 | 评估 Context 池化方案 |
| 用户脚本死循环 | 设置 `ResourceLimits` 限制执行时间 |
| Java 对象在 JS 中的类型映射 | 用 `@HostAccess.Export` 注解或 ProxyObject 适配 |
| 依赖体积增大 (~20MB) | GraalJS 仅运行时需要，不影响编译 |

---

## 删除清单

```
待删除文件/依赖:
- [ ] org.tabooproject.fluxon:core:1.6.1 (build.gradle.kts)
- [ ] FluxonScriptCache.kt
- [ ] FluxonExts.kt (参数解析工具迁移到新工具类后删除)
- [ ] 所有 Fluxon import 语句
- [ ] FluxonEventRegistry.kt (如已存在)
```

---

## 总结

本次迁移的核心价值：
- **性能提升**: GraalJS JIT 编译，热执行微秒级
- **标准语法**: JavaScript ES2023+，用户无需学习自研语法
- **生态优势**: 可复用 JS 生态的工具和库
- **维护成本降低**: 不再依赖小众脚本引擎
