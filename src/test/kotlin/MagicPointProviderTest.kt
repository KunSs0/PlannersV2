import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.event.player.PlayerMagicPointEvent
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.player.magic.DefaultMagicPointProvider
import com.gitee.planners.core.player.magic.MagicPointProvider
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.core.skill.precondition.builtin.MagicPointPreCondition
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.platform.service.PlatformIO
import taboolib.module.configuration.Configuration
import java.lang.reflect.Field
import java.util.UUID

/**
 * 真实执行提供器切换、原生任务关闭和技能/API 变更路由，仅模拟平台调度与资源存储。
 */
class MagicPointProviderTest {
    private val tasks = ArrayList<Scheduled>()
    private val reloads = ArrayList<Runnable>()
    private val services = LinkedHashMap<String, Any?>()
    private val executorState = LinkedHashMap<Field, Any?>()
    private lateinit var executorDelegate: Any
    private lateinit var bukkit: MockedStatic<Bukkit>
    private lateinit var providerField: Field
    private var previousProvider: Any? = null
    private var previousConfig: Any? = null
    private lateinit var player: Player
    private lateinit var manager: PluginManager

    /**
     * 保存全局状态，安装可检查取消状态的主线程任务队列。
     */
    @BeforeEach
    fun prepare() {
        // 包括 TabooLib 的 lazy 缓存一并隔离，避免队列泄漏到其他测试。
        val executorField = Class.forName("taboolib.common.platform.function.ExecutorKt").getDeclaredField("executorService\$delegate")
        executorField.isAccessible = true
        executorDelegate = executorField.get(null)
        for (field in executorDelegate.javaClass.declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                field.isAccessible = true
                executorState[field] = field.get(executorDelegate)
            }
        }
        val executor = Mockito.mock(PlatformExecutor::class.java, Answer { call ->
            val task = Mockito.mock(PlatformExecutor.PlatformTask::class.java)
            tasks.add(Scheduled(call.getArgument(0), task))
            task
        })
        services[PlatformExecutor::class.java.name] = PlatformFactory.serviceMap.put(PlatformExecutor::class.java.name, executor)
        services[PlatformIO::class.java.name] = PlatformFactory.serviceMap.put(PlatformIO::class.java.name, Mockito.mock(PlatformIO::class.java))
        providerField = MagicPointProvider.Companion::class.java.getDeclaredField("instance")
        providerField.isAccessible = true
        previousProvider = providerField.get(null)
        providerField.set(null, null)
        val configField = Planners::class.java.getDeclaredField("config")
        configField.isAccessible = true
        previousConfig = configField.get(null)
        configField.set(null, Mockito.mock(Configuration::class.java, Answer { call ->
            if (call.method.name == "onReload") {
                reloads.add(call.getArgument(0))
            }
            null
        }))
        bukkit = Mockito.mockStatic(Bukkit::class.java)
        bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
        manager = Mockito.mock(PluginManager::class.java)
        bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(manager)
        player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
    }

    /**
     * 关闭本例提供器，恢复平台、配置、法力所有者和档案缓存。
     */
    @AfterEach
    fun cleanup() {
        // 用生产入口释放本例任务，不关闭进入用例前的提供器。
        try {
            MagicPointProvider.shutdown()
        } finally {
            bukkit.close()
            providerField.set(null, previousProvider)
            PlayerTemplateAPI.remove(player.uniqueId)
            val configField = Planners::class.java.getDeclaredField("config")
            configField.isAccessible = true
            configField.set(null, previousConfig)
            for ((field, value) in executorState) {
                field.set(executorDelegate, value)
            }
            for ((name, previous) in services) {
                if (previous == null) {
                    PlatformFactory.serviceMap.remove(name)
                } else {
                    PlatformFactory.serviceMap[name] = previous
                }
            }
        }
    }

    /**
     * 接管取消两项原生任务，reload 和宿主已取出的旧回调都不能再读写外部资源。
     */
    @Test
    fun takeoverClosesNativeTasksAndReloadCannotReviveThem() {
        // 原生提供器在接管前确实创建上限和回复任务。
        val native = DefaultMagicPointProvider()
        MagicPointProvider.INSTANCE = native
        assertEquals(2, tasks.size)
        val replacement = Mockito.mock(MagicPointProvider::class.java)
        MagicPointProvider.INSTANCE = replacement
        assertFalse(native.enabled)
        for (scheduled in tasks) {
            Mockito.verify(scheduled.task).cancel()
            // 模拟任务已被宿主取出，取消后仍抵达入口的极端顺序。
            scheduled.runnable.executor.invoke(scheduled.task)
        }
        native.executeUpdateTask(player)
        native.executeUpdateTaskInUpperLimit(player)
        for (reload in reloads) {
            reload.run()
        }
        assertEquals(2, tasks.size, "配置重载不能为关闭的原生提供器重新创建任务")
        assertNull(native.taskUpperLimit)
        assertNull(native.taskResume)
        Mockito.verifyNoInteractions(replacement)
    }

    /**
     * 重复接入幂等、晚初始化不创建默认任务，卸载只释放当前所有者且不自动恢复默认。
     */
    @Test
    fun ownershipIsIdempotentAndUnregisterDoesNotRestartDefaultProvider() {
        val provider = Mockito.mock(MagicPointProvider::class.java)
        MagicPointProvider.INSTANCE = provider
        MagicPointProvider.INSTANCE = provider
        MagicPointProvider.init()
        assertTrue(tasks.isEmpty())
        Mockito.verify(provider, Mockito.never()).close()
        MagicPointProvider.unregister(Mockito.mock(MagicPointProvider::class.java))
        assertSame(provider, MagicPointProvider.INSTANCE)
        MagicPointProvider.unregister(provider)
        Mockito.verify(provider).close()
        assertThrows(IllegalStateException::class.java) { MagicPointProvider.INSTANCE }
        assertTrue(tasks.isEmpty())
    }

    /**
     * 技能扣费、增加和减少 API 使用存储扩展点，不使用整数展示投影回写余额。
     */
    @Test
    fun skillAndApiChangesUseProviderTransactions() {
        val provider = Mockito.mock(MagicPointProvider::class.java)
        MagicPointProvider.INSTANCE = provider
        Mockito.`when`(provider.getPoint(player)).thenReturn(12)
        Mockito.`when`(provider.consumePoint(player, 5)).thenReturn(true)
        val skill = Mockito.mock(PlayerSkill::class.java)
        val execution = SkillExecutionContext.create(Any(), 1, Mockito.mock(ImmutableSkill::class.java))
        execution.setVariable("mp", 5)
        val condition = MagicPointPreCondition()
        assertNull(condition.verify(player, skill, execution))
        condition.consume(player, skill, execution)
        Mockito.verify(provider).consumePoint(player, 5)
        PlayerTemplateAPI[player.uniqueId] = PlayerTemplate(1, player, null, emptyMap())
        Mockito.doAnswer { call ->
            // 事件监听修改后的数值也必须传入实际的资源变更。
            val event = call.getArgument<Any>(0)
            if (event is PlayerMagicPointEvent.Increase) {
                event.amount = 3
            }
            null
        }.`when`(manager).callEvent(Mockito.any())
        PlayerTemplateAPI.addMagicPoint(player, 2)
        PlayerTemplateAPI.takeMagicPoint(player, 4)
        Mockito.verify(provider).changePoint(player, 3)
        Mockito.verify(provider).changePoint(player, -4)
        for (invocation in Mockito.mockingDetails(provider).invocations) {
            assertNotEquals("setPoint", invocation.method.name, "相对变更不能通过整数投影回写")
        }
    }

    /**
     * 记录生产代码交给平台的任务，以模拟取消后已在途的回调。
     *
     * @property runnable 实际生产任务。
     * @property task 平台取消句柄。
     */
    private class Scheduled(val runnable: PlatformExecutor.PlatformRunnable, val task: PlatformExecutor.PlatformTask)
}
