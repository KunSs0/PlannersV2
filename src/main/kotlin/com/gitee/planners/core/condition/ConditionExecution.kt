package com.gitee.planners.core.condition

import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player

/** 一次请求准备完成的条件执行器；外部异步数据不得在执行阶段重新读取。 */
interface ConditionExecution {
    /** 使用已准备数据判断单个条件，不执行消耗。 */
    fun evaluate(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): Boolean

    /** 批量判断同一模板的输入，每个输入对应结果中的一个字符。 */
    fun evaluateBatchPersistent(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionInputs: List<Map<String, Any>>): BatchEvaluation

    /** 准备可回滚成本，不修改资源；只读条件返回 null。 */
    fun prepareConsume(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): LearningCost?

    /** 一批条件的确定性执行结果，字符串顺序对应输入的通过标记。 */
    class BatchEvaluation(val encoded: String)
}
