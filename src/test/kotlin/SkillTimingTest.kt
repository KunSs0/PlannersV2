import com.gitee.planners.api.SkillInputExec
import com.gitee.planners.api.timing.SkillTimingModifiers
import com.gitee.planners.api.timing.SkillTimingProvider
import com.gitee.planners.api.timing.SkillTimingProviders
import com.gitee.planners.core.config.ImmutableSkill
import com.gitee.planners.core.player.PlayerSkill
import com.gitee.planners.core.skill.ExecutableResult
import com.gitee.planners.core.skill.context.SkillExecutionContext
import com.gitee.planners.core.skill.timing.SkillCastingQueue
import com.gitee.planners.core.skill.timing.SkillTimingPolicy
import com.gitee.planners.core.skill.timing.SkillTimingService
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.library.configuration.ConfigurationSection
import org.mockito.stubbing.Answer
import java.util.UUID

/**
 * 真实技能时间服务、严格配置、延迟队列及一次性Hook续体的回归测试。
 */
class SkillTimingTest {
    /**
     * 校验配置必须明确选择来源与取整，Provider缺失不能当零收益处理。
     */
    @Test
    fun missingConfigurationAndProviderFailExplicitly() {
        assertThrows(IllegalArgumentException::class.java) { SkillTimingPolicy.parse(null) }
        for (values in listOf(emptyMap(), mapOf("provider" to "fightcore"), mapOf("provider" to 1, "tick-rounding" to "ceil"), mapOf("provider" to "fightcore", "tick-rounding" to "unknown"))) {
            assertThrows(IllegalArgumentException::class.java) { SkillTimingPolicy.parse(section(values)) }
        }
        assertThrows(IllegalStateException::class.java) {
            SkillTimingService.prepare(player(), execution(200.0, 40.0), policy("missing", "ceil"))
        }
    }

