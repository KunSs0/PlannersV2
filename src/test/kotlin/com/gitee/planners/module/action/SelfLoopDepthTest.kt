package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证自循环流程的调用深度语义。
 *
 * 延迟续体作为新的执行根，`delay` + `call` 自循环不受同步嵌套深度限制；
 * 纯同步自调用仍然被深度保护拦下，防止配置写出无终止条件的递归。
 */
class SelfLoopDepthTest {

    /** 每个用例前恢复内置条目并注册计数动作。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(CountDownAction)
        ActionScheduler.install(InlineDispatcher)
    }

    /** 用例结束后卸载调度器。 */
    @AfterEach
    fun tearDown() {
        ActionScheduler.uninstall()
    }

    /** 延迟自循环超过同步深度上限仍能跑完，并按计数条件终止。 */
    @Test
    fun delayedSelfLoopRunsBeyondSyncDepthLimit() {
        val spec = spec(
            """
            action:
              loop:
                conditions: ['expr{e="skill.var.ticks > 0"}']
                actions:
                  - 'countdown{}'
                  - 'delay{ticks=1}'
                  - 'call{func="loop"}'
            """,
        )
        val variables = mutableScope(20)
        ActionRunner.run(spec, "loop", QueryScope(label = "test", variables = variables))
        assertEquals(20, variables["iterations"])
        assertEquals(0, variables["ticks"])
    }

    /** 纯同步自调用仍然受调用深度保护。 */
    @Test
    fun synchronousSelfCallStillHitsDepthLimit() {
        val spec = spec(
            """
            action:
              spin:
                - 'call{func="spin"}'
            """,
        )
        assertThrows(IllegalStateException::class.java) {
            ActionRunner.run(spec, "spin", QueryScope(label = "test", variables = mutableScope(0)))
        }
    }

    /** 构造带计数的可改变量表。 */
    private fun mutableScope(ticks: Int): LinkedHashMap<String, Any?> {
        val variables = LinkedHashMap<String, Any?>()
        variables["ticks"] = ticks
        variables["iterations"] = 0
        return variables
    }

    /** 读取一段 YAML 中的 `action` 节点。 */
    private fun spec(yaml: String): ActionSpec {
        val configuration = Configuration.loadFromString(yaml.trimIndent())
        val parsed = ActionParser.parse("loop", configuration.getConfigurationSection("action"), "action")
        if (parsed == null) {
            throw IllegalStateException("spec must not be null")
        }
        return parsed
    }

    /** 每执行一次就把 `ticks` 减一并累加 `iterations`，模拟流程内的计数器写入。 */
    object CountDownAction : Action {

        override val id = "countdown"

        override val aliases = listOf("countdown")

        /** 计数不依赖目标，空目标时也要执行。 */
        override val acceptsEmptyTargets = true

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            val variables = scope.variables
            if (variables !is MutableMap<*, *>) {
                throw IllegalStateException("countdown requires a mutable variable scope.")
            }
            @Suppress("UNCHECKED_CAST")
            val writable = variables as MutableMap<String, Any?>
            val ticks = (writable["ticks"] as? Number)?.toInt() ?: 0
            writable["ticks"] = ticks - 1
            val iterations = (writable["iterations"] as? Number)?.toInt() ?: 0
            writable["iterations"] = iterations + 1
        }
    }

    /** 同步执行延迟任务的调度器，用来在单测里直接推进循环。 */
    object InlineDispatcher : ActionScheduler.Dispatcher {

        override fun schedule(delayTicks: Long, periodTicks: Long, task: Runnable): ActionTask {
            task.run()
            return actionTask
        }

        /** 内联执行后没有可取消的挂起任务。 */
        private val actionTask = ActionTask { }
    }
}
