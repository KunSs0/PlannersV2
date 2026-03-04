package com.gitee.planners.module.script.nashorn;

import com.gitee.planners.module.script.JsSession;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Nashorn 会话实现
 * <p>
 * 通过独立 Bindings 隔离上下文，支持跨调用保持函数定义。
 */
public class NashornSession implements JsSession {

    private final ScriptEngine engine;
    private final Bindings bindings;
    private boolean closed = false;

    NashornSession(ScriptEngine engine, Bindings bindings) {
        this.engine = engine;
        this.bindings = bindings;
    }

    @Override
    public Object eval(String source) {
        checkClosed();
        try {
            return engine.eval(source, bindings);
        } catch (ScriptException e) {
            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasFunction(String name) {
        checkClosed();
        Object member = bindings.get(name);
        if (member == null) return false;
        // Nashorn 函数类型为 ScriptObjectMirror，检查其 class name 即可
        return member.getClass().getSimpleName().equals("ScriptObjectMirror");
    }

    @Override
    public Object invokeFunction(String name, Object... args) {
        checkClosed();
        if (!hasFunction(name)) return null;
        try {
            // 构建调用表达式
            StringBuilder call = new StringBuilder(name).append("(");
            for (int i = 0; i < args.length; i++) {
                bindings.put("__arg" + i, args[i]);
                if (i > 0) call.append(",");
                call.append("__arg").append(i);
            }
            call.append(")");
            Object result = engine.eval(call.toString(), bindings);
            // 清理临时参数
            for (int i = 0; i < args.length; i++) {
                bindings.remove("__arg" + i);
            }
            return result;
        } catch (ScriptException e) {
            throw new RuntimeException("函数调用失败 [" + name + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        closed = true;
        bindings.clear();
    }

    private void checkClosed() {
        if (closed) throw new IllegalStateException("Session 已关闭");
    }
}
