import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerLevelChangedEvent
import com.gitee.planners.api.event.player.PlayerProfileLoadedEvent
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.compat.attribute.AttributeDriver
import com.gitee.planners.module.compat.attribute.AttributeProvider
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
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
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 执行真实属性汇总、驱动注册及事件刷新，仅模拟 Bukkit、调度与等级算法边界。
 */
class AttributePipelineTest {

    private val pending = ArrayList<PlatformExecutor.PlatformRunnable>()
    private val services = HashMap<String, Any?>()
    private val executorState = LinkedHashMap<java.lang.reflect.Field, Any?>()
    private lateinit var executorDelegate: Any
    private lateinit var bukkit: MockedStatic<Bukkit>
    private lateinit var player: Player
    private lateinit var template: PlayerTemplate
    private lateinit var driver: AttributeDriver
    private lateinit var source: AttributeSource
    private lateinit var manager: PluginManager
    private var amount = 11.2

    /**
     * 安装可控主线程队列、就绪档案和合成属性来源。
     */
    @BeforeEach
    fun prepare() {
        amount = 11.2
        val executorField = Class.forName("taboolib.common.platform.function.ExecutorKt").getDeclaredField("executorService\$delegate")
        executorField.isAccessible = true
        executorDelegate = executorField.get(null)
        // 测试结束恢复调度器的完整懒加载状态，避免跨测试持有旧队列。
        for (field in executorDelegate.javaClass.declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                field.isAccessible = true
                executorState[field] = field.get(executorDelegate)
            }
        }
        // 调度任务只入队，测试明确决定提交和执行的先后顺序。
        val task = Mockito.mock(PlatformExecutor.PlatformTask::class.java)
        val executor = Mockito.mock(PlatformExecutor::class.java, Answer { call ->
            pending.add(call.getArgument(0))
            task
        })
        for ((name, service) in mapOf(PlatformExecutor::class.java.name to executor,
            PlatformIO::class.java.name to Mockito.mock(PlatformIO::class.java))) {
            services[name] = PlatformFactory.serviceMap.put(name, service)
        }
        bukkit = Mockito.mockStatic(Bukkit::class.java)
        bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
        manager = Mockito.mock(PluginManager::class.java)
        bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(manager)
        player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
        Mockito.`when`(player.isOnline).thenReturn(true)
        bukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(player))
        template = PlayerTemplate(1, player, null, emptyMap())
        PlayerTemplateAPI[player.uniqueId] = template
        Planners.attributeRegistry.reset(Configuration.fromMap(emptyMap<String, Any>()))
        source = object : AttributeSource {
            override val id = "synthetic-refresh"
            override val priority = 100

            /**
             * 返回测试执行时的最新属性，用于检测是否捕获旧快照。
             */
            override fun getAttributes(entity: LivingEntity): Map<String, Double> {
                return mapOf("endurance" to amount)
            }
        }
        AttributeProxy.register(source)
        driver = Mockito.mock(AttributeDriver::class.java, Mockito.CALLS_REAL_METHODS)
        Mockito.`when`(driver.checkEnable()).thenReturn(true)
        AttributeProvider.e(PluginReloadEvents.Post())
        pending.clear()
    }

    /**
     * 恢复全局服务，避免测试影响其他插件场景。
     */
    @AfterEach
    fun cleanup() {
        // 先失效排队任务，再移除测试档案与来源。
        AttributeDriver.unregister(driver)
        AttributeProvider.invalidate()
        pending.clear()
        AttributeProxy.unregister(source)
        PlayerTemplateAPI.remove(player.uniqueId)
        bukkit.close()
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

    /**
     * 驱动注册自动补刷现有在线档案，重复注册不再次调度，其他所有者不能覆盖。
     */
    @Test
    fun registrationRefreshesExistingProfilesAndEnforcesOwnership() {
        AttributeDriver.register(driver)
        assertEquals(1, pending.size)
        AttributeDriver.register(driver)
        assertEquals(1, pending.size)
        val foreign = Mockito.mock(AttributeDriver::class.java)
        Mockito.`when`(foreign.checkEnable()).thenReturn(true)
        assertThrows(IllegalStateException::class.java) { AttributeDriver.register(foreign) }
        AttributeDriver.unregister(foreign)
        assertSame(driver, AttributeDriver.current())
        drain()
        Mockito.verify(driver).set(player, "planners-proxy", listOf("endurance: +11.2"), -1)
        Mockito.verify(driver).onRegister()
        AttributeDriver.unregister(driver)
        Mockito.verify(driver).onUnregister()
        var removed = 0
        for (invocation in Mockito.mockingDetails(driver).invocations) {
            if (invocation.method.name == "remove") {
                val target = invocation.arguments[0] as com.gitee.planners.api.job.target.ProxyTarget.BukkitEntity
                assertSame(player, target.instance)
                assertEquals("planners-proxy", invocation.arguments[1])
                removed += 1
            }
        }
        assertEquals(1, removed)
        assertThrows(IllegalStateException::class.java) {
            AttributeDriver.set(player, "planners-proxy", emptyList(), -1)
        }
    }

    /**
     * 异步档案事件不直接写驱动，排队后使用最新属性，离线或换档案后丢弃。
     */
    @Test
    fun profileRefreshUsesLatestStateAndRejectsDisconnectedProfiles() {
        AttributeDriver.register(driver)
        drain()
        Mockito.clearInvocations(driver)
        val event = PlayerProfileLoadedEvent(template)
        CompletableFuture.runAsync { AttributeProvider.e(event) }.join()
        Mockito.verifyNoInteractions(driver)
        amount = 12.75
        drain()
        Mockito.verify(driver).set(player, "planners-proxy", listOf("endurance: +12.75"), -1)
        Mockito.clearInvocations(driver)
        AttributeProvider.refresh(template)
        Mockito.`when`(player.isOnline).thenReturn(false)
        drain()
        Mockito.verifyNoInteractions(driver)
        Mockito.`when`(player.isOnline).thenReturn(true)
        AttributeProvider.refresh(template)
        PlayerTemplateAPI[player.uniqueId] = PlayerTemplate(2, player, null, emptyMap())
        drain()
        Mockito.verifyNoInteractions(driver)
    }

    /**
     * 重载中拒绝刷新，成功后补刷，卸载取消旧驱动的排队写入。
     */
    @Test
    fun reloadAndUnregisterInvalidatePendingWrites() {
        AttributeDriver.register(driver)
        drain()
        Mockito.clearInvocations(driver)
        AttributeProvider.refresh(template)
        AttributeProvider.e(PluginReloadEvents.Pre())
        AttributeProvider.refresh(template)
        drain()
        Mockito.verifyNoInteractions(driver)
        amount = 0.36
        AttributeProvider.e(PluginReloadEvents.Post())
        drain()
        Mockito.verify(driver).set(player, "planners-proxy", listOf("endurance: +0.36"), -1)
        Mockito.clearInvocations(driver)
        AttributeProvider.refresh(template)
        AttributeDriver.unregister(driver)
        Mockito.verify(driver).onUnregister()
        Mockito.clearInvocations(driver)
        drain()
        Mockito.verifyNoInteractions(driver)
    }

    /**
     * 直接设置等级与异步经验升级均在实际提交后通知，未变化时不重复通知。
     */
    @Test
    fun levelNotificationFollowsActualCommitAndAsyncCompletion() {
        // 路由算法是外部边界，模板通知与事件派发执行真实实现。
        val router = Mockito.mock(PlayerRouter::class.java)
        var level = 1
        Mockito.`when`(router.level).thenAnswer { level }
        Mockito.`when`(router.minLevel).thenReturn(1)
        Mockito.doAnswer {
            level = 10
            null
        }.`when`(router).setLevel(99, player)
        template.playerRouter = router
        val events = ArrayList<PlayerLevelChangedEvent>()
        Mockito.doAnswer { call ->
            val event = call.getArgument<org.bukkit.event.Event>(0)
            if (event is PlayerLevelChangedEvent) {
                assertEquals(template.level, event.to)
                events.add(event)
            }
            null
        }.`when`(manager).callEvent(Mockito.any())
        template.setLevel(99)
        assertEquals(10, events.single().to)
        template.setLevel(10)
        assertEquals(1, events.size)
        val future = CompletableFuture<Void>()
        Mockito.`when`(router.addExperience(100, player)).thenReturn(future)
        val completion = template.addExperience(100)
        assertEquals(1, events.size)
        level = 11
        future.complete(null)
        completion.join()
        assertEquals(2, events.size)
        assertEquals(10, events[1].from)
        assertEquals(11, events[1].to)
    }

    /**
     * 数值写入统一隔离调用方 Map，并在驱动注销后拒绝旧代任务。
     */
    @Test
    fun numericHookCopiesInputAndRejectsOldDriverTasks() {
        AttributeDriver.register(driver)
        drain()
        Mockito.clearInvocations(driver)
        val values = linkedMapOf("mana-regen" to 0.36)
        val worker = CompletableFuture.runAsync {
            // Bukkit 静态 mock 具有线程所有权，只模拟本次调用的线程标记。
            val threadBukkit = Mockito.mockStatic(Bukkit::class.java)
            try {
                threadBukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(false)
                AttributeDriver.setValues(player, "test-numeric", values, -1)
            } finally {
                threadBukkit.close()
            }
        }
        worker.join()
        Mockito.verifyNoInteractions(driver)
        values["mana-regen"] = 9.0
        drain()
        Mockito.verify(driver).setValues(player, "test-numeric", mapOf("mana-regen" to 0.36), -1)
        Mockito.verify(driver).set(player, "test-numeric", listOf("mana-regen: +0.36"), -1)
        AttributeDriver.setValues(player, "test-numeric", values, -1)
        AttributeDriver.unregister(driver)
        Mockito.clearInvocations(driver)
        drain()
        Mockito.verifyNoInteractions(driver)
    }

    /**
     * 依次执行所有主线程任务，包括任务内部提交的后续刷新。
     */
    private fun drain() {
        // 每次取出一批，避免遍历过程中任务增加导致并发修改。
        val task = Mockito.mock(PlatformExecutor.PlatformTask::class.java)
        while (pending.isNotEmpty()) {
            val batch = pending.toList()
            pending.clear()
            for (runnable in batch) {
                assertFalse(runnable.async)
                runnable.executor.invoke(task)
            }
        }
    }
}
