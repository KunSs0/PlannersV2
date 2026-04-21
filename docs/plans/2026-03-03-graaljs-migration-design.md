# Fluxon → JavaScript 脚本引擎迁移计划

> 版本: 5.0
> 日期: 2026-03-03
> 策略: 完全替换，脚本语法改为标准 JavaScript
> 实现语言: **Java** (新增代码放置在 Java 源码目录)
> 状态: ✅ 全部完成 (Phase 1-4 已实施)

## 概述

将脚本引擎从 Fluxon (自研脚本语言) 迁移到标准 JavaScript。
迁移动机：标准语法降低用户学习成本、更高的执行性能。

## 迁移决策

| 决策项 | 选择 |
|-------|------|
| 引擎 | 双引擎: Nashorn (Java 8~14) / GraalJS (Java 17+) |
| 脚本语法 | 标准 JavaScript |
| 旧脚本兼容 | 不兼容，用户需重写为 JS |
| API 暴露方式 | 全局函数 (如 `damage(10)`) |
| 异步模型 | CompletableFuture，保持现有模式 |
| 迁移顺序 | 自底向上 (引擎核心 → 扩展函数 → 上层集成) |
| Java 兼容性 | 编译目标 Java 8，运行时自动选择引擎 |

### 双引擎架构

```
JsEngine (接口)
├── NashornEngine   — Java 8~14，javax.script API，内置无额外依赖
└── GraalJsEngine   — Java 17+，polyglot API，高性能 JIT (后续实现)

ScriptManager (门面)
└── 运行时检测 Java 版本 → 自动选择引擎
    ├── >= 17 且 GraalJS 可用 → GraalJsEngine
    └── 其他 → NashornEngine
```

---

## 依赖变更

```kotlin
// build.gradle.kts
dependencies {
    // 移除
    - taboo("org.tabooproject.fluxon:core:1.6.1")

    // 新增 (GraalJS 仅运行时，不影响 Java 8 编译)
    + runtimeOnly("org.graalvm.polyglot:polyglot:24.1.1")
    + runtimeOnly("org.graalvm.polyglot:js:24.1.1")
    // Nashorn: Java 8~14 内置，无需额外依赖
}
```

---

## 架构变更

### 核心映射

```
旧 (Fluxon)                        新 (JS)
───────────────────────────────────────────────────
FluxonRuntime                  →   JsEngine (接口，双引擎实现)
Fluxon.parse(source)           →   JsEngine.eval() / Compilable.compile()
ParsedScript                   →   CompiledScript (Nashorn) / Source (GraalJS)
Environment                    →   Bindings (Nashorn) / Value (GraalJS)
ParsedScript.eval(env)         →   JsEngine.eval(source, variables)
runtime.registerFunction()     →   GlobalFunctions.register(name, JsFunction)
FunctionContext                →   Object[] (统一参数数组)
FluxonScriptOptions            →   ScriptOptions (保留"执行选项"语义)
SingletonFluxonScript          →   SingletonScript (保留"单次脚本封装"语义)
FluxonScriptCache              →   ScriptManager (静态门面)
FluxonTrigger                  →   ScriptTrigger (新封装)
```

### 上下文变量

```
旧: env.defineRootVariable("sender", sender)
新: ScriptOptions.set("sender", sender) → 自动注入到引擎 Bindings

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

### 1. ScriptManager (替代 FluxonScriptCache)

> 命名说明: 避免与 GraalJS 的 `Engine` 类和 `javax.script.ScriptEngine` 冲突

```java
public final class ScriptManager {
    private static final ConcurrentHashMap<String, Source> cache = new ConcurrentHashMap<>();
    private static final Engine engine = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    public static Source getOrCompile(String source, String name) {
        return cache.computeIfAbsent(source, s ->
                Source.newBuilder("js", s, name).cached(true).buildLiteral());
    }

