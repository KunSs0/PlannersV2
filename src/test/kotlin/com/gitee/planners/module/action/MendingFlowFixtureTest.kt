package com.gitee.planners.module.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证业务里「directing 选人 + 通用治疗」流程能被加载期解析。
 *
 * fixture 与 `sage_prayer_quick_mending.yml` 的 `action.fire` 同构：不读服务端业务目录，
 * 只保证表达式、目标器与参数语法在 Planners 侧可编译。
 */
class MendingFlowFixtureTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 两行 fire 都能被解析，且治疗行只作用于当前目标。 */
    @Test
    fun parsesDirectingHealFlow() {
        val text = """
            action:
              fire:
                - 'fc.heal{amount=<(skill.var.healingPower + (target.health <= target.max_health * skill.var.emergencyThreshold ? skill.var.emergencyPower : 0)) * (1 + attr.spirit * skill.var.healingMainRate) * skill.var.healingMultiplier>; origin="planners.sage_prayer_quick_mending"; can-crit=true} @target'
                - 'fc.effect{id="priest_skill_2_heal_effect"; durationTicks=0; range=32} @target'
        """.trimIndent()
        val configuration = Configuration.loadFromString(text)
        val spec = ActionParser.parse("sage_prayer_quick_mending", configuration.getConfigurationSection("action"), "action")
        if (spec == null) {
            throw AssertionError("fire flow must be parsed")
        }
        val flow = spec.flow(ActionSpec.FIRE)
        if (flow == null) {
            throw AssertionError("fire flow must exist")
        }
        assertEquals(2, flow.actions.size)
        assertEquals("fc.heal", flow.actions[0].action.id)
        assertEquals("fc.effect", flow.actions[1].action.id)
        assertEquals("target", flow.actions[0].targeters[0].targeter.id)
    }

    /** 治疗表达式必须能被表达式解析器接受（三元、比较与查询路径都要成立）。 */
    @Test
    fun keepsFormulaSyntaxValid() {
        val formula = "(skill.var.healingPower + (target.health <= target.max_health * skill.var.emergencyThreshold ? skill.var.emergencyPower : 0)) * (1 + attr.spirit * skill.var.healingMainRate) * skill.var.healingMultiplier"
        val reference = com.gitee.planners.module.action.query.QueryRef.parseExpression(formula, "mending.amount")
        assertTrue(reference.label == "mending.amount")
    }
}
