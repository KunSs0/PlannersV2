package com.gitee.planners.module.action

/**
 * 一个技能编译后的 Action 结构。
 *
 * @property skillId 所属技能 ID。
 * @property flows 全部命名流程，按声明顺序。
 */
class ActionSpec(
    val skillId: String,
    val flows: Map<String, ActionFlow>,
) {

    /**
     * 按流程名查找。
     *
     * @param id 流程名。
     * @return 流程；不存在时返回 null。
     */
    fun flow(id: String): ActionFlow? {
        return flows[id]
    }

    /**
     * 判断是否声明了指定流程。
     *
     * @param id 流程名。
     * @return 存在时返回 true。
     */
    fun hasFlow(id: String): Boolean {
        return flows.containsKey(id)
    }

    /**
     * 返回全部带触发器的动作行总数，用于启动期诊断。
     *
     * @return 触发器行数。
     */
    fun triggeredLineCount(): Int {
        var count = 0
        for (flow in flows.values) {
            for (line in flow.actions) {
                if (line.hasTrigger()) {
                    count += 1
                }
            }
        }
        return count
    }

    companion object {

        /**
         * 主动技能释放后执行的保留流程名。
         */
        const val FIRE = "fire"

        /**
         * 技能加载后执行的保留流程名。
         */
        const val SERVER = "server"
    }
}
