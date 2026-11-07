package com.gitee.planners.module.action

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * 战斗属性读取入口。
 *
 * `attr.*` 与 `natural.*` 查询根需要读取战斗属性，该能力由战斗插件提供：
 * 安装前两个读取器都返回 null，装上后按安装方实现取值。
 *
 * 这样 Planners 不必反向依赖战斗插件，也不会把属性公式复制一份。
 */
object ActionAttributeAccess {

    /** 含装备与 Buff 的战斗属性读取器。 */
    fun interface AttrReader {

        /**
         * 读取战斗属性。
         *
         * @param entity 目标实体。
         * @param key 属性键。
         * @return 属性值；不存在时返回 null。
         */
        fun read(entity: LivingEntity, key: String): Double?
    }

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

    private val emptyAttr = AttrReader { _, _ -> null }

    private val emptyNatural = NaturalReader { _, _ -> null }

    @Volatile
    private var attrReader: AttrReader = emptyAttr

    @Volatile
    private var naturalReader: NaturalReader = emptyNatural

    /**
     * 安装战斗属性读取器。
     *
     * @param value 读取器实现。
     */
    fun installAttr(value: AttrReader) {
        attrReader = value
    }

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
        attrReader = emptyAttr
        naturalReader = emptyNatural
    }

    /**
     * 读取战斗属性。
     *
     * @param entity 目标实体。
     * @param key 属性键。
     * @return 属性值；未安装读取器或属性不存在时返回 null。
     */
    fun attr(entity: LivingEntity, key: String): Double? {
        return attrReader.read(entity, key)
    }

    /**
     * 读取职业自然成长。
     *
     * @param player 玩家。
     * @param key 属性键。
     * @return 属性值；未安装读取器或属性不存在时返回 null。
     */
    fun natural(player: Player, key: String): Double? {
        return naturalReader.read(player, key)
    }
}
