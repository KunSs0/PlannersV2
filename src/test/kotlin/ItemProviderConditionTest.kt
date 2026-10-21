import com.gitee.planners.api.item.ItemProvider
import com.gitee.planners.api.item.ItemProviders
import com.gitee.planners.core.condition.ConditionFactory
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockito.Mockito
import taboolib.module.configuration.Configuration
import java.util.UUID

/** 验证物品条件只经 Planners 自有物品库注册表匹配，不依赖外部插件类。 */
class ItemProviderConditionTest {

    /** provider 段按注册表匹配，材质引用按原版物品比较。 */
    @Test
    fun matchesThroughRegisteredProviderAndVanillaMaterial() {
        val server = MockBukkit.mock()
        try {
            ItemProviders.register(diamondToken("test_item"))
            val player = server.addPlayer()
            player.inventory.addItem(ItemStack(Material.DIAMOND))
            val execution = itemCondition().prepare(UUID.randomUUID()).join()
            assertTrue(execution.evaluate(player, profile(), null, null, props("test_item:token")))
            assertFalse(execution.evaluate(player, profile(), null, null, props("test_item:other")))
            assertTrue(execution.evaluate(player, profile(), null, null, props("diamond")))
            assertTrue(execution.evaluate(player, profile(), null, null, props("minecraft:diamond")))
        } finally {
            MockBukkit.unmock()
        }
    }

    /** 物品库未注册时条件不满足，不得抛出链接错误或中断求值。 */
    @Test
    fun missesMissingProviderWithoutThrowing() {
        val server = MockBukkit.mock()
        try {
            val player = server.addPlayer()
            player.inventory.addItem(ItemStack(Material.DIAMOND))
            val execution = itemCondition().prepare(UUID.randomUUID()).join()
            assertFalse(execution.evaluate(player, profile(), null, null, props("test_missing:token")))
        } finally {
            MockBukkit.unmock()
        }
    }

    /** 物品引用只接受 provider:item-id 或合法材质 ID。 */
    @Test
    fun rejectsMalformedReferences() {
        val condition = itemCondition()
        for (invalid in listOf("", " ", "test:", ":token", "test:token:extra", "not_a_material")) {
            assertThrows(IllegalArgumentException::class.java) {
                condition.validateProperties(mapOf("matcher" to invalid, "amount" to 1))
            }
        }
        condition.validateProperties(mapOf("matcher" to "test:token", "amount" to 1))
        condition.validateProperties(mapOf("matcher" to "minecraft:diamond", "amount" to 1))
    }

    /** 同一 ID 不允许被两个实现占用，重复注册同一实现保持幂等。 */
    @Test
    fun rejectsDuplicateProviderRegistration() {
        val first = diamondToken("test_duplicate")
        ItemProviders.register(first)
        ItemProviders.register(first)
        assertThrows(IllegalArgumentException::class.java) {
            ItemProviders.register(diamondToken("test_duplicate"))
        }
        assertSame(first, ItemProviders.getOrNull("test_duplicate"))
        assertThrows(IllegalArgumentException::class.java) {
            ItemProviders.register(diamondToken(""))
        }
    }

    /** 物品条件模板；物品内容由 matcher 参数决定。 */
    private fun itemCondition() = ConditionFactory.create(
        "need_item",
        Configuration.loadFromString("type: item\nconsume: true\nhint: 物品不足\n")
    )

    /** 玩家上下文由 MockBukkit 提供，模板为空即可。 */
    private fun profile(): PlayerTemplate = Mockito.mock(PlayerTemplate::class.java)

    /** 节点级字面量参数。 */
    private fun props(matcher: String): Map<String, Any> = mapOf("matcher" to matcher, "amount" to 1)

    /** 仅识别钻石且 ID 匹配的测试用物品库。 */
    private fun diamondToken(id: String): ItemProvider = object : ItemProvider {
        override val providerId: String = id
        override fun matches(itemStack: ItemStack, itemId: String): Boolean {
            return itemStack.type == Material.DIAMOND && itemId == "token"
        }
    }
}
