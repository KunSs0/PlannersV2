import com.gitee.haidercore.module.mastery.model.EquipmentTypeSpec
import com.gitee.haidercore.module.mastery.model.ExperienceSpec
import com.gitee.haidercore.module.mastery.model.MasterySettings
import com.gitee.haidercore.module.mastery.model.PlayerState
import com.gitee.planners.module.compat.haidercore.HaiderMasteryReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

/** 使用真实 HaiderCore 模型验证累计进度到等级的 API 适配。 */
class HaiderMasteryReaderTest {
    /** 精确门槛、满级、未记录进度均遵守当前曲线的起始等级。 */
    @Test
    fun convertsCumulativeExperienceWithConfiguredStartLevel() {
        val sword = EquipmentTypeSpec("sword", "测试剑", mapOf("test.type" to setOf("sword")))
        val axe = EquipmentTypeSpec("axe", "测试斧", mapOf("test.type" to setOf("axe")))
        val group = MasterySettings.Group(BigDecimal.ONE, mapOf("sword" to sword, "axe" to axe))
        val settings = MasterySettings(ExperienceSpec(mapOf(1 to 0L, 2 to 17L, 3 to 41L)), emptyMap(), group, group, listOf("HEAD"))
        val playerId = UUID.randomUUID()
        assertEquals(mapOf("sword" to 2, "axe" to 1), HaiderMasteryReader.levels(settings, PlayerState(playerId, mapOf("sword" to 17L))))
        assertEquals(mapOf("sword" to 3, "axe" to 1), HaiderMasteryReader.levels(settings, PlayerState(playerId, mapOf("sword" to Long.MAX_VALUE))))
    }
}
