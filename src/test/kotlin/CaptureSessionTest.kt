import com.gitee.planners.module.action.ActionTarget
import com.gitee.planners.module.action.CaptureRegistry
import com.gitee.planners.module.action.CaptureSession
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

/**
 * 会话捕捉：累积去重、上限、过期与注册表生命周期。
 */
class CaptureSessionTest {

    /** 每个用例前放开捕捉者校验。 */
    @BeforeEach
    fun setUp() {
        CaptureRegistry.ownerValid = { true }
        CaptureRegistry.clear()
    }

    /** 同一目标重复出现只保留一次。 */
    @Test
    fun shouldDeduplicateTargets() {
        val entity = entity(UUID.randomUUID(), true)
        val session = session(limit = 0)

        assertTrue(session.add(ActionTarget.EntityTarget(entity)))
        assertFalse(session.add(ActionTarget.EntityTarget(entity)))
        assertEquals(1, session.count())
    }

    /** 达到上限后标记结束。 */
    @Test
    fun shouldReachLimit() {
        val session = session(limit = 1)

        session.add(ActionTarget.EntityTarget(entity(UUID.randomUUID(), true)))

        assertTrue(session.reachedLimit())
    }

    /** 过期按时间判定。 */
    @Test
    fun shouldExpireByTime() {
        val session = CaptureSession(
            instance = UUID.randomUUID(),
            id = "dash",
            ownerId = UUID.randomUUID(),
            dimensions = doubleArrayOf(1.6, 2.0, 1.4),
            limit = 0,
            ticks = 8,
            period = 1,
            expiresAt = System.currentTimeMillis() - 1L,
        )

        assertTrue(session.isExpired(System.currentTimeMillis()))
    }

    /** 读取时过滤已失效目标。 */
    @Test
    fun shouldSkipInvalidTargetsOnRead() {
        val alive = entity(UUID.randomUUID(), true)
        val dead = entity(UUID.randomUUID(), false)
        val session = session(limit = 0)
        session.add(ActionTarget.EntityTarget(alive))
        session.add(ActionTarget.EntityTarget(dead))

        val targets = session.targets()

        assertEquals(1, targets.size)
        assertEquals(alive, targets[0].bukkitEntity())
    }

    /** 同名捕捉被新一次覆盖，按名可查到最新实例。 */
    @Test
    fun shouldReplaceSameIdOnRegister() {
        val ownerId = UUID.randomUUID()
        val first = session(limit = 0, ownerId = ownerId)
        val second = session(limit = 0, ownerId = ownerId)

        CaptureRegistry.register(first)
        CaptureRegistry.register(second)

        val found = CaptureRegistry.find(ownerId, "dash")
        assertNotNull(found)
        assertEquals(second.instance, found?.instance)
    }

    /** 结束指定分类后查询不到实例。 */
    @Test
    fun shouldStopOwnedCapture() {
        val ownerId = UUID.randomUUID()
        CaptureRegistry.register(session(limit = 0, ownerId = ownerId))

        assertEquals(1, CaptureRegistry.stopOwned(ownerId, "dash"))
        assertNull(CaptureRegistry.find(ownerId, "dash"))
    }

    /** 捕捉者失效时清理实例。 */
    @Test
    fun shouldPurgeWhenOwnerInvalid() {
        val ownerId = UUID.randomUUID()
        CaptureRegistry.register(session(limit = 0, ownerId = ownerId))

        CaptureRegistry.ownerValid = { false }
        CaptureRegistry.purge()

        assertNull(CaptureRegistry.find(ownerId, "dash"))
    }

    /**
     * 构造捕捉实例。
     *
     * @param limit 累积上限。
     * @param ownerId 捕捉者；缺省随机。
     * @return 实例。
     */
    private fun session(limit: Int, ownerId: UUID = UUID.randomUUID()): CaptureSession {
        return CaptureSession(
            instance = UUID.randomUUID(),
            id = "dash",
            ownerId = ownerId,
            dimensions = doubleArrayOf(1.6, 2.0, 1.4),
            limit = limit,
            ticks = 8,
            period = 1,
            expiresAt = System.currentTimeMillis() + 8000L,
        )
    }

    /**
     * 构造生物替身。
     *
     * @param id 唯一 ID。
     * @param valid 是否有效。
     * @return 生物替身。
     */
    private fun entity(id: UUID, valid: Boolean): LivingEntity {
        val entity = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(entity.uniqueId).thenReturn(id)
        Mockito.`when`(entity.isValid).thenReturn(valid)
        Mockito.`when`(entity.isDead).thenReturn(!valid)
        return entity
    }
}
