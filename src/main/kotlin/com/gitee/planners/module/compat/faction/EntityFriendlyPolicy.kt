package com.gitee.planners.module.compat.faction

import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity

/**
 * 实体友方判定策略。
 *
 * `@enemy` / `@ally` 以及后续需要"两个实体是否同阵营"的功能统一读这里。
 * 未注册时使用 [DefaultEntityFriendlyPolicy]，判定逻辑始终由 Planners 执行，
 * 外部插件只提供策略实现，与 `AttributeDriver` 同构：
 * 唯一所有者、主线程注册、同一实例重复注册幂等、非当前所有者注销无影响。
 */
interface EntityFriendlyPolicy {

    companion object {

        @Volatile
        private var instance: EntityFriendlyPolicy = DefaultEntityFriendlyPolicy

        /**
         * 注册策略，未注册时由默认策略兜底。
         *
         * @param policy 已启用的策略实现。
         */
        @Synchronized
        fun register(policy: EntityFriendlyPolicy) {
            check(Bukkit.isPrimaryThread()) { "实体友方策略注册必须在 Bukkit 主线程执行" }
            check(policy.checkEnable()) { "实体友方策略未启用: ${policy.javaClass.name}" }
            val previous = instance
            check(previous === DefaultEntityFriendlyPolicy || previous === policy) {
                "实体友方策略已注册: ${previous.javaClass.name}，不能覆盖为 ${policy.javaClass.name}"
            }
            if (previous === policy) {
                return
            }
            instance = policy
        }

        /**
         * 仅允许当前策略注销；注销后回到默认策略。
         *
         * @param policy 需要注销的所有者实例。
         */
        @Synchronized
        fun unregister(policy: EntityFriendlyPolicy) {
            if (instance !== policy) {
                return
            }
            instance = DefaultEntityFriendlyPolicy
        }

        /**
         * 当前生效策略；未注册时返回默认策略，调用方无需判空。
         *
         * @return 生效中的策略实例。
         */
        fun current(): EntityFriendlyPolicy {
            return instance
        }

        /**
         * 判断两个实体是否互为友方。
         *
         * @param source 判定发起方。
         * @param target 被判定方。
         * @return 互为友方时返回 true。
         */
        fun isFriendly(source: LivingEntity, target: LivingEntity): Boolean {
            return instance.isFriendly(source, target)
        }
    }

    /**
     * 校验策略是否可用。
     *
     * @return 可用时返回 true。
     */
    fun checkEnable(): Boolean

    /**
     * 判断两者是否互为友方。
     *
     * @param source 判定发起方。
     * @param target 被判定方。
     * @return 互为友方时返回 true。
     */
    fun isFriendly(source: LivingEntity, target: LivingEntity): Boolean
}
