package com.gitee.planners.core.player

import com.gitee.planners.core.condition.ConditionEvaluator

/**
 * 技能树快照的玩家运行时投影。
 *
 * 静态节点定义由调用方缓存，本对象只携带随玩家状态变化的等级、可激活状态与提示。
 *
 * @property trees 按职业技能树配置顺序排列的运行时树数据。
 */
class SkillTreeRuntimeProjection(
    val trees: List<Tree>,
    val profiling: Profiling
) {

    /**
     * 单棵技能树的玩家运行时数据。
     *
     * @property levels 按技能树节点配置顺序排列的当前等级。
     * @property canAdvanceStates 按技能树节点配置顺序排列的可激活状态。
     * @property hints 按技能树节点配置顺序排列的激活提示。
     */
    class Tree(val levels: IntArray, val canAdvanceStates: BooleanArray, val hints: List<List<String>>) {
    }

    /**
     * 单次技能树运行时投影的分段统计。
     *
     * 所有字段均以纳秒累加，日志输出时统一转换为微秒，避免 JS 毫秒时钟污染采样结果。
     */
    class Profiling {

        var nodeStateReadNanos: Long = 0L
        var graphCheckNanos: Long = 0L
        var requestBuildNanos: Long = 0L
        var conditionVerifyNanos: Long = 0L
        var resultApplyNanos: Long = 0L
        var treeBuildNanos: Long = 0L
        var totalNanos: Long = 0L
        var conditionProfiling: ConditionEvaluator.BatchProfiling? = null

        /** 将完整投影统计压缩为服务器日志中的一段文本。 */
        fun toLogText(): String {
            val text = StringBuilder()
            text.append("skillTreeUs={total=")
            text.append(formatMicros(totalNanos))
            text.append(",nodeState=")
            text.append(formatMicros(nodeStateReadNanos))
            text.append(",graph=")
            text.append(formatMicros(graphCheckNanos))
            text.append(",request=")
            text.append(formatMicros(requestBuildNanos))
            text.append(",condition=")
            text.append(formatMicros(conditionVerifyNanos))
            text.append(",apply=")
            text.append(formatMicros(resultApplyNanos))
            text.append(",treeBuild=")
            text.append(formatMicros(treeBuildNanos))
            text.append("}")
            val profiling = conditionProfiling
            if (profiling != null) {
                text.append(" ")
                text.append(profiling.toLogText())
            }
            return text.toString()
        }

        private fun formatMicros(nanos: Long): String {
            return (nanos / 1_000L).toString()
        }
    }
}
