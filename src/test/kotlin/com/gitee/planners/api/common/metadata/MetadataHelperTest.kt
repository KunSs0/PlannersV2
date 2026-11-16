package com.gitee.planners.api.common.metadata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证元数据有效期统一按 tick 表达。
 *
 * 离线环境不运行 TabooLib 的序列化类型扫描，使用公开的 Unsaved 标记跳过类型注册表，
 * 只覆盖 tick 到毫秒绝对时刻的换算与过期判断。
 */
class MetadataHelperTest {

    /** 只用于跳过序列化类型检查的测试值。 */
    private class UnsavedValue : Metadata.Unsaved

    /** 有效期按每 tick 50 毫秒换算成毫秒绝对时刻。 */
    @Test
    fun convertsTimeoutTicksToMillis() {
        val before = System.currentTimeMillis()
        val metadata = metadataValue(UnsavedValue(), 160)
        val after = System.currentTimeMillis()

        // 160 tick 等于 8000 毫秒，失效时刻必须落在这段换算区间内。
        assertTrue(metadata.expiresAtMillis >= before + 8000)
        assertTrue(metadata.expiresAtMillis <= after + 8000)
        assertFalse(metadata.isTimeout())
    }

    /** -1 表示永久有效，不做 tick 换算。 */
    @Test
    fun keepsPermanentMetadata() {
        val metadata = metadataValue(UnsavedValue(), -1)

        assertEquals(-1L, metadata.expiresAtMillis)
        assertFalse(metadata.isTimeout())
    }

    /** 空值返回已过期的空元数据。 */
    @Test
    fun returnsExpiredPlaceholderForNull() {
        val metadata = metadataValue(null, 160)

        assertTrue(metadata.isTimeout())
    }
}
