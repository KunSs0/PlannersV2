package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID
import taboolib.module.configuration.Configuration

/**
 * 验证选择器槽位的序列化与读取。
 *
 * `finder.now` 取当前行目标，`finder.<名字>` 取 `finder.set` 存下的目标，`.count` 取数量。
 */
class FinderRoundTripTest {

    /** 记录读取结果。 */
    private val probe = Probe()

    /** 每个用例前重置注册表。 */
    @BeforeEach
    fun setUp() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(ProbeAction(probe))
        probe.text = null
        probe.count = -1
    }

    /** 构造结构。 */
    private fun spec(yaml: String): ActionSpec {
        val configuration = Configuration.loadFromString(yaml)
        val parsed = ActionParser.parse("chain", configuration.getConfigurationSection("action"), "action")
        if (parsed == null) {
            throw IllegalStateException("spec must not be null")
        }
        return parsed
    }

    /**
     * 构造施法者。
     *
     * @param uuid 唯一 ID。
     * @return 玩家替身。
     */
    private fun player(uuid: UUID): Player {
        val player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(uuid)
        return player
    }

    /** 存入槽位后能按名字与 now 两种方式读回同一批目标。 */
    @Test
    fun storesAndReadsTargets() {
        val uuid = UUID.randomUUID()
        val caster = player(uuid)
        val parsed = spec(
            """
            action:
              fire:
                - 'finder.set{id="allies"} @sender'
                - 'probe{} @sender'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(sender = caster, caster = caster, label = "finder"))

        assertEquals(uuid.toString(), probe.text)
        assertEquals(1, probe.count)
        assertEquals(uuid.toString(), probe.now)
    }

    /** 数量段落可以单独读取。 */
    @Test
    fun exposesStoredCount() {
        val uuid = UUID.randomUUID()
        val caster = player(uuid)
        val parsed = spec(
            """
            action:
              fire:
                - 'finder.set{id="allies"} @sender'
                - 'probe{} @sender'
            """.trimIndent(),
        )

        ActionRunner.run(parsed, "fire", QueryScope(sender = caster, caster = caster, label = "finder"))

        assertEquals(1, probe.count)
    }

    /** 读取现场。 */
    private class Probe {

        var text: String? = null

        var count = -1

        var now: String? = null
    }

    /**
     * 读取选择器槽位的测试动作。
     *
     * @property probe 所属用例的记录容器。
     */
    private class ProbeAction(private val probe: Probe) : Action {

        override val id = "probe"

        override val aliases = listOf("probe")

        override val acceptsEmptyTargets = true

        override fun validate(arguments: ActionArguments) {
            arguments.rejectUnknown()
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            probe.text = QueryRef.parse("<finder.allies>", "probe").resolveText(scope)
            probe.count = QueryRef.parse("<finder.allies.count>", "probe").resolveDouble(scope).toInt()
            probe.now = QueryRef.parse("<finder.now>", "probe").resolveText(scope)
        }
    }
}
