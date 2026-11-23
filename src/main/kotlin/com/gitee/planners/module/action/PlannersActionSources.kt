package com.gitee.planners.module.action

import java.util.Locale

/**
 * 目标器与触发器的分类知识。
 *
 * Planners 的目标器把"是否作为链首源"隐含在"是否忽略上游容器"里，而 Action 的链校验需要显式声明。
 * 这里集中维护分类，避免把判断散落到适配层；分类错误会在解析期以"链首必须是源目标器"报错，
 * 不会静默降级。
 */
object PlannersActionSources {

    /**
     * 作为链首的源目标器主键。
     */
    private val SOURCES: Set<String> = setOf(
        "sender",
        "sender.loc",
        "target",
        "range",
        "sector",
        "rect",
        "source",
        "target.finder",
        "finder.capture",
        "crosshair",
        "online-players",
        "attacker",
        "defender",
        "uuid",
        "forward",
    )

    /**
     * 定时触发器主键。
     */
    private const val TIMER = "timer"

    /**
     * 判断目标器是否作为链首源目标器。
     *
     * @param id 目标器主键
     * @return 源目标器返回 true
     */
    fun isSource(id: String): Boolean {
        return SOURCES.contains(id.trim().lowercase(Locale.ROOT))
    }

    /**
     * 判断触发器是否为定时触发器。
     *
     * @param id 触发器主键
     * @return 定时触发器返回 true
     */
    fun isTimer(id: String): Boolean {
        return TIMER.equals(id.trim().lowercase(Locale.ROOT))
    }
}
