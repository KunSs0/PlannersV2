import com.gitee.planners.core.attribute.AttributeProxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * 验证属性输出保留恢复率与耐力小数，且负值不会形成双符号。
 */
class AttributeTokenTest {

    /**
     * 覆盖小数、整数、负数与微小数，避免通用输出提前取整。
     */
    @Test
    fun preservesDecimalValuesAndSigns() {
        // 只测通用输出协议，不依赖任何服务端职业配置。
        val tokens = AttributeProxy.createTokens(linkedMapOf(
            "endurance" to 11.2,
            "health-regen" to 0.13,
            "mana-regen" to 0.36,
            "stamina-regen" to 1.01,
            "health" to 26.0,
            "strength" to -0.25,
            "small" to 0.0000001
        ))
        assertEquals(listOf("endurance: +11.2", "health-regen: +0.13", "mana-regen: +0.36",
            "stamina-regen: +1.01", "health: +26", "strength: -0.25", "small: +0.0000001"), tokens)
    }

    /**
     * 非有限属性必须明确失败，不能输出无法解析的 token。
     */
    @Test
    fun rejectsNonFiniteValues() {
        // 在写入外部插件前拒绝错误公式结果。
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                AttributeProxy.createTokens(mapOf("health" to value))
            }
        }
    }
}
