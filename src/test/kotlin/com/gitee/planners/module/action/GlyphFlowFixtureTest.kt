package com.gitee.planners.module.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证光印的「会话特效 + 两处场地脉冲」流程能被加载期解析。
 *
 * fixture 与 `sage_prayer_binding_glyph.yml` 的 `action` 段同构：不读服务端业务目录，
 * 只保证动作、目标器链与条件语法在 Planners 侧可编译。
 */
class GlyphFlowFixtureTest {

    /** 每个用例前恢复内置条目，并注册 FightCore 提供的层级目标器替身。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
        ActionRegistry.register(TierTargeter)
    }

    /** 三段 fire 与脉冲流程结构正确。 */
    @Test
    fun parsesGlyphFlows() {
        val text = """
            action:
              fire:
                - 'fc.effect.start{key="sage_glyph"; id="priest_skill_5"; anchor=world; duration-ticks=<skill.var.duration>; scale=<skill.var.effectScale>; range=64} @target'
                - 'area.start{id="sage_glyph_damage"; radius=<skill.var.radius>; height=<skill.var.height>; ticks=<skill.var.duration>; period=<skill.var.interval>; on.run="glyphDamage"} @target'
                - 'area.start{id="sage_glyph_slow"; radius=<skill.var.radius>; height=<skill.var.height>; ticks=<skill.var.duration>; period=5; on.run="glyphSlow"} @target'
              glyphDamage:
                - 'fc.damage.skill{profile=class-skill; power=<skill.var.pulsePower>; main=<attr.intelligence>; multiplier=1; influence=1; origin="planners.sage_prayer_binding_glyph"; on-hit="onGlyphHit"} @range{radius=<skill.var.radius>; height=<skill.var.height>} @enemy'
              glyphSlow:
                - 'call{func="glyphSlowStrong"}'
                - 'call{func="glyphSlowWeak"}'
              glyphSlowStrong:
                conditions: ['expr{e="skill.var.hasFunctionB"}']
                actions:
                  - 'fc.slow{buff="sage-glyph-slow-strong"; ratio=0.35; duration=5; source="planners.sage-glyph"; replaces="sage-glyph-slow"} @range{radius=<skill.var.radius>; height=<skill.var.height>} @enemy @fc.tier{exclude=boss}'
              glyphSlowWeak:
                conditions: ['expr{e="skill.var.hasFunctionB"} false']
                actions:
                  - 'fc.slow{buff="sage-glyph-slow"; ratio=0.25; duration=5; source="planners.sage-glyph"; blocked-by="sage-glyph-slow-strong"} @range{radius=<skill.var.radius>; height=<skill.var.height>} @enemy @fc.tier{exclude=boss}'
        """.trimIndent()
        val configuration = Configuration.loadFromString(text)
        val spec = ActionParser.parse("sage_prayer_binding_glyph", configuration.getConfigurationSection("action"), "action")
        if (spec == null) {
            throw AssertionError("glyph flows must be parsed")
        }
        val fire = spec.flow(ActionSpec.FIRE)
        if (fire == null) {
            throw AssertionError("fire flow must exist")
        }
        assertEquals(3, fire.actions.size)
        assertEquals("fc.effect.start", fire.actions[0].action.id)
        assertEquals("area.start", fire.actions[1].action.id)
        assertEquals("area.start", fire.actions[2].action.id)

        val damage = spec.flow("glyphDamage")
        if (damage == null) {
            throw AssertionError("glyphDamage flow must exist")
        }
        val damageTargeters = damage.actions[0].targeters
        assertEquals(2, damageTargeters.size)
        assertEquals("range", damageTargeters[0].targeter.id)
        assertEquals("enemy", damageTargeters[1].targeter.id)

        val weak = spec.flow("glyphSlowWeak")
        if (weak == null) {
            throw AssertionError("glyphSlowWeak flow must exist")
        }
        assertEquals(1, weak.conditions.size)
        assertTrue(weak.conditions[0].describe().endsWith("false"))
        assertEquals(3, weak.actions[0].targeters.size)
        assertEquals("fc.tier", weak.actions[0].targeters[2].targeter.id)
    }
}

/**
 * c.tier 的测试替身：只声明 id 与参数约定，不参与本次结构校验。
 */
private object TierTargeter : ActionTargeter {

    override val id = "fc.tier"

    override val aliases = listOf("fc.tier")

    override fun validate(arguments: ActionArguments) {
        arguments.rejectUnknown("include", "exclude")
    }

    override fun resolve(scope: QueryScope, arguments: ActionArguments, input: List<ActionTarget>): List<ActionTarget> {
        return input
    }
}