package com.gitee.planners.module.compat.haidercore

import java.util.UUID
import java.util.concurrent.CompletableFuture

/** 异步读取全部已配置武器类型的熟练度等级；失败必须异常结束。 */
fun interface WeaponMasteryReader {
    /** @return 武器类型 ID 到当前曲线等级的完整快照，包含尚无进度的类型。 */
    fun read(playerId: UUID): CompletableFuture<Map<String, Int>>
}
