import com.gitee.planners.module.script.NovaScriptUnit
import com.gitee.planners.module.script.PlannersScriptProgram
import com.novalang.runtime.compilation.BytecodeArtifactCache
import com.novalang.runtime.compilation.SourceUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * 验证插件级字节码缓存能在多次程序编译之间复用未变更模块，
 * 同时每次编译仍得到独立可调用的程序实例。
 */
class NovaBytecodeCacheReuseTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun reusesUnchangedModuleArtifactsAcrossPrograms() {
        write("script/nova.config.yml", "version: 1\nname: planners-test\naliases:\n  \"@planners\": \"planners\"\n")
        write("script/planners/bootstrap.nova", "fun main() { }\n")
        val moduleSource = "fun evaluate(): Int {\n    return 42\n}\n"
        val source = SourceUnit(
            "@planners/cache-test",
            moduleSource,
            root.resolve("cache-test.yml"),
            "cache-test",
            1,
            0,
            null
        )
        val cache = BytecodeArtifactCache()
        val config = root.resolve("script/nova.config.yml")
        val sources = listOf(source)

        val first = PlannersScriptProgram.compile(config, sources, cache) { nova ->
            nova.setScriptClassLoader(NovaBytecodeCacheReuseTest::class.java.classLoader)
        }
        first.initialize()
        val entriesAfterFirst = cache.size()

        val second = PlannersScriptProgram.compile(config, sources, cache) { nova ->
            nova.setScriptClassLoader(NovaBytecodeCacheReuseTest::class.java.classLoader)
        }
        second.initialize()

        assertEquals(entriesAfterFirst, cache.size(), "unchanged modules must reuse cached bytecode")
        assertEquals(42, first.invoke(NovaScriptUnit("@planners/cache-test", "evaluate"), emptyMap()))
        assertEquals(42, second.invoke(NovaScriptUnit("@planners/cache-test", "evaluate"), emptyMap()))

        first.close()
        second.close()
    }

    private fun write(relative: String, content: String) {
        val target = root.resolve(relative)
        Files.createDirectories(target.parent)
        Files.writeString(target, content, StandardCharsets.UTF_8)
    }
}
