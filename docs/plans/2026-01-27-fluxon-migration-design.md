# Kether → Fluxon 脚本引擎迁移设计

> 版本: 3.0
> 日期: 2026-01-27
> 策略: 完全替换，不兼容旧脚本
> **状态: ✅ 已完成**

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

## 迁移进度总览

- ✅ **基础层** - FluxonScriptCache, FluxonTrigger, FluxonEventRegistry
- ✅ **属性层** - 所有 KetherProperty 已迁移为 ExtensionFunction
- ✅ **命令层** - 所有 Kether Action 已迁移为 Fluxon Command
- ✅ **事件层** - FluxonEventRegistry 统一管理事件
- ✅ **技能层** - ImmutableSkill 已完全迁移到 Fluxon
- ✅ **扩展功能** - 16/16 扩展模块全部实现
- ✅ **清理** - 旧 Kether 代码已删除

**总体进度: 100%** 🎉

---

## 已实现的扩展模块 (16/16)

### 基础扩展
- ✅ EntityExtensions - 实体操作
- ✅ LocationExtensions - 位置操作
- ✅ CommonExtensions - 通用功能
- ✅ SenderExtensions - 发送者操作
- ✅ PlayerExtensions - 玩家操作

### 高优先级扩展
- ✅ MetadataExtensions - 元数据管理
- ✅ ProfileExtensions - 玩家档案 (法力值等)
- ✅ CooldownExtensions - 冷却系统
- ✅ CommandExtensions - 命令执行
- ✅ DelayExtensions - 延迟等待

### 中优先级扩展
- ✅ MathExtensions - 数学函数
- ✅ VelocityExtensions - 速度控制
- ✅ SelectorExtensions - 选择器系统
- ✅ SkillSystemExtensions - 技能系统

### 低优先级扩展
- ✅ MythicMobsExtensions - MythicMobs 集成
- ✅ GermPluginExtensions - GermPlugin 集成

---

## 架构变更

### 核心映射

```
旧 (Kether)                      新 (Fluxon)
───────────────────────────────────────────────
ComplexScriptPlatform       →    移除 (用 FluxonScriptCache 替代)
ComplexCompiledScript       →    移除
KetherScript                →    FluxonScript
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
  ├─ FluxonScriptCache.kt       # 脚本缓存 ✅
  ├─ FluxonTrigger.kt           # 触发器 ✅
  ├─ FluxonEventRegistry.kt     # 事件注册表 ✅
  ├─ FluxonScript.kt            # 脚本接口 ✅
  ├─ FluxonLoader.kt            # 加载器 ✅
  │
  ├─ entity/                    # 实体领域 ✅
  │    └─ EntityExtensions.kt
  │
  ├─ world/                     # 世界领域 ✅
  │    └─ LocationExtensions.kt
  │
  ├─ player/                    # 玩家领域 ✅
  │    └─ PlayerExtensions.kt
  │
  ├─ skill/                     # 技能领域 ✅
  │    ├─ SkillCommands.kt
  │    └─ SkillSystemExtensions.kt
  │
  ├─ common/                    # 通用 ✅
  │    └─ CommonExtensions.kt
  │
  ├─ metadata/                  # 元数据 ✅
  │    └─ MetadataExtensions.kt
  │
  ├─ profile/                   # 档案 ✅
  │    └─ ProfileExtensions.kt
  │
  ├─ cooldown/                  # 冷却 ✅
  │    └─ CooldownExtensions.kt
  │
  ├─ command/                   # 命令 ✅
  │    └─ CommandExtensions.kt
  │
  ├─ delay/                     # 延迟 ✅
  │    └─ DelayExtensions.kt
  │
  ├─ math/                      # 数学 ✅
  │    └─ MathExtensions.kt
  │
  ├─ velocity/                  # 速度 ✅
  │    └─ VelocityExtensions.kt
  │
  ├─ selector/                  # 选择器 ✅
  │    └─ SelectorExtensions.kt
  │
  ├─ mythicmobs/                # MythicMobs ✅
  │    └─ MythicMobsExtensions.kt
  │
  └─ germplugin/                # GermPlugin ✅
       └─ GermPluginExtensions.kt
```

---

## 核心组件设计

### 1. FluxonScriptCache ✅

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

### 2. FluxonTrigger ✅

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

