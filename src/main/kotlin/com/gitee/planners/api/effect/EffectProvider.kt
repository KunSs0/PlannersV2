package com.gitee.planners.api.effect

import org.bukkit.Location

/**
 * 外部战斗插件向 Planners 提供的类型化特效播放能力。
 *
 * 契约只描述 Planners 技能脚本所需的调用数据和结果，不持有具体实现、特效 ID、
 * 技能分支或玩家提示等业务规则。
 */
fun interface EffectProvider {

    /**
     * 在指定世界位置播放一次特效。
     *
     * @param effectId 外部战斗插件定义的特效标识。
     * @param location 特效播放的世界坐标和朝向。
     * @param lifetimeTicks 特效持续的服务器 tick 数。
     * @return 外部实现接受并发送播放请求时返回 true。
     */
    fun spawnAtLocation(effectId: String, location: Location, lifetimeTicks: Int): Boolean
}
