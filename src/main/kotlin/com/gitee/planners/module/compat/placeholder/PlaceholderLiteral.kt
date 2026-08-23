package com.gitee.planners.module.compat.placeholder

import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersLoaded
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.Registries
import com.gitee.planners.api.job.target.asTarget
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.config.State.Companion.path
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPoint
import com.gitee.planners.core.player.magic.MagicPointProvider.Companion.magicPointInUpperLimit
import com.gitee.planners.core.skill.cooler.Cooler
import com.gitee.planners.core.skill.entity.state.EntityStateManager
import com.gitee.planners.core.skill.entity.state.TargetStateHolder
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import org.bukkit.entity.Player
import kotlin.math.roundToLong

/**
 * Planners 字面量 PlaceholderAPI 解析器。
 *
 * 占位符语法为 {@code 查询名[:参数...]}; 参数使用冒号分隔，从而允许技能、职业和状态 ID 使用下划线。
 */
object PlaceholderLiteral {

    /**
     * 解析单个 Planners 占位符。
     *
     * @param player 已由扩展入口确认非空的玩家。
     * @param args PlaceholderAPI 传入的扩展参数。
     * @return 查询结果；未知查询、参数无效或档案未加载时返回空字符串。
     */
    fun parse(player: Player, args: String): String {
        val request = Request.parse(args)
        if (request == null) {
            return ""
        }

        if (request.name == "profile_loaded") {
            return player.plannersLoaded.toString()
        }

        if (!player.plannersLoaded) {
            return ""
        }

        val template = player.plannersTemplate
        // 解析层只读取领域对象，不执行占位符文本或用户脚本。
        return when (request.name) {
            "level" -> parseWithoutArguments(request, template.level)
            "level_min" -> parseWithoutArguments(request, template.playerRouter?.minLevel ?: 1)
            "level_max" -> parseWithoutArguments(request, template.playerRouter?.maxLevel ?: Int.MAX_VALUE)
            "experience" -> parseWithoutArguments(request, template.experience)
            "experience_max" -> parseWithoutArguments(request, template.experienceMax)
            "experience_remaining" -> parseExperienceRemaining(request, template)
            "experience_percent" -> parseExperiencePercent(request, template)
            "magic" -> parseWithoutArguments(request, template.magicPoint)
            "magic_max" -> parseWithoutArguments(request, template.magicPointInUpperLimit)
            "magic_percent" -> parseMagicPercent(request, template)
            "router_id" -> parseRouterId(request, template)
            "router_name" -> parseRouterName(request, template)
            "job_id" -> parseJobId(request, template)
            "job_name" -> parseJobName(request, template)
            "skill_points" -> parseSkillPoints(request, template)
            "skill_points_used" -> parseSkillPointsUsed(request, template)
            "skill_points_total" -> parseSkillPointsTotal(request, template)
            "skill_count" -> parseSkillCount(request, template)
            "backpack_page" -> parseBackpackPage(request, template)
            "backpack_page_name" -> parseBackpackPageName(request, template)
            "skill_name" -> parseSkillName(request)
            "skill_level" -> parseSkillLevel(request, template)
            "skill_max_level" -> parseSkillMaxLevel(request)
            "skill_learned" -> parseSkillLearned(request, template)
            "skill_equipped" -> parseSkillEquipped(request, template)
            "skill_page" -> parseSkillPage(request, template)
            "skill_slot" -> parseSkillSlot(request, template)
            "skill_cooldown" -> parseSkillCooldown(request, player, template)
            "backpack_skill_id" -> parseBackpackSkillId(request, template)
            "backpack_skill_name" -> parseBackpackSkillName(request, template)
            "backpack_skill_level" -> parseBackpackSkillLevel(request, template)
            "attribute" -> parseAttribute(request, player)
            "state_active" -> parseStateActive(request, player)
            "state_layer" -> parseStateLayer(request, player)
            "state_remaining" -> parseStateRemaining(request, player)
            else -> ""
        }
    }

