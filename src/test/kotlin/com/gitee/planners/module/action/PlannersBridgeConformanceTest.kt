package com.gitee.planners.module.action

import com.gitee.action.api.Action as ActionApi
import com.gitee.action.api.model.ActionArguments as ApiArguments
import com.gitee.action.api.spi.HostServices
import com.gitee.action.api.spi.ScheduledHandle
import com.gitee.planners.ActionTestBootstrap
import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 桥接一致性用例。
 *
 * 目前只验证"桥接后的机制进入 Action 注册表、可按主键与别名查到"。
 *
 * 派发路径的用例暂缺：引擎对单行结构的 `run` 在本用例的构造方式下返回 0 且不产生告警或异常，
 * 原因尚未查明，因此没有用错误断言把它固定下来。详见设计文档"未完成项"。
 */
class PlannersBridgeConformanceTest {

    /** 记录调用的业务机制。 */
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