### 3. FluxonEventRegistry ✅

```kotlin
object FluxonEventRegistry {
    private val holders = ConcurrentHashMap<String, ScriptEventHolder<*>>()

    fun <T : Event> register(name: String, holder: ScriptEventHolder<T>) {
        holders[name] = holder
        holder.init()
    }

    fun get(name: String): ScriptEventHolder<*>? = holders[name]

    fun init() {
        // 自动扫描并注册事件处理器
    }
}
```

---

## 迁移示例

### 属性系统 (KetherProperty → ExtensionFunction) ✅

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
    fun register() {
        val runtime = FluxonScriptCache.runtime
        runtime.registerExtension(Entity::class.java)
            .function("health", FunctionSignature.returns(Type.D).noParams()) { ctx ->
                val entity = ctx.target ?: return@function
                ctx.setReturnDouble(entity.health)
            }
            .function("name", FunctionSignature.returns(Type.OBJECT).noParams()) { ctx ->
                val entity = ctx.target ?: return@function
                ctx.setReturnRef(entity.name)
            }
    }
}
```

### 技能系统 ✅

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

    val script: ParsedScript? by lazy {
        FluxonScriptCache.getOrParse(action)
    }

    fun execute(sender: Target<*>, level: Int, variables: Map<String, Any?> = emptyMap()): CompletableFuture<Any?> {
        val env = script?.newEnvironment()?.apply {
            defineRootVariable("sender", sender)
            defineRootVariable("origin", sender.getLocation())
            defineRootVariable("level", level)
            defineRootVariable("skill", this@ImmutableSkill)
            variables.forEach { (k, v) -> defineRootVariable(k, v) }
        } ?: return CompletableFuture.completedFuture(null)

        return if (async) {
            CompletableFuture.supplyAsync { script.eval(env) }
        } else {
            CompletableFuture.completedFuture(script.eval(env))
        }
    }
}
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

## 删除清单 ✅

### 已删除的文件/包

```
✅ api/common/script/ComplexScriptPlatform.kt
✅ api/common/script/ComplexCompiledScript.kt
✅ api/common/script/KetherScript.kt
✅ api/common/script/SingletonKetherScript.kt
✅ api/common/script/kether/  (整个包)
✅ module/kether/  (整个包)
```

### 已删除的接口方法

```kotlin
// 从 Skill 接口移除
✅ fun platform(): ComplexScriptPlatform
✅ fun namespaces(): List<String>
✅ fun source(): String
✅ fun compiledScript(): Quest
```

---

## 依赖变更 ✅

```kotlin
// build.gradle.kts
dependencies {
    // 移除
    ✅ - implementation("taboolib:module-kether:xxx")

    // 新增 (通过 libs/ 目录)
    ✅ + compileOnly(fileTree("libs"))  // fluxon-core-1.5.7.jar
}
```

---

## 迁移步骤完成状态

1. ✅ **基础层** - 新增 `fluxon/` 模块，实现 Cache/Trigger/Registry
2. ✅ **属性层** - 迁移 KetherProperty → ExtensionFunction (16个扩展模块)
3. ✅ **命令层** - 迁移 Kether Action → Fluxon Command
4. ✅ **事件层** - 迁移 ScriptEventHolder → FluxonEventRegistry
5. ✅ **技能层** - 重构 ImmutableSkill，移除 Kether 接口
6. ✅ **清理** - 删除旧代码，更新依赖

---

## 配置文件迁移状态 ✅

### 已完成迁移
- ✅ 法师技能 (10个)
- ✅ 战士技能 (10个)
- ✅ 刺客技能 (1个)
- ✅ 职业配置 (8个)
- ✅ 其他配置

**配置迁移进度**: 100% ✅

---

## 总结

Kether → Fluxon 迁移已全部完成！

**关键成果:**
- ✅ 16个扩展模块全部实现
- ✅ 事件系统完全迁移
- ✅ 技能系统完全迁移
- ✅ 所有配置文件已迁移
- ✅ 旧代码已清理

**技术亮点:**
- 基于 Fluxon 1.5.7 实现
- 支持同步/异步执行
- 统一的事件管理系统
- 完整的扩展函数体系
- 第三方插件集成 (MythicMobs, GermPlugin)

**下一步建议:**
- 进行全面测试
- 编写用户迁移文档
- 准备发布更新