    /** 一次性执行，Context 用完即弃 */
    public static Object eval(String source, ScriptOptions options) {
        Source compiled = getOrCompile(source, "script");
        try (Context ctx = newContext(options)) {
            return unwrap(ctx.eval(compiled));
        }
    }

    /** 创建会话，调用方管理生命周期（用于状态回调等跨调用场景） */
    public static ScriptSession openSession(ScriptOptions options) {
        return new ScriptSession(newContext(options));
    }

    private static Context newContext(ScriptOptions options) {
        Context ctx = Context.newBuilder("js")
                .engine(engine)
                .allowAllAccess(true)
                .build();
        Value bindings = ctx.getBindings("js");
        GlobalFunctions.applyTo(bindings);
        options.applyTo(bindings);
        return ctx;
    }

    public static void clear() { cache.clear(); }
}
```

### 2. ScriptOptions (替代 FluxonScriptOptions)

> 命名说明: 保留"执行选项"语义，该类不仅包含变量绑定，还包含 async 等执行配置

```java
public class ScriptOptions {
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private boolean async = false;

    public ScriptOptions set(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public ScriptOptions async(boolean async) {
        this.async = async;
        return this;
    }

    public boolean isAsync() { return async; }
    public Map<String, Object> getVariables() { return variables; }

    public void applyTo(Value jsBindings) {
        variables.forEach(jsBindings::putMember);
    }

    public static ScriptOptions forSkill(Object sender, int level, ImmutableSkill skill, Map<String, Object> extraVars) {
        ScriptOptions options = new ScriptOptions();
        options.set("sender", sender);
        options.set("level", level);
        options.set("skill", skill);
        options.set("ctx", new SkillContext(sender, level, skill));
        options.set("profile", PlayerTemplateAPI.getPlayerTemplate(sender));
        if (extraVars != null) extraVars.forEach(options::set);
        return options;
    }
}
```

### 3. GlobalFunctions (替代 runtime.registerFunction)

```java
public final class GlobalFunctions {
    private static final Map<String, ProxyExecutable> functions = new ConcurrentHashMap<>();

    public static void register(String name, ProxyExecutable fn) {
        functions.put(name, fn);
    }

    public static void applyTo(Value bindings) {
        functions.forEach(bindings::putMember);
    }
}
```

### 4. SingletonScript (替代 SingletonFluxonScript)

> 命名说明: 保留"单次脚本封装"语义 — 封装单个脚本源码并提供执行入口，缓存由 ScriptManager 负责

```java
public class SingletonScript {
    private final String action;

    public SingletonScript(String source) {
        this.action = source != null ? source : "";
    }

    public boolean isNotNull() { return !action.isEmpty(); }
    public String getAction() { return action; }

    public CompletableFuture<Object> run(ScriptOptions options) {
        if (action.isEmpty()) return CompletableFuture.completedFuture(null);
        if (options.isAsync()) {
            return CompletableFuture.supplyAsync(() -> ScriptManager.eval(action, options));
        }
        return CompletableFuture.completedFuture(ScriptManager.eval(action, options));
    }

    public Object eval(ScriptOptions options) {
        if (action.isEmpty()) return null;
        return ScriptManager.eval(action, options);
    }

