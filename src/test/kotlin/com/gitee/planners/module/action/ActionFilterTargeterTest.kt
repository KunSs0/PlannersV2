package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.action.rule.FilterRule
import com.gitee.planners.module.action.rule.FilterRules
import com.gitee.planners.module.action.rule.FilterTargeter
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

/**
 * 验证 `@filter` 的多规则语义。
 *
 * 断言重点是“按书写顺序逐条过滤”：后一条规则只看到前一条的存活元素，容器清空后不再求值。
 * 规则本身只做逐元素判据，因此这里用不依赖实体状态的测试规则，真实实体判据由内置规则承担。
 */
class ActionFilterTargeterTest {

    private val scope = QueryScope()

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 规则按书写顺序依次过滤，后续规则看不到已被淘汰的元素。 */
    @Test
    fun appliesRulesInDeclaredOrder() {
        val first = location(0.0)
        val second = location(1.0)
        val third = location(2.0)
        val fourth = location(3.0)
        val seenBySecond = mutableListOf<ActionTarget>()
        register(AllowingRule("test.keep-three", setOf(first, second, third), mutableListOf()))
        register(AllowingRule("test.keep-two", setOf(second, third), seenBySecond))
        val arguments = ActionArguments(mapOf("r" to "test.keep-three,test.keep-two"), "skill.test.pulse[0]")
        FilterTargeter.validate(arguments)
        val result = FilterTargeter.resolve(scope, arguments, listOf(first, second, third, fourth))
        assertEquals(listOf(second, third), result)
        assertEquals(listOf(first, second, third), seenBySecond)
    }

    /** 容器清空后不再求值后续规则。 */
    @Test
    fun stopsWhenContainerBecomesEmpty() {
        val target = location(0.0)
        val seenBySecond = mutableListOf<ActionTarget>()
        register(AllowingRule("test.keep-none", emptySet(), mutableListOf()))
        register(AllowingRule("test.keep-all", setOf(target), seenBySecond))
        val arguments = ActionArguments(mapOf("r" to "test.keep-none,test.keep-all"), "skill.test.pulse[0]")
        val result = FilterTargeter.resolve(scope, arguments, listOf(target))
        assertTrue(result.isEmpty())
        assertTrue(seenBySecond.isEmpty())
    }

    /** 结果顺序与上游一致，规则不参与排序。 */
    @Test
    fun keepsUpstreamOrder() {
        val targets = listOf(location(0.0), location(1.0), location(2.0))
        val kept = setOf(targets[2], targets[0])
        register(AllowingRule("test.keep-outer", kept, mutableListOf()))
        val arguments = ActionArguments(mapOf("r" to "test.keep-outer"), "skill.test.pulse[0]")
        assertEquals(listOf(targets[0], targets[2]), FilterTargeter.resolve(scope, arguments, targets))
    }

    /** 重复写同一条规则只求值一次。 */
    @Test
    fun deduplicatesRuleList() {
        val target = location(0.0)
        val seen = mutableListOf<ActionTarget>()
        register(AllowingRule("test.keep-all", setOf(target), seen))
        val arguments = ActionArguments(mapOf("r" to "test.keep-all, test.keep-all"), "skill.test.pulse[0]")
        FilterTargeter.resolve(scope, arguments, listOf(target))
        assertEquals(1, seen.size)
    }

    /** 规则主键大小写不敏感。 */
    @Test
    fun acceptsRuleIdInAnyCase() {
        val arguments = ActionArguments(mapOf("r" to "HEALTH.ALIVE"), "skill.test.pulse[0]")
        FilterTargeter.validate(arguments)
        assertEquals("health.alive", FilterRules.require("HEALTH.ALIVE", "skill.test.pulse[0]").id)
    }

