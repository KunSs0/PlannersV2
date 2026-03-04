package com.gitee.planners.module.script;

/**
 * JS 执行会话
 * <p>
 * 持有独立的脚本上下文，支持跨调用保持状态。
 * 用于状态回调等需要在同一上下文中多次执行的场景。
 */
public interface JsSession extends AutoCloseable {

    /**
     * 在此会话上下文中执行脚本
     */
    Object eval(String source);

    /**
     * 检查会话中是否定义了指定函数
     */
    boolean hasFunction(String name);

    /**
     * 调用会话中定义的函数
     *
     * @param name 函数名
     * @param args 参数
     * @return 返回值
     */
    Object invokeFunction(String name, Object... args);

    @Override
    void close();
}
