package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 验证目标槽位两份缓存始终一致。
 *
 * `@target.finder` 读 TargetCache，`finder.<名字>.count` 读 FinderCache；
 * 两个写入动作都必须同时更新两份视图，否则会出现“名单正确、计数是上一跳”的错账。
 */
class TargetCacheSyncTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** target.finder.set 同时写 TargetCache 与 FinderCache。 */
    @Test
    fun targetFinderSetWritesBothCaches() {
        val scope = QueryScope()
        val targets = listOf<ActionTarget>(location())
        BuiltinActions.TargetFinderSet.execute(scope, ActionArguments(mapOf("id" to "slot"), "test.slot"), targets)
        assertEquals(1, scope.targetCache.load("slot")!!.size)
        assertEquals(1, scope.finderCache["slot"]!!.size)
    }

    /** finder.set 同时写 FinderCache 与 TargetCache。 */
    @Test
    fun finderSetWritesBothCaches() {
        val scope = QueryScope()
        val targets = listOf<ActionTarget>(location())
        FinderSetAction.execute(scope, ActionArguments(mapOf("id" to "slot"), "test.slot"), targets)
        assertEquals(1, scope.finderCache["slot"]!!.size)
        assertEquals(1, scope.targetCache.load("slot")!!.size)
    }

    /** 后写入的空快照会同时清掉两份视图，不留下上一跳的人数。 */
    @Test
    fun emptySnapshotClearsBothCaches() {
        val scope = QueryScope()
        val targets = listOf<ActionTarget>(location())
        BuiltinActions.TargetFinderSet.execute(scope, ActionArguments(mapOf("id" to "slot"), "test.slot"), targets)
        BuiltinActions.TargetFinderSet.execute(scope, ActionArguments(mapOf("id" to "slot"), "test.slot"), emptyList())
        assertEquals(0, scope.targetCache.load("slot")!!.size)
        assertEquals(0, scope.finderCache["slot"]!!.size)
    }

    /** 位置目标只用于占位，不参与任何实体判据。 */
    private fun location(): ActionTarget {
        return ActionTarget.LocationTarget(Location(null, 0.0, 0.0, 0.0))
    }
}
