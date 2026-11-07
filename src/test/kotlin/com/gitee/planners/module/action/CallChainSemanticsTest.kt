package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 固定 `call` 的执行语义：**顺序执行，不是 if/else**。
 *
 * 被调流程的条件各自独立求值；前一个流程改了状态，后一个流程会看到新状态。
 * 因此只有条件互斥（且不产生改变条件的副作用）时，`call` 链才等价于分支。
 * 需要真正二选一时，用互斥条件分别 `call`，或在前一个流程里自行改写状态。
 */
class CallChainSemanticsTest {

    /** 每个用例前安装测试替身。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(RecordStep)
        ActionRegistry.register(FakeMetaSet)
        STATE.clear()
        STEPS.clear()
    }

    /** 构造读取共享标记的上下文。 */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = Any(),
            dataReader = { key ->
                STATE[key]
            },
            skillFieldReader = { _, _ -> null },
            label = "call-chain",
        )
    }

    /** 构建结构。 */
    private fun spec(yaml: String): ActionSpec {
        val configuration = Configuration.loadFromString(yaml)
        val parsed = ActionParser.parse("chain", configuration.getConfigurationSection("action"), "action")
        if (parsed == null) {
            throw IllegalStateException("spec must not be null")
        }
        return parsed
    }

    /** 条件互斥时只有一个被调流程执行。 */
    @Test
    fun exclusiveConditionsRunOneBranch() {
        val yaml = """
            action:
              fire:
                - 'call{list=on_hit}'
                - 'call{list=on_miss}'
              on_hit:
                conditions:
                  - 'expr{e="data.flag == 1"}'
                actions:
                  - 'step{tag="hit"}'
              on_miss:
                conditions:
                  - 'expr{e="data.flag == null"}'
                actions:
                  - 'step{tag="miss"}'
        """.trimIndent()
        ActionRunner.run(spec(yaml), ActionSpec.FIRE, scope())
        assertEquals(listOf("miss"), STEPS)
    }

    /** 被调流程改写状态后，后续被调流程会看到新状态并一起执行。 */
    @Test
    fun mutableStateLeaksIntoFollowingCalls() {
        val yaml = """
            action:
              fire:
                - 'call{list=step0}'
                - 'call{list=step1}'
              step0:
                conditions:
                  - 'expr{e="data.x1.status == null"}'
                actions:
                  - 'step{tag="0"}'
                  - 'meta.set{key="x1.status"; value="1"}'
              step1:
                conditions:
                  - 'expr{e="data.x1.status == 1"}'
                actions:
                  - 'step{tag="1"}'
        """.trimIndent()
        ActionRunner.run(spec(yaml), ActionSpec.FIRE, scope())
        assertEquals(listOf("0", "1"), STEPS)
    }

    /** 分支改写状态后，同一轮里的下一个分支会立刻命中，形成级联。 */
    @Test
    fun mutatingBranchCascadesIntoNextCall() {
        val yaml = """
            action:
              fire:
                - 'call{list=on_a}'
                - 'call{list=on_b}'
              on_a:
                conditions:
                  - 'expr{e="data.turn == null || data.turn == 0"}'
                actions:
                  - 'step{tag="a"}'
                  - 'meta.set{key="turn"; value="1"}'
              on_b:
                conditions:
                  - 'expr{e="data.turn == 1"}'
                actions:
                  - 'step{tag="b"}'
                  - 'meta.set{key="turn"; value="0"}'
        """.trimIndent()
        val spec = spec(yaml)
        ActionRunner.run(spec, ActionSpec.FIRE, scope())
        assertEquals(listOf("a", "b"), STEPS)
    }

    /**
     * 记录流程分支的测试动作。
     */
    private object RecordStep : Action {

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

    /**
     * `meta.set` 的测试替身，写入内存映射。
     */
    private object FakeMetaSet : Action {

        override val id = "meta.set"

        override val aliases = listOf("meta.set")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("key")
            arguments.require("value")
            arguments.rejectUnknown("key", "value", "timeout")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val key = arguments.requireText(scope, "key")
            STATE[key] = arguments.requireText(scope, "value")
        }
    }

    private companion object {

        /** 共享标记。 */
        val STATE = LinkedHashMap<String, String>()

        /** 已执行的分支顺序。 */
        val STEPS = ArrayList<String>()
    }
}
