package com.gitee.planners.module.action

import com.gitee.planners.module.action.query.QueryScope

/**
 * 一次执行内的目标缓存。
 *
 * 同一批目标做多件事时只查询一次世界；缓存随执行上下文销毁，
 * 不跨帧、不跨玩家、不跨重载保留。
 */
class TargetCache {

    private val entries = LinkedHashMap<String, List<ActionTarget>>()

    /**
     * 写入命名缓存，同 id 覆盖。
     *
     * @param key 缓存名。
     * @param value 目标容器快照。
     */
    fun store(key: String, value: List<ActionTarget>) {
        entries[key] = ArrayList(value)
    }

    /**
     * 读出命名缓存。
     *
     * @param key 缓存名。
     * @return 目标容器的副本；未写入时返回 null。
     */
    fun load(key: String): List<ActionTarget>? {
        val value = entries[key]
        if (value == null) {
            return null
        }
        return ArrayList(value)
    }

    /**
     * 判断缓存是否已写入。
     *
     * @param key 缓存名。
     * @return 已写入时返回 true。
     */
    fun contains(key: String): Boolean {
        return entries.containsKey(key)
    }

    /**
     * 清空全部缓存。
     */
    fun clear() {
        entries.clear()
    }

    companion object {

        /**
         * 从执行上下文取出缓存，缺失时创建并回填。
         *
         * @param scope 查询上下文。
         * @param key 缓存名。
         * @param value 目标容器。
         */
        fun store(scope: QueryScope, key: String, value: List<ActionTarget>) {
            scope.targetCache.store(key, value)
        }

        /**
         * 从执行上下文取出缓存。
         *
         * @param scope 查询上下文。
         * @param key 缓存名。
         * @return 目标容器；未写入时返回 null。
         */
        fun load(scope: QueryScope, key: String): List<ActionTarget>? {
            return scope.targetCache.load(key)
        }
    }
}