    public Object eval() {
        return eval(new ScriptOptions());
    }
}
```

---

## 扩展函数迁移 (22 个模块)

### 注册方式变化

```kotlin
// 旧 (Fluxon/Kotlin): 通过 runtime + FunctionSignature
runtime.registerFunction("damage",
    returns(Type.VOID).params(Type.D, Type.OBJECT)
) { ctx ->
    val amount = ctx.getAsDouble(0)
    val targets = ctx.getRef(1)
    // ...
}
```

```java
// 新 (GraalJS/Java): ProxyExecutable 全局函数
GlobalFunctions.register("damage", args -> {
    double amount = args[0].asDouble();
    ProxyTargetContainer targets = args.length > 1 ? resolveTargets(args[1]) : null;
    // ...
    return null;
});
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

### Phase 1: 引擎核心 (基础层) — ✅ 已完成

1. ✅ 添加 GraalJS 依赖到 build.gradle.kts
2. ✅ 实现 `ScriptManager` — 编译、缓存、执行 (Java)
3. ✅ 实现 `ScriptOptions` — 上下文变量注入 + 执行配置 (Java)
4. ✅ 实现 `GlobalFunctions` — 全局函数注册框架 (Java)
5. ✅ 实现 `SingletonScript` — 替代 SingletonFluxonScript (Java)
6. ✅ 实现 `GraalJsEngine` / `GraalJsSession` / `GraalJsDependency`

### Phase 2: 扩展函数迁移 — ✅ 已完成

7. ✅ 迁移高优先级模块 (Common, Command, Metadata, Cooldown, SkillCommands, Health)
8. ✅ 迁移中优先级模块 (Effect, Entity, State, Velocity, Potion, Sound, Projectile, Finder, SkillSystem)
9. ✅ 迁移低优先级模块 (Economy, MythicMobs, DragonCore, AttributePlus, GermPlugin)
10. ✅ Bootstrap 注册已接入，全部 Functions 类的 `register()` 已在引擎初始化时调用
11. ✅ TargetFinder / MythicObject 已迁至 `module.script` 包

### Phase 3: 上层集成 — ✅ 已完成

12. ✅ 重构 `FluxonScript.kt` → 使用 SingletonScript
13. ✅ 重构 `FluxonLoader.kt` → 初始化 JS Engine + bootstrap 注册全部 Functions
14. ✅ 重构 `FluxonTrigger.kt` → 使用 ScriptManager
15. ✅ 重构 `States.kt` → 状态回调改用 ScriptSession
16. ✅ 重构 `ImmutableSkill` / `ImmutableState` 中的脚本字段
17. ✅ 全部 21 个外部引用文件的 Fluxon 引用已替换为 JS 引擎对应类

> **涉及的外部引用文件 (21 个)**:
>
> | 引用类 | 文件 |
> |-------|------|
> | `FluxonScriptOptions` (13处) | PlannersAPI, OpenConvertibleCurrencyImpl, PlayerRouteTransferUI, Route, Condition, Algorithm, ImmutableRoute, ImmutableVariable, States, PlaceholderScript, AttributeProvider, DefaultMagicPointProvider, PlayerRoute |
> | `SingletonFluxonScript` (8处) | OpenConvertibleCurrencyImpl, Condition, Algorithm, ImmutableVariable, DynamicSkillIcon, PlaceholderScript, AttributeProvider, DefaultMagicPointProvider |
> | `FluxonScriptCache` (3处) | ImmutableSkill, ImmutableState, States |
> | `FluxonScript` (2处) | PlayerSkillUpgradeUI, Variable |
> | `ParsedScript` (3处) | State, ImmutableSkill, ImmutableState |

### Phase 4: 清理 — ✅ 已完成

18. ✅ 移除 Fluxon 依赖 (build.gradle.kts): `org.tabooproject.fluxon:core:1.6.1`
19. ✅ 删除 `module/fluxon/` 整个目录 (5 核心文件 + 22 扩展模块)
20. ✅ 删除 `libs/FluxonPlugin-1.0.2-beta-2.jar`
21. ✅ 更新示例配置文件为 JS 语法
22. ✅ 重命名包路径 `module/fluxon/` → `module/script/`
23. ✅ 实现 GraalJsEngine / GraalJsSession / GraalJsDependency

---

## 性能优化要点

1. **共享 Engine 实例** — 全局单例 `Engine`，避免重复初始化
2. **Source 缓存** — `cached(true)` 启用编译缓存，重复执行跳过解析
3. **ScriptSession** — 状态回调等跨调用场景使用会话模式，调用方管理生命周期
4. **Host Access 配置** — `allowAllAccess(true)` 允许 JS 直接访问 Java 对象，避免序列化开销

---

## 待解决问题

### 问题 1: Context 创建开销 — ❌ 无需优化

Context 轻量，GC 压力可忽略，保持每次新建 + try-with-resources 即可。
跨调用场景（状态回调）通过 `ScriptSession` 管理生命周期（见核心组件设计）。

**状态**: ❌ 已关闭（无需优化）

### 问题 2: 线程安全

GraalJS `Context` 是单线程的，不能被多个线程并发访问。

**分析现有执行路径**:

| 路径 | 线程 | Context 来源 | 安全性 |
|------|------|-------------|--------|
| `ScriptManager.eval()` | 调用线程 | `newContext()` 每次新建 | ✅ 安全 — 独立 Context |
| `SingletonScript.eval()` | 主线程 (sync) | 内部调 `ScriptManager.eval()` | ✅ 安全 |
| `SingletonScript.run(async=true)` | ForkJoinPool 线程 | `supplyAsync` 内调 `ScriptManager.eval()` | ✅ 安全 — 每次调用新建独立 Context |
| `ScriptSession` (状态回调) | 创建线程 | `openSession()` 新建 | ⚠️ 需约束：创建和调用必须在同一线程 |

**结论**: 由于采用每次新建 Context 方案（问题 1 结论），绝大多数路径天然线程安全 — 每次 `eval()` 都是独立 Context，无共享状态。

**唯一需要注意的点**: `ScriptSession` 跨调用持有 Context，必须保证同一线程使用。
状态回调场景（`onStateAttach`/`onStateDetach`）由 `States.tick()` 在主线程调用，天然满足。

```java
public class ScriptSession implements AutoCloseable {
    private final Context ctx;
    private final long ownerThread = Thread.currentThread().getId();

    // 防御性检查（可选，开发阶段启用）
    private void checkThread() {
        assert Thread.currentThread().getId() == ownerThread
            : "ScriptSession must be used on the creating thread";
    }

    public Object eval(Source source) {
        checkThread();
        return ctx.eval(source);
    }

    public Value getFunction(String name) {
        checkThread();
        return ctx.getBindings("js").getMember(name);
    }

    @Override
    public void close() {
        ctx.close();
    }
}
```

**状态**: ✅ 已细化

### 问题 3: 缺少执行超时 ✅ 已细化

防止用户脚本死循环。GraalJS 提供两种机制：

| 机制 | API | 特点 |
|------|-----|------|
| 语句计数限制 | `ResourceLimits.statementLimit()` | 轻量，但单条语句耗时不恒定（如 `Array.sort`） |
| 手动取消 | `Context.close(true)` 从另一线程调用 | 可靠的时间限制，需额外调度线程 |

**方案: 语句计数 + ScheduledExecutor 兜底**

语句计数拦截常规死循环，ScheduledExecutor 兜底处理单条语句耗时过长的极端情况。

```java
private static final int STATEMENT_LIMIT = 100_000;
private static final long TIMEOUT_MS = 5_000;
private static final ScheduledExecutorService watchdog =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "script-watchdog");
            t.setDaemon(true);
            return t;
        });

private static Context newContext(ScriptOptions options) {
    ResourceLimits limits = ResourceLimits.newBuilder()
            .statementLimit(STATEMENT_LIMIT, null)
            .build();

    Context ctx = Context.newBuilder("js")
            .engine(engine)
            .allowAllAccess(true)
            .resourceLimits(limits)
            .build();

    Value bindings = ctx.getBindings("js");
    GlobalFunctions.applyTo(bindings);
    options.applyTo(bindings);
    return ctx;
}

public static Object eval(String source, ScriptOptions options) {
    Source compiled = getOrCompile(source, "script");
    Context ctx = newContext(options);
    // 超时兜底：5 秒后强制关闭
    ScheduledFuture<?> timeout = watchdog.schedule(
            () -> ctx.close(true), TIMEOUT_MS, TimeUnit.MILLISECONDS);
    try {
        return unwrap(ctx.eval(compiled));
    } catch (PolyglotException e) {
        if (e.isCancelled() || e.isResourceExhausted()) {
            throw new ScriptTimeoutException("脚本执行超时或超出语句限制: " + source);
        }
        throw e;
    } finally {
        timeout.cancel(false);
        ctx.close();
    }
}
```

**设计要点**:
- `statementLimit(100_000)` — 拦截 `while(true)` 等常规死循环，开销低
- `watchdog.schedule()` — 5 秒硬超时兜底，处理 `Array.sort(巨大数组)` 等单语句耗时场景
- watchdog 为 daemon 线程，不阻止 JVM 退出
- `ScriptSession` 同样需要在 `close()` 中取消超时（长生命周期场景由调用方控制超时策略）
- 超时后 Context 不可复用（GraalJS 限制），符合当前每次新建的方案

**ScriptSession 的超时处理**:

状态回调等长生命周期场景，单次函数调用仍应有超时保护：

```java
public class ScriptSession implements AutoCloseable {
    public Object eval(Source source) {
        checkThread();
        ScheduledFuture<?> timeout = ScriptManager.watchdog.schedule(
                () -> ctx.close(true), TIMEOUT_MS, TimeUnit.MILLISECONDS);
        try {
            return ctx.eval(source);
        } finally {
            timeout.cancel(false);
        }
    }
}
```

> 注意：`ctx.close(true)` 后 Context 不可用，ScriptSession 需感知此状态并在后续调用中快速失败。

**状态**: ✅ 已细化

### 问题 4: allowAllAccess 安全隐患 ✅ 已细化

`allowAllAccess(true)` 使用户脚本可访问任意 Java 类（如 `java.lang.Runtime.exec()`）。

**分析注入到脚本的对象**:

| 变量 | 类型 | 来源 |
|------|------|------|
| `sender` | ProxyTarget / Player / Entity | 技能/触发器 |
| `level` | int | 技能等级 |
| `skill` | ImmutableSkill | 技能定义 |
| `ctx` | SkillContext | 执行上下文 |
| `profile` | PlayerTemplate | 玩家档案 |
| `origin` | Location | 位置 |
| `event` | Event | 触发事件 |
| `state` | State | 状态对象 |

脚本通过 `GlobalFunctions` 注册的全局函数（如 `damage()`、`tell()`）操作游戏逻辑，
不需要直接调用注入对象的 Java 方法。注入对象主要作为参数传递给全局函数。

**方案: 分离 Host Access 与 Public Access**

```java
private static final HostAccess HOST_ACCESS = HostAccess.newBuilder()
        .allowPublicAccess(true)           // 允许访问注入对象的 public 方法
        .allowAllImplementations(true)     // 允许 JS 实现 Java 接口 (ProxyExecutable 需要)
        .allowArrayAccess(true)            // JS 数组 ↔ Java 数组
        .allowListAccess(true)             // JS 数组 ↔ Java List
        .allowMapAccess(true)              // JS 对象 ↔ Java Map
        .build();

private static Context newContext(ScriptOptions options) {
    Context ctx = Context.newBuilder("js")
            .engine(engine)
            .allowHostAccess(HOST_ACCESS)
            // 不设置 allowAllAccess(true)
            // 不设置 allowHostClassLookup — 禁止 JS 通过 Java.type() 访问任意类
            .build();
    // ...
}
```

**效果**:
- ✅ 脚本可以读取注入对象的 public 属性/方法（如 `sender.getName()`）
- ✅ 全局函数正常工作（ProxyExecutable）
- ❌ 脚本无法 `Java.type("java.lang.Runtime")` 访问任意 Java 类
- ❌ 脚本无法访问 private/protected 成员

**状态**: ✅ 已细化

### 问题 5: Source 缓存 key 策略 — ❌ 无需优化

`ConcurrentHashMap` 用 String 做 key 时，查找基于 `hashCode()`（O(1)），不会逐字符比较。
Java String 的 hashCode 在首次计算后缓存，后续调用是常数时间。
实际脚本数量有限（几十到几百），不构成性能问题。

**状态**: ❌ 已关闭（无需优化）

### 问题 6: 状态回调函数发现机制 ✅ 已细化

**旧方案 (Fluxon)**:
```kotlin
// 1. 执行脚本，注册函数到环境
script.eval(env)
// 2. 解析调用语句来调用函数，不存在则 catch 忽略
try { FluxonScriptCache.getOrParse("onStateAttach()").eval(env) }
catch (_: Exception) { }
```

问题：通过解析+执行调用语句来发现函数，不够直接。

**新方案 (GraalJS + ScriptSession)**:

JS eval 后函数自然注册到 bindings 中，直接通过 `getMember()` 获取：

```java
// States.invokeCallback 改造
private void invokeCallback(State state, ProxyTarget<?> entity, Object event, String funcName) {
    if (state.getSession() == null) return;
    ScriptSession session = state.getSession();

    Value fn = session.getFunction(funcName);
    if (fn != null && fn.canExecute()) {
        fn.execute();
    }
}
```

**State 生命周期与 ScriptSession 绑定**:

```java
// ImmutableState 持有 ScriptSession
public class ImmutableState {
    private ScriptSession session;

    /** 状态初始化时创建 Session，执行脚本注册回调函数 */
    public void init(ScriptOptions options) {
        this.session = ScriptManager.openSession(options);
        session.eval(ScriptManager.getOrCompile(actionSource, id));
        // 此时 onStateAttach / onStateDetach 等函数已注册到 bindings
        // 调用 main()（如果存在）
        Value main = session.getFunction("main");
        if (main != null && main.canExecute()) {
            main.execute();
        }
    }

    /** 状态完全关闭时释放 Session */
    public void destroy() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
```

**回调约定（保持不变）**:
| 回调函数 | 触发时机 |
|---------|---------|
| `main()` | 状态初始化时执行一次 |
| `onStateAttach()` | 状态附加到实体 |
| `onStateDetach()` | 状态从实体移除 |
| `onStateMount()` | 状态首次挂载 |
| `onStateClose()` | 状态完全关闭 |
| `onStateEnd()` | 状态自然结束 |

**对比旧方案的优势**:
- 直接 `getMember()` 获取函数引用，无需每次解析调用语句
- `canExecute()` 显式判断函数是否存在，不靠 try-catch
- ScriptSession 与 State 生命周期绑定，回调函数引用在整个生命周期内有效

**状态**: ✅ 已细化

### 问题 7: 脚本异常处理 ✅ 已细化

脚本执行中可能出现的异常：

| 异常类型 | 场景 | PolyglotException 属性 |
|---------|------|----------------------|
| 语法错误 | `Source.build()` 或首次 `eval()` | `isSyntaxError()` |
| 运行时错误 | 空引用、类型错误等 | `isGuestException()` |
| 超时/语句超限 | 死循环 (问题 3) | `isCancelled()` / `isResourceExhausted()` |
| Host 异常 | 全局函数内部抛出 Java 异常 | `isHostException()` |

**方案: ScriptManager 统一捕获 + 日志**

```java
public static Object eval(String source, ScriptOptions options) {
    Source compiled;
    try {
        compiled = getOrCompile(source, "script");
    } catch (PolyglotException e) {
        logScriptError("编译失败", source, e);
        return null;
    }

    Context ctx = newContext(options);
    ScheduledFuture<?> timeout = watchdog.schedule(
            () -> ctx.close(true), TIMEOUT_MS, TimeUnit.MILLISECONDS);
    try {
        return unwrap(ctx.eval(compiled));
    } catch (PolyglotException e) {
        handlePolyglotException(source, e);
        return null;
    } finally {
        timeout.cancel(false);
        ctx.close();
    }
}

private static void handlePolyglotException(String source, PolyglotException e) {
    if (e.isCancelled() || e.isResourceExhausted()) {
        logScriptError("执行超时", source, e);
    } else if (e.isSyntaxError()) {
        logScriptError("语法错误", source, e);
    } else if (e.isHostException()) {
        // 全局函数内部异常，打印完整堆栈便于开发者排查
        logScriptError("内部错误", source, e.asHostException());
    } else {
        logScriptError("运行时错误", source, e);
    }
}

private static void logScriptError(String type, String source, Throwable e) {
    // 截取脚本前 80 字符作为摘要
    String preview = source.length() > 80 ? source.substring(0, 80) + "..." : source;
    Logger.warn("[Script] {} | {} | {}", type, preview, e.getMessage());
}
```

**设计要点**:
- 异常不上抛，返回 `null` — 与现有 Fluxon 行为一致（脚本错误不应崩溃服务器）
- Host 异常用 `asHostException()` 取出原始 Java 异常，保留完整堆栈
- 日志包含脚本摘要，便于定位问题脚本
- `ScriptSession` 的异常处理交给调用方（States 等），保持灵活性

**状态**: ✅ 已细化

---

## 风险与应对

| 风险 | 应对 |
|-----|------|
| GraalJS 首次预热慢 (~200ms) | 插件启动时预热执行一次空脚本 |
| Context 创建开销 | Context 池化 (见问题 1) |
| 用户脚本死循环 | ResourceLimits 限制执行时间 (见问题 3) |
| Java 对象在 JS 中的类型映射 | `@HostAccess.Export` 注解或 ProxyObject 适配 |
| 依赖体积增大 (~20MB) | GraalJS 仅运行时需要，不影响编译 |
| 安全风险 | HostAccess 白名单 (见问题 4) |

---

## 删除清单

```
全部已执行完成:

依赖:
- [x] org.tabooproject.fluxon:core:1.6.1 (build.gradle.kts)

核心文件 (module/fluxon/):
- [x] FluxonScriptCache.kt    → 由 ScriptManager.java 替代
- [x] FluxonScript.kt         → 由 SingletonScript.java 替代
- [x] FluxonTrigger.kt        → 由 ScriptTrigger 替代
- [x] FluxonLoader.kt         → 由新 JS 初始化逻辑替代
- [x] FluxonExts.kt           → 参数解析迁移到 ScriptArgs.java 后删除

扩展模块 (module/fluxon/*/):
- [x] 22 个扩展目录           → 已由 module/script/functions/*.java 替代

已迁移:
- [x] fluxon/finder/TargetFinder.kt    → 移至 module/script/
- [x] fluxon/mythicmobs/MythicObject.kt → 移至 module/script/

外部引用清理 (21 个文件):
- [x] 替换 FluxonScriptOptions → ScriptOptions (13 处)
- [x] 替换 SingletonFluxonScript → SingletonScript (8 处)
- [x] 替换 FluxonScriptCache → ScriptManager (3 处)
- [x] 替换 FluxonScript → 对应新接口 (2 处)
- [x] 替换 org.tabooproject.fluxon.parser.ParsedScript 引用 (3 处)

额外清理:
- [x] 删除 libs/FluxonPlugin-1.0.2-beta-2.jar
- [x] 配置文件中残留 Fluxon 语法迁移为合法 JS
```

---

## 总结

本次迁移的核心价值：
- **性能提升**: GraalJS JIT 编译，热执行微秒级
- **标准语法**: JavaScript ES2023+，用户无需学习自研语法
- **生态优势**: 可复用 JS 生态的工具和库
- **维护成本降低**: 不再依赖小众脚本引擎