    /** 未知规则在加载期报错，并列出可用规则。 */
    @Test
    fun rejectsUnknownRuleAtLoadTime() {
        val arguments = ActionArguments(mapOf("r" to "health.none"), "skill.test.pulse[0]")
        val error = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(arguments)
        }
        assertTrue(error.message!!.contains("unknown filter rule 'health.none'"))
        assertTrue(error.message!!.contains("health.damaged"))
    }

    /** 规则列表必须是加载期可解析的字面量。 */
    @Test
    fun rejectsDynamicRuleList() {
        val arguments = ActionArguments(mapOf("r" to "<skill.var.rules>"), "skill.test.pulse[0]")
        val error = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(arguments)
        }
        assertTrue(error.message!!.contains("加载期可解析"))
    }

    /** 规则列表缺失或为空都拒绝。 */
    @Test
    fun requiresNonEmptyRuleList() {
        val missing = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(ActionArguments(emptyMap(), "skill.test.pulse[0]"))
        }
        assertEquals("skill.test.pulse[0] requires argument 'r'.", missing.message)
        val blank = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(ActionArguments(mapOf("r" to " , "), "skill.test.pulse[0]"))
        }
        assertTrue(blank.message!!.contains("至少要写一条规则"))
    }

    /** 多规则行不接受额外参数，避免参数归属歧义。 */
    @Test
    fun rejectsExtraArgumentsWithMultipleRules() {
        val arguments = ActionArguments(mapOf("r" to "test.a,test.b", "max" to "1.0"), "skill.test.pulse[0]")
        val error = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(arguments)
        }
        assertTrue(error.message!!.contains("多规则过滤不接受额外参数"))
    }

    /** 单规则行的参数整套转发给规则校验。 */
    @Test
    fun forwardsArgumentsToSingleRule() {
        register(RatioLikeRule())
        val missing = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(ActionArguments(mapOf("r" to "test.ratio"), "skill.test.pulse[0]"))
        }
        assertEquals("skill.test.pulse[0] requires argument 'max'.", missing.message)
        val unknown = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(
                ActionArguments(mapOf("r" to "test.ratio", "max" to "1.0", "ratio" to "2.0"), "skill.test.pulse[0]"),
            )
        }
        assertEquals("skill.test.pulse[0] contains unsupported argument 'ratio'.", unknown.message)
        FilterTargeter.validate(
            ActionArguments(mapOf("r" to "test.ratio", "max" to "1.0"), "skill.test.pulse[0]"),
        )
    }

    /** 没有实体句柄的位置目标不参与实体规则。 */
    @Test
    fun dropsLocationTargets() {
        val arguments = ActionArguments(mapOf("r" to "health.alive"), "skill.test.pulse[0]")
        assertTrue(FilterTargeter.resolve(scope, arguments, listOf(location(0.0))).isEmpty())
    }

    /** health.damaged 只保留严格缺血的目标，满血与死亡都被排除。 */
    @Test
    fun keepsOnlyStrictlyDamagedTargets() {
        val full = entity(100.0, 100.0)
        val damaged = entity(40.0, 100.0)
        val barelyDamaged = entity(99.9999, 100.0)
        val dead = entity(0.0, 100.0)
        val arguments = ActionArguments(mapOf("r" to "health.damaged"), "skill.test.pulse[0]")
        val result = FilterTargeter.resolve(scope, arguments, listOf(full, damaged, barelyDamaged, dead))
        assertEquals(listOf(damaged, barelyDamaged), result)
    }

    /** health.full 只保留满血目标。 */
    @Test
    fun keepsOnlyFullHealthTargets() {
        val full = entity(100.0, 100.0)
        val damaged = entity(99.0, 100.0)
        val arguments = ActionArguments(mapOf("r" to "health.full"), "skill.test.pulse[0]")
        assertEquals(listOf(full), FilterTargeter.resolve(scope, arguments, listOf(full, damaged)))
    }

    /** health.ratio 的 max 与旧 wounded 一样是闭区间上界。 */
    @Test
    fun keepsTargetsAtOrBelowRatio() {
        val half = entity(50.0, 100.0)
        val justAbove = entity(50.1, 100.0)
        val full = entity(100.0, 100.0)
        val arguments = ActionArguments(mapOf("r" to "health.ratio", "max" to "0.5"), "skill.test.pulse[0]")
        FilterTargeter.validate(arguments)
        assertEquals(listOf(half), FilterTargeter.resolve(scope, arguments, listOf(half, justAbove, full)))
    }

    /** health.ratio 的 min 用于“缺血但不濒死”区间。 */
    @Test
    fun keepsTargetsAboveMinRatio() {
        val dying = entity(10.0, 100.0)
        val wounded = entity(30.0, 100.0)
        val healthy = entity(90.0, 100.0)
        val arguments = ActionArguments(
            mapOf("r" to "health.ratio", "min" to "0.2", "max" to "0.5"),
            "skill.test.pulse[0]",
        )
        assertEquals(listOf(wounded), FilterTargeter.resolve(scope, arguments, listOf(dying, wounded, healthy)))
    }

    /** 区间参数缺失、越界或倒置都在加载期报错。 */
    @Test
    fun validatesRatioBounds() {
        val missing = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(ActionArguments(mapOf("r" to "health.ratio"), "skill.test.ratio"))
        }
        assertTrue(missing.message!!.contains("需要 min 或 max 至少一项"))
        val outOfRange = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(ActionArguments(mapOf("r" to "health.ratio", "max" to "1.5"), "skill.test.ratio"))
        }
        assertTrue(outOfRange.message!!.contains("必须在 (0, 1] 区间"))
        val inverted = assertThrows<IllegalArgumentException> {
            FilterTargeter.validate(
                ActionArguments(mapOf("r" to "health.ratio", "min" to "0.6", "max" to "0.2"), "skill.test.ratio"),
            )
        }
        assertTrue(inverted.message!!.contains("min 不能大于 max"))
    }

    /** health.alive 与 @alive 同口径，只看存活状态。 */
    @Test
    fun keepsActiveTargetsOnly() {
        val active = entity(10.0, 100.0)
        val inactive = entity(10.0, 100.0, activeState = false)
        val arguments = ActionArguments(mapOf("r" to "health.alive"), "skill.test.pulse[0]")
        assertEquals(listOf(active), FilterTargeter.resolve(scope, arguments, listOf(active, inactive)))
    }

    /** 多规则组合按顺序叠加，缺血与存活同时成立才保留。 */
    @Test
    fun combinesRulesInOrder() {
        val full = entity(100.0, 100.0)
        val damaged = entity(40.0, 100.0)
        val arguments = ActionArguments(mapOf("r" to "health.alive, health.damaged"), "skill.test.pulse[0]")
        FilterTargeter.validate(arguments)
        assertEquals(listOf(damaged), FilterTargeter.resolve(scope, arguments, listOf(full, damaged)))
    }

    /** 内置规则都已登记。 */
    @Test
    fun registersBuiltinRules() {
        assertFalse(FilterRules.ids.isEmpty())
        assertTrue(FilterRules.ids.containsAll(
            listOf("health.alive", "health.damaged", "health.full", "health.ratio", "team.ally", "team.enemy"),
        ))
    }

    /** 目标器链能解析 filter 步骤，多规则列表按书写顺序保留。 */
    @Test
    fun parsesThroughTargeterChain() {
        val steps = ActionTargeterChainParser.parseText(
            "@range{radius=18} @filter{r=team.ally, health.damaged} @limit{n=3}",
            "skill.test.chain",
        )
        assertEquals(3, steps.size)
        assertEquals("filter", steps[1].targeter.id)
        assertEquals(listOf("team.ally", "health.damaged"), steps[1].arguments.list(scope, "r"))
    }

    /** 未知规则经目标器链解析时在加载期报错。 */
    @Test
    fun reportsUnknownRuleThroughChain() {
        val error = assertThrows<IllegalArgumentException> {
            ActionTargeterChainParser.parseText("@filter{r=health.none}", "skill.test.chain")
        }
        assertTrue(error.message!!.contains("unknown filter rule 'health.none'"))
    }

    /** 登记测试规则。 */
    private fun register(rule: FilterRule) {
        FilterRules.register(rule)
    }

    /** 位置目标工厂，坐标只用于区分实例。 */
    private fun location(x: Double): ActionTarget {
        return ActionTarget.LocationTarget(Location(null, x, 0.0, 0.0))
    }

    /** 构造带生命值与存活状态的实体目标。 */
    private fun entity(health: Double, maximum: Double, activeState: Boolean = true): ActionTarget {
        val living = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(living.isValid).thenReturn(activeState)
        Mockito.`when`(living.isDead).thenReturn(!activeState)
        Mockito.`when`(living.health).thenReturn(health)
        Mockito.`when`(living.maxHealth).thenReturn(maximum)
        return ActionTarget.EntityTarget(living)
    }
}

/**
 * 允许固定集合的测试规则，并记录自己看到的元素。
 *
 * @property id 规则主键。
 * @property allowed 允许通过的目标集合。
 * @property seen 求值过的目标，按调用顺序记录。
 */
private class AllowingRule(
    override val id: String,
    private val allowed: Set<ActionTarget>,
    private val seen: MutableList<ActionTarget>,
) : FilterRule {

    override fun test(scope: QueryScope, target: ActionTarget, arguments: ActionArguments, compiled: Any?): Boolean {
        seen.add(target)
        return allowed.contains(target)
    }
}

/** 带必填参数的测试规则，用于验证参数转发。 */
private class RatioLikeRule : FilterRule {

    override val id = "test.ratio"

    override fun validate(arguments: ActionArguments) {
        arguments.require("max")
        arguments.rejectUnknown("max")
    }

    override fun test(scope: QueryScope, target: ActionTarget, arguments: ActionArguments, compiled: Any?): Boolean {
        return true
    }
}
