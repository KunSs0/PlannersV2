package com.gitee.planners.core.skill.cooler

import com.gitee.planners.core.config.ImmutableSkill
import org.bukkit.entity.Player

/**
 * 技能共享冷却组。
 *
 * 组由 `__option__.cooldown.group` 声明，索引在技能重载后重建。同一组内任一技能提交冷却后，
 * 其余成员同步进入同样的剩余时间，只延长不缩短；成员各自的 Cooler 值被写入后，
 * HUD 会按正常冷却事件更新对应槽位，不需要额外通道。
 */
object CooldownGroups {

    /** 组标识到成员的索引。 */
    private var groups: Map<String, List<ImmutableSkill>> = emptyMap()

    /**
     * 按当前技能表重建分组索引。
     *
     * @param skills 全部已加载技能。
     */
    fun rebuild(skills: Collection<ImmutableSkill>) {
        val entries = LinkedHashMap<String, String?>()
        val byId = LinkedHashMap<String, ImmutableSkill>()
        for (skill in skills) {
            entries[skill.id] = skill.cooldownGroup
            byId[skill.id] = skill
        }
        val resolved = LinkedHashMap<String, List<ImmutableSkill>>()
        for (entry in indexById(entries)) {
            val members = ArrayList<ImmutableSkill>(entry.value.size)
            for (id in entry.value) {
                val skill = byId[id]
                if (skill != null) {
                    members.add(skill)
                }
            }
            resolved[entry.key] = members
        }
        groups = resolved
    }

    /**
     * 读取组内成员。
     *
     * @param group 组标识。
     * @return 成员列表；未声明该组时返回空列表。
     */
    fun members(group: String): List<ImmutableSkill> {
        return groups[group] ?: emptyList()
    }

    /**
     * 把一次冷却同步给组内其它成员。
     *
     * 只延长、不缩短：本次冷却比成员现有剩余短时跳过，避免短冷却技能把长冷却洗掉。
     *
     * @param player 玩家。
     * @param skill 本次释放的技能。
     * @param ticks 本次冷却 tick。
     */
    fun share(player: Player, skill: ImmutableSkill, ticks: Int) {
        val group = skill.cooldownGroup
        if (group == null || ticks <= 0) {
            return
        }
        for (member in members(group)) {
            if (member.id == skill.id) {
                continue
            }
            val remaining = Cooler.INSTANCE.get(player, member)
            if (!shouldExtend(remaining, ticks)) {
                continue
            }
            Cooler.INSTANCE.set(player, member, ticks)
        }
    }

    /**
     * 判断本次冷却是否覆盖成员现有剩余。
     *
     * @param remainingTicks 成员现有剩余 tick。
     * @param ticks 本次冷却 tick。
     * @return 需要写入时返回 true。
     */
    internal fun shouldExtend(remainingTicks: Long, ticks: Int): Boolean {
        return remainingTicks < ticks.toLong()
    }

    /**
     * 按「技能 ID → 组标识」建立组索引。
     *
     * @param entries 技能 ID 到组标识；组为 null 表示不参与分组。
     * @return 组标识到技能 ID 的映射，按声明顺序。
     */
    internal fun indexById(entries: Map<String, String?>): Map<String, List<String>> {
        val result = LinkedHashMap<String, MutableList<String>>()
        for (entry in entries) {
            val group = entry.value
            if (group == null) {
                continue
            }
            var members = result[group]
            if (members == null) {
                members = ArrayList()
                result[group] = members
            }
            members.add(entry.key)
        }
        return result
    }
}
