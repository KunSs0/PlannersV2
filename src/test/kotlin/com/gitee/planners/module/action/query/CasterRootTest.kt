package com.gitee.planners.module.action.query

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

/**
 * 验证 `caster` 取值根与 `sender` 的关系。
 *
 * 逐目标 `call` 命中玩家目标时，引擎会用该玩家重建作用域（`sender` 换成当前元素、
 * `caster` 保持发起者），业务拼「施法者标识」这类 key 必须拿得到发起者，否则同一个
 * 目标会被多个施法者共用一把 key。该重建发生在 TabooLib 平台服务可用的宿主里，
 * 单元测试只固定取值根本身的语义。
 */
class CasterRootTest {

    private lateinit var server: ServerMock

    /** 每个用例前启动 MockBukkit。 */
    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
    }

    /** 每个用例后关闭 MockBukkit。 */
    @AfterEach
    fun reset() {
        MockBukkit.unmock()
    }

    /** `sender` 与 `caster` 不同时，两个根各读各的实体。 */
    @Test
    fun casterRootReadsCasterRatherThanSender() {
        val caster = server.addPlayer("Caster")
        val current = server.addPlayer("Current")
        val scope = QueryScope(sender = current, caster = caster, label = "caster-root")

        val casterUuid = QueryRef.parse("<caster.uuid>", "test").resolve(scope)
        val senderUuid = QueryRef.parse("<sender.uuid>", "test").resolve(scope)

        assertEquals(caster.uniqueId.toString(), casterUuid)
        assertEquals(current.uniqueId.toString(), senderUuid)
        assertNotEquals(casterUuid, senderUuid)
    }

    /** 只写根名时 `caster` 直接返回施法者对象本身。 */
    @Test
    fun bareCasterResolvesToCasterObject() {
        val caster = server.addPlayer("Caster")
        val current = server.addPlayer("Current")
        val scope = QueryScope(sender = current, caster = caster, label = "caster-root")

        val resolved = QueryRef.parse("<caster>", "test").resolve(scope)

        assertSame(caster, resolved)
    }

    /** 没有单独指定 `caster` 时它与 `sender` 同源，读到的值必须一致。 */
    @Test
    fun casterFallsBackToSender() {
        val player = server.addPlayer("Caster")
        val scope = QueryScope(sender = player, label = "caster-root")

        val casterUuid = QueryRef.parse("<caster.uuid>", "test").resolve(scope)
        val senderUuid = QueryRef.parse("<sender.uuid>", "test").resolve(scope)

        assertEquals(senderUuid, casterUuid)
        assertEquals(player.uniqueId.toString(), casterUuid)
    }

    /** 只写根名时不再被当成未知字段，也不会被字面量吞掉。 */
    @Test
    fun bareCasterIsNotTreatedAsLiteralText() {
        val player = server.addPlayer("Caster")

        val resolved = QueryRef.parse("caster", "test").resolve(QueryScope(sender = player, label = "caster-root"))

        assertSame(player, resolved)
    }
}
