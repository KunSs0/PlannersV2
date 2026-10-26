import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerRouteClearedEvent
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.timing.SkillCastingSessions
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
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 通过真实模板入口验证内存提交、属性、事件与异步持久化的先后关系。
 * 仅模拟 Bukkit、数据库和任务调度边界，不复制清空实现。
 */
class PlayerRouteClearTest {
    private val pending = ArrayList<PlatformExecutor.PlatformRunnable>()
    private val services = LinkedHashMap<String, Any?>()
    private val lazyStates = LinkedHashMap<Any, Map<Field, Any?>>()
    private val events = ArrayList<PlayerRouteClearedEvent>()
    private lateinit var bukkit: MockedStatic<Bukkit>
    private lateinit var player: Player
    private lateinit var template: PlayerTemplate
    private lateinit var router: PlayerRouter
    private lateinit var driver: AttributeDriver
    private lateinit var database: Database
    private lateinit var source: AttributeSource
    private lateinit var logger: Logger
    private var rejectAsync = false
    private val chips = linkedMapOf("equipment" to 25.0, "planners-proxy" to 100.0)

    /**
     * 安装保留 now 语义的调度器，以及能够观察属性提交时序的外部驱动。
     */
    @BeforeEach
    fun prepare() {
        // 还原全部懒加载状态，避免测试向其他用例泄漏数据库或调度器。
        val executorField = Class.forName("taboolib.common.platform.function.ExecutorKt")
            .getDeclaredField("executorService\$delegate")
        executorField.isAccessible = true
        rememberLazy(executorField.get(null))
        val task = Mockito.mock(PlatformExecutor.PlatformTask::class.java)
        val executor = Mockito.mock(PlatformExecutor::class.java, Answer { call ->
            val runnable = call.getArgument<PlatformExecutor.PlatformRunnable>(0)
            if (runnable.now) {
                runnable.executor.invoke(task)
            } else if (runnable.async && rejectAsync) {
                throw IllegalStateException("synthetic scheduler failure")
            } else {
                pending.add(runnable)
            }
            task
        })
        for ((name, service) in mapOf(PlatformExecutor::class.java.name to executor,
            PlatformIO::class.java.name to Mockito.mock(PlatformIO::class.java))) {
            services[name] = PlatformFactory.serviceMap.put(name, service)
        }
        bukkit = Mockito.mockStatic(Bukkit::class.java)
        bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
        player = Mockito.mock(Player::class.java)
        Mockito.`when`(player.uniqueId).thenReturn(UUID.randomUUID())
        Mockito.`when`(player.isOnline).thenReturn(true)
        logger = Mockito.mock(Logger::class.java)
        bukkit.`when`<Logger> { Bukkit.getLogger() }.thenReturn(logger)
        bukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(emptyList())
        val manager = Mockito.mock(PluginManager::class.java)
        bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(manager)
        router = Mockito.mock(PlayerRouter::class.java)
        Mockito.`when`(router.bindingId).thenReturn(42L)
        Mockito.`when`(router.routerId).thenReturn("synthetic-job")
        template = PlayerTemplate(1L, player, router, emptyMap())
        PlayerTemplateAPI[player.uniqueId] = template
        Mockito.doAnswer { call ->
            val event = call.getArgument<org.bukkit.event.Event>(0)
            if (event is PlayerRouteClearedEvent) {
                // 监听方必须已经观察到最终属性，且数据库尚未执行。
                assertNull(template.playerRouter)
                assertNull(template.getRegisteredSkillOrNull("old-skill"))
                assertNull(template.getEquippedSkillByBackpackSlot("page", "slot"))
                assertEquals(0.0, chips["planners-proxy"])
                assertEquals(25.0, chips["equipment"])
                Mockito.verifyNoInteractions(database)
                events.add(event)
            }
            null
        }.`when`(manager).callEvent(Mockito.any())
        database = Mockito.mock(Database::class.java)
        val databaseField = Database.Companion::class.java.getDeclaredField("INSTANCE\$delegate")
        databaseField.isAccessible = true
        val delegate = databaseField.get(null)
        rememberLazy(delegate)
        val valueField = delegate.javaClass.getDeclaredField("_value")
        valueField.isAccessible = true
        valueField.set(delegate, database)
        Planners.attributeRegistry.reset(Configuration.fromMap(emptyMap<String, Any>()))
        source = object : AttributeSource {
            override val id = "synthetic-profession-clear"
            override val priority = 100

            /**
             * 模拟仅随职业路由存在的属性来源，聚合由真实 AttributeProxy 执行。
             */
            override fun getAttributes(entity: LivingEntity): Map<String, Double> {
                // 清空路由后不得再贡献旧职业数值。
                if (template.playerRouter == null) {
                    return emptyMap()
                }
                return mapOf("synthetic-capacity" to 100.0)
            }
        }
        AttributeProxy.register(source)
        driver = Mockito.mock(AttributeDriver::class.java, Answer { call ->
            // Kotlin 非空参数不使用会返回 null 的 Mockito.eq 匹配占位值。
            if (call.method.name == "setValues") {
                val id = call.getArgument<String>(1)
                val values = call.getArgument<Map<String, Double>>(2)
                chips[id] = values.values.sum()
                null
            } else if (call.method.name == "checkEnable") {
                true
            } else {
                Mockito.RETURNS_DEFAULTS.answer(call)
            }
        })
        AttributeProvider.e(PluginReloadEvents.Post())
        AttributeDriver.register(driver)
        pending.clear()
    }

