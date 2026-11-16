package com.gitee.planners.core.config.level

import com.gitee.planners.api.common.Unique
import com.gitee.planners.core.config.ImmutableVariable
import com.gitee.planners.module.action.query.QueryRef
import com.gitee.planners.module.action.query.QueryScope
import com.gitee.planners.module.action.query.QueryText
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import java.util.concurrent.CompletableFuture

interface Algorithm {

    val maxLevel: Int

    val minLevel: Int

    fun getExp(player: Player, level: Int): CompletableFuture<Int>

    fun getCallbacks(level: Int): List<LevelCallback> = emptyList()


    /**
     * 用单条查询公式计算等级经验与回调的算法实现。
     *
     * 经验公式不走脚本编译：`level` 由执行上下文按名提供，
     * 因此配置可以直接写 `level >= 50 ? 2147483647 : [300, 450][level - 1]`。
     *
     * 这是等级表的旧写法，新配置优先使用 [Table] 的等级键写法。
     *
     * @property root 等级算法配置节。
     */
    class Expression(val root: ConfigurationSection) : Algorithm, Unique {

        override val id: String = root.name

        override val minLevel = root.getInt("min")

        override val maxLevel = root.getInt("max")

        private val experience: QueryRef
        init {
            val raw = root.getString("experience")
            if (raw == null || raw.isBlank()) {
                throw IllegalArgumentException("Level algorithm '$id' requires an experience expression")
            }
            experience = QueryRef.parseVariableExpression(raw, "level:$id:experience")
        }

        private val callbacks = parseCallbacks(id, root.getConfigurationSection("callbacks"))

        override fun getExp(player: Player, level: Int): CompletableFuture<Int> {
            val scope = QueryScope(
                sender = player,
                profile = player,
                variables = mapOf("level" to level),
                label = "level:$id:experience",
            )
            val result = experience.resolve(scope)
            if (result == null) {
                throw IllegalStateException("Level algorithm '$id' returned null")
            }
            return CompletableFuture.completedFuture(QueryText.requireDouble(result, "level:$id:experience").cint)
        }

        override fun getCallbacks(level: Int): List<LevelCallback> {
            return callbacks[level] ?: emptyList()
        }

    }

    /**
     * 用等级键表计算经验与回调的算法实现。
     *
     * `experience` 与技能变量的等级分段写法一致：键是等级或闭区间，
     * 值是数字或求值表达式，加载期校验缺档、重叠与区间倒置。
     *
     * @property root 等级算法配置节。
     */
    class Table(val root: ConfigurationSection) : Algorithm, Unique {

        override val id: String = root.name

        override val minLevel = root.getInt("min")

        override val maxLevel = root.getInt("max")

        private val experience: ImmutableVariable
        init {
            val raw = root.get("experience")
            if (raw == null) {
                throw IllegalArgumentException("Level algorithm '$id' requires an experience table")
            }
            val levelRef = QueryRef.parseVariableExpression("level", "level:$id:experience")
            experience = ImmutableVariable.parse(
                "experience",
                raw,
                maxLevel,
                minLevel,
                levelRef,
                "等级算法 '$id' 的 experience",
            )
        }

        private val callbacks = parseCallbacks(id, root.getConfigurationSection("callbacks"))

        override fun getExp(player: Player, level: Int): CompletableFuture<Int> {
            return CompletableFuture.completedFuture(experienceAt(player, level))
        }

        /**
         * 按等级取经验门槛。
         *
         * 供内部与测试直接取值，不构造玩家上下文。
         *
         * @param player 玩家；仅作为查询上下文的 sender，可为 null。
         * @param level 当前等级。
         * @return 该级升到下一级所需经验。
         */
        internal fun experienceAt(player: Player?, level: Int): Int {
            val scope = QueryScope(
                sender = player,
                profile = player,
                variables = mapOf("level" to level),
                label = "level:$id:experience",
            )
            val result = experience.evaluate(scope)
            if (result == null) {
                throw IllegalStateException("Level algorithm '$id' returned null")
            }
            return QueryText.requireDouble(result, "level:$id:experience").cint
        }

        override fun getCallbacks(level: Int): List<LevelCallback> {
            return callbacks[level] ?: emptyList()
        }

    }

    companion object {

        /**
         * 解析一个等级算法。
         *
         * `experience` 是映射时按等级键表解析，是文本时按旧的单条公式解析。
         *
         * @param root 等级算法配置节。
         * @return 等级算法；配置节为空时返回 null。
         * @throws IllegalArgumentException 缺少经验配置、等级范围非法或表结构不合法时抛出。
         */
        fun parse(root: ConfigurationSection?): Algorithm? {
            if (root == null) {
                return null
            }
            val minLevel = root.getInt("min")
            val maxLevel = root.getInt("max")
            if (maxLevel < minLevel) {
                throw IllegalArgumentException(
                    "Level algorithm '${root.name}' max ($maxLevel) must not be less than min ($minLevel)",
                )
            }
            val raw = root.get("experience")
            if (raw is ConfigurationSection || raw is Map<*, *>) {
                return Table(root)
            }
            return Expression(root)
        }

        /**
         * 解析等级回调。
         *
         * @param id 算法 ID，用于告警信息。
         * @param section `callbacks` 配置节。
         * @return 等级到回调列表的映射。
         */
        private fun parseCallbacks(id: String, section: ConfigurationSection?): Map<Int, List<LevelCallback>> {
            if (section == null) {
                return emptyMap()
            }
            val result = LinkedHashMap<Int, List<LevelCallback>>()
            for (key in section.getKeys(false)) {
                val level = key.toIntOrNull()
                if (level == null) {
                    warning("Unknown level callback key: $key")
                    continue
                }
                val sources = ArrayList<String>()
                if (section.isList(key)) {
                    sources.addAll(section.getStringList(key))
                } else if (section.isString(key)) {
                    val source = section.getString(key)
                    if (source != null) {
                        sources.add(source)
                    }
                }
                val callbacks = ArrayList<LevelCallback>()
                for (source in sources) {
                    val callback = LevelCallback.parse(source)
                    if (callback != null) {
                        callbacks.add(callback)
                    }
                }
                result[level] = callbacks
            }
            return result
        }

    }

}
