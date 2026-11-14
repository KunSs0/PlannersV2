package com.gitee.planners.api.directing

import org.bukkit.entity.Entity

/**
 * 指向候选过滤器。
 *
 * 由 Planners 按技能 `__option__.directing.filter` 配置编译，并在指向会话内对每个
 * 客户端候选实体执行一次。预览更新与服务端最终确认共用同一实例，因此“框亮了却确认被拒”
 * 不会出现；provider 只负责在几何与实体有效性校验后调用它，不解释过滤规则本身。
 */
interface DirectingCandidateFilter {

    /**
     * 判断候选是否可作为本次指向目标。
     *
     * @param entity 客户端上报并由服务端解析出的候选实体。
     * @return 允许作为本次目标时返回 true。
     */
    fun accept(entity: Entity): Boolean
}
