package com.gitee.planners.core.condition

/**
 * 一次学习请求的外部成本预留；准备阶段不得修改资源，所有方法由主线程调用。
 * commit 部分失败也必须允许 rollback，且两个方法均须支持重复调用。
 */
interface LearningCost {
    /** 返回交由节点数据库事务统一扣除的技能点数量；外部资源返回零。 */
    fun skillPoints(): Int

    /** 提交已准备资源；不得异步调度实际扣除。 */
    fun commit()

    /** 恢复本句柄已经修改的资源；失败必须抛出异常，禁止覆盖无关资源。 */
    fun rollback()
}
