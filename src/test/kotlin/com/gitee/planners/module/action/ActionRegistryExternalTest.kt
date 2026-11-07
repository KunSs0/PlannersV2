package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 验证外部插件的注册与撤销。
 *
 * 对应 D-14 备选 A：FightCore 在自己的启用阶段登记 `fc.*`，停用时按所有者整体撤销，
 * 因此重载后不会残留旧插件实例。
 */
class ActionRegistryExternalTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 外部注册的动作可按主键查到。 */
    @Test
    fun registersExternalAction() {
        val owner = Any()
        ActionRegistry.registerExternal(owner, ExternalAction)
        assertNotNull(ActionRegistry.action("fc.test"))
        assertEquals("fc.test", ActionRegistry.action("fc.test")!!.id)
    }

    /** 外部注册的条件可按主键查到。 */
    @Test
    fun registersExternalCondition() {
        val owner = Any()
        ActionRegistry.registerExternal(owner, ExternalCondition)
        assertNotNull(ActionRegistry.condition("fc.test-condition"))
    }

    /** 撤销后动作与别名都不再存在。 */
    @Test
    fun unregisterRemovesActionAndAliases() {
        val owner = Any()
        ActionRegistry.registerExternal(owner, ExternalAction)
        ActionRegistry.unregisterAll(owner)
        assertNull(ActionRegistry.action("fc.test"))
        assertNull(ActionRegistry.action("fc.test-alias"))
    }

    /** 撤销只影响该所有者，其他所有者的条目保留。 */
    @Test
    fun unregisterKeepsOtherOwners() {
        val first = Any()
        val second = Any()
        ActionRegistry.registerExternal(first, ExternalAction)
        ActionRegistry.registerExternal(second, AnotherExternalAction)
        ActionRegistry.unregisterAll(first)
        assertNull(ActionRegistry.action("fc.test"))
        assertNotNull(ActionRegistry.action("fc.other"))
    }

    /** 重复撤销同一所有者无副作用。 */
    @Test
    fun unregisterIsIdempotent() {
        val owner = Any()
        ActionRegistry.registerExternal(owner, ExternalAction)
        ActionRegistry.unregisterAll(owner)
        ActionRegistry.unregisterAll(owner)
        assertNull(ActionRegistry.action("fc.test"))
    }

    /** 撤销后内置条目不受影响。 */
    @Test
    fun unregisterKeepsBuiltins() {
        ActionRegistry.registerExternal(Any(), ExternalAction)
        ActionRegistry.unregisterAll(Any())
        assertNotNull(ActionRegistry.action("message"))
        assertNotNull(ActionRegistry.targeter("self"))
        assertNotNull(ActionRegistry.trigger("onTimer"))
        assertNotNull(ActionRegistry.condition("expr"))
    }

    /** 所有者数量随注册与撤销变化。 */
    @Test
    fun tracksOwnerCount() {
        val before = ActionRegistry.externalOwnerCount()
        val owner = Any()
        ActionRegistry.registerExternal(owner, ExternalAction)
        assertEquals(before + 1, ActionRegistry.externalOwnerCount())
        ActionRegistry.unregisterAll(owner)
        assertEquals(before, ActionRegistry.externalOwnerCount())
    }

    /** 外部注册的动作可以写入流程并被解析。 */
    @Test
    fun externalActionIsParsable() {
        ActionRegistry.registerExternal(Any(), ExternalAction)
        val line = ActionParser.parseLine("fc.test{value=1}", "action.fire[0]")
        assertEquals("fc.test", line.action.id)
    }

    /**
     * 测试用外部动作。
     */
    private object ExternalAction : Action {

        override val id = "fc.test"

        override val aliases = listOf("fc.test", "fc.test-alias")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.require("value")
            arguments.rejectUnknown("value")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        }
    }

    /**
     * 第二个所有者的外部动作，用于验证撤销隔离。
     */
    private object AnotherExternalAction : Action {

        override val id = "fc.other"

        override val aliases = listOf("fc.other")

        override val acceptsEmptyTargets = true

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
        }
    }

    /**
     * 测试用外部条件。
     */
    private object ExternalCondition : ActionCondition {

        override val id = "fc.test-condition"

        override val aliases = listOf("fc.test-condition")

        override fun test(scope: QueryScope, arguments: ActionArguments, compiled: Any?): Boolean {
            return true
        }
    }
}
