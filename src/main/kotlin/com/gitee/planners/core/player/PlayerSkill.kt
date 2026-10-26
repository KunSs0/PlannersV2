package com.gitee.planners.core.player

import com.gitee.planners.api.Registries
import com.gitee.planners.api.common.Unique
import com.gitee.planners.api.job.Variable
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.database.ProfilePersistence
import com.gitee.planners.core.database.RecordIdentity

/**
 * 玩家在线技能及装备状态，字段先在业务线程提交，再异步保存不可变快照。
 * @property index 稳定运行时身份，允许尚未持久化的负身份。
 * @param skillId 技能配置标识。
 * @param level 初始基础等级。
 * @property equipped 是否装备。
 * @property backpackPage 装备页面。
 * @property backpackSlot 装备槽位。
 */
class PlayerSkill(
    var index: Long,
    private val skillId: String,
    level: Int,
    var equipped: Boolean = false,
    var backpackPage: String? = null,
    var backpackSlot: String? = null
) : Unique {

    /** 运行时索引不随后台 INSERT 变化，数据库主键独立记录。 */
    val persistenceId = RecordIdentity(index)

    val name: String
        get() = immutable.name

    val immutable: ImmutableSkill
        get() = Registries.SKILL.getOrNull(skillId) ?: error("Couldn't find skill with id $skillId'")

    val hooks: Map<String, ImmutableSkill.Hook>
        get() = immutable.hooks

    var level = level
        set(value) {
            field = value
            // 先完成在线等级变更，再将固定字段交给后台。
            ProfilePersistence.saveSkill(this)
        }

    override val id: String
        get() = immutable.id

    fun getVariables(): Map<String, Variable> {
        return immutable.getVariables()
    }

    fun getVariableOrNull(id: String): Variable? {
        return immutable.getVariableOrNull(id)
    }

}
