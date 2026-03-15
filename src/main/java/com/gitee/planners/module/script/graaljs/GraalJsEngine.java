package com.gitee.planners.module.script.graaljs;

import com.gitee.planners.module.script.JsEngine;
import com.gitee.planners.module.script.JsFunction;
import com.gitee.planners.module.script.JsSession;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GraalJS 引擎实现 (Java 17+)
 */
public class GraalJsEngine implements JsEngine {

    private final Map<String, JsFunction> globalFunctions = new ConcurrentHashMap<>();
    private final Map<String, Source> sourceCache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "GraalJS";
    }

    @Override
    public Object eval(String source, Map<String, Object> variables) {
        try (Context ctx = newContext()) {
            injectAll(ctx, variables);
            Value result = ctx.eval(getOrCache(source));
            return unwrap(result);
        }
    }

    @Override
    public JsSession openSession(Map<String, Object> variables) {
        Context ctx = newContext();
        injectAll(ctx, variables);
        return new GraalJsSession(ctx);
    }

    @Override
    public void registerFunction(String name, JsFunction function) {
        globalFunctions.put(name, function);
    }

    @Override
    public void close() {
        globalFunctions.clear();
        sourceCache.clear();
    }

    private Context newContext() {
        return Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(s -> true)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    private void injectAll(Context ctx, Map<String, Object> variables) {
        Value bindings = ctx.getBindings("js");
        // 注入全局函数
        for (Map.Entry<String, JsFunction> entry : globalFunctions.entrySet()) {
            bindings.putMember(entry.getKey(), new FunctionProxy(entry.getValue()));
        }
        // 注入上下文变量
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                bindings.putMember(entry.getKey(), entry.getValue());
            }
        }
    }

    private Source getOrCache(String source) {
        return sourceCache.computeIfAbsent(source, s ->
                Source.newBuilder("js", s, "<eval>").buildLiteral()
        );
    }

    static Object unwrap(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) {
            if (value.fitsInInt()) return value.asInt();
            if (value.fitsInLong()) return value.asLong();
            return value.asDouble();
        }
        if (value.isString()) return value.asString();
        if (value.isHostObject()) return value.asHostObject();
        return value.as(Object.class);
    }

    /**
     * 将 JsFunction 包装为 GraalJS 可调用的函数代理
     */
    public static class FunctionProxy implements org.graalvm.polyglot.proxy.ProxyExecutable {

        private final JsFunction function;

        public FunctionProxy(JsFunction function) {
            this.function = function;
        }

        @Override
        public Object execute(Value... arguments) {
            Object[] args = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                args[i] = unwrap(arguments[i]);
            }
            return function.invoke(args);
        }
    }
}
