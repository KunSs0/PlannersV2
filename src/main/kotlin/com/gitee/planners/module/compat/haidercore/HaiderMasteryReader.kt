package com.gitee.planners.module.compat.haidercore

import com.gitee.haidercore.module.mastery.api.MasteryAPI
import com.gitee.haidercore.module.mastery.model.MasterySettings
import com.gitee.haidercore.module.mastery.model.PlayerState
import taboolib.common.platform.Ghost
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** HaiderCore 强类型 API 适配，仅在确认插件可用后按需加载。 */
@Ghost
object HaiderMasteryReader : WeaponMasteryReader {
    /** 捕获当前配置并异步读取进度，不等待工作队列，也不缓存到下一次请求。 */
    override fun read(playerId: UUID): CompletableFuture<Map<String, Int>> {
        try {
            val settings = MasteryAPI.settings()
            return MasteryAPI.progress(playerId).thenApply { state ->
                levels(settings, state)
            }
        } catch (exception: Exception) {
            return CompletableFuture.failedFuture(exception)
        }
    }

    /** 按当前曲线换算全部已配置武器，未记录的武器使用累计经验零。 */
    fun levels(settings: MasterySettings, state: PlayerState): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()
        for (id in settings.weapons.types.keys) {
            val experience = state.weapons[id] ?: 0L
            result[id] = settings.experience.levelAt(experience)
        }
        return result
    }
}
