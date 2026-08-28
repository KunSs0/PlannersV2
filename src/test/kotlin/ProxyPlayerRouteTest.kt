import com.gitee.planners.core.script.proxy.ProxyNodeInfo
import com.gitee.planners.core.script.proxy.ProxyPlayerRoute
import com.gitee.planners.core.script.proxy.ProxyRouteTarget
import com.gitee.planners.core.script.proxy.ProxyTreeDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** 验证脚本按节点反查时不会把升级结果和提示拆成两次业务校验。 */
class ProxyPlayerRouteTest {

    @Test
    fun nodeInfoReturnsCompleteResultWithOneBusinessLookup() {
        val target = FakeRouteTarget()
        val route = ProxyPlayerRoute(target, "Dev")

        val first = route.getNodeInfo("combat", "root")
        val second = route.getNodeInfo("combat", "root")

        assertEquals("combat", first.treeId)
        assertEquals("root", first.nodeId)
        assertEquals(2, first.level)
        assertEquals(false, first.canAdvance)
        assertEquals(listOf("节点已满级"), first.hints)
        assertEquals(1, target.lookupCount)
        assertSame(first, second)
    }

    private class FakeRouteTarget : ProxyRouteTarget<String> {

        override val proxySkillTrees: List<ProxyTreeDefinition> = listOf(FakeTreeDefinition())
        var lookupCount: Int = 0

        override fun getNodeInfo(player: String, treeId: String, nodeId: String): ProxyNodeInfo {
            lookupCount += 1
            assertEquals("Dev", player)
            return ProxyNodeInfo(treeId, nodeId, 2, false, listOf("节点已满级"))
        }
    }

    private class FakeTreeDefinition : ProxyTreeDefinition {

        override val id: String = "combat"
        override val nodeIds: List<String> = listOf("root")
    }
}
