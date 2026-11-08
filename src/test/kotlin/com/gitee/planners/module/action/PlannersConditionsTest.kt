package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration

/**
 * 验证 Planners 侧条件与作用域约束。
 *
 * 条件通过 [QueryScope] 的函数注入提供宿主数据，不依赖 Bukkit 运行环境。
 */
class PlannersConditionsTest {

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 构造带节点、标记、技能等级与职业的上下文。 */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = Any(),
            profile = Any(),
            skillFieldReader = { _, field ->
                if (field == "level") {
                    4L
                } else {
                    null
                }
            },
            profileFieldReader = { _, field ->
                when (field) {
                    "job" -> "warder"
                    "level" -> 12L
                    else -> null
                }
            },
            dataReader = { key ->
                when (key) {
                    "knight.skill6.field" -> "rescue"
                    "empty.field" -> ""
                    else -> null
                }
            },
            nodeReader = { tree, node ->
                if (tree == "knight_warder" && node == "knight-F-01_a") {
                    2
                } else {
                    0
                }
            },
            label = "test",
        )
    }


    /** 解析一条条件文本。 */
    private fun condition(text: String): ActionConditionEntry {
        return ActionParser.parseCondition(text, "test.conditions[0]")
    }

    /** 节点条件按学习等级判定。 */
    @Test
    fun nodeConditionComparesLevel() {
        val passed = condition("node{tree=\"knight_warder\"; id=\"knight-F-01_a\"}").matches(scope())
        assertTrue(passed)
        val failed = condition("node{tree=\"knight_warder\"; id=\"knight-F-01_b\"}").matches(scope())
        assertFalse(failed)
        val higher = condition("node{tree=\"knight_warder\"; id=\"knight-F-01_a\"; min=3}").matches(scope())
        assertFalse(higher)
    }

    /** 节点条件必须同时给出树与节点。 */
    @Test
    fun nodeConditionRequiresTreeAndId() {
        val failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            condition("node{id=\"knight-F-01_a\"}")
        }
        assertTrue(failure.message!!.contains("requires argument 'tree'"))
    }

    /** 标记条件支持存在性与等值两种用法。 */
    @Test
    fun metaConditionChecksPresenceAndValue() {
        assertTrue(condition("meta{key=\"knight.skill6.field\"}").matches(scope()))
        assertTrue(condition("meta{key=\"knight.skill6.field\"; is=\"rescue\"}").matches(scope()))
        assertFalse(condition("meta{key=\"knight.skill6.field\"; is=\"bulwark\"}").matches(scope()))
        assertFalse(condition("meta{key=\"empty.field\"}").matches(scope()))
    }

    /** 等级条件按区间判定。 */
    @Test
    fun levelConditionChecksRange() {
        assertTrue(condition("level{min=3}").matches(scope()))
        assertFalse(condition("level{min=5}").matches(scope()))
        assertTrue(condition("level{min=3; max=5}").matches(scope()))
        assertFalse(condition("level{max=3}").matches(scope()))
    }

    /** 等级条件必须至少给一个边界。 */
    @Test
    fun levelConditionRequiresBound() {
        val failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            condition("level{}")
        }
        assertTrue(failure.message!!.contains("requires 'min' or 'max'"))
    }

    /** 职业条件比较当前职业。 */
    @Test
    fun jobConditionComparesJob() {
        assertTrue(condition("job{is=\"warder\"}").matches(scope()))
        assertFalse(condition("job{is=\"mage\"}").matches(scope()))
    }

    /** 取反后缀对全部条件生效。 */
    @Test
    fun falseSuffixNegatesCondition() {
        assertFalse(condition("job{is=\"warder\"} false").matches(scope()))
        assertTrue(condition("job{is=\"mage\"} false").matches(scope()))
    }

    /** 技能作用域条件不能写在 server 流程里。 */
    @Test
    fun rejectsSkillScopedConditionInServerFlow() {
        val builder = StringBuilder()
        builder.append("action:\n")
        builder.append("  server:\n")
        builder.append("    conditions:\n")
        builder.append("      - 'node{tree=\"knight_warder\"; id=\"knight-F-01_a\"}'\n")
        builder.append("    actions:\n")
        builder.append("      - 'log{text=\"a\"}'\n")
        val configuration = Configuration.loadFromString(builder.toString())
        val failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parse("test", configuration.getConfigurationSection("action"), "action")
        }
        assertTrue(failure.message!!.contains("skill-scoped"))
    }

    /** 技能作用域目标器不能写在 server 流程里。 */
    @Test
    fun rejectsSkillScopedTargeterInServerFlow() {
        val builder = StringBuilder()
        builder.append("action:\n")
        builder.append("  server:\n")
        builder.append("    - 'log{text=\"a\"} @range{radius=3}'\n")
        val configuration = Configuration.loadFromString(builder.toString())
        val failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parse("test", configuration.getConfigurationSection("action"), "action")
        }
        assertTrue(failure.message!!.contains("skill-scoped"))
    }

    /** 通用条件与目标器允许出现在 server 流程。 */
    @Test
    fun allowsGeneralScopeInServerFlow() {
        val builder = StringBuilder()
        builder.append("action:\n")
        builder.append("  server:\n")
        builder.append("    conditions:\n")
        builder.append("      - 'chance{value=1}'\n")
        builder.append("    actions:\n")
        builder.append("      - 'log{text=\"a\"} @self'\n")
        val configuration = Configuration.loadFromString(builder.toString())
        val spec = ActionParser.parse("test", configuration.getConfigurationSection("action"), "action")
        assertEquals(1, spec!!.flow(ActionSpec.SERVER)!!.actions.size)
    }

    /** 全部内置条件都已登记。 */
    @Test
    fun registersAllConditions() {
        val ids = ActionRegistry.conditionIds()
        for (expected in listOf("expr", "chance", "node", "meta", "level", "job", "wounded")) {
            assertTrue(ids.contains(expected), "missing condition: $expected")
        }
    }

    /** 特效动作拒绝缺少寿命的配置。 */
    @Test
    fun effectSpawnRequiresTicks() {
        val failure = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            ActionParser.parseLine("effect.spawn{id=\"knight_skill_6\"}", "action.fire[0]")
        }
        assertTrue(failure.message!!.contains("requires argument 'ticks'"))
    }

    /** 特效动作允许写在技能流程里，不允许写在 server 流程。 */
    @Test
    fun effectSpawnIsSkillScoped() {
        val spec = ActionParser.parseLine(
            "effect.spawn{id=\"knight_skill_6\"; ticks=<skill.duration>}",
            "action.fire[0]",
        )
        assertEquals("effect.spawn", spec.action.id)
        assertEquals(ActionScope.SKILL, spec.action.scope)
    }

    /** 特效动作已登记。 */
    @Test
    fun registersEffectAction() {
        assertTrue(ActionRegistry.actionIds().contains("effect.spawn"))
    }

    /** 全部内置目标器都已登记。 */
    @Test
    fun registersAllTargeters() {
        val ids = ActionRegistry.targeterIds()
        val expected = listOf(
            "self", "target", "range", "sector", "rect", "source", "target.finder", "crosshair",
            "enemy", "ally", "type", "exclude-type", "name", "world", "include-self", "alive",
            "sort", "reverse", "shuffle", "limit", "online-players", "payload", "attacker", "defender",
        )
        for (id in expected) {
            assertTrue(ids.contains(id), "missing targeter: $id")
        }
        assertEquals(24, ids.size)
    }
}
