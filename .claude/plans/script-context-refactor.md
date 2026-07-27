# ScriptContext 重构方案

## 问题

`ScriptContext` 使用 `ThreadLocal<Map>` 存储上下文，存在两个致命问题：

1. **嵌套调用互相覆盖**：`ScriptManager.eval()` 在变量求值时会覆盖并清除外层上下文（已通过 save/restore 临时修复）
2. **多玩家并发不安全**：ForkJoinPool 复用线程时，两个玩家的技能执行可能在同一线程上交错，ThreadLocal 被覆盖

## 方案：ThreadLocal + Deque 栈

将 `ThreadLocal<Map>` 改为 `ThreadLocal<Deque<Map>>`，每次进入脚本执行 push，退出时 pop。

### 为什么选这个方案

| 方案 | 改动范围 | 并发安全 | 嵌套安全 | 复杂度 |
|------|---------|---------|---------|--------|
| 参数透传 | 79个函数 + JsFunction接口 | ✅ | ✅ | 极高 |
| GraalJS bindings 注入 | 79个函数 | ✅ | ✅ | 高 |
| **Deque 栈** | **ScriptContext + 2个调用点** | **✅** | **✅** | **低** |
| 保持现状 (save/restore) | ScriptManager.eval | ⚠️ 半修复 | ✅ | 最低 |

栈方案只改 ScriptContext 类本身 + 2 个调用点（`ImmutableSkill.execute()` 和 `ScriptManager.eval()`），所有 79 个 JsFunction 零改动。

### 核心设计

```java
public final class ScriptContext {
    // 从 ThreadLocal<Map> 改为 ThreadLocal<Deque<Map>>
    private static final ThreadLocal<Deque<Map<String, Object>>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Map<String, Object>>> VAR_CACHE_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    /** 压栈：进入脚本执行时调用 */
    public static void push(Map<String, Object> variables) {
        STACK.get().push(variables);
        VAR_CACHE_STACK.get().push(new HashMap<>());
    }

    /** 弹栈：脚本执行完毕时调用 */
    public static void pop() {
        Deque<Map<String, Object>> stack = STACK.get();
        if (!stack.isEmpty()) stack.pop();
        Deque<Map<String, Object>> cacheStack = VAR_CACHE_STACK.get();
        if (!cacheStack.isEmpty()) cacheStack.pop();
    }

    /** 获取当前栈顶上下文 */
    public static Map<String, Object> getCurrent() {
        Deque<Map<String, Object>> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    /** 获取 sender */
    public static Object getSender() {
        Map<String, Object> ctx = getCurrent();
        return ctx != null ? ctx.get("sender") : null;
    }

    /** 获取/设置变量缓存（操作栈顶） */
    public static Object getVarCache(String key) {
        Deque<Map<String, Object>> stack = VAR_CACHE_STACK.get();
        if (stack.isEmpty()) return null;
        return stack.peek().get(key);
    }
    public static void putVarCache(String key, Object value) {
        Deque<Map<String, Object>> stack = VAR_CACHE_STACK.get();
        if (!stack.isEmpty()) stack.peek().put(key, value);
    }
    public static boolean hasVarCache(String key) {
        Deque<Map<String, Object>> stack = VAR_CACHE_STACK.get();
        return !stack.isEmpty() && stack.peek().containsKey(key);
    }

    /** 清空整个栈（插件卸载时用） */
    public static void clear() {
        STACK.remove();
        VAR_CACHE_STACK.remove();
    }
}
```

### 调用点改动

**ImmutableSkill.execute()**:
```kotlin
// 旧: ScriptContext.setCurrent(vars)  ... finally { ScriptContext.clear() }
// 新: ScriptContext.push(vars)        ... finally { ScriptContext.pop() }
```

**ScriptManager.eval()**:
```java
// 旧: ScriptContext.setCurrent(variables) ... finally { restore/clear }
// 新: ScriptContext.push(variables)       ... finally { ScriptContext.pop() }
```

### 执行流程示例（多玩家并发）

```
线程 A (玩家1 shoulder_bash)          线程 B (玩家2 fireball)
─────────────────────────────        ─────────────────────────
push({sender=Player1, ...})          push({sender=Player2, ...})
  stack: [Player1]                     stack: [Player2]
  tell("肩撞") → getSender=Player1     tell("火球") → getSender=Player2
  getVar("power")
    → ScriptManager.eval()
    → push({level=2, ...})
      stack: [Player1, level=2]
    → eval("10 * level + 40")
    → pop()
      stack: [Player1]
  finder() → getSender=Player1 ✓     finder() → getSender=Player2 ✓
pop()
  stack: []
```

两个线程互不干扰，嵌套调用也不会覆盖外层上下文。

### 变更清单

| 文件 | 改动 |
|------|------|
| `ScriptContext.java` | 重写：setCurrent/getCurrent/clear → push/pop/peek |
| `ImmutableSkill.kt` | setCurrent → push, clear → pop |
| `ScriptManager.java` | setCurrent → push, save/restore → pop |

总计 3 个文件，~20 行改动。79 个 JsFunction 零改动。
