import com.gitee.planners.core.player.PlayerRouter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.service.PlatformExecutor
import java.lang.reflect.Field

/**
 * 验证技能点初始化校正只补足累计差额，并且重复调用保持幂等。
 */
class SkillPointReconciliationTest {

    private val pending = ArrayList<PlatformExecutor.PlatformRunnable>()
    private val services = LinkedHashMap<String, Any?>()
    private val executorState = LinkedHashMap<Field, Any?>()
    private lateinit var executorDelegate: Any
    private lateinit var persistence: PersistenceTestScope

    /**
     * 安装只入队不执行的调度器，避免校正触发真实数据库保存。
     */
    @BeforeEach
    fun prepare() {
        persistence = PersistenceTestScope()
        val executorField = Class.forName("taboolib.common.platform.function.ExecutorKt").getDeclaredField("executorService\$delegate")
        executorField.isAccessible = true
        executorDelegate = executorField.get(null)
        for (field in executorDelegate.javaClass.declaredFields) {
            if (!java.lang.reflect.Modifier.isStatic(field.modifiers)) {
                field.isAccessible = true
                executorState[field] = field.get(executorDelegate)
            }
        }
        val task = Mockito.mock(PlatformExecutor.PlatformTask::class.java)
        val executor = Mockito.mock(PlatformExecutor::class.java, Answer { call ->
            pending.add(call.getArgument(0))
            task
        })
        services[PlatformExecutor::class.java.name] = PlatformFactory.serviceMap.put(PlatformExecutor::class.java.name, executor)
    }

    /**
     * 恢复调度器的懒加载状态，避免排队任务泄漏到其他用例。
     */
    @AfterEach
    fun cleanup() {
        persistence.close()
        pending.clear()
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
     * Lv1 首次校正发放 2 点，再次校正不得重复发放。
     */
    @Test
    fun shouldGrantLevelOnePointsOnlyOnce() {
        val router = router(level = 1, current = 0, used = 0)
        assertEquals(2, router.reconcileSkillPoints(2))
        assertEquals(2, router.skillPointsCurrent)
        assertEquals(0, router.reconcileSkillPoints(2))
        assertEquals(2, router.skillPointsCurrent)
    }

    /**
     * 按报告中的累计目标验证 Lv1、Lv4→Lv5、Lv9→Lv10 的差额。
     */
    @Test
    fun shouldGrantExpectedIncrementsAcrossLevels() {
        val router = router(level = 1, current = 0, used = 0)
        assertEquals(2, router.reconcileSkillPoints(2))
        assertEquals(2, router.reconcileSkillPoints(4))
        assertEquals(4, router.reconcileSkillPoints(8))
        assertEquals(6, router.reconcileSkillPoints(14))
        assertEquals(8, router.reconcileSkillPoints(22))
        assertEquals(10, router.reconcileSkillPoints(32))
        assertEquals(32, router.skillPointsCurrent)
    }

    /**
     * 已消费点数参与已发放总量计算，补发只增加可用余额。
     */
    @Test
    fun shouldPreserveConsumedPointsWhenBackfilling() {
        val router = router(level = 5, current = 4, used = 4)
        assertEquals(6, router.reconcileSkillPoints(14))
        assertEquals(10, router.skillPointsCurrent)
        assertEquals(4, router.skillPointsUsed)
    }

    /**
     * 已发放总量达到目标时不再增加可用余额。
     */
    @Test
    fun shouldNotGrantWhenLevelIsAlreadyCovered() {
        val router = router(level = 10, current = 12, used = 20)
        assertEquals(0, router.reconcileSkillPoints(32))
        assertEquals(12, router.skillPointsCurrent)
        assertEquals(20, router.skillPointsUsed)
    }

    /**
     * 已处于高等级但技能点为零的旧档案一次补足累计值。
     */
    @Test
    fun shouldGrantHistoricalBalanceForExistingLevel() {
        val router = router(level = 9, current = 0, used = 0)
        assertEquals(22, router.reconcileSkillPoints(22))
        assertEquals(22, router.skillPointsCurrent)
    }

    /**
     * 创建不访问注册表的合成职业线。
     */
    private fun router(level: Int, current: Int, used: Int): PlayerRouter {
        return PlayerRouter(1L, 1L, "synthetic", level, 0, -1L, current, used, emptyList())
    }
}
