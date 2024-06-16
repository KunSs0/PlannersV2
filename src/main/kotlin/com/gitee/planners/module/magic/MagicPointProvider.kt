package com.gitee.planners.module.magic

import org.bukkit.entity.Player

interface MagicPointProvider {

    /**
     * Get the magic point.
     * @return the magic point
     */
    fun getPoint(player: Player): Int

    /**
     * Set the magic point.
     * @param magicPoint the magic point
     */
    fun setPoint(player: Player,magicPoint: Int)

    /**
     * Get the magic point in the lower limit.
     * @return the magic point in the lower limit
     */
    fun getPointInUpperLimit(player: Player): Int

}
