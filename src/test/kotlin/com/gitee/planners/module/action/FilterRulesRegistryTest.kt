package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.action.rule.FilterRule
import com.gitee.planners.module.action.rule.FilterRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 验证过滤规则注册表的登记、查找与撤销。
 *
 * 外部插件（例如 FightCore 的 `buff.has`）按所有者登记规则，停用时整体撤销；
 * 规则主键按域分组，因此不提供别名。
 */
class FilterRulesRegistryTest {

    /** 每个用例前恢复内置规则。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 内置规则按主键可查。 */
    @Test
    fun registersBuiltinRules() {
        assertNotNull(FilterRules.rule("health.damaged"))
        assertEquals("health.damaged", FilterRules.require("health.damaged", "test").id)
    }

    /** 主键大小写不敏感。 */
    @Test
    fun lookupIgnoresCase() {
        assertNotNull(FilterRules.rule("HEALTH.DAMAGED"))
    }

    /** 未登记的主键报错并列出可用规则。 */
    @Test
    fun reportsUnknownRuleWithAvailableIds() {
        val error = assertThrows<IllegalArgumentException> {
            FilterRules.require("health.missing", "skill.test")
        }
        assertTrue(error.message!!.startsWith("skill.test references unknown filter rule 'health.missing'"))
        assertTrue(error.message!!.contains("health.damaged"))
    }

    /** 外部登记的规则可按所有者整体撤销。 */
    @Test
    fun unregistersExternalRules() {
        val owner = Any()
        FilterRules.registerExternal(owner, NoopRule("fc.test"))
        assertNotNull(FilterRules.rule("fc.test"))
        assertEquals(1, FilterRules.externalOwnerCount())
        FilterRules.unregisterAll(owner)
        assertNull(FilterRules.rule("fc.test"))
        assertEquals(0, FilterRules.externalOwnerCount())
    }

    /** 撤销只影响该所有者，内置规则保留。 */
    @Test
    fun unregisterKeepsBuiltinsAndOtherOwners() {
        val first = Any()
        val second = Any()
        FilterRules.registerExternal(first, NoopRule("fc.first"))
        FilterRules.registerExternal(second, NoopRule("fc.second"))
        FilterRules.unregisterAll(first)
        assertNull(FilterRules.rule("fc.first"))
        assertNotNull(FilterRules.rule("fc.second"))
        assertNotNull(FilterRules.rule("health.damaged"))
    }

    /** 同一主键重复登记时后者覆盖前者。 */
    @Test
    fun laterRegistrationOverridesSameId() {
        val owner = Any()
        FilterRules.registerExternal(owner, NoopRule("fc.test"))
        val override = NoopRule("fc.test")
        FilterRules.registerExternal(owner, override)
        assertEquals(override, FilterRules.rule("fc.test"))
    }

    /** 重复撤销同一所有者无副作用。 */
    @Test
    fun unregisterIsIdempotent() {
        val owner = Any()
        FilterRules.registerExternal(owner, NoopRule("fc.test"))
        FilterRules.unregisterAll(owner)
        FilterRules.unregisterAll(owner)
        assertNull(FilterRules.rule("fc.test"))
    }

    /** 重置恢复内置规则并清空外部所有者。 */
    @Test
    fun resetRestoresBuiltins() {
        val owner = Any()
        FilterRules.registerExternal(owner, NoopRule("fc.test"))
        FilterRules.reset()
        assertNull(FilterRules.rule("fc.test"))
        assertEquals(0, FilterRules.externalOwnerCount())
        assertNotNull(FilterRules.rule("health.alive"))
    }
}

/**
 * 不做任何判定的规则，用于注册表用例。
 *
 * @property id 规则主键。
 */
private class NoopRule(override val id: String) : FilterRule {

    override fun test(scope: QueryScope, target: ActionTarget, arguments: ActionArguments, compiled: Any?): Boolean {
        return true
    }
}
