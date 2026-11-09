package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.module.configuration.Configuration

/**
 * 验证 `call` 把坐标目标绑定给子流程。
 *
 * 子流程里的 `@this` 应当指向调用方传入的那个坐标，而不是施法者。
 */
class CallLocationBindingTest {

    /** 每个用例前安装测试动作。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(CaptureTarget)
        CAPTURED = null
    }

    /** 构造带世界坐标的上下文。 */
    private fun scope(location: Location): QueryScope {
        return QueryScope(sender = location, label = "call-location")
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

    /** `call ... @this.loc` 的子流程能拿到该坐标。 */
    @Test
    fun callBindsLocationToChildFlow() {
        val world = Mockito.mock(World::class.java)
        val origin = Location(world, 4.0, 65.0, -3.0)
        val parsed = spec(
            """
            action:
              fire:
                - 'call{list=inner} @this.loc'
              inner:
                - 'capture{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", scope(origin))

        assertSame(origin, CAPTURED)
    }

    /** 目标器结果为空时子流程不执行。 */
    @Test
    fun emptyTargetSkipsChildFlow() {
        val parsed = spec(
            """
            action:
              fire:
                - 'call{list=inner} @this.loc'
              inner:
                - 'capture{}'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(label = "call-location"))

        assertEquals(null, CAPTURED)
    }

    /** 记录子流程收到的目标。 */
    private object CaptureTarget : Action {

        override val id = "capture"

        override val aliases = listOf("capture")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown()
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            CAPTURED = scope.target
        }
    }

    companion object {

        /** 最近一次子流程收到的目标。 */
        private var CAPTURED: Any? = null
    }
}
