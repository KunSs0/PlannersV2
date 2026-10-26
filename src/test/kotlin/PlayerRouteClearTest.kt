import com.gitee.planners.Planners
import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.api.attribute.AttributeSource
import com.gitee.planners.api.event.PluginReloadEvents
import com.gitee.planners.api.event.player.PlayerRouteClearedEvent
import com.gitee.planners.core.attribute.AttributeProxy
import com.gitee.planners.core.database.Database
import com.gitee.planners.core.database.ProfileStore
import com.gitee.planners.core.database.ProfileSnapshot
import com.gitee.planners.core.database.RecordIdentity
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
    private lateinit var store: ProfileStore
    private lateinit var persistence: PersistenceTestScope
    private lateinit var source: AttributeSource
    private lateinit var logger: Logger
    private var rejectAsync = false
    private val chips = linkedMapOf("equipment" to 25.0, "planners-proxy" to 100.0)

    /**
     * 安装保留 now 语义的调度器，以及能够观察属性提交时序的外部驱动。
     */
    @BeforeEach
    fun prepare() {
        persistence = PersistenceTestScope { task ->
            taboolib.common.platform.function.submitAsync { task.run() }
        }
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
        Mockito.`when`(router.persistenceId).thenReturn(RecordIdentity(42L))
        Mockito.`when`(router.getRoutes()).thenReturn(emptyList())
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
        store = Mockito.mock(ProfileStore::class.java)
        Mockito.`when`(database.store).thenReturn(store)
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
        persistence.close()
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
        val deletion = ProfileSnapshot.Deletion(router.persistenceId, emptyList())
        Mockito.verify(store).deleteRouter(deletion)
    }

    /** 新选职在数据库未执行时已经完成；监听器追加保存不能抢在创建之前。 */
    @Test
    fun routeCreationPublishesMemoryBeforeDatabaseAndOrdersReentrantWrites() {
        template.playerRouter = null
        val definition = Mockito.mock(com.gitee.planners.core.config.ImmutableRoute::class.java)
        Mockito.`when`(definition.routerId).thenReturn("synthetic-job")
        Mockito.`when`(definition.id).thenReturn("stage")
        val request = PlayerTemplateAPI.setPlayerRoute(player, definition)
        assertTrue(request.isDone)
        val stage = request.join()
        val selected = checkNotNull(template.playerRouter)
        assertSame(stage, selected.currentRoute)
        assertTrue(stage.bindingId < -1)
        assertEquals(listOf(stage), selected.routeLine)
        Mockito.verifyNoInteractions(database)
        assertEquals(1, pending.size)
        // 数据库尚未启动时仍允许改变内存点数，固定快照保留各自值。
        selected.addSkillPoints(3)
        assertEquals(3, selected.skillPointsCurrent)
        runTask(pending.removeAt(0))
        val calls = Mockito.mockingDetails(store).invocations.toList()
        assertEquals(listOf("createRoute", "saveRouter"), calls.map { it.method.name })
        val initial = calls[0].arguments[0] as ProfileSnapshot.Router
        val saved = calls[1].arguments[0] as ProfileSnapshot.Router
        assertEquals(0, initial.points)
        assertEquals(3, saved.points)
    }

    /** 成功学习立即更新节点和技能点，SQL 异常不撤销，也不遗留学习所有权。 */
    @Test
    fun learningPublishesImmediatelyAndDatabaseFailureDoesNotHoldLearningOwnership() {
        val state = com.gitee.planners.core.player.PlayerSkillTreeNodeState(21L, "tree", "node", 0)
        val route = Mockito.spy(com.gitee.planners.core.player.PlayerRoute(11L, "synthetic-job", -1L, "stage", emptyList(), listOf(state)))
        val conditions = mapOf("points" to mapOf<String, Any>("amount" to 1))
        val node = com.gitee.planners.core.config.SkillTreeUnlockNode("node", "node", 2,
            mapOf(1 to conditions, 2 to conditions), com.gitee.planners.core.config.SkillTreeNodePosition(0, 0))
        val tree = com.gitee.planners.core.config.ImmutableSkillTree("tree", "tree", "none",
            com.gitee.planners.core.config.TreeType.BASE, mapOf("node" to node), emptyMap())
        Mockito.doReturn(tree).`when`(route).getSkillTreeOrNull("tree")
        val evaluator = com.gitee.planners.core.player.PlayerRoute::class.java.getDeclaredField("evaluator")
        evaluator.isAccessible = true
        val cost = com.gitee.planners.core.condition.NativeConditionConfig("points", "skill_points", emptyMap(), "点数不足", true)
        evaluator.set(route, com.gitee.planners.core.condition.ConditionEvaluator { mapOf("points" to cost) })
        val selected = PlayerRouter(42L, 1L, "synthetic-job", 1, 0, 11L, 5, 0, listOf(route))
        template.playerRouter = selected
        val failure = IllegalStateException("SQL stopped")
        val snapshot = com.gitee.planners.core.database.ProfilePersistence.routerSnapshot(selected).copy(points = 4, used = 1)
        val routeId = route.persistenceId
        val nodeData = ProfileSnapshot.Node(state.persistenceId, "tree", "node", 1)
        Mockito.doThrow(failure).`when`(store).saveLearning(snapshot, routeId, nodeData, null)
        val first = route.advanceNode(player, "tree", "node")
        assertTrue(first.isDone)
        assertFalse(first.isCompletedExceptionally)
        assertEquals(1, state.level)
        assertEquals(4, selected.skillPointsCurrent)
        assertEquals(1, selected.skillPointsUsed)
        Mockito.verifyNoInteractions(database)
        // 第一个 SQL 尚未执行，下一次合法学习已经可以继续提交。
        val second = route.advanceNode(player, "tree", "node")
        assertTrue(second.isDone)
        assertFalse(second.isCompletedExceptionally)
        assertEquals(2, state.level)
        runTask(pending.removeAt(0))
        assertSame(failure, persistence.failures.single())
        assertEquals(2, state.level)
        assertEquals(3, selected.skillPointsCurrent)
        assertEquals(2, selected.skillPointsUsed)
    }

    /** 主线程清空事件重入选择新职业时，旧删除仍排在新创建之前。 */
    @Test
    fun notificationReentryKeepsDeletionAheadOfNewCreation() {
        val replacement = PlayerRouter(RecordIdentity.next(), 1, "synthetic-job", 1, 0, -1, 0, 0, emptyList())
        val stage = com.gitee.planners.core.player.PlayerRoute(RecordIdentity.next(), "synthetic-job", -1, "stage", emptyList(), emptyList())
        replacement.registerRoute(stage)
        replacement.setCurrentRoute(stage)
        com.gitee.planners.core.database.ProfilePersistence.deleteRouter(router) {
            // 真实事件边界中再次选职，嵌套完成事件也先发布再调度 SQL。
            com.gitee.planners.core.database.ProfilePersistence.createRoute(replacement, stage, true) {
                assertTrue(pending.isEmpty())
                replacement.addSkillPoints(2)
            }
            Mockito.verifyNoInteractions(database)
        }
        runTask(pending.removeAt(0))
        val methods = Mockito.mockingDetails(store).invocations.map { it.method.name }
        assertEquals(listOf("deleteRouter", "createRoute", "saveRouter"), methods)
    }

    /** 退出后再次登录，前一次后台加载结果不得覆盖新会话。 */
    @Test
    fun lateProfileReadCannotPublishIntoNewLoginSession() {
        PlayerTemplateAPI.remove(player.uniqueId)
        val stale = PlayerTemplate(10, player, null, emptyMap())
        val current = PlayerTemplate(20, player, null, emptyMap())
        Mockito.`when`(database.getPlayerProfile(player)).thenReturn(stale, current)
        val join = PlayerTemplateAPI::class.java.getDeclaredMethod("handleProfileLinked", org.bukkit.event.player.PlayerJoinEvent::class.java)
        val quit = PlayerTemplateAPI::class.java.getDeclaredMethod("handleProfileUnlinked", org.bukkit.event.player.PlayerQuitEvent::class.java)
        join.isAccessible = true
        quit.isAccessible = true
        join.invoke(PlayerTemplateAPI, org.bukkit.event.player.PlayerJoinEvent(player, null as String?))
        quit.invoke(PlayerTemplateAPI, org.bukkit.event.player.PlayerQuitEvent(player, null as String?))
        join.invoke(PlayerTemplateAPI, org.bukkit.event.player.PlayerJoinEvent(player, null as String?))
        assertNull(PlayerTemplateAPI.getOrNull(player.uniqueId))
        assertEquals(1, pending.size)
        runTask(pending.removeAt(0))
        assertSame(current, PlayerTemplateAPI.getOrNull(player.uniqueId))
        assertTrue(persistence.failures.isEmpty())
    }

    /** 后端读取接口显式拒绝主线程，不能因外部误用绕开异步提交边界。 */
    @Test
    fun backendProfileReadRejectsPrimaryThreadBeforeOpeningConnection() {
        val local = Mockito.mock(com.gitee.planners.core.database.DatabaseLocal::class.java, Mockito.CALLS_REAL_METHODS)
        val sql = Mockito.mock(com.gitee.planners.core.database.DatabaseSQL::class.java, Mockito.CALLS_REAL_METHODS)
        assertThrows(IllegalStateException::class.java) { local.getPlayerProfile(player) }
        assertThrows(IllegalStateException::class.java) { sql.getPlayerProfile(player) }
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
        val deletion = ProfileSnapshot.Deletion(router.persistenceId, emptyList())
        Mockito.doThrow(failure).`when`(store).deleteRouter(deletion)
        template.clearPlayerRouter()
        val replacement = Mockito.mock(PlayerRouter::class.java)
        template.playerRouter = replacement
        runTask(pending.removeAt(0))
        assertSame(replacement, template.playerRouter)
        assertEquals(1, events.size)
        assertSame(failure, persistence.failures.single())
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
        assertTrue(persistence.failures.single() is IllegalStateException)
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
