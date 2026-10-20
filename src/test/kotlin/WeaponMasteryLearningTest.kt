import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.core.condition.ConditionConfig
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.condition.ConditionExecution
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeAttributeNode
import com.gitee.planners.core.config.SkillTreeNodePosition
import com.gitee.planners.core.config.TreeType
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import com.gitee.planners.core.player.PlayerTemplate
import com.gitee.planners.module.compat.haidercore.WeaponMasteryCondition
import com.gitee.planners.module.compat.haidercore.WeaponMasteryReader
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/** 直接验证真实学习入口在外部查询等待期间的重入锁、状态重检与消耗顺序。 */
class WeaponMasteryLearningTest {
    /** 武器树挂在祖先阶段时，转职后仍进入真实学习条件校验。 */
    @Test
    fun allowsAncestorRouteAfterTransferWithoutBypassingConditions() {
        val fixture = Fixture()
        val branch = Mockito.mock(PlayerRoute::class.java)
        Mockito.`when`(fixture.router.currentRoute).thenReturn(branch)
        Mockito.`when`(fixture.router.routeLine).thenReturn(listOf(fixture.route, branch))
        fixture.gate.passed = false
        fixture.execute {
            val learning = fixture.route.advanceNode(fixture.player, "combat", "root")
            assertFalse(learning.isDone)
            fixture.pending.complete(mapOf("sword" to 3))
            val failure = assertThrows(CompletionException::class.java) { learning.join() }
            val cause = failure.cause
            assertNotNull(cause)
            assertEquals("技能点不足", cause!!.message)
            assertEquals(0, fixture.gate.consumptions)
        }
    }

    /** 查询期间转职，即使原阶段仍在祖先链中，也拒绝旧请求。 */
    @Test
    fun rejectsTransferWhileAncestorLearningIsPending() {
        val fixture = Fixture()
        fixture.execute {
            val learning = fixture.route.advanceNode(fixture.player, "combat", "root")
            val branch = Mockito.mock(PlayerRoute::class.java)
            Mockito.`when`(fixture.router.currentRoute).thenReturn(branch)
            Mockito.`when`(fixture.router.routeLine).thenReturn(listOf(fixture.route, branch))
            fixture.pending.complete(mapOf("sword" to 3))
            val failure = assertThrows(CompletionException::class.java) { learning.join() }
            assertEquals("当前职业已经改变", failure.cause!!.message)
            assertEquals(0, fixture.gate.consumptions)
        }
    }

    /** 异步等待不阻塞调用方，第二次点击被拒绝；失败后可发起新查询。 */
    @Test
    fun preventsRepeatedClicksAndUnlocksAfterFailedQuery() {
        val fixture = Fixture()
        fixture.execute {
            val first = fixture.route.advanceNode(fixture.player, "combat", "root")
            assertFalse(first.isDone)
            val duplicate = fixture.route.advanceNode(fixture.player, "combat", "root")
            assertTrue(duplicate.isCompletedExceptionally)
            assertEquals(1, fixture.reads)
            fixture.pending.completeExceptionally(IllegalStateException("数据库不可用"))
            assertThrows(CompletionException::class.java) { first.join() }
            val retry = fixture.route.advanceNode(fixture.player, "combat", "root")
            assertTrue(retry.isCompletedExceptionally)
            assertEquals(2, fixture.reads)
            assertEquals(0, fixture.gate.consumptions)
            assertEquals(0, fixture.state.level)
        }
    }

    /** 查询期间职业、在线状态、节点或技能点改变，均须在任何消耗前拒绝。 */
    @Test
    fun rechecksMutableStateAfterMasteryRead() {
        for (change in listOf("route", "offline", "level", "points", "mastery")) {
            val fixture = Fixture()
            fixture.execute {
                val learning = fixture.route.advanceNode(fixture.player, "combat", "root")
                assertFalse(learning.isDone)
                when (change) {
                    "route" -> {
                        Mockito.`when`(fixture.profile.playerRouter).thenReturn(null)
                    }
                    "offline" -> {
                        Mockito.`when`(fixture.player.isOnline).thenReturn(false)
                    }
                    "level" -> {
                        fixture.state.level = 1
                    }
                    "points" -> {
                        fixture.gate.passed = false
                    }
                }
                val actual = if (change == "mastery") {
                    1
                } else {
                    3
                }
                fixture.pending.complete(mapOf("sword" to actual))
                assertThrows(CompletionException::class.java) { learning.join() }
                assertEquals(0, fixture.gate.consumptions, change)
            }
        }
    }

