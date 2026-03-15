package com.gitee.planners.module.script.graaljs;

import com.gitee.planners.module.script.JsSession;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * GraalJS 会话实现
 */
public class GraalJsSession implements JsSession {

    private final Context context;
    private boolean closed = false;

    GraalJsSession(Context context) {
        this.context = context;
    }

    @Override
    public Object eval(String source) {
        checkClosed();
        Value result = context.eval("js", source);
        return GraalJsEngine.unwrap(result);
    }

    @Override
    public boolean hasFunction(String name) {
        checkClosed();
        Value member = context.getBindings("js").getMember(name);
        return member != null && member.canExecute();
    }

    @Override
    public Object invokeFunction(String name, Object... args) {
        checkClosed();
        Value fn = context.getBindings("js").getMember(name);
        if (fn == null || !fn.canExecute()) return null;
        Value result = fn.execute(args);
        return GraalJsEngine.unwrap(result);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            context.close();
        }
    }

    private void checkClosed() {
        if (closed) throw new IllegalStateException("Session 已关闭");
    }
}
