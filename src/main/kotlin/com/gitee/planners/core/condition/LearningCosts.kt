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

    /** 按准备顺序同步扣除全部外部资源，异常交由事务入口统一处理。 */
    fun commit() {
        for (cost in costs) {
            cost.commit()
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

    /**
     * 同步执行外部资源扣除和持久化；成功返回后不得因通知异常退款。
     * @param persist 在同一主线程内完成提交的存储操作。
     * @return 已提交的存储结果。
     */
    fun <T> purchase(persist: (Int, () -> Unit) -> T): T {
        try {
            return persist(skillPoints(), ::commit)
        } catch (failure: Throwable) {
            rollback(failure)
            throw failure
        }
    }
}
