package com.gitee.planners.module.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证「有队友则作用于队友，否则自救」的两条分支流程能被加载期解析。
 *
 * fixture 与 `sage_prayer_purifying_prayer.yml` 的 `action` 段同构：不读服务端业务目录，
 * 只保证 `targeter` 条件、目标器链与通用动作参数语法在 Planners 侧可编译。
 */
class PurifyFlowFixtureTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 两条分支互补且都使用通用动作。 */
    @Test
    fun parsesPurifyBranches() {
        val text = """
            action:
              onPurify:
                - 'call{func="purifyTeammate"}'
                - 'call{func="purifySelf"}'
              purifyTeammate:
                conditions: ['targeter{e="@crosshair{range=20} @ally @alive"}']
                actions:
                  - 'fc.buff.dispel{max-level=1; tags=general-status,abnormal,stun; categories=harmful; count=1} @crosshair{range=20} @ally @alive'
                  - 'fc.heal{amount=<skill.var.healingPower * (1 + attr.spirit * skill.var.healingMainRate)>; origin="planners.sage_prayer_purifying_prayer"; can-crit=false} @crosshair{range=20} @ally @alive'
              purifySelf:
                conditions: ['targeter{e="@crosshair{range=20} @ally @alive"} false']
                actions:
                  - 'fc.buff.dispel{max-level=1; tags=general-status,abnormal,stun; categories=harmful; count=1} @self'
        """.trimIndent()
        val configuration = Configuration.loadFromString(text)
        val spec = ActionParser.parse("sage_prayer_purifying_prayer", configuration.getConfigurationSection("action"), "action")
        if (spec == null) {
            throw AssertionError("purify flows must be parsed")
        }
        val entry = spec.flow("onPurify")
        if (entry == null) {
            throw AssertionError("onPurify flow must exist")
        }
        assertEquals(2, entry.actions.size)

        val teammate = spec.flow("purifyTeammate")
        if (teammate == null) {
            throw AssertionError("purifyTeammate flow must exist")
        }
        assertEquals(1, teammate.conditions.size)
        val teammateActions = teammate.actions
        assertEquals(2, teammateActions.size)
        assertEquals("fc.buff.dispel", teammateActions[0].action.id)
        assertEquals(3, teammateActions[0].targeters.size)
        assertEquals("crosshair", teammateActions[0].targeters[0].targeter.id)
        assertEquals("fc.heal", teammateActions[1].action.id)

        val selfBranch = spec.flow("purifySelf")
        if (selfBranch == null) {
            throw AssertionError("purifySelf flow must exist")
        }
        assertEquals(1, selfBranch.conditions.size)
        assertNotNull(selfBranch.actions[0].targeters)
        assertEquals("sender", selfBranch.actions[0].targeters[0].targeter.id)
        assertTrue(selfBranch.conditions[0].describe().endsWith("false"))
    }
}