    /**
     * 解析不接受参数的数值查询。
     *
     * @param request 已解析的请求。
     * @param value 查询值。
     * @return 参数数量正确时的字符串值，否则为空字符串。
     */
    private fun parseWithoutArguments(request: Request, value: Any): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        return value.toString()
    }

    /**
     * 解析经验到下一等级所需的剩余经验。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 剩余经验，参数无效时为空字符串。
     */
    private fun parseExperienceRemaining(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        return maxOf(template.experienceMax - template.experience, 0).toString()
    }

    /**
     * 解析经验百分比，范围固定为 0 到 100。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 整数百分比，参数无效时为空字符串。
     */
    private fun parseExperiencePercent(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        return formatPercent(template.experience, template.experienceMax)
    }

    /**
     * 解析魔法值百分比，范围固定为 0 到 100。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 整数百分比，参数无效时为空字符串。
     */
    private fun parseMagicPercent(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        return formatPercent(template.magicPoint, template.magicPointInUpperLimit)
    }

    /**
     * 获取玩家当前选择的完整转职线。
     *
     * PlayerRouter 聚合当前 Job 阶段、父阶段链、全线技能与共享 SP；未选择职业时为空。
     *
     * @param template 玩家档案。
     * @return 玩家转职线；尚未选择职业时为 null。
     */
    private fun getPlayerRouterOrNull(template: PlayerTemplate): PlayerRouter? {
        return template.playerRouter
    }

    /**
     * 解析当前路由 ID。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前转职线 ID；未选择职业或参数无效时为空字符串。
     */
    private fun parseRouterId(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return playerRouter.routerId
    }

    /**
     * 解析当前路由名称。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前转职线显示名称；未选择职业或参数无效时为空字符串。
     */
    private fun parseRouterName(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return playerRouter.router.name
    }

    /**
     * 解析当前职业 ID。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前职业 ID；未选择职业或参数无效时为空字符串。
     */
    private fun parseJobId(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return playerRouter.currentRoute.jobId
    }

    /**
     * 解析当前职业名称。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前职业名称；未选择职业或参数无效时为空字符串。
     */
    private fun parseJobName(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return playerRouter.currentRoute.name
    }

    /**
     * 解析当前可用技能点。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前转职线可用共享 SP；未选择职业或参数无效时为空字符串。
     */
    private fun parseSkillPoints(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return playerRouter.skillPointsCurrent.toString()
    }

    /**
     * 解析已消耗技能点。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前转职线已消耗共享 SP；未选择职业或参数无效时为空字符串。
     */
    private fun parseSkillPointsUsed(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return playerRouter.skillPointsUsed.toString()
    }

    /**
     * 解析累计技能点。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前转职线可用与已消耗共享 SP 之和；未选择职业或参数无效时为空字符串。
     */
    private fun parseSkillPointsTotal(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        return (playerRouter.skillPointsCurrent + playerRouter.skillPointsUsed).toString()
    }

    /**
     * 解析已学习技能数量。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 全转职线中等级大于零的技能数量；未选择职业或参数无效时为空字符串。
     */
    private fun parseSkillCount(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val playerRouter = getPlayerRouterOrNull(template)
        if (playerRouter == null) {
            return ""
        }
        var count = 0
        for (skill in playerRouter.effectiveSkills.values) {
            if (SkillTreeNodeEffectService.getSkillLevel(template, skill.id) > 0) {
                count++
            }
        }
        return count.toString()
    }

    /**
     * 解析当前背包页面 ID。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前页面 ID，参数无效时为空字符串。
     */
    private fun parseBackpackPage(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        return BackpackAPI.getCurrentPage(template)
    }

    /**
     * 解析当前背包页面名称。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 当前页面名称；页面配置不存在或参数无效时为空字符串。
     */
    private fun parseBackpackPageName(request: Request, template: PlayerTemplate): String {
        if (request.arguments.isNotEmpty()) {
            return ""
        }
        val pageId = BackpackAPI.getCurrentPage(template)
        val page = Registries.BACKPACK.getPage(pageId)
        if (page == null) {
            return ""
        }
        return page.name
    }

    /**
     * 解析技能显示名称。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @return 技能名称；技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillName(request: Request): String {
        val skillId = getSingleArgument(request)
        if (skillId == null) {
            return ""
        }
        val skill = Registries.SKILL.getOrNull(skillId)
        if (skill == null) {
            return ""
        }
        return skill.name
    }

    /**
     * 解析玩家技能等级。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param template 玩家档案。
     * @return 已注册技能等级；未注册技能返回 {@code 0}，技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillLevel(request: Request, template: PlayerTemplate): String {
        val skillId = getSingleArgument(request)
        if (skillId == null || Registries.SKILL.getOrNull(skillId) == null) {
            return ""
        }
        return SkillTreeNodeEffectService.getSkillLevel(template, skillId).toString()
    }

    /**
     * 解析配置的技能最高等级。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @return 技能最高等级；技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillMaxLevel(request: Request): String {
        val skillId = getSingleArgument(request)
        if (skillId == null) {
            return ""
        }
        val skill = Registries.SKILL.getOrNull(skillId)
        if (skill == null) {
            return ""
        }
        return skill.maxLevel.toString()
    }

    /**
     * 解析技能是否已学习。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param template 玩家档案。
     * @return 技能等级大于零时为 {@code true}；技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillLearned(request: Request, template: PlayerTemplate): String {
        val skillId = getSingleArgument(request)
        if (skillId == null || Registries.SKILL.getOrNull(skillId) == null) {
            return ""
        }
        return (SkillTreeNodeEffectService.getSkillLevel(template, skillId) > 0).toString()
    }

    /**
     * 解析技能是否已经装备。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param template 玩家档案。
     * @return 技能已装备时为 {@code true}；技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillEquipped(request: Request, template: PlayerTemplate): String {
        val skillId = getSingleArgument(request)
        if (skillId == null || Registries.SKILL.getOrNull(skillId) == null) {
            return ""
        }
        val skill = template.getRegisteredSkillOrNull(skillId)
        return (skill != null && skill.equipped).toString()
    }

    /**
     * 解析技能所在背包页面 ID。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param template 玩家档案。
     * @return 页面 ID；技能未装备、技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillPage(request: Request, template: PlayerTemplate): String {
        val skill = getRegisteredSkill(request, template)
        if (skill == null || !skill.equipped) {
            return ""
        }
        return skill.backpackPage ?: ""
    }

    /**
     * 解析技能所在背包槽位 ID。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param template 玩家档案。
     * @return 槽位 ID；技能未装备、技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillSlot(request: Request, template: PlayerTemplate): String {
        val skill = getRegisteredSkill(request, template)
        if (skill == null || !skill.equipped) {
            return ""
        }
        return skill.backpackSlot ?: ""
    }

    /**
     * 解析技能剩余冷却，单位为 tick。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param player 查询目标玩家。
     * @param template 玩家档案。
     * @return 剩余冷却 tick；技能不存在或参数无效时为空字符串。
     */
    private fun parseSkillCooldown(request: Request, player: Player, template: PlayerTemplate): String {
        val skill = getRegisteredSkill(request, template)
        if (skill == null) {
            return ""
        }
        return Cooler.INSTANCE.get(player, skill).toString()
    }

    /**
     * 解析指定页面槽位的已装备技能。
     *
     * @param request 已解析的请求，必须包含页面 ID 和槽位 ID。
     * @param template 玩家档案。
     * @return 已装备技能；参数无效或槽位为空时返回 null。
     */
    private fun getBackpackSkill(request: Request, template: PlayerTemplate): PlayerSkill? {
        if (request.arguments.size != 2) {
            return null
        }
        val pageId = request.arguments[0]
        val slotId = request.arguments[1]
        if (pageId.isEmpty() || slotId.isEmpty()) {
            return null
        }
        val page = Registries.BACKPACK.getPage(pageId)
        if (page == null || !page.slots.containsKey(slotId)) {
            return null
        }
        return template.getEquippedSkillByBackpackSlot(pageId, slotId)
    }

    /**
     * 解析槽位内技能 ID。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 技能 ID；槽位为空或参数无效时为空字符串。
     */
    private fun parseBackpackSkillId(request: Request, template: PlayerTemplate): String {
        val skill = getBackpackSkill(request, template)
        if (skill == null) {
            return ""
        }
        return skill.id
    }

    /**
     * 解析槽位内技能名称。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 技能名称；槽位为空或参数无效时为空字符串。
     */
    private fun parseBackpackSkillName(request: Request, template: PlayerTemplate): String {
        val skill = getBackpackSkill(request, template)
        if (skill == null) {
            return ""
        }
        return skill.name
    }

    /**
     * 解析槽位内技能等级。
     *
     * @param request 已解析的请求。
     * @param template 玩家档案。
     * @return 技能等级；槽位为空或参数无效时为空字符串。
     */
    private fun parseBackpackSkillLevel(request: Request, template: PlayerTemplate): String {
        val skill = getBackpackSkill(request, template)
        if (skill == null) {
            return ""
        }
        return SkillTreeNodeEffectService.getSkillLevel(template, skill.id).toString()
    }

    /**
     * 解析逻辑属性值。
     *
     * @param request 已解析的请求，必须包含一个属性 ID。
     * @param player 查询目标玩家。
     * @return 属性数值；属性不存在或参数无效时为空字符串。
     */
    private fun parseAttribute(request: Request, player: Player): String {
        val attributeId = getSingleArgument(request)
        if (attributeId == null) {
            return ""
        }
        if (!com.gitee.planners.Planners.attributeRegistry.get().containsKey(attributeId)) {
            return ""
        }
        return formatNumber(AttributeProxy.get(player, attributeId))
    }

    /**
     * 解析玩家是否拥有状态。
     *
     * @param request 已解析的请求，必须包含一个状态 ID。
     * @param player 查询目标玩家。
     * @return 状态有效且至少有一层时为 {@code true}；状态不存在或参数无效时为空字符串。
     */
    private fun parseStateActive(request: Request, player: Player): String {
        val state = getState(request)
        if (state == null) {
            return ""
        }
        return EntityStateManager.has(player.asTarget(), state).toString()
    }

    /**
     * 解析玩家状态层数。
     *
     * @param request 已解析的请求，必须包含一个状态 ID。
     * @param player 查询目标玩家。
     * @return 当前状态层数；状态不存在或参数无效时为空字符串。
     */
    private fun parseStateLayer(request: Request, player: Player): String {
        val state = getState(request)
        if (state == null) {
            return ""
        }
        return EntityStateManager.getLayer(player.asTarget(), state).toString()
    }

    /**
     * 解析玩家状态剩余时间，单位为 tick。
     *
     * @param request 已解析的请求，必须包含一个状态 ID。
     * @param player 查询目标玩家。
     * @return 剩余状态时间；状态不存在、未生效或参数无效时返回 {@code 0} 或空字符串。
     */
    private fun parseStateRemaining(request: Request, player: Player): String {
        val state = getState(request)
        if (state == null) {
            return ""
        }
        val metadata = player.asTarget().getMetadata(state.path())
        val holder = TargetStateHolder.parse(metadata)
        if (holder == null || !holder.isValid || holder.layer <= 0) {
            return "0"
        }
        return maxOf((holder.end - System.currentTimeMillis()) / 50, 0).toString()
    }

    /**
     * 获取请求指定的已注册技能。
     *
     * @param request 已解析的请求，必须包含一个技能 ID。
     * @param template 玩家档案。
     * @return 匹配的玩家技能；技能不存在、未注册或参数无效时返回 null。
     */
    private fun getRegisteredSkill(request: Request, template: PlayerTemplate): PlayerSkill? {
        val skillId = getSingleArgument(request)
        if (skillId == null || Registries.SKILL.getOrNull(skillId) == null) {
            return null
        }
        return template.getRegisteredSkillOrNull(skillId)
    }

    /**
     * 获取请求指定的状态定义。
     *
     * @param request 已解析的请求，必须包含一个状态 ID。
     * @return 状态定义；状态不存在或参数无效时返回 null。
     */
    private fun getState(request: Request): com.gitee.planners.core.config.State? {
        val stateId = getSingleArgument(request)
        if (stateId == null) {
            return null
        }
        return Registries.STATE.getOrNull(stateId)
    }

    /**
     * 获取仅有的一个非空参数。
     *
     * @param request 已解析的请求。
     * @return 唯一参数；参数数量不为一或参数为空时返回 null。
     */
    private fun getSingleArgument(request: Request): String? {
        if (request.arguments.size != 1) {
            return null
        }
        val argument = request.arguments[0]
        if (argument.isEmpty()) {
            return null
        }
        return argument
    }

    /**
     * 将分子和分母格式化为 0 到 100 的整数百分比。
     *
     * @param value 当前值。
     * @param maximum 最大值。
     * @return 整数百分比。
     */
    private fun formatPercent(value: Int, maximum: Int): String {
        if (maximum <= 0) {
            return "0"
        }
        val percent = value.toDouble() * 100.0 / maximum.toDouble()
        return percent.coerceIn(0.0, 100.0).roundToLong().toString()
    }

    /**
     * 将属性数值格式化为紧凑文本。
     *
     * @param value 要格式化的数值。
     * @return 整数去除小数部分，其余数值保留原有精度。
     */
    private fun formatNumber(value: Double): String {
        if (value == value.toLong().toDouble()) {
            return value.toLong().toString()
        }
        return value.toString()
    }

    /**
     * 已拆分的字面量占位符请求。
     *
     * @property name 小写标准化后的查询名。
     * @property arguments 按顺序保留的参数列表。
     */
    private class Request(val name: String, val arguments: List<String>) {

        companion object {

            /**
             * 从 PlaceholderAPI 参数构造请求。
             *
             * @param source 原始参数文本。
             * @return 合法请求；空查询名时返回 null。
             */
            fun parse(source: String): Request? {
                val parts = source.trim().split(':')
                if (parts.isEmpty()) {
                    return null
                }
                val name = parts[0].trim().lowercase()
                if (name.isEmpty()) {
                    return null
                }
                val arguments = mutableListOf<String>()
                var index = 1
                while (index < parts.size) {
                    arguments += parts[index].trim()
                    index++
                }
                return Request(name, arguments)
            }
        }
    }

}
