import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.condition.ConditionFactory
import com.gitee.planners.core.config.SkillNode
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.compat.haidercore.HaiderCoreCompat
import com.gitee.planners.module.compat.haidercore.WeaponMasteryCondition
import com.gitee.planners.module.compat.haidercore.WeaponMasteryReader
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.module.configuration.Configuration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicInteger

/** 验证原生熟练度门槛、可选依赖失败语义和异步请求数据的复用。 */
class WeaponMasteryConditionTest {
    private val player = Mockito.mock(Player::class.java)
    private val profile = Mockito.mock(PlayerTemplate::class.java)
    private val playerId = UUID.randomUUID()

    /** 等于门槛可通过，不足时失败；不同武器各自判断且不产生 Nova 调用。 */
    @Test
    fun checksExactWeaponAndThresholdWithoutConsumption() {
        val levels = linkedMapOf("sword" to 3, "axe" to 0)
        val config = condition(WeaponMasteryReader { CompletableFuture.completedFuture(levels) })
        val execution = config.prepare(playerId).join()
        levels["sword"] = 99
        assertTrue(execution.evaluate(player, profile, null, null, props("sword", 3)))
        assertFalse(execution.evaluate(player, profile, null, null, props("sword", 4)))
        assertTrue(execution.evaluate(player, profile, null, null, props("axe", 0)))
        val result = execution.evaluateBatchPersistent(player, profile, null, null, listOf(
            mapOf("props" to props("sword", 3)), mapOf("props" to props("axe", 1))
        ))
        assertEquals("10", result.encoded)
        execution.prepareConsume(player, profile, null, null, props("sword", 3))
        assertTrue(execution.evaluate(player, profile, null, null, props("sword", 3)))
        assertFalse(config.consumable)
    }

    /** 未知武器不得作为零级参与门槛判断。 */
    @Test
    fun rejectsUnknownWeaponEvenAtZeroMinimum() {
        val config = condition(WeaponMasteryReader { CompletableFuture.completedFuture(mapOf("sword" to 0)) })
        val execution = config.prepare(playerId).join()
        assertThrows(IllegalArgumentException::class.java) {
            execution.evaluate(player, profile, null, null, props("unknown", 0))
        }
    }

    /** 异步准备不阻塞，多个模板共享一次读取，但后续请求必须重新读取。 */
    @Test
    fun sharesOneReadWithinSessionAndRefreshesNextRequest() {
        val calls = AtomicInteger()
        val pending = CompletableFuture<Map<String, Int>>()
        val reader = WeaponMasteryReader {
            calls.incrementAndGet()
            pending
        }
        val first = condition(reader)
        val second = WeaponMasteryCondition("secondary", emptyMap(), "条件未满足", reader)
        val evaluator = ConditionEvaluator { mapOf(first.key to first, second.key to second) }
        val sessionFuture = evaluator.prepare(setOf(first.key, second.key), playerId)
        assertFalse(sessionFuture.isDone)
        assertEquals(1, calls.get())
        pending.complete(mapOf("sword" to 3))
        val session = sessionFuture.join()
        assertSame(session.execution(first.key), session.execution(second.key))
        evaluator.prepare(setOf(first.key), playerId).join()
        assertEquals(2, calls.get())
    }

    /** 存储失败、模块关闭均通过异常传播，禁止默认通过或伪装零经验。 */
    @Test
    fun propagatesProviderFailuresAndReleasesRequest() {
        for (message in listOf("熟练度模块未启用", "存储查询失败")) {
            val config = condition(WeaponMasteryReader { CompletableFuture.failedFuture(IllegalStateException(message)) })
            val evaluator = ConditionEvaluator { mapOf(config.key to config) }
            val failure = assertThrows(CompletionException::class.java) {
                evaluator.prepare(setOf(config.key), playerId).join()
            }
            assertEquals(message, failure.cause?.message)
        }
    }

    /** 缺少 HaiderCore 时返回明确错误，测试环境无需加载 HaiderCore 模块。 */
    @Test
    fun missingPluginFailsExplicitly() {
        val plugins = Mockito.mock(PluginManager::class.java)
        val bukkit = Mockito.mockStatic(Bukkit::class.java)
        try {
            bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(plugins)
            val failure = assertThrows(CompletionException::class.java) {
                HaiderCoreCompat.read(playerId).join()
            }
            assertTrue(failure.cause!!.message!!.contains("HaiderCore"))
        } finally {
            bukkit.close()
        }
    }

    /** 主线程不得等待尚未完成的外部查询，空请求也不得读取可选插件。 */
    @Test
    fun refusesBlockingMainThreadAndSkipsUnusedTemplates() {
        val calls = AtomicInteger()
        val config = condition(WeaponMasteryReader {
            calls.incrementAndGet()
            CompletableFuture<Map<String, Int>>()
        })
        val evaluator = ConditionEvaluator { mapOf(config.key to config) }
        Mockito.`when`(player.uniqueId).thenReturn(playerId)
        val bukkit = Mockito.mockStatic(Bukkit::class.java)
        try {
            bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
            evaluator.prepareBlocking(emptySet(), player)
            assertEquals(0, calls.get())
            assertThrows(IllegalStateException::class.java) {
                evaluator.prepareBlocking(setOf(config.key), player)
            }
            assertEquals(1, calls.get())
        } finally {
            bukkit.close()
        }
    }

    /** 原生 weapon 字符串必须保留字面量，节点加载不得将其编译成 Nova 标识符。 */
    @Test
    fun factoryAndNodeParserKeepLiteralProperties() {
        val section = Configuration.loadFromString("type: weapon_mastery\n")
        val config = ConditionFactory.create("weapon_mastery", section)
        assertInstanceOf(WeaponMasteryCondition::class.java, config)
        val node = Configuration.loadFromString("levels:\n  1:\n    weapon_mastery:\n      weapon: long-sword\n      min: 3\n")
        val levels = SkillNode.parseLevels(node, mapOf(config.key to config))
        assertEquals("long-sword", levels.getValue(1).getValue(config.key)["weapon"])
        assertEquals(3, levels.getValue(1).getValue(config.key)["min"])
        ConditionEvaluator.prepareConfiguredProperties(listOf(config))
    }

    /** 参数只接受合法 ID 和非负 Int 整数，避免隐式截断或错误字段被忽略。 */
    @Test
    fun rejectsMalformedLiteralParameters() {
        val config = condition(WeaponMasteryReader { CompletableFuture.completedFuture(emptyMap()) })
        val invalid = listOf(
            emptyMap(), props("", 1), props("sword", -1),
            mapOf("weapon" to "sword", "min" to 1.5),
            mapOf("weapon" to "sword", "min" to "3"),
            mapOf("weapon" to "sword", "min" to Long.MAX_VALUE),
            mapOf("weapon" to "sword", "min" to 1, "extra" to true)
        )
        for (properties in invalid) {
            assertThrows(IllegalArgumentException::class.java) {
                config.validateProperties(properties)
            }
        }
    }

    /** 构造不依赖 Bukkit 插件生命周期的条件。 */
    private fun condition(reader: WeaponMasteryReader): WeaponMasteryCondition {
        return WeaponMasteryCondition("weapon_mastery", emptyMap(), "需要 {props.weapon} Lv{props.min}", reader)
    }

    /** 生成配置边界上的具体武器门槛参数。 */
    private fun props(weapon: String, min: Int): Map<String, Any> {
        return mapOf("weapon" to weapon, "min" to min)
    }
}
