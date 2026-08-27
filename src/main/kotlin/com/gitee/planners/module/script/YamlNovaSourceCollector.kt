package com.gitee.planners.module.script

import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * 从 Planners 完整部署目录收集所有 YAML 内嵌 Nova 源码。
 *
 * 收集器不依赖 Registry 是否引用某个业务文件，因此现网新增但尚未进入默认资源清单的
 * 技能、职业、技能树、状态和模块配置也会在启动阶段进入 Workspace 模块图。
 */
object YamlNovaSourceCollector {

    private val templatePattern = Regex("\\{\\{(.+?)}}")
    private val modulePattern = Regex("(?m)^\\s*fun\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\(")

    /**
     * 遍历数据目录中的全部 YAML 文件并登记虚拟 SourceUnit。
     *
     * @param root Planners 插件数据目录。
     */
    fun collect(root: Path) {
        val normalizedRoot = root.toAbsolutePath().normalize()
        if (!Files.isDirectory(normalizedRoot)) {
            throw IllegalStateException("The Planners data folder does not exist: $normalizedRoot")
        }
        val files = ArrayList<Path>()
        val stream = Files.walk(normalizedRoot)
        try {
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                val file = iterator.next()
                if (!Files.isRegularFile(file)) {
                    continue
                }
                val name = file.fileName.toString().lowercase()
                if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                    files.add(file)
                }
            }
        } finally {
            stream.close()
        }
        files.sort()
        for (file in files) {
            collectFile(file)
        }
    }

    /** 读取单个 UTF-8 YAML 并递归检查全部节点。 */
    private fun collectFile(file: Path) {
        val configuration = Configuration.loadFromFile(file.toFile())
        val lines = Files.readAllLines(file, StandardCharsets.UTF_8)
        visitSection(file, lines, configuration, emptyList(), false)
    }

    /** 递归访问一个配置节。 */
    private fun visitSection(
        file: Path,
        lines: List<String>,
        section: ConfigurationSection,
        parentPath: List<String>,
        insideVariables: Boolean
    ) {
        for (key in section.getKeys(false)) {
            val path = ArrayList(parentPath)
            path.add(key)
            val value = section[key]
            val variableContext = insideVariables || key == "variables"
            visitValue(file, lines, value, path, variableContext)
        }
    }

    /** 递归访问配置值并识别脚本语义。 */
    private fun visitValue(
        file: Path,
        lines: List<String>,
        value: Any?,
        path: List<String>,
        insideVariables: Boolean
    ) {
        if (value is ConfigurationSection) {
            visitSection(file, lines, value, path, insideVariables)
            return
        }
        if (value is Map<*, *>) {
            for ((mapKey, mapValue) in value) {
                val childPath = ArrayList(path)
                childPath.add(mapKey.toString())
                visitValue(file, lines, mapValue, childPath, insideVariables)
            }
            return
        }
        if (value is List<*>) {
            for (index in value.indices) {
                val childPath = ArrayList(path)
                childPath.add(index.toString())
                visitValue(file, lines, value[index], childPath, insideVariables)
            }
            return
        }
        val scalar: String
        if (value is String) {
            scalar = value
        } else if (insideVariables && value is Number) {
            scalar = value.toString()
        } else if (insideVariables && value is Boolean) {
            scalar = value.toString()
        } else {
            return
        }
        if (scalar.isBlank()) {
            return
        }
        val yamlPath = path.joinToString(".")
        val originLine = findOriginLine(lines, path, scalar)
        registerSemanticSource(file, yamlPath, originLine, path, scalar, insideVariables)
        var templateIndex = 0
        val matches = templatePattern.findAll(scalar)
        for (match in matches) {
            val expression = match.groupValues[1].trim()
            if (expression.isEmpty()) {
                throw IllegalArgumentException("Nova template expression must not be blank: $yamlPath")
            }
            val templatePath = "$yamlPath.{{${templateIndex}}}"
            ScriptManager.registerYamlExpression(file, templatePath, originLine, expression)
            templateIndex += 1
        }
    }

    /** 根据键名与节点上下文登记表达式、语句或完整模块。 */
    private fun registerSemanticSource(
        file: Path,
        yamlPath: String,
        originLine: Int,
        path: List<String>,
        source: String,
        insideVariables: Boolean
    ) {
        val key = path.last()
        if (insideVariables && path.contains("variables")) {
            val variableIndex = path.indexOf("variables")
            val variableDepth = path.size - variableIndex - 1
            if (variableDepth == 1 || key == "condition" || key == "action") {
                ScriptManager.registerYamlExpression(file, yamlPath, originLine, source)
                return
            }
        }
        if (path.size >= 2 && path[path.size - 2] == "action") {
            ScriptManager.registerYamlExpression(file, yamlPath, originLine, source)
            return
        }
        if (key == "exper" || key == "experience" || key == "expression") {
            ScriptManager.registerYamlExpression(file, yamlPath, originLine, source)
            return
        }
        if (key == "condition" && path.contains("variables")) {
            ScriptManager.registerYamlExpression(file, yamlPath, originLine, source)
            return
        }
        if (key == "condition" && path.contains("hook")) {
            ScriptManager.registerYamlExpression(file, yamlPath, originLine, source)
            return
        }
        if (key == "consume") {
            ScriptManager.registerYamlAction(file, yamlPath, originLine, source)
            return
        }
        if (key == "action" || key == "script") {
            if (modulePattern.containsMatchIn(source)) {
                ScriptManager.registerYamlModule(file, yamlPath, originLine, source)
            } else {
                ScriptManager.registerYamlAction(file, yamlPath, originLine, source)
            }
        }
    }

    /** 在原始 YAML 中定位节点或其 scalar 内容的一基行号。 */
    private fun findOriginLine(lines: List<String>, path: List<String>, source: String): Int {
        var marker: String? = null
        val sourceLines = source.lines()
        for (line in sourceLines) {
            if (line.trim().isNotEmpty()) {
                marker = line
                break
            }
        }
        if (marker != null) {
            val normalizedMarker = marker.trim().removeSuffix(";")
            for (index in lines.indices) {
                if (lines[index].contains(normalizedMarker)) {
                    return index + 1
                }
            }
        }
        val key = path.last()
        for (index in lines.indices) {
            val trimmed = lines[index].trimStart()
            if (trimmed.startsWith("$key:")) {
                return index + 1
            }
        }
        throw IllegalArgumentException("Cannot locate YAML Nova source: ${path.joinToString(".")}")
    }
}
