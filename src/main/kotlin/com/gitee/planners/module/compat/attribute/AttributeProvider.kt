package com.gitee.planners.module.compat.attribute

import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.module.fluxon.FluxonScriptOptions
import com.gitee.planners.module.fluxon.SingletonFluxonScript
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.api.event.player.PlayerSetRouteEvent
import com.gitee.planners.api.event.player.PlayerSkillEvent
import com.gitee.planners.core.config.ImmutableJob
import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent

object AttributeProvider {

    const val SOURCE = "planners-base"

    @SubscribeEvent
    fun e(e: PlayerProfileLoadedEvent) {
        update(e.player)
    }

    @SubscribeEvent
    fun e(e: PlayerSkillEvent.LevelChange) {
        update(e.player)
    }

    @SubscribeEvent
    fun e(e: PlayerSetRouteEvent.Post) {
        update(e.player)
    }

    private fun update(player: Player) {
        val attributeList = getAttributeList(player)
        AttributeDriver.set(player, SOURCE, attributeList, -1)
    }

    /**
     * 获取玩家的属性列表
     *
     * @param player 玩家
     *
     * @return 属性列表
     */
    private fun getAttributeList(player: Player): List<String> {
        val listOf = mutableListOf<String>()
        val playerTemplate = player.plannersTemplate
        // 传递技能
        playerTemplate.route?.getImmutableSkillValues()?.forEach {
            listOf.addAll(getAttributeListWithSkill(player, it))
        }
        // 传递职业
        listOf.addAll(getAttributeListWithJob(player))
        return listOf
    }

    /**
     * 获取职业的属性列表
     *
     * @param player 玩家
     * @param job 职业
     *
     * @return 属性列表
     */
    private fun getAttributeListWithJob(player: Player): List<String> {
        val playerTemplate = player.plannersTemplate
        val immutableJob = playerTemplate.route?.getJob() as? ImmutableJob
        if (immutableJob == null) {
            return emptyList()
        }
        val attributes = immutableJob.attributes

        if (attributes.isEmpty()) {
            return emptyList()
        }

        val options = FluxonScriptOptions.common(player)

        return attributes.map { parse(it, options) }
    }

    /**
     * 获取技能的属性列表
     *
     * @param player 玩家
     * @param skill 技能
     *
     * @return 属性列表
     */
    private fun getAttributeListWithSkill(player: Player, skill: ImmutableSkill): List<String> {
        val attributes = skill.attributes

        if (attributes.isEmpty()) {
            return emptyList()
        }

        val options = PlannersAPI.newOptions(player, skill)

        return attributes.map { parse(it, options) }
    }

    fun parse(text: String, options: FluxonScriptOptions): String {
        return SingletonFluxonScript.replaceNested(text.trim(), options)
    }
}
