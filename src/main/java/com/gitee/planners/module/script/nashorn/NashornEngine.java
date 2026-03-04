package com.gitee.planners.module.script.nashorn;

import com.gitee.planners.module.script.JsEngine;
import com.gitee.planners.module.script.JsFunction;
import com.gitee.planners.module.script.JsSession;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nashorn JS 引擎实现
 * <p>
 * 适用于 Java 8~14 (内置 Nashorn)。
 * 使用 {@link Compilable} 预编译脚本，使用独立 {@link Bindings} 隔离上下文。
 */
public class NashornEngine implements JsEngine {

    private final ScriptEngine engine;
    private final Map<String, JsFunction> globalFunctions = new ConcurrentHashMap<>();
    private final Map<String, CompiledScript> compiledCache = new ConcurrentHashMap<>();

    public NashornEngine() {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        if (this.engine == null) {
            throw new IllegalStateException("Nashorn 引擎不可用，请检查 Java 版本 (需要 8~14)");
        }
    }

    @Override
    public String name() {
        return "Nashorn";
    }

    @Override
    public Object eval(String source, Map<String, Object> variables) {
        Bindings bindings = newBindings(variables);
        try {
            CompiledScript compiled = getOrCompile(source);
            return compiled.eval(bindings);
        } catch (ScriptException e) {
            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public JsSession openSession(Map<String, Object> variables) {
        Bindings bindings = newBindings(variables);
        return new NashornSession(engine, bindings);
    }

    @Override
    public void registerFunction(String name, JsFunction function) {
        globalFunctions.put(name, function);
    }

    @Override
    public void close() {
        globalFunctions.clear();
        compiledCache.clear();
    }

    /**
     * 创建 Bindings 并注入全局函数 + 上下文变量
     */
    private Bindings newBindings(Map<String, Object> variables) {
        Bindings bindings = engine.createBindings();
        // 注入全局函数: Java JsFunction → JS callable
        for (Map.Entry<String, JsFunction> entry : globalFunctions.entrySet()) {
            String fnName = entry.getKey();
            JsFunction fn = entry.getValue();
            bindings.put("__jf_" + fnName, fn);
            try {
                engine.eval(
                        "function " + fnName + "() { " +
                                "return __jf_" + fnName + ".invoke(Java.to(arguments, 'java.lang.Object[]')); " +
                                "}",
                        bindings
                );
            } catch (ScriptException e) {
                throw new RuntimeException("注册全局函数失败 [" + fnName + "]", e);
            }
        }
        // 注入上下文变量
        if (variables != null) {
            bindings.putAll(variables);
        }
        return bindings;
    }

    /**
     * 预编译脚本并缓存
     */
    private CompiledScript getOrCompile(String source) {
        return compiledCache.computeIfAbsent(source, s -> {
            try {
                return ((Compilable) engine).compile(s);
            } catch (ScriptException e) {
                throw new RuntimeException("脚本编译失败: " + s, e);
            }
        });
    }
}
