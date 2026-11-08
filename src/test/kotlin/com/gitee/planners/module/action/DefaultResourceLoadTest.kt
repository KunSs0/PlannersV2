package com.gitee.planners.module.action

import com.gitee.planners.core.config.State
import com.gitee.planners.core.config.level.LevelCallback
import com.gitee.planners.module.action.query.QueryRef
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 校验插件自带默认配置可以按当前实现加载。
 *
 * `src/main/resources` 是全新安装时释放出来的通用配置，与业务配置独立演化；
 * 这里把默认资源逐个过一遍当前解析器，避免再出现「代码改了、默认配置没改」。
 */
class DefaultResourceLoadTest {

    /** 默认资源根目录。 */
    private val root = File("src/main/resources")

    /** 每个用例前恢复内置条目，避免外部注册影响解析。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 默认资源里的技能配置必须能按结构化动作解析；纯被动可以没有 action。 */
    @Test
    fun defaultSkillsLoadAsStructuredActions() {
        val files = yamlFiles(File(root, "skill"))
        assertTrue(files.isNotEmpty(), "默认资源缺少技能示例")
        for (file in files) {
            val config = Configuration.loadFromFile(file)
            val id = file.nameWithoutExtension
            val raw = config.get("action")
            if (raw == null) {
                continue
            }
            val actionSection = config.getConfigurationSection("action")
            assertNotNull(actionSection, "skill/$id 的 action 必须是流程映射而不是脚本字符串")
            ActionParser.parse(id, actionSection!!, "skill:$id.action")
        }
    }

    /** 默认状态配置必须能按命名流程解析。 */
    @Test
    fun defaultStatesLoadAsNamedFlows() {
        val files = yamlFiles(File(root, "state"))
        assertTrue(files.isNotEmpty(), "默认资源缺少状态示例")
        for (file in files) {
            val config = Configuration.loadFromFile(file)
            for (id in config.getKeys(false).toList()) {
                val section = config.getConfigurationSection(id)
                assertNotNull(section, "state/$id 必须是配置节点")
                val flows = ActionParser.parseNamedFlows(section!!, "state:$id", State.FLOW_NAMES)
                assertTrue(flows.isNotEmpty(), "state/$id 没有任何生命周期流程")
            }
        }
    }

    /** 默认等级算法的经验表达式必须能被查询引擎解析。 */
    @Test
    fun defaultLevelExpressionsCompile() {
        val files = yamlFiles(File(root, "module/level"))
        assertTrue(files.isNotEmpty(), "默认资源缺少等级算法示例")
        for (file in files) {
            val config = Configuration.loadFromFile(file)
            for (id in config.getKeys(false).toList()) {
                val expression = config.getString("$id.experience")
                assertNotNull(expression, "module/level/$id 缺少 experience")
                QueryRef.parseVariableExpression(expression!!, "level:$id:experience")
            }
        }
    }

    /** 默认 config.yml 的法力表达式必须能被查询引擎解析。 */
    @Test
    fun defaultMagicPointExpressionsCompile() {
        val config = Configuration.loadFromFile(File(root, "config.yml"))
        for (key in listOf("resume", "upper-limit")) {
            val expression = config.getString("settings.magic-point.$key.expression")
            assertNotNull(expression, "config.yml 缺少 settings.magic-point.$key.expression")
            QueryRef.parseVariableExpression(expression!!, "magic-point:$key")
        }
    }

    /** 默认资源不得再出现脚本写法，等级回调只接受控制台命令。 */
    @Test
    fun defaultResourcesHaveNoScriptBodies() {
        val markers = listOf("action: |", "fun execute", "nova:", "expression: 'if (")
        for (file in yamlFiles(root)) {
            val text = file.readText()
            for (marker in markers) {
                assertTrue(!text.contains(marker), "默认资源含有脚本写法 '$marker'：${file.path}")
            }
        }
    }

    /** 默认资源的等级回调必须能解析成控制台命令。 */
    @Test
    fun defaultLevelCallbacksParse() {
        val config = Configuration.loadFromFile(File(root, "module/level/example.yml"))
        for (id in config.getKeys(false).toList()) {
            val section = config.getConfigurationSection("$id.callbacks") ?: continue
            for (level in section.getKeys(false).toList()) {
                val raw = section.get(level)
                val texts = if (raw is List<*>) raw.map { it.toString() } else listOf(raw.toString())
                for (text in texts) {
                    assertNotNull(LevelCallback.parse(text), "module/level/$id.callbacks.$level 为空")
                }
            }
        }
    }

    /**
     * 列出目录下的全部 YAML 文件。
     *
     * @param directory 目录。
     * @return 文件列表；目录不存在时为空。
     */
    private fun yamlFiles(directory: File): List<File> {
        if (!directory.isDirectory) {
            return emptyList()
        }
        return directory.walkTopDown()
            .filter { it.isFile && (it.extension == "yml" || it.extension == "yaml") }
            .toList()
    }
}
