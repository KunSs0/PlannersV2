package com.gitee.planners.core.skill.status

import com.gitee.planners.Planners
import com.gitee.planners.api.BackpackAPI
import com.gitee.planners.api.PlannersAPI
import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.condition.SkillCastConditionRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 技能可用性服务。
 *
 * 复用释放期的同一套条件校验，回答「这个技能现在能不能放」；变化通过 [invalidate] 打脏标记，
 * 按 `settings.skill.status.merge-ticks` 合并后重算，不做周期轮询。业务可在
 * [PlayerSkillStatusCheckEvent] 里补充环境型结论。
 */
object PlayerSkillStatus {

    /** 不可释放原因取值。 */
    val REASONS = listOf(
        "cooldown",
        "mana",
        "stamina",
        "gauge",
        "item",
        "weapon",
        "player-state",
        "state",
        "condition",
        "unknown",
    )

    /**
     * 单个技能的检查结果。
     *
     * @property ready 是否可以释放。
     * @property reason 不可释放原因；可释放时为空。
     * @property detail 服务端诊断信息，不下发客户端。
     */
    data class Status(
        val ready: Boolean,
        val reason: String?,
        val detail: Map<String, Any> = emptyMap(),
    )

    private val cache = ConcurrentHashMap<UUID, Map<String, Status>>()

    private val pending = LinkedHashSet<UUID>()

    private var scheduled = false

    /** 是否启用状态检查。 */
    fun enabled(): Boolean {
        return Planners.config.getBoolean("settings.skill.status.enable", true)
    }

    /**
     * 立即检查该玩家全部已装备技能。
     *
     * @param player 玩家。
     * @return 技能 ID 到状态的快照。
     */
    fun check(player: Player): Map<String, Status> {
        val results = LinkedHashMap<String, Status>()
        val template = player.plannersTemplate
        if (template == null) {
            cache.remove(player.uniqueId)
            return results
        }
        val page = BackpackAPI.getCurrentPage(template)
        val skills = template.getEquippedSkillsForPage(page).values.filterNotNull()
        for (skill in skills) {
            results[skill.id] = evaluate(player, skill)
        }
        PlayerSkillStatusCheckEvent(player, skills, results).call()
        val snapshot = LinkedHashMap(results)
        cache[player.uniqueId] = snapshot
        return snapshot
    }

    /**
     * 标记玩家状态失效，按合并窗口延后重算。
     *
     * @param player 玩家。
     * @param reason 失效原因，仅用于日志排查。
     */
    fun invalidate(player: Player, reason: String) {
        if (!enabled() || !player.isOnline) {
            return
        }
        synchronized(pending) {
            pending.add(player.uniqueId)
        }
        schedule()
    }

    /**
     * 读取缓存状态，不触发计算。
     *
     * @param player 玩家。
     * @return 上次检查结果；从未检查时为空表。
     */
    fun current(player: Player): Map<String, Status> {
        return cache[player.uniqueId] ?: emptyMap()
    }

    /**
     * 清理玩家缓存与未处理失效。
     *
     * @param player 玩家。
     */
    fun clear(player: Player) {
        cache.remove(player.uniqueId)
        synchronized(pending) {
            pending.remove(player.uniqueId)
        }
    }

    /**
     * 检查单个技能。
     *
     * @param player 玩家。
     * @param skill 技能。
     * @return 检查结果。
     */
    private fun evaluate(player: Player, skill: PlayerSkill): Status {
        return try {
            val execution = PlannersAPI.newExecution(player, skill)
            val failure = SkillCastConditionRegistry.verify(player, skill, execution, skill.immutable.castConditions)
            if (failure != null) {
                Status(false, "condition", mapOf("message" to failure))
            } else {
                Status(true, null)
            }
        } catch (exception: Exception) {
            Status(false, "unknown", mapOf("error" to (exception.message ?: "check failed")))
        }
    }

    /** 安排一次合并窗口后的重算。 */
    private fun schedule() {
        synchronized(pending) {
            if (scheduled) {
                return
            }
            scheduled = true
        }
        val delay = Planners.config.getInt("settings.skill.status.merge-ticks", 1).coerceAtLeast(1).toLong()
        submit(delay = delay) {
            flush()
        }
    }

    /** 处理所有待重算玩家。 */
    private fun flush() {
        val players = ArrayList<UUID>()
        synchronized(pending) {
            scheduled = false
            players.addAll(pending)
            pending.clear()
        }
        for (id in players) {
            val player = Bukkit.getPlayer(id)
            if (player != null && player.isOnline) {
                check(player)
            } else {
                cache.remove(id)
            }
        }
    }
}
