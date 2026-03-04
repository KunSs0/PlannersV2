# 扩展函数迁移实施文档

> 日期: 2026-03-04
> 范围: 22 个 Fluxon 扩展模块 → GlobalFunctions.register()

## 核心变化

### 1. 注册方式

```kotlin
// 旧: Fluxon — 每个重载单独注册
runtime.registerFunction("damage", returns(Type.VOID).params(Type.D)) { ctx ->
    val amount = ctx.getAsDouble(0)
    val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
    applyDamage(amount, null, DamageCause.of("SKILL"), targets)
    null
}
runtime.registerFunction("damage", returns(Type.VOID).params(Type.D, Type.OBJECT)) { ctx ->
    val amount = ctx.getAsDouble(0)
    val targets = ctx.getTargetsArg(1, LeastType.SENDER)
    applyDamage(amount, null, DamageCause.of("SKILL"), targets)
    null
}
```

```java
// 新: JS — 单函数 + args.length 分派
GlobalFunctions.register("damage", args -> {
    double amount = ScriptArgs.getDouble(args, 0);
    ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
    applyDamage(amount, null, DamageCause.of("SKILL"), targets);
    return null;
});
```

**要点**: Fluxon 的多个同名重载合并为一个 JS 函数，通过 `args.length` 和类型判断分派。

### 2. 参数解析

| Fluxon (FunctionContext) | JS (Object[]) | ScriptArgs 工具方法 |
|--------------------------|---------------|-------------------|
| `ctx.getString(0)` | `(String) args[0]` | `ScriptArgs.getString(args, 0)` |
| `ctx.getAsDouble(0)` | `((Number) args[0]).doubleValue()` | `ScriptArgs.getDouble(args, 0)` |
| `ctx.getAsInt(0)` | `((Number) args[0]).intValue()` | `ScriptArgs.getInt(args, 0)` |
| `ctx.getAsLong(0)` | `((Number) args[0]).longValue()` | `ScriptArgs.getLong(args, 0)` |
| `ctx.getRef(0)` | `args[0]` | `ScriptArgs.get(args, 0)` |
| `ctx.getAsBoolean(0)` | `(Boolean) args[0]` | `ScriptArgs.getBoolean(args, 0)` |
| `ctx.getTargetsArg(1)` | — | `ScriptArgs.getTargets(args, 1, LeastType.SENDER)` |
| `ctx.getPlayerArg(0)` | — | `ScriptArgs.getPlayer(args, 0)` |

### 3. 隐式 sender 获取

Fluxon 通过 `ctx.environment.rootVariables["sender"]` 获取。
JS 中 sender 是全局变量，直接在脚本中可用，但 `JsFunction` 内部无法访问 bindings。

**方案**: `ScriptArgs.getTargets()` 需要 sender 回退时，由调用方显式传入，或使用 `ScriptContext` 线程变量：

```java
// 方案 A: 函数内通过 ThreadLocal 获取当前执行上下文
public final class ScriptContext {
    private static final ThreadLocal<Map<String, Object>> CURRENT = new ThreadLocal<>();

    public static void setCurrent(Map<String, Object> variables) { CURRENT.set(variables); }
    public static Map<String, Object> getCurrent() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }

    public static Object getSender() {
        Map<String, Object> ctx = CURRENT.get();
        return ctx != null ? ctx.get("sender") : null;
    }
}
```

ScriptManager.eval() 在执行前设置 `ScriptContext.setCurrent(variables)`，执行后 clear。
全局函数内部通过 `ScriptContext.getSender()` 获取当前 sender。

### 4. registerExtension 处理

`registerExtension` 注册的是类方法扩展（Context、PlayerTemplate、SkillContext）。
JS 中这些对象直接通过 bindings 注入，方法天然可调用：

```javascript
// 旧 Fluxon: profile :: level()
// 新 JS:
profile.getLevel()     // 直接调用 Java 方法
ctx.getSender()        // 直接调用 Java 方法
```

**不需要迁移为全局函数**。只需确保注入对象的相关方法是 `public` 的。

### 5. @Export 链式方法 (TargetFinder)

TargetFinder 使用 `@Export` 注解标记链式方法。
JS 中 Java 对象方法天然可链式调用：

```javascript
// 旧 Fluxon: finder()::range(5)::type("ZOMBIE")::build()
// 新 JS:
finder().range(5).type("ZOMBIE").build()
```

**只需迁移 `finder()` 全局函数**，链式方法无需处理。

---

## ScriptArgs 工具类设计

