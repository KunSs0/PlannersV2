package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

/**
 * 验证 `@sort` 的生命值/生命比例键与排序方向。
 *
 * 实体边界用 Mockito 模拟 `LivingEntity`，只替换生命值读取；排序逻辑执行真实生产实现。
 */
class ActionSortTargeterTest {

    private val scope = QueryScope()

    /** 每个用例前恢复内置条目。 */
    @BeforeEach
    fun reset() {
        ActionRegistry.resetToBuiltins()
    }

    /** 生命比例升序：最缺血的目标排在最前。 */
    @Test
    fun sortsByHealthRatioAscending() {
        val healthy = entity(90.0, 100.0)
        val dying = entity(20.0, 100.0)
        val half = entity(50.0, 100.0)
        val result = sort(mapOf("by" to "health-ratio"), listOf(healthy, dying, half))
        assertEquals(listOf(dying, half, healthy), result)
    }

    /** 生命比例降序：满血优先。 */
    @Test
    fun sortsByHealthRatioDescending() {
        val healthy = entity(90.0, 100.0)
        val dying = entity(20.0, 100.0)
        val half = entity(50.0, 100.0)
        val result = sort(mapOf("by" to "health-ratio", "order" to "desc"), listOf(dying, healthy, half))
        assertEquals(listOf(healthy, half, dying), result)
    }

    /** 生命值排序按绝对值比较，不按比例。 */
    @Test
    fun sortsByAbsoluteHealth() {
        val lowAbsolute = entity(8.0, 10.0)
        val highAbsolute = entity(60.0, 100.0)
        val result = sort(mapOf("by" to "health"), listOf(highAbsolute, lowAbsolute))
        assertEquals(listOf(lowAbsolute, highAbsolute), result)
    }

    /** 缺少实体句柄的目标在升序与降序下都排在最后。 */
    @Test
    fun keepsMissingValuesLastInBothOrders() {
        val damaged = entity(40.0, 100.0)
        val location = ActionTarget.LocationTarget(Location(null, 0.0, 0.0, 0.0))
        val ascending = sort(mapOf("by" to "health-ratio"), listOf(location, damaged))
        assertEquals(listOf(damaged, location), ascending)
        val descending = sort(mapOf("by" to "health-ratio", "order" to "desc"), listOf(damaged, location))
        assertEquals(listOf(damaged, location), descending)
    }

    /** 相同生命比例保持输入顺序。 */
    @Test
    fun keepsInputOrderForTies() {
        val first = entity(50.0, 100.0)
        val second = entity(25.0, 50.0)
        val result = sort(mapOf("by" to "health-ratio"), listOf(first, second))
        assertSame(first, result[0])
        assertSame(second, result[1])
    }

    /** 未声明顺序时按升序处理。 */
    @Test
    fun defaultsToAscending() {
        val heavy = entity(80.0, 100.0)
        val light = entity(10.0, 100.0)
        val result = sort(mapOf("by" to "health-ratio"), listOf(heavy, light))
        assertEquals(listOf(light, heavy), result)
    }

    /** 未知排序键在运行时给出全部可选键。 */
    @Test
    fun reportsUnknownSortKey() {
        val arguments = ActionArguments(mapOf("by" to "mana"), "skill.test.sort")
        val error = assertThrows<IllegalArgumentException> {
            BuiltinTargeters.Sort.resolve(scope, arguments, emptyList())
        }
        assertTrue(error.message!!.contains("health-ratio"))
    }

    /** 顺序值只接受 asc 与 desc。 */
    @Test
    fun rejectsInvalidOrder() {
        val arguments = ActionArguments(mapOf("by" to "health", "order" to "up"), "skill.test.sort")
        val error = assertThrows<IllegalArgumentException> {
            BuiltinTargeters.Sort.validate(arguments)
        }
        assertEquals("skill.test.sort order must be asc or desc.", error.message)
    }

    /** 名称与随机排序不接受顺序，避免写出不生效的配置。 */
    @Test
    fun rejectsOrderForNonNumericKeys() {
        val nameError = assertThrows<IllegalArgumentException> {
            BuiltinTargeters.Sort.validate(ActionArguments(mapOf("by" to "name", "order" to "desc"), "skill.test.sort"))
        }
        assertTrue(nameError.message!!.contains("order 只对 distance / health / health-ratio 生效"))
        val randomError = assertThrows<IllegalArgumentException> {
            BuiltinTargeters.Sort.validate(ActionArguments(mapOf("by" to "random", "order" to "desc"), "skill.test.sort"))
        }
        assertTrue(randomError.message!!.contains("order 只对 distance / health / health-ratio 生效"))
    }

    /** 排序键必填。 */
    @Test
    fun requiresSortKey() {
        val error = assertThrows<IllegalArgumentException> {
            BuiltinTargeters.Sort.validate(ActionArguments(emptyMap(), "skill.test.sort"))
        }
        assertEquals("skill.test.sort requires argument 'by'.", error.message)
    }

    /** 用真实目标器解析并执行排序。 */
    private fun sort(values: Map<String, String>, input: List<ActionTarget>): List<ActionTarget> {
        val arguments = ActionArguments(values, "skill.test.sort")
        BuiltinTargeters.Sort.validate(arguments)
        return BuiltinTargeters.Sort.resolve(scope, arguments, input)
    }

    /** 构造带生命值的实体目标。 */
    private fun entity(health: Double, maximum: Double): ActionTarget {
        val living = Mockito.mock(LivingEntity::class.java)
        Mockito.`when`(living.isValid).thenReturn(true)
        Mockito.`when`(living.isDead).thenReturn(false)
        Mockito.`when`(living.health).thenReturn(health)
        Mockito.`when`(living.maxHealth).thenReturn(maximum)
        return ActionTarget.EntityTarget(living)
    }
}
