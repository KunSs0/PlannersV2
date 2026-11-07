package com.gitee.planners.module.action.query

import com.gitee.planners.module.action.ActionCombatFacts
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * 验证 `target.anchored` 计算字段。
 *
 * 该字段由宿主注入的仇恨判定给出；未安装时恒为 false，因此不能凭表达式
 * 推断「谁是当前仇恨目标」。
 */
class AnchoredFieldTest {

    /** 每个用例后恢复默认读取器。 */
    @AfterEach
    fun cleanup() {
        ActionCombatFacts.reset()
    }

    /** 未安装读取器时恒为 false。 */
    @Test
    fun defaultIsFalse() {
        val ref = QueryRef.parse("target.anchored", "test")
        assertEquals(false, ref.resolve(scope("mob", "player")))
    }

    /** 安装后按读取器结果返回。 */
    @Test
    fun usesInstalledReader() {
        ActionCombatFacts.installAnchored { target, sender ->
            target == "mob" && sender == "player"
        }
        val ref = QueryRef.parse("target.anchored", "test")
        assertEquals(true, ref.resolve(scope("mob", "player")))
        assertEquals(false, ref.resolve(scope("mob", "other")))
    }

    /** 可用于条件表达式。 */
    @Test
    fun worksInConditionExpression() {
        ActionCombatFacts.installAnchored { _, _ -> true }
        val ref = QueryRef.parseExpression("target.anchored && skill.level > 1", "test")
        assertEquals(true, ref.resolveBoolean(scope("mob", "player")))
    }

    /** 可用于三元表达式。 */
    @Test
    fun worksInTernary() {
        ActionCombatFacts.installAnchored { _, _ -> false }
        val ref = QueryRef.parseExpression("target.anchored ? 1.2 : 1.0", "test")
        assertEquals(1.0, ref.resolve(scope("mob", "player")))
    }

    /** 构造带锚定读取器的上下文。 */
    private fun scope(target: Any?, sender: Any?): QueryScope {
        return QueryScope(
            sender = sender,
            target = target,
            skill = Any(),
            skillFieldReader = { _, field ->
                if (field == "level") {
                    3L
                } else {
                    null
                }
            },
            anchoredReader = { t, s -> ActionCombatFacts.anchoredReader(t, s) },
            label = "test",
        )
    }
}
