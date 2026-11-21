import com.gitee.planners.api.item.ItemProvider
import com.gitee.planners.api.item.ItemProviders
import com.gitee.planners.core.skill.condition.CastCostReservation
import com.gitee.planners.core.skill.condition.ItemCostCastCondition
import com.gitee.planners.core.skill.condition.ItemReference
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * `item_cost` 条件的加载期校验与物品引用解析。
 *
 * 物品体系由 `ItemProvider` 注册表提供，条件本身只认 `provider:itemId`。
 */
class ItemCostConditionTest {

    private val condition = ItemCostCastCondition()

    /** 注册测试物品库。 */
    @BeforeEach
    fun setup() {
        ItemProviders.register(FakeProvider)
    }

    /** 合法配置通过校验。 */
    @Test
    fun acceptsValidProps() {
        condition.validate(mapOf("item" to "fake:token", "amount" to 1), "test")
        condition.validate(mapOf("item" to "fake:token", "amount" to "<skill.level>"), "test")
    }

    /** 缺前缀或前缀为空都拒绝。 */
    @Test
    fun rejectsMissingProviderPrefix() {
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to "token", "amount" to 1), "test")
        }
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to ":token", "amount" to 1), "test")
        }
    }

    /** 未注册的物品库拒绝。 */
    @Test
    fun rejectsUnknownProvider() {
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to "nope:token", "amount" to 1), "test")
        }
    }

    /** 物品库不支持的物品拒绝。 */
    @Test
    fun rejectsUnsupportedItem() {
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to "fake:unknown", "amount" to 1), "test")
        }
    }

    /** 数量必须为正整数，0 与负数都拒绝。 */
    @Test
    fun rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to "fake:token", "amount" to 0), "test")
        }
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to "fake:token", "amount" to -1), "test")
        }
        assertThrows(IllegalArgumentException::class.java) {
            condition.validate(mapOf("item" to "fake:token"), "test")
        }
    }

    /** 引用解析保留原始文本，供失败文案回退。 */
    @Test
    fun parsesReference() {
        val reference = ItemReference.parse("fake:token", "test")
        assertEquals("fake", reference.provider)
        assertEquals("token", reference.itemId)
        assertEquals("fake:token", reference.raw)
    }

    /** 测试物品库：只支持 `token`。 */
    private object FakeProvider : ItemProvider {

        override val providerId: String = "fake"

        override fun matches(itemStack: ItemStack, itemId: String): Boolean {
            return false
        }

        override fun supportsConsume(itemId: String): Boolean {
            return itemId == "token"
        }

        override fun count(player: Player, itemId: String): Int {
            return 0
        }

        override fun prepareConsume(player: Player, itemId: String, amount: Int): CastCostReservation {
            return object : CastCostReservation {
                override fun commit() {
                }

                override fun rollback() {
                }
            }
        }

        override fun displayName(itemId: String): String? {
            return "代币"
        }
    }
}
