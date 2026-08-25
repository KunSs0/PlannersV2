import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillTreeRuntimeScenarioTest {

    @Test
    fun oneSessionCanRebindPlayerValuesBetweenSkillCalculations() {
        val session = createSession()
        try {
            val bindings = session.getBindings("js")
            bindings.putMember("level", 2)
            val first = session.eval("js", "level * 10").asInt()
            bindings.putMember("level", 4)
            val second = session.eval("js", "level * 10").asInt()
            assertEquals(20, first)
            assertEquals(40, second)
        } finally {
            session.close()
        }
    }

    @Test
    fun skillVariableBatchKeepsValuesIsolatedAfterRebind() {
        val session = createSession()
        try {
            val bindings = session.getBindings("js")
            bindings.putMember("level", 3)
            val first = session.eval("js", "({power: level * 10, label: 'skill-' + level})")
            bindings.putMember("level", 7)
            val second = session.eval("js", "({power: level * 10, label: 'skill-' + level})")
            assertEquals(30, first.getMember("power").asInt())
            assertEquals("skill-3", first.getMember("label").asString())
            assertEquals(70, second.getMember("power").asInt())
            assertEquals("skill-7", second.getMember("label").asString())
            assertNotSame(first, second)
        } finally {
            session.close()
        }
    }

    @Test
    fun sessionReuseAvoidsCreatingOneContextPerSkill() {
        val sharedSession = createSession()
        val separateSession = createSession()
        try {
            val sharedBindings = sharedSession.getBindings("js")
            sharedBindings.putMember("level", 1)
            val sharedFirst = sharedSession.eval("js", "level + 1").asInt()
            sharedBindings.putMember("level", 2)
            val sharedSecond = sharedSession.eval("js", "level + 1").asInt()
            val separateBindings = separateSession.getBindings("js")
            separateBindings.putMember("level", 1)
            val separateValue = separateSession.eval("js", "level + 1").asInt()
            assertEquals(2, sharedFirst)
            assertEquals(3, sharedSecond)
            assertEquals(2, separateValue)
            assertTrue(sharedSession !== separateSession)
        } finally {
            sharedSession.close()
            separateSession.close()
        }
    }

    @Test
    fun measureSessionReuseCost() {
        val warmupSession = createSession()
        warmupSession.getBindings("js").putMember("level", 1)
        repeat(100) {
            warmupSession.eval("js", "level * 10").asInt()
        }
        warmupSession.close()

        val sharedSession = createSession()
        val sharedBindings = sharedSession.getBindings("js")
        val sharedStart = System.nanoTime()
        repeat(1000) { index ->
            sharedBindings.putMember("level", index)
            sharedSession.eval("js", "level * 10").asInt()
        }
        val sharedElapsed = System.nanoTime() - sharedStart
        sharedSession.close()

        val separateStart = System.nanoTime()
        repeat(20) { index ->
            val separateSession = createSession()
            separateSession.getBindings("js").putMember("level", index)
            separateSession.eval("js", "level * 10").asInt()
            separateSession.close()
        }
        val separateElapsed = System.nanoTime() - separateStart
        val sharedMicrosPerEval = sharedElapsed / 1_000_000.0
        val separateMicrosPerEval = separateElapsed / 20_000.0

        println(
            "[SkillTreePerfTest] sharedEvalTotalMs=" + (sharedElapsed / 1_000_000.0) +
                " sharedEvalUsPerOp=" + sharedMicrosPerEval +
                " separateContextTotalMs=" + (separateElapsed / 1_000_000.0) +
                " separateContextUsPerOp=" + separateMicrosPerEval
        )
        assertTrue(sharedElapsed > 0L)
        assertTrue(separateElapsed > 0L)
    }

    private fun createSession(): Context {
        return Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup { true }
            .option("engine.WarnInterpreterOnly", "false")
            .build()
    }
}