```java
// src/main/java/com/gitee/planners/module/script/ScriptArgs.java
public final class ScriptArgs {

    public static Object get(Object[] args, int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public static String getString(Object[] args, int index) {
        Object v = get(args, index);
        return v != null ? v.toString() : null;
    }

    public static double getDouble(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    public static int getInt(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    public static long getLong(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    public static boolean getBoolean(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Boolean && (Boolean) v;
    }

    public static float getFloat(Object[] args, int index) {
        Object v = get(args, index);
        return v instanceof Number ? ((Number) v).floatValue() : 0f;
    }

    /** 解析目标参数，为空时按 leastType 回退到 sender */
    public static ProxyTargetContainer getTargets(Object[] args, int index, LeastType leastType) {
        Object arg = get(args, index);
        ProxyTargetContainer targets = resolveTargets(arg);
        if (targets.isEmpty()) {
            Object sender = ScriptContext.getSender();
            return leastType.getTargetContainer(sender);
        }
        return targets;
    }

    public static Player getPlayer(Object[] args, int index) {
        Object v = get(args, index);
        if (v instanceof Player) return (Player) v;
        if (v == null) {
            Object sender = ScriptContext.getSender();
            return sender instanceof Player ? (Player) sender : null;
        }
        return null;
    }

    public static LivingEntity resolveLivingEntity(Object arg) { /* 同 FluxonExts */ }
    public static ProxyTargetContainer resolveTargets(Object arg) { /* 同 FluxonExts */ }
    public static Skill resolveSkill(Object arg) { /* 同 FluxonExts */ }
}
```

---

## 模块迁移模板

### 模板: 简单函数 (无重载合并)

```java
// 旧
runtime.registerFunction("tell", returns(Type.VOID).params(Type.STRING)) { ctx ->
    val message = ctx.getString(0) ?: return@registerFunction
    val targets = ctx.getTargetsArg(-1, LeastType.SENDER)
    targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { it.sendMessage(message) }
}
runtime.registerFunction("tell", returns(Type.VOID).params(Type.STRING, Type.OBJECT)) { ctx ->
    val message = ctx.getString(0) ?: return@registerFunction
    val targets = ctx.getTargetsArg(1, LeastType.SENDER)
    targets.filterIsInstance<ProxyTarget.CommandSender<*>>().forEach { it.sendMessage(message) }
}

// 新 — 合并为一个函数
GlobalFunctions.register("tell", args -> {
    String message = ScriptArgs.getString(args, 0);
    if (message == null) return null;
    // tell(msg) 或 tell(msg, targets) — 统一处理
    ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
    for (ProxyTarget<?> t : targets) {
        if (t instanceof ProxyTarget.CommandSender) {
            ((ProxyTarget.CommandSender<?>) t).sendMessage(message);
        }
    }
    return null;
});
```

### 模板: 多变体函数 (damage 系列)

```java
// damage(amount)              → damage(50)
// damage(amount, targets)     → damage(50, finder().build())
// damageBy(amount, source)    → damageBy(50, sender)
// damageBy(amount, src, tgts) → damageBy(50, sender, targets)
// damageEx(amount, cause)     → damageEx(50, "FIRE")
// ...

// 不同函数名的变体保持独立注册，同名重载合并:
GlobalFunctions.register("damage", args -> {
    double amount = ScriptArgs.getDouble(args, 0);
    ProxyTargetContainer targets = ScriptArgs.getTargets(args, 1, LeastType.SENDER);
    applyDamage(amount, null, DamageCause.of("SKILL"), targets);
    return null;
});

GlobalFunctions.register("damageBy", args -> {
    double amount = ScriptArgs.getDouble(args, 0);
    LivingEntity source = ScriptArgs.resolveLivingEntity(ScriptArgs.get(args, 1));
    ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
    applyDamage(amount, source, DamageCause.of("SKILL"), targets);
    return null;
});

GlobalFunctions.register("damageEx", args -> {
    double amount = ScriptArgs.getDouble(args, 0);
    String cause = ScriptArgs.getString(args, 1);
    ProxyTargetContainer targets = ScriptArgs.getTargets(args, 2, LeastType.SENDER);
    applyDamage(amount, null, DamageCause.of(cause), targets);
    return null;
});

GlobalFunctions.register("damageExBy", args -> {
    double amount = ScriptArgs.getDouble(args, 0);
    String cause = ScriptArgs.getString(args, 1);
    LivingEntity source = ScriptArgs.resolveLivingEntity(ScriptArgs.get(args, 2));
    ProxyTargetContainer targets = ScriptArgs.getTargets(args, 3, LeastType.SENDER);
    applyDamage(amount, source, DamageCause.of(cause), targets);
    return null;
});
```

