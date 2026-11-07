package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证动作在执行过程中回调另一个流程的能力。
 *
 * 这是逐目标命中效果的基础：动作拿到 `flowRunner` 后，可以为每个实际受创目标
 * 绑定目标再跑一段流程，因此流程内的 `<target.*>` 与默认目标都指向该目标。
 */
class FlowRunnerTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(CallRunnerAction)
        ActionRegistry.register(RecordAction)
        HITS.clear()
        SEEN_TARGETS.clear()
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

    /** 动作能拿到流程跳转入口。 */
    @Test
    fun actionReceivesFlowRunner() {
        val yaml = """
            fire:
              - 'run-flow{flow="on_hit"}'
            on_hit:
              - 'record{tag="hit"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")
        assertNotNull(spec)
        ActionRunner.run(spec!!, ActionSpec.FIRE, blankScope())
        assertEquals(listOf("hit"), HITS)
    }

    /** 跳转时传入的目标对后续流程可见。 */
    @Test
    fun flowRunnerCarriesTarget() {
        val yaml = """
            fire:
              - 'run-flow{flow="on_hit"}'
            on_hit:
              - 'record{tag="hit"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")!!
        ActionRunner.run(spec, ActionSpec.FIRE, blankScope())
        assertEquals(1, SEEN_TARGETS.size)
        assertEquals("bound", SEEN_TARGETS[0])
    }

    /** 跳转到不存在的流程时中止当前动作，不向外抛异常。 */
    @Test
    fun unknownFlowAbortsAction() {
        val yaml = """
            fire:
              - 'run-flow{flow="missing"}'
              - 'record{tag="after"}'
        """.trimIndent()
        val spec = ActionParser.parse("test", section(yaml + "\n"), "action")!!
        ActionRunner.run(spec, ActionSpec.FIRE, blankScope())
        assertTrue(HITS.isEmpty())
    }

    /** 没有流程跳转入口时返回 null，由动作自行处理。 */
    @Test
    fun scopeWithoutRunnerExposesNull() {
        val scope = QueryScope(label = "test")
        assertNull(scope.flowRunner)
    }

    /** 构造最小上下文。 */
    private fun blankScope(): QueryScope {
        return QueryScope(label = "test")
    }

    /**
     * 测试用动作：调用运行时注入的流程入口，并绑定一个目标。
     */
    private object CallRunnerAction : Action {

        override val id = "run-flow"

        override val aliases = listOf("run-flow")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("flow")
            arguments.rejectUnknown("flow")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val runner = scope.flowRunner
            if (runner == null) {
                throw IllegalStateException("missing flow runner")
            }
            val flowId = arguments.requireText(scope, "flow")
            runner(flowId, scope.withTarget("bound"))
        }
    }

    /**
     * 测试用动作：记录调用与当前目标。
     */
    private object RecordAction : Action {

        override val id = "record"

        override val aliases = listOf("record")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            HITS.add(arguments.requireText(scope, "tag"))
            val target = scope.target
            if (target != null) {
                SEEN_TARGETS.add(target.toString())
            }
        }
    }

    private companion object {

        /** 已执行的流程动作。 */
        val HITS = ArrayList<String>()

        /** 流程执行时看到的目标。 */
        val SEEN_TARGETS = ArrayList<String>()
    }
}