    /**
     * 清理注册的属性来源、档案和懒加载服务，隔离测试生命周期。
     */
    @AfterEach
    fun cleanup() {
        // 先取消续体和驱动，再恢复全局依赖。
        SkillCastingSessions.cancel(player)
        AttributeDriver.unregister(driver)
        AttributeProxy.unregister(source)
        AttributeProvider.e(PluginReloadEvents.Post())
        PlayerTemplateAPI.remove(player.uniqueId)
        pending.clear()
        bukkit.close()
        for ((delegate, fields) in lazyStates) {
            for ((field, value) in fields) {
                field.set(delegate, value)
            }
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
     * 入口返回前完成内存、属性和一次事件通知，数据库留在独立异步队列。
     */
    @Test
    fun commitsMemoryAndEventBeforePersistenceAndRepeatedClearIsNoOp() {
        // 调用真实 API，重复清空不产生第二份任务或事件。
        template.clearPlayerRouter()
        assertEquals(1, events.size)
        assertEquals(42L, events[0].previousBindingId)
        assertEquals("synthetic-job", events[0].previousRouterId)
        assertFalse(events[0].allowCancelled)
        assertSame(player, events[0].player)
        assertEquals(1, pending.size)
        assertTrue(pending[0].async)
        template.clearPlayerRouter()
        assertEquals(1, events.size)
        assertEquals(1, pending.size)
        runTask(pending.removeAt(0))
        Mockito.verify(database).deletePlayerRouter(router)
    }

    /**
     * 清空中断读条，调度器迟到执行被取消任务也不能恢复旧技能。
     */
    @Test
    fun cancelsCapturedSkillContinuationBeforeCompletionNotification() {
        var resumed = false
        SkillCastingSessions.begin(player, 10) {
            resumed = true
            ExecutableResult.cancelledWithEvent()
        }
        val oldTask = pending.removeAt(0)
        assertTrue(SkillCastingSessions.isCasting(player))
        template.clearPlayerRouter()
        assertFalse(SkillCastingSessions.isCasting(player))
        runTask(oldTask)
        assertFalse(resumed)
    }

    /**
     * 持久化失败单独记录，不回滚内存、不再次发事件、不覆盖重新选择的路由。
     */
    @Test
    fun persistenceFailureDoesNotChangeCommittedMemoryOrNewProfession() {
        val failure = IllegalStateException("synthetic persistence failure")
        Mockito.doThrow(failure).`when`(database).deletePlayerRouter(router)
        template.clearPlayerRouter()
        val replacement = Mockito.mock(PlayerRouter::class.java)
        template.playerRouter = replacement
        runTask(pending.removeAt(0))
        assertSame(replacement, template.playerRouter)
        assertEquals(1, events.size)
        Mockito.verify(logger).log(Mockito.eq(Level.SEVERE), Mockito.contains("router=42"), Mockito.same(failure))
    }

    /**
     * 异步队列拒绝提交也只报告持久化失败，调用者仍得到内存完成结果。
     */
    @Test
    fun schedulerFailureDoesNotUndoOrRejectMemoryCompletion() {
        // 只令持久化队列失败，主线程属性写入仍同步执行。
        rejectAsync = true
        template.clearPlayerRouter()
        assertNull(template.playerRouter)
        assertEquals(1, events.size)
        assertTrue(pending.isEmpty())
        Mockito.verifyNoInteractions(database)
        Mockito.verify(logger).log(Mockito.eq(Level.SEVERE), Mockito.contains("无法提交持久化删除任务"),
            Mockito.isA(IllegalStateException::class.java))
    }

    /**
     * 清空前排队的属性任务读取新状态，不能重新添加旧职业贡献。
     */
    @Test
    fun queuedAttributeRefreshCannotRestoreClearedContribution() {
        // 保留清空前提交的任务，在完成事件之后再执行。
        AttributeProvider.refresh(template)
        val oldRefresh = pending.removeAt(0)
        template.clearPlayerRouter()
        runTask(oldRefresh)
        assertEquals(0.0, chips["planners-proxy"])
        assertEquals(25.0, chips["equipment"])
    }

    /**
     * 在线档案身份、线程及重载检查必须发生在路由被修改之前。
     */
    @Test
    fun rejectsInvalidLifecycleBeforeChangingMemory() {
        bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(false)
        assertThrows(IllegalStateException::class.java) { template.clearPlayerRouter() }
        bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
        Mockito.`when`(player.isOnline).thenReturn(false)
        assertThrows(IllegalStateException::class.java) { template.clearPlayerRouter() }
        Mockito.`when`(player.isOnline).thenReturn(true)
        PlayerTemplateAPI[player.uniqueId] = PlayerTemplate(2L, player, null, emptyMap())
        assertThrows(IllegalStateException::class.java) { template.clearPlayerRouter() }
        PlayerTemplateAPI[player.uniqueId] = template
        AttributeProvider.e(PluginReloadEvents.Pre())
        assertThrows(IllegalStateException::class.java) { template.clearPlayerRouter() }
        assertSame(router, template.playerRouter)
        assertTrue(events.isEmpty())
        assertTrue(pending.isEmpty())
    }

    /**
     * 保存懒加载对象的全部实例字段，测试结束恢复初始状态。
     *
     * @param delegate 调度器或数据库的 Kotlin 懒加载容器。
     */
    private fun rememberLazy(delegate: Any) {
        // 不触发生产依赖初始化，仅记录容器原始字段。
        val fields = LinkedHashMap<Field, Any?>()
        for (field in delegate.javaClass.declaredFields) {
            if (!Modifier.isStatic(field.modifiers)) {
                field.isAccessible = true
                fields[field] = field.get(delegate)
            }
        }
        lazyStates[delegate] = fields
    }

    /**
     * 明确推进一个受控任务，保持生产续体本身不变。
     *
     * @param runnable 原样提交的调度任务。
     */
    private fun runTask(runnable: PlatformExecutor.PlatformRunnable) {
        // 测试可刻意执行已取消任务，验证身份门禁。
        runnable.executor.invoke(Mockito.mock(PlatformExecutor.PlatformTask::class.java))
    }
}
