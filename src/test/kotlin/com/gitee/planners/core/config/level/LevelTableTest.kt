package com.gitee.planners.core.config.level

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 验证等级算法的等级键表写法。
 *
 * 覆盖单级、区间、表达式取值、min 起点、超出上限的预留档、缺档/重叠/倒置校验，
 * 以及单条公式写法的兼容与按类型分派。
 */
class LevelTableTest {

    /** 测试用等级算法配置。 */
    private val config = Configuration.loadFromFile(File("src/test/resources/level-table.yml"))

    /**
     * 按配置节点 ID 解析等级算法。
     *
     * @param id 测试配置中的节点 ID。
     * @return 已解析的等级键表算法。
     */
    private fun table(id: String): Algorithm.Table {
        val section = config.getConfigurationSection(id)
        assertTrue(section != null, "测试配置缺少节点 $id")
        val algorithm = Algorithm.parse(section!!)
        assertTrue(algorithm is Algorithm.Table, "$id 应按等级键表解析")
        return algorithm as Algorithm.Table
    }

    /** 单级键按当前等级取值。 */
    @Test
    fun readsSingleLevelKeys() {
        val algorithm = table("table")

        assertEquals(300, algorithm.experienceAt(null, 1))
        assertEquals(450, algorithm.experienceAt(null, 2))
        assertEquals(630, algorithm.experienceAt(null, 3))
    }

    /** 区间键覆盖的等级取同一值。 */
    @Test
    fun readsRangeKeys() {
        val algorithm = table("reserved")

        assertEquals(630, algorithm.experienceAt(null, 3))
        assertEquals(630, algorithm.experienceAt(null, 4))
    }

    /** 分段值可以是引用 level 的表达式。 */
    @Test
    fun evaluatesExpressionValues() {
        val algorithm = table("expression")

        assertEquals(310, algorithm.experienceAt(null, 1))
        assertEquals(300, algorithm.experienceAt(null, 3))
    }

    /** min 起点非 1 时按配置的等级范围校验与取值。 */
    @Test
    fun startsFromConfiguredMinLevel() {
        val algorithm = table("zero")

        assertEquals(0, algorithm.minLevel)
        assertEquals(100, algorithm.experienceAt(null, 0))
        assertEquals(300, algorithm.experienceAt(null, 1))
    }

    /** 低于等级上限的分段允许超出 max 预留，不影响 max 内的取值。 */
    @Test
    fun keepsReservedSpansAboveMax() {
        val algorithm = table("reserved")

        assertEquals(2, algorithm.maxLevel)
        assertEquals(450, algorithm.experienceAt(null, 2))
    }

    /** 超出等级上限且没有预留档时求值报错。 */
    @Test
    fun rejectsLevelBeyondTable() {
        val algorithm = table("table")

        assertThrows(IllegalArgumentException::class.java) {
            algorithm.experienceAt(null, 4)
        }
    }

    /** 缺档在加载期报错并列出等级。 */
    @Test
    fun rejectsMissingLevel() {
        val section = config.getConfigurationSection("missing")
        assertTrue(section != null)

        val error = assertThrows(IllegalArgumentException::class.java) {
            Algorithm.parse(section!!)
        }

        assertTrue(error.message!!.contains("3"))
    }

    /** 重叠分段在加载期报错。 */
    @Test
    fun rejectsOverlappingSpans() {
        val section = config.getConfigurationSection("overlap")
        assertTrue(section != null)

        val error = assertThrows(IllegalArgumentException::class.java) {
            Algorithm.parse(section!!)
        }

        assertTrue(error.message!!.contains("多个分段"))
    }

    /** max 小于 min 在加载期报错。 */
    @Test
    fun rejectsInvertedLevelRange() {
        val section = config.getConfigurationSection("inverted")
        assertTrue(section != null)

        assertThrows(IllegalArgumentException::class.java) {
            Algorithm.parse(section!!)
        }
    }

    /** 单条公式写法仍按表达式算法解析。 */
    @Test
    fun keepsStringExpressionForm() {
        val config = Configuration.fromMap(
            mapOf(
                "legacy" to mapOf(
                    "min" to 1,
                    "max" to 10,
                    "experience" to "level * 200",
                ),
            ),
        )
        val section = config.getConfigurationSection("legacy")
        assertTrue(section != null)

        val algorithm = Algorithm.parse(section!!)

        assertTrue(algorithm is Algorithm.Expression)
        assertEquals(10, algorithm!!.maxLevel)
    }

    /** 缺少 experience 在加载期报错。 */
    @Test
    fun rejectsMissingExperience() {
        val config = Configuration.fromMap(
            mapOf(
                "empty" to mapOf(
                    "min" to 1,
                    "max" to 10,
                ),
            ),
        )
        val section = config.getConfigurationSection("empty")
        assertTrue(section != null)

        assertThrows(IllegalArgumentException::class.java) {
            Algorithm.parse(section!!)
        }
    }
}
