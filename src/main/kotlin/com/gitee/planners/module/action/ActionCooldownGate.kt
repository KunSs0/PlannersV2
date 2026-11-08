package com.gitee.planners.module.action

import org.bukkit.entity.Player

/**
 * 按玩家维护的运行时冷却门禁。
 *
 * 与技能冷却不同：这里不对外展示、不写档案，只用于「同一效果在 N tick 内
 * 不重复触发」这类被动内冷却。玩家退出时随内存一起丢弃。
 */
object ActionCooldownGate {

    /** 一 tick 的毫秒数。 */
    private const val MILLIS_PER_TICK = 50L

    /** 玩家 UUID 加门禁键到到期毫秒的映射。 */
    private val expiries = LinkedHashMap<String, Long>()

    /**
     * 判断门禁是否已就绪。
     *
     * @param player 玩家。
     * @param key 门禁键。
     * @return 没有活跃冷却时返回 true。
     */
    fun ready(player: Player, key: String): Boolean {
        val identity = identify(player, key)
        val expire = expiries[identity]
        if (expire == null) {
            return true
        }
        if (System.currentTimeMillis() >= expire) {
            expiries.remove(identity)
            return true
        }
        return false
    }

    /**
     * 开启一次门禁。
     *
     * @param player 玩家。
     * @param key 门禁键。
     * @param ticks 持续 tick；非正数时不记录。
     */
    fun arm(player: Player, key: String, ticks: Int) {
        if (ticks <= 0) {
            return
        }
        expiries[identify(player, key)] = System.currentTimeMillis() + ticks * MILLIS_PER_TICK
    }

    /**
     * 清除某个玩家的全部门禁，供退出时收尾。
     *
     * @param player 玩家。
     */
    fun clear(player: Player) {
        val prefix = player.uniqueId.toString() + ":"
        val iterator = expiries.keys.iterator()
        while (iterator.hasNext()) {
            val current = iterator.next()
            if (current.startsWith(prefix)) {
                iterator.remove()
            }
        }
    }

    /**
     * 生成门禁标识。
     *
     * @param player 玩家。
     * @param key 门禁键。
     * @return 唯一标识。
     */
    private fun identify(player: Player, key: String): String {
        return player.uniqueId.toString() + ":" + key
    }
}
