import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.config.ImmutableVariable
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.novalang.runtime.SchedulerHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

/**
 * 用真实 YAML、变量定义、Nova 编译器和 action 绑定验证常量与显式公式的边界。
 */
class ImmutableVariableTest {
    @TempDir
    lateinit var root: Path

    /**
     * 清理本用例安装的宿主调度器。
     */
    @AfterEach
    fun cleanup() {
        SchedulerHolder.clear()
    }

    /**
     * 原始字符串不被编译、插值或隐式转为数字，公式仍可引用此前声明的常量。
     */
    @Test
    fun literalsAndExpressionsReachActionBindingsWithoutReevaluation() {
        val yaml = """
            hitType: hurt1
            attackLevel: 1
            enabled: true
            numericText: '0'
            code: 'error("must remain data")'
            special: '引号"、反斜杠\、DOLLARlevel、{{level}}'
            values: [1, 2, 3]
            amount:
              expression: attackLevel + level * 10
            chained:
              expression: amount + values[1]
        """.trimIndent().replace("DOLLAR", '$'.toString())
        val decoded = Yaml().load<Map<String, Any>>(yaml)
        val variables = LinkedHashMap<String, ImmutableVariable>()
        val source = StringBuilder("fun evaluate(level, " + ImmutableVariable.LITERALS_PARAMETER + ") {\n")
        for ((id, value) in decoded) {
            val variable = ImmutableVariable.parse(id, value)
            variables[id] = variable
            variable.appendDisplayEvaluation(source)
        }
        source.append("return [hitType, attackLevel, enabled, numericText, code, special, values, amount, chained]\n}\n")
        source.append("fun read() { return this.hitType }\n")
        val workspace = NovaScenarioWorkspace(root)
        try {
            workspace.register("@planners/variables", source.toString())
            workspace.load()
            val values = workspace.invokePure("@planners/variables", "evaluate", 2, ImmutableVariable.literalBindings(variables)) as List<*>
            assertEquals("hurt1", values[0])
            assertEquals(1, values[1])
            assertEquals(true, values[2])
            assertEquals("0", values[3])
            assertEquals(decoded["code"], values[4])
            assertEquals(decoded["special"], values[5])
            assertEquals(listOf(1, 2, 3), values[6])
            assertEquals(21, (values[7] as Number).toInt())
            assertEquals(23, (values[8] as Number).toInt())
            val execution = SkillExecutionContext.create("synthetic-sender", 2, Mockito.mock(ImmutableSkill::class.java))
            execution.setVariable("hitType", values[0])
            assertEquals("hurt1", workspace.invoke("@planners/variables", "read", execution.actionBindings()))
            assertEquals("other", workspace.invoke("@planners/variables", "read", mapOf("hitType" to "other")))
            assertEquals("hurt1", execution.getVariable("hitType"))
        } finally {
            workspace.close()
        }
    }

    /**
     * 普通字符串永远是数据；显式表达式结构错误立即拒绝。
     */
    @Test
    fun expressionSyntaxIsExplicitAndRejectsUnknownFields() {
        assertTrue(ImmutableVariable.parse("text", "level + 1") is ImmutableVariable.Literal)
        assertTrue(ImmutableVariable.parse("text", "\"hurt1\"") is ImmutableVariable.Literal)
        assertTrue(ImmutableVariable.parse("amount", mapOf("expression" to "level + 1")) is ImmutableVariable.Expression)
        for (value in listOf(emptyMap<String, Any>(), mapOf("expr" to "level"), mapOf("expression" to ""), mapOf("expression" to 1), mapOf("expression" to "level", "extra" to true))) {
            assertThrows(IllegalArgumentException::class.java) {
                ImmutableVariable.parse("amount", value)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImmutableVariable.parse(ImmutableVariable.LITERALS_PARAMETER, "reserved")
        }
    }

    /**
     * 条件变量继续使用显式 condition/action，普通空列表是有效常量。
     */
    @Test
    fun conditionalVariablesKeepTheirDeclaredOrder() {
        val variable = ImmutableVariable.parse("choice", listOf(
            mapOf("condition" to "level > 1", "action" to "20"),
            mapOf("condition" to "true", "action" to "10")
        ))
        val source = StringBuilder("fun choose(level) {\n")
        variable.appendDisplayEvaluation(source)
        source.append("return choice\n}")
        val workspace = NovaScenarioWorkspace(root)
        try {
            workspace.register("@planners/branches", source.toString())
            workspace.load()
            assertEquals(10, (workspace.invokePure("@planners/branches", "choose", 1) as Number).toInt())
            assertEquals(20, (workspace.invokePure("@planners/branches", "choose", 2) as Number).toInt())
            assertTrue(ImmutableVariable.parse("empty", emptyList<Any>()) is ImmutableVariable.Literal)
        } finally {
            workspace.close()
        }
    }

    /**
     * 常量列表保持每次调用独立，修改某次绑定不能污染配置和后续绑定。
     */
    @Test
    fun literalListsHaveIndependentInvocationSnapshots() {
        val original = arrayListOf<Any>(1, arrayListOf(2, 3))
        val variable = ImmutableVariable.parse("values", original)
        val variables = mapOf("values" to variable)
        val first = ImmutableVariable.literalBindings(variables)["values"] as MutableList<*>
        first.clear()
        original.clear()
        assertEquals(listOf(1, listOf(2, 3)), ImmutableVariable.literalBindings(variables)["values"])
    }
}
