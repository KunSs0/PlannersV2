package com.gitee.planners.core.config

import com.gitee.planners.module.action.query.QueryScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 验证展示文本的编译与渲染。
 *
 * 展示文本与动作参数共用 `<...>` 求值表达式语法，纯文本不产生求值开销。
 */
class DisplayTextTest {

    /**
     * 构造带技能变量与基础字段的上下文。
     *
     * @return 查询上下文。
     */
    private fun scope(): QueryScope {
        return QueryScope(
            skill = Any(),
            variables = mapOf("damage" to 50.0, "hits" to 3.0),
            skillFieldReader = { _, field ->
                when (field) {
                    "level" -> 5L
                    "name" -> "裂地斩"
                    else -> null
                }
            },
            label = "test",
        )
    }

    /** 纯文本文本按原文返回，并标记为非动态。 */
    @Test
    fun staticTextStaysRaw() {
        val text = DisplayText.compile("&7提升攻击速度。", "test.lore")
        assertFalse(text.dynamic)
        assertEquals("&7提升攻击速度。", text.render(scope()))
    }

    /** 文案里的占位符按变量取值。 */
    @Test
    fun rendersPlaceholderFromVariables() {
        val text = DisplayText.compile("&7每段伤害: &b<skill.var.damage>", "test.lore")
        assertTrue(text.dynamic)
        assertEquals("&7每段伤害: &b50", text.render(scope()))
    }

    /** 占位符里可以写表达式，与动作参数一致。 */
    @Test
    fun rendersExpressionPlaceholder() {
        val text = DisplayText.compile("总伤害 <skill.var.damage * skill.var.hits>", "test.lore")
        assertEquals("总伤害 150", text.render(scope()))
    }

    /** 同一上下文可复用于名称与多行 Lore，结果互不影响。 */
    @Test
    fun sharesScopeAcrossLines() {
        val lines = DisplayText.compileList(
            listOf("等级 &fLv<skill.level>", "段数 &b<skill.var.hits>", "---"),
            "test.lore",
        )
        val context = scope()
        assertEquals("等级 &fLv5", lines[0].render(context))
        assertEquals("段数 &b3", lines[1].render(context))
        assertEquals("---", lines[2].render(context))
        assertFalse(lines[2].dynamic)
    }

    /** 缺失的取值渲染为空串，与动作参数行为一致。 */
    @Test
    fun missingValueRendersEmpty() {
        val text = DisplayText.compile("威力 <skill.var.unknown>", "test.lore")
        assertEquals("威力 ", text.render(scope()))
    }

    /** 旧的 `{{...}}` 写法在加载期直接报错。 */
    @Test
    fun rejectsLegacyDelimiter() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            DisplayText.compile("等级 {{skill.level}}", "test.lore")
        }
        assertTrue(exception.message!!.contains("{{"))
        assertTrue(exception.message!!.contains("test.lore"))
    }

    /** 多行编译的报错信息带行下标。 */
    @Test
    fun listLabelCarriesIndex() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            DisplayText.compileList(listOf("正常", "{{skill.level}}"), "skill:x.display.icon.lore")
        }
        assertTrue(exception.message!!.contains("lore[1]"))
    }

    /** 插件自带默认技能的展示文本必须能按当前语法编译。 */
    @Test
    fun defaultSkillDisplayTextsCompile() {
        val files = File("src/main/resources/skill").listFiles { file -> file.extension == "yml" }
        assertTrue(files != null && files.isNotEmpty(), "默认资源缺少技能示例")
        for (file in files) {
            val config = Configuration.loadFromFile(file)
            val id = file.nameWithoutExtension
            val name = config.getString("__option__.name")
            if (name != null) {
                DisplayText.compile(name, "skill:$id.__option__.name")
            }
            val iconName = config.getString("__option__.display.icon.name")
            if (iconName != null) {
                DisplayText.compile(iconName, "skill:$id.display.icon.name")
            }
            DisplayText.compileList(
                config.getStringList("__option__.display.icon.lore"),
                "skill:$id.display.icon.lore",
            )
        }
    }
}
