package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证触发器索引的构建与派发。
 */
class ActionTriggerIndexTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(CountingAction)
        HITS.clear()
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

    /** 构建单技能结构。 */
    private fun spec(id: String, yaml: String): ActionSpec {
        val parsed = ActionParser.parse(id, section(yaml + "\n"), "action")
        if (parsed == null) {
            throw IllegalStateException("spec must not be null")
        }
        return parsed
    }

    /** 索引按触发器归并技能。 */
    @Test
    fun indexesSpecsByTrigger() {
        val first = spec("s1", "server:\n- 'count{tag=\"a\"} ~on_quit'")
        val second = spec("s2", "server:\n- 'count{tag=\"b\"} ~on_quit'\n- 'count{tag=\"c\"} ~on_skill_cast'")
        val index = ActionTriggerIndex.build(listOf(first, second))
        assertTrue(index.hasTrigger("on_quit"))
        assertEquals(2, index.specsFor("on_quit").size)
        assertEquals(1, index.specsFor("on_skill_cast").size)
        assertFalse(index.hasTrigger("on_level"))
    }

    /** 定时器行不进入事件索引。 */
    @Test
    fun ignoresTimerLines() {
        val spec = spec("s1", "fire:\n- 'count{tag=\"a\"} ~onTimer{ticks=20}'")
        val index = ActionTriggerIndex.build(listOf(spec))
        assertFalse(index.hasTrigger("onTimer"))
        assertTrue(index.triggerIds().isEmpty())
    }

    /** 派发只命中声明了该触发器的技能。 */
    @Test
    fun dispatchesOnlyMatchingSpecs() {
        val first = spec("s1", "server:\n- 'count{tag=\"a\"} ~on_quit'")
        val second = spec("s2", "server:\n- 'count{tag=\"b\"} ~on_skill_cast'")
        val index = ActionTriggerIndex.build(listOf(first, second))
        val executed = index.dispatch("on_quit") { QueryScope(label = "test") }
        assertEquals(1, executed)
        assertEquals(listOf("a"), HITS)
    }

    /** 同一触发器的多个技能都会派发。 */
    @Test
    fun dispatchesAllMatchingSpecs() {
        val first = spec("s1", "server:\n- 'count{tag=\"a\"} ~on_quit'")
        val second = spec("s2", "server:\n- 'count{tag=\"b\"} ~on_quit'")
        val index = ActionTriggerIndex.build(listOf(first, second))
        val executed = index.dispatch("on_quit") { QueryScope(label = "test") }
        assertEquals(2, executed)
        assertEquals(listOf("a", "b"), HITS)
    }

    /** 未声明的触发器派发不产生任何执行。 */
    @Test
    fun dispatchesNothingForUnknownTrigger() {
        val spec = spec("s1", "server:\n- 'count{tag=\"a\"} ~on_quit'")
        val index = ActionTriggerIndex.build(listOf(spec))
        val executed = index.dispatch("on_level") { QueryScope(label = "test") }
        assertEquals(0, executed)
        assertTrue(HITS.isEmpty())
    }

    /**
     * 记录调用参数的测试动作。
     */
    private object CountingAction : Action {

        override val id = "count"

        override val aliases = listOf("count")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            HITS.add(arguments.requireText(scope, "tag"))
        }
    }

    private companion object {

        /** 记录动作调用顺序。 */
        val HITS = ArrayList<String>()
    }
}
