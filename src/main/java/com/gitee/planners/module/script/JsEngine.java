package com.gitee.planners.module.script;

import java.util.Map;

/**
 * JS 引擎抽象接口
 * <p>
 * 实现:
 * <ul>
 *   <li>NashornEngine — Java 8~14 (内置 Nashorn)</li>
 *   <li>GraalJsEngine — Java 17+ (GraalJS, 后续实现)</li>
 * </ul>
 */
public interface JsEngine {

    /**
     * 引擎名称
     */
    String name();

    /**
     * 执行脚本
     *
     * @param source    脚本源码
     * @param variables 上下文变量
     * @return 执行结果
     */
    Object eval(String source, Map<String, Object> variables);

    /**
     * 打开会话 (状态回调等跨调用场景)
     *
     * @param variables 初始上下文变量
     * @return 会话实例，调用方负责关闭
     */
    JsSession openSession(Map<String, Object> variables);

    /**
     * 注册全局函数 (脚本中可直接调用)
     */
    void registerFunction(String name, JsFunction function);

    /**
     * 关闭引擎，释放资源
     */
    void close();
}
