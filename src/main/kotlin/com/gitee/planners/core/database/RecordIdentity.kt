package com.gitee.planners.core.database

import java.util.concurrent.atomic.AtomicLong

/**
 * 将稳定的运行时身份与异步生成的数据库主键分开。
 *
 * 在线对象始终使用 [runtimeId]；数据库身份仅由串行持久化队列读写，
 * 不回写职业集合、RPC 身份或在线技能对象，也不让主线程等待 INSERT。
 * @property runtimeId 已加载的正主键或本进程唯一、低于 -1 的临时身份。
 */
class RecordIdentity(val runtimeId: Long) {
    private var storedId: Long? = if (runtimeId >= 0) runtimeId else null

    /** 运行时身份分配入口，不访问数据库或 Bukkit。 */
    companion object {
        private val sequence = AtomicLong(-1)

        /** 分配不与数据库正主键和无父级哨兵 -1 冲突的身份。 */
        fun next(): Long {
            // 原子减一只分配内存编号，不等待后台任务。
            return sequence.decrementAndGet()
        }
    }

    /**
     * 在成功插入后记录真实主键，只允许持久化队列调用。
     * @param id 数据库自动生成的正主键。
     */
    fun inserted(id: Long) {
        // 禁止失败重试静默替换身份，以免后继操作写错记录。
        check(storedId == null && id > 0) { "记录身份已绑定或生成主键非法: $runtimeId" }
        storedId = id
    }

    /**
     * 在后台执行后继 SQL 时解析真实主键。
     * @return 已加载或前序 INSERT 已生成的主键。
     */
    fun persisted(): Long {
        // 前序插入失败时明确报错，不重试、不伪造数据库身份。
        return checkNotNull(storedId) { "前序记录未成功持久化: $runtimeId" }
    }
}
