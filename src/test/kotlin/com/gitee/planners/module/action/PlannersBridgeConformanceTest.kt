package com.gitee.planners.module.action

import com.gitee.action.api.Action as ActionApi
import com.gitee.action.api.model.ActionArguments as ApiArguments
import com.gitee.action.api.model.ActionSpecs
import com.gitee.action.api.spi.HostServices
import com.gitee.action.api.spi.ScheduledHandle
import com.gitee.planners.ActionTestBootstrap
import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * 桥接一致性用例。
 *
 * 验证技能机制的桥接层在"由 Action 引擎派发"时，能把作用域、参数与目标原样交回业务实现：
 * 这是将来把派发层整体切换到引擎的前提。目标器链的解析路径尚未纳入本用例。
 */
class PlannersBridgeConformanceTest {

    /** 记录最近一次调用的业务机制。 */
    private val recorder = RecordingMechanism()

    /**
     * 安装服务、作用域与取值根，并登记测试机制。
     *
     * 注册表按 JVM 共享，重复注册会被拒绝，因此只在缺失时登记。
     */
    @BeforeEach
    fun setUp() {
        ActionTestBootstrap.ensureInstalled()
        PlannersActionScopes.install(this)
        PlannersQueryRoots.install(this)
        if (ActionApi.mechanisms().get(RECORDER_ID) == null) {
            PlannersActionBridges.registerMechanism(recorder, this)
        }
    }

    /**
     * 桥接后的机制可被 Action 注册表查到，且别名同样可查。
     */
    @Test
    fun registersBridgedMechanism() {
        val bridged = ActionApi.mechanisms().require(RECORDER_ID)
        assertEquals(RECORDER_ID, bridged.getId())
        assertEquals(bridged, ActionApi.mechanisms().get("test.alias"))
    }

    /**
     * 引擎派发时，业务机制收到同一份作用域实例与原始参数。
     *
     * 暂不启用：引擎对无目标器与目标器链的派发路径与技能侧旧执行器仍有差异，
     * 需要随派发层替换一起对齐后再打开，避免用错误断言固定现有行为。
     */
    @Disabled("待派发层替换后启用")
    @Test
    fun dispatchesThroughEngineWithSameScope() {
        val scope = QueryScope(variables = linkedMapOf("power" to 12), payload = linkedMapOf("hit" to true))
        val context = PlannersActionContexts.of(scope, RecordingHostServices())
        val bridged = ActionApi.mechanisms().require(RECORDER_ID)
        val arguments = ApiArguments(mapOf("amount" to "7"), "test.line")
        val line = ActionSpecs.line(bridged, arguments, emptyList(), PlannersActionScopes.SKILL, "test.line")
        val executed = ActionApi.engine().run(ActionSpecs.single(line, "test.line"), "main", context)
        assertEquals(1, executed)
        assertEquals(1, recorder.calls)
        // 作用域必须是同一个实例：变量、载荷与读取器都在里面
        assertSame(scope, recorder.lastScope)
        assertEquals("7", recorder.lastArguments!!.require("amount"))
        assertTrue(recorder.lastTargets!!.isEmpty())
    }

    /** 记录调用的测试机制。 */
    private class RecordingMechanism : Action {

        var calls = 0

        var lastScope: QueryScope? = null

        var lastArguments: ActionArguments? = null

        var lastTargets: List<ActionTarget>? = null

        override val id = RECORDER_ID

        override val aliases = listOf(RECORDER_ID, "test.alias")

        override fun execute(
            scope: QueryScope,
            arguments: ActionArguments,
            targets: List<ActionTarget>,
        ) {
            calls += 1
            lastScope = scope
            lastArguments = arguments
            lastTargets = targets
        }
    }

    /** 只记录告警的宿主服务。 */
    private class RecordingHostServices : HostServices {

        val warnings = ArrayList<String>()

        override fun schedule(delayTicks: Long, task: Runnable): ScheduledHandle {
            // 单测不推进时间，直接返回空句柄
            return ScheduledHandle {}
        }

        override fun isActive(): Boolean {
            return true
        }

        override fun warn(path: String, message: String) {
            warnings.add(path + " " + message)
        }
    }

    private companion object {

        /** 测试机制主键。 */
        const val RECORDER_ID = "test.recorder"
    }
}
