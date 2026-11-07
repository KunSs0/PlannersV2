package com.gitee.planners.module.action

import org.bukkit.entity.LivingEntity

/**
 * 阵营判定入口。
 *
 * `@enemy` / `@ally` 需要知道「两个实体是否互为友方」，该规则由宿主插件提供：
 * 默认实现只把实体自身视为友方，安装后按安装方的策略判定。
 *
 * 之所以做成可安装而不是直接调用某个插件的类，是为了让 Planners 不反向依赖
 * 战斗插件；安装方在自身启用阶段调用 [install] 即可。
 */
object ActionFactionPolicy {

    /**
     * 阵营判定函数：返回 true 表示两者互为友方。
     */
    fun interface Policy {

        /**
         * 判断两者是否互为友方。
         *
         * @param source 判定发起方。
         * @param target 被判定方。
         * @return 互为友方时返回 true。
         */
        fun isFriendly(source: LivingEntity, target: LivingEntity): Boolean
    }

    private val defaultPolicy = Policy { source, target ->
        source.uniqueId == target.uniqueId
    }

    @Volatile
    private var policy: Policy = defaultPolicy

    /**
     * 安装阵营判定策略。
     *
     * @param value 策略实现。
     */
    fun install(value: Policy) {
        policy = value
    }

    /**
     * 恢复默认策略。
     */
    fun reset() {
        policy = defaultPolicy
    }

    /**
     * 判断两者是否互为友方。
     *
     * @param source 判定发起方。
     * @param target 被判定方。
     * @return 互为友方时返回 true。
     */
    fun isFriendly(source: LivingEntity, target: LivingEntity): Boolean {
        return policy.isFriendly(source, target)
    }
}
