package com.gitee.planners.core.player

import com.gitee.planners.core.database.RecordIdentity

/**
 * 单个真实技能树节点的持久状态，不包含虚拟选项。
 *
 * @property index 稳定运行时身份，新建节点可为负数，数据库主键由独立句柄保存。
 * @property treeId 所属技能树。
 * @property nodeId 实际节点 ID。
 * @property level 已学习等级。
 */
class PlayerSkillTreeNodeState(
    val index: Long,
    val treeId: String,
    val nodeId: String,
    var level: Int
) {
    /** 后台持久化身份，与稳定的内存节点索引分离。 */
    val persistenceId = RecordIdentity(index)
}