    /**
     * 使用独立样本核对三种显式取整，避免少等一个tick或整数溢出。
     */
    @Test
    fun roundingIsSelectedByConfiguration() {
        assertEquals(196, policy("unmodified", "ceil").toTicks(195.1))
        assertEquals(195, policy("unmodified", "floor").toTicks(195.9))
        assertEquals(196, policy("unmodified", "nearest").toTicks(195.9))
        assertEquals(196, policy("unmodified", "nearest").toTicks(195.5))
        assertEquals(194, policy("unmodified", "nearest").toTicks(194.5))
        assertEquals(195, policy("unmodified", "ceil").toTicks(195.0))
        for (value in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Int.MAX_VALUE.toDouble() + 1.0)) {
            assertThrows(IllegalArgumentException::class.java) { policy("unmodified", "ceil").toTicks(value) }
        }
    }

    /**
     * 急速100点样本将200tick冷却与40tick施法变成195与38，重复准备只捕获一次。
     */
    @Test
    fun preparationCapturesOnceAndDoesNotApplyReductionTwice() {
        var captures = 0
        val owner = Any()
        val provider = SkillTimingProvider {
            captures += 1
            SkillTimingModifiers(0.025, 0.05)
        }
        SkillTimingProviders.register("test-timing", owner, provider)
        try {
            val execution = execution(200.0, 40.0)
            val first = SkillTimingService.prepare(player(), execution, policy("test-timing", "ceil"))
            val second = SkillTimingService.prepare(player(), execution, policy("test-timing", "ceil"))
            assertSame(first, second)
            assertEquals(1, captures)
            assertEquals(195, first.cooldownTicks)
            assertEquals(38, first.castTimeTicks)
            assertEquals(195, execution.getVariable("cooldown"))
            assertEquals(38, execution.getVariable("castTime"))
        } finally {
            SkillTimingProviders.unregister("test-timing", owner)
        }
    }

    /**
     * 验证显式原时间模式、零时间、未声明冷却，以及基础变量缺项和非法数值。
     */
    @Test
    fun baseTimesHaveNoImplicitDefaults() {
        val original = SkillTimingService.prepare(player(), execution(200.0, 40.0), policy("unmodified", "ceil"))
        assertEquals(200, original.cooldownTicks)
        assertEquals(40, original.castTimeTicks)
        val instant = SkillTimingService.prepare(player(), execution(null, 0.0), policy("unmodified", "ceil"))
        assertNull(instant.cooldownTicks)
        assertEquals(0, instant.castTimeTicks)
        val nullCooldown = execution(null, 0.0)
        nullCooldown.setVariable("cooldown", null)
        assertThrows(IllegalArgumentException::class.java) {
            SkillTimingService.prepare(player(), nullCooldown, policy("unmodified", "ceil"))
        }
        for (invalid in listOf(null, "40", true, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Int.MAX_VALUE.toDouble() + 1.0)) {
            assertThrows(IllegalArgumentException::class.java) {
                SkillTimingService.prepare(player(), execution(200.0, invalid), policy("unmodified", "ceil"))
            }
        }
        for (invalid in listOf("200", true, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                SkillTimingService.prepare(player(), execution(invalid, 40.0), policy("unmodified", "ceil"))
            }
        }
    }

    /**
     * 时间来源必须满足范围和所有权约定，卸载后不能继续查询旧Provider。
     */
    @Test
    fun providerOwnershipAndModifiersAreStrict() {
        val owner = Any()
        val provider = SkillTimingProvider { SkillTimingModifiers(0.0, 0.0) }
        SkillTimingProviders.register("owned-timing", owner, provider)
        try {
            SkillTimingProviders.register("owned-timing", owner, provider)
            assertThrows(IllegalStateException::class.java) { SkillTimingProviders.register("owned-timing", Any(), provider) }
            assertThrows(IllegalStateException::class.java) { SkillTimingProviders.unregister("owned-timing", Any()) }
        } finally {
            SkillTimingProviders.unregister("owned-timing", owner)
        }
        assertThrows(IllegalStateException::class.java) { SkillTimingProviders.requireProvider("owned-timing") }
        for (invalid in listOf(-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { SkillTimingModifiers(invalid, 0.0) }
            assertThrows(IllegalArgumentException::class.java) { SkillTimingModifiers(0.0, invalid) }
        }
    }

    /**
     * 验证实际延迟队列使用最终tick且只恢复一次，等待期间重复释放被拒绝。
     */
    @Test
    fun castingUsesFinalTicksAndCompletesOnlyOnce() {
        val tasks = ArrayList<Runnable>()
        val delays = ArrayList<Int>()
        val queue = SkillCastingQueue { ticks, action ->
            delays.add(ticks)
            tasks.add(action)
            Runnable { }
        }
        val id = UUID.randomUUID()
        var commits = 0
        assertTrue(queue.start(id, 38, Runnable { commits += 1 }))
        assertFalse(queue.start(id, 38, Runnable { commits += 100 }))
        assertEquals(listOf(38), delays)
        assertEquals(0, commits)
        tasks[0].run()
        tasks[0].run()
        assertEquals(1, commits)
        assertFalse(queue.contains(id))
    }

    /**
     * 取消、替换和宿主停止后，迟到回调不能扣资源，也不能完成新会话。
     */
    @Test
    fun cancellationAndShutdownDiscardOldContinuations() {
        val tasks = ArrayList<Runnable>()
        var cancellations = 0
        val queue = SkillCastingQueue { _, action ->
            tasks.add(action)
            Runnable { cancellations += 1 }
        }
        val id = UUID.randomUUID()
        var commits = 0
        queue.start(id, 38, Runnable { commits += 1 })
        queue.cancel(id)
        queue.start(id, 20, Runnable { commits += 10 })
        tasks[0].run()
        assertEquals(0, commits)
        assertTrue(queue.contains(id))
        queue.clear()
        tasks[1].run()
        assertEquals(0, commits)
        assertEquals(2, cancellations)
        assertFalse(queue.contains(id))
    }

    /**
     * 动画Hook多次恢复必须明确失败，不能重复提交同一份资源和冷却。
     */
    @Test
    fun inputHookContinuationIsSingleUse() {
        var commits = 0
        val context = SkillInputExec.Context(player(), Mockito.mock(PlayerSkill::class.java)) {
            commits += 1
            ExecutableResult.successful()
        }
        assertTrue(context.resume().success)
        assertThrows(IllegalStateException::class.java) { context.resume() }
        assertEquals(1, commits)
    }

    /**
     * 用合成基础变量建立真实执行上下文，不依赖服务端数据目录。
     *
     * @param cooldown 基础冷却；null表示未声明该变量。
     * @param casting 基础施法变量原值，用于校验非法输入。
     * @return 带合成变量的执行上下文。
     */
    private fun execution(cooldown: Any?, casting: Any?): SkillExecutionContext {
        val skill = Mockito.mock(ImmutableSkill::class.java)
        Mockito.`when`(skill.id).thenReturn("timing-fixture")
        val context = SkillExecutionContext.create(Any(), 1, skill)
        context.setVariable("castTime", casting)
        if (cooldown != null) {
            context.setVariable("cooldown", cooldown)
        }
        return context
    }

    /**
     * 从显式合成配置解析实际策略。
     *
     * @param provider 时间来源ID。
     * @param rounding tick取整模式。
     * @return 实际生产解析器产生的策略。
     */
    private fun policy(provider: String, rounding: String): SkillTimingPolicy {
        return SkillTimingPolicy.parse(section(mapOf("provider" to provider, "tick-rounding" to rounding)))
    }

    /**
     * 模拟宿主配置节的原始值读取，解析与类型校验仍使用真实生产代码。
     *
     * @param values 显式声明的合成配置字段。
     * @return 只支持本测试所需get操作的配置节。
     */
    private fun section(values: Map<String, Any>): ConfigurationSection {
        return Mockito.mock(ConfigurationSection::class.java, Answer { call ->
            check(call.method.name == "get")
            values[call.getArgument<String>(0)]
        })
    }

    /**
     * 创建仅用于传递给Provider的合成玩家。
     *
     * @return 不参与宿主生命周期的玩家mock。
     */
    private fun player(): Player {
        return Mockito.mock(Player::class.java)
    }
}
