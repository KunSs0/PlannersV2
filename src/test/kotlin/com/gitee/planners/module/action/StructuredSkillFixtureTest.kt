package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证「冲锋」技能从旧脚本写法迁移到结构化写法后的加载结果。
 *
 * 这里使用与迁移后业务配置同构的合成 fixture，覆盖节点结构、变量求值与动作参数，
 * 不读取服务端生产目录。
 */
class StructuredSkillFixtureTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /**
     * 迁移后的技能配置，与业务侧 `charge.yml` 同构。
     */
    private val yaml = """
        __option__:
          name: "冲锋"
          category: [active]
          variables:
            castTime: 0
            display_cast_seconds: "skill.castTime / 20.0"
            cooldown: 80
            speed: "skill.level * 15 + 80"
          max-level: 3
        action:
          fire:
            - 'message{m="&a冲锋 Lv<skill.level>"}'
    """.trimIndent()

    /** 读取 `action` 节点。 */
    private fun actionSection(): taboolib.library.configuration.ConfigurationSection {
        val configuration = Configuration.loadFromString(yaml)
        val result = configuration.getConfigurationSection("action")
        assertNotNull(result, "action section must be present")
        return result!!
    }

    /** 读取 `variables` 节点。 */
    private fun variableSection(): taboolib.library.configuration.ConfigurationSection {
        val configuration = Configuration.loadFromString(yaml)
        val result = configuration.getConfigurationSection("__option__.variables")
        assertNotNull(result, "variables section must be present")
        return result!!
    }

    /** 结构化 `action` 被识别为流程而不是脚本字符串。 */
    @Test
    fun actionIsParsedAsFlow() {
        val spec = ActionParser.parse("charge", actionSection(), "skill:charge.action")
        assertNotNull(spec)
        assertTrue(spec!!.hasFlow(ActionSpec.FIRE))
        val flow = spec.flow(ActionSpec.FIRE)!!
        assertEquals(1, flow.actions.size)
        assertEquals("message", flow.actions[0].action.id)
        assertTrue(flow.conditions.isEmpty())
    }

    /** 动作参数中的占位符渲染为当前技能等级。 */
    @Test
    fun actionArgumentRendersLevel() {
        val spec = ActionParser.parse("charge", actionSection(), "skill:charge.action")!!
        val line = spec.flow(ActionSpec.FIRE)!!.actions[0]
        val scope = QueryScope(
            skill = Any(),
            variables = mapOf("level" to 3),
            skillFieldReader = { _, field ->
                if (field == "level") {
                    3L
                } else {
                    null
                }
            },
            label = "charge",
        )
        assertEquals("&a冲锋 Lv3", line.arguments.requireText(scope, "m", "message"))
    }

    /** 变量表达式按声明顺序求值，且能引用先声明的基础值。 */
    @Test
    fun variablesEvaluateInOrder() {
        val section = variableSection()
        val accumulated = LinkedHashMap<String, Any?>()
        val scope = QueryScope(
            skill = Any(),
            variables = accumulated,
            skillFieldReader = { _, field ->
                if (field == "level") {
                    3L
                } else {
                    null
                }
            },
            label = "charge",
        )
        for (key in section.getKeys(false)) {
            val variable = com.gitee.planners.core.config.ImmutableVariable.parse(key, section.get(key)!!)
            accumulated[key] = variable.evaluate(scope)
        }
        assertEquals(0, accumulated["castTime"])
        assertEquals(0.0, accumulated["display_cast_seconds"])
        assertEquals(80, accumulated["cooldown"])
        assertEquals(125.0, accumulated["speed"])
    }

    /** 带分支的技能用 call + conditions 表达 if/else。 */
    @Test
    fun branchUsesCallWithConditions() {
        val text = """
            action:
              fire:
                - 'call{func=on_result}'
                - 'call{func=on_empty}'
              on_result:
                conditions:
                  - 'expr{e="skill.directing != null"}'
                actions:
                  - 'message{m="&e确认: <skill.directing>"}'
              on_empty:
                conditions:
                  - 'expr{e="skill.directing == null"}'
                actions:
                  - 'message{m="&c结果为空"}'
        """.trimIndent()
        val configuration = Configuration.loadFromString(text)
        val spec = ActionParser.parse("entity-directing", configuration.getConfigurationSection("action"), "action")
        assertNotNull(spec)
        assertTrue(spec!!.hasFlow("on_result"))
        assertTrue(spec.hasFlow("on_empty"))
        assertEquals(2, spec.flow(ActionSpec.FIRE)!!.actions.size)
        assertEquals(1, spec.flow("on_result")!!.conditions.size)
    }

    /** 指向结果为空时只走空分支。 */
    @Test
    fun branchPicksEmptyWhenDirectingMissing() {
        val text = """
            action:
              fire:
                - 'call{func=on_empty}'
              on_empty:
                conditions:
                  - 'expr{e="skill.directing == null"}'
                actions:
                  - 'record{tag="empty"}'
        """.trimIndent()
        ActionRegistry.register(BranchRecorder)
        BranchRecorder.HITS.clear()
        val configuration = Configuration.loadFromString(text)
        val spec = ActionParser.parse("entity-directing", configuration.getConfigurationSection("action"), "action")
        val scope = QueryScope(
            skill = Any(),
            variables = mapOf(),
            skillFieldReader = { _, _ -> null },
            label = "entity-directing",
        )
        ActionRunner.run(spec!!, ActionSpec.FIRE, scope)
        assertEquals(listOf("empty"), BranchRecorder.HITS)
    }

    /**
     * 分支用例使用的记录动作。
     */
    private object BranchRecorder : Action {

        override val id = "record"

        override val aliases = listOf("record")

        override val acceptsEmptyTargets = true

        /** 记录调用顺序。 */
        val HITS = ArrayList<String>()

        override fun validate(arguments: ActionArguments) {
            arguments.require("tag")
            arguments.rejectUnknown("tag")
        }

        override fun execute(scope: QueryScope, arguments: ActionArguments, targets: List<ActionTarget>) {
            HITS.add(arguments.requireText(scope, "tag"))
        }
    }
    @Test
    fun multiVariableSkillEvaluates() {
        val text = """
            __option__:
              variables:
                castTime: 0
                display_cast_seconds: "skill.castTime / 20.0"
                cooldown: 150
                attack_speed: "10 * skill.level + 50"
                duration: "2 * skill.level + 5"
            action:
              fire:
                - 'message{m="&a战斗狂热 Lv<skill.level>"}'
        """.trimIndent()
        val configuration = Configuration.loadFromString(text)
        val section = configuration.getConfigurationSection("__option__.variables")!!
        val accumulated = LinkedHashMap<String, Any?>()
        val scope = QueryScope(
            skill = Any(),
            variables = accumulated,
            skillFieldReader = { _, field ->
                if (field == "level") {
                    6L
                } else {
                    null
                }
            },
            label = "battle_fury",
        )
        for (key in section.getKeys(false)) {
            val variable = com.gitee.planners.core.config.ImmutableVariable.parse(key, section.get(key)!!)
            accumulated[key] = variable.evaluate(scope)
        }
        assertEquals(110.0, accumulated["attack_speed"])
        assertEquals(17.0, accumulated["duration"])
        assertEquals(150, accumulated["cooldown"])
    }
}
