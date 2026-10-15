import org.bukkit.Bukkit
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/** 隐藏真实 HaiderCore 类，验证可选依赖缺失时的类加载隔离和明确失败语义。 */
class HaiderCoreCompatClassLoadingTest {
    /** 插件缺失时不解析 API；插件存在但 API 缺失时返回失败 Future，不直接抛链接错误。 */
    @Test
    fun isolatesMissingApiAndReportsExplicitFailure() {
        val plugins = Mockito.mock(PluginManager::class.java)
        val bukkit = Mockito.mockStatic(Bukkit::class.java)
        try {
            bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(plugins)
            for (enabled in listOf(false, true)) {
                Mockito.`when`(plugins.isPluginEnabled("HaiderCore")).thenReturn(enabled)
                val loader = MissingApiLoader(javaClass.classLoader)
                val type = loader.loadClass("com.gitee.planners.module.compat.haidercore.HaiderCoreCompat")
                val instance = type.getField("INSTANCE").get(null)
                val result = type.getMethod("read", UUID::class.java).invoke(instance, UUID.randomUUID())
                assertInstanceOf(CompletableFuture::class.java, result)
                val future = result as CompletableFuture<*>
                val failure = assertThrows(CompletionException::class.java) { future.join() }
                assertInstanceOf(IllegalStateException::class.java, failure.cause)
                assertEquals(enabled, loader.requestedApi)
                if (enabled) {
                    assertTrue(failure.cause!!.message!!.contains("API"))
                } else {
                    assertTrue(failure.cause!!.message!!.contains("已启用"))
                }
            }
        } finally {
            bukkit.close()
        }
    }

    /** 重新加载兼容实现，并拒绝访问全部 HaiderCore 类，模拟未提供编译依赖的运行环境。 */
    private class MissingApiLoader(parent: ClassLoader) : ClassLoader(parent) {
        var requestedApi: Boolean = false

        /** 兼容包由隔离加载器定义，Bukkit 等宿主契约仍共享父加载器。 */
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name.startsWith("com.gitee.haidercore.")) {
                requestedApi = true
                throw ClassNotFoundException(name)
            }
            if (!name.startsWith("com.gitee.planners.module.compat.haidercore.")) {
                return super.loadClass(name, resolve)
            }
            synchronized(getClassLoadingLock(name)) {
                var type = findLoadedClass(name)
                if (type == null) {
                    val stream = parent.getResourceAsStream(name.replace('.', '/') + ".class")
                    require(stream != null) { "缺少测试类字节码：$name" }
                    val bytes: ByteArray
                    try {
                        bytes = stream.readBytes()
                    } finally {
                        stream.close()
                    }
                    type = defineClass(name, bytes, 0, bytes.size)
                }
                if (resolve) {
                    resolveClass(type)
                }
                return type
            }
        }
    }
}
