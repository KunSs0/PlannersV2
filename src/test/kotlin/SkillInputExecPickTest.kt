import com.gitee.planners.api.SkillInputExec
import com.gitee.planners.api.pick.PickEntityResult
import com.gitee.planners.api.pick.PickResult
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito

/**
 * 技能输入Hook续体的指向结果传递回归测试。
 *
 * 校验施放确认的目标在恢复后仍能绑定到 `@target`：Hook 直接恢复时用施放快照，
 * 显式传入结果时用调用方结果，非指向技能保持 null。
 */
class SkillInputExecPickTest {

    /**
     * Hook 不参与指向时必须沿用施放确认时的结果，否则 `@target` 解析不到目标。
     */
    @Test
    fun resumeWithoutArgumentKeepsCastedPick() {
        val casted = PickEntityResult(Mockito.mock(Entity::class.java))
        var received: PickResult? = null
        val context = SkillInputExec.Context(player(), skill(), casted) { resumedPick ->
            received = resumedPick
            ExecutableResult.successful()
        }

        assertTrue(context.resume().success)
        assertSame(casted, received)
        assertSame(casted, context.pick)
    }

    /**
     * Hook 显式传入结果时必须替换快照，且不改写构造时保存的施放快照。
     */
    @Test
    fun resumeWithArgumentReplacesCastedPick() {
        val casted = PickEntityResult(Mockito.mock(Entity::class.java))
        val replaced = PickEntityResult(Mockito.mock(Entity::class.java))
        var received: PickResult? = null
        val context = SkillInputExec.Context(player(), skill(), casted) { resumedPick ->
            received = resumedPick
            ExecutableResult.successful()
        }

        assertTrue(context.resume(replaced).success)
        assertSame(replaced, received)
        assertSame(casted, context.pick)
    }

    /**
     * 非指向技能的施放快照为空，恢复后不能凭空产生目标。
     */
    @Test
    fun nonPickSkillResumesWithoutPick() {
        var resumed = false
        var received: PickResult? = PickEntityResult(Mockito.mock(Entity::class.java))
        val context = SkillInputExec.Context(player(), skill(), null) { resumedPick ->
            resumed = true
            received = resumedPick
            ExecutableResult.successful()
        }

        assertTrue(context.resume().success)
        assertTrue(resumed)
        assertNull(context.pick)
        assertNull(received)
    }

    /**
     * 合成施放者，避免测试依赖真实玩家数据。
     */
    private fun player(): Player {
        return Mockito.mock(Player::class.java)
    }

    /**
     * 合成玩家技能，指向结果传递与该对象内容无关。
     */
    private fun skill(): PlayerSkill {
        return Mockito.mock(PlayerSkill::class.java)
    }
}
