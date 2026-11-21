import com.gitee.planners.core.skill.cooler.CooldownGroups
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 共享冷却组的分组索引与覆盖规则。
 *
 * 组只由 `__option__.cooldown.group` 决定；写入时只延长不缩短，
 * 避免短冷却技能把同组的长冷却洗掉。
 */
class CooldownGroupTest {

    /** 同组技能归到一起，未声明组的技能不参与索引。 */
    @Test
    fun indexesMembersByGroup() {
        val index = CooldownGroups.indexById(
            linkedMapOf(
                "sage_prayer_rite_graceward" to "ritual",
                "knight_skill_8" to "ritual",
                "sage_prayer_tide_of_faith" to null,
                "sage_prayer_boon_echo" to "echo",
            ),
        )
        assertEquals(listOf("sage_prayer_rite_graceward", "knight_skill_8"), index["ritual"])
        assertEquals(listOf("sage_prayer_boon_echo"), index["echo"])
        assertFalse(index.containsKey("sage_prayer_tide_of_faith"), "未声明组不进入索引")
    }

    /** 索引保持声明顺序，供日志与调试按配置顺序阅读。 */
    @Test
    fun keepsDeclarationOrder() {
        val index = CooldownGroups.indexById(
            linkedMapOf(
                "b" to "group",
                "a" to "group",
                "c" to "group",
            ),
        )
        assertEquals(listOf("b", "a", "c"), index["group"])
    }

    /** 只有本次冷却比现有剩余更长时才覆盖。 */
    @Test
    fun extendsOnlyWhenLonger() {
        assertTrue(CooldownGroups.shouldExtend(0L, 12000), "无冷却时写入")
        assertTrue(CooldownGroups.shouldExtend(6000L, 12000), "剩余更短时延长")
        assertFalse(CooldownGroups.shouldExtend(12000L, 12000), "等长不重复写")
        assertFalse(CooldownGroups.shouldExtend(12000L, 6000), "更短不覆盖")
    }
}
