import com.gitee.planners.core.database.AsyncDatabaseQueue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 验证主线程提交与停滞 SQL 独立，队列有序且异常不会形成等待锁。 */
class AsyncDatabaseQueueTest {
    /** 数据库被闸门阻塞时，调用线程仍可立即提交后续任务并返回。 */
    @Test
    fun stalledDatabaseDoesNotBlockSubmittingThread() {
        val executor = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val failures = ArrayList<Throwable>()
        val queue = AsyncDatabaseQueue({ task -> executor.execute(task) }, { _, failure -> failures.add(failure) })
        try {
            val first = queue.submit("stalled SQL") {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                1
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val began = System.nanoTime()
            val second = queue.submit("next write") { 2 }
            val read = queue.submit("profile read after save") { 3 }
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - began) < 500)
            assertFalse(first.isDone)
            assertFalse(second.isDone)
            assertFalse(read.isDone)
            release.countDown()
            assertEquals(1, first.get(5, TimeUnit.SECONDS))
            assertEquals(2, second.get(5, TimeUnit.SECONDS))
            assertEquals(3, read.get(5, TimeUnit.SECONDS))
            assertTrue(failures.isEmpty())
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    /** 同一批保存及读档案按提交顺序执行；单条写入失败独立记录。 */
    @Test
    fun preservesOrderingAndContinuesAfterFailure() {
        val tasks = ArrayList<Runnable>()
        val records = ArrayList<Int>()
        val failures = ArrayList<Throwable>()
        val queue = AsyncDatabaseQueue({ task -> tasks.add(task) }, { _, failure -> failures.add(failure) })
        val first = queue.submit("first") { records.add(1) }
        val failed = queue.submit<Unit>("failure") { error("database down") }
        val last = queue.submit("load") {
            assertEquals(listOf(1), records)
            records.add(3)
        }
        assertEquals(1, tasks.size)
        assertFalse(first.isDone)
        tasks.removeAt(0).run()
        assertTrue(first.isDone)
        assertTrue(failed.isCompletedExceptionally)
        assertTrue(last.isDone)
        assertEquals(listOf(1, 3), records)
        assertEquals("database down", failures.single().message)
    }

    /** 调度失败完成异常 Future，不悬挂、不在调用线程执行 SQL。 */
    @Test
    fun schedulingRejectionNeverExecutesSqlOrLeavesFuturePending() {
        var reject = true
        var executed = false
        val tasks = ArrayList<Runnable>()
        val failures = ArrayList<Throwable>()
        val queue = AsyncDatabaseQueue({ task ->
            if (reject) {
                error("scheduler stopped")
            }
            tasks.add(task)
        }, { _, failure -> failures.add(failure) })
        val failed = queue.submit("write") { executed = true }
        assertTrue(failed.isCompletedExceptionally)
        assertFalse(executed)
        reject = false
        val next = queue.submit("new write") { 7 }
        tasks.removeAt(0).run()
        assertEquals(7, next.join())
        assertEquals(1, failures.size)
    }

    /** 多个提交线程并发进入也只有单一 SQL 消费者，不会丢失交接期间的任务。 */
    @Test
    fun concurrentSubmissionsHaveOnlyOneConsumerAndNoLostTasks() {
        val backend = Executors.newFixedThreadPool(4)
        val callers = Executors.newFixedThreadPool(4)
        val active = java.util.concurrent.atomic.AtomicInteger()
        val maxActive = java.util.concurrent.atomic.AtomicInteger()
        val finished = CountDownLatch(200)
        val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        val queue = AsyncDatabaseQueue({ task -> backend.execute(task) }, { _, failure -> failures.add(failure) })
        try {
            for (index in 0 until 200) {
                callers.execute {
                    queue.submit("$index") {
                        val count = active.incrementAndGet()
                        maxActive.accumulateAndGet(count, ::maxOf)
                        active.decrementAndGet()
                        finished.countDown()
                    }
                }
            }
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            assertEquals(1, maxActive.get())
            assertTrue(failures.isEmpty())
        } finally {
            callers.shutdownNow()
            backend.shutdownNow()
        }
    }
}
