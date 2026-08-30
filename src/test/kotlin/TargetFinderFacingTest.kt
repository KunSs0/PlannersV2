import com.gitee.planners.api.common.facing.EntityFacingProvider
import com.gitee.planners.api.common.facing.EntityFacingProviders
import com.gitee.planners.module.script.finder.TargetFinder
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

/**
 * TargetFinder 朝向 Provider 的构造与链式调用测试。
 */
class TargetFinderFacingTest {

    /**
     * 验证两参构造和无参 setFacing 均通过 Provider 获取朝向，显式 yaw 则不查询 Provider。
     */
    @Test
    fun shouldResolveDefaultAndChainedFacingThroughProvider() {
        val originalProvider = getCurrentProvider()
        val entity = createLivingEntity()
        var providerCalls = 0
        val provider = object : EntityFacingProvider {

            override fun getFacingYaw(entity: LivingEntity): Float {
                providerCalls += 1
                return 135.0f
            }
        }
        EntityFacingProviders.register(provider)
        try {
            val finder = TargetFinder(Location(null, 0.0, 64.0, 0.0), entity)
            assertEquals(1, providerCalls)

            finder.setFacing()
            assertEquals(2, providerCalls)

            finder.setFacing(90.0f)
            assertEquals(2, providerCalls)
        } finally {
            EntityFacingProviders.register(originalProvider)
        }
    }

    /**
     * 验证未绑定施法者时不能调用无参 setFacing。
     */
    @Test
    fun shouldRejectNoArgumentFacingWithoutSender() {
        val finder = TargetFinder(Location(null, 0.0, 64.0, 0.0))

        assertThrows(IllegalStateException::class.java) {
            finder.setFacing()
        }
    }

    /**
     * 读取测试前的全局朝向 Provider，以便测试结束后还原。
     *
     * @return 当前注册的朝向 Provider。
     */
    private fun getCurrentProvider(): EntityFacingProvider {
        val field = EntityFacingProviders::class.java.getDeclaredField("provider")
        field.isAccessible = true
        val value = field.get(null)
        return value as EntityFacingProvider
    }

    /**
     * 创建仅用于 Provider 入参校验的实体代理。
     *
     * @return LivingEntity 接口代理。
     */
    private fun createLivingEntity(): LivingEntity {
        val classLoader = LivingEntity::class.java.classLoader
        val interfaces = arrayOf(LivingEntity::class.java)
        val proxy = Proxy.newProxyInstance(classLoader, interfaces) { _, _, _ ->
            null
        }
        return proxy as LivingEntity
    }
}