### 模板: 链式对象 (TargetFinder)

```java
// 只注册入口函数，链式方法由 Java 对象提供
GlobalFunctions.register("finder", args -> {
    Object origin = ScriptArgs.get(args, 0);
    if (origin == null) origin = ScriptContext.getSender();
    return new TargetFinder(origin);
});
// JS 中: finder().range(5).type("ZOMBIE").limit(3).build()
```

---

## 迁移清单

### Phase 2a: 高优先级 (8 模块)

| # | 模块 | 函数名 | 合并后数量 | 说明 |
|---|------|--------|-----------|------|
| 1 | Common | tell | 1 | 直接合并 |
| 2 | Command | command, commandOp, commandConsole | 3 | 直接合并 |
| 3 | Metadata | hasMeta, getMeta, setMeta, setMetaTimeout, removeMeta | 5 | 无重载 |
| 4 | Cooldown | getCooldown, setCooldown, resetCooldown, hasCooldown | 4 | 合并重载 |
| 5 | Profile | — | 0 | registerExtension，无需迁移 |
| 6 | Context | — | 0 | registerExtension，无需迁移 |
| 7 | SkillCommands | damage, damageBy, damageEx, damageExBy, heal | 5 | 合并重载 |
| 8 | Health | healthAdd, healthSet, healthTake | 3 | 合并重载 |

### Phase 2b: 中优先级 (9 模块)

| # | 模块 | 函数名 | 合并后数量 | 说明 |
|---|------|--------|-----------|------|
| 9 | Effect | freeze, fire, explosion | 3 | 合并重载 |
| 10 | Entity | entitySpawn, entityRemove, entityTeleport, entityTeleportTo, entitySetAI, entitySetGravity, entitySetInvulnerable, entitySetGlowing, entitySetSilent | 9 | 合并重载 |
| 11 | State | stateAttach, stateDetach, stateRemove, stateHas | 4 | 合并重载 + 额外参数 |
| 12 | Velocity | velocitySet, velocityAdd, velocityMove, velocityZero, getVelocity | 5 | 合并重载 |
| 13 | Potion | potion, potionRemove | 2 | 合并重载 |
| 14 | Sound | sound, soundResource | 2 | 合并重载 |
| 15 | Projectile | projectile, projectileAt, projectileToward | 3 | 合并重载 |
| 16 | TargetFinder | finder | 1 | 仅入口函数，链式方法由对象提供 |
| 17 | SkillSystem | apAttack | 1 | 合并重载 |

### Phase 2c: 低优先级 (5 模块)

| # | 模块 | 函数名 | 合并后数量 | 说明 |
|---|------|--------|-----------|------|
| 18 | Economy | getBalance, takeMoney, giveMoney, setMoney | 4 | 合并重载 |
| 19 | MythicMobs | mythic | 1 | 入口函数，方法由对象提供 |
| 20 | DragonCore | dcParticle, dcSound, dcAnimation, dcAnimationRemove, dcPlayerAnimation, dcPlayerAnimationRemove, dcSync, dcSyncDelete, dcEntityFunction | 9 | 合并重载 |
| 21 | AttributePlus | apAttack | 1 | 合并重载 |
| 22 | GermPlugin | germEffect, germEffectRemove, germEffectClear, germSound, germAnimation, germAnimationStop, germViewLock, germViewUnlock, germLookLock, germLookUnlock, germMoveLock, germMoveUnlock, germCooldown | 13 | 合并重载 |

**合并后总计: ~79 个全局函数** (原 289 个注册调用 / ~115 个函数名)

---

## 前置依赖

实施前需先完成:
1. ✅ `GlobalFunctions.java` — 函数注册表
2. ⬜ `ScriptArgs.java` — 参数解析工具类
3. ⬜ `ScriptContext.java` — 线程变量 (ThreadLocal)，提供 sender 等上下文访问

---

## 脚本语法变化速查

```javascript
// 旧 Fluxon                        →  新 JavaScript
tell("hello")                        →  tell("hello")           // 不变
damage(50)                           →  damage(50)              // 不变
sender :: tell("hi")                 →  tell("hi")              // 方法调用变全局函数
profile :: level()                   →  profile.getLevel()      // 对象方法直接调用
finder()::range(5)::build()          →  finder().range(5).build() // 链式调用
if hasCooldown(skill) { }            →  if (hasCooldown(skill)) { } // 加括号
stateAttach("燃烧", 40)              →  stateAttach("燃烧", 40) // 不变
```
