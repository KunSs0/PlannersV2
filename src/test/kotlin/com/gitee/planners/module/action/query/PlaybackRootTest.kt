package com.gitee.planners.module.action.query

import com.gitee.planners.module.action.ActionCombatFacts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 验证 `playback.*` 查询根。
 *
 * 播放变量由宿主注入的读取器提供；未安装时恒为 null，因此没有播放上下文时
 * 读取不会误判为「已标记」。
 */
class PlaybackRootTest {

    /** 每个用例后恢复默认读取器。 */
    @AfterEach
    fun cleanup() {
        ActionCombatFacts.reset()
    }

    /** 未安装读取器时恒为 null。 */
    @Test
    fun defaultIsNull() {
        val ref = QueryRef.parse("playback.flag", "test")
        assertNull(ref.resolve(scope()))
    }

    /** 安装后按读取器结果返回。 */
    @Test
    fun readsInstalledValue() {
        ActionCombatFacts.installPlayback { _, key ->
            if (key == "knight.emblem-prayer.started") {
                true
            } else {
                null
            }
        }
        val ref = QueryRef.parse("playback.knight.emblem-prayer.started", "test")
        assertEquals(true, ref.resolve(scope()))
    }

    /** 可用于条件表达式。 */
    @Test
    fun worksInConditionExpression() {
        ActionCombatFacts.installPlayback { _, _ -> true }
        val ref = QueryRef.parseExpression("playback.done != true", "test")
        assertEquals(false, ref.resolveBoolean(scope()))
    }

    /** 读取器收到的是完整载荷。 */
    @Test
    fun readerReceivesPayload() {
        ActionCombatFacts.installPlayback { payload, key ->
            payload["marker"].toString() + ":" + key
        }
        val ref = QueryRef.parse("playback.x", "test")
        assertEquals("seen:x", ref.resolve(scope(mapOf("marker" to "seen"))))
    }


    /** 构造带播放读取器的上下文。 */
    private fun scope(payload: Map<String, Any?> = mapOf("marker" to "seen")): QueryScope {
        return QueryScope(
            payload = payload,
            playbackReader = { p, key -> ActionCombatFacts.playbackReaderFunction(p, key) },
            label = "test",
        )
    }
}
