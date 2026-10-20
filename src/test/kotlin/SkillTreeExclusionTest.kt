import com.gitee.planners.api.PlayerTemplateAPI
import com.gitee.planners.core.condition.ConditionConfig
import com.gitee.planners.core.condition.ConditionEvaluator
import com.gitee.planners.core.condition.ConditionExecution
import com.gitee.planners.core.condition.ConditionSession
import com.gitee.planners.core.config.ImmutableSkillTree
import com.gitee.planners.core.config.SkillTreeNode
import com.gitee.planners.core.config.SkillTreeNodePosition
import com.gitee.planners.core.config.SkillTreeUnlockNode
import com.gitee.planners.core.config.TreeType
import com.gitee.planners.core.player.PlayerRoute
import com.gitee.planners.core.player.PlayerRouter
import com.gitee.planners.core.player.PlayerSkillTreeNodeState
import com.gitee.planners.core.skilltree.SkillTreeNodeEffectService
import com.gitee.planners.core.player.PlayerTemplate
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import taboolib.module.configuration.Configuration
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * 验证真实 graph 解析、分支延伸和不同节点请求的排他行为。
 */
class SkillTreeExclusionTest {
    /**
     * A/B 排斥不构成前置循环，两条分支均可接后续并通过 ANY 汇合。
     */
    @Test
    fun parsesBranchesAndRejectsInvalidExclusions() {
        val fixture = Fixture()
        val graph = parseGraph(validGraph(), fixture.nodes)
        assertEquals(setOf("b"), graph.getValue("a").excludes)
        assertEquals("a", graph.getValue("a_next").edges.single().nodeId)
        assertEquals(ImmutableSkillTree.Graph.Mode.ANY, graph.getValue("tail").groups.getValue("path").mode)
        val invalid = listOf(
            validGraph().replace("excludes: [a]", "excludes: []"),
            validGraph().replace("excludes: [b]", "excludes: [missing]"),
            validGraph().replace("excludes: [b]", "excludes: [a]"),
            validGraph().replace("excludes: [b]", "excludes: [b, b]"),
            validGraph().replace("excludes: [b]", "excludes: b"),
            validGraph().replace("mode: ANY", "mode: ALL")
        )
        for (source in invalid) {
            assertThrows(InvocationTargetException::class.java) {
                parseGraph(source, fixture.nodes)
            }
        }
    }

    /**
     * 选择 A 后 B 被拒绝，A 的后续通过，B 的后续缺前置，公共 ANY 可汇合。
     */
    @Test
    fun checksExclusionsBeforeEveryPreconditionBranch() {
        val fixture = Fixture()
        fixture.execute {
            val empty = ConditionSession(emptyMap())
            assertTrue(fixture.route.canAdvanceNode(fixture.player, "tree", "a", empty).passed)
            fixture.states.getValue("a").level = 1
            val blocked = fixture.route.canAdvanceNode(fixture.player, "tree", "b", empty)
            assertFalse(blocked.passed)
            assertTrue(blocked.hints.single().contains("互斥节点 a"))
            assertTrue(fixture.route.canAdvanceNode(fixture.player, "tree", "a_next", empty).passed)
            assertFalse(fixture.route.canAdvanceNode(fixture.player, "tree", "b_next", empty).passed)
            fixture.states.getValue("a_next").level = 1
            assertTrue(fixture.route.canAdvanceNode(fixture.player, "tree", "tail", empty).passed)
            assertFalse(fixture.route.canAdvanceNode(fixture.player, "tree", "a", empty).passed)
        }
    }

    /**
     * 等待外部条件时另一节点不能进入；异常后释放锁，允许下一次请求。
     */
    @Test
    fun serializesDifferentNodesAndReleasesOnFailure() {
        val fixture = Fixture()
        val gate = Gate()
        val evaluator = ConditionEvaluator { mapOf("gate" to gate) }
        val field = PlayerRoute::class.java.getDeclaredField("evaluator")
        field.isAccessible = true
        field.set(fixture.route, evaluator)
        val levels = mapOf(1 to mapOf("gate" to emptyMap<String, Any>()))
        fixture.nodes["a"] = SkillTreeUnlockNode("a", "分支 A", 1, levels, SkillTreeNodePosition(0, 0))
        fixture.nodes["b"] = SkillTreeUnlockNode("b", "分支 B", 1, levels, SkillTreeNodePosition(0, 0))
        fixture.execute {
            val first = fixture.route.advanceNode(fixture.player, "tree", "a")
            assertFalse(first.isDone)
            val second = fixture.route.advanceNode(fixture.player, "tree", "b")
            assertThrows(CompletionException::class.java) {
                second.join()
            }
            assertEquals(1, gate.preparations)
            gate.pending.completeExceptionally(IllegalStateException("外部条件读取失败"))
            assertThrows(CompletionException::class.java) {
                first.join()
            }
            val retry = fixture.route.advanceNode(fixture.player, "tree", "b")
            assertThrows(CompletionException::class.java) {
                retry.join()
            }
            assertEquals(2, gate.preparations)
            assertEquals(0, fixture.states.getValue("a").level)
            assertEquals(0, fixture.states.getValue("b").level)
        }
    }

