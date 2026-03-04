package com.gitee.planners.module.script;

/**
 * JS 全局函数接口
 */
@FunctionalInterface
public interface JsFunction {

    /**
     * 执行函数
     *
     * @param args JS 传入的参数数组
     * @return 返回值 (null 表示 void)
     */
    Object invoke(Object[] args);
}
