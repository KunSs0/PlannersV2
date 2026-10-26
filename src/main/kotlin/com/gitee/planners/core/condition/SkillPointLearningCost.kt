package com.gitee.planners.core.condition

/**
 * 技能点成本声明，由主线程学习编排器扣除，句柄本身不修改档案。
 * @property amount 非负技能点数量。
 */
class SkillPointLearningCost(private val amount: Int) : LearningCost {
    init {
        require(amount >= 0) { "技能点成本不能为负数" }
    }

    /** 向学习编排器声明本次技能点成本。 */
    override fun skillPoints(): Int {
        return amount
    }

    /** 技能点与节点由主线程统一提交，不在外部资源阶段重复扣除。 */
    override fun commit() {
        // 无外部资源需要扣除。
    }

    /** 外部资源失败时技能点尚未发布，无需恢复；数据库失败不回滚内存。 */
    override fun rollback() {
        // 无外部资源需要恢复。
    }
}