    /**
     * 外部条件返回前发生互斥状态变化、配置替换或离线时，提交必须停止。
     */
    @Test
    fun rechecksStateConfigurationAndOnlineStatusBeforeConsumption() {
        for (change in listOf("exclusion", "configuration", "offline")) {
            val fixture = Fixture()
            val condition = ReadyCondition()
            fixture.installCondition(condition)
            fixture.execute {
                val request = fixture.route.advanceNode(fixture.player, "tree", "a")
                assertFalse(request.isDone)
                when (change) {
                    "exclusion" -> {
                        fixture.states.getValue("b").level = 1
                    }
                    "configuration" -> {
                        val replacement = ImmutableSkillTree("tree", "新配置", "none", TreeType.BASE, fixture.nodes, parseGraph(validGraph(), fixture.nodes))
                        Mockito.doReturn(replacement).`when`(fixture.route).getSkillTreeOrNull("tree")
                    }
                    "offline" -> {
                        Mockito.`when`(fixture.player.isOnline).thenReturn(false)
                    }
                }
                condition.pending.complete(condition)
                assertThrows(CompletionException::class.java) {
                    request.join()
                }
                assertEquals(0, condition.consumptions, change)
                assertEquals(0, fixture.states.getValue("a").level, change)
            }
        }
    }

    /**
     * 条件通过后只进入一次消费；消费异常不能记为已学，也不能遗留请求锁。
     */
    @Test
    fun releasesLockWithoutLearningWhenConsumptionFails() {
        val fixture = Fixture()
        val condition = ReadyCondition()
        fixture.installCondition(condition)
        fixture.execute {
            val first = fixture.route.advanceNode(fixture.player, "tree", "a")
            condition.pending.complete(condition)
            assertThrows(CompletionException::class.java) {
                first.join()
            }
            assertEquals(1, condition.consumptions)
            assertEquals(0, fixture.states.getValue("a").level)
            val retry = fixture.route.advanceNode(fixture.player, "tree", "b")
            assertThrows(CompletionException::class.java) {
                retry.join()
            }
            assertEquals(2, condition.consumptions)
            assertEquals(0, fixture.states.getValue("b").level)
        }
    }

    /**
     * 解锁记录可以被业务读取，但不查找属性或技能 Provider。
     */
    @Test
    fun learnedUnlockDoesNotCreateAttributeOrSkillContributions() {
        val fixture = Fixture()
        fixture.states.getValue("a").level = 1
        val tree = fixture.route.getSkillTreeOrNull("tree")
        Mockito.doReturn(listOf(tree)).`when`(fixture.route).skillTrees
        assertTrue(SkillTreeNodeEffectService.getAttributes(fixture.profile).isEmpty())
        assertEquals(0, SkillTreeNodeEffectService.getSkillLevel(fixture.profile, "unrelated"))
    }

    /**
     * 调用真实配置关系解析器，隔离插件启动和条件模板生命周期。
     *
     * @param source graph YAML。
     * @param nodes 同树真实节点。
     * @return 真实解析器生成的关系。
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseGraph(source: String, nodes: Map<String, SkillTreeNode>): Map<String, ImmutableSkillTree.Graph> {
        // 此处仅隔离不属于 graph 的插件启动边界，执行生产解析逻辑。
        val method = ImmutableSkillTree.Companion::class.java.declaredMethods.single { it.name == "parseGraph" }
        method.isAccessible = true
        return method.invoke(ImmutableSkillTree, "tree", Configuration.loadFromString(source), nodes) as Map<String, ImmutableSkillTree.Graph>
    }

    /**
     * 创建两条可延伸、可汇合的通用分支配置。
     *
     * @return 完整 graph 配置。
     */
    private fun validGraph(): String {
        return """
            graph:
              a:
                excludes: [b]
                groups:
                  path: { mode: ALL, requirements: {} }
              b:
                excludes: [a]
                groups:
                  path: { mode: ALL, requirements: {} }
              a_next:
                groups:
                  path: { mode: ALL, requirements: { a: 1 } }
              b_next:
                groups:
                  path: { mode: ALL, requirements: { b: 1 } }
              tail:
                groups:
                  path: { mode: ANY, requirements: { a: 1, b: 1 } }
        """.trimIndent()
    }

