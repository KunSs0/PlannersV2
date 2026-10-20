package com.gitee.planners.core.condition

/**
 * 技能点成本声明，由学习存储事务扣除，句柄本身不修改档案。
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

    /** 技能点与节点由数据库统一提交，不在资源阶段修改。 */
    override fun commit() {
        // 无外部资源需要扣除。
    }

    /** 数据库负责技能点回滚，内存直到成功后才发布。 */
    override fun rollback() {
        // 无外部资源需要恢复。
    }
}