    /** 最小学习环境，使用真实 PlayerRoute 和真实熟练度条件，不启动插件或数据库。 */
    private class Fixture {
        val playerId: UUID = UUID.randomUUID()
        val player: Player = Mockito.mock(Player::class.java)
        val profile: PlayerTemplate = Mockito.mock(PlayerTemplate::class.java)
        val router: PlayerRouter = Mockito.mock(PlayerRouter::class.java)
        val state = PlayerSkillTreeNodeState(1L, "combat", "root", 0)
        val route: PlayerRoute = Mockito.spy(PlayerRoute(1L, "router", 0L, "job", emptyList(), listOf(state)))
        val pending = CompletableFuture<Map<String, Int>>()
        val gate = GateCondition()
        var reads: Int = 0

        init {
            val conditions = linkedMapOf("gate" to emptyMap<String, Any>(), "mastery" to mapOf("weapon" to "sword", "min" to 3))
            val node = SkillTreeAttributeNode("root", "test", emptyMap(), 2, mapOf(1 to conditions, 2 to conditions), SkillTreeNodePosition(0, 0))
            val tree = ImmutableSkillTree("combat", "测试", "none", TreeType.BASE, mapOf("root" to node), emptyMap())
            Mockito.doReturn(tree).`when`(route).getSkillTreeOrNull("combat")
            Mockito.`when`(player.uniqueId).thenReturn(playerId)
            Mockito.`when`(player.isOnline).thenReturn(true)
            Mockito.`when`(profile.playerRouter).thenReturn(router)
            Mockito.`when`(router.currentRoute).thenReturn(route)
            Mockito.`when`(router.routeLine).thenReturn(listOf(route))
            val mastery = WeaponMasteryCondition("mastery", emptyMap(), "熟练度不足", WeaponMasteryReader {
                reads += 1
                pending
            })
            val evaluator = ConditionEvaluator { mapOf("gate" to gate, "mastery" to mastery) }
            val field = PlayerRoute::class.java.getDeclaredField("evaluator")
            field.isAccessible = true
            field.set(route, evaluator)
        }

        /** 将本测试线程绑定为主线程，只验证异步等待和后续业务状态，不启动调度器。 */
        fun execute(action: () -> Unit) {
            val bukkit = Mockito.mockStatic(Bukkit::class.java)
            try {
                bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
                PlayerTemplateAPI.set(playerId, profile)
                action()
            } finally {
                PlayerTemplateAPI.remove(playerId)
                bukkit.close()
            }
        }
    }

    /** 可变技能点门槛，记录是否发生消耗，验证所有条件通过前绝不提交。 */
    private class GateCondition : ConditionConfig("gate", emptyMap(), "技能点不足", true), ConditionExecution {
        var passed: Boolean = true
        var consumptions: Int = 0
        override val preparationKey: String = "test:gate"

        /** 测试原生门槛没有脚本源码。 */

        /** 同步准备可变点数条件的执行器，在正式执行时读取最新状态。 */
        override fun prepare(playerId: UUID): CompletableFuture<ConditionExecution> {
            return CompletableFuture.completedFuture(this)
        }

        /** 模拟查询结束后的最新技能点校验。 */
        override fun evaluate(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): Boolean {
            return passed
        }

        /** 本测试不走展示批处理，调用说明测试设置错误。 */
        override fun evaluateBatchPersistent(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionInputs: List<Map<String, Any>>): ConditionExecution.BatchEvaluation {
            error("本测试仅验证真实学习入口")
        }

        /** 记录真实执行器发起的消耗次数。 */
        override fun prepareConsume(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): com.gitee.planners.core.condition.LearningCost? {
            consumptions += 1
            return com.gitee.planners.core.condition.SkillPointLearningCost(0)
        }
    }
}
