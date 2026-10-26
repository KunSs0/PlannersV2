package com.gitee.planners.core.database

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 非阻塞提交、单消费者执行的数据库队列。
 *
 * 提交线程只入队并安排任务，不获取业务对象锁、不等待 SQL；任务异常独立上报，
 * 后续任务继续执行。读档案也走同一队列，防止重进游戏读到尚未完成的旧保存。
 * @param schedule 后台调度入口，禁止同步执行。
 * @param report 独立故障记录入口。
 */
class AsyncDatabaseQueue(
    private val schedule: (Runnable) -> Unit,
    private val report: (String, Throwable) -> Unit
) {
    private val pending = ConcurrentLinkedQueue<Job>()
    private val running = AtomicBoolean(false)

    /** 执行和调度拒绝各有独立完成入口，保证读档案 Future 不悬挂。 */
    private class Job(val execute: Runnable, val reject: (Throwable) -> Unit)

    /**
     * 提交一次独立数据库操作，返回值仅用于非阻塞读档案续体。
     * @param description 包含玩家和记录身份的故障上下文。
     * @param operation 只在后台执行的数据库操作。
     * @return 操作结果；写业务不得等待这个结果。
     */
    fun <T> submit(description: String, operation: () -> T): CompletableFuture<T> {
        val result = CompletableFuture<T>()
        // 队列元素持有已捕获的数据，不能再从后台查询在线对象。
        pending.add(Job(Runnable {
            try {
                result.complete(operation())
            } catch (failure: Throwable) {
                report(description, failure)
                result.completeExceptionally(failure)
            }
        }, { failure ->
            report(description, failure)
            result.completeExceptionally(failure)
        }))
        start()
        return result
    }

    /** 安排唯一消费者；CAS 失败表示已有消费者，无需等待。 */
    private fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        try {
            schedule(Runnable { drain() })
        } catch (failure: Throwable) {
            // 调度拒绝属于独立持久化故障，不在提交线程执行数据库作为替代。
            while (true) {
                val operation = pending.poll()
                if (operation == null) {
                    break
                }
                operation.reject(failure)
            }
            running.set(false)
            if (pending.isNotEmpty()) {
                start()
            }
        }
    }

    /** 在一个后台任务中依次执行，结束时交接并发提交的剩余元素。 */
    private fun drain() {
        try {
            while (true) {
                val operation = pending.poll()
                if (operation == null) {
                    break
                }
                operation.execute.run()
            }
        } finally {
            running.set(false)
            if (pending.isNotEmpty()) {
                start()
            }
        }
    }
}
