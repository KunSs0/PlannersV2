package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证 `once` 条件的一次性语义。
 *
 * 覆盖两个真实场景：逐目标流程里只对首个目标成立，以及「最多 N 次」的用法。
 */
class OnceConditionTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(StepAction)
        STEPS.clear()
    }

    /** 读取一段 YAML 中的 `action` 节点。 */
    private fun section(yaml: String): taboolib.library.configuration.ConfigurationSection {
        val builder = StringBuilder()
        builder.append("action:\n")
        for (line in yaml.trimEnd().lines()) {
            if (line.isEmpty()) {
                builder.append('\n')
                continue
            }
            builder.append("  ")
            builder.append(line)
            builder.append('\n')
        }
        val configuration = Configuration.loadFromString(builder.toString())
        val result = configuration.getConfigurationSection("action")
        if (result == null) {
            throw IllegalStateException("action section missing")
        }
        return result
    }

    /** 同一作用域内第二次调用返回 false。 */
    @Test
    fun holdsOnlyOncePerScope() {
        val scope = QueryScope(label = "test")
        val entry = ActionParser.parseCondition("once{key=\"a\"}", "test")
        assertTrue(entry.matches(scope))
        assertFalse(entry.matches(scope))
    }

    /** 不同键互不影响。 */
    @Test
    fun differentKeysAreIndependent() {
        val scope = QueryScope(label = "test")
        val first = ActionParser.parseCondition("once{key=\"a\"}", "test")
        val second = ActionParser.parseCondition("once{key=\"b\"}", "test")
        assertTrue(first.matches(scope))
        assertTrue(second.matches(scope))
        assertFalse(first.matches(scope))
    }

    /** 派生作用域共享同一份计数。 */
    @Test
    fun derivedScopesShareCounter() {
        val parent = QueryScope(label = "test")
        val entry = ActionParser.parseCondition("once{key=\"a\"}", "test")
        assertTrue(entry.matches(parent))
        assertFalse(entry.matches(parent.withTarget("mob")))
    }

    /** 新的一次执行重新计数。 */
    @Test
    fun freshScopeStartsOver() {
        val entry = ActionParser.parseCondition("once{key=\"a\"}", "test")
        assertTrue(entry.matches(QueryScope(label = "test")))
        assertTrue(entry.matches(QueryScope(label = "test")))
    }

    /** 逐目标流程里只对第一个目标成立。 */
    @Test
    fun appliesToFirstTargetOnly() {
        val yaml = """
            fire:
              - 'call{func=on_hit}'
            on_hit:
              conditions:
                - 'once{key="ulp"}'
              actions:
                - 'step{tag="reward"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")!!
        val scope = QueryScope(label = "test")
        for (index in 0 until 3) {
            ActionRunner.run(spec, "on_hit", scope.withTarget("mob$index"))
        }
        assertEquals(listOf("reward"), STEPS)
    }

    /** 三个不同键可以实现「最多三次」。 */
    @Test
    fun threeKeysAllowThreeHits() {
        val yaml = """
            fire:
              - 'call{func=sound1}'
              - 'call{func=sound2}'
              - 'call{func=sound3}'
            sound1:
              conditions:
                - 'once{key="s1"}'
              actions:
                - 'step{tag="s1"}'
            sound2:
              conditions:
                - 'once{key="s2"}'
              actions:
                - 'step{tag="s2"}'
            sound3:
              conditions:
                - 'once{key="s3"}'
              actions:
                - 'step{tag="s3"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")!!
        ActionRunner.run(spec, ActionSpec.FIRE, QueryScope(label = "test"))
        assertEquals(3, STEPS.size)
    }

    /**
     * 记录调用的测试动作。
     */
    private object StepAction : Action {

        override val id = "step"

        override val aliases = listOf("step")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            STEPS.add(arguments.requireText(scope, "tag"))
        }
    }

    private companion object {

        /** 已执行的动作标签。 */
        val STEPS = ArrayList<String>()
    }
}
