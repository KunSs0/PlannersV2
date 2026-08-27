import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/** 验证技能变量在预编译 Nova Workspace 中的重绑定、隔离与复用语义。 */
class SkillTreeRuntimeScenarioTest {

    @TempDir
    lateinit var root: Path

    /** 同一预编译入口可以使用不同调用绑定计算玩家等级变量。 */
    @Test
    fun oneSessionCanRebindPlayerValuesBetweenSkillCalculations() {
        val workspace = createWorkspace(root.resolve("rebind"))
        try {
            val first = workspace.invoke(MODULE, "multiply", mapOf("level" to 2))
            val second = workspace.invoke(MODULE, "multiply", mapOf("level" to 4))
            assertEquals(20L, (first as Number).toLong())
            assertEquals(40L, (second as Number).toLong())
        } finally {
            workspace.close()
        }
    }

    /** 两次技能变量批量结果必须保持对象和值隔离。 */
    @Test
    fun skillVariableBatchKeepsValuesIsolatedAfterRebind() {
        val workspace = createWorkspace(root.resolve("batch"))
        try {
            val first = workspace.invoke(MODULE, "buildVariables", mapOf("level" to 3)) as Map<*, *>
            val second = workspace.invoke(MODULE, "buildVariables", mapOf("level" to 7)) as Map<*, *>
            assertEquals(30L, (first["power"] as Number).toLong())
            assertEquals("skill-3", first["label"])
            assertEquals(70L, (second["power"] as Number).toLong())
            assertEquals("skill-7", second["label"])
            assertNotSame(first, second)
        } finally {
            workspace.close()
        }
    }

    /** 一个 Workspace 复用预编译模块，独立 Workspace 仍保持生命周期隔离。 */
    @Test
    fun sessionReuseAvoidsCreatingOneContextPerSkill() {
        val shared = createWorkspace(root.resolve("shared"))
        val separate = createWorkspace(root.resolve("separate"))
        try {
            val sharedFirst = shared.invoke(MODULE, "increment", mapOf("level" to 1))
            val sharedSecond = shared.invoke(MODULE, "increment", mapOf("level" to 2))
            val separateValue = separate.invoke(MODULE, "increment", mapOf("level" to 1))
            assertEquals(2L, (sharedFirst as Number).toLong())
            assertEquals(3L, (sharedSecond as Number).toLong())
            assertEquals(2L, (separateValue as Number).toLong())
            assertTrue(shared !== separate)
        } finally {
            shared.close()
            separate.close()
        }
    }

    /** 采样预编译 Workspace 复用与独立构建的非零耗时。 */
    @Test
    fun measureSessionReuseCost() {
        val shared = createWorkspace(root.resolve("performance-shared"))
        val sharedStart = System.nanoTime()
        repeat(1000) { index ->
            shared.invoke(MODULE, "multiply", mapOf("level" to index))
        }
        val sharedElapsed = System.nanoTime() - sharedStart
        shared.close()

        val separateStart = System.nanoTime()
        repeat(20) { index ->
            val workspace = createWorkspace(root.resolve("performance-$index"))
            workspace.invoke(MODULE, "multiply", mapOf("level" to index))
            workspace.close()
        }
        val separateElapsed = System.nanoTime() - separateStart
        println(
            "[SkillTreeNovaPerfTest] sharedTotalMs=" + sharedElapsed / 1_000_000.0 +
                " separateTotalMs=" + separateElapsed / 1_000_000.0
        )
        assertTrue(sharedElapsed > 0L)
        assertTrue(separateElapsed > 0L)
    }

    /** 创建并预编译当前场景使用的通用技能变量模块。 */
    private fun createWorkspace(path: Path): NovaScenarioWorkspace {
        val workspace = NovaScenarioWorkspace(path)
        workspace.register(
            MODULE,
            "fun multiply() = level * 10\n" +
                "fun increment() = level + 1\n" +
                "fun buildVariables() = mapOf(\"power\" to level * 10, \"label\" to \"skill-\" + level)\n"
        )
        workspace.load()
        return workspace
    }

    companion object {
        private const val MODULE = "@planners/generated/runtime-scenario"
    }
}
