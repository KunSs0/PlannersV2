import com.gitee.planners.core.skill.SkillPointsPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type

/**
 * 验证公共整数规格的等级累计、阶段奖励和严格配置边界。
 */
class SkillPointsPolicyTest {
    /**
     * Lv1 包含每级发放，阶段奖励只累计一次，跨级差额保持不变。
     */
    @Test
    fun accumulatesBaseAndMilestones() {
        val policy = policy("""
            per-level:
              base: 2
            bonuses:
              '5': {base: 4}
              '10': {base: 8}
        """.trimIndent())
        assertEquals(0, policy.accumulated(0))
        assertEquals(2, policy.accumulated(1))
        assertEquals(14, policy.accumulated(5))
        assertEquals(32, policy.accumulated(10))
        assertEquals(6, policy.accumulated(5) - policy.accumulated(4))
        assertEquals(10, policy.accumulated(10) - policy.accumulated(9))
    }

    /**
     * 条件使用每次循环的等级，整数在全部修正完成后截断。
     */
    @Test
    fun evaluatesLevelConditionsAndOrderedOperations() {
        val policy = policy("""
            per-level:
              base: 2
              modifiers:
                early:
                  condition:
                    type: level
                    props: {min: 1, max: 30}
                  operation: '+1'
                half: {operation: '/2'}
                double: {operation: '*2'}
            bonuses: {}
        """.trimIndent())
        assertEquals(90, policy.accumulated(30))
        assertEquals(92, policy.accumulated(31))
    }

    /**
     * 旧公式、非法条件、动态依赖及未支持结构必须被明确拒绝。
     */
    @Test
    fun rejectsInvalidDefinitions() {
        val values = listOf(
            "per-level: 'if (level <= 30) 3 else 2'\nbonuses: {}",
            "per-level: {base: 2, update: [permission]}\nbonuses: {}",
            "per-level: {base: 2, modifiers: {bad: {operation: '/0'}}}\nbonuses: {}",
            "per-level: {base: -1}\nbonuses: {}",
            "per-level: {base: 2}\nbonuses: {'0': {base: 1}}",
            "per-level: {base: 2, modifiers: {bad: {condition: {type: permission}, operation: '+1'}}}\nbonuses: {}",
            "per-level: {base: 2, repeat: 2}\nbonuses: {}",
            "per-level: {base: 2, modifiers: {bad: {condition: {type: level, props: {min: 1}}, operation: '+1'}}}\nbonuses: {}"
        )
        for (raw in values) {
            val error = assertThrows(IllegalArgumentException::class.java) { policy(raw) }
            assertTrue(error.message!!.contains("settings.skill-points"))
        }
    }

    /**
     * 新配置创建新策略后使用新数值，旧实例不受污染。
     */
    @Test
    fun replacesConfigurationWithoutSharingState() {
        val old = policy("per-level: {base: 2}\nbonuses: {}")
        val fresh = policy("per-level: {base: 3}\nbonuses: {}")
        assertEquals(10, old.accumulated(5))
        assertEquals(15, fresh.accumulated(5))
    }

    /**
     * 使用合成配置解析独立规则，不读取本服业务文件。
     *
     * @param raw YAML 文本。
     * @return 发放规则。
     */
    private fun policy(raw: String): SkillPointsPolicy {
        return SkillPointsPolicy.parse(Configuration.loadFromString(raw, Type.YAML))
    }
}
