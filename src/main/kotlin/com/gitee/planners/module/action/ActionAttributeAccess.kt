package com.gitee.planners.module.action

import org.bukkit.entity.Player

/**
 * 职业自然成长读取入口。
 *
 * `natural.*` 查询根需要区分"职业成长"与"含装备、Buff 的属性汇总"，
 * 该判定由战斗插件提供；安装前读取器返回 null。
 *
 * 战斗属性 `attr.*` 不经过本入口：Planners 直接读 [com.gitee.planners.module.compat.attribute.AttributeDriver]，
 * 避免与属性驱动重复暴露同一份数据。
 */
object ActionAttributeAccess {

    /** 只含职业自然成长的属性读取器。 */
    fun interface NaturalReader {

        /**
         * 读取职业自然成长。
         *
         * @param player 玩家。
         * @param key 属性键。
         * @return 属性值；不存在时返回 null。
         */
        fun read(player: Player, key: String): Double?
    }

    private val emptyNatural = NaturalReader { _, _ -> null }

    @Volatile
    private var naturalReader: NaturalReader = emptyNatural

    /**
     * 安装职业自然成长读取器。
     *
     * @param value 读取器实现。
     */
    fun installNatural(value: NaturalReader) {
        naturalReader = value
    }

    /**
     * 恢复默认读取器。
     */
    fun reset() {
        naturalReader = emptyNatural
    }

    /**
     * 读取职业自然成长。
     *
     * @param player 玩家。
     * @param key 属性键。
     * @return 自然成长值；未安装读取器或属性不存在时返回 null。
     */
    fun natural(player: Player, key: String): Double? {
        return naturalReader.read(player, key)
    }
}
