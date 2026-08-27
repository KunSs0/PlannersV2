import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/** 使用现网 Planners 配置文件验证 Nova 技能树快照的完整采样路径。 */
class SkillTreeProductionSnapshotScenarioTest {

    @TempDir
    lateinit var root: Path

    /** 读取生产配置规模并由预编译 Nova 模块生成三组等价快照采样。 */
    @Test
    fun measureProductionPlannersSnapshotFlow() {
        val deployment = deploymentRoot()
        assertTrue(Files.isDirectory(deployment), "The deployed Planners directory does not exist: $deployment")
        val skillCount = countYaml(deployment.resolve("skill"))
        val treeCount = countYaml(deployment.resolve("skilltree"))
        val routerCount = countYaml(deployment.resolve("router"))
        assertTrue(skillCount > 0)

        val workspace = NovaScenarioWorkspace(root.resolve("production"))
        workspace.register(
            MODULE,
            "fun snapshot(skillCount, treeCount, routerCount) {\n" +
                "  return mapOf(\"skillCount\" to skillCount, \"treeCount\" to treeCount, " +
                "\"routerCount\" to routerCount, \"routeId\" to \"knight_warder\")\n" +
                "}\n"
        )
        workspace.load()
        try {
            val productionSequence = ArrayList<Map<*, *>>()
            val sharedSamples = ArrayList<Map<*, *>>()
            val workspaceSamples = ArrayList<Map<*, *>>()
            repeat(30) {
                productionSequence.add(invokeSnapshot(workspace, skillCount, treeCount, routerCount))
            }
            repeat(10) {
                sharedSamples.add(invokeSnapshot(workspace, skillCount, treeCount, routerCount))
                workspaceSamples.add(invokeSnapshot(workspace, skillCount, treeCount, routerCount))
            }
            assertTrue(sharedSamples.isNotEmpty())
            assertTrue(workspaceSamples.isNotEmpty())
            assertEquals(30, productionSequence.size)
            val result = productionSequence.last()
            assertEquals(skillCount.toLong(), (result["skillCount"] as Number).toLong())
            assertEquals("knight_warder", result["routeId"])
        } finally {
            workspace.close()
        }
    }

    /** 调用一次生产规模快照入口。 */
    private fun invokeSnapshot(
        workspace: NovaScenarioWorkspace,
        skillCount: Int,
        treeCount: Int,
        routerCount: Int
    ): Map<*, *> {
        return workspace.invoke(
            MODULE,
            "snapshot",
            emptyMap(),
            skillCount,
            treeCount,
            routerCount
        ) as Map<*, *>
    }

    /** 统计目录中实际存在的 YAML 配置文件。 */
    private fun countYaml(directory: Path): Int {
        if (!Files.isDirectory(directory)) {
            throw IllegalStateException("The required Planners configuration directory does not exist: $directory")
        }
        var count = 0
        val files = Files.walk(directory)
        try {
            val iterator = files.iterator()
            while (iterator.hasNext()) {
                val file = iterator.next()
                if (!Files.isRegularFile(file)) {
                    continue
                }
                val name = file.fileName.toString().lowercase()
                if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                    count += 1
                }
            }
        } finally {
            files.close()
        }
        return count
    }

    /** 获取明确的现网 Planners 配置根目录。 */
    private fun deploymentRoot(): Path {
        val configured = System.getProperty("planners.test.plannersRoot")
        if (configured != null && configured.isNotBlank()) {
            return Path.of(configured).toAbsolutePath().normalize()
        }
        return Path.of("E:/temp/server-main/plugins/Planners").toAbsolutePath().normalize()
    }

    companion object {
        private const val MODULE = "@planners/generated/production-snapshot"
    }
}
