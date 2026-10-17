package com.gitee.planners.core.player.magic

import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.entity.Player
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info

/**
 * 玩家法力访问与变更协议，provider 的替换和释放由 Planners 统一管理。
 */
interface MagicPointProvider : AutoCloseable {

    companion object {

        @Volatile
        private var instance: MagicPointProvider? = null

        /**
         * 当前法力提供器；替换时先关闭旧提供器，避免两个回复所有者同时运行。
         */
        var INSTANCE: MagicPointProvider
            get() {
                return checkNotNull(instance) { "Planners MagicPointProvider 未注册，无法访问法力。" }
            }
            set(value) {
                check(Bukkit.isPrimaryThread()) { "法力提供器替换必须在 Bukkit 主线程执行" }
                val previous = instance
                if (previous === value) {
                    return
                }
                // 先使旧所有者失效，再释放其任务；关闭失败时不能安装半套接入。
                instance = null
                if (previous != null) {
                    previous.close()
                }
                instance = value
                info("MagicPointProvider registered: ${value.javaClass.name}")
            }

        /**
         * 仅释放当前所有者，不自动恢复其他提供器或重新启动默认回复。
         *
         * @param provider 需要注销的提供器实例。
         */
        fun unregister(provider: MagicPointProvider) {
            check(Bukkit.isPrimaryThread()) { "法力提供器注销必须在 Bukkit 主线程执行" }
            // 其他插件的卸载不能释放当前新的所有者。
            if (instance !== provider) {
                return
            }
            instance = null
            provider.close()
        }

        /**
         * 仅在没有外部接入时安装原生法力提供器。
         */
        @JvmStatic
        @Awake(LifeCycle.ENABLE)
        fun init() {
            // 注册默认提供器
            if (instance == null) {
                INSTANCE = DefaultMagicPointProvider()
            }
        }

        /**
         * 插件停止时释放当前提供器的全部自有资源。
         */
        @JvmStatic
        @Awake(LifeCycle.DISABLE)
        fun shutdown() {
            // 与第三方卸载使用相同的唯一所有者协议。
            val provider = instance
            if (provider != null) {
                unregister(provider)
            }
        }

        val PlayerTemplate.magicPointInUpperLimit: Int
            get() = INSTANCE.getPointInUpperLimit(this.onlinePlayer)

        var PlayerTemplate.magicPoint: Int
            get() = INSTANCE.getPoint(this.onlinePlayer)
            set(value) {
                INSTANCE.setPoint(this.onlinePlayer, value)
            }

    }

    /**
     * 获取用于整数技能门槛和展示的当前点数，禁止用此投影执行读改写。
     *
     * @param player 目标玩家。
     * @return 当前法力的整数投影。
     */
    fun getPoint(player: Player): Int

    /**
     * 显式设置整数目标值；相对增减应使用 changePoint 或 consumePoint。
     *
     * @param player 目标玩家。
     * @param magicPoint 明确指定的新值。
     */
    fun setPoint(player: Player, magicPoint: Int)

    /**
     * 获取最终法力容量的整数值。
     *
     * @param player 目标玩家。
     * @return 最终容量。
     */
    fun getPointInUpperLimit(player: Player): Int

    /**
     * 在提供器自己的权威存储中相对增减，外部小数存储应覆盖此扩展点。
     *
     * @param player 目标玩家。
     * @param delta 有符号的整数增量，不代表新的绝对值。
     */
    fun changePoint(player: Player, delta: Int) {
        // 原生提供器使用整数存储；外部实现可以直接调用资源变更 API。
        setPoint(player, Math.addExact(getPoint(player), delta))
    }

    /**
     * 检查并扣除非负消耗，外部提供器可使用原生资源扣费接口保留小数余额。
     *
     * @param player 目标玩家。
     * @param amount 非负消耗点数。
     * @return 余额足够并完成扣费时为 true，否则不修改资源。
     */
    fun consumePoint(player: Player, amount: Int): Boolean {
        require(amount >= 0) { "法力消耗必须为非负整数" }
        // 默认整数存储也在真正扣费时重新校验余额。
        if (getPoint(player) < amount) {
            return false
        }
        changePoint(player, -amount)
        return true
    }

    /**
     * 提供器失去所有权或 Planners 停止时的清理钩子。
     */
    override fun close() {
        // 纯 API 转接无需自有任务；持有任务的实现负责在此取消。
    }

}
