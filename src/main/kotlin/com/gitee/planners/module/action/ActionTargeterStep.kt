package com.gitee.planners.module.action

/**
 * 一条已编译的目标器步骤。
 *
 * @property targeter 目标器实现。
 * @property arguments 目标器参数。
 * @property path 配置路径。
 */
class ActionTargeterStep(
    val targeter: ActionTargeter,
    val arguments: ActionArguments,
    val path: String,
) {

    /**
     * 在输入容器上执行本步骤。
     *
     * @param scope 查询上下文。
     * @param input 上游容器。
     * @return 处理后的容器。
     */
    fun apply(scope: com.gitee.planners.module.action.query.QueryScope, input: List<ActionTarget>): List<ActionTarget> {
        return targeter.resolve(scope, arguments, input)
    }

    /**
     * 返回步骤的稳定描述，用于诊断输出。
     *
     * @return 形如 `@sector{...}` 的文本。
     */
    fun describe(): String {
        val builder = StringBuilder()
        builder.append("@")
        builder.append(targeter.id)
        val values = arguments.all()
        if (values.isNotEmpty()) {
            builder.append("{")
            var first = true
            for (key in values.keys) {
                if (!first) {
                    builder.append("; ")
                }
                builder.append(key)
                first = false
            }
            builder.append("}")
        }
        return builder.toString()
    }
}
