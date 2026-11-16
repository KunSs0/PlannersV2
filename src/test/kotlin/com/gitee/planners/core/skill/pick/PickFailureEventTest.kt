package com.gitee.planners.core.skill.pick

import com.gitee.planners.api.pick.PickDefinition
import com.gitee.planners.api.pick.PickFailureReason
import com.gitee.planners.api.event.player.PlayerSkillCastEvent
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 指向失败必须派发一次事件，而不是静默取消。
 */
class PickFailureEventTest {

    /** 记录本次用例派发的事件。 */
    private val events = ArrayList<BukkitProxyEvent>()

    /**
     * 启动 MockBukkit：事件对象构造会读取服务端主线程状态。
     */
    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        PlayerSkillCastEvent.emitter = { event -> events.add(event) }
    }

    /** 每个用例后恢复默认派发并关闭 MockBukkit。 */
    @AfterEach
    fun reset() {
        PlayerSkillCastEvent.emitter = { event -> event.call() }
        MockBukkit.unmock()
        server = null
    }

    /** MockBukkit 服务端实例，供事件构造使用。 */
    private var server: ServerMock? = null

    /** 缺少运行期 factory 时派发 NO_FACTORY 事件并取消施放。 */
    @Test
    fun reportsMissingFactory() {
        val player = player()
        val skill = skill("fc.position")

        val started = PickSessionManager.start(player, skill, "key5") {
            ExecutableResult.cancelledWithEvent()
        }

        assertFalse(started)
        assertEquals(1, events.size)
        val event = events[0] as PlayerSkillCastEvent.PickFailed
        assertEquals(PickFailureReason.NO_FACTORY, event.reason)
        assertEquals("key5", event.sourceKey)
        assertEquals("sage_prayer_binding_glyph", event.skill.id)
        assertEquals(player, event.player)
    }

    /**
     * 构造施法者替身。
     *
     * @return 玩家替身。
     */
    private fun player(): Player {
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
        Mockito.`when`(player.name).thenReturn("caster")
        Mockito.`when`(player.isOnline).thenReturn(true)
        return player
    }

    /**
     * 构造带指向定义的技能替身。
     *
     * @param type 指向类型。
     * @return 技能替身。
     */
    private fun skill(type: String): PlayerSkill {
        val definition = object : PickDefinition {
            override val type = type
            override val maxDurationTicks = 200
        }
        val immutable = Mockito.mock(ImmutableSkill::class.java)
        Mockito.`when`(immutable.pick).thenReturn(definition)
        val skill = Mockito.mock(PlayerSkill::class.java)
        Mockito.`when`(skill.immutable).thenReturn(immutable)
        Mockito.`when`(skill.id).thenReturn("sage_prayer_binding_glyph")
        return skill
    }
}