    /**
     * 只模拟玩家与平台，使用真实 PlayerRoute 和 graph 判定。
     */
    private inner class Fixture {
        val id: UUID = UUID.randomUUID()
        val player: Player = Mockito.mock(Player::class.java)
        val profile: PlayerTemplate = Mockito.mock(PlayerTemplate::class.java)
        val router: PlayerRouter = Mockito.mock(PlayerRouter::class.java)
        val nodes = LinkedHashMap<String, SkillTreeNode>()
        val states = LinkedHashMap<String, PlayerSkillTreeNodeState>()
        val route: PlayerRoute

        init {
            for ((index, nodeId) in listOf("a", "b", "a_next", "b_next", "tail").withIndex()) {
                nodes[nodeId] = SkillTreeUnlockNode(nodeId, nodeId, 1, mapOf(1 to emptyMap()), SkillTreeNodePosition(index, 0))
                states[nodeId] = PlayerSkillTreeNodeState(index.toLong(), "tree", nodeId, 0)
            }
            val tree = ImmutableSkillTree("tree", "测试", "none", TreeType.BASE, nodes, parseGraph(validGraph(), nodes))
            route = Mockito.spy(PlayerRoute(1L, "router", -1L, "job", emptyList(), states.values.toList()))
            Mockito.doReturn(tree).`when`(route).getSkillTreeOrNull("tree")
            // 条件模板属于宿主边界，此处显式注入空模板并运行真实节点判定。
            val evaluatorField = PlayerRoute::class.java.getDeclaredField("evaluator")
            evaluatorField.isAccessible = true
            evaluatorField.set(route, ConditionEvaluator { emptyMap() })
            Mockito.`when`(player.uniqueId).thenReturn(id)
            Mockito.`when`(player.isOnline).thenReturn(true)
            Mockito.`when`(profile.playerRouter).thenReturn(router)
            Mockito.`when`(router.currentRoute).thenReturn(route)
            Mockito.`when`(router.routeLine).thenReturn(listOf(route))
        }

        /**
         * 给真实学习入口注入受控外部条件与两支相同成本。
         *
         * @param condition 测试平台条件。
         */
        fun installCondition(condition: ReadyCondition) {
            // 只替换外部条件来源，不替换学习、排斥或消费编排。
            val field = PlayerRoute::class.java.getDeclaredField("evaluator")
            field.isAccessible = true
            field.set(route, ConditionEvaluator { mapOf("ready" to condition) })
            val levels = mapOf(1 to mapOf("ready" to emptyMap<String, Any>()))
            for (nodeId in listOf("a", "b")) {
                nodes[nodeId] = SkillTreeUnlockNode(nodeId, nodeId, 1, levels, SkillTreeNodePosition(0, 0))
            }
        }

        /**
         * 在明确的主线程边界运行断言并清理玩家绑定。
         *
         * @param action 测试步骤。
         */
        fun execute(action: () -> Unit) {
            val bukkit = Mockito.mockStatic(Bukkit::class.java)
            try {
                bukkit.`when`<Boolean> { Bukkit.isPrimaryThread() }.thenReturn(true)
                PlayerTemplateAPI.set(id, profile)
                action()
            } finally {
                PlayerTemplateAPI.remove(id)
                bukkit.close()
            }
        }
    }

    /**
     * 挂起后返回成功验证，消费阶段显式报错以隔离真实数据库写入。
     */
    private class ReadyCondition : ConditionConfig("ready", emptyMap(), "未准备", true), ConditionExecution {
        val pending = CompletableFuture<ConditionExecution>()
        var consumptions = 0
        override val preparationKey = "test:ready"

        /**
         * 测试原生条件不注册脚本。
         */

        /**
         * 返回由测试控制的宿主查询。
         */
        override fun prepare(playerId: UUID): CompletableFuture<ConditionExecution> {
            return pending
        }

        /**
         * 外部条件本身通过，排斥和代际由生产学习入口检查。
         */
        override fun evaluate(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): Boolean {
            return true
        }

        /**
         * 本场景不使用展示批处理。
         */
        override fun evaluateBatchPersistent(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionInputs: List<Map<String, Any>>): ConditionExecution.BatchEvaluation {
            error("本场景不使用批处理")
        }

        /**
         * 记录进入次数并模拟消费拒绝，确保不触碰数据库。
         */
        override fun prepareConsume(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): com.gitee.planners.core.condition.LearningCost? {
            consumptions += 1
            error("宿主消费拒绝")
        }
    }

    /**
     * 显式挂起的外部条件，用于验证跨节点请求锁和失败释放。
     */
    private class Gate : ConditionConfig("gate", emptyMap(), "条件未满足", false), ConditionExecution {
        val pending = CompletableFuture<ConditionExecution>()
        var preparations = 0
        override val preparationKey = "test:gate"

        /**
         * 原生测试条件不注册脚本。
         */

        /**
         * 记录准备次数并返回明确的未完成查询。
         */
        override fun prepare(playerId: UUID): CompletableFuture<ConditionExecution> {
            preparations += 1
            return pending
        }

        /**
         * 本场景在准备阶段失败，不应执行实际校验。
         */
        override fun evaluate(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): Boolean {
            error("准备失败后不应校验")
        }

        /**
         * 本场景不调用展示批处理。
         */
        override fun evaluateBatchPersistent(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionInputs: List<Map<String, Any>>): ConditionExecution.BatchEvaluation {
            error("本场景不使用批处理")
        }

        /**
         * 准备失败后不允许发生消费。
         */
        override fun prepareConsume(player: Player, profile: PlayerTemplate, router: PlayerRouter?, route: PlayerRoute?, conditionProps: Map<String, Any>): com.gitee.planners.core.condition.LearningCost? {
            error("准备失败后不应消费")
        }
    }
}
