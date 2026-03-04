package com.gitee.planners.module.script;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 单例脚本封装
 * <p>
 * 替代 SingletonFluxonScript，封装单个脚本源码并提供执行入口。
 */
public class SingletonScript {

    private static final Pattern NESTED_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    private final String action;

    public SingletonScript(String source) {
        this.action = source != null ? source : "";
    }

    public boolean isNotNull() {
        return !action.isEmpty();
    }

    public String getAction() {
        return action;
    }

    /**
     * 执行脚本，根据 async 标志决定同步/异步
     */
    public CompletableFuture<Object> run(ScriptOptions options) {
        if (action.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        if (options.isAsync()) {
            return CompletableFuture.supplyAsync(() -> ScriptManager.eval(action, options));
        }
        return CompletableFuture.completedFuture(ScriptManager.eval(action, options));
    }

    /**
     * 同步执行
     */
    public Object eval(ScriptOptions options) {
        if (action.isEmpty()) return null;
        return ScriptManager.eval(action, options);
    }

    /**
     * 同步执行 (无上下文)
     */
    public Object eval() {
        return eval(new ScriptOptions());
    }

    /**
     * 替换文本中的嵌套脚本表达式 {{expr}}
     */
    public static String replaceNested(String text, ScriptOptions options) {
        Matcher matcher = NESTED_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String expr = matcher.group(1);
            Object result = new SingletonScript(expr).eval(options);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(result != null ? result.toString() : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
