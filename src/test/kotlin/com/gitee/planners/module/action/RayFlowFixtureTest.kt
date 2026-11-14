package com.gitee.planners.module.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证光束的「自循环引导」流程能被加载期解析。
 *
 * fixture 与 `sage_prayer_scolding_ray.yml` 的 `action` 段同构：不读服务端业务目录，
 * 只保证共享标记、自循环、等宽长方体与资源分支的语法在 Planners 侧可编译。
 */
class RayFlowFixtureTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 自循环与分支结构正确。 */
    @Test
    fun parsesRaySelfLoop() {
        val text = """
            action:
              onRayStart:
                - 'meta.set{key="sage_ray_ticks"; value=<skill.var.channelTicks>; timeout=100} @self'
                - 'meta.get{key="sage_ray_ticks"; to="rayTicks"; default=0} @self'
                - 'fc.effect.start{key="sage_ray"; id="priest_skill_4"; anchor=self; duration-ticks=<skill.var.channelTicks>; scale=1.0; range=32; offset-x=0.0; offset-y=0.0; offset-z=0.0; forward=2.0} @self'
                - 'call{func="rayStep"}'
              rayStep:
                conditions: ['expr{e="skill.var.rayTicks > 0"}']
                actions:
                  - 'call{func="rayCharge"}'
                  - 'call{func="rayDrained"}'
                  - 'delay{ticks=1}'
                  - 'call{func="rayStep"}'
              rayCharge:
                conditions: ['expr{e="attr.mana >= skill.var.channelCost"}']
                actions:
                  - 'fc.resource{id=mana; delta=<-skill.var.channelCost>} @self'
                  - 'meta.set{key="sage_ray_ticks"; value=<skill.var.rayTicks - 1>; timeout=100} @self'
                  - 'meta.get{key="sage_ray_ticks"; to="rayTicks"; default=0} @self'
                  - 'call{func="rayHit"}'
              rayHit:
                conditions: ['expr{e="skill.var.rayTicks % skill.var.pulseInterval == 0"}']
                actions:
                  - 'fc.damage.skill{profile=class-skill; power=<skill.var.pulsePower>; main=<attr.intelligence>; multiplier=1; influence=1; origin="planners.sage_prayer_scolding_ray"} @rect{width=<skill.var.width>; height=2.4; length=<skill.var.range>; offsetY=1.0; offsetZ=<skill.var.range / 2.0 + 2.0>} @enemy'
              rayDrained:
                conditions: ['expr{e="attr.mana < skill.var.channelCost"}']
                actions:
                  - 'meta.set{key="sage_ray_ticks"; value=0; timeout=100} @self'
                  - 'meta.get{key="sage_ray_ticks"; to="rayTicks"; default=0} @self'
                  - 'fc.combo.release{cause="expired"} @self'
              onRayEnd:
                - 'meta.clear{key="sage_ray_ticks"} @self'
                - 'fc.effect.stop{key="sage_ray"; stop-mode="play-end"} @self'
        """.trimIndent()
        val configuration = Configuration.loadFromString(text)
        val spec = ActionParser.parse("sage_prayer_scolding_ray", configuration.getConfigurationSection("action"), "action")
        if (spec == null) {
            throw AssertionError("ray flows must be parsed")
        }
        val start = spec.flow("onRayStart")
        if (start == null) {
            throw AssertionError("onRayStart must exist")
        }
        assertEquals(4, start.actions.size)
        assertEquals("meta.set", start.actions[0].action.id)
        assertEquals("sender", start.actions[0].targeters[0].targeter.id)
        assertEquals("call", start.actions[3].action.id)

        val step = spec.flow("rayStep")
        if (step == null) {
            throw AssertionError("rayStep must exist")
        }
        assertEquals(1, step.conditions.size)
        assertEquals(4, step.actions.size)
        assertEquals("delay", step.actions[2].action.id)
        assertEquals("call", step.actions[3].action.id)

        val hit = spec.flow("rayHit")
        if (hit == null) {
            throw AssertionError("rayHit must exist")
        }
        val targeters = hit.actions[0].targeters
        assertEquals(2, targeters.size)
        assertEquals("rect", targeters[0].targeter.id)
        assertEquals("enemy", targeters[1].targeter.id)
        assertEquals(1.0, targeters[0].arguments.requireNumber(EMPTY_SCOPE, "offsetY"))

        val drained = spec.flow("rayDrained")
        if (drained == null) {
            throw AssertionError("rayDrained must exist")
        }
        assertEquals("fc.combo.release", drained.actions[2].action.id)

        val end = spec.flow("onRayEnd")
        if (end == null) {
            throw AssertionError("onRayEnd must exist")
        }
        assertEquals("fc.effect.stop", end.actions[1].action.id)
        assertTrue(end.actions[1].arguments.requireText(EMPTY_SCOPE, "key") == "sage_ray")
    }

    companion object {

        /** 编译期读取字面量参数时使用的最小上下文。 */
        private val EMPTY_SCOPE = com.gitee.planners.module.action.query.QueryScope(label = "compile")
    }
}
