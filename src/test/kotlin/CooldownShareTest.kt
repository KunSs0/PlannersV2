import com.gitee.planners.core.skill.precondition.builtin.shouldExtend
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 共享冷却组的覆盖规则。
 *
 * 同组技能共享冷却时只延长不缩短，避免短冷却技能把组内的长冷却洗掉。
 */
class CooldownShareTest {

    /** 只有本次冷却比现有剩余更长时才覆盖。 */
    @Test
    fun extendsOnlyWhenLonger() {
        assertTrue(shouldExtend(0L, 12000), "无冷却时写入")
        assertTrue(shouldExtend(6000L, 12000), "剩余更短时延长")
        assertFalse(shouldExtend(12000L, 12000), "等长不重复写")
        assertFalse(shouldExtend(12000L, 6000), "更短不覆盖")
    }

    /** 负剩余按 0 处理，任何正冷却都会写入。 */
    @Test
    fun treatsNegativeRemainingAsFree() {
        assertTrue(shouldExtend(-1L, 1))
        assertFalse(shouldExtend(1L, 1))
    }
}
