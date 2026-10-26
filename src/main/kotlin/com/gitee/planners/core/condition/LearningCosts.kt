package com.gitee.planners.core.condition

/**
 * 同一次学习的全部资源句柄，保留准备顺序并在失败时逆序恢复。
 * @property costs 已准备且尚未扣费的成本。
 */
class LearningCosts(private val costs: List<LearningCost>) {
    /** 合计技能点，拒绝负值和溢出；此步骤不修改任何资源。 */
    fun skillPoints(): Int {
        var total = 0
        for (cost in costs) {
            val amount = cost.skillPoints()
            require(amount >= 0) { "技能点成本不能为负数" }
            total = Math.addExact(total, amount)
        }
        return total
    }

    /** 主线程按准备顺序扣除全部外部资源；仅扣费自身失败时恢复，不包含持久化。 */
    fun commit() {
        try {
            for (cost in costs) {
                cost.commit()
            }
        } catch (failure: Throwable) {
            // 外部资源的同步提交失败属于业务失败，数据库失败不走此入口。
            rollback(failure)
            throw failure
        }
    }

    /**
     * 恢复所有已准备句柄，同时保留原始失败及每个回滚失败。
     * @param failure 引发本次失败的原始异常，回滚异常附加到 suppressed。
     */
    fun rollback(failure: Throwable) {
        for (index in costs.indices.reversed()) {
            try {
                costs[index].rollback()
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }
        }
    }

}
