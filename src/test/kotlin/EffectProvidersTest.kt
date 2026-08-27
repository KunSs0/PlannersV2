import com.gitee.planners.api.effect.EffectProvider
import com.gitee.planners.api.effect.EffectProviders
import org.bukkit.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Planners 类型化特效 Provider 的严格注册与调用测试。
 */
class EffectProvidersTest {

    /** 验证已注册 Provider 接收完整调用数据并返回实现结果。 */
    @Test
    fun shouldInvokeRegisteredProvider() {
        val owner = Any()
        var invoked = false
        val provider = EffectProvider { effectId, location, lifetimeTicks ->
            assertEquals("knight_skill_6", effectId)
            assertEquals(12.5, location.x)
            assertEquals(200, lifetimeTicks)
            invoked = true
            true
        }
        EffectProviders.register(owner, provider)
        try {
            val result = EffectProviders.spawnAtLocation(
                "knight_skill_6",
                Location(null, 12.5, 64.0, -8.0),
                200
            )
            assertTrue(result)
            assertTrue(invoked)
        } finally {
            EffectProviders.unregister(owner)
        }
    }

    /** 验证不同实例不能覆盖已有 Provider，且非所有者不能注销。 */
    @Test
    fun shouldRejectDifferentRegistrationAndForeignUnregister() {
        val owner = Any()
        val foreignOwner = Any()
        val provider = EffectProvider { _, _, _ -> true }
        EffectProviders.register(owner, provider)
        try {
            assertThrows(IllegalStateException::class.java) {
                EffectProviders.register(foreignOwner, provider)
            }
            assertThrows(IllegalStateException::class.java) {
                EffectProviders.unregister(foreignOwner)
            }
        } finally {
            EffectProviders.unregister(owner)
        }
    }

    /** 验证未注册时直接抛出英文异常，不执行回退。 */
    @Test
    fun shouldFailWhenProviderIsMissing() {
        val exception = assertThrows(IllegalStateException::class.java) {
            EffectProviders.spawnAtLocation("knight_skill_6", Location(null, 0.0, 0.0, 0.0), 200)
        }
        assertEquals("No Planners effect provider has been registered", exception.message)
    }
}
